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
    fun dragSnapsToStepsAndStaysInDay() {
        // День 50 px, шаг 30 минут — 20 px.
        assertEquals(1 to 10 * 60 + 30, WeekGridMath.moveTarget(10 * 60, 60, 30f, 25f, 50f, 20f))
        assertEquals(-1 to 9 * 60, WeekGridMath.moveTarget(10 * 60, 60, -40f, -45f, 50f, 20f))
        assertEquals("Конец не дальше 24:00", 0 to 22 * 60, WeekGridMath.moveTarget(10 * 60, 120, 0f, 2000f, 50f, 20f))
        assertEquals(0 to 0, WeekGridMath.moveTarget(1 * 60, 30, 0f, -500f, 50f, 20f))
    }

    @Test
    fun resizeSnapsAndLimits() {
        assertEquals(90, WeekGridMath.resizeTarget(16 * 60, 30, 41f, 20f))
        assertEquals("Не короче 30 минут", 30, WeekGridMath.resizeTarget(16 * 60, 60, -300f, 20f))
        assertEquals("Не дальше 24:00", 60, WeekGridMath.resizeTarget(23 * 60, 30, 500f, 20f))
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
