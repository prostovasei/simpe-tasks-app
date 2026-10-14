package com.prostovasei.simpletasks.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.all.AllScreen
import com.prostovasei.simpletasks.ui.feature.all.AllState
import com.prostovasei.simpletasks.ui.feature.inbox.InboxScreen
import com.prostovasei.simpletasks.ui.feature.inbox.InboxState
import com.prostovasei.simpletasks.ui.feature.lists.ListsDialog
import com.prostovasei.simpletasks.ui.feature.lists.ListsIntent
import com.prostovasei.simpletasks.ui.feature.lists.ListsScreen
import com.prostovasei.simpletasks.ui.feature.lists.ListsState
import com.prostovasei.simpletasks.ui.feature.lists.TAG_NEW_LIST
import com.prostovasei.simpletasks.ui.feature.project.ProjectDialog
import com.prostovasei.simpletasks.ui.feature.project.ProjectIntent
import com.prostovasei.simpletasks.ui.feature.project.ProjectScreen
import com.prostovasei.simpletasks.ui.feature.project.ProjectState
import com.prostovasei.simpletasks.ui.feature.project.TAG_PROJECT_MENU
import com.prostovasei.simpletasks.ui.navigation.BottomBar
import com.prostovasei.simpletasks.ui.navigation.Tab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

