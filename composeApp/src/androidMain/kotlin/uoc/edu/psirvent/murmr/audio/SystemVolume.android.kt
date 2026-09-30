package uoc.edu.psirvent.murmr.audio

import android.content.Context
import android.media.AudioManager
import org.koin.mp.KoinPlatform
import uoc.edu.psirvent.murmr.util.Logger

actual class SystemVolume actual constructor() {

    private val tag = "SystemVolume"

    private fun manager(): AudioManager? {
        val ctx = runCatching { KoinPlatform.getKoin().get<Context>() }.getOrNull() ?: return null
        return ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    actual fun currentFraction(): Float {
        val am = manager() ?: return 1.0f
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return cur.toFloat() / max.toFloat()
    }

    actual fun tryRaiseToMax(): Boolean {
        val am = manager() ?: return false
        return runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            am.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
            Logger.i(tag, "tryRaiseToMax: subido a $max (max stream)")
            true
        }.onFailure {
            Logger.w(tag, "tryRaiseToMax falló: ${it.message}")
        }.getOrDefault(false)
    }
}
