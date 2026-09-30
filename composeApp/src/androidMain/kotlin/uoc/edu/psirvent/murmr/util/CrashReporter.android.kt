package uoc.edu.psirvent.murmr.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object CrashReporter {

    @Volatile private var installed = false
    @Volatile private var appContext: Context? = null

    fun bind(context: Context) {
        appContext = context.applicationContext
    }

    actual fun install() {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(thread: Thread, throwable: Throwable) {
        val ctx = appContext ?: return
        val target = File(ctx.filesDir, "crash.log")
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        target.appendText("=== $ts thread=${thread.name} ===\n$sw\n\n")
    }
}
