package com.prostovasei.simpletasks.notifications

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.designsystem.components.dust
import com.prostovasei.simpletasks.designsystem.components.dustParticles
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * Полоска напоминания поверх других приложений (T-69, вариант PM «В»): зелёная, снизу, висит, пока не уберут.
 * Часы — на завтра в то же время, галочка — выполнено, название — окно задачи поверх, смахнуть вправо — закрыть.
 * Несколько сразу — до трёх полосок друг над другом, остальные — «ещё N». Вместо обычного уведомления:
 * оно приходит, только если полоску показать нельзя (выключено, нет разрешения, экран заблокирован).
 */

/** Переключатель в «Настройках» (решение PM: по умолчанию выключен — нужно разрешение телефона). */
object PopupPrefs {
    private const val PREFS = "reminders"
    private const val KEY = "popup_enabled"

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY, value) }

    /** Разрешение «Появление поверх других приложений». */
    fun allowed(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Системный экран этого разрешения для приложения. */
    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
}

/**
 * Полоска одной задачи: название, подпись «Сегодня, 18:00 · Дом» и [later] — когда напомнит после «на завтра».
 * [undo] — полоска уже сменилась на «Выполнено · Отменить» или «Завтра, 18:00 · Отменить» (0.29, П1).
 */
@Immutable
data class PopupStrip(val taskId: String, val title: String, val subtitle: String, val later: String = "", val undo: String? = null)

@Singleton
class ReminderPopup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val strips = mutableStateListOf<PopupStrip>()
    private var window: Overlay? = null

    /** Как вернуть задачу, пока на полоске «Отменить»: результат действия придёт, когда оно запишется в базу. */
    private val reverts = mutableMapOf<String, Deferred<suspend () -> Unit>>()

    /** Полоску можно показать: включена, есть разрешение, экран включён и разблокирован. */
    fun canShow(): Boolean {
        if (!PopupPrefs.enabled(context) || !PopupPrefs.allowed(context)) return false
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return power.isInteractive && !keyguard.isKeyguardLocked
    }

    /** Показывает полоску (повторное напоминание той же задачи — заменяет её). Без вибрации (0.30.1, решение PM). */
    suspend fun show(strip: PopupStrip) = withContext(Dispatchers.Main.immediate) {
        strips.removeAll { it.taskId == strip.taskId }
        strips += strip
        if (window == null) window = Overlay(context) { Content() }.also { it.attach() }
        // Пока полоска на экране, Samsung не должен «заморозить» приложение — иначе она не появится до касания.
        PopupTrace.add(context, "показ «${strip.title}», важность процесса ${PopupTrace.importance()}")
        PopupService.start(context, strip.title)
    }

    /** Задачи на полосках сейчас, по порядку прихода. */
    fun shown(): List<String> = strips.map { it.taskId }

    fun dismiss(taskId: String) {
        appScope.launch(Dispatchers.Main.immediate) { remove(taskId) }
    }

    private fun remove(taskId: String) {
        reverts.remove(taskId)
        strips.removeAll { it.taskId == taskId }
        if (strips.isEmpty()) {
            window?.detach()
            PopupService.stop(context)
            window = null
        }
    }

    @Composable
    private fun Content() {
        AppTheme {
            ReminderStrips(
                strips = strips,
                onDone = { id ->
                    act(id, context.getString(R.string.popup_done)) {
                        val completion = tasks.complete(id)
                        suspend { tasks.undo(completion) }
                    }
                },
                onTomorrow = { id ->
                    act(id, strips.find { it.taskId == id }?.later.orEmpty().ifEmpty { context.getString(R.string.popup_tomorrow) }) {
                        val before = tasks.getTask(id)
                        tasks.moveToTomorrow(id)
                        suspend { if (before != null) tasks.updateTask(before) }
                    }
                },
                onUndo = ::undo,
                onExpire = ::remove,
                onOpen = { id ->
                    remove(id)
                    context.startActivity(
                        Intent(context, TaskPopupActivity::class.java)
                            .putExtra(MainActivity.EXTRA_TASK_ID, id)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    )
                },
                onDismiss = ::remove,
            )
        }
    }

    /** Действие с полоски: полоска сменяется на «… · Отменить» (П1), задача меняется сразу. */
    private fun act(taskId: String, done: String, action: suspend () -> (suspend () -> Unit)) {
        val index = strips.indexOfFirst { it.taskId == taskId }
        if (index < 0) return
        strips[index] = strips[index].copy(undo = done)
        reverts[taskId] = appScope.async { action() }
    }

    /** «Отменить»: задача возвращается как была, полоска — снова с кнопками. */
    private fun undo(taskId: String) {
        val revert = reverts.remove(taskId) ?: return
        val index = strips.indexOfFirst { it.taskId == taskId }
        if (index >= 0) strips[index] = strips[index].copy(undo = null)
        appScope.launch { revert.await().invoke() }
    }
}

