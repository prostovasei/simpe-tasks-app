package com.prostovasei.simpletasks.ui.feature

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.assertTextEquals
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LIST_EDIT_EMOJI
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LIST_EDIT_FOLDER
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LIST_EDIT_FOLDERS
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LIST_EDIT_MORE
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LIST_EDIT_QUICK
import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_DIALOG
import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_FIELD
import com.prostovasei.simpletasks.ui.feature.task.TAG_DIALOG_DONE
import java.time.Instant
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
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

    private val folderTree = ProjectTree(
        inbox = ProjectWithCount(project("inbox", isInbox = true), 0),
        groups = listOf(GroupWithProjects(ProjectGroup("g1", "Личное", 0, Instant.EPOCH, Instant.EPOCH, emoji = "🧩"), emptyList())),
        ungrouped = emptyList(),
    )

    /** Итерация 56: окно «Новый список» — эмодзи из быстрого выбора и папка сразу при создании. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w412dp-h900dp-xhdpi")
    fun createListWithEmojiAndFolder() {
        composeRule.setContent {
            AppTheme { ListsScreen(ListsState(tree = folderTree, dialog = ListsDialog.CreateList), { intents += it }, {}) }
        }
        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Покупки")
        composeRule.onNodeWithTag("$TAG_LIST_EDIT_QUICK:🛒").performClick()
        composeRule.onNodeWithTag("$TAG_LIST_EDIT_FOLDER:g1").performClick()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/list_create_dialog.png")
        composeRule.onNodeWithText("Создать").performClick()

        assertEquals(listOf<Any>(ListsIntent.CreateList("Покупки", "g1", "🛒")), intents)
    }

    /** Итерация 56: окно «Новая папка» — без выбора папки, с эмодзи; «Создать» бледная, пока нет названия. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w412dp-h900dp-xhdpi")
    fun createFolderWithEmoji() {
        composeRule.setContent {
            AppTheme { ListsScreen(ListsState(tree = folderTree, dialog = ListsDialog.CreateFolder), { intents += it }, {}) }
        }
        composeRule.onNodeWithTag(TAG_LIST_EDIT_FOLDERS).assertDoesNotExist()
        composeRule.onNodeWithText("Создать").assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Проекты")
        composeRule.onNodeWithTag("$TAG_LIST_EDIT_QUICK:🎯").performClick()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/folder_create_dialog.png")
        composeRule.onNodeWithText("Создать").performClick()

        assertEquals(listOf<Any>(ListsIntent.CreateFolder("Проекты", "🎯")), intents)
    }

    /** Итерация 56: «Изменить» папку — название и эмодзи заполнены; 📁 сохраняется как «без эмодзи». */
    @Test
    fun editFolderChangesNameAndEmoji() {
        composeRule.setContent {
            AppTheme { ListsScreen(ListsState(tree = folderTree, dialog = ListsDialog.EditFolder("g1", "Личное", "🧩")), { intents += it }, {}) }
        }
        composeRule.onNodeWithTag(TAG_NAME_FIELD).assertTextEquals("Личное")
        composeRule.onNodeWithTag("$TAG_LIST_EDIT_QUICK:🧩").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_LIST_EDIT_QUICK:📁").performClick()
        composeRule.onNodeWithText("Сохранить").performClick()

        assertEquals(listOf<Any>(ListsIntent.UpdateFolder("Личное", null)), intents)
    }

    /** Итерация 56: «…» открывает окно со всеми эмодзи; выбранный там — на плашке окна создания. */
    @Test
    fun moreOpensFullEmojiPicker() {
        composeRule.setContent {
            AppTheme { ListsScreen(ListsState(tree = folderTree, dialog = ListsDialog.CreateList), { intents += it }, {}) }
        }
        composeRule.onNodeWithTag(TAG_LIST_EDIT_MORE).performClick()
        composeRule.onNodeWithTag(TAG_EMOJI_DIALOG).assertExists()
        composeRule.onNodeWithTag(TAG_EMOJI_FIELD).performTextReplacement("📚")
        composeRule.onNodeWithTag("$TAG_EMOJI_DIALOG:$TAG_DIALOG_DONE").performClick()
        composeRule.onNodeWithTag(TAG_LIST_EDIT_EMOJI).assertContentDescriptionContains("📚", substring = true)
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
