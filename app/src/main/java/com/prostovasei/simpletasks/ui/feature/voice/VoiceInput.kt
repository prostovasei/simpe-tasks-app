package com.prostovasei.simpletasks.ui.feature.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/**
 * Голосовой ввод из виджета (#95, #299; макеты design/approved/299): своё окно поверх рабочего стола. Запись начинается сразу;
 * после паузы она останавливается, а текст остаётся в окне — его правят с клавиатуры, 🎤 дописывает продолжение,
 * «Готово» передаёт текст в [onResult] — дальше открывается окно новой задачи. «Отмена» и «Назад» закрывают без задачи.
 */
@Composable
fun VoiceRoute(onClose: () -> Unit, onResult: (String) -> Unit, engineFactory: (Context) -> VoiceEngine = ::AndroidVoiceEngine) {
    val context = LocalContext.current
    val engine = remember { engineFactory(context.applicationContext) }
    var state by remember { mutableStateOf(VoiceState()) }
    var attempt by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(granted(context)) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        if (!ok) state = VoiceState(error = VoiceError.NO_PERMISSION)
    }
    LaunchedEffect(Unit) { if (!allowed) permission.launch(Manifest.permission.RECORD_AUDIO) }
    LaunchedEffect(allowed, attempt) {
        if (!allowed) return@LaunchedEffect
        state = state.copy(partial = "", recording = true, error = null, level = 0f)
        engine.start(object : VoiceEngine.Listener {
            override fun onPartial(text: String) { state = state.copy(partial = text) }
            override fun onLevel(level: Float) { state = state.copy(level = level) }

            // Результат дописывается к тексту: за время правки с клавиатуры пользователь мог уже что-то изменить.
            override fun onResult(text: String) {
                state = state.copy(text = VoiceErrors.capitalize(VoiceErrors.join(state.text, VoiceErrors.clean(text))), partial = "", recording = false, level = 0f)
            }

            override fun onError(error: VoiceError) {
                state = if (state.text.isBlank()) VoiceState(error = error) else state.copy(partial = "", recording = false, level = 0f, error = error)
            }
        })
    }
    DisposableEffect(engine) { onDispose { engine.destroy() } }
    BackHandler(onBack = onClose)
    VoiceSheet(
        state = state,
        onCancel = onClose,
        onDone = {
            // Запись, если идёт, обрывается; в задачу уходит текст как есть.
            engine.destroy()
            onResult(VoiceErrors.result(state.display))
        },
        onMic = {
            if (state.recording) {
                engine.stop()
            } else if (state.error == VoiceError.NO_PERMISSION && state.text.isBlank()) {
                openAppSettings(context)
                onClose()
            } else {
                focus.clearFocus()
                keyboard?.hide()
                attempt++
            }
        },
        onEdit = { edited -> state = state.copy(text = edited, error = null) },
        onFocusText = { if (state.recording) engine.stop() },
    )
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun granted(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Окно: затемнение и белая карточка фиксированной высоты под строкой состояния — по центру в верхней части экрана. Клавиатура
 * открывается под карточкой и не двигает её. Подпись, поле с текстом, микрофон с анимацией, «Отмена» и «Готово».
 */
@Composable
fun VoiceSheet(
    state: VoiceState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onMic: () -> Unit,
    onEdit: (String) -> Unit = {},
    onFocusText: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val error = state.error
    val fatal = error != null && state.text.isBlank()
    // Нажатие на текст во время записи: остановить запись и, как только появится поле, сразу открыть клавиатуру (#299).
    var wantFocus by remember { mutableStateOf(false) }
    LaunchedEffect(fatal) { if (fatal) wantFocus = false }
    Box(
        Modifier.fillMaxSize().background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
            .testTag(TAG_VOICE),
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = spacing.md, end = spacing.md, top = spacing.lg)
                .fillMaxWidth()
                .height(sizes.voiceHeight)
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                // Нажатия по карточке не закрывают окно.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(start = spacing.md, end = spacing.md, top = spacing.md, bottom = spacing.sm),
        ) {
            Text(
                stringResource(
                    when {
                        fatal -> R.string.voice_title
                        error == VoiceError.NOT_HEARD -> R.string.voice_not_heard_again
                        error != null -> errorTitle(error)
                        state.recording && state.display.isEmpty() -> R.string.voice_speak
                        state.recording -> R.string.voice_listening
                        else -> R.string.voice_check
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
            )
            val big = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            if (fatal) {
                Column(Modifier.weight(1f).padding(top = spacing.xs)) {
                    Text(stringResource(errorTitle(error!!)), style = big, modifier = Modifier.testTag(TAG_VOICE_TEXT))
                    Text(
                        stringResource(errorHint(error)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xxs),
                    )
                }
            } else {
                Box(
                    Modifier.padding(top = spacing.xs).fillMaxWidth().heightIn(min = sizes.voiceTextMin).height(sizes.voiceTextMin + spacing.lg)
                        .clip(AppTheme.shapes.large).background(colors.background).padding(horizontal = spacing.sm, vertical = spacing.xs),
                ) {
                    val scroll = rememberScrollState()
                    LaunchedEffect(state.display) { scroll.scrollTo(scroll.maxValue) }
                    if (state.recording && state.display.isEmpty()) {
                        Text(stringResource(R.string.voice_example), style = big.copy(color = colors.inactive), modifier = Modifier.testTag(TAG_VOICE_TEXT))
                    } else if (state.recording) {
                        Text(
                            tailText(state, colors.inactive),
                            style = big,
                            modifier = Modifier.verticalScroll(scroll)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    wantFocus = true
                                    onFocusText()
                                }
                                .testTag(TAG_VOICE_TEXT),
                        )
                    } else {
                        // Курсор после паузы — в конце текста: можно сразу дописывать; внешнее изменение (продолжение диктовки) тоже ставит его в конец.
                        var field by remember { mutableStateOf(TextFieldValue(state.text, TextRange(state.text.length))) }
                        LaunchedEffect(state.text) {
                            if (VoiceErrors.capitalize(field.text) != state.text) field = TextFieldValue(state.text, TextRange(state.text.length))
                        }
                        val requester = remember { FocusRequester() }
                        val keyboard = LocalSoftwareKeyboardController.current
                        LaunchedEffect(wantFocus) {
                            if (wantFocus) {
                                requester.requestFocus()
                                keyboard?.show()
                                wantFocus = false
                            }
                        }
                        BasicTextField(
                            value = field,
                            onValueChange = {
                                field = it
                                onEdit(VoiceErrors.capitalize(it.text))
                            },
                            textStyle = big,
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(),
                            modifier = Modifier.fillMaxSize().verticalScroll(scroll).focusRequester(requester).testTag(TAG_VOICE_FIELD),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { MicButton(state, onMic) }
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
                val enabled = state.canDone || fatal
                Text(
                    stringResource(
                        when {
                            !fatal -> R.string.voice_done
                            error == VoiceError.NO_PERMISSION -> R.string.voice_settings
                            else -> R.string.voice_retry
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onAccent,
                    modifier = Modifier
                        .clip(AppTheme.shapes.large)
                        .background(if (enabled) colors.accent else colors.inactive)
                        .clickable(enabled = enabled, role = Role.Button, onClick = if (fatal) onMic else onDone)
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                        .testTag(TAG_VOICE_ACTION),
                )
            }
        }
    }
}

/** Текст записи: подтверждённая часть чёрным, последнее слово текущей фразы, которое движок ещё уточняет, — серым. */
private fun tailText(state: VoiceState, gray: Color) = buildAnnotatedString {
    val full = state.display
    val (heard, tail) = VoiceErrors.split(VoiceErrors.join(state.text, state.partial))
    val head = VoiceErrors.capitalize(heard)
    if (state.partial.isBlank()) {
        append(full)
    } else {
        append(head)
        if (head.isNotEmpty() && tail.isNotEmpty()) append(' ')
        withStyle(SpanStyle(color = gray)) { append(if (head.isEmpty()) VoiceErrors.capitalize(tail) else tail) }
    }
}

/**
 * Большой микрофон, вариант «Дыхание» (#299, выбор PM): во время записи он чуть «дышит» (масштаб 1 → 1,1 за 1,2 с), вокруг
 * мягкое серое свечение; голос делает свечение крупнее. На паузе — серый круг без движения. Если в Android выключены
 * анимации, дыхание и свечение не движутся.
 */
@Composable
private fun MicButton(state: VoiceState, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val context = LocalContext.current
    val animated = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val recording = state.recording
    val level by animateFloatAsState(if (recording) state.level else 0f, label = "voiceLevel")
    val phase = breathPhase(recording && animated)
    val glow = (GLOW_MIN + (GLOW_MAX - GLOW_MIN) * phase) * (1f + GLOW_LEVEL * level)
    val label = stringResource(if (recording) R.string.voice_tap_stop else R.string.voice_tap_continue)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(sizes.voiceGlow), contentAlignment = Alignment.Center) {
            if (recording) {
                Box(
                    Modifier.size(sizes.voiceGlow).scale(glow).drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(colors.inactive.copy(alpha = GLOW_ALPHA * (GLOW_BASE + phase * GLOW_PULSE)), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 2,
                            ),
                        )
                    },
                )
            }
            Box(
                Modifier.size(sizes.voiceMic).scale(1f + BREATH_SCALE * phase).clip(CircleShape)
                    .background(if (recording) colors.accent else colors.accentSoft)
                    .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                    .testTag(TAG_VOICE_MIC),
                contentAlignment = Alignment.Center,
            ) {
                val off = state.error == VoiceError.NO_PERMISSION || state.error == VoiceError.UNAVAILABLE || state.error == VoiceError.BUSY
                Icon(
                    painterResource(if (off) R.drawable.ic_voice_mic_off else R.drawable.ic_widget_bar_mic),
                    contentDescription = label,
                    tint = if (recording) colors.onAccent else colors.textPrimary,
                    modifier = Modifier.size(sizes.voiceMicIcon),
                )
            }
        }
        if (state.error == null) Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
    }
}

/**
 * Фаза «дыхания» 0..1. Бесконечная анимация создаётся только во время записи: на паузе, в ошибке и без анимаций системы
 * кадры не перерисовываются (проверка #299 — раньше она крутилась всё время, пока открыто окно).
 */
@Composable
private fun breathPhase(active: Boolean): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "voiceBreath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    return breath
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

private const val BREATH_MILLIS = 1200
private const val BREATH_SCALE = 0.1f
private const val GLOW_MIN = 0.85f
private const val GLOW_MAX = 1.25f
private const val GLOW_LEVEL = 0.3f
private const val GLOW_ALPHA = 1f
private const val GLOW_BASE = 0.55f
private const val GLOW_PULSE = 0.45f

const val TAG_VOICE = "voice"
const val TAG_VOICE_TEXT = "voice_text"
const val TAG_VOICE_FIELD = "voice_field"
const val TAG_VOICE_MIC = "voice_mic"
const val TAG_VOICE_CANCEL = "voice_cancel"
const val TAG_VOICE_ACTION = "voice_action"