/**
 * Окно поверх других приложений внизу экрана. Не забирает клавиатуру и нажатия мимо полосок — приложение под ним
 * работает как обычно. Compose в таком окне нужен свой «жизненный цикл» — его даёт [OverlayOwner].
 */
private class Overlay(private val context: Context, private val content: @Composable () -> Unit) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val owner = OverlayOwner()
    private var draws = 0
    private var touched = false

    // Диагностика 0.30.2: первые кадры окна и первое касание — в журнал.
    private val view = object : FrameLayout(context) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (!touched) {
                touched = true
                PopupTrace.add(context, "касание окна полоски")
            }
            return super.dispatchTouchEvent(event)
        }
    }.apply {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        addView(ComposeView(context).apply { setContent { content() } })
        viewTreeObserver.addOnDrawListener {
            if (draws < TRACE_DRAWS) PopupTrace.add(context, "кадр ${++draws}")
        }
    }

    fun attach() {
        owner.start()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM }
        manager.addView(view, params)
        PopupTrace.add(context, "окно добавлено")
    }

    fun detach() {
        runCatching { manager.removeView(view) }
        owner.stop()
    }
}

private const val TRACE_DRAWS = 5

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    fun start() {
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/** Сколько полосок видно одновременно (решение PM); остальные — «ещё N». */
const val POPUP_VISIBLE = 3

/** Стопка полосок: первая пришедшая — внизу, над ней следующие, сверху «ещё N». */
@Composable
fun ReminderStrips(
    strips: List<PopupStrip>,
    onDone: (String) -> Unit,
    onTomorrow: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    onUndo: (String) -> Unit = {},
    onExpire: (String) -> Unit = {},
) {
    val shown = strips.take(POPUP_VISIBLE)
    val more = strips.size - shown.size
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = SizeTokens.POPUP_EDGE.dp, vertical = SizeTokens.POPUP_GAP.dp),
        verticalArrangement = Arrangement.spacedBy(SizeTokens.POPUP_GAP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (more > 0) {
            Text(
                stringResource(R.string.popup_more, more),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.surface)
                    .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs)
                    .testTag(TAG_POPUP_MORE),
            )
        }
        shown.asReversed().forEach { strip ->
            // Своя «личность» у «Отменить»: после отмены полоска появляется заново, с анимацией появления.
            key(strip.taskId, strip.undo != null) {
                if (strip.undo != null) UndoStrip(strip, onUndo, onExpire) else Strip(strip, onDone, onTomorrow, onOpen, onDismiss)
            }
        }
    }
}

/*
 * Анимации полоски (0.28.1, выбор PM в прототипе): появление — П10 «Пульс» (выезжает снизу, вокруг дважды
 * расходится зелёное кольцо), выполнено — В9 «Пыль» (рассыпается слева направо, как раньше задача в ленте),
 * на завтра — О5 «Перелистывание» (листок календаря, на обороте «Завтра, 18:00», затем уезжает вниз).
 * Действие выполняется, когда анимация доиграла; без анимаций в настройках телефона — сразу.
 */
