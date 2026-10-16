package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Экран «Выполненные» (итерация 11): список и возврат задачи. */
class CompletedTasksTest : DatabaseTest() {

    private suspend fun create(title: String, rrule: String? = null) =
        tasks.createTask(NewTask(projects.getInbox().id, title, due = TaskDue.AllDay(LocalDate.of(2026, 10, 13)), rrule = rrule))

    @Test
    fun completedAreNewestFirstAndActiveAreHidden() = runTest {
        val first = create("Первая")
        val second = create("Вторая")
        create("Открытая")
        tasks.setCompleted(first, true)
        time.advance()
        tasks.setCompleted(second, true)

        assertEquals(listOf("Вторая", "Первая"), tasks.observeCompleted().first().map { it.title })
    }

    @Test
    fun restoreReturnsTaskAndUndoCompletesItAgain() = runTest {
        val id = create("Отчёт")
        tasks.setCompleted(id, true)
        val completedAt = tasks.getTask(id)!!.completedAt

        time.advance()
        val previous = tasks.restore(id)!!
        val restored = tasks.getTask(id)!!
        assertEquals(TaskStatus.ACTIVE, restored.status)
        assertNull(restored.completedAt)
        assertEquals(emptyList<String>(), tasks.observeCompleted().first().map { it.id })

        tasks.undoRestore(previous)
        val again = tasks.getTask(id)!!
        assertEquals(TaskStatus.COMPLETED, again.status)
        assertEquals(completedAt, again.completedAt)
    }

    @Test
    fun restoredRepeatBecomesOneTimeTask() = runTest {
        val id = create("Зарядка", rrule = "FREQ=DAILY;COUNT=1")
        tasks.complete(id)
        assertEquals(TaskStatus.COMPLETED, tasks.getTask(id)!!.status)

        val previous = tasks.restore(id)!!
        assertNull("Возвращённая задача без повтора", tasks.getTask(id)!!.rrule)

        tasks.undoRestore(previous)
        assertEquals(previous.rrule, tasks.getTask(id)!!.rrule)
    }

    @Test
    fun activeTaskIsNotRestored() = runTest {
        assertNull(tasks.restore(create("Открытая")))
    }
}
