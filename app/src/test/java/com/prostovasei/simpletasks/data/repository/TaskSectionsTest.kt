package com.prostovasei.simpletasks.data.repository

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Группировка экрана «Все» (SPEC.md §4.1) и «Входящих» (§4.2). Пользователь в Москве, сейчас 13.10.2026 15:00. */
class TaskSectionsTest : DatabaseTest() {

    private val moscow = ZoneId.of("Europe/Moscow")
    private val today = LocalDate.of(2026, 10, 13)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0, zone: ZoneId = moscow) =
        TaskDue.AtTime(LocalDateTime.of(day, LocalTime.of(hour, minute)).atZone(zone).toInstant())

    private suspend fun create(title: String, due: TaskDue?, priority: Priority = Priority.NONE, project: String? = null) =
        tasks.createTask(NewTask(projectId = project ?: projects.getInbox().id, title = title, due = due, priority = priority))

    @Test
    fun tasksAreSplitIntoSections() = runTest {
        time.zone = moscow
        time.now = at(today, 15).instant
        val work = projects.createProject("Работа")

        create("Вчера весь день", TaskDue.AllDay(today.minusDays(1)), project = work)
        create("Вчера 23:00", at(today.minusDays(1), 23), project = work)
        create("Сегодня 09:00", at(today, 9), project = work)
        create("Сегодня весь день", TaskDue.AllDay(today), project = work)
        create("Сегодня 23:59", at(today, 23, 59), project = work)
        create("Завтра 00:00", at(today.plusDays(1), 0), project = work)
        create("Завтра весь день", TaskDue.AllDay(today.plusDays(1)), project = work)
        create("Без даты", null, project = work)

        val sections = tasks.observeAllScreen(today).first()
        assertEquals(listOf("Вчера весь день", "Вчера 23:00"), sections.overdue.map { it.title })
        assertEquals(listOf("Сегодня весь день"), sections.todayAllDay.map { it.title })
        // Прошедшее сегодняшнее время не считается просрочкой: условие — due_date < начало сегодняшнего дня.
        assertEquals(listOf("Сегодня 09:00", "Сегодня 23:59"), sections.todayTimed.map { it.title })
        assertEquals(setOf("Завтра 00:00", "Завтра весь день"), sections.upcoming.map { it.title }.toSet())
        val all = sections.overdue + sections.todayAllDay + sections.todayTimed + sections.upcoming
        assertTrue(all.none { it.title == "Без даты" })
    }

    @Test
    fun completedAndDeletedTasksAreHidden() = runTest {
        val done = create("Выполнена", TaskDue.AllDay(today.minusDays(1)))
        val deleted = create("Удалена", TaskDue.AllDay(today))
        create("Активная", TaskDue.AllDay(today))
        tasks.setCompleted(done, completed = true)
        tasks.deleteTask(deleted)

        val sections = tasks.observeAllScreen(today).first()
        assertTrue(sections.overdue.isEmpty())
        assertEquals(listOf("Активная"), sections.todayAllDay.map { it.title })
    }

    @Test
    fun allDayTaskStaysOnItsDateInAnyTimeZone() = runTest {
        create("13 октября", TaskDue.AllDay(today))

        listOf("Pacific/Kiritimati", "Etc/GMT+12", "Europe/Moscow", "America/New_York").forEach { zoneId ->
            time.zone = ZoneId.of(zoneId)
            val sections = tasks.observeAllScreen(today).first()
            assertEquals(zoneId, listOf("13 октября"), sections.todayAllDay.map { it.title })
            assertTrue(zoneId, sections.overdue.isEmpty() && sections.upcoming.isEmpty())
        }
    }

    @Test
    fun timedTaskBoundariesFollowUserTimeZone() = runTest {
        time.zone = ZoneId.of("America/New_York")
        // 23:30 13 октября в Нью-Йорке — это уже 14 октября по Москве и по UTC.
        create("Поздно вечером", at(today, 23, 30, zone = time.zone))

        val sections = tasks.observeAllScreen(today).first()
        assertEquals(listOf("Поздно вечером"), sections.todayTimed.map { it.title })
    }

    @Test
    fun sectionsAreSorted() = runTest {
        create("Низкий", TaskDue.AllDay(today), Priority.LOW)
        create("Высокий", TaskDue.AllDay(today), Priority.HIGH)
        create("Без приоритета", TaskDue.AllDay(today), Priority.NONE)
        create("18:00", at(today, 18))
        create("10:00", at(today, 10))

        val sections = tasks.observeAllScreen(today).first()
        assertEquals(listOf("Высокий", "Низкий", "Без приоритета"), sections.todayAllDay.map { it.title })
        assertEquals(listOf("10:00", "18:00"), sections.todayTimed.map { it.title })
    }

    @Test
    fun sectionsUpdateLiveWhenTaskIsCompleted() = runTest {
        val id = create("Сегодня", TaskDue.AllDay(today))

        tasks.observeAllScreen(today).test {
            assertEquals(listOf("Сегодня"), awaitItem().todayAllDay.map { it.title })
            tasks.setCompleted(id, completed = true)
            assertTrue(awaitItem().todayAllDay.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun inboxShowsInboxTasksAndUndatedTasksFromOtherLists() = runTest {
        val work = projects.createProject("Работа")
        create("Во входящих с датой", TaskDue.AllDay(today))
        create("Во входящих без даты", null)
        create("Работа без даты", null, project = work)
        create("Работа с датой", TaskDue.AllDay(today), project = work)
        val done = create("Выполненная без даты", null)
        tasks.setCompleted(done, completed = true)

        val inbox = tasks.observeInbox().first().map { it.title }.toSet()
        assertEquals(setOf("Во входящих с датой", "Во входящих без даты", "Работа без даты"), inbox)
    }
}
