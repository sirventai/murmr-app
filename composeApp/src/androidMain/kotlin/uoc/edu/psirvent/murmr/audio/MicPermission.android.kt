package uoc.edu.psirvent.murmr.audio

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform
import uoc.edu.psirvent.murmr.util.Logger

actual class MicPermission actual constructor() {

    private val tag = "MicPermission"

    actual suspend fun ensure(): Boolean {
        val ctx = KoinPlatform.getKoin().get<Context>()
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        Logger.i(tag, "ensure() entry granted=$granted")
        if (granted) return true

        val launcher = MicPermissionRegistry.modernLauncher
        if (launcher == null) {
            Logger.w(tag, "Activity launcher no registrado, no podemos solicitar permiso")
            return false
        }
        return suspendCancellableCoroutine { cont ->
            MicPermissionRegistry.pendingContinuation = { result ->
                Logger.i(tag, "callback granted=$result")
                if (cont.isActive) cont.resume(result)
            }
            Logger.i(tag, "Lanzando diálogo RECORD_AUDIO...")
            launcher()
        }
    }
}

object MicPermissionRegistry {
    var activity: Activity? = null
    internal var modernLauncher: (() -> Unit)? = null
    internal var pendingContinuation: ((Boolean) -> Unit)? = null

    fun notifyGrantStatus(granted: Boolean) {
        val cb = pendingContinuation
        pendingContinuation = null
        cb?.invoke(granted)
    }
}
