package com.prostovasei.simpletasks.ui.feature.all

import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.inbox.InboxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class AllViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 10, 13)
    private val day = MutableStateFlow(today)
    private val tasks = FakeTaskRepository(today)
    private val projects = FakeProjectRepository()
    private val time = FakeTimeProvider(zone = ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        projects.tree.value = projects.tree.value.copy(ungrouped = listOf(ProjectWithCount(project("work", "Работа", color = "#3D8BFD"), 0)))
        tasks.tasks.value = listOf(
            task("late", projectId = "work", due = TaskDue.AllDay(today.minusDays(1))),
            task("allday", due = TaskDue.AllDay(today)),
            task("timed", projectId = "work", due = TaskDue.AtTime(Instant.parse("2026-10-13T10:00:00Z"))),
            task("next", due = TaskDue.AllDay(today.plusDays(1))),
            task("nodate"),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AllViewModel(tasks, projects, DayTicker { day }, time)

    @Test
    fun buildsSectionsWithLabels() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        val state = vm.state.value

        assertFalse(state.isLoading)
        assertEquals(listOf("late"), state.overdue.map { it.id })
        assertEquals(listOf("allday"), state.todayAllDay.map { it.id })
        assertEquals(listOf("timed"), state.todayTimed.map { it.id })
        assertEquals(listOf("next"), state.upcoming.map { it.id })
        assertEquals("Работа", state.overdue.single().listName)
        assertEquals("#3D8BFD", state.overdue.single().listColor)
        assertEquals(DayLabel.Yesterday, state.overdue.single().due!!.day)
        assertEquals("10:00", state.todayTimed.single().due!!.time)
        assertTrue(state.todayAllDay.single().isInboxList)
    }

    @Test
    fun completedTaskStaysWhileDissolvingThenDisappears() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TaskListIntent.ToggleComplete("allday"))
        runCurrent()
        assertEquals(listOf("allday" to true), tasks.completedCalls)
        assertTrue(vm.state.value.todayAllDay.single().isCompleting)

        advanceTimeBy(CompletionTracker.HIDE_DELAY_MS - 100)
        runCurrent()
        assertEquals(listOf("allday"), vm.state.value.todayAllDay.map { it.id })

        advanceTimeBy(200)
        runCurrent()
        assertTrue(vm.state.value.todayAllDay.isEmpty())
    }

    @Test
    fun recurringTaskMovesAfterDissolveAndCanBeUndone() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("gym", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY;BYDAY=TU,FR"))
        val vm = viewModel()
        runCurrent()
        assertTrue(vm.state.value.todayAllDay.single().isRecurring)

        vm.effects.test {
            vm.onIntent(TaskListIntent.ToggleComplete("gym", recurring = true))
            runCurrent()
            // Пока плашка «рассыпается», задача на месте и в БД не менялась.
            assertTrue(vm.state.value.todayAllDay.single().isCompleting)
            assertTrue(tasks.completedCalls.isEmpty())

            advanceTimeBy(CompletionTracker.HIDE_DELAY_MS + 1)
            runCurrent()
            val effect = awaitItem() as TaskListEffect.ShowUndo
            assertEquals(DayLabel.Date(LocalDate.of(2026, 10, 16), showYear = false), effect.next!!.day)
        }
        assertTrue("Сегодня задачи больше нет", vm.state.value.todayAllDay.isEmpty())
        val moved = vm.state.value.upcoming.filterNot { it.isGhost }
        assertEquals(listOf("gym"), moved.map { it.id })
        assertFalse("Без зачёркивания: задача не выполнена, а перенесена", moved.single().isCompleting)

        vm.onIntent(TaskListIntent.Undo("gym"))
        runCurrent()
        assertEquals(listOf("gym"), vm.state.value.todayAllDay.map { it.id })
    }

    @Test
    fun recurringTaskTappedAgainDuringDissolveIsNotWritten() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("gym", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY;BYDAY=TU,FR"))
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TaskListIntent.ToggleComplete("gym", recurring = true))
        runCurrent()
        vm.onIntent(TaskListIntent.ToggleComplete("gym", recurring = true))
        advanceTimeBy(CompletionTracker.HIDE_DELAY_MS * 2)
        runCurrent()

        assertTrue(tasks.completedCalls.isEmpty())
        assertTrue(tasks.undone.isEmpty())
        assertFalse(vm.state.value.todayAllDay.single().isCompleting)
    }

    @Test
    fun futureRepeatsShownAsGhostsInOrder() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            task("gym", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY;BYDAY=TU"),
            task("dentist", due = TaskDue.AllDay(today.plusDays(10))),
        )
        val vm = viewModel()
        runCurrent()

        val upcoming = vm.state.value.upcoming
        assertEquals(listOf("gym@0", "dentist", "gym@1", "gym@2", "gym@3"), upcoming.map { it.id })
        val ghost = upcoming.first()
        assertTrue(ghost.isGhost)
        assertEquals("gym", ghost.taskId)
        assertEquals(DayLabel.Date(LocalDate.of(2026, 10, 20), showYear = false), ghost.due!!.day)
        assertEquals("Сама задача — в «Сегодня»", listOf("gym"), vm.state.value.todayAllDay.map { it.id })
    }

    @Test
    fun undoReturnsTask() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.effects.test {
            vm.onIntent(TaskListIntent.ToggleComplete("timed"))
            assertEquals(TaskListEffect.ShowUndo("timed"), awaitItem())
        }
        vm.onIntent(TaskListIntent.Undo("timed"))
        runCurrent()

        assertEquals(listOf("timed" to true, "timed" to false), tasks.completedCalls)
        assertFalse(vm.state.value.todayTimed.single().isCompleting)
        advanceTimeBy(CompletionTracker.HIDE_DELAY_MS * 2)
        runCurrent()
        assertEquals(listOf("timed"), vm.state.value.todayTimed.map { it.id })
    }

    @Test
    fun secondTapOnFadedTaskCancelsCompletion() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TaskListIntent.ToggleComplete("next"))
        runCurrent()
        vm.onIntent(TaskListIntent.ToggleComplete("next"))
        runCurrent()

        assertEquals(listOf("next" to true, "next" to false), tasks.completedCalls)
        assertFalse(vm.state.value.upcoming.single().isCompleting)
    }

    @Test
    fun sectionsFollowDayChange() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()

        day.value = today.plusDays(1)
        runCurrent()

        assertEquals(setOf("late", "allday", "timed"), vm.state.value.overdue.map { it.id }.toSet())
        assertEquals(listOf("next"), vm.state.value.todayAllDay.map { it.id })
    }

    @Test
    fun inboxShowsInboxAndUndatedTasks() = runTest(dispatcher) {
        tasks.tasks.value = tasks.tasks.value + task("work-undated", projectId = "work")
        val vm = InboxViewModel(tasks, projects, DayTicker { day }, time)
        runCurrent()

        assertEquals(setOf("allday", "next", "nodate", "work-undated"), vm.state.value.tasks.map { it.id }.toSet())

        vm.onIntent(TaskListIntent.ToggleComplete("nodate"))
        runCurrent()
        assertTrue(vm.state.value.tasks.first { it.id == "nodate" }.isCompleting)
        advanceTimeBy(CompletionTracker.HIDE_DELAY_MS + 1)
        runCurrent()
        assertFalse(vm.state.value.tasks.any { it.id == "nodate" })
    }
}
