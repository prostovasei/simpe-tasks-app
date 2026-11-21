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
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.WeekFilter
import com.prostovasei.simpletasks.domain.model.WeekFilterMode
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_DONE
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_GROUP
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_HIDE
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_LIST
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_NONE
import com.prostovasei.simpletasks.ui.feature.week.TAG_WEEK_FILTER_RESET
import com.prostovasei.simpletasks.ui.feature.week.WeekFilterPanel
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
        TaskItem(id, title, priority, due, list, if (list != null) "📁" else if (due != null) "📌" else "📥", isInboxList = list == null, isRecurring = ghostOf != null, ghostOf = ghostOf, isDone = done)

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
    )

    @Test
    fun widgetShowsDayBlocksAndBottomBar() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 400.dp))
        setContext(context)
        provideComposable { WidgetContent(model, appWidgetId = 7, scrollable = false) }

        // Нижняя строка (итерация 25): «Все», «Входящие», открыть приложение и «+»; даты и «⋯» нет.
        // Итерация 43: слева от «+» — капсула с галочкой значка приложения (открыть приложение), вместо кнопки обновления.
        listOf("Задачи · Все, выбрано", "Финансы", "Новая задача").forEach { onNode(hasContentDescription(it)).assertExists() }
        // Итерация 57: галочки нет — «открыть приложение» две свободные полосы.
        onAllNodes(hasContentDescription("Открыть приложение")).assertCountEquals(2)
        onAllNodes(hasContentDescription("Обновление приложения")).assertCountEquals(0)
        onNode(hasTextEqualTo("пт, 16 окт")).assertDoesNotExist()
        // Итерация 43: у каждого дня справа число задач (у «Сегодня» вместо него прогресс «0 из 1»).
        onNode(hasTextEqualTo("2")).assertExists()
        onAllNodes(hasTextEqualTo("1")).assertCountEquals(2)
        onNode(hasTextEqualTo("0 из 1")).assertExists()
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
        provideComposable { WidgetContent(model.copy(overdueCollapsed = true), appWidgetId = 7, scrollable = false) }

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
        provideComposable { WidgetContent(WidgetModel(WidgetFilter.Inbox, null, today, emptyList()), appWidgetId = 7) }
        onNode(hasText("Задач нет")).assertExists()
        // Итерация 25: даты внизу больше нет.
        onNode(hasTextEqualTo("пт, 16 окт")).assertDoesNotExist()
    }

    @Test
    fun priorityPicksCheckColor() {
        assertEquals(widgetCheckColors(Priority.HIGH), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkHigh)
        assertEquals(widgetCheckColors(Priority.NONE), com.prostovasei.simpletasks.widget.theme.WidgetColors.checkNone)
    }

    @Test
    fun intentsOpenTheRightScreens() {
        val open = openTaskIntent(context, "t1")
        assertEquals("t1", open.getStringExtra(MainActivity.EXTRA_TASK_ID))

        val addToday = quickAddIntent(context, WidgetFilter.All)
        assertEquals(true, addToday.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, false))
        assertNull(addToday.getStringExtra(MainActivity.EXTRA_QUICK_ADD_PROJECT))

        val addToInbox = quickAddIntent(context, WidgetFilter.Inbox)
        assertEquals(false, addToInbox.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, true))
        assertEquals("Разные адреса — разные нажатия", false, openTaskIntent(context, "a").filterEquals(openTaskIntent(context, "b")))
        assertEquals(android.content.Intent.ACTION_MAIN, openAppIntent(context).action)
        assertEquals(MainActivity::class.java.name, openAppIntent(context).component?.className)
    }

    private fun at() = java.time.Instant.parse("2026-10-16T09:00:00Z")

    private fun project(id: String, name: String, groupId: String? = null, inbox: Boolean = false, emoji: String? = null) =
        ProjectWithCount(Project(id, groupId, name, null, 0, inbox, at(), at(), null, emoji), 0)

    private val tree = ProjectTree(
        inbox = project("inbox", "Входящие", inbox = true),
        groups = listOf(
            GroupWithProjects(ProjectGroup("g-work", "Работа", 0, at(), at(), "💼"), listOf(project("meet", "Встречи", "g-work", emoji = "📅"), project("proj", "Проекты", "g-work", emoji = "🗂"))),
        ),
        ungrouped = listOf(project("shop", "Покупки", emoji = "🛒")),
    )

    @Test
    fun filterPanelReturnsMarkedListsAndMode() {
        var applied: WeekFilter? = null
        composeRule.setContent { AppTheme { WeekFilterPanel(tree, WeekFilter(), onApply = { applied = it }) } }

        composeRule.onNodeWithTag("$TAG_WEEK_FILTER_GROUP:g-work").performClick()
        composeRule.onNodeWithTag("$TAG_WEEK_FILTER_LIST:proj").performClick()
        composeRule.onNodeWithTag(TAG_WEEK_FILTER_NONE).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_FILTER_HIDE).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_FILTER_DONE).performClick()
        // Папка отметила оба списка, затем «Проекты» сняли; «Без списка» добавлен.
        assertEquals(WeekFilter(WeekFilterMode.HIDE, setOf("meet", WeekFilter.NO_LIST)), applied)
    }

    @Test
    fun filterPanelResetClearsMarks() {
        var applied: WeekFilter? = null
        composeRule.setContent { AppTheme { WeekFilterPanel(tree, WeekFilter(WeekFilterMode.SHOW, setOf("shop")), onApply = { applied = it }) } }
        composeRule.onNodeWithTag(TAG_WEEK_FILTER_RESET).performClick()
        composeRule.onNodeWithTag(TAG_WEEK_FILTER_DONE).performClick()
        assertEquals(WeekFilter(), applied)
    }

    @Test
    fun screenshotFilter() {
        composeRule.setContent { AppTheme { WeekFilterPanel(tree, WeekFilter(WeekFilterMode.SHOW, setOf("meet", "proj", "shop")), onApply = {}) } }
        composeRule.onRoot().captureRoboImage("screenshots/widget_filter_light.png")
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
