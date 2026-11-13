package com.prostovasei.simpletasks.ui.feature.settings

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.DataStats
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Экран «Настройки»: отображение состояний, нажатия кнопок и скриншоты в обеих темах. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<SettingsIntent>()

    private fun show(state: SettingsState, darkTheme: Boolean = false) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    private val doneState = SettingsState(
        stats = DataStats(3399, 145, 12),
        import = ImportState.Done(previewReport),
        export = ExportState.Done(3399),
    )

    private fun showInfo(state: SettingsState, widgetLog: WidgetLogActions = WidgetLogActions.None) {
        composeRule.setContent { AppTheme { AppInfoScreen(state, { intents += it }, onBack = {}, appVersion = AppVersion("0.3.1", 42), widgetLog = widgetLog) } }
    }

    /** #241: обновление и версия — в разделе «Приложение» единых настроек, а не в настройках задач. */
    @Test
    fun updateSectionShowsAvailableVersionAndInstallButton() {
        showInfo(doneState.copy(update = UpdateState.Available(com.prostovasei.simpletasks.core.update.AvailableUpdate(40, "0.10.1", "u"))))
        composeRule.onNodeWithText("Доступна версия 0.10.1 (сборка 40)").assertExists()
        composeRule.onNodeWithTag(TAG_UPDATE_BUTTON).performClick()
        assertEquals(SettingsIntent.InstallUpdate, intents.last())
    }

    @Test
    fun updateSectionCheckButton() {
        showInfo(doneState)
        composeRule.onNodeWithTag(TAG_UPDATE_CHECK).performClick()
        assertEquals(SettingsIntent.CheckUpdate, intents.last())
    }

    /** Скрытый журнал виджета: долгое нажатие на версию, «Отправить» и «Очистить». */
    @Test
    fun longPressOnVersionShowsWidgetLog() {
        val shared = mutableListOf<String>()
        var cleared = false
        showInfo(doneState, WidgetLogActions(read = { "16.10 17:37  вкладка: inbox" }, share = { shared += it }, clear = { cleared = true }))
        composeRule.onNodeWithTag(TAG_VERSION).performTouchInput { longClick() }
        composeRule.onNodeWithText("Журнал виджета").assertExists()
        composeRule.onNodeWithText("16.10 17:37  вкладка: inbox").assertExists()
        composeRule.onNodeWithText("Отправить").performClick()
        assertEquals(listOf("16.10 17:37  вкладка: inbox"), shared)
        composeRule.onNodeWithText("Очистить").performClick()
        assertEquals(true, cleared)
        composeRule.onNodeWithText("Журнал виджета").assertDoesNotExist()
    }

    @Test
    fun showsStatsAndImportReport() {
        show(doneState)

        composeRule.onNodeWithText("Задач в базе: 3399 (активных: 145)").assertExists()
        composeRule.onNodeWithText("Списков: 12").assertExists()
        composeRule.onNodeWithText("Импорт завершён").assertExists()
        composeRule.onNodeWithText("Добавлено задач: 3399").assertExists()
        composeRule.onNodeWithText("Пунктов чек-листов: 1186").assertExists()
        composeRule.onNodeWithText("Подзадач связано: 22").assertExists()
    }

    @Test
    fun showsExportResultAndVersionLivesInAppSection() {
        show(doneState)
        composeRule.onNodeWithText("Сохранено задач: 3399").assertExists()
        composeRule.onNodeWithText("Версия 0.3.1 (сборка 42)").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_UPDATE_CHECK).assertDoesNotExist()
    }

    @Test
    fun appSectionShowsVersion() {
        showInfo(doneState)
        composeRule.onNodeWithText("Версия 0.3.1 (сборка 42)").assertExists()
    }

    /** Единые настройки (#241): три раздела, подпись «Приложения» с версией; каждый ведёт на свою страницу. */
    @Test
    fun settingsHomeLeadsToThreeSections() {
        val clicks = mutableListOf<String>()
        composeRule.setContent {
            AppTheme { AppSettingsScreen(AppVersion("0.66.0", 320), onBack = { clicks += "back" }, onTasks = { clicks += "tasks" }, onFinance = { clicks += "finance" }, onApp = { clicks += "app" }) }
        }
        composeRule.onNodeWithText("Настройки").assertExists()
        composeRule.onNodeWithText("Версия 0.66.0 · обновление").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/settings_home_light.png")
        composeRule.onNodeWithTag(TAG_SETTINGS_TASKS).performClick()
        composeRule.onNodeWithTag(TAG_SETTINGS_FINANCE).performClick()
        composeRule.onNodeWithTag(TAG_SETTINGS_APP).performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        assertEquals(listOf("tasks", "finance", "app", "back"), clicks)
    }

    @Test
    fun showsExportError() {
        show(SettingsState(export = ExportState.Failed))

        composeRule.onNodeWithText("Не удалось сохранить файл. Попробуйте ещё раз.").assertExists()
    }

    @Test
    fun clearButtonSendsIntent() {
        show(doneState)

        composeRule.onNodeWithTag(TAG_CLEAR_BUTTON).performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.ClearRequested), intents)
    }

    @Test
    fun confirmationDialogConfirmsAndCancels() {
        show(doneState.copy(showClearConfirmation = true))

        composeRule.onNodeWithText("Удалить все данные?").assertExists()
        composeRule.onNodeWithText("Удалить").performClick()
        composeRule.onNodeWithText("Отмена").performClick()

        assertEquals(listOf(SettingsIntent.ClearConfirmed, SettingsIntent.ClearDismissed), intents)
    }

    @Test
    fun buttonsAreDisabledDuringImport() {
        show(SettingsState(stats = DataStats(10, 5, 2), import = ImportState.InProgress(1200, 3399)))

        composeRule.onNodeWithText("Импорт: 1200 из 3399").assertExists()
        composeRule.onNodeWithTag(TAG_IMPORT_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_EXPORT_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_CLEAR_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun showsFormatError() {
        show(SettingsState(import = ImportState.Failed(ImportFailure.NOT_TICKTICK_FILE)))

        composeRule.onNodeWithText("Это не резервная копия TickTick. Выберите файл .csv, созданный в TickTick.").assertExists()
    }

    @Test
    fun screenshotLight() {
        show(doneState)
        composeRule.onRoot().captureRoboImage("screenshots/settings_import_done_light.png")
    }

    @Test
    fun screenshotDark() {
        show(doneState, darkTheme = true)
        composeRule.onRoot().captureRoboImage("screenshots/settings_import_done_dark.png")
    }

    @Test
    fun screenshotClearDialog() {
        show(doneState.copy(showClearConfirmation = true))
        composeRule.onRoot().captureRoboImage("screenshots/settings_clear_dialog_light.png")
    }

    /** «Дата и время в названии» (итерация 41): включён по умолчанию, нажатие выключает. */
    @Test
    fun smartDatesSwitch() {
        show(doneState)
        composeRule.onNodeWithText("Дата и время в названии").assertExists()
        composeRule.onNodeWithTag(TAG_SMART_DATES_SWITCH).assertIsOn().performClick()
        assertEquals(SettingsIntent.SetSmartDates(false), intents.last())
    }
}
