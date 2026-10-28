package com.prostovasei.simpletasks.ui.feature.update

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.core.update.AvailableUpdate
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.task.TAG_DIALOG_CANCEL
import com.prostovasei.simpletasks.ui.feature.task.TAG_DIALOG_DONE
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Окно обновления: тексты и кнопки каждого шага, нажатия, скриншоты (итерация 32). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class UpdateDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<UpdateDialogIntent>()
    private val update = AvailableUpdate(130, "0.32.0", "https://example.com/a.apk", 16_777_216)

    private fun show(state: UpdateDialogState, dark: Boolean = false) {
        composeRule.setContent { AppTheme(darkTheme = dark) { Dialog(state) } }
    }

    @Composable
    private fun Dialog(state: UpdateDialogState) = UpdateDialog(state) { intents += it }

    private fun tap(button: String) = composeRule.onNodeWithTag("$TAG_UPDATE_DIALOG:$button").performClick()

    @Test
    fun checkingOffersOnlyClose() {
        show(UpdateDialogState.Checking)
        composeRule.onNodeWithText("Проверяю наличие обновления…").assertIsDisplayed()
        composeRule.onNodeWithText("Закрыть").assertIsDisplayed()
        composeRule.onNodeWithTag("$TAG_UPDATE_DIALOG:$TAG_DIALOG_DONE").assertDoesNotExist()
        tap(TAG_DIALOG_CANCEL)
        assertEquals(listOf(UpdateDialogIntent.Close), intents)
    }

    @Test
    fun upToDateShowsInstalledVersion() {
        show(UpdateDialogState.UpToDate)
        composeRule.onNodeWithText("Обновлений нет").assertIsDisplayed()
        composeRule.onNodeWithText("Установлена последняя версия").assertIsDisplayed()
        composeRule.onNodeWithText("Закрыть").assertIsDisplayed()
    }

    @Test
    fun availableShowsVersionSizeAndDownloadButton() {
        show(UpdateDialogState.Available(update))
        composeRule.onNodeWithText("Доступно обновление").assertIsDisplayed()
        composeRule.onNodeWithText("0.32.0 (сборка 130)").assertIsDisplayed()
        composeRule.onNodeWithText("около 16 МБ", substring = true).assertIsDisplayed()
        tap(TAG_DIALOG_DONE)
        assertEquals(listOf(UpdateDialogIntent.Download), intents)
    }

    @Test
    fun downloadingShowsPercentAndCancelStopsIt() {
        show(UpdateDialogState.Downloading(update, 45))
        composeRule.onNodeWithText("Загружаю обновление").assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_UPDATE_PERCENT).assertIsDisplayed()
        composeRule.onNodeWithText("45%").assertIsDisplayed()
        composeRule.onNodeWithText("7,2 из 16 МБ").assertIsDisplayed()
        tap(TAG_DIALOG_CANCEL)
        assertEquals(listOf(UpdateDialogIntent.Close), intents)
    }

    @Test
    fun readyOffersInstallOrLater() {
        show(UpdateDialogState.Ready(update, File("update.apk")))
        composeRule.onNodeWithText("Обновление загружено").assertIsDisplayed()
        tap(TAG_DIALOG_DONE)
        tap(TAG_DIALOG_CANCEL)
        assertEquals(listOf(UpdateDialogIntent.Install, UpdateDialogIntent.Close), intents)
        composeRule.onNodeWithText("Не сейчас").assertIsDisplayed()
    }

    @Test
    fun errorsOfferRetry() {
        show(UpdateDialogState.CheckFailed)
        composeRule.onNodeWithText("Не удалось проверить").assertIsDisplayed()
        tap(TAG_DIALOG_DONE)
        assertEquals(listOf(UpdateDialogIntent.Retry), intents)
    }

    @Test
    fun permissionOpensPhoneSettings() {
        show(UpdateDialogState.NeedsPermission(update))
        composeRule.onNodeWithText("Нужно разрешение").assertIsDisplayed()
        tap(TAG_DIALOG_DONE)
        assertEquals(listOf(UpdateDialogIntent.GrantPermission), intents)
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi", fontScale = 1.5f)
    fun smallScreenLargeFontKeepsButtons() {
        show(UpdateDialogState.Available(update))
        composeRule.onNodeWithTag("$TAG_UPDATE_DIALOG:$TAG_DIALOG_DONE").assertIsDisplayed()
        composeRule.onNodeWithTag("$TAG_UPDATE_DIALOG:$TAG_DIALOG_CANCEL").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("screenshots/update_dialog_small_large_font.png")
    }

    @Test
    fun screenshotAvailable() {
        show(UpdateDialogState.Available(update))
        composeRule.onRoot().captureRoboImage("screenshots/update_dialog_available.png")
    }

    @Test
    fun screenshotDownloading() {
        show(UpdateDialogState.Downloading(update, 45))
        composeRule.onRoot().captureRoboImage("screenshots/update_dialog_downloading.png")
    }

    @Test
    fun screenshotReadyDark() {
        show(UpdateDialogState.Ready(update, File("update.apk")), dark = true)
        composeRule.onRoot().captureRoboImage("screenshots/update_dialog_ready_dark.png")
    }

}
