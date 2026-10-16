package com.prostovasei.simpletasks.ui.feature.completed

import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import com.prostovasei.simpletasks.ui.common.DayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CompletedViewModelTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val tasks = FakeTaskRepository(today)
    private val projects = FakeProjectRepository()

    private fun done(id: String, at: String?, projectId: String = "inbox") =
        task(id, projectId = projectId, status = TaskStatus.COMPLETED).copy(completedAt = at?.let(Instant::parse))

    private fun viewModel() = CompletedViewModel(tasks, projects, DayTicker { flowOf(today) }, FakeTimeProvider(zone = ZoneOffset.UTC))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        projects.tree.value = projects.tree.value.copy(ungrouped = listOf(ProjectWithCount(project("work", "Работа", color = "#3D8BFD"), 0)))
        tasks.tasks.value = listOf(
            done("a", "2026-10-14T09:05:00Z", projectId = "work"),
            done("b", "2026-10-13T18:00:00Z"),
            done("c", "2026-10-13T08:00:00Z"),
            task("open"),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun groupsByCompletionDay() = runTest {
        val days = viewModel().state.value.days
        assertEquals(listOf(DayLabel.Today, DayLabel.Yesterday), days.map { it.day })
        assertEquals(listOf(listOf("a"), listOf("b", "c")), days.map { d -> d.tasks.map { it.id } })
        val a = days[0].tasks[0]
        assertEquals("Работа", a.listName)
        assertEquals("09:05", a.time)
    }

    @Test
    fun importedWithoutCompletionTimeUsesUpdateDay() = runTest {
        tasks.tasks.value = listOf(done("old", null))
        val day = viewModel().state.value.days.single()
        assertEquals(DayLabel.Date(LocalDate.of(2026, 1, 1), showYear = false), day.day)
        assertEquals(null, day.tasks.single().time)
    }

    @Test
    fun uncheckRestoresAndUndoReturnsBack() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(CompletedIntent.Restore("b"))
            assertEquals(CompletedEffect.Restored, awaitItem())
        }
        assertEquals(listOf("a", "c"), vm.state.value.days.flatMap { d -> d.tasks.map { it.id } })

        vm.onIntent(CompletedIntent.Undo)
        assertEquals(listOf("a", "b", "c"), vm.state.value.days.flatMap { d -> d.tasks.map { it.id } }.sorted())
    }
}
