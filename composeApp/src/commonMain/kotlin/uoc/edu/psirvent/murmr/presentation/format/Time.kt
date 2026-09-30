@file:Suppress("DEPRECATION")

package uoc.edu.psirvent.murmr.presentation.format

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatChatListTime(timestamp: Instant?, now: Instant = Clock.System.now()): String {
    if (timestamp == null) return ""
    val tz = TimeZone.currentSystemDefault()
    val tNow = now.toLocalDateTime(tz)
    val t = timestamp.toLocalDateTime(tz)
    return when {
        sameDay(t, tNow) -> formatHm(t)
        (now - timestamp) < 2.days && t.date == tNow.date.previousDay() -> "Ayer"
        else -> "${t.dayOfMonth}/${t.monthNumber}"
    }
}

private fun LocalDate.previousDay(): LocalDate =
    LocalDate(year, monthNumber, dayOfMonth - 1).takeIf { dayOfMonth >= 2 }
        ?: LocalDate(year, monthNumber, 1)

fun formatHm(timestamp: Instant): String =
    formatHm(timestamp.toLocalDateTime(TimeZone.currentSystemDefault()))

private fun formatHm(t: LocalDateTime): String {
    val h = t.hour.toString().padStart(2, '0')
    val m = t.minute.toString().padStart(2, '0')
    return "$h:$m"
}

private fun sameDay(a: LocalDateTime, b: LocalDateTime): Boolean =
    a.year == b.year && a.monthNumber == b.monthNumber && a.dayOfMonth == b.dayOfMonth
