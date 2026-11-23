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
import androidx.compose.ui.semantics.testTag
import com.prostovasei.simpletasks.widget.WidgetDates
import com.prostovasei.simpletasks.ui.feature.task.ChoiceStyle
import com.prostovasei.simpletasks.ui.feature.task.ChoiceDialog
import com.prostovasei.simpletasks.ui.feature.task.Choice
import com.prostovasei.simpletasks.ui.common.showUndo
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.ui.feature.task.rememberKeyboardHeight
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Голосовой ввод из виджета (#95, #299, #305, #308, #310; макеты design/approved/305, design/approved/310). Окно поверх рабочего
 * стола слушает, пока не нажата ⏸ или 10 с нет речи. Каждая фраза после паузы — отдельная задача в списке над окном, по дням
 * (путь А, без ИИ). Нажатие на задачу — [onEdit]: окно задачи вырастает из окна голоса; сохранённая там задача уходит из списка.
 * Галочка — [onSaveAll] со всеми задачами списка. «Назад» и затемнение при задачах — вопрос «Сохранить N задач?».
 * [today] — как у «+» вида «Все»: задача без срока — на сегодня.
 */
@Composable
fun VoiceRoute(
    onClose: () -> Unit,
    onSaveAll: (List<VoiceTask>) -> Unit,
    onEdit: (VoiceTask, android.graphics.Rect?) -> Unit = { _, _ -> },
    today: Boolean = false,
    smartDates: Boolean = true,
    clock: () -> java.time.ZonedDateTime = { java.time.ZonedDateTime.now() },
    engineFactory: (Context) -> VoiceEngine = ::AndroidVoiceEngine,
    list: VoiceListModel = viewModel(),
) {
    val context = LocalContext.current
    val engine = remember { engineFactory(context.applicationContext) }
    val tasks by list.tasks.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf(VoiceState()) }
    var attempt by remember { mutableIntStateOf(0) }
    var allowed by remember { mutableStateOf(granted(context)) }
    var asking by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val zone = remember { clock().zone }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        if (!ok) state = VoiceState(error = VoiceError.NO_PERMISSION)
    }
    LaunchedEffect(Unit) { if (!allowed) permission.launch(Manifest.permission.RECORD_AUDIO) }
    // #308: окно слушает, пока не нажата ⏸; распознаватель сам завершает фразу на паузе в речи — каждая фраза становится
    // задачей (#310), запись незаметно продолжается. [listening] хранится в [list]: после правки задачи окно возвращается на паузе.
    var listening by remember { mutableStateOf(list.listening) }
    LaunchedEffect(listening) { list.listening = listening }
    var speech by remember { mutableIntStateOf(0) }
    val add: (String) -> Unit = { phrase ->
        if (phrase.isNotBlank()) {
            val now = clock()
            list.add { id -> VoiceTasks.parse(id, phrase, now.toLocalDate(), now.hour * 60 + now.minute, zone, smartDates, today) }
        }
    }
    LaunchedEffect(allowed, attempt) {
        if (!allowed || !listening) return@LaunchedEffect
        state = state.copy(partial = "", recording = true, error = null, level = 0f)
        engine.start(object : VoiceEngine.Listener {
            override fun onPartial(text: String) {
                if (text.isNotBlank() && text != state.partial) speech++
                state = state.copy(partial = text)
            }
            override fun onLevel(level: Float) { state = state.copy(level = level) }

            override fun onResult(text: String) {
                if (text.isNotBlank()) speech++
                add(VoiceErrors.clean(text))
                state = state.copy(partial = "", recording = listening, level = 0f)
                if (listening) attempt++
            }

            override fun onError(error: VoiceError) {
                when {
                    // Тишина во время непрерывной записи — не ошибка: слушаем дальше, паузу ставит отсчёт [SILENCE_MILLIS].
                    error == VoiceError.NOT_HEARD && listening -> attempt++
                    // Автопауза после тишины: если задачи уже есть — просто пауза, без «Не расслышал».
                    error == VoiceError.NOT_HEARD && list.tasks.value.isNotEmpty() -> state = state.copy(partial = "", recording = false, level = 0f)
                    else -> {
                        listening = false
                        state = state.copy(partial = "", recording = false, level = 0f, error = error)
                    }
                }
            }
        })
    }
    // #308, ответ PM: 10 с без речи — пауза, как ⏸. Отсчёт сбрасывает каждое услышанное слово.
    LaunchedEffect(listening, speech, allowed) {
        if (!listening || !allowed) return@LaunchedEffect
        delay(SILENCE_MILLIS)
        listening = false
        engine.stop()
    }
    DisposableEffect(engine) { onDispose { engine.destroy() } }
    // Недоговорённая фраза тоже становится задачей: при паузе, правке, сохранении и закрытии.
    val takePartial = {
        add(VoiceErrors.clean(state.partial))
        state = state.copy(partial = "")
    }
    // Окно уходит (правка, сохранение, закрытие): запись обрывается без ответа движка, недоговорённая фраза — тоже задача.
    val halt = {
        listening = false
        // Сразу, а не через эффект: окно уходит в этом же кадре, а при возврате оно должно быть на паузе.
        list.listening = false
        engine.destroy()
        takePartial()
        state = state.copy(recording = false, level = 0f)
    }
    val saveAll = {
        halt()
        val all = list.tasks.value
        if (all.isNotEmpty()) {
            closing = true
            list.clear()
            onSaveAll(all)
        }
    }
    val back = {
        halt()
        if (list.tasks.value.isEmpty()) onClose() else asking = true
    }
    BackHandler(enabled = !closing, onBack = back)
    if (closing) return
    val removedText = stringResource(R.string.voice_removed)
    val undoText = stringResource(R.string.task_undo)
    VoiceSheet(
        state = state,
        days = remember(tasks) { VoiceTasks.days(tasks, zone) },
        count = tasks.size,
        onCancel = back,
        onDone = saveAll,
        onMic = {
            if (state.recording) {
                // ⏸: распознаватель отдаст последнюю фразу сам — она станет задачей, запись не перезапустится.
                listening = false
                engine.stop()
            } else if (state.error?.blocking() != true) {
                listening = true
                attempt++
            }
        },
        onOpen = { task, origin ->
            halt()
            onEdit(task, origin)
        },
        onRemove = { task ->
            list.remove(task.id)?.let { (index, removed) ->
                scope.launch { snackbar.showUndo(removedText.format(removed.title), undoText, UndoKind.Deleted) { list.restore(index, removed) } }
            }
        },
        onSettings = {
            openAppSettings(context)
            onClose()
        },
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        UndoSnackbarHost(snackbar, Modifier.navigationBarsPadding().padding(bottom = AppTheme.spacing.lg))
    }
    if (asking) {
        val n = tasks.size
        ChoiceDialog(
            title = pluralStringResource(R.plurals.voice_save_question, n, n),
            text = stringResource(R.string.voice_save_question_text),
            tag = TAG_VOICE_ASK,
            choices = listOf(
                Choice(stringResource(R.string.voice_save_all), ChoiceStyle.Primary, TAG_ASK_SAVE) { asking = false; saveAll() },
                Choice(stringResource(R.string.voice_discard), ChoiceStyle.Outline, TAG_ASK_DISCARD) { asking = false; list.clear(); onClose() },
            ),
            onDismiss = { asking = false },
        )
    }
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
 * Окно (#305 «Цветной прилив», #310 вариант PM 7 «По датам»): затемнение, белая карточка на месте окна новой задачи — сразу
 * над клавиатурой, высотой как окно задачи без срока; над ней — продиктованные задачи по дням («вт, 24 ноя», «Без даты» в конце),
 * без кружка выполнения, со временем справа и «×». В карточке — текущая фраза, вода, ⏸/⏺ слева, галочка с числом задач справа,
 * ошибка между ними. «Назад» или затемнение — [onCancel].
 */
@Composable
fun VoiceSheet(
    state: VoiceState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onMic: () -> Unit,
    days: List<VoiceDay> = emptyList(),
    count: Int = 0,
    onOpen: (VoiceTask, android.graphics.Rect?) -> Unit = { _, _ -> },
    onRemove: (VoiceTask) -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val error = state.error
    val closeLabel = stringResource(R.string.voice_close_label)
    val notHeardSpoken = stringResource(R.string.voice_err_not_heard_spoken)
    val view = LocalView.current
    var card by remember { mutableStateOf<android.graphics.Rect?>(null) }
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
            if (days.isNotEmpty()) {
                VoiceList(
                    days, onOpen = { onOpen(it, card) }, onRemove = onRemove,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(start = spacing.xs, end = spacing.xs, top = spacing.sm, bottom = lift + sizes.voiceHeight + spacing.xs),
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = lift, start = spacing.xs, end = spacing.xs)
                    .fillMaxWidth()
                    .height(sizes.voiceHeight)
                    .onGloballyPositioned { coordinates ->
                        // Откуда вырастет окно задачи (#310): место карточки на экране, как у плашки виджета.
                        val at = IntArray(2).also(view::getLocationOnScreen)
                        val bounds = coordinates.boundsInRoot()
                        card = android.graphics.Rect(at[0] + bounds.left.toInt(), at[1] + bounds.top.toInt(), at[0] + bounds.right.toInt(), at[1] + bounds.bottom.toInt())
                    }
                    .shadow(sizes.navShadow, AppTheme.shapes.nav)
                    .clip(AppTheme.shapes.nav)
                    .background(colors.surface)
                    // Нажатия по карточке не закрывают окно; без clickable — TalkBack не видит здесь лишней кнопки.
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Tide(state, Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(SizeTokens.VOICE_TIDE_PERCENT / 100f))
                Text(
                    phraseText(state.partial, colors.inactive),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary),
                    maxLines = PHRASE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.md, top = spacing.md).testTag(TAG_VOICE_TEXT),
                )
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, bottom = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val micEnabled = state.recording || error?.blocking() != true
                    SheetButton(
                        icon = if (state.recording) R.drawable.ic_voice_pause else R.drawable.ic_voice_record,
                        label = stringResource(if (state.recording) R.string.voice_pause else R.string.voice_record),
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
                    Box {
                        SheetButton(
                            icon = R.drawable.ic_voice_check,
                            label = pluralStringResource(R.plurals.voice_save_count, count, count),
                            background = colors.accent,
                            tint = colors.onAccent,
                            enabled = count > 0 || state.partial.isNotBlank(),
                            tag = TAG_VOICE_ACTION,
                            onClick = onDone,
                        )
                        // #310, просьба PM: на галочке — сколько задач будет сохранено.
                        if (count > 0) CountBadge(count, Modifier.align(Alignment.TopEnd).offset(x = sizes.voiceBadge / 3, y = -sizes.voiceBadge / 3))
                    }
                }
            }
        }
    }
}

