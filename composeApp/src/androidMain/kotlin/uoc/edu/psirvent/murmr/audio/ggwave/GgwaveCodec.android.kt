package uoc.edu.psirvent.murmr.audio.ggwave

actual class GgwaveCodec actual constructor(
    private val protocol: GgwaveProtocol,
) {

    private var handle: Int = -1

    init {
        loadLibrary()
        handle = nativeInit(protocol.id)
        require(handle >= 0) { "ggwave_init falló (handle=$handle)" }
    }

    actual fun encode(payload: ByteArray, volume: Int): ShortArray {
        check(handle >= 0) { "GgwaveCodec ya cerrado" }
        return nativeEncode(handle, payload, protocol.id, volume)
            ?: error("ggwave_encode devolvió null (payload size=${payload.size})")
    }

    actual fun decode(samples: ShortArray): ByteArray? {
        check(handle >= 0) { "GgwaveCodec ya cerrado" }
        return nativeDecode(handle, samples)
    }

    actual fun close() {
        if (handle >= 0) {
            nativeClose(handle)
            handle = -1
        }
    }

    private external fun nativeInit(protocolId: Int): Int
    private external fun nativeEncode(
        handle: Int,
        payload: ByteArray,
        protocolId: Int,
        volume: Int,
    ): ShortArray?
    private external fun nativeDecode(handle: Int, samples: ShortArray): ByteArray?
    private external fun nativeClose(handle: Int)

    companion object {
        @Volatile private var loaded = false

        @Synchronized
        private fun loadLibrary() {
            if (loaded) return
            System.loadLibrary("ggwave_native")
            loaded = true
        }
    }
}
