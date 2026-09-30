package uoc.edu.psirvent.murmr.presentation.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.audio.CapturingTransport
import uoc.edu.psirvent.murmr.audio.GgwaveTransport
import uoc.edu.psirvent.murmr.audio.PairingEvent
import uoc.edu.psirvent.murmr.audio.PairingProtocol
import uoc.edu.psirvent.murmr.audio.SystemVolume
import uoc.edu.psirvent.murmr.crypto.KeyStoreManager
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.model.PairingFailure
import uoc.edu.psirvent.murmr.domain.model.PairingState
import uoc.edu.psirvent.murmr.domain.usecase.StartAcousticPairingUseCase
import uoc.edu.psirvent.murmr.util.Logger

class PairingViewModel(
    private val userRepository: UserRepository,
    private val keyStore: KeyStoreManager,
    private val confirmUseCase: StartAcousticPairingUseCase,
    private val transportFactory: () -> GgwaveTransport,
    private val systemVolume: SystemVolume,
) : ScreenModel {

    private val tag = "PairingVM"
    private val _state = MutableStateFlow<PairingState>(PairingState.Idle)
    val state: StateFlow<PairingState> = _state.asStateFlow()

    private val _volumeWarning = MutableStateFlow<String?>(null)
    val volumeWarning: StateFlow<String?> = _volumeWarning.asStateFlow()

    private var transport: GgwaveTransport? = null
    private var listenJob: Job? = null

    init {

        screenModelScope.launch {
            while (true) {
                val curState = _state.value
                if (curState is PairingState.Idle || curState is PairingState.Failed) {
                    val level = systemVolume.currentFraction()
                    if (level >= 0.5f && _volumeWarning.value != null) {
                        Logger.i(tag, "poll volumen: ${(level * 100).toInt()}% por encima del 50%, quito aviso")
                        _volumeWarning.value = null
                    }
                }
                delay(700)
            }
        }
    }

    fun dismissVolumeWarning() {
        _volumeWarning.value = null
    }

    private fun checkAndAdjustVolume(): Boolean {
        val level = systemVolume.currentFraction()
        Logger.i(tag, "volumen sistema: ${(level * 100).toInt()}%")
        if (level >= 0.5f) {
            _volumeWarning.value = null
            return true
        }
        val raised = systemVolume.tryRaiseToMax()
        val finalLevel = systemVolume.currentFraction()
        if (raised && finalLevel >= 0.5f) {
            Logger.i(tag, "auto-subido al ${(finalLevel * 100).toInt()}%")
            _volumeWarning.value =
                "Subimos el volumen al ${(finalLevel * 100).toInt()}% para mejorar la conexión acústica."
            return true
        }
        Logger.w(tag, "volumen ${(finalLevel * 100).toInt()}% < 50%, bloqueando pairing")
        _volumeWarning.value =
            "Volumen al ${(finalLevel * 100).toInt()}%. Súbelo al menos al 50% con los botones laterales y vuelve a pulsar."
        return false
    }

    fun start() {

        val active = listenJob?.isActive == true
        val st = _state.value
        val canRestart = st is PairingState.Idle ||
            st is PairingState.Failed ||
            st is PairingState.Verified
        if (active && !canRestart) {
            Logger.w(tag, "start ignorado: pairing en curso (state=${st::class.simpleName})")
            return
        }
        Logger.i(tag, "start (modo AUTO)")
        startInternal()
    }

    private fun startInternal() {
        cancelJob()
        if (!checkAndAdjustVolume()) {
            Logger.w(tag, "startInternal abortado: volumen insuficiente")
            return
        }

        val previousTransport = transport
        transport = null
        listenJob = screenModelScope.launch {
            if (previousTransport != null) {
                runCatching { previousTransport.stop() }
                    .onFailure { Logger.w(tag, "stop transport previo fallo: ${it.message}") }
                Logger.i(tag, "transport previo cerrado antes de re-arranque")
            }
            val me = userRepository.getLocalUser()
            if (me == null) {
                _state.value = PairingState.Failed(PairingFailure.UNKNOWN)
                return@launch
            }
            val t = transportFactory().also { transport = it }
            Logger.i(tag, "transport creado: ${t::class.simpleName}")
            try {
                (t as? CapturingTransport)?.startCapture(screenModelScope)
                _state.value = PairingState.Discovering

                val onEvent: (PairingEvent) -> Unit = { ev ->
                    when (ev) {
                        is PairingEvent.PeerDiscovered -> {
                            _state.value = PairingState.PeerFound(ev.peerAlias)
                        }
                        is PairingEvent.KeyExchangeStarted -> {
                            val cur = _state.value
                            val peerAlias = (cur as? PairingState.PeerFound)?.peerAlias
                                ?: (cur as? PairingState.ExchangingKeys)?.peerAlias
                                ?: "?"
                            _state.value = PairingState.ExchangingKeys(peerAlias, ev.profileName)
                        }
                        is PairingEvent.PhaseChange -> {
                            val cur = _state.value as? PairingState.ExchangingKeys
                            if (cur != null) {
                                _state.value = cur.copy(phase = ev.phase, phaseDetail = ev.detail)
                            }
                        }
                        is PairingEvent.KeyExchangeComplete -> Unit
                    }
                }

                val protocol = PairingProtocol(t, keyStore)
                val outcome = protocol.auto(me, onEvent)
                if (outcome == null) {
                    val cur = _state.value
                    _state.value = PairingState.Failed(
                        if (cur is PairingState.Discovering) PairingFailure.NO_PEER_FOUND
                        else PairingFailure.AUDIO_TIMEOUT
                    )
                } else {
                    Logger.i(tag, "outcome OK alias=${outcome.remoteAlias} telemetry=${outcome.telemetry}")
                    val (contact, session) = confirmUseCase.confirm(outcome)
                    _state.value = PairingState.Verified(contact, session, sas = outcome.sas)
                }
            } catch (err: Throwable) {
                Logger.e(tag, "excepción en start", err)
                val msg = err.message?.lowercase().orEmpty()
                _state.value = if ("permission" in msg || "record_audio" in msg || "denied" in msg) {
                    PairingState.Failed(PairingFailure.PERMISSION_DENIED)
                } else {
                    PairingState.Failed(PairingFailure.UNKNOWN)
                }
            }
        }
    }

    fun retryFromFailure() {
        Logger.i(tag, "retryFromFailure (estado previo=${_state.value::class.simpleName})")
        cancelJob()
        val previous = transport
        transport = null
        _state.value = PairingState.Idle
        screenModelScope.launch {
            runCatching { previous?.stop() }
            start()
        }
    }

    fun cancel() {
        Logger.i(tag, "cancel (estado=${_state.value::class.simpleName})")
        reset()
    }

    fun reset() {
        cancelJob()
        val previous = transport
        transport = null
        _state.value = PairingState.Idle
        screenModelScope.launch { runCatching { previous?.stop() } }
    }

    private fun cancelJob() {
        listenJob?.cancel()
        listenJob = null
    }

    override fun onDispose() {
        cancelJob()
        super.onDispose()
    }
}
