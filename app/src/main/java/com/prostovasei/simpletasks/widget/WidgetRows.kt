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
            topPadding(context, R.id.overdue_root, line.isFirst)
            setTextViewText(R.id.overdue_text, text)
            setImageViewResource(R.id.overdue_fold, if (entry.collapsed) R.drawable.ic_fold_closed else R.drawable.ic_fold_open)
            setContentDescription(
                R.id.overdue_fold,
                context.getString(if (entry.collapsed) R.string.overdue_fold_closed else R.string.overdue_fold_open),
            )
            setOnClickFillInIntent(R.id.overdue_toggle, fillIn(WidgetClicks.TOGGLE_OVERDUE, null))
            visible(R.id.overdue_move, entry.count > 0)
            setOnClickFillInIntent(R.id.overdue_move, fillIn(if (entry.count > 0) WidgetClicks.MOVE_OVERDUE else WidgetClicks.NONE, null))
        }

    private fun title(context: Context, line: WidgetLine.Title, model: WidgetModel): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_row_title).apply {
            val title = widgetDayTitle(context, line.day, model.today)
            // Прошедший день — красным.
            val color = if (title.isPast) ColorTokens.overdue else ColorTokens.textPrimary
            topPadding(context, R.id.title_root, line.isFirst)
            setTextViewText(R.id.title_text, title.text)
            color(R.id.title_text, "setTextColor", color)
            visible(R.id.title_chip, title.relative != null)
            setTextViewText(R.id.title_chip, title.relative.orEmpty())
            color(R.id.title_chip, "setTextColor", color)
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
     * Строка задачи. У выполненной своя разметка (*_done): залитый кружок с галочкой, «Снять отметку», бледные название,
     * значки и квадратик — уже в ней. Так строки выполненных и невыполненных не перекрашиваются друг в друга, и в пакете
     * нет этих значений у каждой строки.
     */
    private fun task(context: Context, line: WidgetLine.Task, filter: WidgetFilter): RemoteViews {
        val row = line.row
        val item = row.item
        val done = item.showsDone
        // Итерация 47: будущий повтор не тусклый — тусклая только прошедшая задача.
        val faded = row.past
        val layout = when {
            row.isPeriod && done -> R.layout.widget_row_period_done
            row.isPeriod -> R.layout.widget_row_period
            done -> R.layout.widget_row_task_done
            else -> R.layout.widget_row_task
        }
        return RemoteViews(context.packageName, layout).apply {
            val (textColor, textAlpha) = when {
                done -> ColorTokens.textPrimary to DONE_ALPHA
                faded -> ColorTokens.textSecondary to 1f
                else -> ColorTokens.textPrimary to 1f
            }
            // Время: красное и мельче у прошедшего дня; конец периода — серым.
            time(R.id.row_start, row.start, if (line.overdue) ColorTokens.overdue else textColor, if (line.overdue) 1f else textAlpha, line.overdue)
            time(R.id.row_end, row.end, if (line.overdue) ColorTokens.overdue else ColorTokens.textSecondary, 1f, line.overdue)
            if (row.isPeriod) visible(R.id.row_time_gap, row.end != null)

            // Шкала: линия вверх и вниз, кружок цвета приоритета (у прошедшей — серый). Итерация 47: у повторяющейся — знак повтора
            // вместо кружка (у периода — вытянутый), выполненная высокого приоритета — тусклый фиолетовый.
            setViewVisibility(R.id.row_line_top, if (line.line.top) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(R.id.row_line_bottom, if (line.line.bottom) View.VISIBLE else View.INVISIBLE)
            color(
                R.id.row_node,
                "setColorFilter",
                when {
                    faded && !done -> ColorTokens.inactive
                    done -> doneFill(item.priority)
                    else -> ring(item.priority)
                },
            )
            // Проверка PM (0.47.0): у выполненной повторяющейся галочки нет — знак повтора остаётся тусклым. Рисунок и галочка
            // пишутся у каждой строки: строки одного макета переиспользуются лаунчером.
            setImageViewResource(
                R.id.row_node,
                when {
                    row.isPeriod && item.isRecurring -> R.drawable.ic_widget_period_repeat
                    row.isPeriod -> if (done) R.drawable.widget_period_fill else R.drawable.widget_period_ring
                    item.isRecurring -> R.drawable.ic_repeat_round
                    else -> if (done) R.drawable.ic_widget_node_fill else R.drawable.ic_widget_node_ring
                },
            )
            if (done) visible(R.id.row_node_check, !item.isRecurring)
            // У будущего повтора кружок ничего не отмечает (как раньше): пустое нажатие и без подписи.
            if (!done) setContentDescription(R.id.row_node, if (item.isGhost) null else context.getString(R.string.widget_complete))
            val check = when {
                item.isGhost -> WidgetClicks.NONE
                done -> WidgetClicks.UNCOMPLETE
                else -> WidgetClicks.COMPLETE
            }
            setOnClickFillInIntent(R.id.row_check, fillIn(check, item.id.takeUnless { item.isGhost }))

            // Плашка: название (выполненная — зачёркнута), значки справа налево — список, колокольчик.
            setTextViewText(
                R.id.row_title,
                if (done) SpannableString(item.title).apply { setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else item.title,
            )
            if (!done) color(R.id.row_title, "setTextColor", textColor)
            setOnClickFillInIntent(R.id.row_plaque, fillIn(WidgetClicks.OPEN, item.taskId))
            val masks = item.reminder?.let { ReminderIconMasks.of(it, (SizeTokens.WIDGET_REMINDER_ICON * context.resources.displayMetrics.density).roundToInt()) }
            visible(R.id.row_bell, masks != null)
            if (masks != null) {
                setImageViewBitmap(R.id.row_bell_main, masks.bell)
                visible(R.id.row_bell_early, masks.early != null)
                masks.early?.let { setImageViewBitmap(R.id.row_bell_early, it) }
            }
            // Эмодзи списка (итерация 47) вместо цветного квадратика со значком; в виджете одного списка не нужен.
            visible(R.id.row_emoji, filter !is WidgetFilter.ProjectList)
            setTextViewText(R.id.row_emoji, item.listEmoji)
        }
    }

    private fun RemoteViews.time(id: Int, text: String?, color: ThemedColor, alpha: Float, small: Boolean) {
        visible(id, text != null)
        if (text == null) return
        setTextViewText(id, text)
        color(id, "setTextColor", color, alpha)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, if (small) OVERDUE_TIME_SP else TIME_SP)
    }

    private fun now(context: Context, line: WidgetLine.Now, liveClock: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_row_now).apply {
            setViewVisibility(R.id.now_line_top, if (line.line.top) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(R.id.now_line_bottom, if (line.line.bottom) View.VISIBLE else View.INVISIBLE)
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
