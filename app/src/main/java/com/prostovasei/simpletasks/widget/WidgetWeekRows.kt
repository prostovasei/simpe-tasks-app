package com.prostovasei.simpletasks.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WEEK_LENGTH
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import com.prostovasei.simpletasks.ui.feature.week.startTime
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import com.prostovasei.simpletasks.widget.WidgetRows.taskCanvasColor
import com.prostovasei.simpletasks.widget.WidgetRows.visible
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * Вид «Неделя» в виджете (итерация 45, макет `VariantK1`; 0.46.0 — `VariantK2`: компактные строки с фоном по приоритету, до 6 в ячейке): четыре строки одинаковой высоты на весь виджет над нижней панелью
 * (0.46.0: не лента, а вертикальный LinearLayout с долями высоты — `widget_root_week`) —
 * Пн–Вт, Ср–Чт, Пт–Сб, Вс и мини-календарь. Каждая строка пишет свой вид целиком (Android перекрашивает показанные строки, см. WidgetRows).
 * Ячейка: строка даты («пн 26», у сегодня — метка, справа «+N» — не поместившиеся задачи; нажатие открывает день в приложении) и плашки
 * задач: эмодзи, название, начало справа. Любая область ячейки открывает день. Плашек в ячейке столько, сколько влезает по высоте. Разметка — `widget_week_*`,
 * `widget_row_week*` (генерирует design/tools/gen_widget_week.py), id — WidgetWeekIds.
 */
@RequiresApi(Build.VERSION_CODES.S)
object WidgetWeekRows {
    /** Строк недели: шесть дней по два, воскресенье с мини-календарём. */
    const val ROWS = 4

    /**
     * Нажатие на элемент (0.46.0): строки недели больше не лента, поэтому вместо добавки к общему шаблону у каждого элемента своё
     * намерение ([WidgetClicks.pending]).
     */
    fun interface Click {
        fun RemoteViews.set(viewId: Int, code: String, id: String?)
    }

    private val RU: Locale = Locale.forLanguageTag("ru")

    /**
     * Высота строки недели по размеру от рабочего стола — только для [capacity]: сами строки делят настоящую высоту виджета поровну
     * (weight, `widget_root_week`). Рабочий стол бывает щедрее настоящего размера; строки задач тогда чуть ниже, но всё помещается.
     */
    fun rowHeightDp(size: DpSize): Float {
        val height = size.height.value.takeIf { !it.isNaN() && it > 0f } ?: return SizeTokens.WIDGET_WEEK_ROW_DEFAULT.toFloat()
        return ((height - SizeTokens.WIDGET_WEEK_TOP - SizeTokens.WIDGET_BAR) / ROWS).coerceAtLeast(SizeTokens.WIDGET_WEEK_ROW_MIN.toFloat())
    }

    /**
     * Сколько строк задач влезает в ячейку высотой [rowHeight] (поле сверху, строка даты, строки с промежутком над каждой) — по наименьшей
     * читаемой строке `WIDGET_WEEK_TASK`: сами строки делят высоту поровну и выходят выше. Решение PM — до 6 в ячейке. Строка даты
     * (итерация 48) — по высоте текста, здесь считается по наименьшей `WIDGET_WEEK_HEADER`.
     */
    fun capacity(rowHeight: Float): Int {
        val room = rowHeight - SizeTokens.WIDGET_WEEK_CELL_TOP - SizeTokens.WIDGET_WEEK_HEADER
        return (room / (SizeTokens.WIDGET_WEEK_TASK + SizeTokens.WIDGET_WEEK_TASK_GAP)).toInt().coerceIn(0, WeekIds.SLOTS)
    }

    fun views(context: Context, week: WidgetWeek, size: DpSize, click: Click): List<RemoteViews> {
        val capacity = capacity(rowHeightDp(size))
        val today = week.today
        return (0 until ROWS).map { row ->
            val left = week.days[row * 2]
            val right = week.days.getOrNull(row * 2 + 1)
            RemoteViews(context.packageName, if (right == null) R.layout.widget_row_week_last else R.layout.widget_row_week).apply {
                cell(context, WeekIds.LEFT, left, capacity, today, click)
                if (right != null) cell(context, WeekIds.RIGHT, right, capacity, today, click) else calendar(week, click)
            }
        }
    }

