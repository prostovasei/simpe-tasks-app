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
import androidx.compose.ui.test.onNodeWithTag
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

    /** Снимки делаются на нулевом кадре анимации «Дыхание» — иначе они были бы разными от запуска к запуску. */
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
        assertEquals(VoiceError.BUSY, VoiceErrors.from(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(VoiceError.UNAVAILABLE, VoiceErrors.from(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(VoiceError.UNAVAILABLE, VoiceErrors.from(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals("Позвонить маме завтра" to "в", VoiceErrors.split("Позвонить маме завтра в "))
        assertEquals("" to "Позвонить", VoiceErrors.split("Позвонить"))
        assertEquals("Позвонить маме завтра в 10", VoiceErrors.clean("  Позвонить маме завтра в 10. "))
        assertEquals(0f, VoiceErrors.level(-5f))
        assertEquals(1f, VoiceErrors.level(15f))
    }

    @Test
    fun listeningWaitsForSpeech() {
        show(VoiceState(recording = true))
        composeRule.onNodeWithText("Говорите").assertExists()
        composeRule.onNodeWithText("Например, «Позвонить маме завтра в 10»").assertExists()
        composeRule.onNodeWithText("Нажмите, чтобы остановить").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_listening_light.png")
        composeRule.onNodeWithTag(TAG_VOICE_CANCEL).performClick()
        assertEquals(listOf("cancel"), clicks)
    }

    @Test
    fun recordingStreamsTextWithCapital() {
        show(VoiceState(text = "", partial = "позвонить маме завтра в 10", recording = true, level = 0.6f))
        composeRule.onNodeWithText("Слушаю…").assertExists()
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
        composeRule.onNodeWithText("Проверьте текст").assertExists()
        composeRule.onNodeWithText("Нажмите, чтобы продолжить").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_paused_light.png")
    }

    @Test
    fun notHeardAgainKeepsText() {
        show(VoiceState(text = "Позвонить маме завтра в 10 утра", error = VoiceError.NOT_HEARD))
        composeRule.onNodeWithText("Не расслышал — нажмите 🎤 ещё раз").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_not_heard_again_light.png")
    }

    @Test
    fun errorsWithoutTextOfferRetryOrSettings() {
        var state by mutableStateOf(VoiceState(error = VoiceError.NOT_HEARD))
        composeRule.setContent { AppTheme { VoiceSheet(state, { clicks += "cancel" }, { clicks += "done" }, { clicks += "mic" }) } }
        composeRule.onNodeWithText("Не расслышал").assertExists()
        composeRule.onNodeWithText("Повторить").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_not_heard_light.png")

        state = VoiceState(error = VoiceError.NO_PERMISSION)
        composeRule.onNodeWithText("Нужен доступ к микрофону").assertExists()
        composeRule.onNodeWithText("Открыть настройки").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_no_permission_light.png")

        state = VoiceState(error = VoiceError.UNAVAILABLE)
        composeRule.onNodeWithText("Распознавание недоступно").assertExists()
        composeRule.onNodeWithText("Закрыть").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_unavailable_light.png")

        state = VoiceState(error = VoiceError.BUSY)
        composeRule.onNodeWithText("Микрофон занят другим приложением. Попробуйте ещё раз.").assertExists()
        // Нажатие — после всех снимков: след нажатия на кнопке делал снимки нестабильными.
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(listOf("mic"), clicks)
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
        composeRule.onNodeWithText("Проверьте текст").assertExists()
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
        assertEquals("🎤 после паузы запускает запись заново", 2, engine.starts)
        composeRule.onNodeWithText("Слушаю…").assertExists()
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
        assertEquals("Во время записи 🎤 останавливает её", 1, engine.stops)

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
        composeRule.onNodeWithText("Не расслышал").assertExists()
        composeRule.onNodeWithText("Повторить").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        composeRule.waitForIdle()
        assertEquals("«Повторить» запускает запись заново", 2, engine.starts)

        composeRule.runOnIdle { engine.listener!!.onResult("купить хлеб") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Не расслышал — нажмите 🎤 ещё раз").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_FIELD).assertTextEquals("Купить хлеб")
        assertEquals(emptyList<String>(), results)
    }
}
