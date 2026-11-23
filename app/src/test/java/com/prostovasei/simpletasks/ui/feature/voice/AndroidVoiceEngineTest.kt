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
    private val logs = mutableListOf<String>()
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
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
        assertEquals(listOf("error:UNAVAILABLE"), events)
    }

    @Test
    fun requestsRussianWithPartialResults() {
        installRecognizer()
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
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
        val engine = AndroidVoiceEngine(app, log = { logs += it })
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
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
        idle()
        shadow().triggerOnPartialResults(results("Купить молоко"))
        shadow().triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(listOf("partial:Купить молоко", "result:Купить молоко"), events)
    }

    @Test
    fun silenceAndNetworkErrors() {
        installRecognizer()
        val engine = AndroidVoiceEngine(app, log = { logs += it })
        engine.start(listener)
        idle()
        shadow().triggerOnError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        engine.start(listener)
        idle()
        shadow().triggerOnError(SpeechRecognizer.ERROR_NETWORK)
        engine.start(listener)
        idle()
        shadow().triggerOnResults(results(""))
        assertEquals(listOf("error:NOT_HEARD", "error:NO_NETWORK", "error:NOT_HEARD"), events)
        assertEquals("Каждый код ошибки — в журнале", listOf("голос: ошибка 6", "голос: ошибка 2"), logs)
    }

    /** #302: повторный запуск — тот же распознаватель, без уничтожения и пересоздания. */
    @Test
    fun restartReusesRecognizer() {
        installRecognizer()
        val engine = AndroidVoiceEngine(app, log = { logs += it })
        engine.start(listener)
        idle()
        val first = ShadowSpeechRecognizer.getLatestSpeechRecognizer()
        shadow().triggerOnResults(results("позвонить маме"))
        engine.start(listener)
        idle()
        assertTrue(first === ShadowSpeechRecognizer.getLatestSpeechRecognizer())
        assertTrue(!shadowOf(first).isDestroyed)
        shadow().triggerOnResults(results("завтра"))
        assertEquals(listOf("result:позвонить маме", "result:завтра"), events)
    }

    /** #302: временный сбой до первых слов — тихий повтор через 0,5 с с новым распознавателем; второй сбой — «не удалось начать». */
    @Test
    fun transientErrorRetriesOnceSilently() {
        installRecognizer()
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
        idle()
        val first = ShadowSpeechRecognizer.getLatestSpeechRecognizer()
        shadow().triggerOnError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals("Первый сбой не показывается", emptyList<String>(), events)
        assertTrue(shadowOf(first).isDestroyed)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(AndroidVoiceEngine.RETRY_MILLIS))
        assertTrue("Повтор с новым распознавателем", first !== ShadowSpeechRecognizer.getLatestSpeechRecognizer())
        shadow().triggerOnPartialResults(results("купить"))
        shadow().triggerOnResults(results("купить хлеб"))
        assertEquals(listOf("partial:купить", "result:купить хлеб"), events)
        assertEquals(listOf("голос: ошибка 5"), logs)
    }

    @Test
    fun secondTransientErrorIsShown() {
        installRecognizer()
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
        idle()
        shadow().triggerOnError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(AndroidVoiceEngine.RETRY_MILLIS))
        shadow().triggerOnError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals(listOf("error:START_FAILED"), events)
        assertEquals(listOf("голос: ошибка 11", "голос: ошибка 5 (после повтора)"), logs)
    }

    @Test
    fun transientErrorAfterSpeechIsNotRetried() {
        installRecognizer()
        AndroidVoiceEngine(app, log = { logs += it }).start(listener)
        idle()
        shadow().triggerOnPartialResults(results("купить"))
        shadow().triggerOnError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals("После первых слов не повторяем — сразу сообщение", listOf("partial:купить", "error:START_FAILED"), events)
    }
}
