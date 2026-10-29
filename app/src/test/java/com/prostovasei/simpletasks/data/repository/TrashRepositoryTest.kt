package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Корзина (итерация 39, бэклог №35): что в ней лежит, возврат в список, стирание навсегда. */
class TrashRepositoryTest : DatabaseTest() {

    private suspend fun create(title: String, projectId: String, checklist: List<String> = emptyList()) =
        tasks.createTask(NewTask(projectId = projectId, title = title, checklist = checklist))

    /** Подзадача: родитель задаётся правкой (как в тестах удаления). */
    private suspend fun subtaskOf(parent: String, title: String, projectId: String): String {
        val id = create(title, projectId, listOf("Пункт подзадачи"))
        tasks.updateTask(tasks.observeTaskDetails(id).first()!!.task.copy(parentId = parent))
        return id
    }

    @Test
    fun trashHoldsOnlyRootsNewestFirstAndHidesThemEverywhereElse() = runTest {
        val inbox = projects.getInbox().id
        val parent = create("Родитель", inbox)
        val child = subtaskOf(parent, "Подзадача", inbox)
        val other = create("Другая", inbox)
        val alive = create("Живая", inbox)

        tasks.deleteTask(other)
        time.advance()
        tasks.deleteTask(parent)

        assertEquals("Корни, новые сверху; подзадача отдельно не видна", listOf(parent, other), tasks.observeTrash().first().map { it.id })
        assertEquals(2, tasks.observeTrashCount().first())
        assertEquals(listOf(alive), tasks.observeProjectTasks(inbox, time.today()).first().map { it.id })
        assertTrue("Из корзины задача не ищется", tasks.observeSearchable().first().none { it.task.id in setOf(parent, child, other) })

        val trashed = tasks.observeTrashed(parent).first()!!
        assertEquals("Родитель", trashed.details.task.title)
        assertEquals(listOf("Подзадача"), trashed.subtasks.map { it.title })
        assertNull("Живую задачу как удалённую не показываем", tasks.observeTrashed(alive).first())
    }

    @Test
    fun restoreBringsTreeBackToItsListAndUndoPutsItBackInTrash() = runTest {
        val inbox = projects.getInbox().id
        val work = projects.createProject("Работа")
        val parent = create("Родитель", work, listOf("Пункт 1", "Пункт 2"))
        val child = subtaskOf(parent, "Подзадача", work)
        tasks.deleteTask(parent)
        val deletedAt = tasks.observeTrash().first().single().updatedAt
        time.advance()

        val restoration = tasks.restoreFromTrash(parent)!!

        assertEquals(1, restoration.count)
        assertEquals(emptyList<String>(), tasks.observeTrash().first().map { it.id })
        assertEquals(work, tasks.getTask(parent)?.projectId)
        assertEquals("Подзадача", tasks.getTask(child)?.title)
        assertEquals(listOf("Пункт 1", "Пункт 2"), tasks.observeTaskDetails(parent).first()!!.checklist.map { it.title })
        assertNull("Второй раз вернуть нечего", tasks.restoreFromTrash(parent))

        tasks.undoRestoreFromTrash(restoration)

        assertNull(tasks.getTask(parent))
        assertEquals("Снова в корзине с прежним временем удаления", listOf(parent to deletedAt), tasks.observeTrash().first().map { it.id to it.updatedAt })
    }

    @Test
    fun taskOfDeletedListReturnsToInbox() = runTest {
        val inbox = projects.getInbox().id
        val doomed = projects.createProject("Старый список")
        val parent = create("Из удалённого списка", doomed)
        val child = subtaskOf(parent, "Её подзадача", doomed)
        projects.deleteProject(doomed)
        assertEquals(listOf(parent), tasks.observeTrash().first().map { it.id })

        tasks.restoreFromTrash(parent)

        assertEquals(inbox, tasks.getTask(parent)?.projectId)
        assertEquals("Подзадача уезжает вместе с родителем", inbox, tasks.getTask(child)?.projectId)
    }

    @Test
    fun restoreAllReturnsEveryRootAndUndoTrashesThemAgain() = runTest {
        val inbox = projects.getInbox().id
        val a = create("А", inbox)
        val b = create("Б", inbox)
        tasks.deleteTask(a)
        time.advance()
        tasks.deleteTask(b)

        val restoration = tasks.restoreAllFromTrash()

        assertEquals(2, restoration.count)
        assertEquals(0, tasks.observeTrashCount().first())
        assertNotNull(tasks.getTask(a))
        assertNotNull(tasks.getTask(b))

        tasks.undoRestoreFromTrash(restoration)
        assertEquals(setOf(a, b), tasks.observeTrash().first().map { it.id }.toSet())
    }

    @Test
    fun deleteForeverErasesTreeAndChecklistButNotOthers() = runTest {
        val inbox = projects.getInbox().id
        val parent = create("Родитель", inbox, listOf("Пункт"))
        val child = subtaskOf(parent, "Подзадача", inbox)
        val keep = create("Другая в корзине", inbox)
        val alive = create("Живая", inbox, listOf("Живой пункт"))
        tasks.deleteTask(parent)
        tasks.deleteTask(keep)

        tasks.deleteForever(parent)

        assertNull(db.taskDao().getById(parent))
        assertNull(db.taskDao().getById(child))
        assertEquals(listOf(keep), tasks.observeTrash().first().map { it.id })
        tasks.deleteForever(alive)
        assertNotNull("Живую задачу так не стереть", tasks.getTask(alive))
        assertEquals(listOf("Живой пункт"), tasks.observeTaskDetails(alive).first()!!.checklist.map { it.title })
    }

    @Test
    fun emptyTrashErasesEverythingInIt() = runTest {
        val inbox = projects.getInbox().id
        val a = create("А", inbox, listOf("Пункт"))
        val child = subtaskOf(a, "Подзадача", inbox)
        val b = create("Б", inbox)
        val alive = create("Живая", inbox)
        tasks.deleteTask(a)
        tasks.deleteTask(b)

        assertEquals(2, tasks.emptyTrash())

        assertNull(db.taskDao().getById(a))
        assertNull(db.taskDao().getById(child))
        assertNull(db.taskDao().getById(b))
        assertEquals(0, tasks.observeTrashCount().first())
        assertNotNull(tasks.getTask(alive))
        assertEquals(0, tasks.emptyTrash())
    }

    /** Удалённая задача не звонит; вернули — напоминание снова в списке будильника. */
    @Test
    fun trashedTaskHasNoRemindersUntilRestored() = runTest {
        val inbox = projects.getInbox().id
        val id = tasks.createTask(
            NewTask(projectId = inbox, title = "С напоминанием", due = TaskDue.AtTime(Instant.parse("2026-10-20T10:00:00Z")), reminders = listOf("PT0S")),
        )
        assertEquals(listOf(id), tasks.observeWithReminders().first().map { it.id })

        tasks.deleteTask(id)
        assertEquals(emptyList<String>(), tasks.observeWithReminders().first().map { it.id })

        tasks.restoreFromTrash(id)
        assertEquals(listOf(id), tasks.observeWithReminders().first().map { it.id })
    }
}
