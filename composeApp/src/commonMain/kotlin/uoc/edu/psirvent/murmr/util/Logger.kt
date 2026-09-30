package uoc.edu.psirvent.murmr.util

import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object Logger {

    @Volatile
    var enabled: Boolean = false

    fun i(tag: String, msg: String) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] $msg")
    }

    inline fun i(tag: String, msg: () -> String) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] ${msg()}")
    }

    fun w(tag: String, msg: String, t: Throwable? = null) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] WARN $msg ${t?.message ?: ""}")
    }

    inline fun w(tag: String, t: Throwable? = null, msg: () -> String) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] WARN ${msg()} ${t?.message ?: ""}")
    }

    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] ERROR $msg ${t?.message ?: ""}")
        t?.printStackTrace()
    }

    inline fun e(tag: String, t: Throwable? = null, msg: () -> String) {
        if (!enabled) return
        println("${ts()} [Murmr/$tag] ERROR ${msg()} ${t?.message ?: ""}")
        t?.printStackTrace()
    }

    @OptIn(ExperimentalTime::class)
    @PublishedApi
    internal fun ts(): String {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val secInDay = (nowMs / 1000) % 86400
        val ms = nowMs % 1000
        val h = (secInDay / 3600).toInt()
        val m = ((secInDay % 3600) / 60).toInt()
        val s = (secInDay % 60).toInt()
        return "${pad2(h)}:${pad2(m)}:${pad2(s)}.${pad3(ms.toInt())}"
    }

    private fun pad2(v: Int): String = if (v < 10) "0$v" else v.toString()
    private fun pad3(v: Int): String = when {
        v < 10 -> "00$v"
        v < 100 -> "0$v"
        else -> v.toString()
    }
}
