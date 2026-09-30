package uoc.edu.psirvent.murmr.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionPortOverrideSpeaker
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.posix.memcpy
import uoc.edu.psirvent.murmr.util.Logger

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private object SharedAudio {
    private val tag = "AudioIo"
    private var sessionConfigured = false

    val standardFormat: AVAudioFormat = AVAudioFormat(
        commonFormat = AVAudioPCMFormatFloat32,
        sampleRate = 48000.0,
        channels = 1u,
        interleaved = false,
    )

    val playerNode: AVAudioPlayerNode = AVAudioPlayerNode()

    val engine: AVAudioEngine by lazy {
        AVAudioEngine().also { e ->
            e.attachNode(playerNode)
            e.connect(playerNode, to = e.mainMixerNode, format = standardFormat)
            playerNode.volume = 1.0f
            e.mainMixerNode.outputVolume = 1.0f

            val warmFmt = e.inputNode.outputFormatForBus(0u)
            Logger.i(tag, "engine inputNode warmup: sr=${warmFmt.sampleRate} ch=${warmFmt.channelCount}")

            memScoped {
                val errPtr = alloc<ObjCObjectVar<NSError?>>()
                e.inputNode.setVoiceProcessingEnabled(false, error = errPtr.ptr)
                errPtr.value?.let { Logger.w(tag, "setVoiceProcessingEnabled(false): ${it.localizedDescription}") }
            }
        }
    }

    fun ensureSession() {
        if (sessionConfigured) return
        val session = AVAudioSession.sharedInstance()
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(
                category = AVAudioSessionCategoryPlayAndRecord,
                withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
                error = errPtr.ptr,
            )
            errPtr.value?.let { Logger.w(tag, "setCategory: ${it.localizedDescription}") }
            errPtr.value = null

            session.setMode(AVAudioSessionModeMeasurement, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "setMode: ${it.localizedDescription}") }
            errPtr.value = null
            session.setActive(active = true, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "setActive: ${it.localizedDescription}") }
            errPtr.value = null
            session.overrideOutputAudioPort(AVAudioSessionPortOverrideSpeaker, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "overrideOutput.speaker: ${it.localizedDescription}") }
        }
        sessionConfigured = true
    }

    fun startEngineIfNeeded() {
        if (engine.running) return
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            engine.startAndReturnError(errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "engine.start: ${it.localizedDescription}") }
        }
        Logger.i(tag, "engine started, running=${engine.running}")
    }

    fun stopEngine() {
        if (engine.running) {
            engine.stop()
            Logger.i(tag, "engine stopped")
        }
    }

    fun fullReset() {
        runCatching { if (engine.running) engine.stop() }
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance().setActive(active = false, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "fullReset.setActive(false): ${it.localizedDescription}") }
        }
        sessionConfigured = false
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AudioPlayer actual constructor() {

    private val tag = "AudioPlayer"

    actual suspend fun play(samples: ShortArray, sampleRateHz: Int) {
        SharedAudio.ensureSession()
        if (sampleRateHz != 48000) {
            Logger.w(tag, "sampleRateHz=$sampleRateHz pedido pero engine fijo a 48000 Hz")
        }
        SharedAudio.startEngineIfNeeded()
        val frames = samples.size.toUInt()
        val buffer = AVAudioPCMBuffer(SharedAudio.standardFormat, frameCapacity = frames)
        buffer.frameLength = frames
        val channels = buffer.floatChannelData ?: error("AudioPlayer: floatChannelData null")
        val ch0 = channels[0] ?: error("AudioPlayer: canal 0 null")
        val floats = FloatArray(samples.size) { samples[it].toFloat() / 32768.0f }
        floats.usePinned { pinned ->
            memcpy(ch0, pinned.addressOf(0), (samples.size * 4).toULong())
        }
        val done = CompletableDeferred<Unit>()
        SharedAudio.playerNode.scheduleBuffer(buffer) {
            done.complete(Unit)
        }
        if (!SharedAudio.playerNode.playing) SharedAudio.playerNode.play()
        var peak = 0
        for (s in samples) {
            val a = if (s.toInt() < 0) -s.toInt() else s.toInt()
            if (a > peak) peak = a
        }
        Logger.i(
            tag,
            "play scheduled frames=$frames sr=$sampleRateHz peak=$peak engine.running=${SharedAudio.engine.running}",
        )

        val durationMs = (samples.size.toLong() * 1000L / sampleRateHz)
        delay(durationMs)
        delay(30)
        withTimeoutOrNull(120) { done.await() }
    }

    actual fun release() {

        runCatching { SharedAudio.playerNode.stop() }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AudioCapture actual constructor() {

    private val tag = "AudioCapture"
    private var tapInstalled = false

    actual fun start(sampleRateHz: Int, onChunk: (ShortArray) -> Unit) {
        SharedAudio.ensureSession()
        val input = SharedAudio.engine.inputNode
        var hwFormat = input.outputFormatForBus(0u)
        Logger.i(
            tag,
            "input HW format: sr=${hwFormat.sampleRate} ch=${hwFormat.channelCount} (esperado=$sampleRateHz)",
        )

        var attempt = 0
        while ((hwFormat.sampleRate <= 0.0 || hwFormat.channelCount.toInt() <= 0) && attempt < 2) {
            attempt++
            if (attempt == 1) {
                Logger.w(tag, "HW format inválido, recovery #1: stop + restart engine")
                runCatching { SharedAudio.stopEngine() }
                SharedAudio.startEngineIfNeeded()
            } else {
                Logger.w(tag, "HW format aún inválido, recovery #2: full session reset")
                SharedAudio.fullReset()
                SharedAudio.ensureSession()
                SharedAudio.startEngineIfNeeded()
            }
            hwFormat = input.outputFormatForBus(0u)
            Logger.i(
                tag,
                "HW format tras recovery #$attempt: sr=${hwFormat.sampleRate} ch=${hwFormat.channelCount}",
            )
        }
        if (hwFormat.sampleRate <= 0.0 || hwFormat.channelCount.toInt() <= 0) {
            Logger.e(tag, "HW format sigue inválido tras todos los recoveries, abortando captura")
            error("AudioCapture: HW format inválido (sr=${hwFormat.sampleRate} ch=${hwFormat.channelCount}). El AVAudioEngine no negocia input con HW. Reinicia la app.")
        }
        if (hwFormat.sampleRate.toInt() != sampleRateHz) {
            Logger.w(
                tag,
                "sample rate HW=${hwFormat.sampleRate} != target=$sampleRateHz; el FSK puede degradarse",
            )
        }
        if (tapInstalled) runCatching { input.removeTapOnBus(0u) }
        input.installTapOnBus(
            bus = 0u,

            bufferSize = 8192u,
            format = hwFormat,
        ) { buffer, _ ->
            buffer ?: return@installTapOnBus
            val frames = buffer.frameLength.toInt()
            if (frames <= 0) return@installTapOnBus
            val channels = buffer.floatChannelData
            if (channels == null) {
                Logger.w(tag, "tap: floatChannelData null")
                return@installTapOnBus
            }
            val ch0 = channels[0] ?: return@installTapOnBus
            val floats = FloatArray(frames)
            floats.usePinned { pinned ->
                memcpy(pinned.addressOf(0), ch0, (frames * 4).toULong())
            }
            val out = ShortArray(frames)
            for (i in 0 until frames) {
                val f = floats[i]
                val clamped = if (f > 1.0f) 1.0f else if (f < -1.0f) -1.0f else f
                out[i] = (clamped * 32767.0f).toInt().toShort()
            }
            onChunk(out)
        }
        tapInstalled = true
        SharedAudio.startEngineIfNeeded()
        Logger.i(tag, "engine arrancado")
    }

    actual fun stop() {
        if (tapInstalled) {
            runCatching { SharedAudio.engine.inputNode.removeTapOnBus(0u) }
            tapInstalled = false
        }

        runCatching { SharedAudio.stopEngine() }
    }
}
