package uoc.edu.psirvent.murmr.audio

import kotlinx.coroutines.flow.Flow

interface GgwaveTransport {

    suspend fun send(payload: ByteArray)

    fun listen(): Flow<ByteArray>

    fun amplitudes(): Flow<List<Float>>

    suspend fun stop()
}

interface CapturingTransport {
    suspend fun startCapture(scope: kotlinx.coroutines.CoroutineScope)
}
