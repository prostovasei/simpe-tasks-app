package com.prostovasei.simpletasks.widget

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WEEK_CALENDAR_ROWS
import com.prostovasei.simpletasks.ui.feature.week.WEEK_LENGTH
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import com.prostovasei.simpletasks.ui.feature.week.startTime
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import com.prostovasei.simpletasks.widget.WidgetRows.color
import com.prostovasei.simpletasks.widget.WidgetRows.fillIn
import com.prostovasei.simpletasks.widget.WidgetRows.ring
import com.prostovasei.simpletasks.widget.WidgetRows.visible
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * Вид «Неделя» в виджете (итерация 45, макет `VariantK1`): четыре строки ленты одинаковой высоты на весь виджет над нижней панелью —
 * Пн–Вт, Ср–Чт, Пт–Сб, Вс и мини-календарь. Каждая строка пишет свой вид целиком (Android перекрашивает показанные строки, см. WidgetRows).
 * Ячейка: строка даты («пн 26», у сегодня — метка, справа «+N» — не поместившиеся задачи; нажатие открывает день в приложении) и плашки
 * задач: кружок (нажатие — «выполнено»), название, время. Плашек в ячейке столько, сколько влезает по высоте. Разметка — `widget_week_*`,
 * `widget_row_week*` (генерирует design/tools/gen_widget_week.py), id — WidgetWeekIds.
 */
@RequiresApi(Build.VERSION_CODES.S)
object WidgetWeekRows {
    /** Строк недели в ленте: шесть дней по два, воскресенье с мини-календарём. */
    const val ROWS = 4

    private val RU: Locale = Locale.forLanguageTag("ru")

    /** Высота строки недели: виджет без верхнего поля и нижней панели делится на [ROWS] поровну. */
    fun rowHeightDp(size: DpSize): Float {
        val height = size.height.value.takeIf { !it.isNaN() && it > 0f } ?: return SizeTokens.WIDGET_WEEK_ROW_DEFAULT.toFloat()
        return ((height - SizeTokens.WIDGET_EDGE - SizeTokens.WIDGET_BAR) / ROWS).coerceAtLeast(SizeTokens.WIDGET_WEEK_ROW_MIN.toFloat())
    }

    /** Сколько плашек влезает в ячейку высотой [rowHeight]. */
    fun capacity(rowHeight: Float): Int {
        val room = rowHeight - 2 * SpacingTokens.XS - SizeTokens.WIDGET_WEEK_HEADER
        return (room / (SizeTokens.WIDGET_WEEK_TASK + SpacingTokens.XXS)).toInt().coerceIn(0, WeekIds.SLOTS)
    }

    fun views(context: Context, week: WidgetWeek, size: DpSize): List<RemoteViews> {
        val height = rowHeightDp(size)
        val capacity = capacity(height)
        val today = week.today
        return (0 until ROWS).map { row ->
            val left = week.days[row * 2]
            val right = week.days.getOrNull(row * 2 + 1)
            RemoteViews(context.packageName, if (right == null) R.layout.widget_row_week_last else R.layout.widget_row_week).apply {
                setViewLayoutHeight(R.id.week_row, height, TypedValue.COMPLEX_UNIT_DIP)
                cell(context, WeekIds.LEFT, left, capacity, today)
                if (right != null) cell(context, WeekIds.RIGHT, right, capacity, today) else calendar(context, week, height)
            }
        }
    }

