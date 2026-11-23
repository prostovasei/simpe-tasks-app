package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
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
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WEEK_LENGTH
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import com.prostovasei.simpletasks.ui.feature.week.startTime
import com.prostovasei.simpletasks.ui.feature.week.weekCalendarFullDate
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import com.prostovasei.simpletasks.widget.WidgetRows.taskCanvasColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * Вид «Неделя» в виджете (итерация 45; 0.46.0 — строки делят высоту поровну; #313 — вариант PM 5 «Всё листается»).
 * Четыре строки на весь виджет над нижней панелью: Пн–Вт, Ср–Чт, Пт–Сб, Вс и мини-календарь. У каждого дня под неподвижной строкой
 * даты («ПН 23») — свой список задач (эмодзи, название, начало справа), который листается отдельно от остальных; под задачами —
 * пустое место, открывающее день. Мини-календарь — лента недель с названиями месяцев. Нажатие на дату, задачу или пустое место
 * дня открывает окно дня, на неделю ленты — выбирает её, на число выбранной недели — открывает этот день.
 * Разметка — `widget_root_week`, `widget_week_*` (генерирует design/tools/gen_widget_week.py), id — WidgetWeekIds.
 */
@RequiresApi(Build.VERSION_CODES.S)
object WidgetWeekRows {
    /** Строк недели: шесть дней по два, воскресенье с мини-календарём. */
    const val ROWS = 4

    /** Нажатие на элемент вне списков (строка даты, ячейка): у каждого своё намерение ([WidgetClicks.pending]). */
    fun interface Click {
        fun RemoteViews.set(viewId: Int, code: String, id: String?)
    }

    /** Строка ленты календаря: название месяца или неделя с понедельника [start]. */
    sealed interface CalLine {
        data class Month(val month: YearMonth) : CalLine
        data class Week(val start: LocalDate) : CalLine
    }

    private val RU: Locale = Locale.forLanguageTag("ru")

    /**
     * Высота строки недели по размеру от рабочего стола — для пустого места под задачами: сами строки делят настоящую высоту
     * виджета поровну (weight). Рабочий стол бывает щедрее настоящего размера — пустое место тогда чуть выше нужного.
     */
    fun rowHeightDp(size: DpSize): Float {
        val height = size.height.value.takeIf { !it.isNaN() && it > 0f } ?: return SizeTokens.WIDGET_WEEK_ROW_DEFAULT.toFloat()
        return ((height - SizeTokens.WIDGET_WEEK_TOP - SizeTokens.WIDGET_BAR) / ROWS).coerceAtLeast(SizeTokens.WIDGET_WEEK_ROW_MIN.toFloat())
    }

    /** Пустое место под [tasks] задачами дня: до низа ячейки высотой [rowHeight], но не меньше строки задачи — его всегда можно нажать. */
    fun fillerDp(rowHeight: Float, tasks: Int): Int {
        val room = rowHeight - SizeTokens.WIDGET_WEEK_CELL_TOP - SizeTokens.WIDGET_WEEK_HEADER - tasks * SizeTokens.WIDGET_WEEK_ITEM - SizeTokens.WIDGET_WEEK_FILL_SLACK
        return room.toInt().coerceAtLeast(SizeTokens.WIDGET_WEEK_ITEM)
    }

    /**
     * Лента календаря: с недели за две до текущей (или с выбранной, если она раньше) — Android не умеет открыть список на середине,
     * поэтому начало ленты и есть место, которое видно сразу, — и на [CAL_MONTHS] месяцев вперёд. Неделя относится к месяцу своего
     * воскресенья; перед первой неделей месяца — его название.
     */
    fun calendarLines(today: LocalDate, selected: LocalDate): List<CalLine> {
        val start = minOf(today.weekStart().minusWeeks(CAL_WEEKS_BEFORE), selected.weekStart())
        val end = maxOf(YearMonth.from(today).plusMonths(CAL_MONTHS).atEndOfMonth(), selected.plusDays(WEEK_LENGTH - 1L))
        val lines = mutableListOf<CalLine>()
        var week = start
        var month: YearMonth? = null
        while (week <= end) {
            val own = YearMonth.from(week.plusDays(WEEK_LENGTH - 1L))
            if (own != month) lines += CalLine.Month(own)
            month = own
            lines += CalLine.Week(week)
            week = week.plusWeeks(1)
        }
        return lines
    }

    /**
     * Заполняет корень `widget_root_week`: строки дат, списки задач дней и ленту календаря. [template] — общий шаблон нажатий
     * списков (номер виджета внутри). [scrollDays] / [scrollCalendar] — вернуть списки в начало (выбор недели, «к сегодня»).
     */
    fun fill(
        views: RemoteViews,
        context: Context,
        week: WidgetWeek,
        size: DpSize,
        click: Click,
        template: PendingIntent,
        scrollDays: Boolean = false,
        scrollCalendar: Boolean = false,
    ) {
        val rowHeight = rowHeightDp(size)
        week.days.take(WeekIds.DAYS.size).forEachIndexed { index, day ->
            val ids = WeekIds.DAYS[index]
            views.head(context, ids, day, week.today, click)
            val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(DAY_VIEW_TYPES)
            day.items.forEach { item -> items.addItem("${day.date}:${item.id}".hashCode().toLong(), taskRow(context, day.date, item)) }
            items.addItem(FILLER_ID, filler(context, day.date, fillerDp(rowHeight, day.items.size)))
            views.setRemoteAdapter(ids.list, items.build())
            views.setPendingIntentTemplate(ids.list, template)
            if (scrollDays) views.setScrollPosition(ids.list, 0)
        }
        val calendar = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(CAL_VIEW_TYPES)
        calendarLines(week.today, week.weekStart).forEach { line ->
            when (line) {
                is CalLine.Month -> calendar.addItem(-line.month.atDay(1).toEpochDay() - 1, monthRow(context, line.month, week.today))
                is CalLine.Week -> calendar.addItem(line.start.toEpochDay(), weekRow(context, line.start, week))
            }
        }
        views.setRemoteAdapter(WeekIds.CALENDAR, calendar.build())
        views.setPendingIntentTemplate(WeekIds.CALENDAR, template)
        if (scrollCalendar) views.setScrollPosition(WeekIds.CALENDAR, 0)
    }

    private fun RemoteViews.head(context: Context, ids: DayIds, day: WeekDay, today: LocalDate, click: Click) {
        val isToday = day.date == today
        // Сегодня (0.46.0): без кружка — день недели и число жирные и основного цвета; остальные дни — обычным начертанием, серые.
        // Итерация 48: день недели капсом («ПТ 6»). Жирность — через начертание в тексте: у RemoteViews нет смены шрифта.
        setTextViewText(ids.weekday, day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).uppercase(RU).bold(isToday))
        taskCanvasColor(ids.weekday, "setTextColor", if (isToday) ColorTokens.textPrimary else ColorTokens.textSecondary)
        val number = day.date.dayOfMonth.toString() + if (isToday) " ${context.getString(R.string.day_today)}" else ""
        setTextViewText(ids.number, number.bold(isToday))
        taskCanvasColor(ids.number, "setTextColor", if (isToday) ColorTokens.textPrimary else ColorTokens.textSecondary)
        with(click) {
            set(ids.root, WidgetClicks.DAY, day.date.toString())
            set(ids.head, WidgetClicks.DAY, day.date.toString())
        }
    }

    /** Строка задачи в списке дня: эмодзи, название, начало справа; нажатие открывает день. */
    internal fun taskRow(context: Context, date: LocalDate, item: TaskItem): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_week_task).apply {
            val high = item.priority == Priority.HIGH && !item.showsDone
            setTextViewText(R.id.wi_title, item.title.weekWeight(high))
            setTextViewText(R.id.wi_emoji, item.listEmoji.orEmpty())
            val time = item.startTime()
            setViewVisibility(R.id.wi_time, if (time != null) View.VISIBLE else View.GONE)
            setTextViewText(R.id.wi_time, time.orEmpty().weekWeight(high))
            // #316: выполненная — 40 % обычного цвета (на белом холсте — готовые цвета), эмодзи — с той же прозрачностью.
            val color = when {
                item.priority == Priority.HIGH && item.showsDone -> TaskCanvasTokens.priorityDone
                item.priority == Priority.HIGH -> TaskCanvasTokens.weekPriority
                item.showsDone -> TaskCanvasTokens.done
                else -> ColorTokens.textPrimary
            }
            taskCanvasColor(R.id.wi_title, "setTextColor", color)
            taskCanvasColor(R.id.wi_time, "setTextColor", color)
            taskCanvasColor(R.id.wi_emoji, "setTextColor", ColorTokens.textPrimary, if (item.showsDone) TaskCanvasTokens.DONE_EMOJI_ALPHA else 1f)
            setOnClickFillInIntent(R.id.wi_row, WidgetRows.fillIn(WidgetClicks.DAY, date.toString()))
            setContentDescription(R.id.wi_row, listOfNotNull(item.title, time).joinToString(", "))
        }

    /** Пустое место под задачами дня высотой [heightDp]: открывает день. */
    private fun filler(context: Context, date: LocalDate, heightDp: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_week_filler).apply {
            // Список меряет строку по содержимому — высоту из разметки он не учитывает, поэтому наименьшая высота.
            setInt(R.id.wi_filler, "setMinimumHeight", TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), context.resources.displayMetrics).toInt())
            setOnClickFillInIntent(R.id.wi_filler, WidgetRows.fillIn(WidgetClicks.DAY, date.toString()))
        }

    /** Название месяца в ленте: «ноябрь», у другого года — «январь 2027». */
    private fun monthRow(context: Context, month: YearMonth, today: LocalDate): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_week_cal_month).apply {
            setTextViewText(R.id.wcm_title, monthTitle(month, today))
            taskCanvasColor(R.id.wcm_title, "setTextColor", ColorTokens.textSecondary)
        }

    fun monthTitle(month: YearMonth, today: LocalDate): String {
        val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, RU).lowercase(RU)
        return if (month.year == today.year) name else "$name ${month.year}"
    }

    /**
     * Неделя в ленте (#284 — светлая полоса у выбранной): нажатие выбирает неделю, у выбранной — число открывает свой день.
     * Числа чужого месяца серые, сегодня — жирное. Строку Android переиспользует — все свойства задаются каждый раз.
     */
    private fun weekRow(context: Context, start: LocalDate, week: WidgetWeek): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_week_cal_week).apply {
            val selected = start == week.weekStart
            val month = YearMonth.from(start.plusDays(WEEK_LENGTH - 1L))
            setInt(R.id.wcw_row, "setBackgroundResource", if (selected) R.drawable.widget_task_week_band else 0)
            setOnClickFillInIntent(R.id.wcw_row, WidgetRows.fillIn(if (selected) WidgetClicks.NONE else WidgetClicks.PICK_WEEK, start.toString()))
            WeekIds.CAL_DAYS.forEachIndexed { offset, id ->
                val date = start.plusDays(offset.toLong())
                val isToday = date == week.today
                setTextViewText(id, date.dayOfMonth.toString().bold(isToday))
                taskCanvasColor(id, "setTextColor", calendarDayColor(isToday, inMonth = YearMonth.from(date) == month))
                val action = if (selected) R.string.week_open_calendar_day else R.string.week_select_calendar_week
                setContentDescription(id, "${weekCalendarFullDate.format(date)}, ${context.getString(action)}")
                setOnClickFillInIntent(id, WidgetRows.fillIn(if (selected) WidgetClicks.DAY else WidgetClicks.PICK_WEEK, if (selected) date.toString() else start.toString()))
            }
        }

    private fun calendarDayColor(isToday: Boolean, inMonth: Boolean): ThemedColor =
        if (isToday || inMonth) ColorTokens.textPrimary else ColorTokens.inactive

    /** Текст высокого приоритета — ExtraBold 800 (#259), иначе как есть. */
    private fun String.weekWeight(high: Boolean): CharSequence =
        if (high && isNotEmpty()) SpannableString(this).apply {
            setSpan(TypefaceSpan(Typeface.create(Typeface.DEFAULT, EXTRA_BOLD, false)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else this

    private fun String.bold(bold: Boolean): CharSequence =
        if (bold) SpannableString(this).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) } else this

    /** Видов строк в списке дня: задача и пустое место; в ленте календаря: месяц и неделя. */
    private const val DAY_VIEW_TYPES = 2
    private const val CAL_VIEW_TYPES = 2
    private const val FILLER_ID = Long.MIN_VALUE
    private const val EXTRA_BOLD = 800

    /** Лента календаря: недель до текущей и месяцев вперёд. */
    const val CAL_WEEKS_BEFORE = 2L
    const val CAL_MONTHS = 6L
}
