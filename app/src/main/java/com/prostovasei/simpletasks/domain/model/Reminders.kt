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

    /** Решение PM (итерация 23): во время, за 30 минут, 1 час, 1 день, 3 дня; остальное — «вручную». */
    val TIMED_PRESETS = listOf(AT_DUE, "-PT30M", "-PT1H", "-P1D", "-P3D")

    /** Решение PM (итерация 23): утро, обед, день, вечер, ночь в день срока; за 1 и 3 дня в 10:00. */
    val ALL_DAY_PRESETS = listOf(
        ALL_DAY_DEFAULT, "P0DT13H0M0S", "P0DT16H0M0S", "P0DT19H0M0S", "P0DT22H0M0S", "-P0DT14H0M0S", "-P2DT14H0M0S",
    )

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

    /** Напоминание «вручную» у задачи со временем: за [minutesBefore] минут до срока. */
    fun timed(minutesBefore: Long): String = if (minutesBefore <= 0) AT_DUE else format(-minutesBefore)

    /** Напоминание «вручную» у задачи на весь день: за [daysBefore] дней в [minuteOfDay] (от полуночи). */
    fun allDay(daysBefore: Int, minuteOfDay: Int): String = format(-daysBefore * MINUTES_PER_DAY + minuteOfDay)

    /** Смещение в минутах → запись TickTick: «P0DT10H0M0S», «-P2DT14H0M0S». */
    private fun format(minutes: Long): String {
        val abs = Math.abs(minutes)
        val sign = if (minutes < 0) "-" else ""
        return "${sign}P${abs / MINUTES_PER_DAY}DT${abs % MINUTES_PER_DAY / 60}H${abs % 60}M0S"
    }

    /**
     * Значок напоминаний задачи (решение PM, итерация 23): несколько — число, одно — его вид; без срока
     * или без напоминаний — null.
     */
    fun badge(due: TaskDue?, reminders: List<String>): ReminderBadge? {
        if (due == null) return null
        val distinct = reminders.fold(emptyList<String>()) { acc, r -> if (acc.any { sameMoment(it, r) }) acc else acc + r }
        return when (distinct.size) {
            0 -> null
            1 -> badgeOf(due, distinct.single())
            else -> ReminderBadge.Several(distinct.size)
        }
    }

    /** Вид одного напоминания: готовый вариант — свой значок, любое другое — «вручную». */
    fun badgeOf(due: TaskDue, trigger: String): ReminderBadge {
        val (presets, badges) = when (due) {
            is TaskDue.AtTime -> TIMED_PRESETS to TIMED_BADGES
            is TaskDue.AllDay -> ALL_DAY_PRESETS to ALL_DAY_BADGES
        }
        val index = presets.indexOfFirst { sameMoment(it, trigger) }
        return if (index >= 0) badges[index] else ReminderBadge.Custom
    }

    private val TIMED_BADGES = listOf(
        ReminderBadge.AtDue,
        ReminderBadge.Before(BeforeLabel.MINUTES_30),
        ReminderBadge.Before(BeforeLabel.HOUR_1),
        ReminderBadge.Before(BeforeLabel.DAY_1),
        ReminderBadge.Before(BeforeLabel.DAYS_3),
    )
    private val ALL_DAY_BADGES = DayPart.entries.map { ReminderBadge.AtDayPart(it) } +
        listOf(ReminderBadge.Before(BeforeLabel.DAY_1), ReminderBadge.Before(BeforeLabel.DAYS_3))

    /** Одинаковые напоминания в разной записи («PT0S» и «-PT0S») считаются одним. */
    fun sameMoment(a: String, b: String): Boolean = parseReminder(a)?.let { it == parseReminder(b) } ?: (a == b)

    /** Моменты срабатывания напоминаний задачи (по возрастанию). */
    fun instants(task: Task, zone: ZoneId): List<Instant> {
        val due = task.due ?: return emptyList()
        val anchor = anchor(due, task.start, zone)
        return task.reminders.mapNotNull { parseReminder(it) }.map { anchor.plus(it) }.distinct().sorted()
    }

    /** Когда сработает напоминание [trigger] задачи со сроком [due] и началом [start]; null — неизвестный формат. */
    fun instant(due: TaskDue, start: TaskDue?, trigger: String, zone: ZoneId): Instant? =
        parseReminder(trigger)?.let { anchor(due, start, zone).plus(it) }

    private fun anchor(due: TaskDue, start: TaskDue?, zone: ZoneId): Instant = when (due) {
        is TaskDue.AllDay -> ((start as? TaskDue.AllDay)?.date ?: due.date).atStartOfDay(zone).toInstant()
        is TaskDue.AtTime -> (start as? TaskDue.AtTime)?.instant ?: due.instant
    }
}

/** Значок напоминаний: колокольчик и серый значок в углу (решение PM, итерация 23). */
sealed interface ReminderBadge {
    /** В момент срока — колокольчик без значка. */
    data object AtDue : ReminderBadge

    /** Готовое «за …»: «30», «1ч», «1д», «3д» справа внизу. */
    data class Before(val label: BeforeLabel) : ReminderBadge

    /** Задача на весь день: время суток кругом-заливкой (ночь — луна) справа внизу. */
    data class AtDayPart(val part: DayPart) : ReminderBadge

    /** Заданное вручную — часики справа внизу. */
    data object Custom : ReminderBadge

    /** Несколько напоминаний — их число справа вверху. */
    data class Several(val count: Int) : ReminderBadge
}

enum class BeforeLabel { MINUTES_30, HOUR_1, DAY_1, DAYS_3 }

/** Время суток напоминания задачи на весь день: 10, 13, 16, 19 и 22 часа. */
enum class DayPart { MORNING, NOON, AFTERNOON, EVENING, NIGHT }

/** Смещение напоминания задачи на весь день: «за [daysBefore] дн. в [minuteOfDay]». */
data class AllDayReminder(val daysBefore: Int, val minuteOfDay: Int)

fun allDayReminder(offset: Duration): AllDayReminder {
    val minutes = offset.toMinutes()
    return AllDayReminder(daysBefore = -Math.floorDiv(minutes, MINUTES_PER_DAY).toInt(), minuteOfDay = Math.floorMod(minutes, MINUTES_PER_DAY).toInt())
}

private const val MINUTES_PER_DAY = 24L * 60
