package com.prostovasei.simpletasks.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Напоминания в формате TickTick (iCalendar TRIGGER) — чтобы резервная копия загружалась обратно без потерь.
 * - Задача со временем: смещение от срока (у интервала — от начала): «PT0S» — в момент срока, «-PT30M» — за 30 минут.
 * - Задача на весь день: смещение от полуночи дня срока: «P0DT10H0M0S» — в день срока в 10:00,
 *   «-P0DT14H0M0S» — накануне в 10:00.
 */
object Reminders {
    const val AT_DUE = "PT0S"

    /** Решение PM: задачи на весь день напоминают в 10:00. */
    const val ALL_DAY_DEFAULT = "P0DT10H0M0S"

    val TIMED_PRESETS = listOf(AT_DUE, "-PT5M", "-PT30M", "-PT1H", "-P1D")
    val ALL_DAY_PRESETS = listOf(ALL_DAY_DEFAULT, "-P0DT14H0M0S", "-P6DT14H0M0S")

    /** Решение PM: у новой задачи со сроком напоминание включено по умолчанию. */
    fun default(due: TaskDue?): List<String> = when (due) {
        null -> emptyList()
        is TaskDue.AllDay -> listOf(ALL_DAY_DEFAULT)
        is TaskDue.AtTime -> listOf(AT_DUE)
    }

    fun presets(due: TaskDue?): List<String> = when (due) {
        null -> emptyList()
        is TaskDue.AllDay -> ALL_DAY_PRESETS
        is TaskDue.AtTime -> TIMED_PRESETS
    }

    /** Одинаковые напоминания в разной записи («PT0S» и «-PT0S») считаются одним. */
    fun sameMoment(a: String, b: String): Boolean = parseReminder(a)?.let { it == parseReminder(b) } ?: (a == b)

    /** Моменты срабатывания напоминаний задачи (по возрастанию). */
    fun instants(task: Task, zone: ZoneId): List<Instant> {
        val due = task.due ?: return emptyList()
        val anchor: Instant = when (due) {
            is TaskDue.AllDay -> ((task.start as? TaskDue.AllDay)?.date ?: due.date).atStartOfDay(zone).toInstant()
            is TaskDue.AtTime -> (task.start as? TaskDue.AtTime)?.instant ?: due.instant
        }
        return task.reminders.mapNotNull { parseReminder(it) }.map { anchor.plus(it) }.distinct().sorted()
    }
}

/** Смещение напоминания задачи на весь день: «за [daysBefore] дн. в [minuteOfDay]». */
data class AllDayReminder(val daysBefore: Int, val minuteOfDay: Int)

fun allDayReminder(offset: Duration): AllDayReminder {
    val minutes = offset.toMinutes()
    return AllDayReminder(daysBefore = -Math.floorDiv(minutes, MINUTES_PER_DAY).toInt(), minuteOfDay = Math.floorMod(minutes, MINUTES_PER_DAY).toInt())
}

private const val MINUTES_PER_DAY = 24L * 60
