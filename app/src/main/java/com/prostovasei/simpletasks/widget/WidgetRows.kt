package com.prostovasei.simpletasks.widget

import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.designsystem.tokens.WidgetCheckTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.ReminderIconMasks
import kotlin.math.roundToInt

/*
 * Строки ленты на обычных элементах Android (итерация 42). Раньше каждая строка собиралась через Glance и весила ~7 КБ:
 * на 100+ задачах лента не доходила до Nova Launcher. Здесь строка — разметка из res/layout/widget_row_*.xml плюс
 * несколько значений (текст, цвета, видимость), ~1 КБ. Внешний вид — как у строк Glance в TasksWidget.kt (сверка снимками).
 * Цвета: постоянные — ресурсы с тёмной темой (values-night/widget_rows.xml), зависящие от задачи — светлый и тёмный
 * из токенов (setColorInt). Только Android 12+: на старых лента остаётся колонкой Glance без прокрутки.
 */
@RequiresApi(Build.VERSION_CODES.S)
object WidgetRows {
    /** Разных разметок строк: задача, период, подпись дня, «Просрочено», «сейчас», хвост. */
    const val VIEW_TYPES = 6

    fun views(context: Context, line: WidgetLine, model: WidgetModel, liveClock: Boolean = true): RemoteViews = when (line) {
        is WidgetLine.Overdue -> overdue(context, line)
        is WidgetLine.Title -> title(context, line, model)
        is WidgetLine.Task -> task(context, line, model.filter)
        is WidgetLine.Now -> now(context, line, liveClock)
        WidgetLine.Tail -> RemoteViews(context.packageName, R.layout.widget_row_tail)
    }