/** Задачи над окном по дням: заголовок дня — «вт, 24 ноя» (формат PM), строки — название, время справа, «×». */
@Composable
private fun VoiceList(days: List<VoiceDay>, onOpen: (VoiceTask) -> Unit, onRemove: (VoiceTask) -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val zone = remember { java.time.ZoneId.systemDefault() }
    val scroll = rememberScrollState()
    val total = days.sumOf { it.tasks.size }
    // Новая задача всегда видна: список прокручивается к концу.
    LaunchedEffect(total) { scroll.animateScrollTo(scroll.maxValue) }
    val noDate = stringResource(R.string.voice_no_date)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(sizes().navShadow / 2, AppTheme.shapes.large)
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(scroll)
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .testTag(TAG_VOICE_LIST),
    ) {
        days.forEach { day ->
            Text(
                day.date?.let { WidgetDates.bar(it, withWeekday = true) } ?: noDate,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs).semantics { heading() },
            )
            day.tasks.forEach { task ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = sizes().voiceRow)
                        .clip(AppTheme.shapes.small)
                        .clickable(onClick = { onOpen(task) })
                        .testTag("$TAG_VOICE_ROW:${task.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f).padding(vertical = spacing.xxs))
                    task.time(zone)?.let { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.textSecondary) }
                    val removeLabel = stringResource(R.string.voice_remove, task.title)
                    Box(
                        Modifier.size(sizes().voiceRow).clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = removeLabel) { onRemove(task) }
                            .semantics { contentDescription = removeLabel }
                            .testTag("$TAG_VOICE_REMOVE:${task.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.inactive, modifier = Modifier.size(sizes().voiceRemoveIcon))
                    }
                }
            }
        }
    }
}

