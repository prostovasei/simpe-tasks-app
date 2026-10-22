package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
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
        ProjectOption("inbox", "Inbox", null, isInbox = true),
        ProjectOption("work", "Работа", "#3D8BFD", isInbox = false),
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

    private val quickState = QuickAddState(
        title = "Созвон с командой",
        projectId = "work",
        priority = Priority.MEDIUM,
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

        // В шторке — плитки дней (итерация 22): выбранный день зелёный, на нём время; сетки нет.
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(1)}").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_DAY_TILE:$today").assertIsNotSelected()
        composeRule.onNodeWithText("10:00").assertExists()
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
        // По умолчанию ни один день не выбран, времени на плитках нет; подсказки — примеры.
        (0L until 3L).forEach { composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(it)}").assertIsNotSelected() }
        composeRule.onNodeWithTag(TAG_DAY_TIME, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Купить билеты на концерт").assertExists()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsNotSelected()
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).assertIsNotEnabled()

        // Без даты напоминание не выбрать: меню подсказывает выбрать день.
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithText("Сначала выберите день").assertExists()
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
    fun stripDayOpensTimelineCenteredOnIt() {
        show { QuickAddScreen(quickState.copy(due = TaskDue.AllDay(today)), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag("$TAG_DAY_TILE:$today").assertIsSelected()

        // Нажали на «послезавтра» — таймлайн на 3 дня: завтра, послезавтра (в центре), день после.
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(2)}").performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertExists()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_GRID_DAY:${today.plusDays(2)}").performClick()
        // Точка в начале первой колонки (завтра), 2:00 по высоте часа (сетка прокручена к полуночи — «сейчас» в состоянии 0:00).
        val hour = 44f
        composeRule.onNodeWithTag(TAG_WEEK_GRID).performTouchInput {
            val dp = density
            click(Offset(5f * dp, (2 * hour + 2f) * dp))
        }
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.DayTap(today.plusDays(2))), QuickAddIntent.Grid(GridAction.SlotTap(today.plusDays(1), 2 * 60))),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )

        // Галочка — снова шторка, занятость — с сегодняшнего дня.
        composeRule.onNodeWithTag(TAG_TIMELINE_DONE).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertExists()
        assertEquals(QuickAddIntent.Grid(GridAction.FirstDay(today)), intents.last())
    }

    @Test
    fun calendarOpensMonthThenTimeline() {
        show { QuickAddScreen(quickState.copy(due = null), { intents += it }, {}, autoFocus = false) }
        // Значок календаря — месяц; «Готово» — таймлайн с выбранным днём.
        composeRule.onNodeWithTag(TAG_DAY_CALENDAR).performClick()
        composeRule.onNodeWithTag(TAG_DATE_DIALOG_OK).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertExists()
        composeRule.onNodeWithTag("$TAG_GRID_DAY:$today").assertExists()
    }

    @Test
    fun farDateShowsAsTileAfterCalendar() {
        val far = today.plusDays(30)
        show { QuickAddScreen(quickState.copy(due = TaskDue.AtTime(far.atTime(9, 0).toInstant(zone))), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag("$TAG_DAY_TILE:$far").assertIsSelected()
        composeRule.onNodeWithText("09:00").assertExists()
    }

    @Test
    fun periodTimeIsStackedOnTile() {
        show { QuickAddScreen(quickState.copy(start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), { intents += it }, {}, autoFocus = false) }
        // Период — начало над концом, без чёрточки.
        composeRule.onNodeWithText("10:00").assertExists()
        composeRule.onNodeWithText("11:30").assertExists()
    }

    @Test
    fun timelineButtons() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(1)}").performClick()
        // Крестик на блоке — убрать время; стрелок и подписи сверху нет.
        composeRule.onNodeWithTag(TAG_GRID_CLEAR, useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag(TAG_TIMELINE_NO_DATE).assertExists()
        // «Без времени» — оставить только день и вернуться к задаче.
        composeRule.onNodeWithTag(TAG_TIMELINE_NO_TIME).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_GRID).assertDoesNotExist()
        assertEquals(
            listOf<Any>(QuickAddIntent.Grid(GridAction.ClearTime), QuickAddIntent.Grid(GridAction.ClearTime)),
            intents.filterNot { it is QuickAddIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun intervalBlockResizeAndMove() {
        show { QuickAddScreen(quickState.copy(start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), { intents += it }, {}, autoFocus = false) }
        // Таймлайн с завтрашним днём в центре: сегодня, завтра, послезавтра (колонка ≈ 125 dp).
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(1)}").performClick()
        composeRule.onNodeWithText("10:00\n11:30", useUnmergedTree = true).assertExists()

        // Ручка: вниз на час (44dp) — +60 минут.
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

        composeRule.onNodeWithTag(TAG_TOOL_LIST).performClick()
        composeRule.onNodeWithTag("$TAG_PROJECT_OPTION:inbox").performClick()
        composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).performClick()
        composeRule.onNodeWithText("P1 Высокий").performClick()
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
    fun repeatToolOpensConstructor() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).performClick()
        composeRule.onNodeWithTag(TAG_REPEAT_PREVIEW).assertTextEquals("Каждые 2 недели: вт, чт")
    }

    @Test
    fun repeatConstructorBuildsWeeklyRule() {
        var result: String? = "не задано"
        show { RepeatDialog(rrule = null, due = today, onConfirm = { result = it }, onDismiss = {}) }

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
        show { RepeatDialog(rrule = "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", due = today, onConfirm = {}, onDismiss = {}) }
        composeRule.onNodeWithTag(TAG_REPEAT_PREVIEW).assertTextEquals("Каждый месяц")
        composeRule.onNodeWithText("Особый повтор из TickTick").assertDoesNotExist()
    }

    @Test
    fun complexRuleIsShownAndCanBeRemoved() {
        var result: String? = "не задано"
        show { RepeatDialog(rrule = "FREQ=MONTHLY;BYDAY=-1FR", due = today, onConfirm = { result = it }, onDismiss = {}) }

        composeRule.onNodeWithText("Особый повтор из TickTick").assertExists()
        composeRule.onNodeWithText("Не повторять").performClick()
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
        composeRule.onNodeWithTag("$TAG_DAY_TILE:$today").assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).assertIsSelected()
        composeRule.onNodeWithText("Собрать данные").assertExists()
        composeRule.onNodeWithText("Запросить цифры у бухгалтерии").assertExists()
        composeRule.onNodeWithText("Создана 1 января 2026").assertExists()
    }

    @Test
    fun detailReminderMenuTogglesOptions() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }

        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithText("За 1 неделю в 10:00").assertExists()
        composeRule.onNodeWithText("За 1 день в 10:00").performClick()
        // Меню не закрылось — сразу «Без напоминания».
        composeRule.onNodeWithText("Без напоминания").performClick()

        assertEquals(
            listOf<Any>(
                TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("P0DT10H0M0S", "-P6DT14H0M0S", "-P0DT14H0M0S"))),
                TaskDetailIntent.Edit(FieldEdit.SetReminders(emptyList())),
            ),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
    }

    @Test
    fun clearBadgesResetOnlyTheirField() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }
        composeRule.onNodeWithTag(TAG_CLEAR_REMINDERS).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_REPEAT).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_PRIORITY).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_LIST).performClick()
        composeRule.onNodeWithTag(TAG_CLEAR_DATE).performClick()
        assertEquals(
            listOf<Any>(
                TaskDetailIntent.Edit(FieldEdit.SetReminders(emptyList())),
                TaskDetailIntent.Edit(FieldEdit.Repeat(null)),
                TaskDetailIntent.Edit(FieldEdit.SetPriority(Priority.NONE)),
                TaskDetailIntent.Edit(FieldEdit.Project(projects.first { it.isInbox }.id)),
                TaskDetailIntent.Grid(GridAction.ClearDate),
            ),
            intents.filterNot { it is TaskDetailIntent.Grid && it.action is GridAction.FirstDay },
        )
    }

    @Test
    fun detailGridAndClose() {
        var closed = false
        show { TaskDetailScreen(detailState, { intents += it }, { closed = true }, {}) }
        // Другой день, затем «Без времени» в таймлайне — только этот день, снова шторка.
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(1)}").performClick()
        composeRule.onNodeWithTag(TAG_TIMELINE_NO_TIME).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_CLOSE).performClick()
        assertEquals(listOf<Any>(TaskDetailIntent.Grid(GridAction.DayTap(today.plusDays(1)))), intents.filterNot { it is TaskDetailIntent.Grid && it.action is GridAction.FirstDay })
        assertTrue(closed)
    }

    @Test
    fun quickAddReminderMenu() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertIsSelected().performClick()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:PT0S").assertExists()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-PT30M").performClick()
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
        assertEquals(
            listOf<Any>(TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("P0DT10H0M0S", "-P6DT14H0M0S", "-P0DT15H0M0S")))),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
    }

    @Test
    fun detailActions() {
        var opened: String? = null
        show { TaskDetailScreen(detailState, { intents += it }, {}, { opened = it }) }

        composeRule.onNodeWithTag(TAG_DETAIL_DONE).performClick()
        composeRule.onNodeWithTag("item_menu:i1").performScrollTo().performClick()
        composeRule.onNodeWithText("Ниже").performClick()
        composeRule.onNodeWithTag("subtask:s1").performScrollTo().performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()

        assertEquals(
            listOf<Any>(TaskDetailIntent.ToggleCompleted, TaskDetailIntent.MoveItem("i1", up = false), TaskDetailIntent.RequestDelete),
            intents.filterNot { it is TaskDetailIntent.Grid },
        )
        assertEquals("s1", opened)
    }

    @Test
    fun screenshotQuickAdd() {
        show { QuickAddScreen(quickState.copy(busy = busy, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_light.png")
    }

    @Test
    fun screenshotTimeline() {
        show { QuickAddScreen(quickState.copy(busy = busy, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        composeRule.onNodeWithTag("$TAG_DAY_TILE:${today.plusDays(1)}").performClick()
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_timeline_light.png")
    }

    /** Широкий экран и крупный шрифт (как на телефоне PM): 3 дня и край четвёртого, «окт» не обрезается. */
    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi", fontScale = 1.3f)
    fun screenshotQuickAddLargeFont() {
        show { QuickAddScreen(quickState.copy(checklist = null, start = TaskDue.AtTime(Instant.parse("2026-10-15T10:00:00Z")), due = TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z"))), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_large_font.png")
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
        show { RepeatDialog(rrule = "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,TH", due = today, onConfirm = {}, onDismiss = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/repeat_dialog_light.png")
    }
}
