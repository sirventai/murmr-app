package uoc.edu.psirvent.murmr.audio.ggwave

actual class GgwaveCodec actual constructor(protocol: GgwaveProtocol) {

    init {
        error("GgwaveCodec no disponible en JVM. Usa Android, iOS o LoopbackGgwaveTransport en tests.")
    }

    actual fun encode(payload: ByteArray, volume: Int): ShortArray =
        error("GgwaveCodec stub JVM")

    actual fun decode(samples: ShortArray): ByteArray? =
        error("GgwaveCodec stub JVM")

    actual fun close() {}
}
