package com.prostovasei.simpletasks.ui.feature

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.prostovasei.simpletasks.designsystem.components.TAG_NAME_FIELD
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.ui.feature.lists.ListsDialog
import com.prostovasei.simpletasks.ui.feature.lists.ListsIntent
import com.prostovasei.simpletasks.ui.feature.lists.ListsScreen
import com.prostovasei.simpletasks.ui.feature.lists.ListsState
import com.prostovasei.simpletasks.ui.feature.project.ProjectDialog
import com.prostovasei.simpletasks.ui.feature.project.ProjectIntent
import com.prostovasei.simpletasks.ui.feature.project.ProjectScreen
import com.prostovasei.simpletasks.ui.feature.project.ProjectState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Диалоги с полем ввода. Отдельный класс на прежнем правиле createComposeRule: с новым (v2) мигающий курсор
 * не даёт тестовому экрану «простаивать», и тест зависает.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class TextInputDialogsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<Any>()

    @Test
    fun createListSendsNameWithoutFolder() {
        val tree = ProjectTree(inbox = ProjectWithCount(project("inbox", isInbox = true), 0), groups = emptyList(), ungrouped = emptyList())
        composeRule.setContent {
            AppTheme { ListsScreen(ListsState(tree = tree, dialog = ListsDialog.CreateList), { intents += it }, {}) }
        }

        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("  Покупки ")
        composeRule.onNodeWithText("Создать").performClick()

        assertEquals(listOf<Any>(ListsIntent.CreateList("Покупки", null)), intents)
    }

    @Test
    fun renameProjectSendsNewName() {
        composeRule.setContent {
            AppTheme { ProjectScreen(ProjectState(name = "Работа", isLoading = false, dialog = ProjectDialog.Rename), { intents += it }, {}) }
        }

        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Офис")
        composeRule.onNodeWithText("Сохранить").performClick()

        assertEquals(listOf<Any>(ProjectIntent.Rename("Офис")), intents)
    }

    @Test
    fun emptyNameCannotBeSaved() {
        composeRule.setContent {
            AppTheme { ProjectScreen(ProjectState(name = "Работа", isLoading = false, dialog = ProjectDialog.Rename), { intents += it }, {}) }
        }

        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("   ")
        composeRule.onNodeWithText("Сохранить").performClick()

        assertEquals(emptyList<Any>(), intents)
    }
}
