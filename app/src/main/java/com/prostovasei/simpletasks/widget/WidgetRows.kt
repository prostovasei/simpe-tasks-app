package com.prostovasei.simpletasks.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import com.prostovasei.simpletasks.ui.common.canvasNodeIcon
import com.prostovasei.simpletasks.ui.common.canvasTextColor
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.designsystem.tokens.TaskRowTokens
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.designsystem.tokens.WidgetCheckTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.ReminderIconMasks
import kotlin.math.roundToInt

/*
 * Строки ленты на обычных элементах Android (итерация 42). Раньше каждая строка собиралась через Glance и весила ~7 КБ:
 * на 100+ задачах лента не доходила до Nova Launcher. Здесь строка — разметка из res/layout/widget_row_*.xml плюс
 * её значения (текст, цвета, видимость), ~1 КБ. Строка всегда пишет свой вид целиком: при прокрутке и обновлении Android
 * перекрашивает уже показанную строку другой задачи, и всё ненаписанное остаётся от неё (0.42.2). Внешний вид — как у строк Glance в TasksWidget.kt (сверка снимками).
 * Цвета: постоянные — ресурсы с тёмной темой (values-night/widget_rows.xml), зависящие от задачи — светлый и тёмный
 * из токенов (setColorInt). Только Android 12+: на старых лента остаётся колонкой Glance без прокрутки.
 */
@RequiresApi(Build.VERSION_CODES.S)
object WidgetRows {
    /** Разных разметок строк: задача и период (каждая — обычная и выполненная), подпись дня, «Просрочено», «сейчас», хвост и две строки недели. */
    const val VIEW_TYPES = 10

    fun views(context: Context, line: WidgetLine, model: WidgetModel, liveClock: Boolean = true): RemoteViews = when (line) {
        is WidgetLine.Overdue -> overdue(context, line, model.filter == WidgetFilter.All)
        is WidgetLine.Title -> title(context, line, model)
        is WidgetLine.Task -> task(context, line, model)
        is WidgetLine.Now -> now(context, line, liveClock, model.filter == WidgetFilter.All)
        WidgetLine.Tail -> RemoteViews(context.packageName, R.layout.widget_row_tail)
    }

    private fun overdue(context: Context, line: WidgetLine.Overdue, flat: Boolean): RemoteViews =
        RemoteViews(context.packageName, if (flat) R.layout.widget_row_overdue_flat else R.layout.widget_row_overdue).apply {
            val entry = line.entry
            // #334: «Просрочено» 12 sp, без стрелки сворачивания (сворачивает нажатие на подпись), число — справа.
            topPadding(context, R.id.overdue_root, line.isFirst)
            setTextViewText(R.id.overdue_text, context.getString(R.string.overdue_title))
            setContentDescription(
                R.id.overdue_toggle,
                context.getString(if (entry.collapsed) R.string.overdue_fold_closed else R.string.overdue_fold_open),
            )
            visible(R.id.overdue_fold, false)
            setTextViewText(R.id.overdue_count, if (entry.count > 0) entry.count.toString() else "")
            setOnClickFillInIntent(R.id.overdue_toggle, fillIn(WidgetClicks.TOGGLE_OVERDUE, null))
            visible(R.id.overdue_move, entry.count > 0)
            setOnClickFillInIntent(R.id.overdue_move, fillIn(if (entry.count > 0) WidgetClicks.MOVE_OVERDUE else WidgetClicks.NONE, null))
        }

    private fun title(context: Context, line: WidgetLine.Title, model: WidgetModel): RemoteViews =
        RemoteViews(context.packageName, if (model.filter == WidgetFilter.All) R.layout.widget_row_title_flat else R.layout.widget_row_title).apply {
            val title = widgetDayTitle(context, line.day, model.today)
            // #334: «вт 25 ноя · Сегодня» одной строкой 12 sp средним серым; прошедший день — красным.
            val color = (if (title.isPast) ColorTokens.overdue else ColorTokens.textSecondary).forTaskCanvas(model.filter == WidgetFilter.All)
            topPadding(context, R.id.title_root, line.isFirst)
            setTextViewText(R.id.title_text, title.relative?.let { "${title.text} · $it" } ?: title.text)
            color(R.id.title_text, "setTextColor", color)
            visible(R.id.title_chip, false)
            val progress = line.progress
            // Число задач дня; у «Сегодня» вместо него прогресс.
            visible(R.id.title_count, progress == null)
            setTextViewText(R.id.title_count, line.count.toString())
            visible(R.id.title_progress, progress != null)
            visible(R.id.title_progress_text, progress != null)
            setProgressBar(R.id.title_progress, PROGRESS_MAX, ((progress?.fraction ?: 0f) * PROGRESS_MAX).roundToInt(), false)
            setTextViewText(R.id.title_progress_text, progress?.let { context.getString(R.string.widget_progress, it.done, it.total) }.orEmpty())
        }

