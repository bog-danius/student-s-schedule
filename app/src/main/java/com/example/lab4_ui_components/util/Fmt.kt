package com.example.lab4_ui_components.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

object Fmt {
    private val ru = Locale("ru")
    private val shortDays = arrayOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    fun time(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
    fun range(start: Int, end: Int) = "${time(start)} — ${time(end)}"

    fun dayMonth(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM", ru))
    fun fullDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ru))
    fun weekdayDate(d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", ru)).replaceFirstChar { it.uppercase(ru) }

    fun dayShort(d: LocalDate): String = shortDays[d.dayOfWeek.value - 1]

    fun room(r: String): String = if (r.isNotEmpty() && r.all { it.isDigit() }) "Каб. $r" else r

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    fun lessons(n: Int) = "$n ${plural(n, "занятие", "занятия", "занятий")}"

    /** "Сегодня, 18:00" / "Завтра, 14:00" / "27 сентября, 14:00" */
    fun dateTimeLabel(millis: Long): String {
        val dt = millis.toLocalDateTime()
        val today = LocalDate.now()
        val label = when (dt.toLocalDate()) {
            today -> "Сегодня"
            today.plusDays(1) -> "Завтра"
            else -> dayMonth(dt.toLocalDate())
        }
        return "$label, ${time(dt.hour * 60 + dt.minute)}"
    }
}
