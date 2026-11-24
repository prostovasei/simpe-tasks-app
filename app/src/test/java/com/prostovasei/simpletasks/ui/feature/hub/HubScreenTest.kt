package com.prostovasei.simpletasks.ui.feature.hub

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.testing.account
import com.prostovasei.simpletasks.testing.operation
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** #329: хаб — полоса лимита со счетами, задачи «Просрочено · Сегодня · Завтра», новости 2 × 2 страницами. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class HubScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-11-24")
    private fun item(id: String, title: String, time: String?, label: DayLabel = DayLabel.Today, priority: Priority = Priority.NONE) =
        TaskItem(id, title, priority, DueText(label, time), "Дом", "🏠", isInboxList = false)

    private val tasks = HubState(
        today = today,
        overdue = listOf(item("o1", "Оплатить интернет", "12:00", DayLabel.Yesterday, Priority.HIGH)),
        todayItems = listOf(item("t1", "Созвон с командой", "10:00"), item("t2", "Купить продукты", null)),
        tomorrowItems = listOf(item("m1", "Стоматолог", "09:30", DayLabel.Tomorrow)),
        isLoading = false,
    )
    private val finance = FinanceState(
        accounts = listOf(account("a1", "Карта", 4_200_000), account("a2", "Наличные", 600_000)),
        operations = listOf(operation("e1", 5_400_000, "2026-11-20", category = "Продукты")),
        today = today,
    )
    private val news = (1..6).map { HubNews("https://example.com/$it", "09:0$it · Футбол", "Новость номер $it о матче", unread = it <= 2) }

    @Test
    fun showsFinanceTasksAndNewsPages() {
        val intents = mutableListOf<TaskListIntent>()
        val financeIntents = mutableListOf<FinanceIntent>()
        composeRule.setContent {
            AppTheme { HubScreen(tasks, { intents += it }, finance, { financeIntents += it }, news, onOpenTask = {}, onAddTask = {}) }
        }
        composeRule.onNodeWithText("Просрочено").assertExists()
        composeRule.onNodeWithText("Созвон с командой").assertExists()
        composeRule.onNodeWithText("Стоматолог").assertExists()
        composeRule.onAllNodesWithText("Новость номер 1 о матче").assertCountEquals(1)
        composeRule.onNodeWithTag(TAG_HUB_NEWS).assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/hub_light.png")
        composeRule.onNodeWithTag("task:t1:check", useUnmergedTree = true).let { runCatching { it.performClick() } }
    }

    @Test
    fun emptyTasksOffersToAdd() {
        var added = 0
        composeRule.setContent {
            AppTheme { HubScreen(HubState(today = today, isLoading = false), {}, finance, {}, emptyList(), onOpenTask = {}, onAddTask = { added++ }) }
        }
        composeRule.onNodeWithText("+ Задача").performClick()
        assertEquals(1, added)
        composeRule.onRoot().captureRoboImage("screenshots/hub_empty_light.png")
    }
}
