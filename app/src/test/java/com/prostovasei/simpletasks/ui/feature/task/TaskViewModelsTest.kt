package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.FakeChecklistRepository
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val tasks = FakeTaskRepository()
    private val projects = FakeProjectRepository()
    private val checklist = FakeChecklistRepository()
    private val time = FakeTimeProvider(now = Instant.parse("2026-10-14T05:00:00Z"), zone = ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 14)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        projects.tree.value = projects.tree.value.copy(ungrouped = listOf(ProjectWithCount(project("work", "Работа"), 0)))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun quickAdd(vararg args: Pair<String, Any?>) = QuickAddViewModel(SavedStateHandle(mapOf(*args)), tasks, projects, time)

    private fun detail(scope: CoroutineScope, taskId: String = "t1") = TaskDetailViewModel(
        SavedStateHandle(mapOf("taskId" to taskId)), tasks, checklist, projects, DayTicker { flowOf(today) }, time, scope,
    )

    @Test
    fun quickAddDefaultsToInbox() = runTest(dispatcher) {
        val vm = quickAdd()
        runCurrent()
        assertEquals("inbox", vm.state.value.projectId)
        assertNull(vm.state.value.due)
        assertFalse("Без названия сохранить нельзя", vm.state.value.canSave)
    }

    @Test
    fun quickAddFromListAndFromAllScreen() = runTest(dispatcher) {
        val fromList = quickAdd("projectId" to "work")
        val fromAll = quickAdd("today" to true)
        runCurrent()
        assertEquals("work", fromList.state.value.projectId)
        assertEquals(TaskDue.AllDay(today), fromAll.state.value.due)
    }

    @Test
    fun quickAddSavesAllFields() = runTest(dispatcher) {
        val vm = quickAdd("today" to true)
        runCurrent()
        vm.onIntent(QuickAddIntent.Title("  Купить продукты "))
        vm.onIntent(QuickAddIntent.Content("Молоко, хлеб"))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Project("work")))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.SetPriority(Priority.HIGH)))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Repeat("FREQ=WEEKLY;BYDAY=SA")))
        vm.onIntent(QuickAddIntent.AddChecklist)
        vm.onIntent(QuickAddIntent.ChecklistItem(0, "Молоко"))
        vm.onIntent(QuickAddIntent.AddChecklist)

        vm.effects.test {
            vm.onIntent(QuickAddIntent.Save)
            runCurrent()
            assertEquals(QuickAddEffect.Close, awaitItem())
        }
        val created = tasks.created.single()
        assertEquals("Купить продукты", created.title)
        assertEquals("Молоко, хлеб", created.content)
        assertEquals("work", created.projectId)
        assertEquals(Priority.HIGH, created.priority)
        assertEquals("Сегодня среда + «каждую субботу» → суббота", TaskDue.AllDay(LocalDate.of(2026, 10, 17)), created.due)
        assertEquals("FREQ=WEEKLY;BYDAY=SA", created.rrule)
        assertEquals("Пустой пункт отбрасывается", listOf("Молоко"), created.checklist)
        assertEquals("Напоминание включено по умолчанию: в день срока в 10:00", listOf("P0DT10H0M0S"), created.reminders)
    }

    @Test
    fun quickAddReminderFollowsDue() = runTest(dispatcher) {
        val vm = quickAdd()
        assertTrue("Без срока напоминаний нет", vm.state.value.reminders.isEmpty())

        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AllDay(today))))
        assertEquals(listOf("P0DT10H0M0S"), vm.state.value.reminders)
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AtTime(Instant.parse("2026-10-14T15:00:00Z")))))
        assertEquals(listOf("PT0S"), vm.state.value.reminders)

        vm.onIntent(QuickAddIntent.Edit(FieldEdit.SetReminders(listOf("-PT30M"))))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AtTime(Instant.parse("2026-10-15T15:00:00Z")))))
        assertEquals("Своё напоминание сохраняется при смене даты", listOf("-PT30M"), vm.state.value.reminders)

        vm.onIntent(QuickAddIntent.Edit(FieldEdit.SetReminders(emptyList())))
        assertTrue("Можно отключить", vm.state.value.reminders.isEmpty())
    }

    /** 14.10.2026 — среда; 17.10 — суббота. */
    @Test
    fun quickAddRepeatStartsFromFirstMatchingDay() = runTest(dispatcher) {
        val vm = quickAdd()
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AllDay(today.plusDays(1)))))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Repeat("FREQ=WEEKLY;BYDAY=SA")))
        assertEquals("Дата «завтра» + «каждую субботу» → суббота", TaskDue.AllDay(LocalDate.of(2026, 10, 17)), vm.state.value.due)

        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AllDay(LocalDate.of(2026, 10, 19)))))
        assertEquals("Новая дата (понедельник) → ближайшая суббота", TaskDue.AllDay(LocalDate.of(2026, 10, 24)), vm.state.value.due)
    }

    @Test
    fun detailRepeatStartsFromFirstMatchingDay() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)))
        val vm = detail(this)
        runCurrent()
        vm.onIntent(TaskDetailIntent.Edit(FieldEdit.Repeat("FREQ=WEEKLY;BYDAY=SA")))
        runCurrent()
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 17)), vm.state.value.task!!.due)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 17)), tasks.updated.last().due)
    }

    @Test
    fun detailRemindersAreSavedImmediately() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)))
        val vm = detail(this)
        runCurrent()
        vm.onIntent(TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("P0DT10H0M0S", "-P0DT14H0M0S"))))
        runCurrent()
        assertEquals(listOf("P0DT10H0M0S", "-P0DT14H0M0S"), tasks.updated.last().reminders)
    }

    @Test
    fun detailRecurringTaskMovesToNextTime() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=DAILY"))
        val vm = detail(this)
        runCurrent()

        vm.effects.test {
            vm.onIntent(TaskDetailIntent.ToggleCompleted)
            runCurrent()
            val effect = awaitItem() as TaskDetailEffect.Rescheduled
            assertEquals(com.prostovasei.simpletasks.ui.common.DayLabel.Tomorrow, effect.next.day)
        }
        assertEquals(TaskDue.AllDay(today.plusDays(1)), vm.state.value.task!!.due)
        assertEquals(TaskStatus.ACTIVE, vm.state.value.task!!.status)

        // Следующая правка не возвращает старую дату.
        vm.onIntent(TaskDetailIntent.Edit(FieldEdit.SetPriority(Priority.HIGH)))
        runCurrent()
        assertEquals(TaskDue.AllDay(today.plusDays(1)), tasks.tasks.value.single().due)
    }

    @Test
    fun removingDateRemovesRepeat() = runTest(dispatcher) {
        val vm = quickAdd("today" to true)
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Repeat("FREQ=DAILY")))
        vm.onIntent(QuickAddIntent.Edit(FieldEdit.Due(null)))
        assertNull(vm.state.value.rrule)
    }

    @Test
    fun detailSavesTitleHalfSecondAfterTyping() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", title = "Отчёт", projectId = "work"))
        val vm = detail(this)
        runCurrent()
        assertEquals("Отчёт", vm.state.value.task!!.title)

        vm.onIntent(TaskDetailIntent.Title("Отчёт за"))
        vm.onIntent(TaskDetailIntent.Title("Отчёт за квартал"))
        advanceTimeBy(400)
        runCurrent()
        assertTrue("Пока печатает — не сохраняем", tasks.updated.isEmpty())

        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("Отчёт за квартал"), tasks.updated.map { it.title })
        assertEquals("Отчёт за квартал", vm.state.value.task!!.title)
    }

    @Test
    fun detailSavesImmediatelyAndShiftsInterval() = runTest(dispatcher) {
        val start = TaskDue.AtTime(Instant.parse("2026-10-14T10:00:00Z"))
        val due = TaskDue.AtTime(Instant.parse("2026-10-14T11:00:00Z"))
        tasks.tasks.value = listOf(task("t1", due = due, start = start))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.Edit(FieldEdit.Due(TaskDue.AtTime(Instant.parse("2026-10-15T11:00:00Z")))))
        runCurrent()

        val saved = tasks.updated.single()
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), saved.start)
    }

    @Test
    fun detailBlankTitleIsNotSaved() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", title = "Отчёт"))
        val vm = detail(this)
        runCurrent()
        vm.onIntent(TaskDetailIntent.Title("   "))
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(tasks.updated.isEmpty())
    }

    @Test
    fun detailFlushesPendingTextWhenClosed() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", title = "Черновик"))
        val store = ViewModelStore()
        val vm = ViewModelProvider.create(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = detail(this@runTest) as T
        })[TaskDetailViewModel::class]
        runCurrent()

        vm.onIntent(TaskDetailIntent.Content("Важная заметка"))
        store.clear()
        runCurrent()

        assertEquals(listOf("Важная заметка"), tasks.updated.map { it.content })
    }

    @Test
    fun detailCompleteAndDelete() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.ToggleCompleted)
        runCurrent()
        assertEquals(TaskStatus.COMPLETED, vm.state.value.task!!.status)

        vm.effects.test {
            vm.onIntent(TaskDetailIntent.RequestDelete)
            assertTrue(vm.state.value.showDeleteConfirm)
            vm.onIntent(TaskDetailIntent.ConfirmDelete)
            runCurrent()
            assertEquals(TaskDetailEffect.Close, awaitItem())
        }
        assertTrue(tasks.tasks.value.isEmpty())
    }

    @Test
    fun detailChecklistOperations() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.AddItem("  Позвонить  "))
        vm.onIntent(TaskDetailIntent.AddItem("   "))
        runCurrent()

        assertEquals(listOf("add:t1:Позвонить"), checklist.calls)
    }

    @Test
    fun detailShowsSubtasks() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1"), task("s1", title = "Купить лампочки").copy(parentId = "t1"))
        val vm = detail(this)
        runCurrent()
        assertEquals(listOf("Купить лампочки"), vm.state.value.subtasks.map { it.title })

        vm.onIntent(TaskDetailIntent.ToggleSubtask("s1"))
        runCurrent()
        assertEquals("s1" to true, tasks.completedCalls.last())
    }
}
