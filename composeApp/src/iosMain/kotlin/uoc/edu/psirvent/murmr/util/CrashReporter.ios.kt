package uoc.edu.psirvent.murmr.util

import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalNativeApi
import kotlin.time.Clock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual object CrashReporter {

    @Volatile private var installed = false

    @OptIn(ExperimentalNativeApi::class)
    actual fun install() {
        if (installed) return
        installed = true
        setUnhandledExceptionHook { throwable ->
            runCatching { writeCrash(throwable) }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun writeCrash(throwable: Throwable) {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        ).firstOrNull() as? String ?: return
        val path = "$docs/crash.log"
        val now = Clock.System.now().toString()
        val text = "=== $now ===\n${throwable.stackTraceToString()}\n\n"
        val existing = NSString.stringWithContentsOfFile(
            path,
            NSUTF8StringEncoding,
            null,
        ) ?: ""
        val payload = NSString.create(string = existing + text)
        payload.writeToFile(path, true, NSUTF8StringEncoding, null)
    }
}
