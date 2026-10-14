package com.prostovasei.simpletasks.data.local

import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Хранение срока задачи в колонках due_date / is_all_day (аудит R1). */
internal object DueDateCodec {
    const val MILLIS_PER_DAY = 86_400_000L

    data class Encoded(val dueDate: Long?, val isAllDay: Boolean)

    fun encode(due: TaskDue?): Encoded = when (due) {
        // Без срока is_all_day = 1 — значение по умолчанию из SPEC.md §2.3.
        null -> Encoded(dueDate = null, isAllDay = true)
        is TaskDue.AllDay -> Encoded(dueDate = allDayKey(due.date), isAllDay = true)
        is TaskDue.AtTime -> Encoded(dueDate = due.instant.toEpochMilli(), isAllDay = false)
    }

    fun decode(dueDate: Long?, isAllDay: Boolean): TaskDue? = when {
        dueDate == null -> null
        isAllDay -> TaskDue.AllDay(LocalDate.ofEpochDay(Math.floorDiv(dueDate, MILLIS_PER_DAY)))
        else -> TaskDue.AtTime(Instant.ofEpochMilli(dueDate))
    }

    /** Полночь календарной даты по UTC. */
    fun allDayKey(date: LocalDate): Long = date.toEpochDay() * MILLIS_PER_DAY
}

/** Границы одного календарного дня для выборок экрана «Все». */
internal data class DayWindow(
    val allDayStart: Long,
    val allDayEnd: Long,
    val start: Long,
    val end: Long,
) {
    companion object {
        fun of(date: LocalDate, zone: ZoneId) = DayWindow(
            allDayStart = DueDateCodec.allDayKey(date),
            allDayEnd = DueDateCodec.allDayKey(date.plusDays(1)),
            start = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }
}
