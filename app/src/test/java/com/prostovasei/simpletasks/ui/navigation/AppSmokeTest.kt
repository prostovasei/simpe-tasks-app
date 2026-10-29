package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Входящие пусты").assertExists()

        // Поиск: поле ввода сразу на экране, ничего не найдено.
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
        composeRule.onNodeWithText("Лишняя задача").assertDoesNotExist()

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Лишняя задача") }
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasText(text) or androidx.compose.ui.test.hasContentDescription(text))
        .fetchSemanticsNodes().isNotEmpty()
