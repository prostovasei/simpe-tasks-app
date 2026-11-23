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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.ui.feature.task.rememberKeyboardHeight
import kotlin.math.PI
import kotlin.math.sin

/**
 * Голосовой ввод из виджета (#95, #299, #305; макеты design/approved/305): своё окно поверх рабочего стола. Запись начинается
 * сразу; после паузы она останавливается, а текст остаётся в окне — его правят с клавиатуры, ⏺ дописывает продолжение,
 * галочка передаёт текст в [onResult] — дальше открывается окно новой задачи. «Назад» и затемнение закрывают без задачи.
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
            } else if (state.error?.blocking() != true) {
                focus.clearFocus()
                keyboard?.hide()
                attempt++
            }
        },
        onEdit = { edited -> state = state.copy(text = edited, error = null) },
        onFocusText = { if (state.recording) engine.stop() },
        onSettings = {
            openAppSettings(context)
            onClose()
        },
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
 * Окно (#305, вариант PM «Цветной прилив»): затемнение и белая карточка на месте окна новой задачи — сразу над клавиатурой,
 * высотой как окно задачи без срока; клавиатура её не двигает. Сверху распознанный текст, снизу «вода» на всю ширину,
 * поверх неё — ⏸/⏺ слева, галочка справа, ошибка между ними. Других подписей нет; закрытие — «Назад» или затемнение.
 */
@Composable
fun VoiceSheet(
    state: VoiceState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onMic: () -> Unit,
    onEdit: (String) -> Unit = {},
    onFocusText: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val error = state.error
    // Нажатие на текст во время записи: остановить запись и, как только появится поле, сразу открыть клавиатуру (#299).
    var wantFocus by remember { mutableStateOf(false) }
    val closeLabel = stringResource(R.string.voice_close_label)
    val notHeardSpoken = stringResource(R.string.voice_err_not_heard_spoken)
    Box(
        Modifier.fillMaxSize().background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
            .semantics { contentDescription = closeLabel }
            .testTag(TAG_VOICE),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            val density = LocalDensity.current
            val keyboard = with(density) { rememberKeyboardHeight((maxHeight * KEYBOARD_FALLBACK).roundToPx()).toDp() }
            val ime = with(density) { WindowInsets.ime.getBottom(this).toDp() }
            val navBar = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val lift = maxOf(keyboard, ime, navBar) + spacing.xs
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = lift, start = spacing.xs, end = spacing.xs)
                    .fillMaxWidth()
                    .height(sizes.voiceHeight)
                    .shadow(sizes.navShadow, AppTheme.shapes.nav)
                    .clip(AppTheme.shapes.nav)
                    .background(colors.surface)
                    // Нажатия по карточке не закрывают окно; без clickable — TalkBack не видит здесь лишней кнопки.
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Tide(state, Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(SizeTokens.VOICE_TIDE_PERCENT / 100f))
                VoiceText(state, wantFocus, { wantFocus = it }, onEdit, onFocusText)
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, bottom = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val micEnabled = state.recording || error?.blocking() != true
                    val micLabel = stringResource(if (state.recording) R.string.voice_pause else R.string.voice_record)
                    SheetButton(
                        icon = if (state.recording) R.drawable.ic_voice_pause else R.drawable.ic_voice_record,
                        label = micLabel,
                        background = colors.surface,
                        tint = colors.textPrimary,
                        enabled = micEnabled,
                        tag = TAG_VOICE_MIC,
                        onClick = onMic,
                    )
                    Box(Modifier.weight(1f).padding(horizontal = spacing.xs), contentAlignment = Alignment.Center) {
                        if (error != null) {
                            Text(
                                stringResource(errorText(error)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = ERROR_LINES,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .then(if (error == VoiceError.NO_PERMISSION) Modifier.clickable(role = Role.Button, onClick = onSettings) else Modifier)
                                    // Ошибка появляется без действия пользователя — TalkBack её проговаривает; «●» читается словами.
                                    .semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        if (error == VoiceError.NOT_HEARD) contentDescription = notHeardSpoken
                                    }
                                    .testTag(TAG_VOICE_ERROR),
                            )
                        }
                    }
                    SheetButton(
                        icon = R.drawable.ic_voice_check,
                        label = stringResource(R.string.voice_done),
                        background = colors.accent,
                        tint = colors.onAccent,
                        enabled = state.canDone,
                        tag = TAG_VOICE_ACTION,
                        onClick = onDone,
                    )
                }
            }
        }
    }
}

/** Распознанный текст вверху окна: во время записи — строка с серым уточняемым словом, на паузе — поле для правки (#299). */
@Composable
private fun VoiceText(state: VoiceState, wantFocus: Boolean, setWantFocus: (Boolean) -> Unit, onEdit: (String) -> Unit, onFocusText: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val big = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
    val scroll = rememberScrollState()
    LaunchedEffect(state.display) { scroll.scrollTo(scroll.maxValue) }
    // Текст не заходит на кнопки: снизу — место под строку кнопок.
    val modifier = Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.md, top = spacing.md)
        .height(AppTheme.sizes.voiceHeight - AppTheme.sizes.toolButton - spacing.sm * 2 - spacing.md)
    if (state.recording || state.error != null && state.text.isBlank()) {
        Text(
            tailText(state, colors.inactive),
            style = big,
            modifier = modifier.verticalScroll(scroll)
                .clickable(enabled = state.recording, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    setWantFocus(true)
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
                setWantFocus(false)
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
            modifier = modifier.verticalScroll(scroll).focusRequester(requester).testTag(TAG_VOICE_FIELD),
        )
    }
}

