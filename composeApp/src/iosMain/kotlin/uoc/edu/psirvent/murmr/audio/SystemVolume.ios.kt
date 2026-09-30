package uoc.edu.psirvent.murmr.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.valueForKey
import uoc.edu.psirvent.murmr.util.Logger

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SystemVolume actual constructor() {

    private val tag = "SystemVolume"
    private var sessionPrimed = false

    private fun primeSessionOnce() {
        if (sessionPrimed) return
        memScoped {
            val errPtr = alloc<ObjCObjectVar<NSError?>>()
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryAmbient, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "setCategory(ambient): ${it.localizedDescription}") }
            errPtr.value = null
            session.setActive(active = true, error = errPtr.ptr)
            errPtr.value?.let { Logger.w(tag, "setActive(true): ${it.localizedDescription}") }
        }
        sessionPrimed = true
    }

    actual fun currentFraction(): Float {
        primeSessionOnce()
        val raw = runCatching {
            AVAudioSession.sharedInstance().valueForKey("outputVolume")
        }.onFailure { Logger.w(tag, "valueForKey(outputVolume) falló: ${it.message}") }
            .getOrNull() ?: return 1.0f
        return runCatching { raw.toString().toFloat() }
            .getOrDefault(1.0f)
            .coerceIn(0.0f, 1.0f)
    }

    actual fun tryRaiseToMax(): Boolean = false
}
