package com.prostovasei.simpletasks.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.task.ProjectOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun item(id: String, title: String, due: DueText?, list: String? = "Работа", ghostOf: String? = null, priority: Priority = Priority.NONE) =
        TaskItem(id, title, priority, due, list, null, isInboxList = list == null, isRecurring = ghostOf != null, ghostOf = ghostOf)

    private val today = LocalDate.of(2026, 10, 16)

    private val model = WidgetModel(
        WidgetFilter.All,
        null,
        today,
        listOf(
            WidgetDayBlock(WidgetDay.Overdue, listOf(item("late", "Отправить отчёт", DueText(DayLabel.Yesterday, null), priority = Priority.HIGH))),
            WidgetDayBlock(WidgetDay.On(today), listOf(item("gym", "Зарядка", DueText(DayLabel.Today, "08:00"), list = null))),
            WidgetDayBlock(WidgetDay.On(today.plusDays(3)), listOf(item("gym@0", "Зарядка", DueText(DayLabel.Date(today.plusDays(3), false), "08:00"), list = null, ghostOf = "gym"))),
        ),
    )

    @Test
    fun widgetShowsDayBlocksAndBottomBar() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 400.dp))
        setContext(context)
        provideComposable { WidgetContent(model, appWidgetId = 7) }

        // Нижняя строка: список, сегодняшняя дата; числа задач нет.
        onNode(hasText("Все")).assertExists()
        onNode(hasTextEqualTo("16 окт")).assertExists()
        onAllNodes(hasText("2")).assertCountEquals(0)
        // Подписи дней.
        onNode(hasText("ПРОСРОЧЕНО")).assertExists()
        onNode(hasText("СЕГОДНЯ")).assertExists()
        onNode(hasTextEqualTo(" · 16 окт")).assertExists()
        onNode(hasText("ПОНЕДЕЛЬНИК")).assertExists()
        // У просроченной — день, у остальных — только время и список.
        onNode(hasText("Вчера · Работа")).assertExists()
        onNode(hasText("08:00 · Входящие ↻")).assertExists()
        // Галочки только у двух настоящих задач: у будущего повтора её нет.
        onAllNodes(hasContentDescription("Выполнить")).assertCountEquals(2)
    }

    @Test
    fun emptyListWidget() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 200.dp))
        setContext(context)
        provideComposable { WidgetContent(WidgetModel(WidgetFilter.ProjectList("p"), "Покупки", today, emptyList()), appWidgetId = 7) }
        onNode(hasText("Покупки")).assertExists()
        onNode(hasText("Задач нет")).assertExists()
        onNode(hasText("16 окт")).assertExists()
    }

    @Test
    fun priorityPicksCheckColor() {
        assertEquals(widgetCheckColors(Priority.HIGH), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkHigh)
        assertEquals(widgetCheckColors(Priority.MEDIUM), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkMedium)
        assertEquals(widgetCheckColors(Priority.LOW), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkLow)
        assertEquals(widgetCheckColors(Priority.NONE), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkNone)
    }

    @Test
    fun intentsOpenTheRightScreens() {
        val open = openTaskIntent(context, "t1")
        assertEquals("t1", open.getStringExtra(MainActivity.EXTRA_TASK_ID))

        val addToday = quickAddIntent(context, WidgetFilter.All)
        assertEquals(true, addToday.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, false))
        assertNull(addToday.getStringExtra(MainActivity.EXTRA_QUICK_ADD_PROJECT))

        val addToList = quickAddIntent(context, WidgetFilter.ProjectList("work"))
        assertEquals("work", addToList.getStringExtra(MainActivity.EXTRA_QUICK_ADD_PROJECT))
        assertEquals(false, addToList.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, true))
        assertEquals("Разные адреса — разные нажатия", false, openTaskIntent(context, "a").filterEquals(openTaskIntent(context, "b")))
    }

    private val options = listOf(
        ProjectOption("inbox", "Inbox", null, isInbox = true),
        ProjectOption("work", "Работа", "#3D8BFD", isInbox = false),
        ProjectOption("home", "Дом", "#46A758", isInbox = false),
    )

    @Test
    fun pickerShowsChoicesAndReturnsSelection() {
        val picked = mutableListOf<WidgetFilter>()
        composeRule.setContent { AppTheme { WidgetPicker(options, WidgetFilter.All, onPick = { picked += it }, onDismiss = {}) } }

        composeRule.onNodeWithText("Просроченные и ближайшие 30 дней").assertIsDisplayed()
        composeRule.onNodeWithText("Inbox").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_PICK_INBOX).performClick()
        composeRule.onNodeWithTag("$TAG_PICK_LIST:home").performClick()
        assertEquals(listOf(WidgetFilter.Inbox, WidgetFilter.ProjectList("home")), picked)
    }

    @Test
    fun screenshotPicker() {
        composeRule.setContent { AppTheme { WidgetPicker(options, WidgetFilter.ProjectList("work"), onPick = {}, onDismiss = {}) } }
        composeRule.onRoot().captureRoboImage("screenshots/widget_picker_light.png")
    }

    /** Галочка в виджете: задача из настоящей базы приложения выполняется, повторяющаяся — переезжает. */
    @Test
    fun checkboxCompletesTask() = kotlinx.coroutines.runBlocking {
        val entry = context.widgetEntryPoint()
        val inbox = entry.projects().getInbox().id
        val today = entry.time().today()
        val plain = entry.tasks().createTask(com.prostovasei.simpletasks.domain.model.NewTask(inbox, "Купить хлеб", due = com.prostovasei.simpletasks.domain.model.TaskDue.AllDay(today)))
        val daily = entry.tasks().createTask(
            com.prostovasei.simpletasks.domain.model.NewTask(inbox, "Зарядка", due = com.prostovasei.simpletasks.domain.model.TaskDue.AllDay(today), rrule = "FREQ=DAILY"),
        )
        val glanceId = object : androidx.glance.GlanceId {}

        CompleteTaskAction().onAction(context, glanceId, androidx.glance.action.actionParametersOf(CompleteTaskAction.TASK_ID to plain))
        CompleteTaskAction().onAction(context, glanceId, androidx.glance.action.actionParametersOf(CompleteTaskAction.TASK_ID to daily))

        assertEquals(com.prostovasei.simpletasks.domain.model.TaskStatus.COMPLETED, entry.tasks().getTask(plain)!!.status)
        assertEquals(com.prostovasei.simpletasks.domain.model.TaskDue.AllDay(today.plusDays(1)), entry.tasks().getTask(daily)!!.due)
    }
}
