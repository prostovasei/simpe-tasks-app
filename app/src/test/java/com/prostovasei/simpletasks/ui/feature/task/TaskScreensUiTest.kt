package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onNodeWithContentDescription
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

        composeRule.onNodeWithTag(TAG_QUICK_WHEN).assertTextEquals("Завтра, 10:00")
        composeRule.onNodeWithTag("$TAG_GRID_DAY:${today.plusDays(1)}").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_GRID_DAY:$today").assertIsNotSelected()
        composeRule.onNodeWithTag(TAG_GRID_SELECTION, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_PRIORITY).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_REPEAT).assertIsSelected()
        composeRule.onNodeWithTag(TAG_TOOL_CHECKLIST).assertIsSelected()
        composeRule.onNodeWithText("Повестка").assertExists()
    }

    @Test
    fun quickAddWithoutDate() {
        show { QuickAddScreen(QuickAddState(projectId = "inbox", projects = projects, today = today, zone = zone), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_QUICK_WHEN).assertTextEquals("Без даты")
        composeRule.onNodeWithTag(TAG_GRID_SELECTION, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_TOOL_LIST).assertIsNotSelected()
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).assertIsNotEnabled()

        // Без даты напоминание не выбрать: меню подсказывает выбрать день.
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).performClick()
        composeRule.onNodeWithText("Сначала выберите день на сетке").assertExists()
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
    fun gridTapsSendDayAndTime() {
        show { QuickAddScreen(quickState.copy(due = TaskDue.AllDay(today)), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_QUICK_WHEN).assertTextEquals("Сегодня, весь день")

        composeRule.onNodeWithTag("$TAG_GRID_DAY:${today.plusDays(2)}").performClick()
        // Точка в начале колонки «сегодня», 2:00 по высоте часа (сетка прокручена к полуночи — «сейчас» в состоянии 0:00).
        val hour = 44f
        composeRule.onNodeWithTag(TAG_WEEK_GRID).performTouchInput {
            val dp = density
            click(Offset(5f * dp, (2 * hour + 2f) * dp))
        }
        assertEquals(
            listOf<Any>(QuickAddIntent.DayTap(today.plusDays(2)), QuickAddIntent.SlotTap(today, 2 * 60)),
            intents.filterNot { it is QuickAddIntent.GridDays },
        )
    }

    @Test
    fun clearTimeCross() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_GRID_CLEAR, useUnmergedTree = true).performClick()
        assertTrue(QuickAddIntent.ClearTime in intents)
    }

    @Test
    fun intervalBlockResizeAndMove() {
        show { QuickAddScreen(quickState.copy(durationMinutes = 90), { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_QUICK_WHEN).assertTextEquals("Завтра, 10:00–11:30")
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
            moveBy(Offset(30f * density, 11f * density))
            moveBy(Offset(30f * density, 11f * density))
            up()
        }
        assertEquals(
            listOf<Any>(QuickAddIntent.Resize(150), QuickAddIntent.MoveSlot(today.plusDays(2), 10 * 60 + 30)),
            intents.filter { it is QuickAddIntent.Resize || it is QuickAddIntent.MoveSlot },
        )
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
            intents.filterNot { it is QuickAddIntent.GridDays },
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
        composeRule.onNodeWithTag(TAG_CHIP_DATE).assertTextEquals("Сегодня")
        composeRule.onNodeWithTag(TAG_CHIP_REPEAT).assertTextEquals("Каждый месяц: 14-го")
        composeRule.onNodeWithTag(TAG_CHIP_REMINDER).assertTextEquals("10:00 +1")
        composeRule.onNodeWithText("Собрать данные").assertExists()
        composeRule.onNodeWithText("Запросить цифры у бухгалтерии").assertExists()
        composeRule.onNodeWithText("Создана 1 января 2026").assertExists()
    }

    @Test
    fun reminderMenuTogglesOptions() {
        show { TaskDetailScreen(detailState, { intents += it }, {}, {}) }

        composeRule.onNodeWithTag(TAG_CHIP_REMINDER).performScrollTo().performClick()
        composeRule.onNodeWithText("За 1 неделю в 10:00").assertExists()
        composeRule.onNodeWithText("За 1 день в 10:00").performClick()
        composeRule.onNodeWithTag(TAG_CHIP_REMINDER).performScrollTo().performClick()
        composeRule.onNodeWithText("Без напоминания").performClick()

        assertEquals(
            listOf<Any>(
                TaskDetailIntent.Edit(FieldEdit.SetReminders(listOf("P0DT10H0M0S", "-P6DT14H0M0S", "-P0DT14H0M0S"))),
                TaskDetailIntent.Edit(FieldEdit.SetReminders(emptyList())),
            ),
            intents,
        )
    }

    @Test
    fun quickAddReminderMenu() {
        show { QuickAddScreen(quickState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithTag(TAG_TOOL_REMINDER).assertIsSelected().performClick()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:PT0S").assertExists()
        composeRule.onNodeWithTag("$TAG_REMINDER_OPTION:-PT30M").performClick()
        assertEquals(
            listOf<Any>(QuickAddIntent.Edit(FieldEdit.SetReminders(listOf("PT0S", "-PT30M")))),
            intents.filterNot { it is QuickAddIntent.GridDays },
        )
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
        show { QuickAddScreen(quickState.copy(busy = busy, durationMinutes = 90), {}, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/quick_add_light.png")
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
