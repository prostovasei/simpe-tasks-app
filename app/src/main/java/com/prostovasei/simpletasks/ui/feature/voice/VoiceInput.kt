package com.prostovasei.simpletasks.ui.feature.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.lerp
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Почему не получилось (#95): по ним выбираются текст, значок и кнопки окна. */
enum class VoiceError { NOT_HEARD, NO_PERMISSION, UNAVAILABLE, BUSY }

/**
 * Состояние окна голосового ввода. [heard] — уже распознанная часть, [tail] — слова, которые движок ещё уточняет (серым);
 * [level] — громкость 0..1 для круга вокруг микрофона; [error] — окно ошибки вместо записи.
 */
data class VoiceState(val heard: String = "", val tail: String = "", val level: Float = 0f, val error: VoiceError? = null) {
    val text: String get() = listOf(heard, tail).filter { it.isNotBlank() }.joinToString(" ").trim()
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

/**
 * Голосовой ввод из виджета (#95, макеты design/approved/95): своё окно поверх рабочего стола. При первом запуске — системный
 * запрос микрофона; текст появляется по ходу речи; результат передаётся в [onResult] — дальше открывается окно новой задачи.
 */
@Composable
fun VoiceRoute(onClose: () -> Unit, onResult: (String) -> Unit, engineFactory: (Context) -> VoiceEngine = ::AndroidVoiceEngine) {
    val context = LocalContext.current
    val engine = remember { engineFactory(context.applicationContext) }
    val deliver = onResult
    var state by remember { mutableStateOf(VoiceState()) }
    var attempt by remember { mutableStateOf(0) }
    var allowed by remember { mutableStateOf(granted(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        if (!ok) state = VoiceState(error = VoiceError.NO_PERMISSION)
    }
    LaunchedEffect(Unit) { if (!allowed) permission.launch(Manifest.permission.RECORD_AUDIO) }
    LaunchedEffect(allowed, attempt) {
        if (!allowed) return@LaunchedEffect
        state = VoiceState()
        engine.start(object : VoiceEngine.Listener {
            override fun onPartial(text: String) {
                val (heard, tail) = VoiceErrors.split(text)
                state = state.copy(heard = heard, tail = tail)
            }
            override fun onLevel(level: Float) { state = state.copy(level = level) }
            override fun onResult(text: String) = deliver(VoiceErrors.clean(text))
            override fun onError(error: VoiceError) { state = VoiceState(error = error) }
        })
    }
    DisposableEffect(engine) { onDispose { engine.destroy() } }
    BackHandler(onBack = onClose)
    VoiceSheet(
        state = state,
        onCancel = onClose,
        onDone = engine::stop,
        onRetry = {
            if (state.error == VoiceError.NO_PERMISSION) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                onClose()
            } else {
                attempt++
            }
        },
    )
}

private fun granted(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Окно: затемнение, снизу белая карточка — подпись, текст, микрофон с кругом громкости, «Отмена» и главная кнопка. */
@Composable
fun VoiceSheet(state: VoiceState, onCancel: () -> Unit, onDone: () -> Unit, onRetry: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val error = state.error
    Box(
        Modifier.fillMaxSize().background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
            .testTag(TAG_VOICE),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(spacing.xs)
                .fillMaxWidth()
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                // Нажатия по карточке не закрывают окно.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(start = spacing.md, end = spacing.md, top = spacing.md, bottom = spacing.sm),
        ) {
            Text(
                stringResource(
                    when {
                        error != null -> R.string.voice_title
                        state.text.isEmpty() -> R.string.voice_speak
                        else -> R.string.voice_listening
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
            )
            val big = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            Column(Modifier.heightIn(min = sizes.voiceTextMin).padding(top = spacing.xs)) {
                when {
                    error != null -> {
                        Text(stringResource(errorTitle(error)), style = big, color = colors.textPrimary, modifier = Modifier.testTag(TAG_VOICE_TEXT))
                        Text(
                            stringResource(errorHint(error)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.xxs),
                        )
                    }
                    state.text.isEmpty() -> Text(stringResource(R.string.voice_example), style = big, color = colors.inactive, modifier = Modifier.testTag(TAG_VOICE_TEXT))
                    else -> Text(
                        buildAnnotatedString {
                            append(state.heard)
                            if (state.heard.isNotEmpty() && state.tail.isNotEmpty()) append(' ')
                            withStyle(SpanStyle(color = colors.inactive)) { append(state.tail) }
                        },
                        style = big,
                        color = colors.textPrimary,
                        modifier = Modifier.testTag(TAG_VOICE_TEXT),
                    )
                }
            }
            MicArea(state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (error == VoiceError.UNAVAILABLE || error == VoiceError.BUSY) R.string.voice_close else R.string.voice_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(AppTheme.shapes.large)
                        .clickable(role = Role.Button, onClick = onCancel)
                        .padding(horizontal = spacing.xs, vertical = spacing.sm)
                        .testTag(TAG_VOICE_CANCEL),
                )
                val enabled = error != null || state.text.isNotEmpty()
                Text(
                    stringResource(
                        when (error) {
                            null -> R.string.voice_done
                            VoiceError.NO_PERMISSION -> R.string.voice_settings
                            else -> R.string.voice_retry
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onAccent,
                    modifier = Modifier
                        .clip(AppTheme.shapes.large)
                        .background(if (enabled) colors.accent else colors.inactive)
                        .clickable(enabled = enabled, role = Role.Button, onClick = if (error == null) onDone else onRetry)
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                        .testTag(TAG_VOICE_ACTION),
                )
            }
        }
    }
}

@Composable
private fun MicArea(state: VoiceState) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val listening = state.error == null
    val level by animateFloatAsState(if (listening) state.level else 0f, label = "voiceLevel")
    Box(Modifier.fillMaxWidth().height(sizes.voiceMicArea), contentAlignment = Alignment.Center) {
        if (listening) {
            Box(Modifier.size(lerp(sizes.voiceMic + AppTheme.spacing.md, sizes.voiceRingMax, level)).clip(CircleShape).background(colors.accentSoft))
        }
        Box(
            Modifier.size(sizes.voiceMic).clip(CircleShape).background(if (listening) colors.accent else colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            val off = state.error == VoiceError.NO_PERMISSION || state.error == VoiceError.UNAVAILABLE || state.error == VoiceError.BUSY
            Icon(
                painterResource(if (off) R.drawable.ic_voice_mic_off else R.drawable.ic_widget_bar_mic),
                contentDescription = null,
                tint = if (listening) colors.onAccent else colors.inactive,
                modifier = Modifier.size(sizes.voiceMicIcon),
            )
        }
    }
}

private fun errorTitle(error: VoiceError) = when (error) {
    VoiceError.NOT_HEARD -> R.string.voice_not_heard
    VoiceError.NO_PERMISSION -> R.string.voice_no_permission
    VoiceError.UNAVAILABLE -> R.string.voice_unavailable
    VoiceError.BUSY -> R.string.voice_unavailable
}

private fun errorHint(error: VoiceError) = when (error) {
    VoiceError.NOT_HEARD -> R.string.voice_not_heard_hint
    VoiceError.NO_PERMISSION -> R.string.voice_no_permission_hint
    VoiceError.UNAVAILABLE -> R.string.voice_unavailable_hint
    VoiceError.BUSY -> R.string.voice_busy_hint
}

const val TAG_VOICE = "voice"
const val TAG_VOICE_TEXT = "voice_text"
const val TAG_VOICE_CANCEL = "voice_cancel"
const val TAG_VOICE_ACTION = "voice_action"
