package uoc.edu.psirvent.murmr.audio.ggwave

expect class GgwaveCodec(protocol: GgwaveProtocol = GgwaveProtocol.ULTRASOUND_FAST) {

    fun encode(payload: ByteArray, volume: Int = 50): ShortArray

    fun decode(samples: ShortArray): ByteArray?

    fun close()
}

enum class GgwaveProtocol(val id: Int) {
    AUDIBLE_NORMAL(0),
    AUDIBLE_FAST(1),
    AUDIBLE_FASTEST(2),
    ULTRASOUND_NORMAL(3),
    ULTRASOUND_FAST(4),
    ULTRASOUND_FASTEST(5),
    DT_NORMAL(6),
}