/** Экраны Итерации 04: отображение, нажатия, диалоги и скриншоты. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class ScreensUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<Any>()

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { AppTheme(darkTheme = dark) { content() } }
    }

    private fun item(
        id: String,
        title: String,
        due: DueText? = null,
        list: String? = "Работа",
        color: String? = "#3D8BFD",
        priority: Priority = Priority.NONE,
        completing: Boolean = false,
        recurring: Boolean = false,
        ghostOf: String? = null,
    ) = TaskItem(id, title, priority, due, list, color, isInboxList = list == null, isCompleting = completing, isRecurring = recurring, ghostOf = ghostOf)

    private val allState = AllState(
        overdue = listOf(item("o1", "Отправить отчёт", DueText(DayLabel.Yesterday, null), priority = Priority.HIGH)),
        todayAllDay = listOf(
            item("d1", "Купить продукты", DueText(DayLabel.Today, null), list = null, color = null, priority = Priority.LOW),
            item("d2", "Оплатить интернет", DueText(DayLabel.Today, null), list = "Дом", color = "#46A758", completing = true),
        ),
        todayTimed = listOf(item("t1", "Созвон с командой", DueText(DayLabel.Today, "10:00–11:00"), priority = Priority.MEDIUM, recurring = true)),
        upcoming = listOf(
            item("t1@0", "Созвон с командой", DueText(DayLabel.Tomorrow, "10:00–11:00"), recurring = true, ghostOf = "t1"),
            item("u1", "Записаться к врачу", DueText(DayLabel.Date(LocalDate.of(2026, 10, 20), false), "09:30"), list = "Здоровье", color = "#E5484D"),
        ),
        isLoading = false,
    )

    @Test
    fun allScreenShowsSectionsAndLabels() {
        show { AllScreen(allState, { intents += it }) }

        composeRule.onNodeWithText("ПРОСРОЧЕНО").assertExists()
        composeRule.onNodeWithText("СЕГОДНЯ").assertExists()
        composeRule.onNodeWithText("Весь день").assertExists()
        composeRule.onNodeWithText("По времени").assertExists()
        composeRule.onNodeWithText("ПРЕДСТОЯЩИЕ").assertExists()
        composeRule.onNodeWithText("Вчера · Работа").assertExists()
        composeRule.onNodeWithText("10:00–11:00 · Работа").assertExists()
        composeRule.onNodeWithText("Входящие").assertExists()
        composeRule.onNodeWithText("20 октября, 09:30 · Здоровье").assertExists()
        composeRule.onNodeWithText("P1").assertExists()
    }

    @Test
    fun overdueSectionHiddenWhenEmpty() {
        show { AllScreen(allState.copy(overdue = emptyList()), { intents += it }) }
        composeRule.onNodeWithText("ПРОСРОЧЕНО").assertDoesNotExist()
    }

    @Test
    fun allScreenEmptyState() {
        show { AllScreen(AllState(isLoading = false), { intents += it }) }
        composeRule.onNodeWithText("Задач со сроком нет").assertExists()
    }

    @Test
    fun tappingCheckboxCompletesTask() {
        show { AllScreen(allState, { intents += it }) }
        composeRule.onNodeWithTag("task:t1:check").performClick()
        assertEquals(listOf<Any>(TaskListIntent.ToggleComplete("t1", recurring = true)), intents)
    }

    @Test
    fun ghostRepeatHasNoCheckboxAndOpensRealTask() {
        var opened: String? = null
        show { AllScreen(allState, { intents += it }, onOpenTask = { opened = it }) }
        composeRule.onNodeWithTag("task:t1@0:check").assertDoesNotExist()
        composeRule.onNodeWithTag("task:t1@0").performClick()
        assertEquals("t1", opened)
    }

    @Test
    fun inboxShowsTasks() {
        show { InboxScreen(InboxState(tasks = listOf(item("i1", "Идея для отпуска", list = null, color = null)), isLoading = false), { intents += it }) }
        composeRule.onNodeWithText("Идея для отпуска").assertExists()
        composeRule.onNodeWithTag("task:i1:check").performClick()
        assertEquals(listOf<Any>(TaskListIntent.ToggleComplete("i1")), intents)
    }

    private val tree = ProjectTree(
        inbox = ProjectWithCount(project("inbox", "Inbox", isInbox = true), 3),
        groups = listOf(
            GroupWithProjects(
                ProjectGroup("g1", "Личное", 0, Instant.EPOCH, Instant.EPOCH),
                listOf(ProjectWithCount(project("home", "Дом", groupId = "g1", color = "#46A758"), 4), ProjectWithCount(project("health", "Здоровье", groupId = "g1", color = "#E5484D"), 1)),
            ),
        ),
        ungrouped = listOf(ProjectWithCount(project("work", "Работа", color = "#3D8BFD"), 12), ProjectWithCount(project("ideas", "Идеи"), 0)),
    )

    @Test
    fun listsScreenShowsTreeAndOpensProject() {
        var opened: String? = null
        show { ListsScreen(ListsState(tree = tree), { intents += it }, { opened = it }) }

        composeRule.onNodeWithText("Личное").assertExists()
        composeRule.onNodeWithText("Дом").assertExists()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithTag("project:work").performClick()
        composeRule.onNodeWithTag("folder:g1").performClick()
        composeRule.onNodeWithTag(TAG_NEW_LIST).performClick()

        assertEquals("work", opened)
        assertEquals(listOf<Any>(ListsIntent.ToggleFolder("g1"), ListsIntent.OpenDialog(ListsDialog.CreateList)), intents)
    }

    @Test
    fun collapsedFolderHidesLists() {
        show { ListsScreen(ListsState(tree = tree, collapsedFolders = setOf("g1")), { intents += it }, {}) }
        composeRule.onNodeWithText("Личное").assertExists()
        composeRule.onNodeWithText("Дом").assertDoesNotExist()
    }

    private val projectState = ProjectState(
        name = "Работа",
        color = "#3D8BFD",
        tasks = listOf(item("w1", "Подготовить презентацию", DueText(DayLabel.Tomorrow, null), priority = Priority.HIGH), item("w2", "Разобрать почту")),
        isLoading = false,
    )

    @Test
    fun projectMenuOpensDialogs() {
        show { ProjectScreen(projectState, { intents += it }, {}) }

        composeRule.onNodeWithText("Работа").assertExists()
        composeRule.onNodeWithTag(TAG_PROJECT_MENU).performClick()
        composeRule.onNodeWithText("Переименовать").performClick()

        assertEquals(listOf<Any>(ProjectIntent.OpenDialog(ProjectDialog.Rename)), intents)
    }

    @Test
    fun colorDialogSelectsColor() {
        show { ProjectScreen(projectState.copy(dialog = ProjectDialog.Color), { intents += it }, {}) }

        composeRule.onNodeWithTag("color:#E5484D").performClick()
        composeRule.onNodeWithText("Без цвета").performClick()

        assertEquals(listOf<Any>(ProjectIntent.SetColor("#E5484D"), ProjectIntent.SetColor(null)), intents)
    }

    @Test
    fun deleteDialogConfirms() {
        show { ProjectScreen(projectState.copy(dialog = ProjectDialog.Delete), { intents += it }, {}) }

        composeRule.onNodeWithText("Удалить список «Работа»?").assertExists()
        composeRule.onNodeWithText("Удалить").performClick()

        assertEquals(listOf<Any>(ProjectIntent.ConfirmDelete), intents)
    }

    @Test
    fun inboxListHasNoMenu() {
        show { ProjectScreen(projectState.copy(isInbox = true), { intents += it }, {}) }
        composeRule.onNodeWithText("Входящие").assertExists()
        composeRule.onNodeWithTag(TAG_PROJECT_MENU).assertDoesNotExist()
    }

    @Test
    fun bottomBarSwitchesTabs() {
        val selected = mutableListOf<Tab>()
        show { BottomBar(selected = Tab.ALL) { selected += it } }

        composeRule.onNodeWithText("Входящие").assertExists()
        composeRule.onNodeWithText("Списки").performClick()
        composeRule.onNodeWithTag("tab:SETTINGS").performClick()

        assertEquals(listOf(Tab.LISTS, Tab.SETTINGS), selected)
    }

    @Test
    fun screenshotAllLight() {
        show { AllScreen(allState, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/all_light.png")
    }

    @Test
    fun screenshotAllDark() {
        show(dark = true) { AllScreen(allState, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/all_dark.png")
    }

    @Test
    fun screenshotLists() {
        show { ListsScreen(ListsState(tree = tree), {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/lists_light.png")
    }

    @Test
    fun screenshotProject() {
        show { ProjectScreen(projectState, {}, {}) }
        composeRule.onRoot().captureRoboImage("screenshots/project_light.png")
    }

    @Test
    fun screenshotBottomBar() {
        show { BottomBar(selected = Tab.ALL) {} }
        composeRule.onRoot().captureRoboImage("screenshots/bottom_bar_light.png")
    }
}
