package com.prostovasei.simpletasks.ui.feature.lists

import app.cash.turbine.test
import com.prostovasei.simpletasks.testing.FakeProjectRepository
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
        val vm = ListsViewModel(projects)
        vm.onIntent(ListsIntent.ToggleFolder("g1"))
        assertEquals(setOf("g1"), vm.state.value.collapsedFolders)
        vm.onIntent(ListsIntent.ToggleFolder("g1"))
        assertTrue(vm.state.value.collapsedFolders.isEmpty())
    }

    @Test
    fun createdListOpensImmediately() = runTest(dispatcher) {
        val vm = ListsViewModel(projects)
        runCurrent()
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.CreateList))

        vm.effects.test {
            vm.onIntent(ListsIntent.CreateList("Дом", folderId = "g7"))
            runCurrent()
            assertEquals(ListsEffect.OpenProject("p1"), awaitItem())
        }
        assertEquals(ListsDialog.None, vm.state.value.dialog)
        assertEquals("createProject:Дом:g7", projects.calls.single())
        assertEquals(listOf("Дом"), vm.state.value.tree!!.ungrouped.map { it.project.name })
    }

    @Test
    fun folderCreateRenameDelete() = runTest(dispatcher) {
        val vm = ListsViewModel(projects)

        vm.onIntent(ListsIntent.CreateFolder("Личное"))
        runCurrent()
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.RenameFolder("g1", "Личное")))
        vm.onIntent(ListsIntent.RenameFolder("Дом и семья"))
        runCurrent()
        vm.onIntent(ListsIntent.OpenDialog(ListsDialog.DeleteFolder("g1", "Дом и семья")))
        vm.onIntent(ListsIntent.ConfirmDeleteFolder)
        runCurrent()

        assertEquals(listOf("createGroup:Личное", "renameGroup:g1:Дом и семья", "deleteGroup:g1"), projects.calls)
    }

    @Test
    fun renameWithoutOpenDialogIsIgnored() = runTest(dispatcher) {
        val vm = ListsViewModel(projects)
        vm.onIntent(ListsIntent.RenameFolder("Х"))
        vm.onIntent(ListsIntent.ConfirmDeleteFolder)
        runCurrent()
        assertTrue(projects.calls.isEmpty())
    }
}
