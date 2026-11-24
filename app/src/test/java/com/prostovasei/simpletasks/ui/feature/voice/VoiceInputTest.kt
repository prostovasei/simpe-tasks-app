package com.prostovasei.simpletasks.ui.feature.voice

import android.Manifest
import android.app.Application
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** #95, #299, #305, #308, #310: окно голосового ввода — состояния, непрерывная запись, задачи по фразам, правка, сохранение. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class VoiceInputTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    private val zone: ZoneId = ZoneId.systemDefault()

    /** «Сейчас» в тестах — вт, 24 ноя 2026, 09:00. */
    private val now = ZonedDateTime.of(2026, 11, 24, 9, 0, 0, 0, zone)

    private fun task(id: Long, phrase: String, today: Boolean = false) =
        VoiceTasks.parse(id, phrase, now.toLocalDate(), now.hour * 60 + now.minute, zone, smart = true, defaultToday = today)

    /** Снимки делаются на нулевом кадре анимации «Прилив» — иначе они были бы разными от запуска к запуску. */
    private fun show(state: VoiceState, tasks: List<VoiceTask> = emptyList()) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                VoiceSheet(
                    state, onCancel = { clicks += "cancel" }, onDone = { clicks += "done" }, onMic = { clicks += "mic" },
                    days = VoiceTasks.days(tasks, zone), count = tasks.size,
                    onOpen = { t, _ -> clicks += "open:${t.id}" }, onRemove = { clicks += "remove:${it.id}" },
                )
            }
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

    /** Фраза → задача: срок из фразы, название без него; без срока — по умолчанию; разбор выключен — как сказано. */
    @Test
    fun phraseBecomesTask() {
        val mom = task(1, "позвонить маме завтра в 10")
        assertEquals("Позвонить маме", mom.title)
        assertEquals("Позвонить маме завтра в 10", mom.phrase)
        assertEquals(TaskDue.AtTime(now.plusDays(1).withHour(10).toInstant()), mom.due)
        assertEquals("10:00", mom.time(zone))
        assertNull(task(2, "записаться к стоматологу").due)
        assertEquals("Вид «Все» — на сегодня", TaskDue.AllDay(now.toLocalDate()), task(3, "Купить хлеб", today = true).due)
        val off = VoiceTasks.parse(4, "созвон завтра", now.toLocalDate(), 540, zone, smart = false, defaultToday = false)
        assertEquals("Созвон завтра", off.title)
        assertNull(off.due)
    }

    /** Дни по порядку, «Без даты» в конце; в дне — сначала без времени, потом по времени. */
    @Test
    fun daysAreSortedNoDateLast() {
        val days = VoiceTasks.days(
            listOf(task(1, "Хакатон 15 декабря"), task(2, "Стоматолог"), task(3, "Позвонить маме завтра в 10"), task(4, "Зал завтра в 8"), task(5, "Купить продукты сегодня"), task(6, "Отчёт завтра")),
            zone,
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 15), null), days.map { it.date })
        assertEquals(listOf(6L, 4L, 3L), days[1].tasks.map { it.id })
    }

    @Test
    fun listeningWaitsForSpeech() {
        show(VoiceState(recording = true))
        // #305: в окне нет подписей — только текст, вода и две кнопки.
        composeRule.onNodeWithText("Говорите").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Пауза").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_VOICE_COUNT, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onRoot().captureRoboImage("screenshots/voice_listening_light.png")
        composeRule.onNodeWithTag(TAG_VOICE).performTouchInput { click(Offset(10f, 10f)) }
        assertEquals("Нажатие на затемнение — закрыть", listOf("cancel"), clicks)
    }

    @Test
    fun recordingStreamsPhrase() {
        show(VoiceState(partial = "позвонить маме завтра в 10", recording = true, level = 0.6f))
        composeRule.onNodeWithTag(TAG_VOICE_TEXT, useUnmergedTree = true).assertTextEquals("Позвонить маме завтра в 10")
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).assertIsEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/voice_partial_light.png")
    }

    /** #310: задачи по дням над окном, без кружка, время справа, «×»; на галочке — число задач (просьба PM). */
    @Test
    fun tasksByDaysWithCount() {
        val tasks = listOf(task(1, "Купить продукты сегодня"), task(2, "Провести хакатон 15 декабря"), task(3, "Позвонить маме завтра в 10"))
        show(VoiceState(partial = "записаться к", recording = true, level = 0.4f), tasks)
        composeRule.onNodeWithText("вт, 24 ноя").assertExists()
        composeRule.onNodeWithText("ср, 25 ноя").assertExists()
        composeRule.onNodeWithText("вт, 15 дек").assertExists()
        composeRule.onNodeWithText("10:00", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_COUNT, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Сохранить 3 задачи").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_tasks_light.png")
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("$TAG_VOICE_ROW:3").performClick()
        composeRule.onNodeWithTag("$TAG_VOICE_REMOVE:2").performClick()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        assertEquals(listOf("open:3", "remove:2", "done"), clicks)
    }

    @Test
    fun pausedWithTasks() {
        show(VoiceState(), listOf(task(1, "Купить продукты сегодня"), task(2, "Записаться к стоматологу")))
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
        composeRule.onNodeWithText("Без даты").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/voice_paused_light.png")
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

    @Test
    fun darkTheme() {
        composeRule.mainClock.autoAdvance = false
        val tasks = listOf(task(1, "Купить продукты сегодня"), task(2, "Позвонить маме завтра в 10"))
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                VoiceSheet(VoiceState(partial = "провести хакатон", recording = true, level = 0.6f), {}, {}, {}, days = VoiceTasks.days(tasks, zone), count = tasks.size)
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/voice_partial_dark.png")
    }

    /** Крупный шрифт (как на телефоне PM): список, ошибка и число на галочке не наезжают друг на друга. */
    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi", fontScale = 1.3f)
    fun largeFontError() {
        show(VoiceState(error = VoiceError.NO_NETWORK), listOf(task(1, "Позвонить маме завтра в 10"), task(2, "Купить подарок Ане на день рождения в субботу")))
        composeRule.onRoot().captureRoboImage("screenshots/voice_error_large_font.png")
    }

    /** Распознаватель-заглушка: запоминает запуски, события передаёт тест. */
    private class FakeEngine : VoiceEngine {
        var listener: VoiceEngine.Listener? = null
        var starts = 0
        var stops = 0
        var cancels = 0
        var destroys = 0
        override fun start(listener: VoiceEngine.Listener) { this.listener = listener; starts++ }
        override fun stop() { stops++ }
        override fun cancel() { cancels++ }
        override fun destroy() { destroys++ }
    }

    private val saved = mutableListOf<List<VoiceTask>>()
    private val edits = mutableListOf<VoiceTask>()

    private fun route(engine: FakeEngine, today: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        composeRule.setContent {
            AppTheme {
                VoiceRoute(
                    onClose = { clicks += "close" }, onSaveAll = { saved += it }, onEdit = { t, _ -> edits += t },
                    today = today, clock = { now }, engineFactory = { engine },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun say(engine: FakeEngine, phrase: String) {
        composeRule.runOnIdle { engine.listener!!.onPartial(phrase) }
        composeRule.runOnIdle { engine.listener!!.onResult(phrase) }
        composeRule.waitForIdle()
    }

    private fun row(title: String) = composeRule.onNode(hasText(title), useUnmergedTree = true)

    /** #310 + #308: каждая фраза — задача в списке, запись не останавливается; ⏸ — пауза без перезапуска. */
    @Test
    fun phrasesBecomeTasksWhileListening() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить продукты сегодня")
        say(engine, "позвонить маме завтра в 10")
        assertEquals("После каждой фразы запись продолжается", 3, engine.starts)
        row("Купить продукты").assertExists()
        row("Позвонить маме").assertExists()
        composeRule.onNodeWithText("вт, 24 ноя").assertExists()
        composeRule.onNodeWithText("ср, 25 ноя").assertExists()
        composeRule.onNodeWithContentDescription("Сохранить 2 задачи").assertExists()

        // Тишина до 10 с — не ошибка: запись перезапускается, «Не расслышал» нет.
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.waitForIdle()
        assertEquals(4, engine.starts)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        assertEquals("⏸ останавливает запись", 1, engine.stops)
        composeRule.runOnIdle { engine.listener!!.onResult("записаться к стоматологу") }
        composeRule.waitForIdle()
        assertEquals("После ⏸ запись не перезапускается", 4, engine.starts)
        row("Записаться к стоматологу").assertExists()
        composeRule.onNodeWithText("Без даты").assertExists()
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
        assertTrue(saved.isEmpty())
    }

    /** Галочка сохраняет все задачи списка и недоговорённую фразу; окно уходит. */
    @Test
    fun doneSavesAllIncludingPartial() {
        val engine = FakeEngine()
        route(engine, today = true)
        say(engine, "позвонить маме завтра в 10")
        composeRule.runOnIdle { engine.listener!!.onPartial("купить хлеб") }
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        composeRule.waitForIdle()
        val all = saved.single()
        assertEquals(listOf("Позвонить маме", "Купить хлеб"), all.map { it.title })
        assertEquals("Вид «Все»: без срока — сегодня", TaskDue.AllDay(now.toLocalDate()), all[1].due)
        assertTrue("Запись оборвана без ответа движка", engine.destroys > 0)
        composeRule.onNodeWithTag(TAG_VOICE).assertDoesNotExist()
    }

    /** Нажатие на задачу — запись на паузу, задача уходит в окно задачи; «×» убирает, «Отмена» возвращает. */
    @Test
    fun openAndRemoveTask() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить продукты сегодня")
        say(engine, "нет это не надо")
        composeRule.onNodeWithTag("$TAG_VOICE_REMOVE:2").performClick()
        composeRule.waitForIdle()
        row("Нет это не надо").assertDoesNotExist()
        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitForIdle()
        row("Нет это не надо").assertExists()

        composeRule.onNodeWithTag("$TAG_VOICE_ROW:1").performClick()
        composeRule.waitForIdle()
        assertEquals("Купить продукты сегодня", edits.single().phrase)
        assertTrue("Запись остановлена", engine.destroys > 0)
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
    }

    /** «Назад» при задачах — вопрос «Сохранить N задач?»; без задач — просто закрыть. */
    @Test
    fun backAsksBeforeLosingTasks() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить продукты сегодня")
        say(engine, "позвонить маме завтра в 10")
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Сохранить 2 задачи?").assertExists()
        composeRule.onNodeWithTag("$TAG_VOICE_ASK:$TAG_ASK_DISCARD").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("close"), clicks)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun backAsksAndSavesAll() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить продукты сегодня")
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Сохранить 1 задачу?").assertExists()
        composeRule.onNodeWithTag("$TAG_VOICE_ASK:$TAG_ASK_SAVE").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Купить продукты"), saved.single().map { it.title })
    }

    @Test
    fun backWithoutTasksCloses() {
        val engine = FakeEngine()
        route(engine)
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(listOf("close"), clicks)
    }

    /** #308, ответ PM: 10 с без речи — пауза сама; задачи остаются, ошибки нет. Без задач — «Не расслышал». */
    @Test
    fun silenceOfTenSecondsPauses() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить хлеб")
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(SILENCE_MILLIS - 1_000)
        assertEquals("До 10 с тишины запись идёт", 0, engine.stops)
        composeRule.mainClock.advanceTimeBy(2_000)
        assertEquals("10 с тишины — пауза", 1, engine.stops)
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals("После автопаузы запись не перезапускается", 2, engine.starts)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertDoesNotExist()
        row("Купить хлеб").assertExists()
    }

    @Test
    fun silenceWithoutTasksShowsNotHeard() {
        val engine = FakeEngine()
        route(engine)
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(SILENCE_MILLIS + 1_000)
        assertEquals(1, engine.stops)
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NOT_HEARD) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Не расслышал — нажмите ●")
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        assertEquals("⏺ после «Не расслышал» запускает запись заново", 2, engine.starts)
    }

    /**
     * #321 (видео PM): ⏸ после продиктованной фразы, когда новая речь ещё не началась, — распознаватель не отвечает.
     * Пауза видна сразу, без ответа за 1,5 с запись обрывается; задачи на месте, перезапуска нет.
     */
    @Test
    fun pauseWorksEvenIfTheRecognizerNeverAnswers() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "сделать двухфакторную аутентификацию на гитхабе")
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, engine.stops)
        composeRule.onNodeWithContentDescription("Продолжить запись").assertExists()
        composeRule.mainClock.advanceTimeBy(STOP_GRACE_MILLIS + 100)
        assertEquals("Без ответа запись оборвана", 1, engine.cancels)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals("Перезапуска нет", 2, engine.starts)
        row("Сделать двухфакторную аутентификацию на гитхабе").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.waitForIdle()
        assertEquals("⏺ снова пишет", 3, engine.starts)
        composeRule.onNodeWithContentDescription("Продолжить запись").assertDoesNotExist()
    }

    /** #321: недоговорённая фраза при ⏸ без ответа — тоже задача; ⏺ раньше ответа — то же и сразу новая запись. */
    @Test
    fun unansweredPauseKeepsThePartialPhrase() {
        val engine = FakeEngine()
        route(engine)
        composeRule.runOnIdle { engine.listener!!.onPartial("купить хлеб") }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick()
        composeRule.mainClock.advanceTimeBy(STOP_GRACE_MILLIS + 100)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        row("Купить хлеб").assertExists()
        assertEquals(1, engine.cancels)

        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick() // ⏺
        composeRule.waitForIdle()
        composeRule.runOnIdle { engine.listener!!.onPartial("позвонить маме") }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick() // ⏸
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag(TAG_VOICE_MIC).performClick() // ⏺ раньше ответа
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        row("Позвонить маме").assertExists()
        assertEquals("Новая запись началась", 3, engine.starts)
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(STOP_GRACE_MILLIS + 100)
        composeRule.mainClock.autoAdvance = true
        assertEquals("Отменённое ожидание не обрывает новую запись", 1, engine.cancels)
    }

    @Test
    fun networkErrorKeepsTasks() {
        val engine = FakeEngine()
        route(engine)
        say(engine, "купить хлеб")
        composeRule.runOnIdle { engine.listener!!.onError(VoiceError.NO_NETWORK) }
        composeRule.waitForIdle()
        assertEquals("Ошибка сети останавливает непрерывную запись", 2, engine.starts)
        composeRule.onNodeWithTag(TAG_VOICE_ERROR, useUnmergedTree = true).assertTextEquals("Нет интернета для распознавания")
        row("Купить хлеб").assertExists()
        composeRule.onNodeWithTag(TAG_VOICE_ACTION).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("Купить хлеб"), saved.single().map { it.title })
    }
}