/** Квадратная кнопка окна, как на панели окна задачи: ⏸/⏺ слева (белая с тенью — лежит на воде) и галочка справа. */
@Composable
private fun SheetButton(icon: Int, label: String, background: Color, tint: Color, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.large
    Box(
        Modifier
            .size(sizes.toolButton)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .shadow(if (background == AppTheme.colors.surface) sizes.navShadow / 3 else Dp.Hairline, shape)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(sizes.toolIcon))
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
 * «Цветной прилив» (#305): три слоя воды снизу окна. Во время записи вода цветная, голос поднимает её и раскачивает
 * волны; на паузе и при ошибке она сереет, опускается и успокаивается. Если в Android выключены анимации — неподвижна.
 */
@Composable
private fun Tide(state: VoiceState, modifier: Modifier) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val animated = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val rec by animateFloatAsState(if (state.recording) 1f else 0f, tween(CALM_MILLIS), label = "voiceRec")
    val level by animateFloatAsState(if (state.recording) state.level else 0f, label = "voiceLevel")
    val phase = tidePhase(state.recording, animated) { rec }
    val rise = with(LocalDensity.current) { AppTheme.sizes.voiceTideRise.toPx() }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        LAYERS.forEachIndexed { i, layer ->
            val y0 = h * layer.base + (1f - rec) * h * CALM_DROP - level * rec * rise
            val amp = (WAVE_MIN + (WAVE_REC + level * WAVE_VOICE) * rec) * h * (1f - i * WAVE_DEPTH)
            val path = Path().apply {
                moveTo(0f, h)
                var x = 0f
                while (x <= w + STEP) {
                    lineTo(x, y0 + sin(x / w * PI.toFloat() * 2f * layer.freq + phase * (1f + i * 0.35f) + layer.shift) * amp)
                    x += STEP
                }
                lineTo(w, h)
                close()
            }
            drawPath(path, lerp(colors.voiceCalm[i], colors.voiceTide[i], rec))
        }
    }
}

/**
 * Фаза волн: свои часы вместо бесконечной анимации, чтобы на паузе волны не прыгали к началу, а плавно замедлялись вместе
 * с переходом в серую воду и продолжались с того же места после ⏺. Кадры идут только пока вода движется; при выключенных
 * анимациях системы — никогда.
 */
@Composable
private fun tidePhase(recording: Boolean, animated: Boolean, rec: () -> Float): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(recording, animated) {
        if (!animated) return@LaunchedEffect
        var last = withInfiniteAnimationFrameNanos { it }
        while (recording || rec() > STILL) {
            val now = withInfiniteAnimationFrameNanos { it }
            val speed = WAVE_SPEED * (WAVE_SLOW + (1f - WAVE_SLOW) * rec())
            phase = (phase + (now - last) / NANOS * speed) % PHASE_WRAP
            last = now
        }
    }
    return phase
}

private class TideLayer(val base: Float, val freq: Float, val shift: Float)

private val LAYERS = listOf(TideLayer(0.16f, 1f, 0.9f), TideLayer(0.34f, 1.35f, 2.1f), TideLayer(0.54f, 1.8f, 3.4f))

private fun errorText(error: VoiceError) = when (error) {
    VoiceError.NOT_HEARD -> R.string.voice_err_not_heard
    VoiceError.NO_PERMISSION -> R.string.voice_err_no_permission
    VoiceError.UNAVAILABLE -> R.string.voice_err_unavailable
    VoiceError.BUSY -> R.string.voice_err_busy
    VoiceError.NO_NETWORK -> R.string.voice_err_no_network
    VoiceError.NO_LANGUAGE -> R.string.voice_err_no_language
    VoiceError.START_FAILED -> R.string.voice_err_start_failed
}

/** Ошибки, после которых повтор не поможет: ⏺ неактивна (нет доступа — «Настройки» в тексте ошибки). */
private fun VoiceError.blocking() = this == VoiceError.NO_PERMISSION || this == VoiceError.UNAVAILABLE || this == VoiceError.NO_LANGUAGE

private const val KEYBOARD_FALLBACK = 0.4f
private const val DISABLED_ALPHA = 0.35f
private const val CALM_MILLIS = 600
private const val WAVE_SPEED = 1.96f
private const val WAVE_SLOW = 0.15f
private const val STILL = 0.01f
private const val NANOS = 1_000_000_000f
private const val ERROR_LINES = 2

/** Слои движутся с множителями 1; 1,35; 1,7 — сброс фазы на 20 кругах для всех слоёв приходится на целый круг. */
private val PHASE_WRAP = 2f * PI.toFloat() * 20f
private const val CALM_DROP = 0.16f
private const val WAVE_MIN = 0.015f
private const val WAVE_REC = 0.04f
private const val WAVE_VOICE = 0.12f
private const val WAVE_DEPTH = 0.15f
private const val STEP = 6f

const val TAG_VOICE = "voice"
const val TAG_VOICE_TEXT = "voice_text"
const val TAG_VOICE_FIELD = "voice_field"
const val TAG_VOICE_MIC = "voice_mic"
const val TAG_VOICE_ERROR = "voice_error"
const val TAG_VOICE_ACTION = "voice_action"
