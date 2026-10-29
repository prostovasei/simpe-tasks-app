package com.prostovasei.simpletasks.ui.feature.bulk

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Массовые действия (итерация 40): что делают с выбранными задачами и как «Отмена» всё возвращает. */
class BulkActionsTest : DatabaseTest() {

    private suspend fun create(title: String, due: TaskDue? = null, projectId: String? = null) =
        tasks.createTask(NewTask(projectId = projectId ?: projects.getInbox().id, title = title, due = due))

    private fun TestScope.actions() = BulkActions(tasks, time, this)

    @Test
    fun completeManyAndUndo() = runTest {
        val a = create("А")
        val b = create("Б", TaskDue.AllDay(time.today()))
        val actions = actions()
        actions.events.test {
            actions.complete(listOf(a, b))
            val result = awaitItem()
            assertEquals(BulkKind.Done, result.kind)
            assertEquals(2, result.count)
            assertEquals(TaskStatus.COMPLETED, tasks.getTask(a)?.status)
            assertEquals(TaskStatus.COMPLETED, tasks.getTask(b)?.status)

            result.undo()
            assertEquals(TaskStatus.ACTIVE, tasks.getTask(a)?.status)
            assertEquals(TaskStatus.ACTIVE, tasks.getTask(b)?.status)
        }
    }

    @Test
    fun tomorrowKeepsTimeAndGivesDateToUndatedThenUndo() = runTest {
        val timed = create("Со временем", TaskDue.AtTime(Instant.parse("2026-10-13T07:30:00Z")))
        val undated = create("Без даты")
        val actions = actions()
        actions.events.test {
            actions.toTomorrow(listOf(timed, undated))
            val result = awaitItem()
            assertEquals(BulkKind.Tomorrow, result.kind)
            assertEquals(TaskDue.AtTime(Instant.parse("2026-10-14T07:30:00Z")), tasks.getTask(timed)?.due)
            assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 14)), tasks.getTask(undated)?.due)

            result.undo()
            assertEquals(TaskDue.AtTime(Instant.parse("2026-10-13T07:30:00Z")), tasks.getTask(timed)?.due)
            assertNull(tasks.getTask(undated)?.due)
        }
    }

    @Test
    fun otherDayMoveAndPriorityThenUndo() = runTest {
        val work = projects.createProject("Работа")
        val a = create("А", TaskDue.AllDay(time.today()))
        val b = create("Б")
        val day = LocalDate.of(2026, 11, 3)
        val actions = actions()
        actions.events.test {
            actions.toDay(listOf(a), day)
            val moved = awaitItem()
            assertEquals(day, moved.date)
            assertEquals(TaskDue.AllDay(day), tasks.getTask(a)?.due)

            actions.moveTo(listOf(a, b), work, "Работа")
            val list = awaitItem()
            assertEquals("Работа", list.listName)
            assertEquals(listOf(work, work), listOf(tasks.getTask(a)?.projectId, tasks.getTask(b)?.projectId))

            actions.setPriority(listOf(a, b), Priority.HIGH)
            val priority = awaitItem()
            assertEquals(listOf(Priority.HIGH, Priority.HIGH), listOf(tasks.getTask(a)?.priority, tasks.getTask(b)?.priority))

            priority.undo()
            list.undo()
            moved.undo()
            assertEquals(Priority.NONE, tasks.getTask(a)?.priority)
            assertEquals(projects.getInbox().id, tasks.getTask(b)?.projectId)
            assertEquals(TaskDue.AllDay(time.today()), tasks.getTask(a)?.due)
        }
    }

    @Test
    fun reopenDeleteAndRestoreFromTrashWithUndo() = runTest {
        val done = create("Сделана")
        tasks.setCompleted(done, completed = true)
        val a = create("А")
        val b = create("Б")
        val actions = actions()
        actions.events.test {
            actions.reopen(listOf(done))
            val reopened = awaitItem()
            assertEquals(TaskStatus.ACTIVE, tasks.getTask(done)?.status)
            reopened.undo()
            assertEquals(TaskStatus.COMPLETED, tasks.getTask(done)?.status)

            actions.delete(listOf(a, b))
            val deleted = awaitItem()
            assertEquals(BulkKind.Deleted, deleted.kind)
            assertEquals(setOf(a, b), tasks.observeTrash().first().map { it.id }.toSet())
            deleted.undo()
            assertTrue(tasks.observeTrash().first().isEmpty())

            tasks.deleteTask(a)
            tasks.deleteTask(b)
            actions.restoreFromTrash(listOf(a, b))
            val restored = awaitItem()
            assertEquals(2, restored.count)
            assertTrue(tasks.observeTrash().first().isEmpty())
            restored.undo()
            assertEquals(2, tasks.observeTrashCount().first())

            actions.deleteForever(listOf(a))
            expectNoEvents()
        }
        assertEquals(listOf(b), tasks.observeTrash().first().map { it.id })
    }
}
