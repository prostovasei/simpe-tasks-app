package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Шторка дня поверх рабочего стола (итерация 51): подпись, «выполнено из всех», лента, «Задача на …», закрытие, снимок. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class DaySheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<Any>()
    private val opened = mutableListOf<String>()
    private val added = mutableListOf<LocalDate>()
    private var closed = 0
    private val today = LocalDate.of(2026, 11, 7)

    private fun item(id: String, title: String, time: String?, done: Boolean = false, recurring: Boolean = false) =
        TaskItem(id, title, Priority.NONE, DueText(DayLabel.Today, time), "Дом", "🏠", isInboxList = false, isDone = done, isRecurring = recurring)

    private val state = DayState(
        date = today,
        today = today,
        items = listOf(
            item("tg", "Telegram", "10:30", done = true, recurring = true),
            item("hookah", "Кальян", "19:30–21:30"),
            item("football", "Футбол", "21:30–23:30"),
            item("flowers", "Цветы", "23:00", recurring = true),
        ),
        isLoading = false,
    )

    private fun show(value: DayState = state) {
        composeRule.setContent {
            AppTheme {
                DaySheet(value, { intents += it }, onClose = { closed++ }, onOpenTask = { opened += it }, onAdd = { added += it })
            }
        }
    }

    @Test
    fun showsDateProgressAndTasks() {
        show()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("СБ, 07 ноя")
        composeRule.onNodeWithTag(TAG_DAY_SHEET_COUNT).assertTextEquals("1 из 4")
        listOf("tg", "hookah", "football", "flowers").forEach { composeRule.onNodeWithTag("task:$it").assertExists() }
    }

    @Test
    fun circleTogglesPlaqueOpensAddAndScrimClose() {
        show()
        composeRule.onNodeWithTag("task:hookah:check").performClick()
        assertEquals(TaskListIntent.ToggleComplete("hookah", recurring = false, done = false), intents.last())
        composeRule.onNodeWithTag("task:football").performClick()
        assertEquals(listOf("football"), opened)
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).assertTextEquals("Задача на сб, 07 ноя").performClick()
        assertEquals(listOf(today), added)
        assertEquals("Нажатие по шторке не закрывает", 0, closed)
        composeRule.onNodeWithTag(TAG_DAY_SCRIM).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun emptyDayStillOffersNewTask() {
        show(state.copy(items = emptyList()))
        composeRule.onNodeWithTag(TAG_DAY_SHEET_COUNT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).assertExists()
    }

    @Test
    fun daySheetLight() {
        show()
        composeRule.onRoot().captureRoboImage("screenshots/day_sheet_light.png")
    }
}
