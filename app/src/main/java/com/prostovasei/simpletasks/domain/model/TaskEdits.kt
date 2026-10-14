package com.prostovasei.simpletasks.domain.model

import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Новый срок задачи. Начало интервала («10:00–11:00») сдвигается вместе со сроком, длительность сохраняется;
 * при смене вида срока («весь день» ↔ «со временем») или его снятии интервал убирается.
 */
fun Task.withDue(newDue: TaskDue?): Task {
    val oldDue = due
    val newStart = when {
        start == null || oldDue == null || newDue == null -> null
        start is TaskDue.AtTime && oldDue is TaskDue.AtTime && newDue is TaskDue.AtTime ->
            TaskDue.AtTime(start.instant.plus(Duration.between(oldDue.instant, newDue.instant)))
        start is TaskDue.AllDay && oldDue is TaskDue.AllDay && newDue is TaskDue.AllDay ->
            TaskDue.AllDay(start.date.plusDays(ChronoUnit.DAYS.between(oldDue.date, newDue.date)))
        else -> null
    }
    return copy(due = newDue, start = newStart, rrule = if (newDue == null) null else rrule)
}

/** Напоминание TickTick («PT0S», «-P1D», «-PT10080M») → смещение относительно срока; null — неизвестный формат. */
fun parseReminder(trigger: String): Duration? = runCatching { Duration.parse(trigger.trim()) }.getOrNull()