    /**
     * Строка задачи (#334): одна разметка [R.layout.widget_row_line] — кружок 18, эмодзи списка, название 14 sp полужирное, колокольчик 14,
     * время 13 sp справа («14:00–16:00» одной строкой; у просроченной в блоке «Просрочено» — день недели или дата). Высокий приоритет —
     * ярким фиолетовым, выполненные и прошедшие — «ещё светлее» ([TaskRowTokens]). Строка пишет свой вид целиком: лаунчер перекрашивает
     * уже показанные строки других задач.
     */
    private fun task(context: Context, line: WidgetLine.Task, model: WidgetModel): RemoteViews {
        val row = line.row
        val item = row.item
        val done = item.showsDone
        // #342: прошедшая невыполненная не бледнеет — красное время.
        val muted = done
        val flat = model.filter == WidgetFilter.All
        return RemoteViews(context.packageName, R.layout.widget_row_line).apply {
            val text = when {
                muted -> TaskRowTokens.done
                item.priority == Priority.HIGH -> TaskRowTokens.priority
                else -> TaskRowTokens.title
            }
            paint(R.id.row_title, "setTextColor", text, flat)
            setTextViewText(R.id.row_title, item.title)
            setOnClickFillInIntent(R.id.row_plaque, fillIn(WidgetClicks.OPEN, item.taskId))

            // Кружок: у повторяющейся — знак повтора (стрелки-треугольники), у выполненной — заливка с галочкой.
            val ring = when {
                muted || item.priority == Priority.HIGH -> text
                else -> ColorTokens.inactive
            }
            setImageViewResource(
                R.id.row_node,
                when {
                    item.isRecurring -> R.drawable.ic_task_repeat_round
                    done -> R.drawable.ic_widget_node_fill
                    else -> R.drawable.ic_widget_node_ring
                },
            )
            paint(R.id.row_node, "setColorFilter", ring, flat)
            visible(R.id.row_node_check, done && !item.isRecurring)
            setContentDescription(R.id.row_node, if (item.isGhost) null else context.getString(if (done) R.string.task_uncomplete else R.string.widget_complete))
            val check = when {
                item.isGhost -> WidgetClicks.NONE
                done -> WidgetClicks.UNCOMPLETE
                else -> WidgetClicks.COMPLETE
            }
            setOnClickFillInIntent(R.id.row_check, fillIn(check, item.id.takeUnless { item.isGhost }))

            val iconAlpha = if (muted) TaskRowTokens.FADED_ICON_ALPHA else 1f
            setTextViewText(R.id.row_emoji, item.listEmoji)
            paint(R.id.row_emoji, "setTextColor", ColorTokens.textPrimary, flat, iconAlpha)
            val masks = item.reminder?.let { ReminderIconMasks.of(it, (SizeTokens.TASK_BELL * context.resources.displayMetrics.density).roundToInt()) }
            visible(R.id.row_bell, masks != null)
            if (masks != null) {
                setImageViewBitmap(R.id.row_bell_main, masks.bell)
                paint(R.id.row_bell_main, "setColorFilter", ColorTokens.textSecondary, flat, iconAlpha)
                visible(R.id.row_bell_early, masks.early != null)
                masks.early?.let { setImageViewBitmap(R.id.row_bell_early, it) }
            }

            val date = (line.day as? WidgetDay.On)?.date
            val right = when {
                line.overdue && date != null -> WidgetDates.overdueDay(date, model.today)
                row.start == null -> null
                else -> row.end?.let { "${row.start}–$it" } ?: row.start
            }
            visible(R.id.row_time, right != null)
            setTextViewText(R.id.row_time, right.orEmpty())
            paint(R.id.row_time, "setTextColor", if ((line.overdue || row.past) && !done) ColorTokens.overdue else text, flat)
        }
    }

