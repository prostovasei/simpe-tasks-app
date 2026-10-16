package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** День срока относительно сегодняшнего. */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Tomorrow : DayLabel
    data object Yesterday : DayLabel
    data class Date(val date: LocalDate, val showYear: Boolean) : DayLabel
}

/** Срок для подписи под задачей: день и время («10:00» или интервал «10:00–11:00»). */
data class DueText(val day: DayLabel, val time: String?)

object DueFormatter {
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Для задач с интервалом день и порядок определяются началом (решение PM, docs/ARCHITECTURE.md §4). */
    fun format(due: TaskDue?, start: TaskDue?, today: LocalDate, zone: ZoneId): DueText? = when (due) {
        null -> null
        is TaskDue.AllDay -> DueText(dayLabel(due.date, today), time = null)
        is TaskDue.AtTime -> {
            val begin = (start as? TaskDue.AtTime)?.instant ?: due.instant
            val time = if (begin == due.instant) time(begin, zone) else "${time(begin, zone)}–${time(due.instant, zone)}"
            DueText(dayLabel(begin.atZone(zone).toLocalDate(), today), time)
        }
    }

    private fun time(instant: Instant, zone: ZoneId): String = TIME.format(instant.atZone(zone))

    fun dayLabel(date: LocalDate, today: LocalDate): DayLabel = when (date) {
        today -> DayLabel.Today
        today.plusDays(1) -> DayLabel.Tomorrow
        today.minusDays(1) -> DayLabel.Yesterday
        else -> DayLabel.Date(date, showYear = date.year != today.year)
    }
}