@Composable
private fun sizes() = AppTheme.sizes

/** Число задач на галочке: кружок цвета приоритета, как метка «Новое» у новостей. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier.size(AppTheme.sizes.voiceBadge).clip(CircleShape).background(colors.priority.high.content).clearAndSetSemantics { testTag = TAG_VOICE_COUNT },
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = colors.surface)
    }
}

/** Текущая фраза: последнее слово, которое движок ещё уточняет, — серым. */
private fun phraseText(partial: String, gray: Color) = buildAnnotatedString {
    val (heard, tail) = VoiceErrors.split(partial)
    val head = VoiceErrors.capitalize(heard)
    append(head)
    if (head.isNotEmpty() && tail.isNotEmpty()) append(' ')
    withStyle(SpanStyle(color = gray)) { append(if (head.isEmpty()) VoiceErrors.capitalize(tail) else tail) }
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

/** #308: тишина, после которой запись сама встаёт на паузу (ответ PM 23.11.2026). */
internal const val SILENCE_MILLIS = 10_000L
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
const val TAG_VOICE_MIC = "voice_mic"
const val TAG_VOICE_ERROR = "voice_error"
const val TAG_VOICE_ACTION = "voice_action"
const val TAG_VOICE_LIST = "voice_list"
const val TAG_VOICE_ROW = "voice_row"
const val TAG_VOICE_REMOVE = "voice_remove"
const val TAG_VOICE_COUNT = "voice_count"
const val TAG_VOICE_ASK = "voice_ask"
const val TAG_ASK_SAVE = "save"
const val TAG_ASK_DISCARD = "discard"
private const val PHRASE_LINES = 3
