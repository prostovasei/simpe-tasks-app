package com.prostovasei.simpletasks.ui.feature.sorted

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.common.TaskListIntent
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

/** «Когда-нибудь» и «Без списка» (итерация 52): группы по спискам и дням, число, нажатия, «+», снимок. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class SortedScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 7)
    private val zone = ZoneOffset.UTC
    private val tree = ProjectTree(
        inbox = ProjectWithCount(project("inbox", "Входящие", isInbox = true), 0),
        groups = listOf(
            GroupWithProjects(
                ProjectGroup("g", "Жизнь", 0, Instant.EPOCH, Instant.EPOCH),
                listOf(ProjectWithCount(project("home", "Дом", groupId = "g", emoji = "🏠"), 0), ProjectWithCount(project("sport", "Спорт", groupId = "g", emoji = "💪"), 0)),
            ),
        ),
        ungrouped = listOf(ProjectWithCount(project("work", "Работа", emoji = "💼"), 0)),
    )
    private val intents = mutableListOf<TaskListIntent>()
    private val opened = mutableListOf<String>()
    private var added = 0

    private fun someday() = SortedState(
        groups = somedayGroups(
            listOf(
                task("w1", title = "Обновить портфолио", projectId = "work"),
                task("h1", title = "Повесить полку", projectId = "home"),
                task("s1", title = "Купить кроссовки", projectId = "sport"),
                task("h2", title = "Поменять фильтр воды", projectId = "home"),
            ),
            tree, today, zone, emptySet(),
        ),
        today = today,
        isLoading = false,
    )

    private fun show(kind: SortedKind, state: SortedState) {
        composeRule.setContent {
            AppTheme { SortedScreen(kind, state, { intents += it }, onBack = {}, onOpenTask = { opened += it }, onAddTask = { added++ }) }
        }
    }

    @Test
    fun somedayGroupsFollowListsOrder() {
        val state = someday()
        assertEquals("Как на «Списках»: папки, потом без папки", listOf("home", "sport", "work"), state.groups.map { it.list!!.id })
        assertEquals(listOf("h1", "h2"), state.groups.first().tasks.map { it.id })
        show(SortedKind.Someday, state)
        composeRule.onNodeWithTag(TAG_SORTED_COUNT).assertTextEquals("4")
        composeRule.onNodeWithTag("$TAG_SORTED_GROUP:home").assertExists()
        composeRule.onNodeWithTag("task:w1").performClick()
        composeRule.onNodeWithTag("task:s1:check").performClick()
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        assertEquals(listOf("w1"), opened)
        assertEquals(TaskListIntent.ToggleComplete("s1", recurring = false, done = false), intents.last())
        assertEquals(1, added)
    }

    @Test
    fun noListGroupsByDay() {
        val state = SortedState(
            groups = noListGroups(
                listOf(
                    task("later", title = "Забрать посылку", due = TaskDue.AllDay(today.plusDays(6))),
                    task("now", title = "Купить подарок", due = TaskDue.AllDay(today)),
                ),
                tree, today, zone, emptySet(),
            ),
            today = today,
            isLoading = false,
        )
        assertEquals(listOf(today, today.plusDays(6)), state.groups.map { it.date })
        show(SortedKind.NoList, state)
        composeRule.onNodeWithText("Сегодня", substring = true).assertExists()
        composeRule.onNodeWithTag("task:later").assertExists()
    }

    @Test
    fun emptySomedayShowsHint() {
        show(SortedKind.Someday, SortedState(today = today, isLoading = false))
        composeRule.onNodeWithText("Задач со списком, но без даты нет").assertExists()
    }

    @Test
    fun screenshotSomeday() {
        show(SortedKind.Someday, someday())
        composeRule.onRoot().captureRoboImage("screenshots/someday_light.png")
    }
}
