package uoc.edu.psirvent.murmr.data.network

import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uoc.edu.psirvent.murmr.data.repository.UserRepository
import uoc.edu.psirvent.murmr.domain.usecase.ReceiveDecryptMessageUseCase
import uoc.edu.psirvent.murmr.notifications.NotificationPermissionRegistry
import uoc.edu.psirvent.murmr.util.Logger

enum class ConnectionState { Disconnected, Connecting, Connected }

class AppSession(
    private val userRepository: UserRepository,
    private val relayApi: RelayApi,
    private val relayWebSocket: RelayWebSocket,
    private val tokenStore: DeviceTokenStore,
    private val receiveDecryptMessage: ReceiveDecryptMessageUseCase,
    private val outboxWorker: OutboxWorker? = null,
    private val relayConfigStore: RelayConfigStore? = null,
) {
    private val tag = "AppSession"

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var job: Job? = null
    private var listenJob: Job? = null
    private val startMutex = Mutex()

    private var sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Suppress("UNUSED_PARAMETER")
    suspend fun start(scope: CoroutineScope? = null): Unit = startMutex.withLock {
        if (job?.isActive == true) return@withLock
        Logger.i(tag, "Iniciando sesion de aplicacion...")
        job = sessionScope.launch {
            val user = userRepository.observeLocalUser().filterNotNull().first()
            Logger.i(tag, "User local detectado: deviceId=${user.deviceId} alias=${user.alias}")
            ensureRegistered(user.deviceId, user.alias, user.publicKey)
            connectAndListen(sessionScope, user.deviceId)
        }
        relayConfigStore?.let { store ->
            sessionScope.launch {
                var lastSeen = store.current().baseUrl
                store.flow.collect { cfg ->
                    if (cfg.baseUrl == lastSeen) return@collect
                    Logger.i(tag, "Cambio de URL relay: $lastSeen -> ${cfg.baseUrl}, reconectando")
                    lastSeen = cfg.baseUrl
                    runCatching { tokenStore.clear() }
                    relayWebSocket.disconnect()
                    listenJob?.cancel()
                    val u = userRepository.getLocalUser() ?: return@collect
                    ensureRegistered(u.deviceId, u.alias, u.publicKey)
                    connectAndListen(sessionScope, u.deviceId)
                }
            }
        }
    }

    private suspend fun ensureRegistered(deviceId: String, alias: String, publicKey: ByteArray) {

        val existing = tokenStore.get()
        if (existing != null && existing != deviceId) {
            Logger.w(tag, "Token persistido obsoleto (token=$existing != deviceId=$deviceId), limpiando")
            tokenStore.put(deviceId)
            NotificationPermissionRegistry.requestIfNeeded()
            return
        }
        if (existing != null) {
            Logger.i(tag, "Token presente, saltando registro")
            NotificationPermissionRegistry.requestIfNeeded()
            return
        }
        try {
            Logger.i(tag, "Token ausente, registrando contra relay...")
            val resp = relayApi.registerDevice(
                RegisterDeviceRequest(
                    deviceId = deviceId,
                    publicKeyB64 = publicKey.encodeBase64(),
                    alias = alias,
                ),
            )
            tokenStore.put(resp.token)
            Logger.i(tag, "Registro OK, token persistido")
            NotificationPermissionRegistry.requestIfNeeded()
        } catch (t: Throwable) {
            _lastError.value = "No se pudo registrar contra el relay: ${t.message}"
            Logger.w(tag, "Registro contra relay fallo, se reintentara en proximo arranque", t)
        }
    }

    private fun connectAndListen(scope: CoroutineScope, deviceId: String) {
        val token = tokenStore.get() ?: deviceId
        Logger.i(tag, "Abriendo WS persistente al relay para deviceId=$deviceId")
        _state.value = ConnectionState.Connecting

        scope.launch {
            runCatching {
                val pending = relayApi.fetchInbox(token = token, deviceId = deviceId)
                if (pending.isNotEmpty()) {
                    Logger.i(tag, "Inbox pull: ${pending.size} mensajes pendientes")
                    pending.forEach { runCatching { receiveDecryptMessage(it) } }
                }
            }.onFailure { Logger.w(tag, "fetchInbox fallo: ${it.message}") }
        }

        relayWebSocket.connect(scope, deviceId, token)
        listenJob?.cancel()
        listenJob = scope.launch {
            relayWebSocket.incoming.collect { envelope ->
                Logger.i(tag, "Envelope entrante: ${envelope.senderDeviceId} -> ${envelope.recipientDeviceId} (${envelope.ciphertextB64.length}B b64)")

                scope.launch { runCatching { receiveDecryptMessage(envelope) } }
            }
        }
        outboxWorker?.start(scope)

        scope.launch {
            relayWebSocket.connected.collect { open ->
                _state.value = if (open) ConnectionState.Connected else ConnectionState.Connecting
            }
        }
    }

    fun stop() {
        listenJob?.cancel(); listenJob = null
        job?.cancel(); job = null
        relayWebSocket.disconnect()
        sessionScope.cancel()
        sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        _state.value = ConnectionState.Disconnected
    }

    fun pauseNetwork() {
        if (job?.isActive != true) return
        Logger.i(tag, "pauseNetwork: cerrando WS por background")
        relayWebSocket.disconnect()
        _state.value = ConnectionState.Disconnected
    }

    fun resumeNetwork() {
        if (job?.isActive != true) return
        sessionScope.launch {
            val user = userRepository.getLocalUser() ?: return@launch
            val token = tokenStore.get() ?: user.deviceId
            Logger.i(tag, "resumeNetwork: reabriendo WS y drenando buzon")
            _state.value = ConnectionState.Connecting
            runCatching {
                val pending = relayApi.fetchInbox(token = token, deviceId = user.deviceId)
                if (pending.isNotEmpty()) {
                    Logger.i(tag, "Inbox pull on resume: ${pending.size} mensajes pendientes")
                    pending.forEach { runCatching { receiveDecryptMessage(it) } }
                }
            }.onFailure { Logger.w(tag, "fetchInbox on resume fallo: ${it.message}") }
            relayWebSocket.connect(sessionScope, user.deviceId, token)
        }
    }

    fun observeState(): Flow<ConnectionState> = state
}