    private fun overdue(context: Context, line: WidgetLine.Overdue): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_row_overdue).apply {
            val entry = line.entry
            val text = if (entry.count > 0) context.getString(R.string.overdue_count, entry.count) else context.getString(R.string.overdue_title)
            if (line.isFirst) topPadding(context, R.id.overdue_root, R.dimen.widget_day_gap)
            setTextViewText(R.id.overdue_text, text)
            setImageViewResource(R.id.overdue_fold, if (entry.collapsed) R.drawable.ic_fold_closed else R.drawable.ic_fold_open)
            setContentDescription(
                R.id.overdue_fold,
                context.getString(if (entry.collapsed) R.string.overdue_fold_closed else R.string.overdue_fold_open),
            )
            setOnClickFillInIntent(R.id.overdue_toggle, fillIn(WidgetClicks.TOGGLE_OVERDUE, null))
            if (entry.count > 0) {
                setOnClickFillInIntent(R.id.overdue_move, fillIn(WidgetClicks.MOVE_OVERDUE, null))
            } else {
                setViewVisibility(R.id.overdue_move, View.GONE)
            }
        }

    private fun title(context: Context, line: WidgetLine.Title, model: WidgetModel): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_row_title).apply {
            val title = widgetDayTitle(context, line.day, model.today)
            if (line.isFirst) topPadding(context, R.id.title_root, R.dimen.widget_day_gap)
            setTextViewText(R.id.title_text, title.text)
            // Прошедший день — красным; обычный цвет уже в разметке.
            if (title.isPast) color(R.id.title_text, "setTextColor", ColorTokens.overdue)
            title.relative?.let {
                setViewVisibility(R.id.title_chip, View.VISIBLE)
                setTextViewText(R.id.title_chip, it)
                if (title.isPast) color(R.id.title_chip, "setTextColor", ColorTokens.overdue)
            }
            line.progress?.let { progress ->
                setViewVisibility(R.id.title_progress, View.VISIBLE)
                setProgressBar(R.id.title_progress, PROGRESS_MAX, (progress.fraction * PROGRESS_MAX).roundToInt(), false)
                setViewVisibility(R.id.title_progress_text, View.VISIBLE)
                setTextViewText(R.id.title_progress_text, context.getString(R.string.widget_progress, progress.done, progress.total))
            }
        }

    private fun task(context: Context, line: WidgetLine.Task, filter: WidgetFilter): RemoteViews {
        val row = line.row
        val item = row.item
        val done = item.showsDone
        val faded = row.past || item.isGhost
        val layout = if (row.isPeriod) R.layout.widget_row_period else R.layout.widget_row_task
        return RemoteViews(context.packageName, layout).apply {
            // Цвет текста: обычный (textPrimary) — уже в разметке, здесь только отличия; так пакет легче.
            val textColor = when {
                done -> ColorTokens.textPrimary to DONE_ALPHA
                faded -> ColorTokens.textSecondary to 1f
                else -> null
            }
            // Время: красное и мельче у прошедшего дня; конец периода — серым.
            row.start?.let {
                setViewVisibility(R.id.row_start, View.VISIBLE)
                setTextViewText(R.id.row_start, it)
                if (line.overdue) overdueTime(R.id.row_start) else textColor?.let { (c, a) -> color(R.id.row_start, "setTextColor", c, a) }
            }
            row.end?.let {
                setViewVisibility(R.id.row_end, View.VISIBLE)
                setTextViewText(R.id.row_end, it)
                if (line.overdue) overdueTime(R.id.row_end) else color(R.id.row_end, "setTextColor", ColorTokens.textSecondary)
                if (row.isPeriod) setViewVisibility(R.id.row_time_gap, View.VISIBLE)
            }

            // Шкала: линия вверх и вниз, кружок цвета приоритета (у прошедшей и будущего повтора — серый), выполненная — залита с галочкой.
            if (!line.line.top) setViewVisibility(R.id.row_line_top, View.INVISIBLE)
            if (!line.line.bottom) setViewVisibility(R.id.row_line_bottom, View.INVISIBLE)
            // Пустой кружок и подпись «Выполнить» — уже в разметке; у выполненной — залитый с галочкой.
            if (done) setImageViewResource(R.id.row_node, if (row.isPeriod) R.drawable.widget_period_fill else R.drawable.ic_widget_node_fill)
            // Серый кружок без приоритета — уже в разметке.
            when {
                faded && !done -> color(R.id.row_node, "setColorFilter", ColorTokens.inactive)
                item.priority != Priority.NONE -> color(R.id.row_node, "setColorFilter", ring(item.priority))
            }
            if (done) setViewVisibility(R.id.row_node_check, View.VISIBLE)
            if (item.isGhost) {
                setContentDescription(R.id.row_node, null)
            } else {
                if (done) setContentDescription(R.id.row_node, context.getString(R.string.task_uncomplete))
                setOnClickFillInIntent(R.id.row_check, fillIn(if (done) WidgetClicks.UNCOMPLETE else WidgetClicks.COMPLETE, item.id))
            }

            // Плашка: название (выполненная — зачёркнута и бледнее), значки справа налево — список, колокольчик, повтор.
            setTextViewText(
                R.id.row_title,
                if (done) SpannableString(item.title).apply { setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else item.title,
            )
            textColor?.let { (c, a) -> color(R.id.row_title, "setTextColor", c, a) }
            setOnClickFillInIntent(R.id.row_plaque, fillIn(WidgetClicks.OPEN, item.taskId))
            // Значки серые и зелёные — уже в разметке; у выполненной — бледнее.
            val iconAlpha = if (done) DONE_ALPHA else 1f
            if (item.isRecurring) {
                setViewVisibility(R.id.row_repeat, View.VISIBLE)
                if (done) color(R.id.row_repeat, "setColorFilter", ColorTokens.textSecondary, iconAlpha)
            }
            item.reminder?.let { mark ->
                val sizePx = (SizeTokens.WIDGET_REMINDER_ICON * context.resources.displayMetrics.density).roundToInt()
                val masks = ReminderIconMasks.of(mark, sizePx)
                setViewVisibility(R.id.row_bell, View.VISIBLE)
                setImageViewBitmap(R.id.row_bell_main, masks.bell)
                if (done) color(R.id.row_bell_main, "setColorFilter", ColorTokens.textSecondary, iconAlpha)
                masks.early?.let {
                    setViewVisibility(R.id.row_bell_early, View.VISIBLE)
                    setImageViewBitmap(R.id.row_bell_early, it)
                    if (done) color(R.id.row_bell_early, "setColorFilter", ColorTokens.accent, iconAlpha)
                }
            }
            if (filter is WidgetFilter.ProjectList) {
                setViewVisibility(R.id.row_square, View.GONE)
            } else {
                val listColor = parseListColor(item.listColor)
                when {
                    item.isInboxList -> color(R.id.row_square_bg, "setColorFilter", ColorTokens.inboxList)
                    // Цвет списка одинаков в светлой и тёмной теме.
                    listColor != null -> setInt(R.id.row_square_bg, "setColorFilter", listColor.toArgb())
                    else -> color(R.id.row_square_bg, "setColorFilter", ColorTokens.inactive)
                }
                // У выполненной квадратик бледнее — просвечивает плашка, как у Glance.
                if (done) setInt(R.id.row_square_bg, "setImageAlpha", (DONE_ALPHA * OPAQUE).roundToInt())
                ListIcons.drawable(item.listIcon)?.let { setImageViewResource(R.id.row_square_glyph, it) }
                    ?: setViewVisibility(R.id.row_square_glyph, View.GONE)
            }
        }
    }

    private fun RemoteViews.overdueTime(id: Int) {
        color(id, "setTextColor", ColorTokens.overdue)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, OVERDUE_TIME_SP)
    }

    private fun now(context: Context, line: WidgetLine.Now, liveClock: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_row_now).apply {
            if (!line.line.top) setViewVisibility(R.id.now_line_top, View.INVISIBLE)
            if (!line.line.bottom) setViewVisibility(R.id.now_line_bottom, View.INVISIBLE)
            // В снимках для тестов — неподвижное время из модели вместо часов системы.
            if (!liveClock) {
                setViewVisibility(R.id.now_clock, View.GONE)
                line.nowMinute?.let {
                    setViewVisibility(R.id.now_fixed, View.VISIBLE)
                    setTextViewText(R.id.now_fixed, "%02d:%02d".format(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR))
                }
            }
        }

    private fun ring(priority: Priority): ThemedColor = when (priority) {
        Priority.HIGH -> WidgetCheckTokens.high.ring
        Priority.MEDIUM -> WidgetCheckTokens.medium.ring
        Priority.LOW -> WidgetCheckTokens.low.ring
        Priority.NONE -> WidgetCheckTokens.none.ring
    }

    /** Цвет темы: Android сам берёт светлый или тёмный по теме системы. */
    private fun RemoteViews.color(id: Int, method: String, color: ThemedColor, alpha: Float = 1f) =
        setColorInt(id, method, argb(color.light, alpha), argb(color.dark, alpha))

    private fun argb(color: Long, alpha: Float): Int = Color(color).let { it.copy(alpha = it.alpha * alpha) }.toArgb()

    /** Верхний отступ первой подписи — меньше: лента начинается сразу под краем виджета. */
    private fun RemoteViews.topPadding(context: Context, id: Int, dimen: Int) {
        val res = context.resources
        setViewPadding(
            id,
            res.getDimensionPixelSize(R.dimen.widget_edge),
            res.getDimensionPixelSize(dimen),
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
    private const val OPAQUE = 255
    private const val OVERDUE_TIME_SP = 11f
    private const val MINUTES_PER_HOUR = 60
}
