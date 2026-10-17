package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistRepositoryTest : DatabaseTest() {

    private suspend fun newTask(vararg items: String) =
        tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача", checklist = items.toList()))

    private suspend fun items(taskId: String) = tasks.observeTaskDetails(taskId).first()!!.checklist

    @Test
    fun addAppendsToEnd() = runTest {
        val task = newTask("Первый")
        checklist.addItem(task, "Второй")
        checklist.addItem(task, "Третий")

        assertEquals(listOf("Первый", "Второй", "Третий"), items(task).map { it.title })
    }

    @Test
    fun searchableTasksCarryChecklistWithoutDeletedItemsAndTasks() = runTest {
        val task = newTask("Паспорт", "Лишний")
        val gone = newTask("Удалённая")
        checklist.deleteItem(items(task)[1].id)
        tasks.deleteTask(gone)

        val found = tasks.observeSearchable().first()

        assertEquals(listOf(task), found.map { it.task.id })
        assertEquals(listOf("Паспорт"), found.single().checklist)
    }

    @Test
    fun renameMarkAndDelete() = runTest {
        val task = newTask("Пункт", "Лишний")
        val (item, extra) = items(task)
        time.advance()

        checklist.renameItem(item.id, "Переименован")
        checklist.setDone(item.id, done = true)
        checklist.deleteItem(extra.id)

        val result = items(task).single()
        assertEquals("Переименован", result.title)
        assertTrue(result.isDone)
        assertEquals(time.now, result.updatedAt)
        assertTrue(db.checklistDao().getById(extra.id)!!.isDeleted)
    }

    @Test
    fun reorderChangesOrder() = runTest {
        val task = newTask("A", "B", "C")
        val (a, b, c) = items(task)

        checklist.reorder(task, listOf(c.id, a.id, b.id))

        assertEquals(listOf("C", "A", "B"), items(task).map { it.title })
    }

    @Test
    fun reorderIgnoresItemsOfOtherTasks() = runTest {
        val task = newTask("A", "B")
        val other = newTask("Чужой")
        val (a, b) = items(task)
        val foreign = items(other).single()

        checklist.reorder(task, listOf(foreign.id, b.id, a.id))

        assertEquals(listOf("B", "A"), items(task).map { it.title })
        assertEquals(0, items(other).single().sortOrder)
    }
}
