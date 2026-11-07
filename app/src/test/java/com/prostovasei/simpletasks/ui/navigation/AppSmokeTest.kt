package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_SCRIM
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextReplacement
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.designsystem.components.TAG_NAME_FIELD
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.feature.lists.TAG_NEW_LIST
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_CLOSE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_DELETE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_MENU
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_TITLE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_SAVE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_TITLE
import com.prostovasei.simpletasks.ui.feature.search.TAG_SEARCH_FIELD
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_HEADER
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_MOVE
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_TOGGLE
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.widget.widgetEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import com.prostovasei.simpletasks.ui.feature.lists.TAG_TRASH_COUNT
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_EMPTY
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_BANNER
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_MENU
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_RESTORE
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_BAR
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_COUNT
import com.prostovasei.simpletasks.designsystem.components.TAG_ACTION_RIBBON
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_DELETE
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Запуск настоящего приложения (Hilt, Room, навигация): вкладки открываются, список создаётся,
 * открывается и закрывается кнопкой «Назад». Ловит ошибки, которые видны только на телефоне.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun walkThroughTabsAndCreateList() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }

        composeRule.onNodeWithTag("tab:INBOX").performClick()
        composeRule.onNodeWithText("Всё разобрано").assertExists()

        // Поиск: поле ввода сразу на экране, ничего не найдено.
        // Итерация 44: вкладка «Неделя» — сетка из семи дней и мини-календарь.
        composeRule.onNodeWithTag("tab:WEEK").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("week-screen") }
        composeRule.onNodeWithTag("week-calendar").assertExists()

        composeRule.onNodeWithTag("tab:SEARCH").performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("молоко")
        composeRule.onNodeWithText("Ничего не найдено").assertExists()

        composeRule.onNodeWithTag("tab:SETTINGS").performClick()
        composeRule.onNodeWithText("Импорт из TickTick").assertExists()

        composeRule.onNodeWithTag("tab:LISTS").performClick()
        composeRule.onNodeWithText("Списков пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_NEW_LIST).performClick()
        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Покупки")
        composeRule.onNodeWithText("Создать").performClick()

        // Новый список открывается сразу, нижний бар скрыт.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В списке пусто") }
        composeRule.onNodeWithTag("tab:ALL").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Покупки") }
        composeRule.onNodeWithTag("tab:ALL").performClick()
        composeRule.onNodeWithText("Всё сделано").assertExists()
    }

    @Test
    fun createTaskOpenItAndRename() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        // «+» на «Все» → задача на сегодня.
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Купить продукты")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Купить продукты") }
        composeRule.onNodeWithText("Сегодня").assertExists()

        // Поиск находит новую задачу по части слова.
        composeRule.onNodeWithTag("tab:SEARCH").performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("прод")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("НАЙДЕНА 1 ЗАДАЧА") }
        composeRule.onNodeWithTag("tab:ALL").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_SEARCH_FIELD).fetchSemanticsNodes().isEmpty() }

        // Тап по карточке → шторка задачи → переименование → «Готово».
        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_TITLE).performTextReplacement("Купить продукты на неделю")
        composeRule.onNodeWithTag(TAG_DETAIL_CLOSE).performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Купить продукты на неделю") }
    }

    /** Итерация 36: удаление из шторки — без вопроса; под шторкой «Задача удалена · Отмена», «Отмена» возвращает задачу. */
    @Test
    fun deleteTaskShowsUndoAndUndoBringsItBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Лишняя задача")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Лишняя задача") }

        composeRule.onNodeWithText("Лишняя задача").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Задача удалена") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }
        // Строка уходит из списка с анимацией — ждём, пока исчезнет.
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Лишняя задача") }

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Лишняя задача") }
    }

    /** Итерация 37: «Просрочено · N» — стрелка сворачивает прошедшие дни, значок переносит на сегодня с «Отменой». */
    @Test
    fun overdueBlockCollapsesAndMovesToToday() {
        val entry = ApplicationProvider.getApplicationContext<android.app.Application>().widgetEntryPoint()
        val past = entry.time().today().minusDays(3)
        val id = runBlocking { entry.tasks().createTask(NewTask(entry.projects().getInbox().id, "Просроченная", due = TaskDue.AllDay(past))) }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Просроченная") }
        composeRule.onNodeWithText("Просрочено · 1").assertExists()

        composeRule.onNodeWithTag(TAG_OVERDUE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Просроченная") }
        composeRule.onNodeWithText("Просрочено · 1").assertExists()
        composeRule.onNodeWithTag(TAG_OVERDUE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Просроченная") }

        composeRule.onNodeWithTag(TAG_OVERDUE_MOVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Перенесено на сегодня: 1") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_OVERDUE_HEADER).fetchSemanticsNodes().isEmpty() }
        assertEquals(TaskDue.AllDay(entry.time().today()), runBlocking { entry.tasks().getTask(id) }?.due)

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_OVERDUE_HEADER).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TaskDue.AllDay(past), runBlocking { entry.tasks().getTask(id) }?.due)
    }

    /** Итерация 40: долгое нажатие — выбор; вместо вкладок лента; «Удалить» убирает выбранные в корзину, «Отмена» возвращает. */
    @Test
    fun bulkDeleteFromSelectionAndUndo() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        listOf("Первая", "Вторая").forEach { title ->
            composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
            composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement(title)
            composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists(title) }
        }

        composeRule.onNodeWithText("Первая").performTouchInput { longClick() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_SELECTION_BAR).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("tab:ALL").assertDoesNotExist()
        composeRule.onNodeWithText("Вторая").performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 2")

        composeRule.onNodeWithTag(TAG_ACTION_RIBBON).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(TAG_BULK_DELETE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Удалено: 2") }
        composeRule.onNodeWithTag(TAG_SELECTION_BAR).assertDoesNotExist()
        composeRule.onNodeWithTag("tab:ALL").assertExists()
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Первая") }

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Первая") && composeRule.onAllNodesWithTextExists("Вторая") }
    }

    /** Итерация 39: удалённая задача лежит в «Корзине» (строка в «Списках» со счётчиком), открывается без правки и возвращается из «⋮». */
    @Test
    fun deletedTaskGoesToTrashAndComesBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("В корзину")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В корзину") }
        composeRule.onNodeWithText("В корзину").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }

        composeRule.onNodeWithTag("tab:LISTS").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).assertTextEquals("1")
        composeRule.onNodeWithTag(TAG_TRASH).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В корзину") }

        composeRule.onNodeWithText("В корзину").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_TASK_BANNER).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_TRASH_TASK_MENU).performClick()
        composeRule.onNodeWithTag(TAG_TRASH_TASK_RESTORE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Задача восстановлена") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_TASK_BANNER).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_EMPTY).fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithTag("tab:ALL").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В корзину") }
    }

    /** Итерация 38: закрыли карточку новой задачи, не сохранив, — следующее «+» открывается с тем же названием. */
    @Test
    fun draftOfNewTaskComesBackOnNextPlus() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Купить билеты")
        // Нажатие на затемнение под карточкой закрывает её без сохранения (как «Назад»).
        composeRule.onNodeWithTag(TAG_QUICK_SCRIM).performTouchInput { click(Offset(centerX, bottom - 5f)) }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }

        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Купить билеты")))

        // Сохранили — черновика больше нет.
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    /** Итерация 38: запрос запоминается по «поиску» на клавиатуре; при пустом поле он в «Недавних», нажатие ищет. */
    @Test
    fun searchRemembersQueriesAndRepeatsThem() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag("tab:SEARCH").performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("молоко")
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performImeAction()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("")

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("НЕДАВНИЕ") }
        composeRule.onNodeWithTag("search_recent:молоко").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Ничего не найдено") }
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).assertTextEquals("молоко")
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasText(text) or androidx.compose.ui.test.hasContentDescription(text))
        .fetchSemanticsNodes().isNotEmpty()

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
