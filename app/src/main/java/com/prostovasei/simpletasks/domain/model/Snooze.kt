package com.prostovasei.simpletasks.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Вариант меню «Отложить» (итерация 53, T-84). */
enum class SnoozeKind { HOUR, THREE_HOURS, EVENING, TOMORROW, DAY_AFTER, SATURDAY, WEEK }

/** Вариант «Отложить»: новый срок [due] и начало [start] задачи (у задачи с периодом сдвигается весь период). */
data class SnoozeOption(val kind: SnoozeKind, val due: TaskDue, val start: TaskDue?)

/**
 * «Отложить» из уведомления и полоски (итерация 53, решения PM 07.11.2026, вариант Б): переносит срок самой задачи.
 *
 * - Задача со временем: через 1 час, через 3 часа, вечером (19:00), завтра (в то же время).
 * - Задача на весь день: завтра, послезавтра, в субботу, через неделю — задача остаётся без времени.
 * - Новый срок считается от старого срока; если он уже прошёл — от текущего момента (через N часов — от «сейчас»,
 *   округлённо вверх до 5 минут; дни — от сегодня).
 * - «Вечером» — только если задача раньше 19:00 и 19:00 её дня ещё впереди. «В субботу» — если не совпадает с «завтра» или «послезавтра».
 */
object Snooze {
    const val EVENING_HOUR = 19
    private const val ROUND_MINUTES = 5L

    fun options(due: TaskDue?, start: TaskDue?, now: Instant, zone: ZoneId): List<SnoozeOption> {
        val today = now.atZone(zone).toLocalDate()
        return when (due) {
            is TaskDue.AtTime -> timed(due, start, now, zone)
            is TaskDue.AllDay -> allDay(due, start, today)
            null -> allDay(TaskDue.AllDay(today), null, today)
        }
    }

    /** Отсчёт — от начала задачи (у периода «18:00–19:00» — от 18:00), весь период сдвигается на столько же. */
    private fun timed(due: TaskDue.AtTime, start: TaskDue?, now: Instant, zone: ZoneId): List<SnoozeOption> {
        val today = now.atZone(zone).toLocalDate()
        val anchor = (start as? TaskDue.AtTime)?.instant ?: due.instant
        fun option(kind: SnoozeKind, target: Instant): SnoozeOption {
            val by = Duration.between(anchor, target)
            return SnoozeOption(kind, TaskDue.AtTime(due.instant.plus(by)), start.shifted(by))
        }
        fun later(hours: Long): Instant {
            val fromOld = anchor.plus(Duration.ofHours(hours))
            return if (fromOld.isAfter(now)) fromOld else roundUp(now.plus(Duration.ofHours(hours)))
        }
        val at = anchor.atZone(zone)
        val evening = at.toLocalDate().atTime(LocalTime.of(EVENING_HOUR, 0)).atZone(zone).toInstant()
        // «Завтра» — на день позже старого срока; если это сегодня или раньше — завтра в то же время.
        val tomorrow = at.plusDays(1).takeIf { it.toLocalDate().isAfter(today) }?.toInstant()
            ?: at.toLocalTime().atDate(today.plusDays(1)).atZone(zone).toInstant()
        return listOfNotNull(
            option(SnoozeKind.HOUR, later(1)),
            option(SnoozeKind.THREE_HOURS, later(3)),
            option(SnoozeKind.EVENING, evening).takeIf { at.hour < EVENING_HOUR && evening.isAfter(now) },
            option(SnoozeKind.TOMORROW, tomorrow),
        )
    }

    /** Дни — от даты задачи (у многодневной — от первого дня); если выходит сегодня или раньше — от сегодня. */
    private fun allDay(due: TaskDue.AllDay, start: TaskDue?, today: LocalDate): List<SnoozeOption> {
        val anchor = (start as? TaskDue.AllDay)?.date ?: due.date
        fun day(days: Long): LocalDate = anchor.plusDays(days).takeIf { it.isAfter(today) } ?: today.plusDays(days)
        fun option(kind: SnoozeKind, target: LocalDate): SnoozeOption {
            val by = ChronoUnit.DAYS.between(anchor, target)
            return SnoozeOption(kind, TaskDue.AllDay(due.date.plusDays(by)), start.shiftedDays(by))
        }
        val tomorrow = day(1)
        val dayAfter = day(2)
        val saturday = maxOf(anchor, today).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        return listOfNotNull(
            option(SnoozeKind.TOMORROW, tomorrow),
            option(SnoozeKind.DAY_AFTER, dayAfter),
            option(SnoozeKind.SATURDAY, saturday).takeIf { saturday != tomorrow && saturday != dayAfter },
            option(SnoozeKind.WEEK, day(7)),
        )
    }

    private fun roundUp(at: Instant): Instant {
        val step = ROUND_MINUTES * 60
        val seconds = Math.floorDiv(at.epochSecond + step - 1, step) * step
        return Instant.ofEpochSecond(seconds)
    }

    private fun TaskDue?.shifted(by: Duration): TaskDue? = when (this) {
        null -> null
        is TaskDue.AtTime -> TaskDue.AtTime(instant.plus(by))
        // Начало «на весь день» у задачи со временем — сдвигается на столько же дней, сколько срок.
        is TaskDue.AllDay -> TaskDue.AllDay(date.plusDays(Math.floorDiv(by.toMinutes(), 24L * 60)))
    }

    private fun TaskDue?.shiftedDays(days: Long): TaskDue? = when (this) {
        null -> null
        is TaskDue.AllDay -> TaskDue.AllDay(date.plusDays(days))
        is TaskDue.AtTime -> TaskDue.AtTime(instant.plus(Duration.ofDays(days)))
    }
}
