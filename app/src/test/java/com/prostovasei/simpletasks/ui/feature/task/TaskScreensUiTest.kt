package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

        composeRule.onNodeWithTag(TAG_CHIP_DATE).assertTextEquals("Завтра")
        composeRule.onNodeWithTag(TAG_CHIP_TIME).assertTextEquals("10:00")
        composeRule.onNodeWithTag(TAG_CHIP_LIST).assertTextEquals("Работа")
        composeRule.onNodeWithTag(TAG_CHIP_PRIORITY).assertTextEquals("P2")
        composeRule.onNodeWithTag(TAG_CHIP_REPEAT).assertTextEquals("Каждые 2 недели: вт, чт")
        composeRule.onNodeWithText("Повестка").assertExists()
    }

    @Test
    fun quickAddEmptyTitleCannotBeSaved() {
        show { QuickAddScreen(QuickAddState(projectId = "inbox", projects = projects, today = today, zone = zone), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_CHIP_LIST).assertTextEquals("Входящие")
    }

    @Test
    fun dateMenuSetsTomorrowAndPriorityMenuSetsP1() {
        show { QuickAddScreen(quickState.copy(due = null, priority = Priority.NONE, checklist = null), { intents += it }, {}, autoFocus = false) }

        composeRule.onNodeWithTag(TAG_CHIP_DATE).performClick()
        composeRule.onNodeWithText("Завтра").performClick()
        composeRule.onNodeWithTag(TAG_CHIP_PRIORITY).performClick()
        composeRule.onNodeWithText("P1 Высокий").performClick()
        composeRule.onNodeWithTag(TAG_CHIP_CHECKLIST).performScrollTo().performClick()

        assertEquals(
            listOf<Any>(
                QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AllDay(today.plusDays(1)))),
                QuickAddIntent.Edit(FieldEdit.SetPriority(Priority.HIGH)),
                QuickAddIntent.AddChecklist,
            ),
            intents,
        )
    }

    @Test
    fun timeMenuSwitchesToAllDay() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_CHIP_TIME).performClick()
        composeRule.onNodeWithText("Весь день").performClick()
        assertEquals(listOf<Any>(QuickAddIntent.Edit(FieldEdit.Due(TaskDue.AllDay(today.plusDays(1))))), intents)
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
            .copy(content = "Квартальный отчёт для руководства", rrule = "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", reminders = listOf("PT0S", "-PT10080M")),
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
        composeRule.onNodeWithTag(TAG_CHIP_DATE).assertTextEquals("Сегодня")
        composeRule.onNodeWithTag(TAG_CHIP_REPEAT).assertTextEquals("Каждый месяц: 14-го")
        composeRule.onNodeWithText("В момент срока, За 1 неделю").assertExists()
        composeRule.onNodeWithText("Собрать данные").assertExists()
        composeRule.onNodeWithText("Запросить цифры у бухгалтерии").assertExists()
        composeRule.onNodeWithText("Создана 1 января 2026").assertExists()
    }

    @Test
    fun detailActions() {
        var opened: String? = null
        show { TaskDetailScreen(detailState, { intents += it }, {}, { opened = it }) }

        composeRule.onNodeWithTag(TAG_DETAIL_DONE).performClick()
        composeRule.onNodeWithTag("item_menu:i1").performClick()
        composeRule.onNodeWithText("Ниже").performClick()
        composeRule.onNodeWithTag("subtask:s1").performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithText("Удалить").performClick()

        assertEquals(
            listOf<Any>(TaskDetailIntent.ToggleCompleted, TaskDetailIntent.MoveItem("i1", up = false), TaskDetailIntent.RequestDelete),
            intents,
        )
        assertEquals("s1", opened)
    }

    @Test
    fun screenshotQuickAdd() {
        show { QuickAddScreen(quickState, {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_light.png")
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
