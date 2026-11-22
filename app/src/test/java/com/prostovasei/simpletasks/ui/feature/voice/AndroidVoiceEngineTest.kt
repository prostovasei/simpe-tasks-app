package com.prostovasei.simpletasks.ui.feature.voice

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSpeechRecognizer

/** #95: настоящий SpeechRecognizer под Robolectric — параметры запроса и разбор событий распознавателя. */
@RunWith(RobolectricTestRunner::class)
class AndroidVoiceEngineTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<String>()
    private val listener = object : VoiceEngine.Listener {
        override fun onPartial(text: String) { events += "partial:$text" }
        override fun onLevel(level: Float) { events += "level:$level" }
        override fun onResult(text: String) { events += "result:$text" }
        override fun onError(error: VoiceError) { events += "error:$error" }
    }

    @Before
    fun setUp() = ShadowSpeechRecognizer.reset()

    /** Распознаватель Google на телефоне: сервис с действием android.speech.RecognitionService. */
    private fun installRecognizer() {
        val component = ComponentName("com.google.android.tts", "com.google.android.tts.service.GoogleRecognitionService")
        val pm = shadowOf(app.packageManager)
        pm.addServiceIfNotPresent(component)
        pm.addIntentFilterForService(component, IntentFilter(RecognitionService.SERVICE_INTERFACE))
    }

    private fun results(text: String) = Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun shadow() = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

    @Test
    fun noRecognizerMeansUnavailable() {
        AndroidVoiceEngine(app).start(listener)
        assertEquals(listOf("error:UNAVAILABLE"), events)
    }

    @Test
    fun requestsRussianWithPartialResults() {
        installRecognizer()
        AndroidVoiceEngine(app).start(listener)
        idle()
        val intent: Intent = shadow().lastRecognizerIntent
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("ru-RU", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals(2000L, intent.getLongExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 0))
    }

    @Test
    fun streamsTextAndFinishesWithResult() {
        installRecognizer()
        val engine = AndroidVoiceEngine(app)
        engine.start(listener)
        idle()
        shadow().triggerOnRmsChanged(10f)
        shadow().triggerOnPartialResults(results("Позвонить маме"))
        shadow().triggerOnResults(results("Позвонить маме завтра в 10"))
        shadow().triggerOnError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals(
            "После результата поздняя ошибка движка не показывается",
            listOf("level:1.0", "partial:Позвонить маме", "result:Позвонить маме завтра в 10"),
            events,
        )
        val recognizer = shadow()
        engine.destroy()
        assertTrue(recognizer.isDestroyed)
    }

    @Test
    fun noMatchAfterPartialKeepsPartialText() {
        installRecognizer()
        AndroidVoiceEngine(app).start(listener)
        idle()
        shadow().triggerOnPartialResults(results("Купить молоко"))
        shadow().triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(listOf("partial:Купить молоко", "result:Купить молоко"), events)
    }

    @Test
    fun silenceAndNetworkErrors() {
        installRecognizer()
        val engine = AndroidVoiceEngine(app)
        engine.start(listener)
        idle()
        shadow().triggerOnError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        engine.start(listener)
        idle()
        shadow().triggerOnError(SpeechRecognizer.ERROR_NETWORK)
        engine.start(listener)
        idle()
        shadow().triggerOnResults(results(""))
        assertEquals(listOf("error:NOT_HEARD", "error:UNAVAILABLE", "error:NOT_HEARD"), events)
    }
}
