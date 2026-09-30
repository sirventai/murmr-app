package uoc.edu.psirvent.murmr.util

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object CrashReporter {

    @Volatile private var installed = false

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
        val target = Paths.get(System.getProperty("java.io.tmpdir"), "murmr-crash.log")
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        Files.writeString(
            target,
            "=== $ts thread=${thread.name} ===\n$sw\n\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }
}
