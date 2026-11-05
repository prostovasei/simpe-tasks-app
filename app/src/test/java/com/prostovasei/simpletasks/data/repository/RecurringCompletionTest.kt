package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** Выполнение повторяющейся задачи, «Отмена» и «На завтра». Сегодня — 13.10.2026, Москва. */
class RecurringCompletionTest : DatabaseTest() {
    private val today = LocalDate.of(2026, 10, 13)

    private suspend fun create(due: TaskDue?, rrule: String? = null, reminders: List<String> = emptyList(), checklist: List<String> = emptyList()) =
        tasks.createTask(NewTask(projects.getInbox().id, "Зарядка", due = due, rrule = rrule, reminders = reminders, checklist = checklist))

    private suspend fun count(sql: String) = db.openHelper.readableDatabase.query(sql).use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun recurringTaskMovesAndLeavesCompletedCopy() = runTest {
        val id = create(TaskDue.AllDay(today), rrule = "FREQ=DAILY", reminders = listOf("P0DT10H0M0S"), checklist = listOf("Разминка", "Бег"))
        val items = tasks.observeTaskDetails(id).first()!!.checklist
        checklist.setDone(items.first().id, true)

        val result = tasks.complete(id) as Completion.Rescheduled

        val task = tasks.observeTaskDetails(id).first()!!
        assertEquals(TaskDue.AllDay(today.plusDays(1)), task.task.due)
        assertEquals(TaskStatus.ACTIVE, task.task.status)
        assertEquals(listOf("P0DT10H0M0S"), task.task.reminders)
        assertTrue("Галочки чек-листа сняты", task.checklist.none { it.isDone })

        val copy = checkNotNull(tasks.getTask(result.doneCopyId))
        assertEquals(TaskStatus.COMPLETED, copy.status)
        assertEquals(TaskDue.AllDay(today), copy.due)
        assertNull("У копии нет повтора", copy.rrule)
        assertTrue("Копия — выполненный раз повтора: со знаком повтора", copy.isRepeatDone)
        val copyItems = tasks.observeTaskDetails(result.doneCopyId).first()!!.checklist
        assertEquals(listOf(true, false), copyItems.map { it.isDone })
    }

    /** Проверка PM (0.47.0): вернутый в работу раз повтора — обычная задача, без знака повтора. */
    @Test
    fun uncompletedRepeatCopyBecomesPlainTask() = runTest {
        val id = create(TaskDue.AllDay(today), rrule = "FREQ=DAILY")
        val result = tasks.complete(id) as Completion.Rescheduled

        tasks.setCompleted(result.doneCopyId, completed = false)

        val copy = checkNotNull(tasks.getTask(result.doneCopyId))
        assertEquals(TaskStatus.ACTIVE, copy.status)
        assertFalse(copy.isRepeatDone)
    }

    @Test
    fun undoRestoresEverything() = runTest {
        val id = create(TaskDue.AllDay(today), rrule = "FREQ=WEEKLY;COUNT=3", checklist = listOf("Разминка"))
        val item = tasks.observeTaskDetails(id).first()!!.checklist.single()
        checklist.setDone(item.id, true)
        val tasksBefore = count("SELECT COUNT(*) FROM tasks")

        val result = tasks.complete(id) as Completion.Rescheduled
        assertEquals("FREQ=WEEKLY;COUNT=2", tasks.getTask(id)!!.rrule)
        tasks.undo(result)

        val task = tasks.observeTaskDetails(id).first()!!
        assertEquals(TaskDue.AllDay(today), task.task.due)
        assertEquals("FREQ=WEEKLY;COUNT=3", task.task.rrule)
        assertTrue(task.checklist.single().isDone)
        assertEquals("Копия удалена", tasksBefore, count("SELECT COUNT(*) FROM tasks"))
        assertEquals(0, count("SELECT COUNT(*) FROM checklist_items WHERE task_id = '${result.doneCopyId}'"))
    }

    @Test
    fun lastRepeatCompletesTask() = runTest {
        val id = create(TaskDue.AllDay(today), rrule = "FREQ=DAILY;COUNT=1")
        assertEquals(Completion.Done(id), tasks.complete(id))
        assertEquals(TaskStatus.COMPLETED, tasks.getTask(id)!!.status)
        tasks.undo(Completion.Done(id))
        assertEquals(TaskStatus.ACTIVE, tasks.getTask(id)!!.status)
    }

    @Test
    fun plainTaskJustCompletes() = runTest {
        val id = create(TaskDue.AllDay(today))
        assertEquals(Completion.Done(id), tasks.complete(id))
        assertEquals(TaskStatus.COMPLETED, tasks.getTask(id)!!.status)
        assertEquals("Повторная отметка ничего не ломает", Completion.Done(id), tasks.complete(id))
    }

    @Test
    fun moveToTomorrowKeepsTime() = runTest {
        val moscow = time.zone
        val due = TaskDue.AtTime(LocalDateTime.of(2026, 10, 10, 18, 30).atZone(moscow).toInstant())
        val timed = create(due, reminders = listOf("PT0S"))
        tasks.moveToTomorrow(timed)
        assertEquals(TaskDue.AtTime(LocalDateTime.of(2026, 10, 14, 18, 30).atZone(moscow).toInstant()), tasks.getTask(timed)!!.due)
        assertEquals(listOf("PT0S"), tasks.getTask(timed)!!.reminders)

        val allDay = create(TaskDue.AllDay(today.minusDays(3)))
        tasks.moveToTomorrow(allDay)
        assertEquals(TaskDue.AllDay(today.plusDays(1)), tasks.getTask(allDay)!!.due)
    }

    @Test
    fun remindersAreSavedAndWatched() = runTest {
        val withReminder = create(TaskDue.AllDay(today), reminders = listOf("P0DT10H0M0S"))
        create(TaskDue.AllDay(today))
        create(null, reminders = listOf("PT0S"))
        assertEquals(listOf(withReminder), tasks.observeWithReminders().first().map { it.id })

        tasks.complete(withReminder)
        assertTrue(tasks.observeWithReminders().first().isEmpty())
        assertFalse(tasks.getTask(withReminder)!!.reminders.isEmpty())
    }

    @Test
    fun timedRepeatKeepsLocalTime() = runTest {
        time.now = Instant.parse("2026-10-13T06:00:00Z")
        val due = TaskDue.AtTime(LocalDateTime.of(2026, 10, 13, 9, 30).atZone(time.zone).toInstant())
        val id = create(due, rrule = "FREQ=WEEKLY;BYDAY=TU,TH")
        tasks.complete(id)
        assertEquals(TaskDue.AtTime(LocalDateTime.of(2026, 10, 15, 9, 30).atZone(time.zone).toInstant()), tasks.getTask(id)!!.due)
    }
}
