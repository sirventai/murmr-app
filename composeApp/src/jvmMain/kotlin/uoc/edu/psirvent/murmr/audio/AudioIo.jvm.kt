package uoc.edu.psirvent.murmr.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class AudioPlayer actual constructor() {

    private var line: SourceDataLine? = null

    actual suspend fun play(samples: ShortArray, sampleRateHz: Int) = withContext(Dispatchers.IO) {
        val format = AudioFormat(sampleRateHz.toFloat(), 16, 1, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, format)
        val mixer = AudioDeviceRegistry.resolveOutputMixer()
        val l = (mixer?.getLine(info) ?: AudioSystem.getLine(info)) as SourceDataLine
        line = l
        try {
            l.open(format)
            l.start()
            val bytes = samplesToBytesLE(samples)
            var written = 0
            while (written < bytes.size) {
                val chunk = minOf(bytes.size - written, 4_096)
                val n = l.write(bytes, written, chunk)
                if (n <= 0) break
                written += n
            }
            l.drain()
            l.stop()
        } finally {
            runCatching { l.close() }
            line = null
        }
    }

    actual fun release() {
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
    }

    private fun samplesToBytesLE(samples: ShortArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        return out
    }
}

actual class AudioCapture actual constructor() {

    @Volatile private var capturing = false
    private var line: TargetDataLine? = null

    actual fun start(sampleRateHz: Int, onChunk: (ShortArray) -> Unit) {
        if (capturing) return
        val format = AudioFormat(sampleRateHz.toFloat(), 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val mixer = AudioDeviceRegistry.resolveInputMixer()
        val l = (mixer?.getLine(info) ?: AudioSystem.getLine(info)) as TargetDataLine
        line = l
        l.open(format)
        l.start()
        capturing = true
        thread(name = "MurmrAudioCapture", isDaemon = true) {
            val byteBuf = ByteArray(4_096)
            while (capturing) {
                val n = l.read(byteBuf, 0, byteBuf.size)
                if (n > 0) onChunk(bytesToSamplesLE(byteBuf, n))
            }
        }
    }

    actual fun stop() {
        capturing = false
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
    }

    private fun bytesToSamplesLE(buf: ByteArray, length: Int): ShortArray {
        val out = ShortArray(length / 2)
        for (i in out.indices) {
            val lo = buf[i * 2].toInt() and 0xFF
            val hi = buf[i * 2 + 1].toInt()
            out[i] = ((hi shl 8) or lo).toShort()
        }
        return out
    }
}
