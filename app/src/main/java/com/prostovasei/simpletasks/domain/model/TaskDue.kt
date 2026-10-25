package com.prostovasei.simpletasks.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Срок задачи. */
sealed interface TaskDue {
    /** На весь день: календарная дата, не зависит от часового пояса. */
    data class AllDay(val date: LocalDate) : TaskDue

    /** Точное время: конкретный момент. */
    data class AtTime(val instant: Instant) : TaskDue
}

/** Тот же срок, перенесённый на [date]: время суток сохраняется, без срока — на весь день. */
fun TaskDue?.movedTo(date: LocalDate, zone: ZoneId): TaskDue = when (this) {
    null, is TaskDue.AllDay -> TaskDue.AllDay(date)
    is TaskDue.AtTime -> TaskDue.AtTime(instant.atZone(zone).toLocalTime().atDate(date).atZone(zone).toInstant())
}