    /** Цвет строки: на холсте «Все» — всегда светлый, в остальных лентах — по теме системы. */
    private fun RemoteViews.paint(id: Int, method: String, color: ThemedColor, flat: Boolean, alpha: Float = 1f) =
        if (flat) taskCanvasColor(id, method, color, alpha) else color(id, method, color, alpha)

    private fun now(context: Context, line: WidgetLine.Now, liveClock: Boolean, flat: Boolean): RemoteViews =
        RemoteViews(context.packageName, if (flat) R.layout.widget_row_now_flat else R.layout.widget_row_now).apply {
            if (flat) setViewLayoutWidth(R.id.now_time, canvasTimeWidthPx(context).toFloat(), TypedValue.COMPLEX_UNIT_PX)
            setViewVisibility(R.id.now_line_top, View.INVISIBLE /* #331: без линии */)
            setViewVisibility(R.id.now_line_bottom, View.INVISIBLE)
            // В снимках для тестов — неподвижное время из модели вместо часов системы.
            val fixed = line.nowMinute?.takeIf { !liveClock }
            visible(R.id.now_clock, liveClock)
            visible(R.id.now_fixed, fixed != null)
            fixed?.let { setTextViewText(R.id.now_fixed, "%02d:%02d".format(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR)) }
        }

    /** Видимость пишется всегда: Android перекрашивает уже показанные строки, и прошлое значение иначе остаётся. */
    internal fun RemoteViews.visible(id: Int, shown: Boolean) = setViewVisibility(id, if (shown) View.VISIBLE else View.GONE)

    internal fun ring(priority: Priority): ThemedColor = when (priority) {
        Priority.HIGH -> WidgetCheckTokens.high.ring
        Priority.NONE -> WidgetCheckTokens.none.ring
    }

    /** Заливка кружка выполненной задачи (итерация 47): у высокого приоритета — тусклый фиолетовый. */
    internal fun doneFill(priority: Priority): ThemedColor = when (priority) {
        Priority.HIGH -> WidgetCheckTokens.high.done
        Priority.NONE -> WidgetCheckTokens.none.done
    }

    /** Цвет темы: Android сам берёт светлый или тёмный по теме системы. */
    internal fun RemoteViews.color(id: Int, method: String, color: ThemedColor, alpha: Float = 1f) =
        setColorInt(id, method, argb(color.light, alpha), argb(color.dark, alpha))

    internal fun RemoteViews.taskCanvasColor(id: Int, method: String, color: ThemedColor, alpha: Float = 1f) =
        setColorInt(id, method, argb(color.light, alpha), argb(color.light, alpha))

    private fun ThemedColor.forTaskCanvas(flat: Boolean) = if (flat) ThemedColor(light, light) else this

    private fun argb(color: Long, alpha: Float): Int = Color(color).let { it.copy(alpha = it.alpha * alpha) }.toArgb()

    /** Верхний отступ подписи: у первой — меньше, лента начинается сразу под краем виджета. */
    private fun RemoteViews.topPadding(context: Context, id: Int, isFirst: Boolean) {
        val res = context.resources
        setViewPadding(
            id,
            res.getDimensionPixelSize(R.dimen.widget_edge),
            res.getDimensionPixelSize(if (isFirst) R.dimen.widget_day_gap else R.dimen.widget_edge),
            if (id == R.id.title_root) res.getDimensionPixelSize(R.dimen.widget_list_edge) else 0,
            res.getDimensionPixelSize(R.dimen.widget_day_gap),
        )
    }

    /**
     * Добавка к общему шаблону нажатий списка — только короткий адрес «wt:<что>/<задача>»: таких добавок в ленте по две
     * на строку, и каждый лишний байт умножается на сотню строк. Разбирает [WidgetClicks.parse] в [WidgetClickActivity].
     */
    fun fillIn(code: String, taskId: String?): Intent = Intent().setData(WidgetClicks.uri(code, taskId))

    private const val PROGRESS_MAX = 1000
    private const val OVERDUE_TIME_SP = 11f
    private const val TIME_SP = 13f
    private const val MINUTES_PER_HOUR = 60
}