@Composable
private fun Strip(strip: PopupStrip, onDone: (String) -> Unit, onTomorrow: (String) -> Unit, onOpen: (String) -> Unit, onDismiss: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val context = LocalContext.current
    val animate = remember { animationsEnabled(context) }
    val offset = remember { Animatable(0f) }
    val enter = remember { Animatable(if (animate) 0f else 1f) }
    val pulse = remember { Animatable(0f) }
    val dust = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val back by remember { derivedStateOf { flip.value >= HALF } }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { SizeTokens.POPUP_SWIPE.dp.toPx() }
    val spread = with(density) { SizeTokens.POPUP_PULSE.dp.toPx() }
    val particleSize = with(density) { AppTheme.sizes.dustParticle.toPx() }
    val particles = remember(strip.taskId) { dustParticles(strip.taskId.hashCode(), lift = DUST_LIFT) }
    val dustColors = listOf(colors.accent, colors.accentSoft, colors.accent.copy(alpha = DUST_FADED))

    LaunchedEffect(Unit) {
        PopupTrace.add(context, "анимация появления: старт (анимации ${if (animate) "вкл" else "выкл"})")
        if (!animate) return@LaunchedEffect
        enter.animateTo(1f, tween(ENTER_MS, easing = LinearOutSlowInEasing))
        PopupTrace.add(context, "анимация появления: полоска на месте")
        pulse.animateTo(PULSES, tween(PULSE_MS, easing = LinearEasing))
    }

    fun leave(action: (String) -> Unit, motion: suspend () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            if (animate) motion()
            action(strip.taskId)
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(SizeTokens.POPUP_STRIP.dp)
            // Жест — до сдвига: координаты пальца считаются от неподвижного места, а не от уезжающей полоски.
            // Закрыть — смахнуть вправо (0.29.2, решение PM: вниз неудобно); дальше порога — улетает за край экрана.
            .pointerInput(strip.taskId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offset.value > threshold) {
                                leaving = true
                                offset.animateTo(size.width.toFloat(), tween(SWIPE_OUT_MS, easing = FastOutLinearInEasing))
                                onDismiss(strip.taskId)
                            } else {
                                offset.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { change, dx ->
                    change.consume()
                    if (!leaving) scope.launch { offset.snapTo((offset.value + dx).coerceAtLeast(0f)) }
                }
            }
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .graphicsLayer { alpha = 1f - (offset.value / size.width).coerceIn(0f, 1f) * SWIPE_FADE }
            .graphicsLayer {
                translationY = ((1f - enter.value) * ENTER_DEPTH + drop.value * DROP_DEPTH) * size.height
                rotationX = if (flip.value < HALF) flip.value / HALF * QUARTER_TURN else (flip.value / HALF - 2f) * QUARTER_TURN
                cameraDistance = CAMERA_DISTANCE * density.density
            }
            .drawBehind { pulseRing(pulse.value, spread, colors.accent) }
            .dust({ dust.value }, particles, particleSize, dustColors)
            .clip(CircleShape)
            .background(colors.accent)
            .padding(start = spacing.md, end = spacing.xs)
            .testTag("$TAG_POPUP:${strip.taskId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (back) {
            // Оборот листка: когда напомнит.
            Icon(painterResource(R.drawable.ic_popup_tomorrow), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
            Text(strip.later, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            return@Row
        }
        Icon(painterResource(R.drawable.ic_tool_reminder), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        Column(Modifier.weight(1f).clickable { if (!leaving) onOpen(strip.taskId) }.testTag("$TAG_POPUP_OPEN:${strip.taskId}")) {
            Text(strip.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(strip.subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onAccent.copy(alpha = SUBTITLE_ALPHA), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val tomorrow = stringResource(R.string.popup_tomorrow)
        Box(
            Modifier
                .size(SizeTokens.POPUP_BUTTON.dp)
                .clip(CircleShape)
                .background(colors.onAccent.copy(alpha = BUTTON_ALPHA))
                .clickable {
                    leave(onTomorrow) {
                        flip.animateTo(HALF, tween(FLIP_OUT_MS, easing = FastOutLinearInEasing))
                        flip.animateTo(1f, tween(FLIP_IN_MS, easing = LinearOutSlowInEasing))
                        // Пауза — задержкой анимации: её отсчитывают кадры, как и остальную анимацию.
                        drop.animateTo(1f, tween(DROP_MS, delayMillis = LATER_HOLD_MS, easing = FastOutLinearInEasing))
                    }
                }
                .semantics { contentDescription = tomorrow }
                .testTag("$TAG_POPUP_TOMORROW:${strip.taskId}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_popup_tomorrow), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        }
        val done = stringResource(R.string.notification_complete)
        Box(
            Modifier
                .size(SizeTokens.POPUP_BUTTON.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .clickable { leave(onDone) { dust.animateTo(1f, tween(STRIP_DUST_MS, easing = LinearEasing)) } }
                .semantics { contentDescription = done }
                .testTag("$TAG_POPUP_DONE:${strip.taskId}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.accent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        }
    }
}

/**
 * «Выполнено · Отменить» (0.29, П1): полторы секунды на случай промаха пальцем. Тонкая линия внизу показывает, сколько
 * осталось, потом полоска уезжает вниз и закрывается.
 */
@Composable
private fun UndoStrip(strip: PopupStrip, onUndo: (String) -> Unit, onExpire: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val context = LocalContext.current
    val animate = remember { animationsEnabled(context) }
    val appear = remember { Animatable(if (animate) 0f else 1f) }
    val left = remember { Animatable(1f) }
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(UNDO_APPEAR_MS, easing = LinearOutSlowInEasing))
        left.animateTo(0f, tween(UNDO_MS, easing = LinearEasing))
        if (animate) drop.animateTo(1f, tween(DROP_MS, easing = FastOutLinearInEasing))
        onExpire(strip.taskId)
    }
    val undoLabel = stringResource(R.string.popup_undo)
    Row(
        Modifier
            .fillMaxWidth()
            .height(SizeTokens.POPUP_STRIP.dp)
            .graphicsLayer {
                alpha = appear.value
                translationY = drop.value * DROP_DEPTH * size.height
            }
            .clip(CircleShape)
            .background(colors.accent)
            .drawWithContent {
                drawContent()
                // Сколько осталось до закрытия — линия внизу, тает справа налево.
                val line = SizeTokens.POPUP_UNDO_LINE.dp.toPx()
                drawRect(colors.onAccent.copy(alpha = UNDO_LINE_ALPHA), Offset(0f, size.height - line), Size(size.width * left.value, line))
            }
            .padding(start = spacing.md, end = spacing.xs)
            .testTag("$TAG_POPUP_UNDO_STRIP:${strip.taskId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        Text(strip.undo.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            undoLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surface)
                .clickable { onUndo(strip.taskId) }
                .padding(horizontal = spacing.md, vertical = spacing.xs)
                .testTag("$TAG_POPUP_UNDO:${strip.taskId}"),
        )
    }
}

/** Кольцо «Пульса»: [phase] от 0 до [PULSES], каждое кольцо расходится на [spread] и тает. */
private fun DrawScope.pulseRing(phase: Float, spread: Float, color: Color) {
    if (phase <= 0f || phase >= PULSES) return
    val t = phase - floor(phase)
    val grow = t * spread
    drawRoundRect(
        color = color,
        topLeft = Offset(-grow, -grow),
        size = Size(size.width + grow * 2, size.height + grow * 2),
        cornerRadius = CornerRadius(size.height / 2 + grow),
        alpha = RING_ALPHA * (1f - t),
    )
}

/** Анимации выключены в настройках телефона («Масштаб анимации» — откл.). */
private fun animationsEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

private const val ENTER_MS = 360
private const val UNDO_APPEAR_MS = 200
/** Сколько висит «Отменить» (0.29.2: было 3 с — долго, решение PM). */
private const val UNDO_MS = 1500
private const val SWIPE_OUT_MS = 180
private const val SWIPE_FADE = 0.7f
private const val UNDO_LINE_ALPHA = 0.6f
private const val PULSE_MS = 1100
private const val PULSES = 2f
private const val RING_ALPHA = 0.55f
private const val STRIP_DUST_MS = 600
private const val FLIP_OUT_MS = 200
private const val FLIP_IN_MS = 220
private const val LATER_HOLD_MS = 700
private const val DROP_MS = 280

/** Откуда выезжает и куда уезжает — в высотах полоски, за нижний край окна. */
private const val ENTER_DEPTH = 1.6f
private const val DROP_DEPTH = 1.7f
private const val HALF = 0.5f
private const val QUARTER_TURN = 90f
private const val CAMERA_DISTANCE = 12f

/** Частицы пыли летят почти горизонтально: над полоской окна мало места. */
private val DUST_LIFT = -0.3f..0.3f
private const val DUST_FADED = 0.6f

private const val SUBTITLE_ALPHA = 0.8f
private const val BUTTON_ALPHA = 0.18f

const val TAG_POPUP = "popup"
const val TAG_POPUP_OPEN = "popup_open"
const val TAG_POPUP_TOMORROW = "popup_tomorrow"
const val TAG_POPUP_DONE = "popup_done"
const val TAG_POPUP_MORE = "popup_more"
const val TAG_POPUP_UNDO = "popup_undo"
const val TAG_POPUP_UNDO_STRIP = "popup_undo_strip"
