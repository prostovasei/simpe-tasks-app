package com.prostovasei.simpletasks.ui.feature.lists

import app.cash.turbine.test
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val projects = FakeProjectRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun foldersCollapseAndExpand() = runTest(dispatcher) {
        val vm = ListsViewModel(projects, FakeTaskRepository())
        vm.onIntent(ListsIntent.ToggleFolder("g1"))
        assertEquals(setOf("g1"), vm.state.value.collapsedFolders)
        vm.onIntent(ListsIntent.ToggleFolder("g1"))
        assertTrue(vm.state.value.collapsedFolders.isEmpty())
    }

    @Test
    fun createdListOpensImmediately() = runTest(dispatcher) {
        val vm = ListsViewModel(projects, FakeTaskRepository())
        runCurrent()
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.CreateList))

        vm.effects.test {
            vm.onIntent(ListsIntent.CreateList("Дом", folderId = "g7", emoji = "🏠"))
            runCurrent()
            assertEquals(ListsEffect.OpenProject("p1"), awaitItem())
        }
        assertEquals(ListsDialog.None, vm.state.value.dialog)
        assertEquals("createProject:Дом:g7:🏠", projects.calls.single())
        assertEquals("🏠", vm.state.value.tree!!.ungrouped.single().project.emojiShown)
        assertEquals(listOf("Дом"), vm.state.value.tree!!.ungrouped.map { it.project.name })
    }

    @Test
    fun folderCreateRenameDelete() = runTest(dispatcher) {
        val vm = ListsViewModel(projects, FakeTaskRepository())

        vm.onIntent(ListsIntent.CreateFolder("Личное", "🧩"))
        runCurrent()
        assertEquals("🧩", vm.state.value.tree!!.groups.single().group.emojiShown)
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.EditFolder("g1", "Личное", "🧩")))
        vm.onIntent(ListsIntent.UpdateFolder("Дом и семья", "🏠"))
        runCurrent()
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.DeleteFolder("g1", "Дом и семья")))
        vm.onIntent(ListsIntent.ConfirmDeleteFolder)
        runCurrent()

        assertEquals(listOf("createGroup:Личное:🧩", "updateGroup:g1:Дом и семья:🏠", "deleteGroup:g1"), projects.calls)
    }

    @Test
    fun renameWithoutOpenDialogIsIgnored() = runTest(dispatcher) {
        val vm = ListsViewModel(projects, FakeTaskRepository())
        vm.onIntent(ListsIntent.UpdateFolder("Х", null))
        vm.onIntent(ListsIntent.ConfirmDeleteFolder)
        runCurrent()
        assertTrue(projects.calls.isEmpty())
    }

    @Test
    fun sortCountsReachRows() = runTest(dispatcher) {
        val tasks = FakeTaskRepository()
        val vm = ListsViewModel(projects, tasks)
        runCurrent()
        // Итерация 56 (T-125): подписка на числа была вложена в другую и не запускалась — у «Входящих» не было числа.
        val day = com.prostovasei.simpletasks.domain.model.TaskDue.AllDay(java.time.LocalDate.of(2026, 10, 14))
        tasks.tasks.value = listOf(
            com.prostovasei.simpletasks.testing.task("a", projectId = "inbox"),
            com.prostovasei.simpletasks.testing.task("b", projectId = "inbox"),
            com.prostovasei.simpletasks.testing.task("c", projectId = "work"),
            com.prostovasei.simpletasks.testing.task("d", projectId = "inbox", due = day),
        )
        runCurrent()
        assertEquals(2, vm.state.value.inboxCount)
        assertEquals(1, vm.state.value.somedayCount)
        assertEquals(1, vm.state.value.noListCount)
    }
}
