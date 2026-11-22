package com.prostovasei.simpletasks.ui.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Почему не получилось (#95): по ним выбираются текст, значок и кнопки окна. */
enum class VoiceError { NOT_HEARD, NO_PERMISSION, UNAVAILABLE, BUSY }

/**
 * Состояние окна голосового ввода (#299). [text] — подтверждённый текст (его правят с клавиатуры), [partial] — фраза,
 * которую распознаватель ещё говорит; [recording] — идёт запись; [level] — громкость 0..1; [error] — последняя ошибка
 * (если текста нет — окно ошибки, если есть — подсказка в подписи).
 */
data class VoiceState(
    val text: String = "",
    val partial: String = "",
    val recording: Boolean = false,
    val level: Float = 0f,
    val error: VoiceError? = null,
) {
    /** Всё, что видно в поле: текст и текущая фраза, первая буква заглавная. */
    val display: String get() = VoiceErrors.capitalize(VoiceErrors.join(text, partial))
    val canDone: Boolean get() = display.isNotBlank()
}

/** Распознавание без Android-зависимостей в логике окна: в тестах подменяется. */
interface VoiceEngine {
    fun start(listener: Listener)
    fun stop()
    fun destroy()

    interface Listener {
        fun onPartial(text: String)
        fun onLevel(level: Float)
        fun onResult(text: String)
        fun onError(error: VoiceError)
    }
}

internal object VoiceErrors {
    /** Коды [SpeechRecognizer] → что показать. Тишина и «ничего не подошло» — «Не расслышал». */
    fun from(code: Int): VoiceError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.NOT_HEARD
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.NO_PERMISSION
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_AUDIO -> VoiceError.BUSY
        else -> VoiceError.UNAVAILABLE
    }

    /**
     * Уточнённый текст и хвост из частичного результата: Google присылает всю фразу целиком, последнее слово ещё может
     * измениться — показываем его серым.
     */
    fun split(partial: String): Pair<String, String> {
        val text = partial.trim()
        val cut = text.lastIndexOf(' ')
        return if (cut < 0) "" to text else text.substring(0, cut) to text.substring(cut + 1)
    }

    /** Итоговый текст: без пробелов по краям и точки в конце, которую иногда ставит распознаватель. */
    fun clean(text: String): String = text.trim().trimEnd('.', '!').trim()

    /** Распознаватель Google возвращает русскую фразу с маленькой буквы (#299): первая буква всегда заглавная. */
    fun capitalize(text: String): String = text.trimStart().replaceFirstChar { it.uppercaseChar() }

    /** Дописывает [add] к [text] через пробел; пустые части пропускаются. */
    fun join(text: String, add: String): String = when {
        add.isBlank() -> text
        text.isBlank() -> add.trim()
        else -> text.trimEnd() + " " + add.trim()
    }

    /** Что передать в задачу: заглавная первая буква, без точки в конце. */
    fun result(text: String): String = capitalize(clean(text))

    /** Громкость в дБ из onRmsChanged (примерно −2..10) → 0..1. */
    fun level(rmsDb: Float): Float = ((rmsDb + RMS_MIN) / RMS_RANGE).coerceIn(0f, 1f)

    private const val RMS_MIN = 2f
    private const val RMS_RANGE = 12f
}

/** Системный распознаватель (обычно Google): русский язык, промежуточные результаты, пауза около 2 с завершает запись. */
class AndroidVoiceEngine(private val context: Context) : VoiceEngine {
    private var recognizer: SpeechRecognizer? = null

    override fun start(listener: VoiceEngine.Listener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError(VoiceError.UNAVAILABLE)
            return
        }
        destroy()
        var partial = ""
        var finished = false
        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = listener.onLevel(VoiceErrors.level(rmsdB))
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                first(partialResults)?.let {
                    partial = it
                    listener.onPartial(it)
                }
            }

            override fun onResults(results: Bundle?) {
                if (finished) return
                finished = true
                val text = first(results)?.takeIf { it.isNotBlank() } ?: partial
                if (text.isBlank()) listener.onError(VoiceError.NOT_HEARD) else listener.onResult(text)
            }

            override fun onError(error: Int) {
                if (finished) return
                finished = true
                // Часть движков после паузы присылает «ничего не подошло», хотя промежуточный текст уже был.
                val kind = VoiceErrors.from(error)
                if (kind == VoiceError.NOT_HEARD && partial.isNotBlank()) listener.onResult(partial) else listener.onError(kind)
            }
        })
        speech.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MILLIS)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName),
        )
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun first(bundle: Bundle?): String? = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private companion object {
        const val LANGUAGE = "ru-RU"
        const val SILENCE_MILLIS = 2000L
    }
}

