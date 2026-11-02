package com.prostovasei.simpletasks.ui.feature.week

import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.TaskDue
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    // Среда, 14 октября 2026: неделя — с понедельника 12-го.
    private val today = LocalDate.of(2026, 10, 14)
    private val monday = LocalDate.of(2026, 10, 12)
    private val day = MutableStateFlow(today)
    private val tasks = FakeTaskRepository(today)
    private val projects = FakeProjectRepository()
    private val time = FakeTimeProvider(zone = ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tasks.tasks.value = listOf(
            task("mon", due = TaskDue.AllDay(monday)),
            task("wed", due = TaskDue.AllDay(today)),
            task("next", due = TaskDue.AllDay(monday.plusDays(7))),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = WeekViewModel(tasks, projects, DayTicker { day }, time)

    @Test
    fun showsCurrentWeekFromMonday() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(monday, state.weekStart)
        assertEquals(today, state.today)
        assertEquals(YearMonth.of(2026, 10), state.month)
        assertEquals(listOf("mon"), state.days[0].items.map { it.id })
        assertEquals(listOf("wed"), state.days[2].items.map { it.id })
        assertTrue(state.days.drop(3).all { it.items.isEmpty() })
    }

    @Test
    fun selectingWeekShowsItsTasks() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(SelectWeek(monday.plusDays(7)))
        runCurrent()
        assertEquals(monday.plusDays(7), vm.state.value.weekStart)
        assertEquals(listOf("next"), vm.state.value.days[0].items.map { it.id })
        assertTrue(vm.state.value.days[2].items.isEmpty())
    }

    @Test
    fun monthShiftsBothWaysWithoutChangingTheWeek() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(ShiftMonth(1))
        runCurrent()
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
        vm.onIntent(ShiftMonth(-1))
        vm.onIntent(ShiftMonth(-1))
        runCurrent()
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertEquals(monday, vm.state.value.weekStart)
    }

    @Test
    fun currentWeekFollowsMidnightUntilAWeekIsPicked() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        day.value = LocalDate.of(2026, 10, 19)
        runCurrent()
        assertEquals(LocalDate.of(2026, 10, 19), vm.state.value.weekStart)

        vm.onIntent(SelectWeek(monday))
        runCurrent()
        day.value = LocalDate.of(2026, 10, 26)
        runCurrent()
        assertEquals("Выбранная неделя не прыгает в полночь", monday, vm.state.value.weekStart)
    }

    @Test
    fun completingKeepsTheTaskOnItsDayAsDone() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(TaskListIntent.ToggleComplete("wed"))
        runCurrent()
        assertEquals(listOf("wed" to true), tasks.completedCalls)
        val done = vm.state.value.days[2].items.single()
        assertEquals("wed", done.id)
        assertTrue(done.showsDone)
    }
}
