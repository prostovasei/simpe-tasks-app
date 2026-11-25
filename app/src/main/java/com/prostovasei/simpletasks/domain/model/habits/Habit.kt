package com.prostovasei.simpletasks.domain.model.habits

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Расписание привычки (#340): каждый день, по дням недели или N раз в неделю. */
sealed interface HabitSchedule {
    data object Daily : HabitSchedule

    /** [mask] — дни недели: пн = 1, вт = 2 … вс = 64; хотя бы один день. */
    data class Weekdays(val mask: Int) : HabitSchedule {
        fun has(day: DayOfWeek): Boolean = mask and (1 shl (day.value - 1)) != 0
    }

    /** [times] раз за неделю пн–вс (1–6). */
    data class PerWeek(val times: Int) : HabitSchedule

    companion object {
        const val ALL_DAYS = 0b1111111
        const val MAX_PER_WEEK = 6

        fun mask(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    }
}

/** Привычка (#340). [checks] — дни, когда отмечено «сделано». */
data class Habit(
    val id: String,
    val name: String,
    val emoji: String?,
    val schedule: HabitSchedule,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val checks: Set<LocalDate> = emptySet(),
) {
    val emojiShown: String get() = emoji ?: DEFAULT_EMOJI

    fun isDone(date: LocalDate): Boolean = date in checks

    companion object {
        const val DEFAULT_EMOJI = "✅"
        const val MAX_NAME = 60
    }
}

/** Настройки модуля (#340): общее напоминание. */
data class HabitSettings(val reminderOn: Boolean = false, val reminderMinute: Int = DEFAULT_REMINDER_MINUTE) {
    companion object {
        const val DEFAULT_REMINDER_MINUTE = 21 * 60
    }
}

class InvalidHabitBackupException(message: String) : Exception(message)

/** Что лежит в файле копии «Привычек». */
data class HabitBackupInfo(val habits: Int, val checks: Int, val settings: HabitSettings)

/** Серии, расписание и статистика привычек (#340, ТЗ §3). */
object HabitMath {
    /** День по расписанию (для «N в неделю» — любой день недели). */
    fun scheduledOn(schedule: HabitSchedule, date: LocalDate): Boolean = when (schedule) {
        HabitSchedule.Daily, is HabitSchedule.PerWeek -> true
        is HabitSchedule.Weekdays -> schedule.has(date.dayOfWeek)
    }

    /** Нужна ли привычка в [date]: «N в неделю» — пока за неделю сделано меньше N (сегодняшняя отметка не убирает её из «сегодня»). */
    fun dueOn(habit: Habit, date: LocalDate): Boolean = when (val s = habit.schedule) {
        HabitSchedule.Daily -> true
        is HabitSchedule.Weekdays -> s.has(date.dayOfWeek)
        is HabitSchedule.PerWeek -> habit.isDone(date) || weekCount(habit, date) < s.times
    }

    /** Отметок за неделю пн–вс, где лежит [date]. */
    fun weekCount(habit: Habit, date: LocalDate): Int {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L..6L).count { habit.isDone(monday.plusDays(it)) }
    }

    /** Текущая серия: дни расписания подряд (для «N в неделю» — недели); незакрытый сегодняшний день или неделя её не обрывают. */
    fun streak(habit: Habit, today: LocalDate): Int = when (val s = habit.schedule) {
        is HabitSchedule.PerWeek -> weekStreak(habit, s.times, today)
        else -> {
            var day = today
            if (!habit.isDone(today)) day = today.minusDays(1)
            var count = 0
            val earliest = habit.checks.minOrNull() ?: return 0
            while (!day.isBefore(earliest)) {
                if (scheduledOn(s, day)) {
                    if (habit.isDone(day)) count++ else break
                }
                day = day.minusDays(1)
            }
            count
        }
    }

    private fun weekStreak(habit: Habit, times: Int, today: LocalDate): Int {
        val earliest = habit.checks.minOrNull() ?: return 0
        var monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var count = 0
        if (weekCount(habit, monday) >= times) count++
        monday = monday.minusWeeks(1)
        while (!monday.plusDays(6).isBefore(earliest)) {
            if (weekCount(habit, monday) >= times) count++ else break
            monday = monday.minusWeeks(1)
        }
        return count
    }

    /** Лучшая серия за всю историю (в днях расписания; для «N в неделю» — в неделях). */
    fun bestStreak(habit: Habit, today: LocalDate): Int {
        val earliest = habit.checks.minOrNull() ?: return 0
        return when (val s = habit.schedule) {
            is HabitSchedule.PerWeek -> {
                var monday = earliest.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                var best = 0
                var run = 0
                while (!monday.isAfter(today)) {
                    if (weekCount(habit, monday) >= s.times) { run++; best = maxOf(best, run) } else if (monday.plusDays(6).isBefore(today)) run = 0
                    monday = monday.plusWeeks(1)
                }
                best
            }
            else -> {
                var best = 0
                var run = 0
                var day = earliest
                while (!day.isAfter(today)) {
                    if (scheduledOn(s, day)) {
                        if (habit.isDone(day)) { run++; best = maxOf(best, run) } else if (day != today) run = 0
                    }
                    day = day.plusDays(1)
                }
                best
            }
        }
    }

    /** Доля выполнения за последние [days] дней (0–100): сделанные дни расписания / дни расписания; «N в неделю» — отметки / (N × дни / 7). */
    fun rate(habit: Habit, today: LocalDate, days: Int = RATE_DAYS): Int {
        val from = today.minusDays(days - 1L)
        val done = habit.checks.count { !it.isBefore(from) && !it.isAfter(today) }
        return when (val s = habit.schedule) {
            is HabitSchedule.PerWeek -> {
                val planned = s.times * days / 7.0
                if (planned <= 0) 0 else minOf(100, (done * 100 / planned).toInt())
            }
            else -> {
                val planned = (0 until days).count { scheduledOn(s, from.plusDays(it.toLong())) }
                val doneScheduled = habit.checks.count { !it.isBefore(from) && !it.isAfter(today) && scheduledOn(s, it) }
                if (planned == 0) 0 else doneScheduled * 100 / planned
            }
        }
    }

    /** Подпись серии: «🔥 12», для «N в неделю» — «🔥 3 нед.»; нулевая серия — пусто. */
    fun streakLabel(habit: Habit, today: LocalDate): String {
        val n = streak(habit, today)
        if (n == 0) return ""
        return if (habit.schedule is HabitSchedule.PerWeek) "🔥 $n нед." else "🔥 $n"
    }

    /** Дни текущей недели пн–вс. */
    fun week(today: LocalDate): List<LocalDate> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L..6L).map { monday.plusDays(it) }
    }

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    const val RATE_DAYS = 30
}
