package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Создание задачи, экран задачи и конструктор повтора: отображение, нажатия, скриншоты. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class TaskScreensUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 14)
    private val zone = ZoneOffset.UTC
    private val projects = listOf(
        ProjectOption("inbox", "Inbox", "📥", isInbox = true),
        ProjectOption("work", "Работа", "💼", isInbox = false),
    )
    private val intents = mutableListOf<Any>()
    private val busy = listOf(
        BusySlot("a", "Планёрка", today, 9 * 60, 10 * 60, isDone = true),
        BusySlot("b", "Обед", today.plusDays(1), 13 * 60, 14 * 60 + 30, isDone = false),
        BusySlot("c", "Звонок", today.plusDays(2), 11 * 60, 11 * 60 + 30, isDone = false),
    )

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { AppTheme(darkTheme = dark) { content() } }
    }

    /** Нажатие на подпись даты открывает окно даты (крестик «убрать дату» стоит рядом с ней, вне нажатия). */
    private fun openDateWindow() {
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).performClick()
    }

    private val quickState = QuickAddState(
        title = "Созвон с командой",
        projectId = "work",
        priority = Priority.HIGH,
        due = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")),
        rrule = "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH",
        checklist = listOf("Повестка", ""),
        projects = projects,
        today = today,
        zone = zone,
    )

    @Test
    fun quickAddShowsChosenFields() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }

        // В шторке — два поля (итерация 34): дата и время; повтор и уведомления — в панели; сетки нет.
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertTextEquals("чт, 15 окт")
        composeRule.onNodeWithTag(TAG_TIME_TEXT, useUnmergedTree = true).assertTextEquals("10:00")
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).assertIsSelected()
        composeRule.onNodeWithText("Повестка").assertExists()
    }

    @Test
    fun quickAddWithoutDate() {
        show { QuickAddScreen(QuickAddState(projectId = "inbox", projects = projects, today = today, zone = zone), { intents += it }, {}, autoFocus = false) }
        // По умолчанию даты нет: серая подпись без крестика и без поля времени; подсказки полей — примеры.
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertTextEquals("Без даты и времени")
        composeRule.onNodeWithTag(TAG_CLEAR_DATE).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TIME_FIELD).assertDoesNotExist()
        composeRule.onNodeWithText("Купить билеты на концерт").assertExists()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsNotSelected()
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).assertIsNotEnabled()

        // Без даты напоминание и повтор не выбрать: кнопки нажимаются, окно подсказывает выбрать день.
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithText("Сначала выберите день").assertExists()
        composeRule.onNodeWithText("Понятно").performClick()
        composeRule.onNodeWithTag(TAG_REMINDERS_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).performClick()
        composeRule.onNodeWithText("Повтор начинается от даты задачи — сначала выберите дату.").assertExists()
    }

    @Test
    fun quickAddClosesOnTapOutsideCard() {
        var closed = false
        show { QuickAddScreen(quickState, { intents += it }, { closed = true }, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_QUICK_CARD).assertExists()
        // Шторка занимает низ экрана — нажимаем на затемнение над ней.
        composeRule.onNodeWithContentDescription("Закрыть").performTouchInput { click(Offset(centerX, top + 5f)) }
        assertTrue(closed)
    }

    @Test
    fun stripOpensWindowAndBlockDayMovesGrid() {
        show { QuickAddScreen(quickState.copy(due = TaskDue.AllDay(today)), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertTextEquals("ср, 14 окт")

        // Нажали на дату — окно даты на весь экран: сетка, блок «2 недели»; повтора и уведомлений в окне нет.
        openDateWindow()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertExists()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_DATE_RANGE).assertTextEquals("12 окт – 25 окт")

        // День в блоке — выбирается сразу, сетка переезжает к нему (послезавтра в центре: завтра, послезавтра, день после).
        composeRule.onNodeWithTag("$TAG_DATE_CELL:${today.plusDays(2)}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$TAG_GRID_DAY:${today.plusDays(1)}").assertExists()
        composeRule.onNodeWithTag("$TAG_GRID_DAY:${today.plusDays(2)}").performClick()
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.PickDay(today.plusDays(2))), QuickAddIntent.Grid(GridAction.DayTap(today.plusDays(2)))),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )

        // Галочка — снова шторка, занятость — с сегодняшнего дня.
        composeRule.onNodeWithTag(TAG_TIMELINE_DONE).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertExists()
        assertEquals(QuickAddIntent.Grid(GridAction.FirstDay(today)), intents.last())
    }

    @Test
    fun blockArrowsPageByTwoWeeksAndStayInTheFuture() {
        show { QuickAddScreen(quickState.copy(due = TaskDue.AllDay(today)), { intents += it }, {}, autoFocus = false) }
        openDateWindow()
        // На первой странице назад не листать — в прошлое нельзя.
        composeRule.onNodeWithTag(TAG_DATE_PREV).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_DATE_NEXT).performClick()
        composeRule.onNodeWithTag(TAG_DATE_RANGE).assertTextEquals("26 окт – 08 ноя")
        composeRule.onNodeWithTag(TAG_DATE_PREV).performClick()
        composeRule.onNodeWithTag(TAG_DATE_RANGE).assertTextEquals("12 окт – 25 окт")
        // Прошедший день бледный и недоступен.
        composeRule.onNodeWithTag("$TAG_DATE_CELL:${today.minusDays(1)}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$TAG_DATE_CELL:$today").assertIsEnabled()
    }

    @Test
    fun calendarPicksDayAtOnceAndCloses() {
        show { QuickAddScreen(quickState.copy(due = null), { intents += it }, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onNodeWithTag(TAG_DATE_CALENDAR).performClick()
        // Два месяца — октябрь и ноябрь; «Готово» нет, только «Закрыть».
        composeRule.onNodeWithTag("$TAG_CALENDAR_MONTH:2026-10").assertExists()
        composeRule.onNodeWithTag("$TAG_CALENDAR_MONTH:2026-11").assertExists()
        composeRule.onNodeWithTag("$TAG_CALENDAR_DIALOG:$TAG_DIALOG_DONE").assertDoesNotExist()
        // 20 ноября — вне блока «2 недели», поэтому такая ячейка одна, в календаре.
        composeRule.onNodeWithTag("$TAG_DATE_CELL:${LocalDate.of(2026, 11, 20)}").performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_DIALOG).assertDoesNotExist()
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.PickDay(LocalDate.of(2026, 11, 20)))),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun calendarCloseKeepsDate() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onNodeWithTag(TAG_DATE_CALENDAR).performClick()
        composeRule.onNodeWithTag("$TAG_CALENDAR_DIALOG:$TAG_DIALOG_CANCEL").performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_DIALOG).assertDoesNotExist()
        assertTrue(intents.none { it is QuickAddIntent.Grid && it.action is GridAction.PickDay })
    }

    @Test
    fun crossesClearDateAndTimeSeparately() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_CLEAR_TIME).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_DATE).performClick()
        // Итерация 50: после каждого крестика — «Готово» (у повторяющейся по нему решается, к чему применить).
        assertEquals(listOf<Any>(QuickAddIntent.Grid(GridAction.ClearTime), QuickAddIntent.Grid(GridAction.Done), QuickAddIntent.Grid(GridAction.ClearDate), QuickAddIntent.Grid(GridAction.Done)), intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay })
    }

    @Test
    fun farDateShowsInFields() {
        val far = today.plusDays(30)
        show { QuickAddScreen(quickState.copy(due = TaskDue.AtTime(far.atTime(9, 0).toInstant(zone))), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertTextEquals("пт, 13 ноя")
        composeRule.onNodeWithTag(TAG_TIME_TEXT, useUnmergedTree = true).assertTextEquals("09:00")
    }

    @Test
    fun periodTimeIsShownInTimeField() {
        show { QuickAddScreen(quickState.copy(start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TIME_TEXT, useUnmergedTree = true).assertTextEquals("10:00–11:30")
    }

    @Test
    fun repeatOpensFromPanel() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertIsSelected().performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").assertIsSelected()
    }

    @Test
    fun timelineButtons() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        openDateWindow()
        // Крестик на блоке — убрать время, день остаётся; «Без даты» — убрать дату и вернуться в шторку.
        composeRule.onNodeWithTag(TAG_GRID_CLEAR, useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag(TAG_TIMELINE_NO_DATE).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.ClearTime), QuickAddIntent.Grid(GridAction.ClearDate), QuickAddIntent.Grid(GridAction.Done)),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun backArrowCancelsChanges() {
        val before = Schedule(quickState.due, quickState.start)
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onNodeWithTag("$TAG_DATE_CELL:${today.plusDays(3)}").performClick()
        // «←» возвращает срок, каким он был при открытии окна, и закрывает окно.
        composeRule.onNodeWithTag(TAG_TIMELINE_BACK).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.PickDay(today.plusDays(3))), QuickAddIntent.Grid(GridAction.Restore(before))),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun intervalBlockResizeAndMove() {
        show { QuickAddScreen(quickState.copy(start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), { intents += it }, {}, autoFocus = false) }
        // Окно с завтрашним днём в центре: сегодня, завтра, послезавтра (колонка ≈ 125 dp).
        openDateWindow()
        composeRule.onNodeWithText("10:00\n11:30", useUnmergedTree = true).assertExists()

        // Ручка: вниз на час (≈ 43 dp: час подогнан под высоту окна) — +60 минут.
        composeRule.onNodeWithTag(TAG_GRID_RESIZE, useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(0f, 10f * density))
            moveBy(Offset(0f, 34f * density))
            up()
        }
        // Блок: долгое нажатие и перенос на день вправо и на полчаса вниз.
        composeRule.onNodeWithTag(TAG_GRID_SELECTION, useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(1000)
            moveBy(Offset(62f * density, 11f * density))
            moveBy(Offset(62f * density, 11f * density))
            up()
        }
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.Resize(150)), QuickAddIntent.Grid(GridAction.MoveSlot(today.plusDays(2), 10 * 60 + 30))),
            intents.filter { it is QuickAddIntent.Grid && (it.action is GridAction.Resize || it.action is GridAction.MoveSlot) },
        )
    }

    /** Как во ViewModel: «Добавить пункт» дописывает пустой пункт в конец чек-листа. */
    private fun statefulQuickAdd(initial: QuickAddState): androidx.compose.runtime.MutableState<QuickAddState> {
        val state = androidx.compose.runtime.mutableStateOf(initial)
        show {
            QuickAddScreen(
                state.value,
                { if (it == QuickAddIntent.AddChecklist) state.value = state.value.copy(checklist = state.value.checklist.orEmpty() + "") },
                {},
                autoFocus = false,
            )
        }
        return state
    }

    @Test
    fun nextOnChecklistItemMovesCursorToNewItem() {
        statefulQuickAdd(quickState.copy(checklist = listOf("Повестка")))
        composeRule.onNodeWithTag("$TAG_QUICK_CHECKLIST:0").performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$TAG_QUICK_CHECKLIST:1").assertIsFocused()
        composeRule.onNodeWithTag("$TAG_QUICK_CHECKLIST:0").assertIsNotFocused()
    }

    @Test
    fun checklistButtonPutsCursorInFirstItem() {
        statefulQuickAdd(quickState.copy(checklist = null))
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$TAG_QUICK_CHECKLIST:0").assertIsFocused()
    }

    @Test
    fun toolbarMenusSendEdits() {
        show { QuickAddScreen(quickState.copy(due = null, priority = Priority.NONE, checklist = null), { intents += it }, {}, autoFocus = false) }

        // Выбор в окне меняет задачу только по «Готово»; «Отмена» — без изменений.
        composeRule.onNodeWithTag(TAG_TOOL_LIST).performClick()
        composeRule.onNodeWithTag("$TAG_PROJECT_OPTION:inbox").performClick()
        assertTrue(intents.none { it is QuickAddIntent.Edit })
        composeRule.onNodeWithTag("$TAG_PROJECT_DIALOG:$TAG_DIALOG_DONE").performClick()
        // Итерация 47 (решение PM): окна приоритета нет — нажатие сразу включает высокий.
        composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).performClick()
        composeRule.onNodeWithTag(TAG_PRIORITY_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).performClick()

        assertEquals(
            listOf<Any>(
                QuickAddIntent.Edit(FieldEdit.Project("inbox")),
                QuickAddIntent.Edit(FieldEdit.SetPriority(Priority.HIGH)),
                QuickAddIntent.AddChecklist,
            ),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun repeatToolOpensListWithCustomRule() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).performClick()
        // Правило не из готовых — отмечено «Произвольно…» с описанием; конструктор открывается с ним же.
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").assertIsSelected()
        composeRule.onNodeWithText("Каждые 2 недели: вт, чт").assertExists()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").performClick()
        composeRule.onNodeWithTag(TAG_REPEAT_PREVIEW).assertTextEquals("Каждые 2 недели: вт, чт")
    }

    @Test
    fun repeatPresetsFollowTaskDate() {
        var result: String? = "не задано"
        var dismissed = false
        show { RepeatDialog(rrule = null, due = today, onConfirm = { result = it }, onDismiss = { dismissed = true }) }

        // 14.10.2026 — среда.
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:NONE").assertIsSelected()
        composeRule.onNodeWithText("по средам").assertExists()
        composeRule.onNodeWithText("14 числа").assertExists()
        composeRule.onNodeWithText("14 октября").assertExists()
        composeRule.onNodeWithText("пн–пт").assertExists()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:WORKDAYS").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:WORKDAYS").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:NONE").assertIsNotSelected()
        assertEquals("не задано", result)
        composeRule.onNodeWithTag("$TAG_REPEAT_DIALOG:$TAG_DIALOG_DONE").performClick()

        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", result)
        assertTrue(!dismissed)
    }

    @Test
    fun repeatCancelKeepsRule() {
        var result: String? = "не задано"
        var dismissed = false
        show { RepeatDialog(rrule = "FREQ=DAILY", due = today, onConfirm = { result = it }, onDismiss = { dismissed = true }) }
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:DAILY").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:YEARLY").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_DIALOG:$TAG_DIALOG_CANCEL").performClick()
        assertEquals("не задано", result)
        assertTrue(dismissed)
    }

    @Test
    fun customRepeatReturnsToList() {
        var result: String? = "не задано"
        show { RepeatDialog(rrule = "FREQ=DAILY", due = today, onConfirm = { result = it }, onDismiss = {}) }
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").performClick()

        // «Отмена» в конструкторе — назад к списку без изменений.
        composeRule.onNodeWithTag("repeat_freq:WEEKLY").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_CUSTOM_DIALOG:$TAG_DIALOG_CANCEL").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:DAILY").assertIsSelected()

        // «Готово» — правило конструктора отмечено «Произвольно…»; задача меняется только общим «Готово».
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").performClick()
        composeRule.onNodeWithTag(TAG_REPEAT_PLUS).performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_CUSTOM_DIALOG:$TAG_DIALOG_DONE").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").assertIsSelected()
        composeRule.onNodeWithText("Каждые 2 дня").assertExists()
        assertEquals("не задано", result)
        composeRule.onNodeWithTag("$TAG_REPEAT_DIALOG:$TAG_DIALOG_DONE").performClick()
        assertEquals("FREQ=DAILY;INTERVAL=2", result)
    }

    @Test
    fun repeatConstructorBuildsWeeklyRule() {
        var result: String? = "не задано"
        show { CustomRepeatDialog(rrule = null, due = today, onConfirm = { result = it }, onDismiss = {}) }

        composeRule.onNodeWithTag("repeat_freq:WEEKLY").performClick()
        composeRule.onNodeWithTag(TAG_REPEAT_PLUS).performClick()
        composeRule.onNodeWithTag("repeat_day:TUESDAY").performClick()
        composeRule.onNodeWithTag("repeat_day:FRIDAY").performClick()
        composeRule.onNodeWithTag(TAG_REPEAT_PREVIEW).assertTextEquals("Каждые 2 недели: вт, пт")
        composeRule.onNodeWithText("Готово").performClick()

        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,FR", result)
    }

    @Test
    fun repeatConstructorOpensSimpleRuleForEditing() {
        show { CustomRepeatDialog(rrule = "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", due = today, onConfirm = {}, onDismiss = {}) }
        composeRule.onNodeWithTag(TAG_REPEAT_PREVIEW).assertTextEquals("Каждый месяц")
        composeRule.onNodeWithText("Особый повтор из TickTick").assertDoesNotExist()
    }

    @Test
    fun complexRuleIsShownAndCanBeRemoved() {
        var result: String? = "не задано"
        show { RepeatDialog(rrule = "FREQ=MONTHLY;BYDAY=-1FR", due = today, onConfirm = { result = it }, onDismiss = {}) }

        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:CUSTOM").assertIsSelected()
        composeRule.onNodeWithText("Особый повтор из TickTick").assertExists()
        composeRule.onNodeWithTag("$TAG_REPEAT_OPTION:NONE").performClick()
        composeRule.onNodeWithTag("$TAG_REPEAT_DIALOG:$TAG_DIALOG_DONE").performClick()
        assertEquals(null, result)
    }

    @Test
    fun repeatNeedsDate() {
        show { RepeatDialog(rrule = null, due = null, onConfirm = {}, onDismiss = {}) }
        composeRule.onNodeWithText("Повтор начинается от даты задачи — сначала выберите дату.").assertExists()
    }

    private val detailState = TaskDetailState(
        task = task("t1", title = "Подготовить отчёт", projectId = "work", priority = Priority.HIGH, due = TaskDue.AllDay(today))
            .copy(content = "Квартальный отчёт для руководства", rrule = "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", reminders = listOf("P0DT10H0M0S", "-P6DT14H0M0S")),
        checklist = listOf(
            ChecklistItem("i1", "t1", "Собрать данные", isDone = true, sortOrder = 0, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
            ChecklistItem("i2", "t1", "Сверстать таблицу", isDone = false, sortOrder = 1, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH),
        ),
        subtasks = listOf(task("s1", title = "Запросить цифры у бухгалтерии")),
        projects = projects,
        today = today,
        zone = zone,
    )

    @Test
    fun detailShowsEverything() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }

        composeRule.onNodeWithText("Подготовить отчёт").assertExists()
        composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertTextEquals("ср, 14 окт")
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).assertIsSelected()
        composeRule.onNodeWithText("Собрать данные").assertExists()
        composeRule.onNodeWithText("Запросить цифры у бухгалтерии").assertExists()
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_CREATED).assertTextEquals("создано чт, 01 янв 2026")
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).assertExists()
    }

    /** Итерация 49: в «⋮» — «Не буду делать» над «Удалить»; у закрытой задачи его нет. */
    @Test
    fun detailMenuOffersWontDo() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_WONT_DO).performClick()
        assertEquals(TaskDetailIntent.WontDo, intents.last())
    }

    @Test
    fun detailClosedTaskHasNoWontDo() {
        show { TaskDetailScreen(detailState.copy(task = detailState.task!!.copy(status = TaskStatus.COMPLETED)), { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).assertExists()
        composeRule.onNodeWithTag(TAG_DETAIL_WONT_DO).assertDoesNotExist()
    }

    /** Итерация 49: будущий повтор — плашка «Будущий повтор · ближайший — …». */
    @Test
    fun detailFutureRepeatShowsBanner() {
        show { TaskDetailScreen(detailState.copy(occurrence = TaskDue.AllDay(today.plusMonths(1))), { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_DETAIL_FUTURE).assertExists()
    }

    /** Итерация 49: окно «Удалить серию повторов?» — три кнопки, «Не буду делать» закрывает раз. */
    @Test
    fun screenshotDeleteSeriesDialog() {
        var picked = ""
        show {
            ChoiceDialog(
                title = "Удалить серию повторов?",
                text = "Задача «Стирка» повторяется. Удалятся все её будущие повторы, выполненные останутся в истории.\n\nЧтобы убрать только этот повтор, нажмите «Не буду делать».",
                tag = TAG_DELETE_SERIES_DIALOG,
                choices = listOf(
                    Choice("Удалить серию", ChoiceStyle.Danger, TAG_CHOICE_DELETE) { picked = "delete" },
                    Choice("Не буду делать", ChoiceStyle.Outline, TAG_CHOICE_WONT_DO, com.prostovasei.simpletasks.R.drawable.ic_wont_do) { picked = "wont" },
                    Choice("Отмена", ChoiceStyle.Plain, TAG_CHOICE_CANCEL) { picked = "cancel" },
                ),
                onDismiss = {},
            )
        }
        composeRule.onRoot().captureRoboImage("screenshots/delete_series_dialog_light.png")
        composeRule.onNodeWithTag("$TAG_DELETE_SERIES_DIALOG:$TAG_CHOICE_WONT_DO").performClick()
        assertEquals("wont", picked)
    }

    /** Итерация 50: окно «Изменить повторяющуюся задачу» у ближайшего раза — обе кнопки работают, «Отмена» тоже. */
    @Test
    fun detailChangeRepeatDialogSendsChoice() {
        show { TaskDetailScreen(detailState.copy(dialog = DetailDialog.ChangeRepeat("вс, 08 ноя", "пн, 09 ноя 18:00")), { intents += it }, {}, {}) }

        composeRule.onNodeWithText("Изменить повторяющуюся задачу").assertExists()
        composeRule.onNodeWithTag("$TAG_CHANGE_REPEAT_DIALOG:$TAG_CHOICE_CURRENT").performClick()
        composeRule.onNodeWithTag("$TAG_CHANGE_REPEAT_DIALOG:$TAG_CHOICE_ALL").performClick()
        composeRule.onNodeWithTag("$TAG_CHANGE_REPEAT_DIALOG:$TAG_CHOICE_CANCEL").performClick()
        assertEquals(
            listOf(TaskDetailIntent.ChangeRepeat(all = false), TaskDetailIntent.ChangeRepeat(all = true), TaskDetailIntent.DismissDialog),
            intents.takeLast(3),
        )
    }

    /** У будущего раза «Текущий повтор» бледный и не нажимается, под ним подсказка с датой ближайшего. */
    @Test
    fun detailChangeRepeatDialogFutureDisablesCurrent() {
        show { TaskDetailScreen(detailState.copy(dialog = DetailDialog.ChangeRepeat("вс, 22 ноя", "пн, 23 ноя 18:00", nearest = "вс, 08 ноя")), { intents += it }, {}, {}) }

        composeRule.onNodeWithTag("$TAG_CHANGE_REPEAT_DIALOG:$TAG_CHOICE_CURRENT").assertIsNotEnabled()
        composeRule.onNodeWithText("Сначала закройте ближайший повтор — вс, 08 ноя").assertExists()
        composeRule.onNodeWithTag("$TAG_CHANGE_REPEAT_DIALOG:$TAG_CHOICE_ALL").performClick()
        assertEquals(TaskDetailIntent.ChangeRepeat(all = true), intents.last())
    }

    /** Скриншоты окна (макет iteration50.py): ближайший раз и будущий. */
    @Test
    fun screenshotChangeRepeatDialog() {
        var future by mutableStateOf(false)
        show {
            ChoiceDialog(
                title = "Изменить повторяющуюся задачу",
                text = if (future) "«Стирка»: вс, 22 ноя → пн, 23 ноя 18:00. Что перенести?" else "«Стирка»: вс, 08 ноя → пн, 09 ноя 18:00. Что перенести?",
                tag = TAG_CHANGE_REPEAT_DIALOG,
                choices = listOf(
                    Choice(
                        "Текущий повтор", if (future) ChoiceStyle.Outline else ChoiceStyle.Primary, TAG_CHOICE_CURRENT,
                        enabled = !future, hint = if (future) "Сначала закройте ближайший повтор — вс, 08 ноя" else null,
                    ) {},
                    Choice("Все повторы с этого момента", if (future) ChoiceStyle.Primary else ChoiceStyle.Outline, TAG_CHOICE_ALL) {},
                    Choice("Отмена", ChoiceStyle.Plain, TAG_CHOICE_CANCEL) {},
                ),
                onDismiss = {},
            )
        }
        composeRule.onRoot().captureRoboImage("screenshots/change_repeat_dialog_light.png")
        future = true
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("screenshots/change_repeat_dialog_future_light.png")
    }

    @Test
    fun detailReminderMenuTogglesOptions() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }

        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        // Своё напоминание задачи — в «Созданных вручную», хотя истории ещё нет; рядом — когда сработает.
        composeRule.onNodeWithText("СОЗДАННЫЕ ВРУЧНУЮ").assertExists()
        composeRule.onNodeWithText("За 1 неделю в 10:00").assertExists()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-P6DT14H0M0S").assertIsOn()
        composeRule.onNodeWithText("За 1 день в 10:00").performClick()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:P0DT10H0M0S").performClick()
        // Снятая галочка не убирает вариант из окна.
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:P0DT10H0M0S").assertIsOff()
        assertTrue(intents.none { it is TaskDetailIntent.Edit })
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_DONE").performClick()

        assertEquals(
            listOf<Any>(TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("-P6DT14H0M0S", "-P0DT14H0M0S")))),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
    }

    @Test
    fun reminderCancelChangesNothing() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithText("За 1 день в 10:00").performClick()
        composeRule.onNodeWithTag(TAG_REMINDER_CUSTOM).performClick()
        composeRule.onNodeWithTag(TAG_CUSTOM_ADD).performClick()
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_CANCEL").performClick()

        assertTrue(intents.none { it is TaskDetailIntent.Edit })
        // Созданный, но отменённый общей «Отменой» вариант в историю не попал.
        assertEquals(emptyList<String>(), ReminderHistory.load(ApplicationProvider.getApplicationContext(), allDay = true))
    }

    @Test
    fun detailToolbarKeepsGapsAndDoneCircleStaysAtLeftEdge() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }

        // Кнопки панели не слипаются: щель не меньше обычной (8 dp, с допуском на округление), галочка — в пределах окна.
        val list = composeRule.onNodeWithTag(TAG_TOOL_LIST).getBoundsInRoot()
        val reminder = composeRule.onNodeWithTag(TAG_TOOL_REMINDER).getBoundsInRoot()
        val close = composeRule.onNodeWithTag(TAG_DETAIL_CLOSE).getBoundsInRoot()
        assertTrue("щель между кнопками панели", (composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).getBoundsInRoot().left - list.right).value >= 7.5f)
        assertTrue("щель уведомления–галочка", (close.left - reminder.right).value >= 7.5f)
        assertTrue("кнопки в пределах окна", close.right <= composeRule.onNodeWithTag(TAG_QUICK_CARD).getBoundsInRoot().right)
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).assertDoesNotExist()

        // Отметка «выполнено» — вплотную к левому краю текста, как описание под названием.
        val done = composeRule.onNodeWithTag(TAG_DETAIL_DONE).getBoundsInRoot()
        val content = composeRule.onNodeWithTag(TAG_DETAIL_CONTENT).getBoundsInRoot()
        assertEquals(content.left.value, done.left.value, 0.5f)
    }

    @Test
    fun doneCircleToggles() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_DETAIL_DONE).assertIsOff().performClick()
        assertEquals(listOf<Any>(TaskDetailIntent.ToggleCompleted), intents.filterNot { it is TaskDetailIntent.Grid })
    }

    /** Итерация 47: повторное нажатие на включённый приоритет выключает его; крестика у кнопки приоритета нет. */
    @Test
    fun priorityButtonTogglesOff() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_CLEAR_PRIORITY).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).performClick()
        assertEquals(listOf<Any>(TaskDetailIntent.Edit(FieldEdit.SetPriority(Priority.NONE))), intents.filterNot { it is TaskDetailIntent.Grid })
    }

    @Test
    fun clearBadgesResetOnlyTheirField() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_CLEAR_LIST).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_DATE).performClick()
        assertEquals(
            listOf<Any>(
                TaskDetailIntent.Edit(FieldEdit.Project(projects.first { it.isInbox }.id)),
                TaskDetailIntent.Grid(GridAction.ClearDate),
                TaskDetailIntent.Grid(GridAction.Done),
            ),
            intents.filterNot { it is TaskDetailIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun detailGridAndClose() {
        var closed = false
        show { TaskDetailScreen(detailState, { intents += it }, { closed = true }, {}) }
        // Другой день в блоке окна даты, затем галочка — снова шторка.
        openDateWindow()
        composeRule.onNodeWithTag("$TAG_DATE_CELL:${today.plusDays(1)}").performClick()
        composeRule.onNodeWithTag(TAG_TIMELINE_DONE).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_CLOSE).performClick()
        assertEquals(listOf<Any>(TaskDetailIntent.Grid(GridAction.PickDay(today.plusDays(1))), TaskDetailIntent.Grid(GridAction.Done)), intents.filterNot { it is TaskDetailIntent.Grid && it.action is GridAction.FirstDay })
        assertTrue(closed)
    }

    @Test
    fun keyboardOkDoesNotSave() {
        show { QuickAddScreen(quickState.copy(title = "Купить хлеб"), { intents += it }, {}, autoFocus = false) }
        // «ОК» на клавиатуре только убирает её; сохраняет задачу зелёная галочка (итерация 24).
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performImeAction()
        assertTrue(intents.none { it == QuickAddIntent.Save })
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        assertTrue(intents.any { it == QuickAddIntent.Save })
    }

    @Test
    fun quickAddReminderMenu() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertIsSelected().performClick()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:PT0S").assertIsOn()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-PT30M").performClick()
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_DONE").performClick()
        assertEquals(
            listOf<Any>(QuickAddIntent.Edit(FieldEdit.SetReminders(listOf("PT0S", "-PT30M")))),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun customReminderCountsDaysHoursMinutes() {
        var result: String? = null
        val due = TaskDue.AtTime(today.plusDays(3).atTime(18, 0).atZone(zone).toInstant())
        show { CustomReminderDialog(due = due, start = null, zone = zone, onConfirm = { result = it }, onDismiss = {}) }
        repeat(2) { composeRule.onNodeWithTag("$TAG_CUSTOM_DAYS:plus").performClick() }
        repeat(3) { composeRule.onNodeWithTag("$TAG_CUSTOM_HOURS:plus").performClick() }
        repeat(6) { composeRule.onNodeWithTag("$TAG_CUSTOM_MINUTES:plus").performClick() }
        composeRule.onNodeWithTag(TAG_CUSTOM_FIRES).assertTextContains("12:30", substring = true)
        composeRule.onNodeWithTag(TAG_CUSTOM_ADD).performClick()
        assertEquals("-P2DT5H30M0S", result)
    }

    @Test
    fun reminderMenuOffersCustom() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithTag(TAG_REMINDER_CUSTOM).performClick()
        composeRule.onNodeWithTag(TAG_CUSTOM_DAYS).assertTextEquals("01")
        composeRule.onNodeWithTag(TAG_CUSTOM_ADD).performClick()
        // Вариант вернулся в окно уведомлений отмеченным; задача меняется общим «Готово».
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-P0DT15H0M0S").assertIsOn()
        assertTrue(intents.none { it is TaskDetailIntent.Edit })
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_DONE").performClick()
        assertEquals(
            listOf<Any>(TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("P0DT10H0M0S", "-P6DT14H0M0S", "-P0DT15H0M0S")))),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
        assertEquals(listOf("-P0DT15H0M0S"), ReminderHistory.load(ApplicationProvider.getApplicationContext(), allDay = true))
    }

    @Test
    fun reminderHistoryIsSharedAndKeepsAssigned() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ReminderHistory.add(context, allDay = false, created = listOf("-PT2H", "-PT3H", "-PT4H", "-PT45M"))
        // У задачи назначено «за 2 часа» — оно выпало из тройки истории, но видно и отмечено.
        show { QuickAddScreen(quickState.copy(customReminders = listOf("-PT2H")), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        listOf("-PT45M", "-PT4H", "-PT3H").forEach { composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:$it").assertIsOff() }
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-PT2H").assertIsOn()
    }

    @Test
    fun detailActions() {
        var opened: String? = null
        show { TaskDetailScreen(detailState, { intents += it }, {}, { opened = it }) }

        composeRule.onNodeWithTag(TAG_DETAIL_DONE).performClick()
        composeRule.onNodeWithTag("item_menu:i1").performScrollTo().performClick()
        composeRule.onNodeWithText("Ниже").performClick()
        composeRule.onNodeWithTag("subtask:s1").performScrollTo().performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()

        assertEquals(
            listOf<Any>(TaskDetailIntent.ToggleCompleted, TaskDetailIntent.MoveItem("i1", up = false), TaskDetailIntent.RequestDelete),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
        assertEquals("s1", opened)
    }

    /** Р1 (0.29): из виджета окно растёт из плашки — посередине пути белая «плашка» между ней и окном, текста ещё нет. */
    @Test
    fun screenshotQuickAddGrowing() {
        show {
            CompositionLocalProvider(LocalSheetOrigin provides android.graphics.Rect(40, 120, 560, 200)) {
                QuickAddScreen(quickState, {}, {}, autoFocus = false, progress = { 0.5f }, reveal = { 0f })
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_growing.png")
    }

    @Test
    fun screenshotQuickAdd() {
        show { QuickAddScreen(quickState.copy(busy = busy, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_light.png")
    }

    @Test
    fun screenshotTimeline() {
        show { QuickAddScreen(quickState.copy(busy = busy, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_timeline_light.png")
    }

    /** Широкий экран и крупный шрифт (как на телефоне PM): 3 дня и край четвёртого, «окт» не обрезается. */
    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi", fontScale = 1.3f)
    fun screenshotQuickAddLargeFont() {
        show { QuickAddScreen(quickState.copy(checklist = null, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_large_font.png")
    }

    /** Без даты: иконки повтора и уведомлений бледнее остальных (0.31.2). */
    @Test
    fun screenshotQuickAddNoDate() {
        show { QuickAddScreen(QuickAddState(title = "Купить хлеб", projectId = "inbox", projects = projects, today = today, zone = zone), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_no_date.png")
    }

    @Test
    fun screenshotQuickAddDark() {
        show(dark = true) { QuickAddScreen(quickState.copy(busy = busy, checklist = null), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_dark.png")
    }

    @Test
    fun screenshotDetailLight() {
        show { TaskDetailScreen(detailState, {}, {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/task_detail_light.png")
    }

    @Test
    fun screenshotDetailDark() {
        show(dark = true) { TaskDetailScreen(detailState, {}, {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/task_detail_dark.png")
    }

    @Test
    fun screenshotRepeatDialog() {
        show { RepeatDialog(rrule = "FREQ=WEEKLY", due = today, onConfirm = {}, onDismiss = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/repeat_dialog_light.png")
    }

    @Test
    fun screenshotRepeatCustomDialog() {
        show { CustomRepeatDialog(rrule = "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH", due = today, onConfirm = {}, onDismiss = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/repeat_custom_dialog_light.png")
    }

    @Test
    fun screenshotRemindersDialog() {
        ReminderHistory.add(ApplicationProvider.getApplicationContext(), allDay = false, created = listOf("-PT2H", "-PT45M"))
        show { RemindersDialog(quickState.due, null, zone, listOf("PT0S", "-PT1H", "-P0DT2H15M0S"), {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/reminders_dialog_light.png")
    }

    @Test
    fun screenshotRemindersDialogDark() {
        show(dark = true) { RemindersDialog(quickState.due, null, zone, listOf("PT0S"), {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/reminders_dialog_dark.png")
    }

    @Test
    fun screenshotProjectDialog() {
        show { ProjectDialog("work", projects, {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/project_dialog_light.png")
    }

    @Test
    fun screenshotCalendarDialog() {
        show { QuickAddScreen(quickState, {}, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onNodeWithTag(TAG_DATE_CALENDAR).performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_DIALOG).captureRoboImage("screenshots/calendar_dialog_light.png")
    }

    /** Маленький экран и крупный шрифт: окно даты — сетка, блок и нижняя строка помещаются. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun screenshotWindowSmallLargeFont() {
        show { QuickAddScreen(quickState.copy(busy = busy), {}, {}, autoFocus = false) }
        openDateWindow()
        composeRule.onNodeWithTag(TAG_TIMELINE_DONE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DATE_CALENDAR).assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("screenshots/date_window_small_large_font.png")
    }

    /** Телефон PM (412 dp, шрифт 130%): подпись недель в нижней строке окна помещается целиком. */
    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi", fontScale = 1.3f)
    fun screenshotWindowLargeFont() {
        show { QuickAddScreen(quickState.copy(busy = busy), {}, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_DATE_FIELD).performClick()
        composeRule.onNodeWithTag(TAG_DATE_RANGE).assertTextEquals("12 окт – 25 окт")
        composeRule.onRoot().captureRoboImage("screenshots/date_window_large_font.png")
    }

    /** Несколько уведомлений — колокольчик со стрелкой и точкой в панели (вариант «Г»). */
    @Test
    fun screenshotMultipleReminders() {
        show { QuickAddScreen(quickState.copy(customReminders = listOf("PT0S", "-PT30M")), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_multi.png")
    }

    /** Маленький экран и крупный шрифт (ТЗ PM): длинный список прокручивается, «Отмена» и «Готово» видны. */
    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi", fontScale = 1.5f)
    fun smallScreenLargeFontKeepsButtons() {
        ReminderHistory.add(ApplicationProvider.getApplicationContext(), allDay = false, created = listOf("-PT2H", "-PT45M", "-PT4H"))
        show { RemindersDialog(quickState.due, null, zone, listOf("PT0S"), {}, {}) }
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_DONE").assertIsDisplayed()
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_CANCEL").assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_REMINDER_CUSTOM).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("$TAG_REMINDERS_DIALOG:$TAG_DIALOG_DONE").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("screenshots/reminders_dialog_small_large_font.png")
    }

    // ---------------------------------------------------------------- итерация 41: дата и время в названии

    private fun smartState(title: String): QuickAddState {
        val found = com.prostovasei.simpletasks.domain.smartdate.SmartDateParser.parse(title, today, 9 * 60)!!
        return QuickAddState(
            title = title, projectId = "inbox", projects = projects, today = today, zone = zone,
            due = found.due(zone), smart = SmartTitle(found = found),
        )
    }

    @Test
    fun smartDateChipShowsDueAndCrossTurnsItOff() {
        show { QuickAddScreen(smartState("Созвон завтра в 10:00"), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.designsystem.components.TAG_SMART_CHIP).assertExists()
        composeRule.onNodeWithText("Завтра, 10:00").assertExists()
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.designsystem.components.TAG_SMART_CHIP_OFF).performClick()
        assertEquals(QuickAddIntent.DismissSmartDate, intents.last())
    }

    @Test
    fun noSmartChipWithoutPhrase() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.designsystem.components.TAG_SMART_CHIP).assertDoesNotExist()
    }

    @Test
    fun screenshotSmartDate() {
        show { QuickAddScreen(smartState("Созвон завтра в 10:00"), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/smart_date_light.png")
    }

    @Test
    fun screenshotSmartDateDark() {
        show(dark = true) { QuickAddScreen(smartState("Отчёт в пятницу"), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/smart_date_dark.png")
    }
}
