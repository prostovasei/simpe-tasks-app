package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

/** Полный список дня (итерация 44): подпись, число задач, лента, нажатия, «Назад», снимок. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class DayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<Any>()
    private val opened = mutableListOf<String>()
    private var back = 0
    private val today = LocalDate.of(2026, 10, 30)

    private fun item(id: String, title: String, time: String?, priority: Priority = Priority.NONE, done: Boolean = false) =
        TaskItem(id, title, priority, DueText(DayLabel.Today, time), "Работа", "#3D8BFD", isInboxList = false, isDone = done)

    private val state = DayState(
        date = today,
        today = today,
        items = listOf(
            item("a", "Кино", null),
            item("b", "Позвонить врачу", null),
            item("c", "Зарядка", "08:00", Priority.MEDIUM, done = true),
            item("d", "Отчёт за неделю", "17:00", Priority.HIGH),
        ),
        isLoading = false,
    )

    private fun show(dark: Boolean = false, value: DayState = state) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) { DayScreen(value, { intents += it }, onBack = { back++ }, onOpenTask = { opened += it }) }
        }
    }

    @Test
    fun showsTitleCountAndTasks() {
        show()
        composeRule.onNodeWithTag("day-title").assertTextEquals("ПТ, 30 окт")
        composeRule.onNodeWithTag("day-count").assertTextEquals("4")
        listOf("a", "b", "c", "d").forEach { composeRule.onNodeWithTag("task:$it").assertExists() }
    }

    @Test
    fun circleTogglesPlaqueOpensAndBackGoesBack() {
        show()
        composeRule.onNodeWithTag("task:d:check").performClick()
        assertEquals(TaskListIntent.ToggleComplete("d", recurring = false, done = false), intents.last())
        composeRule.onNodeWithTag("task:c:check").performClick()
        assertEquals(TaskListIntent.ToggleComplete("c", recurring = false, done = true), intents.last())
        composeRule.onNodeWithTag("task:b").performClick()
        assertEquals(listOf("b"), opened)
        composeRule.onNodeWithContentDescription("Назад").performClick()
        assertEquals(1, back)
    }

    @Test
    fun emptyDayShowsHint() {
        show(value = state.copy(items = emptyList()))
        composeRule.onNodeWithTag("day-count").assertTextEquals("0")
        composeRule.onNodeWithTag("task:a").assertDoesNotExist()
    }

    @Test
    fun dayScreenLight() {
        show()
        composeRule.onNodeWithTag("day-screen").captureRoboImage("screenshots/day_light.png")
    }
}
