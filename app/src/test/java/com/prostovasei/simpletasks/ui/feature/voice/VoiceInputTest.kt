package com.prostovasei.simpletasks.ui.feature.voice

import android.Manifest
import android.app.Application
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** #95, #299: окно голосового ввода — состояния, правка текста, продолжение диктовки и передача в задачу по «Готово». */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class VoiceInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    /** Снимки делаются на нулевом кадре анимации «Прилив» — иначе они были бы разными от запуска к запуску. */
    private fun show(state: VoiceState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme { VoiceSheet(state, onCancel = { clicks += "cancel" }, onDone = { clicks += "done" }, onMic = { clicks += "mic" }) }
        }
    }

    @Test
    fun capitalizationJoinAndResult() {
        assertEquals("Позвонить маме", VoiceErrors.capitalize("позвонить маме"))
        assertEquals("Позвонить маме", VoiceErrors.capitalize("  позвонить маме"))
        assertEquals("Позвонить маме завтра в 10", VoiceErrors.join("Позвонить маме", "завтра в 10 "))
        assertEquals("Купить хлеб", VoiceErrors.join("", " Купить хлеб"))
        assertEquals("Купить хлеб", VoiceErrors.join("Купить хлеб", "  "))
        assertEquals("Позвонить маме завтра в 10", VoiceErrors.result("позвонить маме завтра в 10."))
        assertEquals("Первая буква заглавная и в предварительном тексте", "Позвонить маме завтра", VoiceState(partial = "позвонить маме завтра").display)
        assertEquals("Позвонить маме завтра в 10", VoiceState(text = "Позвонить маме", partial = "завтра в 10").display)
        assertEquals(false, VoiceState().canDone)
    }

    @Test
    fun errorCodesAndPartialText() {
        assertEquals(VoiceError.NOT_HEARD, VoiceErrors.from(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(VoiceError.NOT_HEARD, VoiceErrors.from(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(VoiceError.NO_PERMISSION, VoiceErrors.from(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(VoiceError.BUSY, VoiceErrors.from(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(VoiceError.NO_NETWORK, VoiceErrors.from(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(VoiceError.NO_NETWORK, VoiceErrors.from(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(VoiceError.NO_LANGUAGE, VoiceErrors.from(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(VoiceError.NO_LANGUAGE, VoiceErrors.from(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        // #302: временные сбои сервиса больше не выдаются за «Распознавание недоступно».
        assertEquals(VoiceError.START_FAILED, VoiceErrors.from(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(VoiceError.START_FAILED, VoiceErrors.from(SpeechRecognizer.ERROR_SERVER_DISCONNECTED))
        assertEquals(VoiceError.START_FAILED, VoiceErrors.from(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals("Позвонить маме завтра" to "в", VoiceErrors.split("Позвонить маме завтра в "))
        assertEquals("" to "Позвонить", VoiceErrors.split("Позвонить"))
        assertEquals("Позвонить маме завтра в 10", VoiceErrors.clean("  Позвонить маме завтра в 10. "))
        assertEquals(0f, VoiceErrors.level(-5f))
        assertEquals(1f, VoiceErrors.level(15f))
    }

    @Test
    fun listeningWaitsForSpeech() {
        show(VoiceState(recording = true))
        // #305: в окне нет подписей — только текст, вода и две кнопки.
        composeRule.onNodeWithText("Говорите").assertDoesNotExist()
        composeRule.onNodeWithText("Отмена").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Пауза").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_listening_light.png")
        composeRule.onNodeWithTag(TAG_VOICE).performTouchInput { click(Offset(10f, 10f)) }
        assertEquals("Нажатие на затемнение закрывает окно", listOf("cancel"), clicks)
    }

    @Test
    fun recordingStreamsTextWithCapital() {
        show(VoiceState(text = "", partial = "позвонить маме завтра в 10", recording = true, level = 0.6f))
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).assertTextEquals("Позвонить маме завтра в 10")
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_partial_light.png")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(listOf("mic", "done"), clicks)
    }

    @Test
    fun pauseKeepsTextInEditableField() {
        show(VoiceState(text = "Позвонить маме завтра в 10"))
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onRoot().captureRoboImage("screenshots/voice_paused_light.png")
    }

    @Test
    fun notHeardAgainKeepsText() {
        show(VoiceState(text = "Позвонить маме завтра в 10 утра", error = VoiceError.NOT_HEARD))
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не расслышал — нажмите ●")
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_not_heard_again_light.png")
    }

    @Test
    fun errorsShowBetweenButtons() {
        var state by mutableStateOf(VoiceState(error = VoiceError.NOT_HEARD))
        composeRule.setContent {
            AppTheme { VoiceSheet(state, { clicks += "cancel" }, { clicks += "done" }, { clicks += "mic" }, onSettings = { clicks += "settings" }) }
        }
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не расслышал — нажмите ●")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).assertIsEnabled()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_not_heard_light.png")

        state = VoiceState(error = VoiceError.NO_PERMISSION)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Нет доступа к микрофону · Настройки")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_no_permission_light.png")

        state = VoiceState(error = VoiceError.UNAVAILABLE)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Распознавание недоступно")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_unavailable_light.png")

        state = VoiceState(error = VoiceError.START_FAILED)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не удалось начать запись")
        composeRule.onRoot().captureRoboImage("screenshots/voice_start_failed_light.png")

        state = VoiceState(error = VoiceError.NO_NETWORK)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Нет интернета для распознавания")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).assertIsEnabled()

        state = VoiceState(error = VoiceError.BUSY)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Микрофон занят")
        // Нажатия — после всех снимков: след нажатия на кнопке делал снимки нестабильными.
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        state = VoiceState(error = VoiceError.NO_PERMISSION)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).performClick()
        assertEquals(listOf("mic", "settings"), clicks)
    }

    /** Распознаватель-заглушка: запоминает запуски, события передаёт тест. */
    private class FakeEngine : VoiceEngine {
        var listener: VoiceEngine.Listener? = null
        var starts = 0
        var stops = 0
        var destroyed = false
        override fun start(listener: VoiceEngine.Listener) { this.listener = listener; starts++ }
        override fun stop() { stops++ }
        override fun destroy() { destroyed = true }
    }

    private fun route(engine: FakeEngine, results: MutableList<String>) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        composeRule.setContent { AppTheme { VoiceRoute(onClose = { clicks += "close" }, onResult = { results += it }, engineFactory = { engine }) } }
        composeRule.waitForIdle()
    }

    @Test
    fun pauseDoesNotSendTextToTask() {
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        route(engine, results)
        assertEquals(1, engine.starts)

        composeRule.runOnIdle { engine.listener!!.onPartial("позвонить маме") }
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).assertTextEquals("Позвонить маме")
        composeRule.runOnIdle { engine.listener!!.onResult("позвонить маме завтра в 10.") }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertTextEquals("Позвонить маме завтра в 10")
        assertEquals("После паузы в задачу ничего не уходит", emptyList<String>(), results)
    }

    @Test
    fun editContinueAndDone() {
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        route(engine, results)
        composeRule.runOnIdle { engine.listener!!.onResult("позвонить маме") }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_VOICE_FIELD).performTextInput(" завтра")
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertTextEquals("Позвонить маме завтра")

        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        assertEquals("⏺ после паузы запускает запись заново", 2, engine.starts)
        composeRule.onNodeWithContentDescription("Пауза").assertExists()
        composeRule.runOnIdle { engine.listener!!.onResult("в 10 утра") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertTextEquals("Позвонить маме завтра в 10 утра")

        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(listOf("Позвонить маме завтра в 10 утра"), results)
        assertEquals(true, engine.destroyed)
    }

    /** Проверка #299: одно нажатие на текст во время записи — остановка и сразу клавиатура в поле, без второго нажатия. */
    @Test
    fun tapOnTextWhileRecordingStopsAndFocusesField() {
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        route(engine, results)
        composeRule.runOnIdle { engine.listener!!.onPartial("купить молоко") }
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).performClick()
        assertEquals(1, engine.stops)
        composeRule.runOnIdle { engine.listener!!.onResult("купить молоко") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertIsFocused().assertTextEquals("Купить молоко")
        assertEquals(emptyList<String>(), results)
    }

    @Test
    fun micStopsRecordingAndDoneWorksWhileRecording() {
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        route(engine, results)
        composeRule.runOnIdle { engine.listener!!.onPartial("купить молоко") }
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        assertEquals("Во время записи ⏸ останавливает её", 1, engine.stops)

        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals("«Готово» во время записи берёт текущий текст", listOf("Купить молоко"), results)
    }

    @Test
    fun errorKeepsTypedTextAndWithoutTextShowsErrorWindow() {
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        route(engine, results)
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не расслышал — нажмите ●")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        assertEquals("⏺ после ошибки запускает запись заново", 2, engine.starts)

        composeRule.runOnIdle { engine.listener!!.onResult("купить хлеб") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не расслышал — нажмите ●")
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertTextEquals("Купить хлеб")
        assertEquals(emptyList<String>(), results)
    }
}
