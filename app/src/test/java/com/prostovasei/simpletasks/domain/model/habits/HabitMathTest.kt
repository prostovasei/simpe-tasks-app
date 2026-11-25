package com.prostovasei.simpletasks.domain.model.habits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Серии, расписание и доля выполнения привычек (#340, ТЗ §3). Сегодня — среда 25.11.2026. */
class HabitMathTest {
    private val today = LocalDate.of(2026, 11, 25)
    private fun habit(schedule: HabitSchedule, vararg daysAgo: Long) =
        Habit("h", "Зарядка", null, schedule, checks = daysAgo.map { today.minusDays(it) }.toSet())

    @Test
    fun dailyStreakCountsBackAndTodayDoesNotBreakIt() {
        assertEquals(3, HabitMath.streak(habit(HabitSchedule.Daily, 0, 1, 2), today))
        // Сегодня ещё не отмечено: серия со вчера не обрывается.
        assertEquals(2, HabitMath.streak(habit(HabitSchedule.Daily, 1, 2, 4), today))
        // Вчера пропущено — серия только сегодняшняя.
        assertEquals(1, HabitMath.streak(habit(HabitSchedule.Daily, 0, 2, 3), today))
        assertEquals(0, HabitMath.streak(habit(HabitSchedule.Daily), today))
    }

    @Test
    fun weekdayStreakSkipsDaysOutOfSchedule() {
        // Пн, ср, пт. Сегодня ср (0), пн (2), пт прошлой недели (5), ср прошлой (7) — подряд 4; вт и чт вне расписания.
        val mwf = HabitSchedule.Weekdays(HabitSchedule.mask(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        assertEquals(4, HabitMath.streak(habit(mwf, 0, 2, 5, 7), today))
        // Пропущен пн (2) — серия 1.
        assertEquals(1, HabitMath.streak(habit(mwf, 0, 5, 7), today))
        assertTrue(HabitMath.dueOn(habit(mwf), today))
        assertFalse(HabitMath.dueOn(habit(mwf), today.plusDays(1)))
    }

    @Test
    fun perWeekCountsWeeksAndLeavesTodayWhenDone() {
        val twice = HabitSchedule.PerWeek(2)
        // Эта неделя: пн (2) — 1 из 2, не обрывает; прошлая: 2 отметки; позапрошлая: 2 отметки.
        val h = habit(twice, 2, 8, 9, 15, 16)
        assertEquals(2, HabitMath.streak(h, today))
        assertTrue(HabitMath.dueOn(h, today))
        // Эта неделя набрана (пн и вт): привычка не нужна сегодня, серия 3 недели.
        val done = habit(twice, 1, 2, 8, 9, 15, 16)
        assertEquals(3, HabitMath.streak(done, today))
        assertFalse(HabitMath.dueOn(done, today))
        // Отмеченная сегодня — остаётся в «сегодня».
        assertTrue(HabitMath.dueOn(habit(twice, 0, 2), today))
        assertEquals("🔥 3 нед.", HabitMath.streakLabel(done, today))
    }

    @Test
    fun bestStreakAndRate() {
        val h = habit(HabitSchedule.Daily, 0, 1, 2, 10, 11, 12, 13, 14)
        assertEquals(5, HabitMath.bestStreak(h, today))
        assertEquals(8 * 100 / 30, HabitMath.rate(h, today))
        assertEquals(100, HabitMath.rate(habit(HabitSchedule.PerWeek(1), *LongArray(10) { it * 3L }), today))
        assertEquals("", HabitMath.streakLabel(habit(HabitSchedule.Daily, 3), today))
    }
}
