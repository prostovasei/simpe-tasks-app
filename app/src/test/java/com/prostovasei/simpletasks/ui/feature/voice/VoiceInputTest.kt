package com.prostovasei.simpletasks.ui.feature.voice

import android.Manifest
import android.app.Application
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

/** #95: окно голосового ввода — состояния, кнопки и работа с распознавателем (подменён). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class VoiceInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: VoiceState) = composeRule.setContent {
        AppTheme { VoiceSheet(state, onCancel = { clicks += "cancel" }, onDone = { clicks += "done" }, onRetry = { clicks += "retry" }) }
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
        assertEquals(0f, VoiceErrors.level(-5f))
        assertEquals(1f, VoiceErrors.level(15f))
    }

    @Test
    fun listeningWaitsForSpeech() {
        show(VoiceState())
        composeRule.onNodeWithText("Говорите").assertExists()
        composeRule.onNodeWithText("Например, «Позвонить маме завтра в 10»").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_listening_light.png")
        composeRule.onNodeWithTag(TAG_VOICE_CANCEL).performClick()
        assertEquals(listOf("cancel"), clicks)
    }

    @Test
    fun partialTextAndDone() {
        show(VoiceState(heard = "Позвонить маме завтра", tail = "в 10", level = 0.6f))
        composeRule.onNodeWithText("Слушаю…").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).assertTextEquals("Позвонить маме завтра в 10")
        composeRule.onRoot().captureRoboImage("screenshots/voice_partial_light.png")
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(listOf("done"), clicks)
    }

    @Test
    fun errorsOfferRetryOrSettings() {
        var state by mutableStateOf(VoiceState(error = VoiceError.NOT_HEARD))
        composeRule.setContent { AppTheme { VoiceSheet(state, { clicks += "cancel" }, { clicks += "done" }, { clicks += "retry" }) } }
        composeRule.onNodeWithText("Не расслышал").assertExists()
        composeRule.onNodeWithText("Повторить").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_not_heard_light.png")
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()

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
        assertEquals(listOf("retry"), clicks)
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

    @Test
    fun routeStreamsTextAndReturnsResult() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val engine = FakeEngine()
        val results = mutableListOf<String>()
        composeRule.setContent { AppTheme { VoiceRoute(onClose = { clicks += "close" }, onResult = { results += it }, engineFactory = { engine }) } }
        composeRule.waitForIdle()
        assertEquals(1, engine.starts)

        composeRule.runOnIdle { engine.listener!!.onPartial("Купить молоко") }
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).assertTextEquals("Купить молоко")
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(1, engine.stops)

        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.onNodeWithText("Не расслышал").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        composeRule.waitForIdle()
        assertEquals("«Повторить» запускает запись заново", 2, engine.starts)

        composeRule.runOnIdle { engine.listener!!.onResult("  Купить молоко завтра ") }
        assertEquals(listOf("Купить молоко завтра"), results)
    }
}
