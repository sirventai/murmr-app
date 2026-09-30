package uoc.edu.psirvent.murmr.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class LoopbackGgwaveTransport private constructor(
    private val sharedBus: MutableSharedFlow<ByteArray>,
    private val tag: String,
) : GgwaveTransport {

    private val ampScope = CoroutineScope(Dispatchers.Default)
    private var ampFlow = MutableSharedFlow<List<Float>>(replay = 1, extraBufferCapacity = 32)

    override suspend fun send(payload: ByteArray) {

        ampScope.launch {
            repeat(30) { i ->
                ampFlow.tryEmit(simAmplitudes(i))
                kotlinx.coroutines.delay(40)
            }
            ampFlow.tryEmit(List(32) { 0f })
        }
        sharedBus.emit(payload)
    }

    override fun listen(): Flow<ByteArray> = sharedBus.asSharedFlow()

    override fun amplitudes(): Flow<List<Float>> = flow {
        ampFlow.collect { emit(it) }
    }

    override suspend fun stop() {
        ampFlow.tryEmit(List(32) { 0f })
    }

    private fun simAmplitudes(t: Int): List<Float> = List(32) { i ->
        val n = i.toFloat() / 32f
        val s = (sin((t * 0.4f + n * 6f) * 2f * PI.toFloat()) * 0.4f) + 0.5f
        s.coerceIn(0.05f, 1f)
    }

    companion object {

        private val processBus = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)

        fun forProcess(tag: String = "loop"): LoopbackGgwaveTransport =
            LoopbackGgwaveTransport(processBus, tag)
    }
}