    private fun RemoteViews.cell(context: Context, ids: CellIds, day: WeekDay, capacity: Int, today: LocalDate, click: Click) {
        val isToday = day.date == today
        val shown = day.items.take(capacity)
        val hidden = day.items.size - shown.size
        // Сегодня (0.46.0, проверка PM): без кружка — день недели и число жирные и основного цвета. Итерация 47: остальные дни —
        // день недели и число одним нежирным начертанием (Medium) и серые. Жирность — через начертание в тексте: у RemoteViews нет смены шрифта.
        // Итерация 48 (решение PM, макет `week_tune.py`): день недели капсом («ПТ 6»), остальные дни — обычным начертанием, мельче.
        setTextViewText(ids.weekday, day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).uppercase(RU).bold(isToday))
        taskCanvasColor(ids.weekday, "setTextColor", if (isToday) ColorTokens.textPrimary else ColorTokens.textSecondary)
        val number = day.date.dayOfMonth.toString() + if (isToday) " ${context.getString(R.string.day_today)}" else ""
        setTextViewText(ids.number, number.bold(isToday))
        taskCanvasColor(ids.number, "setTextColor", if (isToday) ColorTokens.textPrimary else ColorTokens.textSecondary)
        visible(ids.more, hidden > 0)
        setTextViewText(ids.more, "+$hidden")
        taskCanvasColor(ids.more, "setTextColor", ColorTokens.textPrimary)
        setInt(ids.more, "setBackgroundResource", 0)
        with(click) {
            set(ids.root, WidgetClicks.DAY, day.date.toString())
            set(ids.head, WidgetClicks.DAY, day.date.toString())
        }
        ids.slots.forEachIndexed { index, slot ->
            val item = shown.getOrNull(index)
            visible(slot.plaque, index < capacity)
            // Every property is reset for launcher reapply, including empty slots.
            setViewVisibility(slot.title, if (item != null) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(slot.emoji, if (item != null) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(slot.time, if (item?.startTime() != null) View.VISIBLE else View.GONE)
            setInt(slot.plaque, "setBackgroundResource", 0)
            setTextViewText(slot.title, item?.title.orEmpty().weekWeight(item?.priority == Priority.HIGH && !item.showsDone))
            setTextViewText(slot.emoji, item?.listEmoji.orEmpty())
            setTextViewText(slot.time, item?.startTime().orEmpty().weekWeight(item?.priority == Priority.HIGH && !item.showsDone))
            val color = when {
                item?.priority == Priority.HIGH && item.showsDone -> TaskCanvasTokens.priorityDone
                item?.priority == Priority.HIGH -> TaskCanvasTokens.weekPriority
                item?.showsDone == true -> TaskCanvasTokens.done
                else -> ColorTokens.textPrimary
            }
            taskCanvasColor(slot.title, "setTextColor", color)
            taskCanvasColor(slot.time, "setTextColor", color)
            taskCanvasColor(slot.emoji, "setTextColor", ColorTokens.textPrimary, if (item?.showsDone == true) TaskCanvasTokens.DONE_EMOJI_ALPHA else 1f)
            with(click) { set(slot.plaque, WidgetClicks.DAY, day.date.toString()) }
        }
    }

    /**
     * Мини-календарь в последней ячейке (0.46.0: без строки месяца и стрелок): «пн…вс» и шесть строк недель; нажатие на строку выбирает
     * неделю, а неделя с днями соседнего месяца перелистывает календарь на него (см. WidgetActionReceiver, monthAfterPick).
     */
    private fun RemoteViews.calendar(week: WidgetWeek, click: Click) {
        val month: YearMonth = week.month
        DayOfWeek.entries.forEachIndexed { index, day ->
            setTextViewText(WeekIds.CAL_WEEKDAYS[index], day.getDisplayName(TextStyle.SHORT, RU))
            taskCanvasColor(WeekIds.CAL_WEEKDAYS[index], "setTextColor", ColorTokens.textSecondary)
        }
        // Шесть строк недель делят высоту под «пн…вс» поровну (weight в разметке).
        val first = month.atDay(1).weekStart()
        WeekIds.CAL_ROWS.forEachIndexed { index, row ->
            val start = first.plusDays(index * WEEK_LENGTH.toLong())
            setInt(row.row, "setBackgroundResource", if (start == week.weekStart) R.drawable.widget_task_week_band else 0)
            with(click) { set(row.row, WidgetClicks.PICK_WEEK, start.toString()) }
            row.days.forEachIndexed { offset, id ->
                val date = start.plusDays(offset.toLong())
                val isToday = date == week.today
                setTextViewText(id, date.dayOfMonth.toString())
                taskCanvasColor(id, "setTextColor", calendarDayColor(isToday, inMonth = YearMonth.from(date) == month))
                setInt(id, "setBackgroundResource", if (isToday) R.drawable.widget_task_week_today else 0)
            }
        }
    }

    private fun calendarDayColor(isToday: Boolean, inMonth: Boolean): ThemedColor = when {
        isToday -> ColorTokens.onAccent
        inMonth -> ColorTokens.textPrimary
        else -> ColorTokens.inactive
    }

    /** Текст жирным начертанием ([bold] = true) или как есть. */
    private fun String.weekWeight(high: Boolean): CharSequence =
        if (high && isNotEmpty()) SpannableString(this).apply {
            setSpan(TypefaceSpan(Typeface.create(Typeface.DEFAULT, 800, false)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else this

    private fun String.bold(bold: Boolean): CharSequence =
        if (bold) SpannableString(this).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else this

    private fun dp(context: Context, value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
