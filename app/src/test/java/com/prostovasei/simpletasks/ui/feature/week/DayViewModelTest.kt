package com.prostovasei.simpletasks.ui.feature.week

import androidx.lifecycle.SavedStateHandle
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Полный список дня недели (итерация 44): все задачи дня, выполненные тоже, «весь день» раньше задач со временем. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 10, 14)
    private val friday = LocalDate.of(2026, 10, 16)
    private val tasks = FakeTaskRepository(today)
    private val time = FakeTimeProvider(zone = ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tasks.tasks.value = listOf(
            task("evening", due = TaskDue.AtTime(Instant.parse("2026-10-16T20:00:00Z"))),
            task("allday", due = TaskDue.AllDay(friday)),
            task("done", due = TaskDue.AtTime(Instant.parse("2026-10-16T08:00:00Z")), status = TaskStatus.COMPLETED),
            task("thursday", due = TaskDue.AllDay(friday.minusDays(1))),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(date: LocalDate = friday) =
        DayViewModel(SavedStateHandle(mapOf("date" to date.toString())), tasks, FakeProjectRepository(), DayTicker { MutableStateFlow(today) }, time)

    @Test
    fun showsOnlyThatDaysTasksInOrder() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(friday, state.date)
        assertEquals(listOf("allday", "done", "evening"), state.items.map { it.id })
        assertTrue(state.items.first { it.id == "done" }.isDone)
    }

    @Test
    fun completingKeepsTheTaskInTheList() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(TaskListIntent.ToggleComplete("allday"))
        runCurrent()
        assertEquals(listOf("allday" to true), tasks.completedCalls)
        assertTrue(vm.state.value.items.first { it.id == "allday" }.showsDone)
    }

    @Test
    fun emptyDayHasNoItems() = runTest(dispatcher) {
        val vm = viewModel(friday.plusDays(1))
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertTrue(vm.state.value.items.isEmpty())
    }
}
