package com.prostovasei.simpletasks.ui.feature

import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_FIELD
import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_OPTION
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_DELETE_FOREVER
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_RESTORE
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_COMPLETE
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_REOPEN
import com.prostovasei.simpletasks.designsystem.components.TAG_ACTION_RIBBON
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_CLOSE
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_ALL
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_COUNT
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_BAR
import com.prostovasei.simpletasks.ui.feature.lists.TAG_TRASH_COUNT
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_DELETE
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_RESTORE
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_MENU
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_BANNER
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_TITLE
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_BANNER
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_EMPTY
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_DELETE_ALL
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_RESTORE_ALL
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskIntent
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskState
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskScreen
import com.prostovasei.simpletasks.ui.feature.trash.TrashIntent
import com.prostovasei.simpletasks.ui.feature.trash.TrashState
import com.prostovasei.simpletasks.ui.feature.trash.TrashScreen
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
        id, title, priority, due, list, EMOJIS[list ?: "Inbox"] ?: "📁", isInboxList = list == null, isCompleting = completing, isRecurring = recurring, ghostOf = ghostOf,
    )

    // Эмодзи списков в тестовых данных (итерация 47; до этого — значки и цвета).
    private val EMOJIS = mapOf("Inbox" to "📥", "Работа" to "💼", "Дом" to "🏠", "Здоровье" to "❤️")

    private val allState = AllState(
        overdue = listOf(item("o1", "Отправить отчёт", DueText(DayLabel.Yesterday, null), priority = Priority.HIGH)),
        todayAllDay = listOf(
            item("d1", "Купить продукты", DueText(DayLabel.Today, null), list = null, color = null, priority = Priority.NONE),
            item("d2", "Оплатить интернет", DueText(DayLabel.Today, null), list = "Дом", color = "#46A758", completing = true),
        ),
        todayTimed = listOf(item("t1", "Созвон с командой", DueText(DayLabel.Today, "10:00–11:00"), priority = Priority.HIGH, recurring = true)),
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

    /** Перенос на сегодня: Room обновляет разделы не разом, и задача на миг в двух днях — экран не должен вылетать («Key was already used»). */
    private val trashState = TrashState(
        items = listOf(
            item("t1", "Позвонить врачу", DueText(DayLabel.Today, "12:30"), list = "Здоровье", color = "#E5484D"),
            item("t2", "Купить продукты", DueText(DayLabel.Date(LocalDate.of(2026, 10, 20), false), null), list = null, color = null),
        ),
        deletedOn = mapOf("t1" to LocalDate.of(2026, 10, 28), "t2" to LocalDate.of(2026, 10, 27)),
        isLoading = false,
    )

    /** Итерация 39: корзина — задачи как в списке с подписью «удалена …»; «Удалить все» просит подтверждение, «Восстановить все» — нет. */
    @Test
    fun trashShowsTasksAndAsksBeforeEraseAll() {
        var opened: String? = null
        show { TrashScreen(trashState, { intents += it }, onBack = {}, onOpenTask = { opened = it }) }

        composeRule.onNodeWithText("ЗАДАЧ: 2").assertExists()
        composeRule.onNodeWithTag("task:t1:subtitle", useUnmergedTree = true).assertTextEquals("удалена 28 окт")
        composeRule.onNodeWithTag("task:t2:subtitle", useUnmergedTree = true).assertTextEquals("удалена 27 окт")
        composeRule.onNodeWithText("Позвонить врачу").performClick()
        assertEquals("t1", opened)

        composeRule.onNodeWithTag(TAG_TRASH_RESTORE_ALL).performClick()
        assertEquals(listOf<Any>(TrashIntent.RestoreAll), intents.toList())

        composeRule.onNodeWithTag(TAG_TRASH_DELETE_ALL).performClick()
        composeRule.onNodeWithText("Удалить все навсегда?").assertExists()
        assertEquals("Пока не подтвердили, ничего не стёрто", listOf<Any>(TrashIntent.RestoreAll), intents.toList())
        composeRule.onNodeWithTag("trash_delete_all_dialog:done").performClick()
        assertEquals(listOf<Any>(TrashIntent.RestoreAll, TrashIntent.DeleteAll), intents.toList())
    }

    @Test
    fun emptyTrashShowsHint() {
        show { TrashScreen(TrashState(isLoading = false), { intents += it }, onBack = {}, onOpenTask = {}) }
        composeRule.onNodeWithTag(TAG_TRASH_EMPTY).assertExists()
        composeRule.onNodeWithText("Корзина пуста").assertExists()
        composeRule.onNodeWithTag(TAG_TRASH_DELETE_ALL).assertDoesNotExist()
    }

    @Test
    fun screenshotTrash() {
        show { TrashScreen(trashState, {}, onBack = {}, onOpenTask = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/trash_light.png")
    }

    /** Задача в корзине: плашка «В корзине», править нельзя, в «⋮» — «Восстановить» и «Удалить навсегда» (с подтверждением). */
    @Test
    fun trashedTaskIsReadOnlyWithRestoreAndEraseInMenu() {
        val task = com.prostovasei.simpletasks.testing.task("t1", title = "Позвонить врачу").copy(content = "Спросить про справку")
        val trashed = TrashedTask(TaskDetails(task, emptyList()), emptyList(), java.time.Instant.parse("2026-10-28T10:00:00Z"))
        val state = TrashTaskState(trashed, trashState.items.first(), LocalDate.of(2026, 10, 28), isLoading = false)
        show { TrashTaskScreen(state, { intents += it }, onClose = {}) }

        composeRule.onNodeWithTag(TAG_TRASH_TASK_BANNER).assertTextEquals("В корзине · удалена 28 окт")
        composeRule.onNodeWithTag(TAG_TRASH_TASK_TITLE).assertTextEquals("Позвонить врачу")
        composeRule.onNodeWithText("Спросить про справку").assertExists()

        composeRule.onNodeWithTag(TAG_TRASH_TASK_MENU).performClick()
        composeRule.onNodeWithTag(TAG_TRASH_TASK_RESTORE).performClick()
        assertEquals(listOf<Any>(TrashTaskIntent.Restore), intents.toList())

        composeRule.onNodeWithTag(TAG_TRASH_TASK_MENU).performClick()
        composeRule.onNodeWithTag(TAG_TRASH_TASK_DELETE).performClick()
        composeRule.onNodeWithText("Удалить навсегда?").assertExists()
        assertEquals("Без подтверждения ничего не стёрто", 1, intents.size)
        composeRule.onNodeWithTag("trash_delete_dialog:done").performClick()
        assertEquals(listOf<Any>(TrashTaskIntent.Restore, TrashTaskIntent.DeleteForever), intents.toList())
    }

    @Test
    fun screenshotTrashedTask() {
        val task = com.prostovasei.simpletasks.testing.task("t1", title = "Позвонить врачу").copy(content = "Спросить про справку")
        val trashed = TrashedTask(TaskDetails(task, emptyList()), emptyList(), java.time.Instant.parse("2026-10-28T10:00:00Z"))
        show { TrashTaskScreen(TrashTaskState(trashed, trashState.items.first(), LocalDate.of(2026, 10, 28), isLoading = false), {}, onClose = {}) }
        composeRule.onRoot().captureRoboImage("screenshots/trash_task_light.png")
    }

    /** Строка «Корзина» на «Списках»: число задач есть, пока корзина не пуста. */
    @Test
    fun listsShowTrashRowWithCountOnlyWhenNotEmpty() {
        var opened = false
        show { ListsScreen(ListsState(tree = ProjectTree(null, emptyList(), emptyList()), trashCount = 5), { intents += it }, {}, onOpenTrash = { opened = true }) }
        composeRule.onNodeWithTag(TAG_TRASH).performClick()
        composeRule.onNodeWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).assertTextEquals("5")
        assertTrue(opened)
    }

    @Test
    fun listsHideTrashCountWhenEmpty() {
        show { ListsScreen(ListsState(tree = ProjectTree(null, emptyList(), emptyList()), trashCount = 0), { intents += it }, {}) }
        composeRule.onNodeWithTag(TAG_TRASH).assertExists()
        composeRule.onNodeWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).assertDoesNotExist()
    }

    /** Итерация 40: долгое нажатие — режим выбора; нажатие отмечает, «Выбрать все» без будущих повторов, «✕» закрывает. */
    @Test
    fun longPressStartsSelectionAndRibbonFollowsChoice() {
        show { AllScreen(allState, { intents += it }) }
        composeRule.onNodeWithTag(TAG_SELECTION_BAR).assertDoesNotExist()

        composeRule.onNodeWithTag("task:t1").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 1")
        composeRule.onNodeWithTag(TAG_ACTION_RIBBON).assertExists()
        composeRule.onNodeWithTag(TAG_BULK_REOPEN).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_BULK_COMPLETE).assertIsEnabled()

        // В режиме нажатие отмечает, а не открывает и не выполняет.
        composeRule.onNodeWithText("Записаться к врачу").performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 2")
        composeRule.onNodeWithTag("task:d2:check").performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 3")
        // «Оплатить интернет» только отмечается (ещё не выполнена в базе) — вернуть пока нечего.
        composeRule.onNodeWithTag(TAG_BULK_REOPEN).assertIsNotEnabled()
        assertTrue("Ничего не выполнено и не открыто", intents.isEmpty())

        composeRule.onNodeWithTag(TAG_SELECTION_ALL).performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 5")

        composeRule.onNodeWithTag(TAG_SELECTION_CLOSE).performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_BAR).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_ACTION_RIBBON).assertDoesNotExist()
    }

    @Test
    fun futureRepeatCannotBeSelected() {
        show { AllScreen(allState, { intents += it }) }
        composeRule.onNodeWithTag("task:t1@0").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TAG_SELECTION_BAR).assertDoesNotExist()
    }

    @Test
    fun screenshotSelection() {
        show { AllScreen(allState, {}) }
        composeRule.onNodeWithTag("task:t1").performTouchInput { longClick() }
        composeRule.onNodeWithText("Записаться к врачу").performClick()
        composeRule.onRoot().captureRoboImage("screenshots/selection_light.png")
    }

    @Test
    fun completedAndSearchAndTrashSelect() {
        show { CompletedScreen(completedState, { intents += it }, {}, {}) }
        composeRule.onNodeWithText("Отправить отчёт").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 1")
        composeRule.onNodeWithTag(TAG_BULK_COMPLETE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_BULK_REOPEN).assertIsEnabled()
        assertTrue("Долгое нажатие не возвращает задачу", intents.isEmpty())
    }

    @Test
    fun searchResultsSelect() {
        show { SearchScreen(searchState, { intents += it }, {}, autoFocus = false) }
        composeRule.onNodeWithText("Вернуть книгу").performTouchInput { longClick() }
        composeRule.onNodeWithText("Позвонить врачу").performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 2")
        composeRule.onNodeWithTag(TAG_BULK_COMPLETE).assertIsEnabled()
        composeRule.onNodeWithTag(TAG_BULK_REOPEN).assertIsEnabled()
    }

    @Test
    fun trashSelectionHasRestoreAndEraseForever() {
        show { TrashScreen(trashState, { intents += it }, onBack = {}, onOpenTask = {}) }
        composeRule.onNodeWithText("Позвонить врачу").performTouchInput { longClick() }
        composeRule.onNodeWithTag(TAG_BULK_RESTORE).assertExists()
        composeRule.onNodeWithTag(TAG_BULK_COMPLETE).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_BULK_DELETE_FOREVER).performClick()
        composeRule.onNodeWithText("Удалить навсегда?").assertExists()
    }

    @Test
    fun sameTaskInTwoDaysDuringMoveDoesNotCrash() {
        val moved = allState.overdue.first()
        show { AllScreen(allState.copy(todayAllDay = allState.todayAllDay + moved), { intents += it }) }
        composeRule.onAllNodesWithText(moved.title).assertCountEquals(2)
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
                TaskRow(title = "Записаться к врачу", priority = Priority.HIGH, listEmoji = null, isCompleting = true, onToggle = {}, trailing = listOf("18:30"))
                TaskRow(title = "Купить продукты", priority = Priority.NONE, listEmoji = null, isCompleting = false, onToggle = {})
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

    /** Итерация 41: «Сегодня» слева от плашки не обрезается — и с крупным системным шрифтом (как у PM). */
    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi", fontScale = 1.3f)
    fun screenshotInboxDayColumnLargeFont() {
        val tasks = listOf(
            item("i1", "Позвонить врачу", DueText(DayLabel.Today, "12:30"), list = null, color = null, priority = Priority.HIGH),
            item("i2", "Купить продукты", DueText(DayLabel.Tomorrow, null), list = null, color = null),
            item("i3", "Отчёт за неделю", DueText(DayLabel.Yesterday, "11:00"), list = null, color = null, priority = Priority.NONE),
        )
        show { InboxScreen(InboxState(tasks = tasks, isLoading = false), { intents += it }) }
        composeRule.onNodeWithText("Сегодня").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/inbox_day_column_large_font.png")
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
            CompletedDay(DayLabel.Today, listOf(CompletedTask("c1", "Отправить отчёт", "Работа", "💼", false, "09:05"))),
            CompletedDay(DayLabel.Date(LocalDate.of(2026, 10, 1), showYear = false), listOf(CompletedTask("c2", "Купить продукты", null, "📥", true, null))),
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

    /** Итерация 49: «Не буду делать» — по дням, ✕ возвращает задачу, нажатие на строку открывает её. */
    @Test
    fun wontDoScreenReopensAndOpens() {
        var opened: String? = null
        val state = com.prostovasei.simpletasks.ui.feature.wontdo.WontDoState(days = completedState.days, repeats = setOf("c1"), isLoading = false)
        show { com.prostovasei.simpletasks.ui.feature.wontdo.WontDoScreen(state, { intents += it }, {}, { opened = it }) }

        composeRule.onNodeWithText("Не буду делать").assertExists()
        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.onNodeWithTag("wont_do:c1:check", useUnmergedTree = true).performClick()

        assertEquals("c2", opened)
        assertEquals(listOf<Any>(com.prostovasei.simpletasks.ui.feature.wontdo.WontDoIntent.Reopen("c1")), intents)
        composeRule.onRoot().captureRoboImage("screenshots/wont_do_light.png")
    }

    /** Итерация 49: строка «Не буду делать» на «Списках» — с числом задач. */
    @Test
    fun listsShowWontDoRow() {
        var opened = false
        show { ListsScreen(ListsState(tree = ProjectTree(null, emptyList(), emptyList()), wontDoCount = 3), { intents += it }, {}, onOpenWontDo = { opened = true }) }
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.feature.lists.TAG_WONT_DO).performClick()
        composeRule.onNodeWithTag("${com.prostovasei.simpletasks.ui.feature.lists.TAG_WONT_DO}:count", useUnmergedTree = true).assertTextEquals("3")
        assertTrue(opened)
    }

    /** Итерация 49: задача «Не буду делать» — только чтение; «Удалить» без подтверждения. */
    @Test
    fun wontDoTaskIsReadOnly() {
        val task = com.prostovasei.simpletasks.testing.task("t1", title = "Стирка").copy(status = com.prostovasei.simpletasks.domain.model.TaskStatus.ABANDONED)
        val shown = TrashedTask(TaskDetails(task, emptyList()), emptyList(), java.time.Instant.parse("2026-11-06T10:00:00Z"))
        show {
            TrashTaskScreen(
                TrashTaskState(shown, trashState.items.first(), LocalDate.of(2026, 11, 6), isLoading = false), { intents += it }, onClose = {},
                kind = com.prostovasei.simpletasks.ui.feature.trash.ReadonlyKind.WontDo,
            )
        }
        composeRule.onNodeWithTag(TAG_TRASH_TASK_BANNER).assertTextEquals("Не буду делать · отмечено пт, 06 ноя")
        composeRule.onRoot().captureRoboImage("screenshots/wont_do_task_light.png")
        composeRule.onNodeWithTag(TAG_TRASH_TASK_MENU).performClick()
        composeRule.onNodeWithText("Вернуть в работу").assertExists()
        composeRule.onNodeWithTag(TAG_TRASH_TASK_DELETE).performClick()
        assertEquals(listOf<Any>(TrashTaskIntent.DeleteForever), intents.toList())
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
        emoji = "💼",
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

    /** Итерация 47: эмодзи списка — из сетки или любой с клавиатуры; список получает его по «Готово». */
    @Test
    fun emojiDialogPicksFromGridOrKeyboard() {
        show { ProjectScreen(projectState.copy(dialog = ProjectDialog.Emoji), { intents += it }, {}) }

        composeRule.onNodeWithText("Эмодзи списка").assertExists()
        composeRule.onNodeWithTag("$TAG_EMOJI_OPTION:🏠").performClick()
        composeRule.onNodeWithText("Готово").performClick()
        assertEquals(listOf<Any>(ProjectIntent.SetEmoji("🏠")), intents)
    }

    @Test
    fun emojiDialogTakesAnyEmojiFromKeyboard() {
        show { ProjectScreen(projectState.copy(dialog = ProjectDialog.Emoji), { intents += it }, {}) }

        composeRule.onNodeWithTag(TAG_EMOJI_FIELD).performTextInput("🦄 единорог")
        composeRule.onNodeWithText("Готово").performClick()
        assertEquals(listOf<Any>(ProjectIntent.SetEmoji("🦄")), intents)
    }

    @Test
    fun menuHasEmojiItemInsteadOfColorAndIcon() {
        show { ProjectScreen(projectState, { intents += it }, {}) }

        composeRule.onNodeWithTag(TAG_PROJECT_MENU).performClick()
        composeRule.onNodeWithText("Цвет").assertDoesNotExist()
        composeRule.onNodeWithText("Значок").assertDoesNotExist()
        composeRule.onNodeWithText("Эмодзи").performClick()

        assertEquals(listOf<Any>(ProjectIntent.OpenDialog(ProjectDialog.Emoji)), intents)
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

    /** Итерация 44: вкладок шесть — на самом узком экране они сжимаются, «+» и «Настройки» остаются на экране и нажимаются. */
    @Test
    @Config(qualifiers = "w320dp-h900dp-xhdpi")
    fun bottomBarFitsNarrowScreenWithWeekTab() {
        val selected = mutableListOf<Tab>()
        var added = 0
        show { BottomBar(selected = Tab.WEEK, onSelect = { selected += it }, onAdd = { added++ }) }

        Tab.entries.forEach { composeRule.onNodeWithTag("tab:${it.name}").assertIsDisplayed() }
        composeRule.onNodeWithTag("tab:WEEK").performClick()
        composeRule.onNodeWithTag(TAG_ADD_TASK).assertIsDisplayed().performClick()
        assertEquals(listOf(Tab.WEEK), selected)
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
        show { BottomBar(selected = Tab.SEARCH, onSelect = {}, inboxCount = 3) }
        composeRule.onRoot().captureRoboImage("screenshots/bottom_bar_light.png")
    }

    /** Итерация 52: у «Входящих» — число неразобранных; при нуле кружка нет. */
    @Test
    fun bottomBarShowsInboxCount() {
        var count by mutableStateOf(3)
        show { BottomBar(selected = Tab.ALL, onSelect = {}, inboxCount = count) }
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.navigation.TAG_INBOX_BADGE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("3", useUnmergedTree = true).assertExists()
        count = 0
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.navigation.TAG_INBOX_BADGE, useUnmergedTree = true).assertDoesNotExist()
    }

    /** Итерация 52: «Когда-нибудь» и «Без списка» на «Списках» — над «Выполненными», с числами. */
    @Test
    fun listsShowSomedayAndNoListRows() {
        var someday = false
        var noList = false
        show {
            ListsScreen(
                ListsState(tree = ProjectTree(null, emptyList(), emptyList()), somedayCount = 7, noListCount = 2), { intents += it }, {},
                onOpenSomeday = { someday = true }, onOpenNoList = { noList = true },
            )
        }
        composeRule.onNodeWithTag("${com.prostovasei.simpletasks.ui.feature.lists.TAG_SOMEDAY}:count", useUnmergedTree = true).assertTextEquals("7")
        composeRule.onNodeWithTag("${com.prostovasei.simpletasks.ui.feature.lists.TAG_NO_LIST}:count", useUnmergedTree = true).assertTextEquals("2")
        // 0.52.1: у разделов свои эмодзи — 💭 «Когда-нибудь» и 📌 «Без списка».
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.feature.lists.TAG_SOMEDAY).assertTextContains("💭")
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.feature.lists.TAG_NO_LIST).assertTextContains("📌")
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.feature.lists.TAG_SOMEDAY).performClick()
        composeRule.onNodeWithTag(com.prostovasei.simpletasks.ui.feature.lists.TAG_NO_LIST).performClick()
        assertTrue(someday && noList)
    }
}
