package com.prostovasei.simpletasks.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
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
import com.prostovasei.simpletasks.designsystem.tokens.WeekRowTokens
import com.prostovasei.simpletasks.designsystem.tokens.WidgetCheckTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WEEK_LENGTH
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import com.prostovasei.simpletasks.ui.feature.week.startTime
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import com.prostovasei.simpletasks.widget.WidgetRows.color
import com.prostovasei.simpletasks.widget.WidgetRows.ring
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
 * задач: кружок (нажатие — «выполнено»), название, время. Плашек в ячейке столько, сколько влезает по высоте. Разметка — `widget_week_*`,
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
        return ((height - SizeTokens.WIDGET_EDGE - SizeTokens.WIDGET_BAR) / ROWS).coerceAtLeast(SizeTokens.WIDGET_WEEK_ROW_MIN.toFloat())
    }

    /**
     * Сколько строк задач влезает в ячейку высотой [rowHeight] (поле сверху, строка даты, строки с промежутком над каждой) — по наименьшей
     * читаемой строке `WIDGET_WEEK_TASK`: сами строки делят высоту поровну и выходят выше. Решение PM — до 6 в ячейке.
     */
    fun capacity(rowHeight: Float): Int {
        val room = rowHeight - SizeTokens.WIDGET_WEEK_TASK_GAP - SizeTokens.WIDGET_WEEK_HEADER
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
        // Сегодня (0.46.0, проверка PM): без кружка — день недели и число жирные и основного цвета; остальные дни — обычным шрифтом,
        // число серое. Жирность — через начертание в тексте: у RemoteViews нет смены шрифта.
        setTextViewText(ids.weekday, day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).bold(isToday))
        color(ids.weekday, "setTextColor", ColorTokens.textPrimary)
        setTextViewText(ids.number, day.date.dayOfMonth.toString().bold(isToday))
        color(ids.number, "setTextColor", if (isToday) ColorTokens.textPrimary else ColorTokens.textSecondary)
        visible(ids.more, hidden > 0)
        setTextViewText(ids.more, "+$hidden")
        with(click) { set(ids.head, WidgetClicks.DAY, day.date.toString()) }
        // Строк задач в ячейке — [capacity], они делят высоту под строкой даты поровну; пустые места невидимы, но держат размер,
        // поэтому строки во всех ячейках одной высоты. Лишние места убраны совсем.
        ids.slots.forEachIndexed { index, slot ->
            val item = shown.getOrNull(index)
            setViewVisibility(slot.plaque, if (item != null) View.VISIBLE else if (index < capacity) View.INVISIBLE else View.GONE)
            if (item != null) slot(context, slot, item, overdue = day.date < today && !item.showsDone, click)
        }
    }

    /**
     * Строка задачи в ячейке (0.46.0, `VariantK2`): фон по приоритету (без приоритета и у выполненной — белый), выполненная — серая и
     * зачёркнутая с серым кружком, будущий повтор — наполовину прозрачный. Всё, что зависит от задачи, пишется заново.
     */
    private fun RemoteViews.slot(context: Context, ids: SlotIds, item: TaskItem, overdue: Boolean, click: Click) {
        val done = item.showsDone
        // Итерация 46 (вариант 2 «Контрастный графит»): у высокого и среднего свои текст, кружок, время и просрочка.
        val level = if (done) Priority.NONE else item.priority
        val ringColor = when {
            done -> WeekRowTokens.doneMark
            level == Priority.HIGH -> WeekRowTokens.highContent
            level == Priority.MEDIUM -> WeekRowTokens.mediumContent
            item.isGhost -> ColorTokens.inactive
            else -> ring(item.priority)
        }
        setInt(ids.plaque, "setBackgroundResource", plaque(level))
        setFloat(ids.plaque, "setAlpha", if (item.isGhost) GHOST_ALPHA else 1f)
        setImageViewResource(
            ids.node,
            when {
                done -> R.drawable.ic_widget_node_fill
                level == Priority.HIGH -> R.drawable.ic_widget_node_ring_high
                else -> R.drawable.ic_widget_node_ring
            },
        )
        color(ids.node, "setColorFilter", ringColor)
        visible(ids.tick, done)
        color(ids.tick, "setColorFilter", WidgetCheckTokens.none.check)
        setContentDescription(
            ids.node,
            when {
                item.isGhost -> null
                done -> context.getString(R.string.task_uncomplete)
                else -> context.getString(R.string.widget_complete)
            },
        )
        with(click) {
            set(
                ids.check,
                when {
                    item.isGhost -> WidgetClicks.NONE
                    done -> WidgetClicks.UNCOMPLETE
                    else -> WidgetClicks.COMPLETE
                },
                item.id.takeUnless { item.isGhost },
            )
        }
        setTextViewText(
            ids.title,
            if (done) SpannableString(item.title).apply { setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else item.title,
        )
        color(
            ids.title,
            "setTextColor",
            when {
                done -> ColorTokens.inactive
                level == Priority.HIGH -> WeekRowTokens.highContent
                level == Priority.MEDIUM -> WeekRowTokens.mediumContent
                item.isGhost -> ColorTokens.textSecondary
                else -> ColorTokens.textPrimary
            },
        )
        val time = item.startTime()
        visible(ids.time, time != null)
        setTextViewText(ids.time, time.orEmpty())
        color(
            ids.time,
            "setTextColor",
            when {
                overdue && level == Priority.HIGH -> WeekRowTokens.highOverdue
                overdue && level == Priority.MEDIUM -> WeekRowTokens.mediumOverdue
                overdue -> ColorTokens.overdue
                done -> ColorTokens.inactive
                level == Priority.HIGH -> WeekRowTokens.highTime
                level == Priority.MEDIUM -> WeekRowTokens.mediumTime
                else -> ColorTokens.textSecondary
            },
        )
        // Без времени название доходит до края плашки — нужен правый отступ.
        setViewPadding(ids.plaque, 0, 0, if (time == null) dp(context, SpacingTokens.XS) else 0, 0)
        with(click) { set(ids.plaque, WidgetClicks.OPEN, item.taskId) }
    }

    /**
     * Мини-календарь в последней ячейке (0.46.0: без строки месяца и стрелок): «пн…вс» и шесть строк недель; нажатие на строку выбирает
     * неделю, а неделя с днями соседнего месяца перелистывает календарь на него (см. WidgetActionReceiver, monthAfterPick).
     */
    private fun RemoteViews.calendar(week: WidgetWeek, click: Click) {
        val month: YearMonth = week.month
        DayOfWeek.entries.forEachIndexed { index, day -> setTextViewText(WeekIds.CAL_WEEKDAYS[index], day.getDisplayName(TextStyle.SHORT, RU)) }
        // Шесть строк недель делят высоту под «пн…вс» поровну (weight в разметке).
        val first = month.atDay(1).weekStart()
        WeekIds.CAL_ROWS.forEachIndexed { index, row ->
            val start = first.plusDays(index * WEEK_LENGTH.toLong())
            setInt(row.row, "setBackgroundResource", if (start == week.weekStart) R.drawable.widget_week_band else 0)
            with(click) { set(row.row, WidgetClicks.PICK_WEEK, start.toString()) }
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

    /** Текст жирным начертанием ([bold] = true) или как есть. */
    private fun String.bold(bold: Boolean): CharSequence =
        if (bold) SpannableString(this).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else this

    /** Фон строки задачи по приоритету. */
    private fun plaque(priority: Priority): Int = when (priority) {
        Priority.HIGH -> R.drawable.widget_week_plaque_high
        Priority.MEDIUM -> R.drawable.widget_week_plaque_medium
        Priority.LOW -> R.drawable.widget_week_plaque_low
        Priority.NONE -> R.drawable.widget_week_plaque_plain
    }

    private fun dp(context: Context, value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    /** Будущий повтор — наполовину прозрачный, как в приложении. */
    private const val GHOST_ALPHA = 0.5f
}
