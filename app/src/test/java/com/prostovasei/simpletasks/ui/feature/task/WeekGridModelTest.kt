package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Сетка недели (итерация 16): геометрия нажатий и занятое время. */
class WeekGridModelTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 14)
    private val hourPx = 100f

    @Test
    fun minuteSnapsToHalfHour() {
        assertEquals(0, WeekGridMath.minuteAt(0f, hourPx))
        assertEquals(9 * 60, WeekGridMath.minuteAt(9 * hourPx + 10f, hourPx))
        assertEquals(9 * 60 + 30, WeekGridMath.minuteAt(9 * hourPx + 55f, hourPx))
        assertEquals("Ниже сетки — последний шаг 23:30", 23 * 60 + 30, WeekGridMath.minuteAt(25 * hourPx, hourPx))
        assertEquals(0, WeekGridMath.minuteAt(-5f, hourPx))
    }

    @Test
    fun dayUnderFingerFollowsScroll() {
        assertEquals(0, WeekGridMath.dayAt(10f, 0f, 50f))
        assertEquals(2, WeekGridMath.dayAt(110f, 0f, 50f))
        assertEquals("Пролистано на полдня назад", -1, WeekGridMath.dayAt(10f, -0.5f, 50f))
    }

    @Test
    fun swipeSettlesOnDayBorder() {
        assertEquals(1f, WeekGridMath.settleTarget(0.6f, 0f, 50f))
        assertEquals(0f, WeekGridMath.settleTarget(0.4f, 0f, 50f))
        assertEquals("Быстрый свайп влево уводит дальше", 3f, WeekGridMath.settleTarget(0.4f, -1000f, 50f))
    }

    @Test
    fun startsFromTodayUnlessSelectedFar() {
        assertEquals(0f, WeekGridMath.initialScroll(today, null))
        assertEquals(0f, WeekGridMath.initialScroll(today, today.plusDays(6)))
        assertEquals(10f, WeekGridMath.initialScroll(today, today.plusDays(10)))
        assertEquals(-3f, WeekGridMath.initialScroll(today, today.minusDays(3)))
    }

    @Test
    fun busySlotsFromTimedTasks() {
        val tasks = listOf(
            task("a", due = at("2026-10-14T16:00:00Z")),
            task("b", due = at("2026-10-15T11:00:00Z"), start = at("2026-10-15T09:30:00Z"), status = TaskStatus.COMPLETED),
            task("c", due = at("2026-10-17T01:00:00Z"), start = at("2026-10-16T22:00:00Z")),
            task("d", due = TaskDue.AllDay(today)),
            task("e", due = at("2026-10-30T10:00:00Z")),
        )
        assertEquals(
            listOf(
                BusySlot("a", "a", today, 16 * 60, 16 * 60 + 30, isDone = false),
                BusySlot("b", "b", today.plusDays(1), 9 * 60 + 30, 11 * 60, isDone = true),
                BusySlot("c", "c", today.plusDays(2), 22 * 60, 24 * 60, isDone = false),
            ),
            busySlots(tasks, zone, today, today.plusDays(6)),
        )
    }

    @Test
    fun repeatingTaskShowsNextOccurrences() {
        val daily = task("r", due = at("2026-10-14T08:00:00Z")).copy(rrule = "FREQ=DAILY")
        val days = busySlots(listOf(daily), zone, today, today.plusDays(3)).map { it.date }
        assertEquals(listOf(today, today.plusDays(1), today.plusDays(2), today.plusDays(3)), days)
    }

    private fun at(text: String) = TaskDue.AtTime(Instant.parse(text))
}
