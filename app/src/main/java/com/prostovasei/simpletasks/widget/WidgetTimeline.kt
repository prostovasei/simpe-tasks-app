package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItem

/*
 * Шкала дня в виджете (итерация 20, макет PM «I2, период 64 dp»): слева время, по центру сплошная линия
 * с кружками приоритета, справа плашка. В блоке «Сегодня» — отметка «сейчас» и бледные прошедшие задачи.
 */

/** Строка шкалы дня: задача или отметка «сейчас». */
sealed interface WidgetRow {
    /**
     * [start] и [end] — подписи слева: время начала и конца периода (у просроченной — день и время);
     * у задачи на весь день обе пустые. [isPeriod] — у задачи начало и конец (строка выше, капсула на шкале);
     * [past] — время сегодняшней задачи уже прошло.
     */
    data class Task(val item: TaskItem, val start: String?, val end: String?, val isPeriod: Boolean = false, val past: Boolean = false) : WidgetRow

    data object Now : WidgetRow
}

object WidgetTimeline {
    /**
     * Строки блока: у просроченных слева день и время начала; в блоке «Сегодня» ([nowMinute] — минута дня или null)
     * отметка «сейчас» встаёт перед первой задачей, которая ещё не началась, а прошедшие задачи бледнеют.
     * Отметка есть, только если в блоке есть задачи со временем.
     */
    fun rows(items: List<TaskItem>, overdue: Boolean, nowMinute: Int?, days: DayNames): List<WidgetRow> {
        val rows = items.map { item ->
            if (overdue) {
                WidgetRow.Task(item, dayOf(item, days), item.due?.time?.substringBefore(RANGE))
            } else {
                val times = item.due?.time?.split(RANGE)
                val span = span(item)
                WidgetRow.Task(
                    item,
                    start = times?.first(),
                    end = times?.getOrNull(1),
                    isPeriod = times?.size == 2,
                    past = nowMinute != null && span != null && span.last < nowMinute,
                )
            }
        }
        if (overdue || nowMinute == null || items.none { span(it) != null }) return rows
        val at = items.indexOfFirst { item -> span(item)?.let { it.first > nowMinute } == true }.let { if (it < 0) rows.size else it }
        return rows.take(at) + WidgetRow.Now + rows.drop(at)
    }

    /**
     * Ближайшая минута дня после [nowMinute], когда шкала «Сегодня» меняется: начало или конец задачи.
     * null — сегодня больше ничего не меняется (дальше обновит полночь).
     */
    fun nextChange(items: List<TaskItem>, nowMinute: Int): Int? =
        items.mapNotNull(::span).flatMap { listOf(it.first, it.last + 1) }.filter { it in (nowMinute + 1) until MINUTES_PER_DAY }.minOrNull()

    /** Минуты дня начала и конца (конец после полуночи — дальше конца дня); null — без времени. */
    private fun span(item: TaskItem): IntRange? {
        val times = item.due?.time?.split(RANGE) ?: return null
        val start = minuteOf(times.first()) ?: return null
        val end = times.getOrNull(1)?.let(::minuteOf)?.let { if (it < start) it + MINUTES_PER_DAY else it } ?: start
        return start..end
    }

    private fun minuteOf(time: String): Int? {
        val parts = time.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hours * MINUTES_PER_HOUR + minutes
    }

    private fun dayOf(item: TaskItem, days: DayNames): String? = when (val label = item.due?.day) {
        null -> null
        is DayLabel.Date -> WidgetDates.short(label.date)
        else -> days.day(label)
    }

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
}

/** Разделитель начала и конца в подписи времени («10:00–11:00»). */
private const val RANGE = "–"
