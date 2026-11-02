package com.prostovasei.simpletasks.ui.feature.week

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.WeekTasks
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Сборка недели (итерация 44): задачи по дням, выполненные на своём дне, будущие повторы, порядок внутри дня. */
class WeekDaysTest {
    // Вторник, 13 октября 2026; неделя — с понедельника 12-го.
    private val today = LocalDate.of(2026, 10, 13)
    private val monday = LocalDate.of(2026, 10, 12)
    private val zone = ZoneOffset.UTC

    private fun at(time: String) = TaskDue.AtTime(Instant.parse("2026-10-${time}Z"))

    private fun build(inRange: List<com.prostovasei.simpletasks.domain.model.Task>, repeating: List<com.prostovasei.simpletasks.domain.model.Task> = emptyList()) =
        buildWeek(monday, WeekTasks(inRange, repeating), emptyMap(), today, zone, emptySet())

    @Test
    fun weekStartIsMonday() {
        assertEquals(monday, LocalDate.of(2026, 10, 18).weekStart())
        assertEquals(monday, monday.weekStart())
        assertEquals(monday, LocalDate.of(2026, 10, 14).weekStart())
        assertEquals(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 31).weekStart())
    }

    @Test
    fun sevenDaysFromMondayEvenWhenEmpty() {
        val days = build(emptyList())
        assertEquals((0L..6L).map { monday.plusDays(it) }, days.map { it.date })
        assertTrue(days.all { it.items.isEmpty() })
    }

    @Test
    fun tasksLandOnTheirDayAllDayFirstThenByTime() {
        val days = build(
            listOf(
                task("late", due = at("14T18:00:00")),
                task("early", due = at("14T09:00:00")),
                task("allday", due = TaskDue.AllDay(LocalDate.of(2026, 10, 14))),
                task("mon", due = TaskDue.AllDay(monday)),
            ),
        )
        assertEquals(listOf("mon"), days[0].items.map { it.id })
        assertEquals(listOf("allday", "early", "late"), days[2].items.map { it.id })
    }

    @Test
    fun intervalBelongsToItsStartDay() {
        // Начало во вторник 23:00, конец в среду 01:00 — день задачи вторник (решение PM: день и порядок по началу).
        val days = build(listOf(task("night", start = at("13T23:00:00"), due = at("14T01:00:00"))))
        assertEquals(listOf("night"), days[1].items.map { it.id })
        assertTrue(days[2].items.isEmpty())
    }

    @Test
    fun doneTasksStayOnTheirDay() {
        val done = task("done", due = at("12T10:00:00"), status = TaskStatus.COMPLETED)
        val days = build(listOf(done))
        assertTrue(days[0].items.single().isDone)
    }

    @Test
    fun futureRepeatsAreGhostsAfterToday() {
        val daily = task("daily", due = at("13T08:00:00")).copy(rrule = "FREQ=DAILY")
        val days = build(listOf(daily), repeating = listOf(daily))
        // Сегодня — сама задача; завтра и дальше — будущие разы; до сегодня (понедельник) ничего не придумываем.
        assertTrue(days[0].items.isEmpty())
        assertFalse(days[1].items.single().isGhost)
        assertEquals(listOf(true, true, true, true, true), days.drop(2).map { it.items.single().isGhost })
        assertEquals("daily", days[2].items.single().taskId)
    }

    @Test
    fun pastWeekHasNoGhosts() {
        val daily = task("daily", due = at("20T08:00:00")).copy(rrule = "FREQ=DAILY")
        val days = buildWeek(monday, WeekTasks(emptyList(), listOf(daily)), emptyMap(), today = LocalDate.of(2026, 10, 25), zone, emptySet())
        assertTrue(days.all { it.items.isEmpty() })
    }

    @Test
    fun futureWeekShowsRepeatsForAllSevenDays() {
        val daily = task("daily", due = at("13T08:00:00")).copy(rrule = "FREQ=DAILY")
        val nextMonday = monday.plusDays(7)
        val days = buildWeek(nextMonday, WeekTasks(emptyList(), listOf(daily)), emptyMap(), today, zone, emptySet())
        assertEquals(7, days.count { it.items.size == 1 && it.items.single().isGhost })
    }

    @Test
    fun priorityAndTitleAreKept() {
        val days = build(listOf(task("p", title = "Созвон", due = at("15T10:00:00"), priority = Priority.HIGH)))
        val item = days[3].items.single()
        assertEquals("Созвон", item.title)
        assertEquals(Priority.HIGH, item.priority)
        assertEquals("10:00", item.startTime())
    }

    @Test
    fun startTimeCutsIntervalEnd() {
        val days = build(listOf(task("i", start = at("15T10:00:00"), due = at("15T11:30:00"))))
        assertEquals("10:00", days[3].items.single().startTime())
        assertEquals(null, build(listOf(task("a", due = TaskDue.AllDay(monday))))[0].items.single().startTime())
    }
}