    private fun RemoteViews.cell(context: Context, ids: CellIds, day: WeekDay, capacity: Int, today: LocalDate) {
        val isToday = day.date == today
        val shown = day.items.take(capacity)
        val hidden = day.items.size - shown.size
        setTextViewText(ids.weekday, day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU))
        color(ids.weekday, "setTextColor", if (isToday) ColorTokens.accent else ColorTokens.textPrimary)
        setTextViewText(ids.number, day.date.dayOfMonth.toString())
        color(ids.number, "setTextColor", if (isToday) ColorTokens.onAccent else ColorTokens.textSecondary)
        // Метка «сегодня» у числа: фон accent и поля; у остальных дней — без фона и полей.
        setInt(ids.number, "setBackgroundResource", if (isToday) R.drawable.widget_week_today else 0)
        val sidePad = if (isToday) dp(context, SpacingTokens.XS) else 0
        setViewPadding(ids.number, sidePad, 0, sidePad, 0)
        visible(ids.more, hidden > 0)
        setTextViewText(ids.more, "+$hidden")
        setOnClickFillInIntent(ids.head, fillIn(WidgetClicks.DAY, day.date.toString()))
        ids.slots.forEachIndexed { index, slot ->
            val item = shown.getOrNull(index)
            visible(slot.plaque, item != null)
            if (item != null) slot(context, slot, item, overdue = day.date < today && !item.showsDone)
        }
    }

    /** Плашка задачи в ячейке; всё, что зависит от задачи, пишется заново. */
    private fun RemoteViews.slot(context: Context, ids: SlotIds, item: TaskItem, overdue: Boolean) {
        val done = item.showsDone
        val ringColor = when {
            item.isGhost && !done -> ColorTokens.inactive
            else -> ring(item.priority)
        }
        setFloat(ids.plaque, "setAlpha", if (item.isGhost) GHOST_ALPHA else if (done) DONE_ALPHA else 1f)
        setImageViewResource(ids.node, if (done) R.drawable.ic_widget_node_fill else R.drawable.ic_widget_node_ring)
        color(ids.node, "setColorFilter", ringColor)
        visible(ids.tick, done)
        setContentDescription(
            ids.node,
            when {
                item.isGhost -> null
                done -> context.getString(R.string.task_uncomplete)
                else -> context.getString(R.string.widget_complete)
            },
        )
        setOnClickFillInIntent(
            ids.check,
            fillIn(
                when {
                    item.isGhost -> WidgetClicks.NONE
                    done -> WidgetClicks.UNCOMPLETE
                    else -> WidgetClicks.COMPLETE
                },
                item.id.takeUnless { item.isGhost },
            ),
        )
        setTextViewText(
            ids.title,
            if (done) SpannableString(item.title).apply { setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else item.title,
        )
        color(ids.title, "setTextColor", if (item.isGhost) ColorTokens.textSecondary else ColorTokens.textPrimary)
        val time = item.startTime()
        visible(ids.time, time != null)
        setTextViewText(ids.time, time.orEmpty())
        color(ids.time, "setTextColor", if (overdue) ColorTokens.overdue else ColorTokens.textSecondary)
        // Без времени название доходит до края плашки — нужен правый отступ.
        setViewPadding(ids.plaque, 0, 0, if (time == null) dp(context, SpacingTokens.XS) else 0, 0)
        setOnClickFillInIntent(ids.plaque, fillIn(WidgetClicks.OPEN, item.taskId))
    }

    /** Мини-календарь в последней ячейке: месяц со стрелками, «пн…вс», шесть строк недель; нажатие на строку выбирает неделю. */
    private fun RemoteViews.calendar(context: Context, week: WidgetWeek, rowHeight: Float) {
        val month: YearMonth = week.month
        setTextViewText(WeekIds.CAL_TITLE, "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, RU)} ${month.year}")
        setOnClickFillInIntent(WeekIds.CAL_PREV, fillIn(WidgetClicks.PREV_MONTH, null))
        setOnClickFillInIntent(WeekIds.CAL_NEXT, fillIn(WidgetClicks.NEXT_MONTH, null))
        DayOfWeek.entries.forEachIndexed { index, day -> setTextViewText(WeekIds.CAL_WEEKDAYS[index], day.getDisplayName(TextStyle.SHORT, RU)) }
        // Строки недель делят высоту под заголовком; ячейка ниже — календарь слегка обрезается.
        val room = rowHeight - 2 * SpacingTokens.XXS - SizeTokens.WIDGET_WEEK_CAL_HEAD - SizeTokens.WIDGET_WEEK_CAL_WEEKDAYS
        val weekRow = (room / WEEK_CALENDAR_ROWS).coerceIn(SizeTokens.WIDGET_WEEK_CAL_DAY.toFloat(), SizeTokens.WIDGET_WEEK_CAL_ROW_MAX.toFloat())
        val first = month.atDay(1).weekStart()
        WeekIds.CAL_ROWS.forEachIndexed { index, row ->
            val start = first.plusDays(index * WEEK_LENGTH.toLong())
            setViewLayoutHeight(row.row, weekRow, TypedValue.COMPLEX_UNIT_DIP)
            setInt(row.row, "setBackgroundResource", if (start == week.weekStart) R.drawable.widget_week_band else 0)
            setOnClickFillInIntent(row.row, fillIn(WidgetClicks.PICK_WEEK, start.toString()))
            row.days.forEachIndexed { offset, id ->
                val date = start.plusDays(offset.toLong())
                val isToday = date == week.today
                setTextViewText(id, date.dayOfMonth.toString())
                color(id, "setTextColor", calendarDayColor(isToday, inMonth = YearMonth.from(date) == month))
                setInt(id, "setBackgroundResource", if (isToday) R.drawable.widget_week_today else 0)
            }
        }
    }

    private fun calendarDayColor(isToday: Boolean, inMonth: Boolean): ThemedColor = when {
        isToday -> ColorTokens.onAccent
        inMonth -> ColorTokens.textPrimary
        else -> ColorTokens.inactive
    }

    private fun dp(context: Context, value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    /** Будущий повтор — наполовину прозрачный, как в приложении. */
    private const val GHOST_ALPHA = 0.5f
}
