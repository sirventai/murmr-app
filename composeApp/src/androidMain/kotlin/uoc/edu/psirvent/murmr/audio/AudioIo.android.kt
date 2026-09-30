package uoc.edu.psirvent.murmr.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import uoc.edu.psirvent.murmr.util.Logger

actual class AudioPlayer actual constructor() {

    private var track: AudioTrack? = null

    actual suspend fun play(samples: ShortArray, sampleRateHz: Int) = withContext(Dispatchers.IO) {
        val tag = "AudioPlayer"
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBuf, samples.size * 2)
        var peak = 0
        for (s in samples) {
            val a = if (s.toInt() < 0) -s.toInt() else s.toInt()
            if (a > peak) peak = a
        }
        Logger.i(
            tag,
            "play: ${samples.size} samples @${sampleRateHz}Hz peak=$peak minBuf=$minBuf bufSize=$bufSize",
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.setVolume(AudioTrack.getMaxVolume())
        this@AudioPlayer.track = track
        try {
            track.play()
            var written = 0
            while (written < samples.size) {
                val chunk = minOf(samples.size - written, 4_096)
                val n = track.write(samples, written, chunk)
                if (n < 0) {
                    Logger.w(tag, "track.write devolvió $n, abortando (written=$written/${samples.size})")
                    break
                }
                written += n
            }
            Logger.i(tag, "play: escritos $written samples, drenando...")

            val durationMs = (written.toLong() * 1000L) / sampleRateHz.toLong()
            delay(durationMs + 100L)
            track.stop()
            Logger.i(tag, "play: stop OK tras drenar ~${durationMs}ms")
        } finally {
            runCatching { track.release() }
            this@AudioPlayer.track = null
        }
    }

    actual fun release() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }
}

actual class AudioCapture actual constructor() {

    private var record: AudioRecord? = null
    @Volatile private var capturing = false
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    @SuppressWarnings("MissingPermission")
    actual fun start(sampleRateHz: Int, onChunk: (ShortArray) -> Unit) {
        if (capturing) return
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBuf, sampleRateHz / 10 * 2)

        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
        )
        var r: AudioRecord? = null
        for (src in sources) {
            r = runCatching {
                AudioRecord(
                    src,
                    sampleRateHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            }.getOrNull()
            if (r != null) {
                Logger.i("AudioCapture", "AudioRecord OK source=$src")
                break
            } else {
                Logger.w("AudioCapture", "AudioRecord falló para source=$src")
            }
        }
        r ?: error("Permission denied: ningún AudioSource pudo inicializarse (¿RECORD_AUDIO concedido?)")
        record = r
        capturing = true

        disableAudioEffects(r.audioSessionId)
        r.startRecording()
        Logger.i("AudioCapture", "AudioRecord.startRecording() bufSize=$bufSize sampleRate=$sampleRateHz")
        thread(name = "MurmrAudioCapture", isDaemon = true) {
            val buffer = ShortArray(2_048)
            var iterations = 0L
            while (capturing) {
                val n = r.read(buffer, 0, buffer.size)
                if (n > 0) {
                    iterations++
                    if (iterations == 1L) {
                        var p = 0
                        for (i in 0 until n) {
                            val a = if (buffer[i].toInt() < 0) -buffer[i].toInt() else buffer[i].toInt()
                            if (a > p) p = a
                        }
                        Logger.i("AudioCapture", "primer chunk: $n samples peak=$p")
                    }
                    onChunk(buffer.copyOf(n))
                } else if (n < 0) {
                    Logger.w("AudioCapture", "AudioRecord.read devolvió $n (error)")
                }
            }
            Logger.i("AudioCapture", "thread terminado tras $iterations chunks")
        }
    }

    actual fun stop() {
        capturing = false
        runCatching { aec?.release() }; aec = null
        runCatching { ns?.release() }; ns = null
        runCatching { agc?.release() }; agc = null
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
    }

    private fun disableAudioEffects(sessionId: Int) {
        val tag = "AudioCapture"
        if (AcousticEchoCanceler.isAvailable()) {
            aec = runCatching {
                AcousticEchoCanceler.create(sessionId)?.also {
                    val r = it.setEnabled(false)
                    Logger.i(tag, "AEC.setEnabled(false) result=$r enabled=${it.enabled} (success=${r == AudioEffect.SUCCESS})")
                }
            }.onFailure { Logger.w(tag, "AEC disable falló: ${it.message}") }.getOrNull()
            if (aec == null) Logger.w(tag, "AEC: create() devolvió null pese a isAvailable=true")
        } else {
            Logger.w(tag, "AEC: NO DISPONIBLE en este dispositivo (HW puede aplicar AEC propietario)")
        }
        if (NoiseSuppressor.isAvailable()) {
            ns = runCatching {
                NoiseSuppressor.create(sessionId)?.also {
                    val r = it.setEnabled(false)
                    Logger.i(tag, "NS.setEnabled(false) result=$r enabled=${it.enabled} (success=${r == AudioEffect.SUCCESS})")
                }
            }.onFailure { Logger.w(tag, "NS disable falló: ${it.message}") }.getOrNull()
            if (ns == null) Logger.w(tag, "NS: create() devolvió null pese a isAvailable=true")
        } else {
            Logger.w(tag, "NS: NO DISPONIBLE en este dispositivo (HW puede aplicar NS propietario)")
        }
        if (AutomaticGainControl.isAvailable()) {
            agc = runCatching {
                AutomaticGainControl.create(sessionId)?.also {
                    val r = it.setEnabled(false)
                    Logger.i(tag, "AGC.setEnabled(false) result=$r enabled=${it.enabled} (success=${r == AudioEffect.SUCCESS})")
                }
            }.onFailure { Logger.w(tag, "AGC disable falló: ${it.message}") }.getOrNull()
            if (agc == null) Logger.w(tag, "AGC: create() devolvió null pese a isAvailable=true")
        } else {
            Logger.w(tag, "AGC: NO DISPONIBLE vía AudioEffect API (Samsung suele aplicarlo a nivel HW pese a UNPROCESSED)")
        }
    }
}
