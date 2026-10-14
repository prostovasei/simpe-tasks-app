package com.prostovasei.simpletasks.domain.model

import java.time.Instant
import java.time.LocalDate

/** Срок задачи. */
sealed interface TaskDue {
    /** На весь день: календарная дата, не зависит от часового пояса. */
    data class AllDay(val date: LocalDate) : TaskDue

    /** Точное время: конкретный момент. */
    data class AtTime(val instant: Instant) : TaskDue
}
