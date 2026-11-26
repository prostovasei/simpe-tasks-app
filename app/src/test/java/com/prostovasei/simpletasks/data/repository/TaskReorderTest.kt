package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.SortScope
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.testing.DatabaseTest
import com.prostovasei.simpletasks.testing.FakeTaskSortStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** #357: ручной порядок сохраняется в задачах; при «Вручную» новая задача встаёт в начало, иначе — в конец. */
class TaskReorderTest : DatabaseTest() {
    private val sorts = FakeTaskSortStore()

    private suspend fun order(projectId: String) = tasks.observeProjectTasks(projectId, time.today()).first().sortedBy { it.sortOrder }.map { it.title }

    @Test
    fun reorderAndNewTaskOnTopInManualMode() = runTest {
        val repo = RoomTaskRepository(db, time, UuidIdGenerator(), sorts)
        val project = projects.createProject("Работа", null, null)
        val a = repo.createTask(NewTask(project, "А"))
        val b = repo.createTask(NewTask(project, "Б"))
        val c = repo.createTask(NewTask(project, "В"))
        assertEquals(listOf("А", "Б", "В"), order(project))

        repo.reorderTasks(listOf(c, a, b))
        assertEquals(listOf("В", "А", "Б"), order(project))

        sorts.set(SortScope.Project(project), TaskSort(SortBy.MANUAL))
        repo.createTask(NewTask(project, "Новая"))
        assertEquals(listOf("Новая", "В", "А", "Б"), order(project))

        val inbox = projects.getInbox().id
        sorts.set(SortScope.Inbox, TaskSort(SortBy.MANUAL))
        repo.createTask(NewTask(inbox, "Первая"))
        repo.createTask(NewTask(inbox, "Вторая"))
        assertEquals(listOf("Вторая", "Первая"), tasks.observeInbox(time.today()).first().sortedBy { it.sortOrder }.map { it.title })
    }
}
