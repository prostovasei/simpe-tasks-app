package com.prostovasei.simpletasks.notifications

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/*
 * Полоска напоминания поверх других приложений (T-69, вариант PM «В»): зелёная, снизу, висит, пока не уберут.
 * Часы — на завтра в то же время, галочка — выполнено, название — окно задачи поверх, смахнуть вниз — закрыть.
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

/** Полоска одной задачи: название и подпись «Сегодня, 18:00 · Дом». */
@Immutable
data class PopupStrip(val taskId: String, val title: String, val subtitle: String)

@Singleton
class ReminderPopup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val strips = mutableStateListOf<PopupStrip>()
    private var window: Overlay? = null

    /** Полоску можно показать: включена, есть разрешение, экран включён и разблокирован. */
    fun canShow(): Boolean {
        if (!PopupPrefs.enabled(context) || !PopupPrefs.allowed(context)) return false
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return power.isInteractive && !keyguard.isKeyguardLocked
    }

    /** Показывает полоску (повторное напоминание той же задачи — заменяет её) с вибрацией. */
    suspend fun show(strip: PopupStrip) = withContext(Dispatchers.Main.immediate) {
        strips.removeAll { it.taskId == strip.taskId }
        strips += strip
        if (window == null) window = Overlay(context) { Content() }.also { it.attach() }
        vibrate()
    }

    /** Задачи на полосках сейчас, по порядку прихода. */
    fun shown(): List<String> = strips.map { it.taskId }

    fun dismiss(taskId: String) {
        appScope.launch(Dispatchers.Main.immediate) { remove(taskId) }
    }

    private fun remove(taskId: String) {
        strips.removeAll { it.taskId == taskId }
        if (strips.isEmpty()) {
            window?.detach()
            window = null
        }
    }

    @Composable
    private fun Content() {
        AppTheme {
            ReminderStrips(
                strips = strips,
                onDone = { id -> act(id) { tasks.complete(id) } },
                onTomorrow = { id -> act(id) { tasks.moveToTomorrow(id) } },
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

    private fun act(taskId: String, action: suspend () -> Unit) {
        remove(taskId)
        appScope.launch { action() }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(AndroidReminderNotifications.VIBRATION, -1))
    }
}

/**
 * Окно поверх других приложений внизу экрана. Не забирает клавиатуру и нажатия мимо полосок — приложение под ним
 * работает как обычно. Compose в таком окне нужен свой «жизненный цикл» — его даёт [OverlayOwner].
 */
private class Overlay(private val context: Context, private val content: @Composable () -> Unit) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private val owner = OverlayOwner()
    private val view = ComposeView(context).apply {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        setContent { content() }
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
    }

    fun detach() {
        runCatching { manager.removeView(view) }
        owner.stop()
    }
}

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
            key(strip.taskId) { Strip(strip, onDone, onTomorrow, onOpen, onDismiss) }
        }
    }
}

@Composable
private fun Strip(strip: PopupStrip, onDone: (String) -> Unit, onTomorrow: (String) -> Unit, onOpen: (String) -> Unit, onDismiss: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { SizeTokens.POPUP_SWIPE.dp.toPx() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(SizeTokens.POPUP_STRIP.dp)
            // Жест — до сдвига: координаты пальца считаются от неподвижного места, а не от уезжающей полоски.
            .pointerInput(strip.taskId) {
                detectVerticalDragGestures(
                    onDragEnd = { if (offset.value > threshold) onDismiss(strip.taskId) else scope.launch { offset.animateTo(0f) } },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { change, dy ->
                    change.consume()
                    scope.launch { offset.snapTo((offset.value + dy).coerceAtLeast(0f)) }
                }
            }
            .offset { IntOffset(0, offset.value.roundToInt()) }
            .clip(CircleShape)
            .background(colors.accent)
            .padding(start = spacing.md, end = spacing.xs)
            .testTag("$TAG_POPUP:${strip.taskId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(painterResource(R.drawable.ic_tool_reminder), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        Column(Modifier.weight(1f).clickable { onOpen(strip.taskId) }.testTag("$TAG_POPUP_OPEN:${strip.taskId}")) {
            Text(strip.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(strip.subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onAccent.copy(alpha = SUBTITLE_ALPHA), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val tomorrow = stringResource(R.string.popup_tomorrow)
        Box(
            Modifier
                .size(SizeTokens.POPUP_BUTTON.dp)
                .clip(CircleShape)
                .background(colors.onAccent.copy(alpha = BUTTON_ALPHA))
                .clickable { onTomorrow(strip.taskId) }
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
                .clickable { onDone(strip.taskId) }
                .semantics { contentDescription = done }
                .testTag("$TAG_POPUP_DONE:${strip.taskId}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.accent, modifier = Modifier.size(SizeTokens.POPUP_ICON.dp))
        }
    }
}

private const val SUBTITLE_ALPHA = 0.8f
private const val BUTTON_ALPHA = 0.18f

const val TAG_POPUP = "popup"
const val TAG_POPUP_OPEN = "popup_open"
const val TAG_POPUP_TOMORROW = "popup_tomorrow"
const val TAG_POPUP_DONE = "popup_done"
const val TAG_POPUP_MORE = "popup_more"
