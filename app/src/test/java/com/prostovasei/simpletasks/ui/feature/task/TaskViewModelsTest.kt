package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.SnoozeKind
import com.prostovasei.simpletasks.domain.model.SnoozeOption
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

    private val draftStore = TaskDrafts(initial = null, persist = {})
    private val smartDates = SmartDateSetting(initial = true, save = {})

    private fun quickAdd(vararg args: Pair<String, Any?>) = QuickAddViewModel(SavedStateHandle(mapOf(*args)), tasks, projects, time, draftStore, smartDates)

    private fun QuickAddViewModel.intervalEnd() = (state.value.due as? TaskDue.AtTime)?.instant?.takeIf { state.value.start != null }

    private fun detail(scope: CoroutineScope, taskId: String = "t1") = TaskDetailViewModel(
        SavedStateHandle(mapOf("taskId" to taskId)), tasks, checklist, projects, DayTicker { flowOf(today) }, time, scope, deletions, smartDates,
    )

    private val deletions = TaskDeletions(tasks, CoroutineScope(dispatcher))

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

    /** Итерация 51: «Задача на сб, 7 ноя» в шторке дня — новая задача на этот день. */
    @Test
    fun quickAddFromDaySheetTakesItsDate() = runTest(dispatcher) {
        val vm = quickAdd("date" to "2026-11-07", "today" to false)
        runCurrent()
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 7)), vm.state.value.due)
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
    fun quickAddGridTaps() = runTest(dispatcher) {
        val vm = quickAdd("today" to true)
        vm.onIntent(QuickAddIntent.Grid(GridAction.SlotTap(today.plusDays(1), 16 * 60)))
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T16:00:00Z")), vm.state.value.due)
        assertEquals(16 * 60, vm.state.value.fields.selectedMinute)

        vm.onIntent(QuickAddIntent.Grid(GridAction.ClearTime))
        assertEquals("Крестик — тот же день без времени", TaskDue.AllDay(today.plusDays(1)), vm.state.value.due)

        vm.onIntent(QuickAddIntent.Grid(GridAction.DayTap(today.plusDays(3))))
        assertEquals(TaskDue.AllDay(today.plusDays(3)), vm.state.value.due)
        vm.onIntent(QuickAddIntent.Grid(GridAction.DayTap(today.plusDays(3))))
        assertEquals("Повторное нажатие на день убирает дату", null, vm.state.value.due)
    }

    @Test
    fun quickAddIntervalMoveAndResize() = runTest(dispatcher) {
        val vm = quickAdd()
        runCurrent()
        vm.onIntent(QuickAddIntent.Title("Встреча"))
        vm.onIntent(QuickAddIntent.Grid(GridAction.SlotTap(today.plusDays(1), 16 * 60)))
        vm.onIntent(QuickAddIntent.Grid(GridAction.Resize(90)))
        assertEquals(Instant.parse("2026-10-15T17:30:00Z"), vm.intervalEnd())

        vm.onIntent(QuickAddIntent.Grid(GridAction.SlotTap(today.plusDays(1), 17 * 60)))
        assertEquals("Нажатие внутри блока его не сбрасывает", 90, vm.state.value.fields.durationMinutes)

        vm.onIntent(QuickAddIntent.Grid(GridAction.MoveSlot(today.plusDays(2), 23 * 60)))
        assertEquals("Перенос сохраняет длительность", 90, vm.state.value.fields.durationMinutes)
        assertEquals("Блок сдвигается, чтобы конец был не дальше 24:00", TaskDue.AtTime(Instant.parse("2026-10-16T22:30:00Z")), vm.state.value.start)
        assertEquals("Не дальше 24:00", Instant.parse("2026-10-17T00:00:00Z"), vm.intervalEnd())

        vm.onIntent(QuickAddIntent.Grid(GridAction.MoveSlot(today.plusDays(2), 9 * 60)))
        vm.onIntent(QuickAddIntent.Grid(GridAction.Resize(120)))
        vm.effects.test {
            vm.onIntent(QuickAddIntent.Save)
            runCurrent()
            awaitItem()
        }
        val created = tasks.created.single()
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-16T09:00:00Z")), created.start)
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-16T11:00:00Z")), created.due)
        assertEquals("Напоминание — в начале (решение PM)", listOf("PT0S"), created.reminders)
    }

    @Test
    fun quickAddPlainTimeAfterNewTapOrDay() = runTest(dispatcher) {
        val vm = quickAdd()
        vm.onIntent(QuickAddIntent.Grid(GridAction.SlotTap(today, 10 * 60)))
        vm.onIntent(QuickAddIntent.Grid(GridAction.Resize(60)))
        vm.onIntent(QuickAddIntent.Grid(GridAction.SlotTap(today, 14 * 60)))
        assertEquals("Новое время на сетке — снова 30 минут", null, vm.intervalEnd())
        vm.onIntent(QuickAddIntent.Grid(GridAction.Resize(60)))
        vm.onIntent(QuickAddIntent.Grid(GridAction.ClearTime))
        assertEquals(30, vm.state.value.fields.durationMinutes)
    }

    @Test
    fun quickAddShowsBusyTimeOfVisibleDays() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            task("near", title = "Встреча", due = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z"))),
            task("far", due = TaskDue.AtTime(Instant.parse("2026-11-20T10:00:00Z"))),
        )
        val vm = quickAdd()
        runCurrent()
        assertEquals(listOf("near"), vm.state.value.busy.map { it.taskId })
        assertEquals(5 * 60, vm.state.value.nowMinute)

        vm.onIntent(QuickAddIntent.Grid(GridAction.FirstDay(LocalDate.of(2026, 11, 18))))
        runCurrent()
        assertEquals("Пролистали сетку — подгрузилась та неделя", listOf("far"), vm.state.value.busy.map { it.taskId })
    }

    @Test
    fun detailGridEditsSaveImmediately() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            task("t1", due = TaskDue.AtTime(Instant.parse("2026-10-15T11:00:00Z")), start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z"))),
            task("other", due = TaskDue.AtTime(Instant.parse("2026-10-15T14:00:00Z"))),
        )
        val vm = detail(this)
        runCurrent()
        assertEquals("Своя задача не серая плашка", listOf("other"), vm.state.value.busy.map { it.taskId })
        assertEquals(60, vm.state.value.fields!!.durationMinutes)

        vm.onIntent(TaskDetailIntent.Grid(GridAction.MoveSlot(today.plusDays(2), 16 * 60)))
        runCurrent()
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-16T16:00:00Z")), tasks.updated.last().start)
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-16T17:00:00Z")), tasks.updated.last().due)

        vm.onIntent(TaskDetailIntent.Grid(GridAction.ClearTime))
        runCurrent()
        assertEquals(TaskDue.AllDay(today.plusDays(2)), tasks.updated.last().due)
        assertEquals(null, tasks.updated.last().start)
    }

    @Test
    fun detailMultiDayTaskMovesWhole() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(LocalDate.of(2026, 10, 22)), start = TaskDue.AllDay(LocalDate.of(2026, 10, 20))))
        val vm = detail(this)
        runCurrent()
        vm.onIntent(TaskDetailIntent.Grid(GridAction.DayTap(LocalDate.of(2026, 10, 21))))
        runCurrent()
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 21)), tasks.updated.last().start)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 23)), tasks.updated.last().due)
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
        assertEquals(TaskDue.AllDay(today.plusDays(1)), tasks.tasks.value.single { it.id == "t1" }.due)
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

        // Итерация 36: удаление без вопроса; шторка закрывается после записи, и «Отмена» возвращает задачу.
        deletions.events.test {
            vm.effects.test {
                vm.onIntent(TaskDetailIntent.RequestDelete)
                runCurrent()
                assertEquals(TaskDetailEffect.Close, awaitItem())
            }
            val deletion = awaitItem()
            assertEquals("t1", deletion.taskId)
            assertTrue(tasks.tasks.value.isEmpty())
            deletions.undo(deletion)
            runCurrent()
            assertEquals(listOf("t1"), tasks.tasks.value.map { it.id })
        }
    }

    /** Итерация 49: «Удалить» у повторяющейся сначала спрашивает; «Не буду делать» закрывает раз, под шторкой — «Отмена». */
    @Test
    fun detailRecurringDeleteAsksAndWontDoClosesOccurrence() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.RequestDelete)
        runCurrent()
        assertEquals(DetailDialog.DeleteSeries, vm.state.value.dialog)
        assertEquals("Серия не удалена до выбора", listOf("t1"), tasks.tasks.value.map { it.id })

        deletions.notices.test {
            vm.effects.test {
                vm.onIntent(TaskDetailIntent.WontDo)
                runCurrent()
                assertEquals(TaskDetailEffect.Close, awaitItem())
            }
            val notice = awaitItem()
            assertEquals(com.prostovasei.simpletasks.designsystem.components.UndoKind.WontDo, notice.kind)
            assertTrue(notice.message is UndoMessage.WontDo && (notice.message as UndoMessage.WontDo).day != null)
            assertEquals(Triple("t1", null, EarlierRepeats.SKIP), tasks.wontDoCalls.single())
            assertEquals(TaskDue.AllDay(today.plusDays(7)), tasks.getTask("t1")!!.due)
            deletions.undo(notice)
            runCurrent()
            assertEquals(TaskDue.AllDay(today), tasks.getTask("t1")!!.due)
        }
    }

    /** Итерация 54: «Отложить» в окне задачи — новый срок, шторка закрывается, под ней «Отложено на … · Отмена». */
    @Test
    fun detailSnoozeMovesTaskAndOffersUndo() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)))
        val vm = detail(this)
        runCurrent()
        val tomorrow = TaskDue.AllDay(today.plusDays(1))

        deletions.notices.test {
            vm.effects.test {
                vm.onIntent(TaskDetailIntent.Snooze(SnoozeOption(SnoozeKind.TOMORROW, tomorrow, null)))
                runCurrent()
                assertEquals(TaskDetailEffect.Close, awaitItem())
            }
            val notice = awaitItem()
            assertEquals(com.prostovasei.simpletasks.designsystem.components.UndoKind.Tomorrow, notice.kind)
            assertTrue(notice.message is UndoMessage.Snoozed)
            assertEquals(tomorrow, tasks.getTask("t1")!!.due)
            deletions.undo(notice)
            runCurrent()
            assertEquals(TaskDue.AllDay(today), tasks.getTask("t1")!!.due)
        }
    }

    /** Итерация 49: будущий повтор открывается со своей датой; «Не буду делать» спрашивает про незакрытые повторы перед ним. */
    @Test
    fun detailFutureRepeatAsksAboutEarlierOnes() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val future = TaskDue.AllDay(today.plusDays(14))
        val vm = detail(this, taskId = com.prostovasei.simpletasks.ui.common.TaskOpenKey.of("t1", future))
        runCurrent()

        assertEquals(future, vm.state.value.occurrence)
        assertEquals("Поле даты показывает этот раз", future, vm.state.value.fields!!.due)

        vm.effects.test {
            vm.onIntent(TaskDetailIntent.ToggleCompleted)
            runCurrent()
            assertTrue("Будущий раз раньше ближайшего не выполняется", awaitItem() is TaskDetailEffect.FutureDone)
        }
        assertEquals(TaskStatus.ACTIVE, tasks.getTask("t1")!!.status)

        vm.onIntent(TaskDetailIntent.WontDo)
        runCurrent()
        val dialog = vm.state.value.dialog as DetailDialog.Unclosed
        assertEquals(listOf(TaskDue.AllDay(today), TaskDue.AllDay(today.plusDays(7))), dialog.dates)

        vm.onIntent(TaskDetailIntent.CloseEarlier(EarlierRepeats.COMPLETE))
        runCurrent()
        assertEquals(Triple("t1", future, EarlierRepeats.COMPLETE), tasks.wontDoCalls.single())
    }

    /**
     * Итерация 50: новая дата у повторяющейся не пишется до «Готово», потом окно «Изменить повторяющуюся задачу»; «Отмена» возвращает
     * дату, «Текущий повтор» переносит только раз и закрывает шторку с «Перенесено на … · Отмена».
     */
    @Test
    fun detailRepeatDateChangeAsksWhatToMove() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this)
        runCurrent()
        val tomorrow = TaskDue.AllDay(today.plusDays(1))

        vm.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(1))))
        runCurrent()
        assertEquals("Поле показывает выбор", tomorrow, vm.state.value.fields!!.due)
        assertEquals("В базе — по-старому", TaskDue.AllDay(today), tasks.getTask("t1")!!.due)

        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        val dialog = vm.state.value.dialog as DetailDialog.ChangeRepeat
        assertNull("У ближайшего раза «Текущий повтор» доступен", dialog.nearest)

        vm.onIntent(TaskDetailIntent.DismissDialog)
        assertNull(vm.state.value.dialog)
        assertEquals("«Отмена» вернула дату", TaskDue.AllDay(today), vm.state.value.fields!!.due)

        // «←» в окне даты: выбор забыт, окна нет.
        vm.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(1))))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Restore(Schedule(TaskDue.AllDay(today), null))))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        assertNull(vm.state.value.dialog)
        assertEquals(TaskDue.AllDay(today), vm.state.value.fields!!.due)

        vm.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(1))))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        deletions.notices.test {
            vm.effects.test {
                vm.onIntent(TaskDetailIntent.ChangeRepeat(all = false))
                runCurrent()
                assertEquals(TaskDetailEffect.Close, awaitItem())
            }
            assertEquals(UndoMessage.Moved(vm.dayText(tomorrow)), awaitItem().message)
        }
        assertEquals("t1" to tomorrow, tasks.movedRepeats.single())
    }

    /** «Все повторы с этого момента» у ближайшего раза — как раньше: серия сдвигается, шторка остаётся. */
    @Test
    fun detailRepeatAllShiftsSeries() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(2))))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        vm.onIntent(TaskDetailIntent.ChangeRepeat(all = true))
        runCurrent()

        assertEquals(TaskDue.AllDay(today.plusDays(2)), tasks.getTask("t1")!!.due)
        assertTrue(tasks.movedRepeats.isEmpty())
        assertNull(vm.state.value.pending)
    }

    /** 0.51.1: «Все повторы» — правило с одним днём под новую дату, с несколькими — ближайший день правила и подсказка. */
    @Test
    fun detailRepeatAllRetargetsOrHints() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            task("water", due = TaskDue.AllDay(today.plusDays(4))).copy(rrule = "FREQ=MONTHLY;BYMONTHDAY=${today.plusDays(4).dayOfMonth}"),
            task("pool", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY;BYDAY=TU,TH"),
        )
        val water = detail(this, "water")
        runCurrent()
        water.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(1))))
        water.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        water.onIntent(TaskDetailIntent.ChangeRepeat(all = true))
        runCurrent()
        assertEquals(TaskDue.AllDay(today.plusDays(1)), tasks.getTask("water")!!.due)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=${today.plusDays(1).dayOfMonth}", tasks.getTask("water")!!.rrule)

        // Сегодня ср 14.10.2026: «вт, чт» на пт 16 → вт 20, подсказка с этой датой.
        val pool = detail(this, "pool")
        runCurrent()
        pool.effects.test {
            pool.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(2))))
            pool.onIntent(TaskDetailIntent.Grid(GridAction.Done))
            pool.onIntent(TaskDetailIntent.ChangeRepeat(all = true))
            runCurrent()
            assertEquals(TaskDetailEffect.SeriesAligned(pool.dayText(TaskDue.AllDay(today.plusDays(6)))), awaitItem())
        }
        assertEquals(TaskDue.AllDay(today.plusDays(6)), tasks.getTask("pool")!!.due)
    }

    /** Будущий раз: «Текущий повтор» недоступен, «Все повторы с этого момента» делят серию; снятие даты — без окна. */
    @Test
    fun detailFutureRepeatSplitsSeries() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val future = TaskDue.AllDay(today.plusDays(14))
        val vm = detail(this, taskId = com.prostovasei.simpletasks.ui.common.TaskOpenKey.of("t1", future))
        runCurrent()

        vm.onIntent(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(15))))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        val dialog = vm.state.value.dialog as DetailDialog.ChangeRepeat
        assertEquals(vm.dayText(TaskDue.AllDay(today)), dialog.nearest)
        assertEquals(future, vm.state.value.occurrence)

        vm.onIntent(TaskDetailIntent.ChangeRepeat(all = true))
        runCurrent()
        assertEquals(Triple("t1", future, TaskDue.AllDay(today.plusDays(15)) as TaskDue?), tasks.movedRepeatsFrom.single())
    }

    @Test
    fun detailRepeatClearedDateNeedsNoDialog() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.Grid(GridAction.ClearDate))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))
        runCurrent()

        assertNull(vm.state.value.dialog)
        assertNull(tasks.getTask("t1")!!.due)
    }

    /** Крестик у времени — тоже окно (решение PM). */
    @Test
    fun detailRepeatClearedTimeAsks() = runTest(dispatcher) {
        val at = TaskDue.AtTime(today.atTime(14, 30).atZone(time.zone()).toInstant())
        tasks.tasks.value = listOf(task("t1", due = at).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this)
        runCurrent()

        vm.onIntent(TaskDetailIntent.Grid(GridAction.ClearTime))
        vm.onIntent(TaskDetailIntent.Grid(GridAction.Done))

        assertTrue(vm.state.value.dialog is DetailDialog.ChangeRepeat)
        assertEquals(at, tasks.getTask("t1")!!.due)
    }

    /** Будущий повтор, который на деле ближайший, открывается как обычная задача. */
    @Test
    fun detailNearestKeyOpensTheTask() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", due = TaskDue.AllDay(today)).copy(rrule = "FREQ=WEEKLY"))
        val vm = detail(this, taskId = com.prostovasei.simpletasks.ui.common.TaskOpenKey.of("t1", TaskDue.AllDay(today)))
        runCurrent()
        assertNull(vm.state.value.occurrence)
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

    /** Итерация 38: закрыли карточку — название и описание в черновике; следующее «+» подставляет их выделенными. */
    @Test
    fun dismissedQuickAddKeepsDraftForNextOne() = runTest(dispatcher) {
        val first = quickAdd()
        runCurrent()
        first.onIntent(QuickAddIntent.Title("Купить билеты"))
        first.onIntent(QuickAddIntent.Content("Две штуки"))
        first.onIntent(QuickAddIntent.Edit(FieldEdit.SetPriority(Priority.HIGH)))
        first.onIntent(QuickAddIntent.Dismiss)

        val second = quickAdd()
        runCurrent()
        assertEquals("Купить билеты", second.state.value.title)
        assertEquals("Две штуки", second.state.value.content)
        assertTrue(second.state.value.draftRestored)
        assertEquals("Приоритет не запоминается", Priority.NONE, second.state.value.priority)
        assertEquals(null, second.state.value.due)
    }

    @Test
    fun savedTaskOrBlankTitleClearsDraft() = runTest(dispatcher) {
        val first = quickAdd()
        runCurrent()
        first.onIntent(QuickAddIntent.Title("Черновик"))
        first.onIntent(QuickAddIntent.Dismiss)
        assertEquals("Черновик", draftStore.get()?.title)

        val second = quickAdd()
        runCurrent()
        second.onIntent(QuickAddIntent.Save)
        runCurrent()
        assertEquals("Сохранили — черновика нет", null, draftStore.get())
        second.onIntent(QuickAddIntent.Dismiss)
        assertEquals("После сохранения закрытие черновик не возвращает", null, draftStore.get())

        val third = quickAdd()
        runCurrent()
        third.onIntent(QuickAddIntent.Title("Опять"))
        third.onIntent(QuickAddIntent.Dismiss)
        val fourth = quickAdd()
        runCurrent()
        fourth.onIntent(QuickAddIntent.Title("  "))
        fourth.onIntent(QuickAddIntent.Dismiss)
        assertEquals("Название очистили — черновика нет", null, draftStore.get())
    }

    // ---------------------------------------------------------------- итерация 41: дата и время в названии

    /** #95: продиктованный текст сразу в названии, срок из фразы разобран, черновик прежнего окна не подставляется. */
    @Test
    fun quickAddFromVoiceTakesTextAndDate() = runTest(dispatcher) {
        draftStore.save("Старый черновик", "Описание черновика")
        val vm = quickAdd("today" to true, "voiceText" to "Позвонить маме завтра в 10")
        runCurrent()
        val state = vm.state.value
        assertEquals("Позвонить маме завтра в 10", state.title)
        assertEquals("", state.content)
        assertTrue(state.fromVoice)
        assertFalse(state.draftRestored)
        assertEquals("Срок из фразы важнее «на сегодня»", TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), state.due)

        vm.onIntent(QuickAddIntent.Save)
        runCurrent()
        assertEquals("Позвонить маме", tasks.created.single().title)
    }

    @Test
    fun quickAddFromVoiceWithoutDateKeepsTarget() = runTest(dispatcher) {
        val vm = quickAdd("today" to true, "voiceText" to "Купить молоко")
        runCurrent()
        assertEquals(TaskDue.AllDay(today), vm.state.value.due)
        assertFalse(quickAdd().state.value.fromVoice)
    }

    @Test
    fun quickAddTakesDateFromTitleAndCleansIt() = runTest(dispatcher) {
        val vm = quickAdd()
        runCurrent()
        vm.onIntent(QuickAddIntent.Title("Созвон завтра"))
        assertEquals(TaskDue.AllDay(today.plusDays(1)), vm.state.value.due)
        vm.onIntent(QuickAddIntent.Title("Созвон завтра в 10:00"))
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), vm.state.value.due)
        assertEquals("Фраза подсвечивается", listOf("завтра в 10:00"), vm.state.value.smart.found!!.phrases("Созвон завтра в 10:00"))

        vm.onIntent(QuickAddIntent.Save)
        runCurrent()
        val created = tasks.created.single()
        assertEquals("Фраза уходит из названия", "Созвон", created.title)
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), created.due)
    }

    @Test
    fun quickAddTitleDateWinsOverPresetAndErasingRestoresIt() = runTest(dispatcher) {
        val vm = quickAdd("today" to true)
        runCurrent()
        assertEquals(TaskDue.AllDay(today), vm.state.value.due)
        vm.onIntent(QuickAddIntent.Title("Билеты 15 ноября"))
        assertEquals("Дата из названия побеждает", TaskDue.AllDay(LocalDate.of(2026, 11, 15)), vm.state.value.due)
        vm.onIntent(QuickAddIntent.Title("Билеты 15"))
        assertEquals("Фразу стёрли — срок прежний", TaskDue.AllDay(today), vm.state.value.due)
        assertNull(vm.state.value.smart.found)
    }

    @Test
    fun quickAddChipCrossKeepsPhraseAndStopsParsing() = runTest(dispatcher) {
        val vm = quickAdd()
        runCurrent()
        vm.onIntent(QuickAddIntent.Title("Сериал в пятницу"))
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 16)), vm.state.value.due)
        vm.onIntent(QuickAddIntent.DismissSmartDate)
        assertNull("Срок вернулся", vm.state.value.due)
        vm.onIntent(QuickAddIntent.Title("Сериал в пятницу в 20:00"))
        assertNull("Для этой задачи больше не разбираем", vm.state.value.due)
        vm.onIntent(QuickAddIntent.Save)
        runCurrent()
        assertEquals("Сериал в пятницу в 20:00", tasks.created.single().title)
    }

    @Test
    fun quickAddSettingOffDisablesParsing() = runTest(dispatcher) {
        smartDates.set(false)
        val vm = quickAdd()
        runCurrent()
        vm.onIntent(QuickAddIntent.Title("Созвон завтра"))
        assertNull(vm.state.value.due)
        assertNull(vm.state.value.smart.found)
    }

    @Test
    fun detailParsesOnlyNewPhrasesAndSavesCleanTitle() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", title = "Отчёт в пятницу", projectId = "work"))
        val vm = detail(this)
        runCurrent()
        vm.onIntent(TaskDetailIntent.Title("Отчёт в пятницу!"))
        assertNull("Фраза была в названии при открытии — не трогаем", vm.state.value.smart.found)
        assertNull(vm.state.value.task!!.due)

        vm.onIntent(TaskDetailIntent.Title("Отчёт завтра в 18:00"))
        runCurrent()
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T18:00:00Z")), tasks.updated.last().due)
        assertEquals("В базе — без фразы", "Отчёт", tasks.updated.last().title)
        assertEquals("На экране фраза остаётся до закрытия", "Отчёт завтра в 18:00", vm.state.value.task!!.title)

        vm.onIntent(TaskDetailIntent.DismissSmartDate)
        runCurrent()
        assertNull("«✕» вернул прежний срок", tasks.updated.last().due)
        assertEquals("Отчёт завтра в 18:00", tasks.updated.last().title)
    }
}
