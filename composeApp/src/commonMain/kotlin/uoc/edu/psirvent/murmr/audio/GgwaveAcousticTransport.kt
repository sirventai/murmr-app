package uoc.edu.psirvent.murmr.audio

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uoc.edu.psirvent.murmr.audio.ggwave.GgwaveCodec
import uoc.edu.psirvent.murmr.audio.ggwave.GgwaveProtocol
import uoc.edu.psirvent.murmr.util.Logger

private const val SAMPLE_RATE_HZ = 48_000
private const val PRE_TX_GAP_MS = 100L
private const val POST_TX_COOLDOWN_MS = 80L

private const val MIN_DECODE_CHUNK = 16_384

class GgwaveAcousticTransport(
    private val player: AudioPlayer = AudioPlayer(),
    private val capture: AudioCapture = AudioCapture(),
    private val micPermission: MicPermission = MicPermission(),
    private val protocols: List<GgwaveProtocol> = listOf(GgwaveProtocol.ULTRASOUND_NORMAL),
    private val volume: Int = 100,
    private val payloadRepeats: Int = 2,
    private val applyPreEmphasis: Boolean = true,
) : GgwaveTransport, CapturingTransport {

    constructor(
        protocol: GgwaveProtocol,
        volume: Int = 100,
        payloadRepeats: Int = 2,
        applyPreEmphasis: Boolean = true,
    ) : this(
        protocols = listOf(protocol),
        volume = volume,
        payloadRepeats = payloadRepeats,
        applyPreEmphasis = applyPreEmphasis,
    )

    private val tag = "GgwaveAcoustic"

    private val incoming = MutableSharedFlow<ByteArray>(
        replay = 1,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val ampFlow = MutableSharedFlow<List<Float>>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val active = MutableStateFlow(false)
    @Volatile private var muted = false
    private var totalChunks = 0L
    private var totalSamples = 0L
    private var captureScope: Job? = null

    init {
        require(protocols.isNotEmpty()) { "Se necesita al menos un protocolo" }
        require(volume in 1..100) { "Volume debe estar en 1..100" }
        require(payloadRepeats in 1..5) { "payloadRepeats debe estar en 1..5" }
    }

    private val codecsTx: List<GgwaveCodec> by lazy {
        protocols.map {
            Logger.i(tag, "Inicializando ggwave TX codec con protocolo=${it.name} (id=${it.id})")
            GgwaveCodec(it)
        }
    }
    private val codecsRx: List<GgwaveCodec> by lazy {
        protocols.map {
            Logger.i(tag, "Inicializando ggwave RX codec con protocolo=${it.name} (id=${it.id})")
            GgwaveCodec(it)
        }
    }

    private val recentPayloads = ArrayDeque<RecentPayload>()
    private val recentMutex = kotlinx.coroutines.sync.Mutex()

    private data class RecentPayload(val bytes: ByteArray, val seenAtMs: Long)

    private val needsPreEmphasis: Boolean by lazy {
        applyPreEmphasis && protocols.any {
            it == GgwaveProtocol.ULTRASOUND_NORMAL ||
                it == GgwaveProtocol.ULTRASOUND_FAST ||
                it == GgwaveProtocol.ULTRASOUND_FASTEST
        }
    }
    private val preEmphasisFilter: PreEmphasisFilter? by lazy {
        if (needsPreEmphasis) PreEmphasisFilter() else null
    }

    private val rxBuffer = ShortArray(MIN_DECODE_CHUNK * 2)
    private var rxBufferLen = 0

    override suspend fun startCapture(scope: CoroutineScope) {
        if (active.value) {
            Logger.i(tag, "startCapture: ya activo, ignorando")
            return
        }
        Logger.i(tag, "startCapture: solicitando permiso de micrófono...")
        if (!micPermission.ensure()) {
            Logger.e(tag, "startCapture: PERMISO DENEGADO")
            error("Permission denied: micrófono. RECORD_AUDIO requerido para emparejamiento acústico.")
        }
        Logger.i(
            tag,
            "startCapture: permiso OK, iniciando AudioCapture @${SAMPLE_RATE_HZ}Hz, " +
                "protocolos=${protocols.joinToString(",") { it.name }}, " +
                "repeats=$payloadRepeats, preEmphasis=$applyPreEmphasis",
        )
        active.value = true
        totalChunks = 0
        totalSamples = 0
        rxBufferLen = 0
        capture.start(SAMPLE_RATE_HZ) { chunk ->
            totalChunks++
            totalSamples += chunk.size
            if (totalChunks % 50L == 0L) {
                var peak = 0
                for (s in chunk) {
                    val a = if (s < 0) -s.toInt() else s.toInt()
                    if (a > peak) peak = a
                }
                Logger.i(tag, "captura tick chunks=$totalChunks samples=$totalSamples peak=$peak muted=$muted bufLen=$rxBufferLen")
            }
            if (muted) {

                rxBufferLen = 0
                return@start
            }
            var remaining = chunk
            while (remaining.isNotEmpty()) {
                val space = rxBuffer.size - rxBufferLen
                val toCopy = minOf(space, remaining.size)
                remaining.copyInto(rxBuffer, rxBufferLen, 0, toCopy)
                rxBufferLen += toCopy
                if (rxBufferLen >= MIN_DECODE_CHUNK) {
                    val batch = rxBuffer.copyOf(rxBufferLen)
                    rxBufferLen = 0
                    decodeBatch(batch)
                }
                remaining = if (toCopy < remaining.size) {
                    remaining.copyOfRange(toCopy, remaining.size)
                } else {
                    shortArrayOf()
                }
            }
        }
        captureScope = scope.launch(Dispatchers.Default) {
            while (active.value) { delay(50) }
        }
        Logger.i(tag, "startCapture: AudioCapture corriendo")
    }

    private fun decodeBatch(batch: ShortArray) {
        for ((idx, codec) in codecsRx.withIndex()) {
            val payload = runCatching { codec.decode(batch) }
                .onFailure { Logger.w(tag, "ggwave[${protocols[idx].name}] decode error: ${it.message}") }
                .getOrNull() ?: continue
            if (payload.isEmpty()) continue
            if (isDuplicateRecently(payload)) {
                Logger.i(tag, "FRAME duplicado (repeat-coding) descartado en ${protocols[idx].name}")
                continue
            }
            val head = payload.take(8).joinToString(" ") {
                val v = it.toInt() and 0xFF
                v.toString(16).padStart(2, '0')
            }
            val ascii = payload.take(8)
                .map { b -> b.toInt().toChar().takeIf { it.code in 32..126 } ?: '.' }
                .joinToString("")
            Logger.i(tag, "FRAME OK ${payload.size}B en ${protocols[idx].name} head=$head ('$ascii')")
            incoming.tryEmit(payload)
        }
    }

    override suspend fun send(payload: ByteArray) {

        val effectiveVolume = if (needsPreEmphasis) minOf(volume, 60) else volume
        val pcmFragments = codecsTx.mapIndexed { idx, codec ->
            codec.encode(payload, effectiveVolume).also {
                Logger.i(
                    tag,
                    "send: encode protocolo=${protocols[idx].name} payload=${payload.size}B -> PCM=${it.size} samples (vol=$effectiveVolume)",
                )
            }
        }
        val concatenated = concatPcm(pcmFragments)
        preEmphasisFilter?.let {
            it.applyInPlace(concatenated)
            Logger.i(tag, "send: pre-emphasis aplicado (banda ULTRASOUND)")
        }
        val durationMs = concatenated.size * 1000L / SAMPLE_RATE_HZ
        Logger.i(
            tag,
            "send: total payload=${payload.size}B -> PCM=${concatenated.size} samples " +
                "(~${durationMs}ms) x ${payloadRepeats}repeats",
        )
        muted = true
        try {
            delay(PRE_TX_GAP_MS)
            for (rep in 0 until payloadRepeats) {
                if (rep > 0) {
                    Logger.i(tag, "send: repeticion ${rep + 1}/$payloadRepeats")

                    delay(150)
                }
                player.play(concatenated, SAMPLE_RATE_HZ)
            }
            delay(POST_TX_COOLDOWN_MS)
        } finally {
            muted = false
        }
        Logger.i(tag, "send: TX+cooldown terminado")
    }

    private fun concatPcm(fragments: List<ShortArray>): ShortArray {
        if (fragments.size == 1) return fragments[0]
        val total = fragments.sumOf { it.size } + (fragments.size - 1) * INTER_PROTOCOL_SILENCE_SAMPLES
        val out = ShortArray(total)
        var pos = 0
        for ((i, frag) in fragments.withIndex()) {
            frag.copyInto(out, pos)
            pos += frag.size
            if (i < fragments.size - 1) {

                pos += INTER_PROTOCOL_SILENCE_SAMPLES
            }
        }
        return out
    }

    private fun isDuplicateRecently(payload: ByteArray): Boolean {
        val now = nowMs()
        val it = recentPayloads.iterator()
        var found = false
        while (it.hasNext()) {
            val r = it.next()
            if (now - r.seenAtMs > DEDUP_WINDOW_MS) {
                it.remove()
                continue
            }
            if (!found && payload.contentEquals(r.bytes)) found = true
        }
        if (!found) {
            recentPayloads.addLast(RecentPayload(payload, now))
            while (recentPayloads.size > 32) recentPayloads.removeFirst()
        }
        return found
    }

    private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    override fun listen(): Flow<ByteArray> = incoming.asSharedFlow()
    override fun amplitudes(): Flow<List<Float>> = ampFlow.asSharedFlow()

    override suspend fun stop() {
        Logger.i(tag, "stop: total chunks=$totalChunks samples=$totalSamples")
        active.value = false
        rxBufferLen = 0
        captureScope?.cancel(); captureScope = null
        capture.stop()
        player.release()
        codecsTx.forEach { runCatching { it.close() } }
        codecsRx.forEach { runCatching { it.close() } }
    }

    val isActive: kotlinx.coroutines.flow.StateFlow<Boolean> get() = active.asStateFlow()

    companion object {

        private const val INTER_PROTOCOL_SILENCE_SAMPLES = 4_800
        private const val DEDUP_WINDOW_MS = 6_000L
    }
}
