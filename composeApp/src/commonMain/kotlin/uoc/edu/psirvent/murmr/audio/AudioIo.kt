package uoc.edu.psirvent.murmr.audio

expect class AudioPlayer() {

    suspend fun play(samples: ShortArray, sampleRateHz: Int)
    fun release()
}

expect class AudioCapture() {

    fun start(sampleRateHz: Int, onChunk: (ShortArray) -> Unit)
    fun stop()
}
