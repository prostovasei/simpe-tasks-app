package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * «Отложить» (итерация 53, решения PM): от старого срока; если вышло в прошлом — от «сейчас» (часы — с округлением до 5 минут,
 * дни — от сегодня). Сегодня — ср 11.11.2026, Москва.
 */
class SnoozeTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val today = LocalDate.of(2026, 11, 11)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Instant = LocalDateTime.of(day, java.time.LocalTime.of(hour, minute)).atZone(zone).toInstant()
    private fun time(hour: Int, minute: Int = 0) = TaskDue.AtTime(at(today, hour, minute))
    private fun options(due: TaskDue?, start: TaskDue? = null, nowHour: Int, nowMinute: Int = 0) =
        Snooze.options(due, start, at(today, nowHour, nowMinute), zone).associate { it.kind to it }

    @Test
    fun timedTaskCountsFromItsOwnTime() {
        val found = options(time(17), nowHour = 16, nowMinute = 58)
        assertEquals(listOf(SnoozeKind.HOUR, SnoozeKind.THREE_HOURS, SnoozeKind.EVENING, SnoozeKind.TOMORROW), found.keys.toList())
        assertEquals(time(18), found.getValue(SnoozeKind.HOUR).due)
        assertEquals(time(20), found.getValue(SnoozeKind.THREE_HOURS).due)
        assertEquals(time(19), found.getValue(SnoozeKind.EVENING).due)
        assertEquals(TaskDue.AtTime(at(today.plusDays(1), 17)), found.getValue(SnoozeKind.TOMORROW).due)
    }

    @Test
    fun passedResultCountsFromNowRoundedUp() {
        // Задача на 18:00, нажали в 20:27: от старого срока вышло бы 19:00 (прошло) — через час от сейчас, 21:30.
        val found = options(time(18), nowHour = 20, nowMinute = 27)
        assertEquals(time(21, 30), found.getValue(SnoozeKind.HOUR).due)
        assertEquals("Через 3 часа от старого срока — 21:00, ещё впереди", time(21), found.getValue(SnoozeKind.THREE_HOURS).due)
        assertNull("«Вечером» скрыт: 19:00 уже позади", found[SnoozeKind.EVENING])
        assertEquals(TaskDue.AtTime(at(today.plusDays(1), 18)), found.getValue(SnoozeKind.TOMORROW).due)
    }

    @Test
    fun eveningHiddenForLateTask() {
        assertNull(options(time(19, 30), nowHour = 19)[SnoozeKind.EVENING])
    }

    @Test
    fun overdueTaskGoesToTomorrowNotPast() {
        val yesterday = TaskDue.AtTime(at(today.minusDays(1), 17))
        assertEquals(TaskDue.AtTime(at(today.plusDays(1), 17)), options(yesterday, nowHour = 9)[SnoozeKind.TOMORROW]!!.due)
    }

    @Test
    fun periodMovesWhole() {
        // «18:00–19:00»: отсчёт от начала, длительность сохраняется.
        val found = options(time(19), start = time(18), nowHour = 17)
        assertEquals(time(19), found.getValue(SnoozeKind.HOUR).start)
        assertEquals(time(20), found.getValue(SnoozeKind.HOUR).due)
        assertEquals(time(19), found.getValue(SnoozeKind.EVENING).start)
        assertEquals(time(20), found.getValue(SnoozeKind.EVENING).due)
    }

    @Test
    fun allDayTaskOffersDaysOnly() {
        val found = options(TaskDue.AllDay(today), nowHour = 10)
        assertEquals(listOf(SnoozeKind.TOMORROW, SnoozeKind.DAY_AFTER, SnoozeKind.SATURDAY, SnoozeKind.WEEK), found.keys.toList())
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 12)), found.getValue(SnoozeKind.TOMORROW).due)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 13)), found.getValue(SnoozeKind.DAY_AFTER).due)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 14)), found.getValue(SnoozeKind.SATURDAY).due)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 18)), found.getValue(SnoozeKind.WEEK).due)
    }

    @Test
    fun saturdayHiddenWhenItIsTomorrow() {
        // Пятница: «В субботу» совпало бы с «Завтра».
        val friday = TaskDue.AllDay(LocalDate.of(2026, 11, 13))
        val found = Snooze.options(friday, null, at(LocalDate.of(2026, 11, 13), 10), zone).map { it.kind }
        assertEquals(listOf(SnoozeKind.TOMORROW, SnoozeKind.DAY_AFTER, SnoozeKind.WEEK), found)
    }

    @Test
    fun overdueAllDayCountsFromToday() {
        val found = options(TaskDue.AllDay(today.minusDays(3)), nowHour = 10)
        assertEquals(TaskDue.AllDay(today.plusDays(1)), found.getValue(SnoozeKind.TOMORROW).due)
        // «Через неделю» — от старого срока (вс 08.11 + 7 = вс 15.11), он ещё впереди.
        assertEquals(TaskDue.AllDay(today.plusDays(4)), found.getValue(SnoozeKind.WEEK).due)
    }
}
