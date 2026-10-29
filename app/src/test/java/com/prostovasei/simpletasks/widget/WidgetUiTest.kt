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

    private fun item(id: String, title: String, due: DueText?, list: String? = "Работа", ghostOf: String? = null, priority: Priority = Priority.NONE, done: Boolean = false) =
        TaskItem(id, title, priority, due, list, null, isInboxList = list == null, isRecurring = ghostOf != null, ghostOf = ghostOf, isDone = done)

    private val today = LocalDate.of(2026, 10, 16)

    private val model = WidgetModel(
        WidgetFilter.All,
        null,
        today,
        listOf(
            WidgetDayBlock(
                WidgetDay.On(today.minusDays(1)),
                listOf(
                    item("late", "Отправить отчёт", DueText(DayLabel.Yesterday, "18:00"), priority = Priority.HIGH),
                    item("paid", "Оплатить свет", DueText(DayLabel.Yesterday, null), done = true),
                ),
            ),
            WidgetDayBlock(WidgetDay.On(today), listOf(item("gym", "Зарядка", DueText(DayLabel.Today, "08:00"), list = null))),
            WidgetDayBlock(WidgetDay.On(today.plusDays(1)), listOf(item("net", "Оплатить интернет", DueText(DayLabel.Tomorrow, null)))),
            WidgetDayBlock(WidgetDay.On(today.plusDays(3)), listOf(item("gym@0", "Зарядка", DueText(DayLabel.Date(today.plusDays(3), false), "08:00"), list = null, ghostOf = "gym"))),
        ),
        tabs = listOf(WidgetTab(WidgetFilter.All), WidgetTab(WidgetFilter.Inbox), WidgetTab(WidgetFilter.ProjectList("work"), "Работа", "#3D8BFD")),
    )

    @Test
    fun widgetShowsDayBlocksAndBottomBar() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 400.dp))
        setContext(context)
        provideComposable { WidgetContent(model, appWidgetId = 7) }

        // Нижняя строка (итерация 25): «Все», «Входящие», открыть приложение и «+»; даты и «⋯» нет.
        // Итерация 32: слева от «+» — кнопка обновления.
        listOf("Все", "Входящие", "Открыть приложение", "Обновление приложения", "Новая задача").forEach { onNode(hasContentDescription(it)).assertExists() }
        onNode(hasTextEqualTo("пт, 16 окт")).assertDoesNotExist()
        onAllNodes(hasText("2")).assertCountEquals(0)
        // Подписи дней как в приложении: просроченные — по дням (итерация 27), «ЧТ, 15 окт» + «Вчера»,
        // «ПТ, 16 окт» + метка «Сегодня», «СБ, 17 окт» + «Завтра», «ПН, 19 окт».
        // Итерация 37: над прошедшими днями — «Просрочено · 1» (выполненная не считается), со стрелкой и значком переноса.
        onNode(hasTextEqualTo("Просрочено · 1")).assertExists()
        onNode(hasContentDescription("Свернуть просроченные")).assertExists()
        onNode(hasContentDescription("Перенести просроченные на сегодня")).assertExists()
        onNode(hasTextEqualTo("ЧТ, 15 окт")).assertExists()
        onNode(hasTextEqualTo("ПТ, 16 окт")).assertExists()
        onNode(hasTextEqualTo("Сегодня")).assertExists()
        onNode(hasTextEqualTo("СБ, 17 окт")).assertExists()
        onNode(hasTextEqualTo("Завтра")).assertExists()
        onNode(hasTextEqualTo("ПН, 19 окт")).assertExists()
        // Слева время (итерация 20), у просроченной тоже (итерация 27); название без «↻» (повтор — значком).
        onNode(hasTextEqualTo("Вчера")).assertExists()
        onNode(hasTextEqualTo("18:00")).assertExists()
        onAllNodes(hasTextEqualTo("08:00")).assertCountEquals(2)
        onAllNodes(hasTextEqualTo("Зарядка")).assertCountEquals(2)
        // Кружки-галочки на шкале только у настоящих задач: у будущего повтора нажатия нет; у выполненной — «снять отметку».
        onAllNodes(hasContentDescription("Выполнить")).assertCountEquals(3)
        onAllNodes(hasContentDescription("Снять отметку «выполнено»")).assertCountEquals(1)
        onNode(hasTextEqualTo("Оплатить свет")).assertExists()
    }

    @Test
    fun collapsedOverdueHidesPastDays() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 400.dp))
        setContext(context)
        provideComposable { WidgetContent(model.copy(overdueCollapsed = true), appWidgetId = 7) }

        onNode(hasTextEqualTo("Просрочено · 1")).assertExists()
        onNode(hasContentDescription("Развернуть просроченные")).assertExists()
        onNode(hasTextEqualTo("ЧТ, 15 окт")).assertDoesNotExist()
        onNode(hasTextEqualTo("Отправить отчёт")).assertDoesNotExist()
        onNode(hasTextEqualTo("ПТ, 16 окт")).assertExists()
    }

    @Test
    fun emptyListWidget() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 200.dp))
        setContext(context)
        val tabs = listOf(WidgetTab(WidgetFilter.All), WidgetTab(WidgetFilter.Inbox), WidgetTab(WidgetFilter.ProjectList("p"), "Покупки", null))
        provideComposable { WidgetContent(WidgetModel(WidgetFilter.ProjectList("p"), "Покупки", today, emptyList(), tabs), appWidgetId = 7) }
        onNode(hasText("Задач нет")).assertExists()
        // Итерация 25: даты внизу больше нет.
        onNode(hasTextEqualTo("пт, 16 окт")).assertDoesNotExist()
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
        assertEquals(android.content.Intent.ACTION_MAIN, openAppIntent(context).action)
        assertEquals(MainActivity::class.java.name, openAppIntent(context).component?.className)
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
        WidgetActionReceiver.handle(context, WidgetActions.complete(context, plain))
        WidgetActionReceiver.handle(context, WidgetActions.complete(context, daily))

        assertEquals(com.prostovasei.simpletasks.domain.model.TaskStatus.COMPLETED, entry.tasks().getTask(plain)!!.status)
        assertEquals(com.prostovasei.simpletasks.domain.model.TaskDue.AllDay(today.plusDays(1)), entry.tasks().getTask(daily)!!.due)
    }
}
