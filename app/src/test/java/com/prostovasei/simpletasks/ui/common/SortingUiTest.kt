package com.prostovasei.simpletasks.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.ui.feature.project.ProjectScreen
import com.prostovasei.simpletasks.ui.feature.project.ProjectState
import com.prostovasei.simpletasks.ui.navigation.FloatingNavBar
import com.prostovasei.simpletasks.ui.navigation.TAG_NAV_SORT
import com.prostovasei.simpletasks.ui.navigation.TAG_NAV_SORT_DOT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** #357: экран списка с сортировкой, шторка «Сортировка», кнопка ↑↓ и перетаскивание. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class SortingUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<TaskListIntent>()
    private val today = LocalDate.of(2026, 11, 27)

    private fun item(id: String, title: String, day: DayLabel?, time: String? = null, priority: Priority = Priority.NONE, done: Boolean = false) =
        TaskItem(id, title, priority, day?.let { DueText(it, time) }, "Работа", "💼", false, isDone = done)

    private val tasks = listOf(
        item("w3", "Отправить отчёт", DayLabel.Yesterday),
        item("w7", "Купить билеты", DayLabel.Today),
        item("w4", "Созвон с командой", DayLabel.Today, "10:00"),
        item("w1", "Подготовить презентацию", DayLabel.Tomorrow, priority = Priority.HIGH),
        item("w5", "Запланировать отпуск", DayLabel.Date(today.plusDays(6), false), priority = Priority.HIGH),
        item("w6", "Обновить резюме", null, priority = Priority.HIGH),
        item("w2", "Разобрать почту", null),
        item("w9", "Сдать отчёт за октябрь", DayLabel.Yesterday, done = true),
        item("w10", "Оплатить сервис", null, done = true),
    )

    private fun state(sort: TaskSort = TaskSort.LIST_DEFAULT, ask: Boolean = false) =
        ProjectState(name = "Работа", emoji = "💼", tasks = tasks, isLoading = false, sort = sort, today = today, askManual = ask)

    private fun show(content: @Composable () -> Unit) = composeRule.setContent { AppTheme { content() } }

    @Test
    fun dueSortShowsDaysNoDateAndFoldedDone() {
        show { ProjectScreen(state(), { intents += it }, {}) }
        listOf("overdue", "day-$today", "day-${today.plusDays(1)}", "day-${today.plusDays(6)}", "nodate")
            .forEach { composeRule.onNodeWithTag("$TAG_GROUP_HEADER:$it", useUnmergedTree = true).assertIsDisplayed() }
        composeRule.onAllNodesWithTag("task:w9").assertCountEquals(0)
        composeRule.onNodeWithTag(TAG_DONE_HEADER).performClick()
        composeRule.onNodeWithTag("task:w9").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("screenshots/project_sorted_light.png")
    }

    @Test
    fun otherSortHasNoDayHeaders() {
        show { ProjectScreen(state(TaskSort(SortBy.PRIORITY)), { intents += it }, {}) }
        composeRule.onAllNodesWithTag("$TAG_GROUP_HEADER:nodate", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag("task:w3").assertIsDisplayed()
    }

    @Test
    fun longPressAndMoveAsksForManualSort() {
        show { ProjectScreen(state(TaskSort(SortBy.PRIORITY)), { intents += it }, {}) }
        composeRule.onNodeWithTag("task:w7").performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(0f, 120f))
            up()
        }
        assertEquals(listOf<TaskListIntent>(TaskListIntent.AskManualSort), intents)
    }

    @Test
    fun manualDialogSwitchesWithSkip() {
        show { ProjectScreen(state(ask = true), { intents += it }, {}) }
        composeRule.onNodeWithTag(TAG_MANUAL_SKIP).performClick()
        composeRule.onNodeWithTag(TAG_MANUAL_SWITCH).performClick()
        assertEquals(listOf<TaskListIntent>(TaskListIntent.SwitchToManual(skipQuestion = true)), intents)
    }

    @Test
    fun dragInManualModeReorders() {
        show { ProjectScreen(state(TaskSort(SortBy.MANUAL)), { intents += it }, {}) }
        composeRule.onNodeWithTag("task:w3").performTouchInput {
            down(center)
            advanceEventTime(800)
            repeat(10) { moveBy(Offset(0f, 12f)) }
            up()
        }
        val reorder = intents.filterIsInstance<TaskListIntent.Reorder>().single()
        assertEquals("w7", reorder.ids.first())
        assertTrue(reorder.ids.indexOf("w3") > 0)
    }

    @Test
    fun longPressWithoutMoveSelects() {
        show { ProjectScreen(state(TaskSort(SortBy.MANUAL)), { intents += it }, {}) }
        composeRule.onNodeWithTag("task:w7").performTouchInput {
            down(center)
            advanceEventTime(800)
            up()
        }
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_BAR).assertIsDisplayed()
        assertTrue(intents.none { it is TaskListIntent.Reorder || it == TaskListIntent.AskManualSort })
    }

    @Test
    fun sheetPicksAndFlips() {
        var sort by mutableStateOf(TaskSort.LIST_DEFAULT)
        show { SortSheet("Работа · у каждого списка своя", sort, TaskSort.LIST_DEFAULT, upcoming = false, onChange = { sort = it }, onDismiss = {}) }
        composeRule.onNodeWithTag("$TAG_SORT_OPTION:DUE").assertIsSelected()
        composeRule.onRoot().captureRoboImage("screenshots/sort_sheet_light.png")
        composeRule.onNodeWithTag("$TAG_SORT_OPTION:PRIORITY").performClick()
        assertEquals(TaskSort(SortBy.PRIORITY, true), sort)
        composeRule.onNodeWithTag("$TAG_SORT_OPTION:PRIORITY").performClick()
        assertEquals(TaskSort(SortBy.PRIORITY, false), sort)
        composeRule.onNodeWithTag(TAG_SORT_FLIP).performClick()
        assertEquals(TaskSort(SortBy.PRIORITY, true), sort)
        composeRule.onNodeWithTag(TAG_SORT_RESET).performClick()
        assertEquals(TaskSort.LIST_DEFAULT, sort)
    }

    @Test
    fun upcomingSheetHasNoManual() {
        show { SortSheet("Предстоящее", TaskSort.DAY_DEFAULT, TaskSort.DAY_DEFAULT, upcoming = true, onChange = {}, onDismiss = {}) }
        composeRule.onAllNodesWithTag("$TAG_SORT_OPTION:MANUAL").assertCountEquals(0)
        composeRule.onNodeWithTag("$TAG_SORT_OPTION:TIME").assertIsSelected()
    }

    @Test
    fun navBarShowsSortWithDotOnlyWhenChanged() {
        var changed by mutableStateOf(false)
        var opened = 0
        show { FloatingNavBar({}, {}, {}, {}, onSort = { opened++ }, sortChanged = changed) }
        composeRule.onAllNodesWithTag(TAG_NAV_SORT_DOT, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag(TAG_NAV_SORT).performClick()
        assertEquals(1, opened)
        changed = true
        composeRule.onNodeWithTag(TAG_NAV_SORT_DOT, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Отправить").assertDoesNotExistSafe()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistSafe() = assertDoesNotExist()
}
