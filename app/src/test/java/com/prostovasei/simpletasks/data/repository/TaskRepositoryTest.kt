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
    fun createIntervalKeepsStart() = runTest {
        val start = TaskDue.AtTime(Instant.parse("2026-10-15T16:00:00Z"))
        val end = TaskDue.AtTime(Instant.parse("2026-10-15T17:30:00Z"))
        val id = tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Встреча", due = end, start = start))
        val created = tasks.observeTaskDetails(id).first()!!.task
        assertEquals(start, created.start)
        assertEquals(end, created.due)
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
        assertTrue(tasks.observeInbox(time.today()).first().isEmpty())
        val taskRow = db.taskDao().getById(id)!!
        val itemRow = db.checklistDao().getById(itemId)!!
        assertTrue(taskRow.isDeleted)
        assertTrue(itemRow.isDeleted)
        assertEquals(time.now.toEpochMilli(), taskRow.updatedAt)
        assertEquals(time.now.toEpochMilli(), itemRow.updatedAt)
    }

    @Test
    fun newTasksGoToEndOfProject() = runTest {
        val inbox = projects.getInbox().id
        tasks.createTask(NewTask(projectId = inbox, title = "Первая"))
        tasks.createTask(NewTask(projectId = inbox, title = "Вторая"))
        tasks.createTask(NewTask(projectId = inbox, title = "Третья"))

        val list = tasks.observeProjectTasks(inbox, time.today()).first()
        assertEquals(listOf("Первая", "Вторая", "Третья"), list.map { it.title })
        assertEquals(listOf(0, 1, 2), list.map { it.sortOrder })
    }

    @Test
    fun blankTitleIsRejected() = runTest {
        val result = runCatching { tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "  ")) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun undoDeleteRestoresTaskSubtasksAndChecklist() = runTest {
        val inbox = projects.getInbox().id
        val parent = tasks.createTask(NewTask(projectId = inbox, title = "Родитель", checklist = listOf("Пункт 1", "Пункт 2")))
        val child = tasks.createTask(NewTask(projectId = inbox, title = "Подзадача", checklist = listOf("Дочерний пункт")))
        tasks.updateTask(tasks.observeTaskDetails(child).first()!!.task.copy(parentId = parent))
        // Подзадача, удалённая раньше, при «Отмене» не оживает.
        val old = tasks.createTask(NewTask(projectId = inbox, title = "Старая"))
        tasks.updateTask(tasks.observeTaskDetails(old).first()!!.task.copy(parentId = parent))
        tasks.deleteTask(old)
        time.advance()

        val deletion = tasks.deleteTask(parent)
        assertNull(tasks.getTask(parent))
        assertNull(tasks.getTask(child))
        assertTrue(tasks.observeProjectTasks(inbox, time.today()).first().isEmpty())

        time.advance()
        tasks.undoDelete(deletion)

        assertEquals("Родитель", tasks.getTask(parent)?.title)
        assertEquals("Подзадача", tasks.getTask(child)?.title)
        assertNull("Раньше удалённое осталось удалённым", tasks.getTask(old))
        assertEquals(listOf("Пункт 1", "Пункт 2"), tasks.observeTaskDetails(parent).first()!!.checklist.map { it.title })
        assertEquals(listOf("Дочерний пункт"), tasks.observeTaskDetails(child).first()!!.checklist.map { it.title })
    }

    @Test
    fun moveOverdueToTodayMovesPastDaysKeepingTimeAndUndoRestores() = runTest {
        val inbox = projects.getInbox().id
        val today = time.today()
        suspend fun create(title: String, due: TaskDue?) = tasks.createTask(NewTask(projectId = inbox, title = title, due = due))
        val pastAllDay = create("Весь день", TaskDue.AllDay(today.minusDays(3)))
        val pastTimed = create("Со временем", TaskDue.AtTime(Instant.parse("2026-10-11T07:30:00Z")))
        val earlierToday = create("Сегодня раньше", TaskDue.AtTime(Instant.parse("2026-10-13T06:00:00Z")))
        val future = create("Завтра", TaskDue.AllDay(today.plusDays(1)))
        val noDate = create("Без даты", null)
        val done = create("Сделана", TaskDue.AllDay(today.minusDays(2)))
        tasks.setCompleted(done, completed = true)

        val moved = tasks.moveOverdueToToday()

        assertEquals(setOf(pastAllDay, pastTimed), moved.previous.map { it.id }.toSet())
        assertEquals(2, moved.count)
        assertEquals(TaskDue.AllDay(today), tasks.getTask(pastAllDay)?.due)
        assertEquals("Время дня то же (10:30 по Москве)", TaskDue.AtTime(Instant.parse("2026-10-13T07:30:00Z")), tasks.getTask(pastTimed)?.due)
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-13T06:00:00Z")), tasks.getTask(earlierToday)?.due)
        assertEquals(TaskDue.AllDay(today.plusDays(1)), tasks.getTask(future)?.due)
        assertNull(tasks.getTask(noDate)?.due)
        assertEquals(TaskDue.AllDay(today.minusDays(2)), tasks.getTask(done)?.due)
        assertTrue("Второе нажатие ничего не находит", tasks.moveOverdueToToday().previous.isEmpty())

        tasks.undoReschedule(moved)

        assertEquals(TaskDue.AllDay(today.minusDays(3)), tasks.getTask(pastAllDay)?.due)
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-11T07:30:00Z")), tasks.getTask(pastTimed)?.due)
    }
}
