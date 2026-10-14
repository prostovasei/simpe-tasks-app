package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.designsystem.components.TAG_NAME_FIELD
import com.prostovasei.simpletasks.ui.feature.lists.TAG_NEW_LIST
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
        composeRule.onNodeWithText("Задач со сроком нет").assertExists()

        composeRule.onNodeWithTag("tab:INBOX").performClick()
        composeRule.onNodeWithText("Во «Входящих» пусто").assertExists()

        composeRule.onNodeWithTag("tab:SETTINGS").performClick()
        composeRule.onNodeWithText("Импорт из TickTick").assertExists()

        composeRule.onNodeWithTag("tab:LISTS").performClick()
        composeRule.onNodeWithText("Списков пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_NEW_LIST).performClick()
        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Покупки")
        composeRule.onNodeWithText("Создать").performClick()

        // Новый список открывается сразу, нижний бар скрыт.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В списке нет активных задач") }
        composeRule.onNodeWithTag("tab:ALL").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Покупки") }
        composeRule.onNodeWithTag("tab:ALL").performClick()
        composeRule.onNodeWithText("Задач со сроком нет").assertExists()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasText(text) or androidx.compose.ui.test.hasContentDescription(text))
        .fetchSemanticsNodes().isNotEmpty()
