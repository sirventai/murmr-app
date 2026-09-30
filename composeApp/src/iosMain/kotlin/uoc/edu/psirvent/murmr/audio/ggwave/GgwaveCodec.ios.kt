package uoc.edu.psirvent.murmr.audio.ggwave

import ggwave.GGWAVE_OPERATING_MODE_RX_AND_TX
import ggwave.ggwave_Instance
import ggwave.ggwave_Parameters
import ggwave.ggwave_ProtocolId
import ggwave.ggwave_SampleFormat
import ggwave.ggwave_encode
import ggwave.ggwave_free
import ggwave.ggwave_init
import ggwave.ggwave_ndecode
import ggwave.ggwave_rxToggleProtocol
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class GgwaveCodec actual constructor(
    private val protocol: GgwaveProtocol,
) {

    private val handle: ggwave_Instance

    init {

        handle = memScoped {
            val params = alloc<ggwave_Parameters>()
            params.payloadLength = -1
            params.sampleRateInp = SAMPLE_RATE
            params.sampleRateOut = SAMPLE_RATE
            params.sampleRate = SAMPLE_RATE
            params.samplesPerFrame = SAMPLES_PER_FRAME
            params.soundMarkerThreshold = SOUND_MARKER_THRESHOLD
            params.sampleFormatInp = ggwave_SampleFormat.GGWAVE_SAMPLE_FORMAT_I16
            params.sampleFormatOut = ggwave_SampleFormat.GGWAVE_SAMPLE_FORMAT_I16
            params.operatingMode = GGWAVE_OPERATING_MODE_RX_AND_TX.toInt()
            ggwave_init(params.readValue())
        }
        require(handle >= 0) { "ggwave_init falló (handle=$handle)" }

        for (i in 0 until PROTOCOL_COUNT) {
            ggwave_rxToggleProtocol(toProtocolId(i), 0)
        }
        ggwave_rxToggleProtocol(toProtocolId(protocol.id), 1)
    }

    actual fun encode(payload: ByteArray, volume: Int): ShortArray {
        check(handle >= 0) { "GgwaveCodec ya cerrado" }
        if (payload.isEmpty()) error("encode: payload vacío")
        return memScoped {
            payload.usePinned { pinnedPayload ->
                val nBytes = ggwave_encode(
                    handle,
                    pinnedPayload.addressOf(0),
                    payload.size,
                    toProtocolId(protocol.id),
                    volume,
                    null,
                    1,
                )
                if (nBytes <= 0) error("ggwave_encode query devolvió $nBytes")
                val numSamples = nBytes / Short.SIZE_BYTES
                val waveform = allocArray<ShortVar>(numSamples)
                val produced = ggwave_encode(
                    handle,
                    pinnedPayload.addressOf(0),
                    payload.size,
                    toProtocolId(protocol.id),
                    volume,
                    waveform,
                    0,
                )
                if (produced <= 0) error("ggwave_encode devolvió $produced")
                val producedSamples = produced / Short.SIZE_BYTES
                val out = ShortArray(producedSamples)
                out.usePinned { pinnedOut ->
                    memcpy(
                        pinnedOut.addressOf(0),
                        waveform,
                        (producedSamples * Short.SIZE_BYTES).toULong(),
                    )
                }
                out
            }
        }
    }

    actual fun decode(samples: ShortArray): ByteArray? {
        check(handle >= 0) { "GgwaveCodec ya cerrado" }
        if (samples.isEmpty()) return null
        return memScoped {
            val payloadBuf = allocArray<ByteVar>(MAX_PAYLOAD)
            samples.usePinned { pinnedSamples ->
                val decoded = ggwave_ndecode(
                    handle,
                    pinnedSamples.addressOf(0),
                    samples.size * Short.SIZE_BYTES,
                    payloadBuf,
                    MAX_PAYLOAD,
                )
                if (decoded <= 0) null else payloadBuf.readBytes(decoded)
            }
        }
    }

    actual fun close() {
        if (handle >= 0) ggwave_free(handle)
    }

    private fun toProtocolId(id: Int): ggwave_ProtocolId =
        ggwave_ProtocolId.values().first { it.value.toInt() == id }

    companion object {
        private const val SAMPLE_RATE: Float = 48_000.0f
        private const val MAX_PAYLOAD: Int = 256

        private const val PROTOCOL_COUNT: Int = 22

        private const val SAMPLES_PER_FRAME: Int = 1024
        private const val SOUND_MARKER_THRESHOLD: Float = 3.0f
    }
}
