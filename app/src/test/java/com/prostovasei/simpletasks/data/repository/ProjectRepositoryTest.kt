package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.data.local.db.InboxSeed
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProjectRepositoryTest : DatabaseTest() {

    @Test
    fun treeGroupsProjectsAndCountsOpenTasks() = runTest {
        val folder = projects.createGroup("Личное")
        val home = projects.createProject("Дом", groupId = folder, color = "#3D8BFD")
        val sport = projects.createProject("Спорт", groupId = folder)
        val work = projects.createProject("Работа")
        tasks.createTask(NewTask(projectId = home, title = "Купить продукты"))
        tasks.createTask(NewTask(projectId = home, title = "Починить кран"))
        val done = tasks.createTask(NewTask(projectId = home, title = "Готово"))
        tasks.setCompleted(done, completed = true)
        val deleted = tasks.createTask(NewTask(projectId = work, title = "Удалена"))
        tasks.deleteTask(deleted)

        val tree = projects.observeProjectTree().first()
        assertEquals(InboxSeed.INBOX_ID, tree.inbox?.project?.id)
        assertEquals(listOf("Личное"), tree.groups.map { it.group.name })
        val inFolder = tree.groups.single().projects
        assertEquals(listOf(home, sport), inFolder.map { it.project.id })
        assertEquals(listOf(2, 0), inFolder.map { it.openTaskCount })
        assertEquals("#3D8BFD", inFolder.first().project.color)
        assertEquals(listOf(work), tree.ungrouped.map { it.project.id })
        assertEquals(0, tree.ungrouped.single().openTaskCount)
    }

    @Test
    fun renameAndUpdateBumpUpdatedAt() = runTest {
        val folder = projects.createGroup("Папка")
        val list = projects.createProject("Список")
        time.advance()

        projects.renameGroup(folder, "Новая папка")
        projects.updateProject(list, "Новый список", groupId = folder, color = "#FF0000")

        val tree = projects.observeProjectTree().first()
        val group = tree.groups.single()
        assertEquals("Новая папка", group.group.name)
        assertEquals(time.now, group.group.updatedAt)
        val project = group.projects.single().project
        assertEquals("Новый список", project.name)
        assertEquals("#FF0000", project.color)
        assertEquals(time.now, project.updatedAt)
    }

    @Test
    fun deletingGroupKeepsItsProjects() = runTest {
        val folder = projects.createGroup("Папка")
        val list = projects.createProject("Список", groupId = folder)
        tasks.createTask(NewTask(projectId = list, title = "Задача"))

        projects.deleteGroup(folder)

        val tree = projects.observeProjectTree().first()
        assertTrue(tree.groups.isEmpty())
        assertEquals(list, tree.ungrouped.single().project.id)
        assertNull(tree.ungrouped.single().project.groupId)
        assertEquals(1, tasks.observeProjectTasks(list, time.today()).first().size)
    }

    @Test
    fun deletingProjectCascadesToTasksAndChecklist() = runTest {
        val today = LocalDate.of(2026, 10, 13)
        val list = projects.createProject("Список")
        val dated = tasks.createTask(NewTask(projectId = list, title = "С датой", due = TaskDue.AllDay(today), checklist = listOf("Пункт")))
        tasks.createTask(NewTask(projectId = list, title = "Без даты"))
        val itemId = tasks.observeTaskDetails(dated).first()!!.checklist.single().id

        time.advance()
        projects.deleteProject(list)

        assertTrue(projects.observeProjectTree().first().ungrouped.isEmpty())
        val sections = tasks.observeAllScreen(today).first()
        assertTrue(sections.todayAllDay.isEmpty())
        assertTrue(tasks.observeInbox(time.today()).first().isEmpty())
        assertNull(tasks.observeTaskDetails(dated).first())
        val item = db.checklistDao().getById(itemId)!!
        assertTrue(item.isDeleted)
        assertEquals(time.now.toEpochMilli(), item.updatedAt)
        assertTrue(db.projectDao().getById(list)!!.isDeleted)
    }

    @Test
    fun inboxCannotBeDeleted() = runTest {
        val result = runCatching { projects.deleteProject(InboxSeed.INBOX_ID) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(InboxSeed.INBOX_ID, projects.getInbox().id)
    }

    @Test
    fun newProjectsGoToEndOfTheirGroup() = runTest {
        val folder = projects.createGroup("Папка")
        val first = projects.createProject("Первый", groupId = folder)
        val second = projects.createProject("Второй", groupId = folder)
        val outside = projects.createProject("Снаружи")

        val tree = projects.observeProjectTree().first()
        assertEquals(listOf(first, second), tree.groups.single().projects.map { it.project.id })
        assertEquals(listOf(0, 1), tree.groups.single().projects.map { it.project.sortOrder })
        assertEquals(1, tree.ungrouped.single { it.project.id == outside }.project.sortOrder)
    }
}
