package com.prostovasei.simpletasks.ui.feature.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val tasks = FakeTaskRepository()
    private val projects = FakeProjectRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        projects.tree.value = projects.tree.value.copy(
            ungrouped = listOf(ProjectWithCount(project("work", "Работа", groupId = "g1", color = "#3D8BFD"), 2)),
        )
        tasks.tasks.value = listOf(task("a", projectId = "work"), task("b", projectId = "work"), task("c"))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(projectId: String = "work") = ProjectViewModel(
        SavedStateHandle(mapOf("projectId" to projectId)),
        tasks,
        projects,
        DayTicker { flowOf(LocalDate.of(2026, 10, 13)) },
        FakeTimeProvider(),
    )

    @Test
    fun showsProjectAndItsTasks() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        assertEquals("Работа", vm.state.value.name)
        assertEquals("#3D8BFD", vm.state.value.color)
        assertEquals(listOf("a", "b"), vm.state.value.tasks.map { it.id })
    }

    @Test
    fun renameKeepsFolderAndColor() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(ProjectIntent.OpenDialog(ProjectDialog.Rename))
        assertEquals(ProjectDialog.Rename, vm.state.value.dialog)
        vm.onIntent(ProjectIntent.Rename("Офис"))
        runCurrent()

        assertEquals(ProjectDialog.None, vm.state.value.dialog)
        assertEquals("updateProject:work:Офис:g1:#3D8BFD", projects.calls.last())
        assertEquals("Офис", vm.state.value.name)
    }

    @Test
    fun colorCanBeSetAndRemoved() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(ProjectIntent.SetColor("#E5484D"))
        runCurrent()
        assertEquals("#E5484D", vm.state.value.color)

        vm.onIntent(ProjectIntent.SetColor(null))
        runCurrent()
        assertEquals("updateProject:work:Работа:g1:null", projects.calls.last())
    }

    @Test
    fun iconIsSavedAndDialogCloses() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(ProjectIntent.OpenDialog(ProjectDialog.Icon))

        vm.onIntent(ProjectIntent.SetIcon("work"))
        runCurrent()
        assertEquals("icon:work:work", projects.calls.last())
        assertEquals(ProjectDialog.None, vm.state.value.dialog)

        vm.onIntent(ProjectIntent.SetIcon(null))
        runCurrent()
        assertEquals("icon:work:null", projects.calls.last())
    }

    @Test
    fun deleteClosesScreen() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.effects.test {
            vm.onIntent(ProjectIntent.ConfirmDelete)
            runCurrent()
            assertEquals(ProjectEffect.Close, awaitItem())
        }
        assertEquals("deleteProject:work", projects.calls.last())
    }

    @Test
    fun inboxCannotBeDeleted() = runTest(dispatcher) {
        val vm = viewModel("inbox")
        runCurrent()

        vm.onIntent(ProjectIntent.ConfirmDelete)
        runCurrent()

        assertEquals(emptyList<String>(), projects.calls)
    }
}
