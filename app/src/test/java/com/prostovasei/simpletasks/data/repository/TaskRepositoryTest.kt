package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TaskRepositoryTest : DatabaseTest() {

    @Test
    fun createdTaskIsReadBackWithChecklist() = runTest {
        val project = projects.createProject("Работа")
        val due = TaskDue.AtTime(Instant.parse("2026-10-14T09:30:00Z"))
        val id = tasks.createTask(
            NewTask(
                projectId = project,
                title = "Отчёт",
                content = "Квартальный",
                priority = Priority.HIGH,
                due = due,
                checklist = listOf("Собрать данные", "Отправить"),
            ),
        )

        val details = checkNotNull(tasks.observeTaskDetails(id).first())
        with(details.task) {
            assertEquals(project, projectId)
            assertEquals("Отчёт", title)
            assertEquals("Квартальный", content)
            assertEquals(Priority.HIGH, priority)
            assertEquals(due, this.due)
            assertEquals(TaskStatus.ACTIVE, status)
            assertEquals(time.now, createdAt)
            assertEquals(time.now, updatedAt)
        }
        assertEquals(listOf("Собрать данные", "Отправить"), details.checklist.map { it.title })
        assertTrue(details.checklist.all { it.taskId == id })
    }

    @Test
    fun updateChangesFieldsAndUpdatedAtOnly() = runTest {
        val inbox = projects.getInbox().id
        val work = projects.createProject("Работа")
        val id = tasks.createTask(NewTask(projectId = inbox, title = "Черновик"))
        val created = tasks.observeTaskDetails(id).first()!!.task

        time.advance()
        tasks.updateTask(
            created.copy(
                projectId = work,
                title = "Готово",
                content = "Описание",
                priority = Priority.MEDIUM,
                due = TaskDue.AllDay(LocalDate.of(2026, 10, 20)),
                rrule = "FREQ=WEEKLY",
            ),
        )

        val updated = tasks.observeTaskDetails(id).first()!!.task
        assertEquals(work, updated.projectId)
        assertEquals("Готово", updated.title)
        assertEquals("Описание", updated.content)
        assertEquals(Priority.MEDIUM, updated.priority)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 20)), updated.due)
        assertEquals("FREQ=WEEKLY", updated.rrule)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(time.now, updated.updatedAt)
        assertNotEquals(created.updatedAt, updated.updatedAt)
    }

    @Test
    fun completeAndUndo() = runTest {
        val id = tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача"))

        time.advance()
        tasks.setCompleted(id, completed = true)
        val completed = tasks.observeTaskDetails(id).first()!!.task
        assertEquals(TaskStatus.COMPLETED, completed.status)
        assertEquals(time.now, completed.completedAt)
        assertEquals(time.now, completed.updatedAt)

        time.advance()
        tasks.setCompleted(id, completed = false)
        val restored = tasks.observeTaskDetails(id).first()!!.task
        assertEquals(TaskStatus.ACTIVE, restored.status)
        assertNull(restored.completedAt)
        assertEquals(time.now, restored.updatedAt)
    }

    @Test
    fun deleteHidesTaskAndChecklistButKeepsRows() = runTest {
        val id = tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача", checklist = listOf("Пункт")))
        val itemId = tasks.observeTaskDetails(id).first()!!.checklist.single().id

        time.advance()
        tasks.deleteTask(id)

        assertNull(tasks.observeTaskDetails(id).first())
        assertTrue(tasks.observeInbox().first().isEmpty())
        val taskRow = db.taskDao().getById(id)!!
        val itemRow = db.checklistDao().getById(itemId)!!
        assertTrue(taskRow.isDeleted)
        assertTrue(itemRow.isDeleted)
        assertEquals(time.now.toEpochMilli(), taskRow.updatedAt)
        assertEquals(time.now.toEpochMilli(), itemRow.updatedAt)
    }

    @Test
    fun importSkipsDuplicateExternalId() = runTest {
        val inbox = projects.getInbox().id
        val task = NewTask(projectId = inbox, title = "Из TickTick", checklist = listOf("Пункт"))

        assertTrue(tasks.createImportedTask(task, externalId = "tt-1"))
        assertFalse(tasks.createImportedTask(task, externalId = "tt-1"))
        assertTrue(tasks.createImportedTask(task, externalId = "tt-2"))

        assertEquals(2, tasks.observeInbox().first().size)
    }

    @Test
    fun importSkipsTaskDeletedByUser() = runTest {
        val task = NewTask(projectId = projects.getInbox().id, title = "Из TickTick")
        tasks.createImportedTask(task, externalId = "tt-1")
        tasks.deleteTask(tasks.observeInbox().first().single().id)

        assertFalse(tasks.createImportedTask(task, externalId = "tt-1"))
        assertTrue(tasks.observeInbox().first().isEmpty())
    }

    @Test
    fun newTasksGoToEndOfProject() = runTest {
        val inbox = projects.getInbox().id
        tasks.createTask(NewTask(projectId = inbox, title = "Первая"))
        tasks.createTask(NewTask(projectId = inbox, title = "Вторая"))
        tasks.createTask(NewTask(projectId = inbox, title = "Третья"))

        val list = tasks.observeProjectTasks(inbox).first()
        assertEquals(listOf("Первая", "Вторая", "Третья"), list.map { it.title })
        assertEquals(listOf(0, 1, 2), list.map { it.sortOrder })
    }

    @Test
    fun blankTitleIsRejected() = runTest {
        val result = runCatching { tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "  ")) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
