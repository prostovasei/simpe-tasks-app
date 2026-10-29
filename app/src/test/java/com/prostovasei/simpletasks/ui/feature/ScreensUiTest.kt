package com.prostovasei.simpletasks.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import com.prostovasei.simpletasks.ui.feature.search.TAG_SEARCH_RECENT_CLEAR
import com.prostovasei.simpletasks.ui.common.showUndo
import com.prostovasei.simpletasks.designsystem.components.TAG_UNDO_ACTION
import com.prostovasei.simpletasks.designsystem.components.TAG_UNDO_SNACKBAR
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextInput
import com.prostovasei.simpletasks.designsystem.components.TAG_EMPTY_TASKS
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_HEADER
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_MOVE
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_TOGGLE
import com.prostovasei.simpletasks.designsystem.components.TaskRow
import com.prostovasei.simpletasks.ui.feature.search.SearchIntent
import com.prostovasei.simpletasks.ui.feature.search.SearchResult
import com.prostovasei.simpletasks.ui.feature.search.SearchScreen
import com.prostovasei.simpletasks.ui.feature.search.SearchState
import com.prostovasei.simpletasks.ui.feature.search.TAG_SEARCH_CLEAR
import com.prostovasei.simpletasks.ui.feature.search.TAG_SEARCH_FIELD
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
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.all.AllScreen
import com.prostovasei.simpletasks.ui.feature.all.AllState
import com.prostovasei.simpletasks.ui.feature.completed.CompletedDay
import com.prostovasei.simpletasks.ui.feature.completed.CompletedIntent
import com.prostovasei.simpletasks.ui.feature.completed.CompletedScreen
import com.prostovasei.simpletasks.ui.feature.completed.CompletedState
import com.prostovasei.simpletasks.ui.feature.completed.CompletedTask
import com.prostovasei.simpletasks.ui.feature.inbox.InboxScreen
import com.prostovasei.simpletasks.ui.feature.inbox.InboxState
import com.prostovasei.simpletasks.ui.feature.lists.ListsDialog
import com.prostovasei.simpletasks.ui.feature.lists.ListsIntent
import com.prostovasei.simpletasks.ui.feature.lists.ListsScreen
import com.prostovasei.simpletasks.ui.feature.lists.ListsState
import com.prostovasei.simpletasks.ui.feature.lists.TAG_COMPLETED
import com.prostovasei.simpletasks.ui.feature.lists.TAG_NEW_LIST
import com.prostovasei.simpletasks.ui.feature.project.ProjectDialog
import com.prostovasei.simpletasks.ui.feature.project.ProjectIntent
import com.prostovasei.simpletasks.ui.feature.project.ProjectScreen
import com.prostovasei.simpletasks.ui.feature.project.ProjectState
import com.prostovasei.simpletasks.ui.feature.project.TAG_PROJECT_MENU
import com.prostovasei.simpletasks.ui.navigation.BottomBar
import com.prostovasei.simpletasks.ui.navigation.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    ) = TaskItem(
        id, title, priority, due, list, color, isInboxList = list == null, isCompleting = completing, isRecurring = recurring, ghostOf = ghostOf,
        listIcon = ICONS[list ?: "Inbox"],
    )

    // Значки списков в тестовых данных (итерация 14).
    private val ICONS = mapOf("Inbox" to "inbox", "Работа" to "work", "Дом" to "home", "Здоровье" to "health")

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
        today = LocalDate.of(2026, 10, 16),
    )

    @Test
    fun allScreenShowsDaysAndTimes() {
        show { AllScreen(allState, { intents += it }) }

        composeRule.onNodeWithText("ЧТ, 15 окт").assertExists()
        composeRule.onNodeWithText("Вчера").assertExists()
        composeRule.onNodeWithText("ПТ, 16 окт").assertExists()
        composeRule.onNodeWithText("Сегодня").assertExists()
        composeRule.onNodeWithText("СБ, 17 окт").assertExists()
        composeRule.onNodeWithText("Завтра").assertExists()
        composeRule.onNodeWithText("ВТ, 20 окт").assertExists()
        composeRule.onAllNodesWithText("10:00").assertCountEquals(2)
        composeRule.onAllNodesWithText("11:00").assertCountEquals(2)
        composeRule.onNodeWithText("09:30").assertExists()
        composeRule.onNodeWithTag("task:t1:repeat", useUnmergedTree = true).assertExists()
    }

    /** Итерация 37: «Просрочено · N» над прошедшими днями; стрелка сворачивает, значок переносит на сегодня. */
    @Test
    fun overdueHeaderTogglesAndMovesToToday() {
        show { AllScreen(allState, { intents += it }) }

        composeRule.onNodeWithTag(TAG_OVERDUE_HEADER).assertExists()
        composeRule.onNodeWithText("Просрочено · 1").assertExists()
        composeRule.onNodeWithTag(TAG_OVERDUE_TOGGLE).performClick()
        composeRule.onNodeWithTag(TAG_OVERDUE_MOVE).performClick()
        assertEquals(listOf<Any>(TaskListIntent.ToggleOverdue, TaskListIntent.MoveOverdue), intents.toList())
    }

    @Test
    fun collapsedOverdueHidesPastDaysButKeepsHeader() {
        show { AllScreen(allState.copy(overdueCollapsed = true), { intents += it }) }

        composeRule.onNodeWithText("Просрочено · 1").assertExists()
        composeRule.onNodeWithText("ЧТ, 15 окт").assertDoesNotExist()
        composeRule.onNodeWithText("ПТ, 16 окт").assertExists()
    }

    @Test
    fun noOverdueNoHeader() {
        show { AllScreen(allState.copy(overdue = emptyList()), { intents += it }) }
        composeRule.onNodeWithTag(TAG_OVERDUE_HEADER).assertDoesNotExist()
    }

    @Test
    fun overdueDayHiddenWhenEmpty() {
        show { AllScreen(allState.copy(overdue = emptyList()), { intents += it }) }
        composeRule.onNodeWithText("ЧТ, 15 окт").assertDoesNotExist()
    }

    @Test
    fun allScreenEmptyState() {
        show { AllScreen(AllState(isLoading = false), { intents += it }) }
        composeRule.onNodeWithText("Всё сделано").assertExists()
        composeRule.onNodeWithText("Нажмите «+» внизу, чтобы добавить задачу").assertExists()
        composeRule.onNodeWithTag(TAG_EMPTY_TASKS).captureRoboImage("screenshots/empty_all_light.png")
    }

    private val searchState = SearchState(
        query = "вра",
        active = listOf(
            SearchResult(item("s1", "Записаться к врачу", DueText(DayLabel.Tomorrow, "09:30"), list = "Здоровье", color = "#E5484D"), isDone = false, match = null),
            SearchResult(item("s2", "Вернуть книгу", list = "Дом", color = "#46A758"), isDone = false, match = "до 20-го, спросить врача"),
        ),
        done = listOf(SearchResult(item("s3", "Позвонить врачу", list = null, color = null).copy(isCompleting = true), isDone = true, match = null)),
        isLoading = false,
    )

    @Test
    fun searchShowsResultsAndTogglesBothWays() {
        var opened: String? = null
        show { SearchScreen(searchState, { intents += it }, { opened = it }, autoFocus = false) }

        composeRule.onNodeWithText("НАЙДЕНО 3 ЗАДАЧИ").assertExists()
        composeRule.onNodeWithText("ВЫПОЛНЕННЫЕ").assertExists()
        composeRule.onNodeWithTag("search:s2:subtitle", useUnmergedTree = true).assertTextEquals("до 20-го, спросить врача")
        composeRule.onNodeWithTag("search:s1:check").performClick()
        composeRule.onNodeWithTag("search:s3:check").performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_CLEAR).performClick()
        composeRule.onNodeWithText("Позвонить врачу").performClick()

        assertEquals(
            listOf<Any>(SearchIntent.Toggle("s1", isDone = false), SearchIntent.Toggle("s3", isDone = true), SearchIntent.Query(""), SearchIntent.Submit),
            intents,
        )
        assertEquals("s3", opened)
    }

    @Test
    fun searchStartsWithHintAndShowsNothingFound() {
        show { SearchScreen(SearchState(isLoading = false), { intents += it }, autoFocus = false) }
        composeRule.onNodeWithText("Начните вводить — задачи появятся сразу").assertExists()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextInput("молоко")
        assertEquals(listOf<Any>(SearchIntent.Query("молоко")), intents)
    }

    /** Итерация 38: пока поле пустое — «Недавние»: нажатие ищет, крестик убирает один запрос, «Очистить» — все. */
    @Test
    fun searchRecentsPickRemoveAndClear() {
        show { SearchScreen(SearchState(isLoading = false, recents = listOf("отчёт", "бюджет")), { intents += it }, autoFocus = false) }

        composeRule.onNodeWithText("НЕДАВНИЕ").assertExists()
        composeRule.onNodeWithText("Начните вводить — задачи появятся сразу").assertDoesNotExist()
        composeRule.onNodeWithTag("search_recent:отчёт").performClick()
        composeRule.onNodeWithTag("search_recent_remove:бюджет").performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_RECENT_CLEAR).performClick()
        assertEquals(listOf<Any>(SearchIntent.PickRecent("отчёт"), SearchIntent.RemoveRecent("бюджет"), SearchIntent.ClearRecents), intents)
    }

    @Test
    fun screenshotSearchRecents() {
        show { SearchScreen(SearchState(isLoading = false, recents = listOf("отчёт", "купить продукты", "бюджет", "врач", "зарядка")), {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/search_recent_light.png")
    }

    /** Итерация 38: подсказка «Отмена» — белая полупрозрачная карточка со значком. */
    @Test
    fun screenshotUndoSnackbars() {
        val kinds = listOf(UndoKind.Done to "Задача выполнена", UndoKind.Repeat to "Следующий повтор: сб, 17 окт", UndoKind.Deleted to "Задача удалена", UndoKind.Moved to "Перенесено на сегодня: 3")
        show {
            androidx.compose.foundation.layout.Column {
                kinds.forEach { (kind, text) ->
                    val host = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
                    androidx.compose.runtime.LaunchedEffect(Unit) { host.showUndo(text, "Отмена", kind) {} }
                    UndoSnackbarHost(host)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(TAG_UNDO_SNACKBAR).assertCountEquals(4)
        composeRule.onAllNodesWithTag(TAG_UNDO_ACTION).assertCountEquals(4)
        composeRule.onRoot().captureRoboImage("screenshots/undo_snackbar_light.png")
    }

    @Test
    fun screenshotDustMidway() {
        composeRule.mainClock.autoAdvance = false
        show {
            androidx.compose.foundation.layout.Column {
                TaskRow(title = "Записаться к врачу", priority = Priority.HIGH, listColor = null, isCompleting = true, onToggle = {}, trailing = listOf("18:30"))
                TaskRow(title = "Купить продукты", priority = Priority.NONE, listColor = null, isCompleting = false, onToggle = {})
            }
        }
        composeRule.mainClock.advanceTimeBy(550)
        composeRule.onRoot().captureRoboImage("screenshots/dust_midway.png")
    }

    @Test
    fun screenshotSearch() {
        show { SearchScreen(searchState, {}, autoFocus = false) }
        composeRule.onRoot().captureRoboImage("screenshots/search_light.png")
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
                listOf(ProjectWithCount(project("home", "Дом", groupId = "g1", color = "#46A758", icon = "home"), 4), ProjectWithCount(project("health", "Здоровье", groupId = "g1", color = "#E5484D", icon = "health"), 1)),
            ),
        ),
        ungrouped = listOf(ProjectWithCount(project("work", "Работа", color = "#3D8BFD", icon = "work"), 12), ProjectWithCount(project("ideas", "Идеи"), 0)),
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
    fun listsScreenOpensCompleted() {
        var opened = false
        show { ListsScreen(ListsState(tree = tree), { intents += it }, {}, { opened = true }) }
        composeRule.onNodeWithTag(TAG_COMPLETED).performClick()
        assertTrue(opened)
    }

    private val completedState = CompletedState(
        days = listOf(
            CompletedDay(DayLabel.Today, listOf(CompletedTask("c1", "Отправить отчёт", "Работа", "#3D8BFD", false, "09:05"))),
            CompletedDay(DayLabel.Date(LocalDate.of(2026, 10, 1), showYear = false), listOf(CompletedTask("c2", "Купить продукты", null, null, true, null))),
        ),
        isLoading = false,
    )

    @Test
    fun completedScreenShowsDaysAndRestores() {
        var opened: String? = null
        show { CompletedScreen(completedState, { intents += it }, {}, { opened = it }) }

        composeRule.onNodeWithText("Сегодня").assertExists()
        composeRule.onNodeWithText("1 октября").assertExists()
        composeRule.onNodeWithText("09:05").assertExists()
        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.onNodeWithTag("completed:c1:check", useUnmergedTree = true).performClick()

        assertEquals("c2", opened)
        assertEquals(listOf<Any>(CompletedIntent.Restore("c1")), intents)
        composeRule.onRoot().captureRoboImage("screenshots/completed_light.png")
    }

    @Test
    fun completedScreenEmpty() {
        show { CompletedScreen(CompletedState(isLoading = false), {}, {}, {}) }
        composeRule.onNodeWithText("Выполненных задач пока нет").assertExists()
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
        icon = "work",
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
    fun iconDialogSelectsIcon() {
        show { ProjectScreen(projectState.copy(dialog = ProjectDialog.Icon), { intents += it }, {}) }

        composeRule.onNodeWithText("Значок списка").assertExists()
        composeRule.onNodeWithTag("icon:home").performClick()
        composeRule.onNodeWithText("Без значка").performClick()

        assertEquals(listOf<Any>(ProjectIntent.SetIcon("home"), ProjectIntent.SetIcon(null)), intents)
    }

    @Test
    fun menuHasIconItem() {
        show { ProjectScreen(projectState, { intents += it }, {}) }

        composeRule.onNodeWithTag(TAG_PROJECT_MENU).performClick()
        composeRule.onNodeWithText("Значок").performClick()

        assertEquals(listOf<Any>(ProjectIntent.OpenDialog(ProjectDialog.Icon)), intents)
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
        var added = 0
        show { BottomBar(selected = Tab.ALL, onSelect = { selected += it }, onAdd = { added++ }) }

        composeRule.onNodeWithContentDescription("Входящие").assertExists()
        composeRule.onNodeWithContentDescription("Списки").performClick()
        composeRule.onNodeWithTag("tab:SETTINGS").performClick()
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()

        assertEquals(listOf(Tab.LISTS, Tab.SETTINGS), selected)
        assertEquals(1, added)
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
        show { BottomBar(selected = Tab.SEARCH, onSelect = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/bottom_bar_light.png")
    }
}
