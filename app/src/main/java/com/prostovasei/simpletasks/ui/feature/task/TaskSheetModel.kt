package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Recurrence
import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Поля шторки задачи, общие для создания и правки (итерация 19). Срок и начало — как у задачи:
 * у интервала [start] — начало, [due] — конец; у простого времени и «на весь день» [start] пустое
 * (у многодневной задачи из TickTick — первый день).
 */
data class SheetFields(
    val due: TaskDue?,
    val start: TaskDue?,
    val projectId: String,
    val priority: Priority,
    val rrule: String?,
    val reminders: List<String>,
    val projects: List<ProjectOption>,
    val today: LocalDate,
    val zone: ZoneId,
    val nowMinute: Int,
    val busy: List<BusySlot>,
) {
    val selectedDate: LocalDate? get() = (start ?: due)?.localDate(zone)
    val selectedMinute: Int? get() = (start ?: due)?.localTime(zone)?.let { it.hour * 60 + it.minute }
    val durationMinutes: Int get() = durationOf(due, start, zone)
}

/** Действие на сетке недели. */
sealed interface GridAction {
    /** Нажатие на день в шапке: «на весь день»; повторное — убрать дату. */
    data class DayTap(val date: LocalDate) : GridAction

    /** Нажатие на сетку: время [minute] (от начала суток) в день [date]; внутри выбранного блока — ничего. */
    data class SlotTap(val date: LocalDate, val minute: Int) : GridAction

    /** Блок перетащили (после долгого нажатия): новое начало, длина та же. */
    data class MoveSlot(val date: LocalDate, val minute: Int) : GridAction

    /** Блок растянули за ручку: новая длительность в минутах (кратна 30). */
    data class Resize(val minutes: Int) : GridAction

    /** Крестик на зелёном блоке: убрать время, день остаётся. */
    data object ClearTime : GridAction

    /** «Без даты» в строке недели (итерация 21): убрать и день, и время. */
    data object ClearDate : GridAction

    /** Сетку пролистали: первый видимый день — [first]. */
    data class FirstDay(val first: LocalDate) : GridAction
}

/** Срок и начало задачи. */
data class Schedule(val due: TaskDue?, val start: TaskDue?)

/** Длительность выбранного времени: у интервала — от начала до конца (в пределах суток), иначе — шаг сетки. */
fun durationOf(due: TaskDue?, start: TaskDue?, zone: ZoneId): Int {
    val end = (due as? TaskDue.AtTime)?.instant ?: return WeekGridMath.STEP_MINUTES
    val begin = (start as? TaskDue.AtTime)?.instant ?: return WeekGridMath.STEP_MINUTES
    val local = begin.atZone(zone)
    val left = WeekGridMath.DAY_MINUTES - (local.hour * 60 + local.minute)
    return Duration.between(begin, end).toMinutes().toInt().coerceIn(WeekGridMath.STEP_MINUTES, left.coerceAtLeast(WeekGridMath.STEP_MINUTES))
}

/**
 * Срок и начало после действия на сетке (решения PM, итерации 16–17, 19): 30 минут — просто время, дольше — интервал
 * в пределах суток; многодневная задача «на весь день» переезжает на новый первый день целиком.
 */
fun Schedule.after(action: GridAction, zone: ZoneId): Schedule {
    val first = start ?: due
    val date = first?.localDate(zone)
    val minute = first?.localTime(zone)?.let { it.hour * 60 + it.minute }
    val duration = durationOf(due, start, zone)
    return when (action) {
        is GridAction.DayTap -> when {
            date == action.date -> Schedule(null, null)
            start is TaskDue.AllDay && due is TaskDue.AllDay -> {
                val days = ChronoUnit.DAYS.between(start.date, action.date)
                Schedule(TaskDue.AllDay(due.date.plusDays(days)), TaskDue.AllDay(action.date))
            }
            else -> Schedule(TaskDue.AllDay(action.date), null)
        }
        is GridAction.SlotTap -> {
            val inside = minute != null && date == action.date && action.minute in minute until minute + duration
            if (inside) this else timed(action.date, action.minute, WeekGridMath.STEP_MINUTES, zone)
        }
        is GridAction.MoveSlot -> timed(action.date, action.minute, duration, zone)
        is GridAction.Resize -> if (date == null || minute == null) this else timed(date, minute, action.minutes, zone)
        GridAction.ClearTime -> if (date == null) this else Schedule(TaskDue.AllDay(date), null)
        GridAction.ClearDate -> Schedule(null, null)
        is GridAction.FirstDay -> this
    }
}

private fun timed(date: LocalDate, minute: Int, length: Int, zone: ZoneId): Schedule {
    val duration = length.coerceIn(WeekGridMath.STEP_MINUTES, WeekGridMath.DAY_MINUTES)
    val begin = minute.coerceIn(0, WeekGridMath.DAY_MINUTES - duration)
    fun at(m: Int) = TaskDue.AtTime(date.atStartOfDay(zone).plusMinutes(m.toLong()).toInstant())
    return if (duration > WeekGridMath.STEP_MINUTES) Schedule(at(begin + duration), at(begin)) else Schedule(at(begin), null)
}

/** Повтор начинается с первого подходящего дня (решение PM); конец интервала сдвигается вместе с началом. */
fun Schedule.alignedTo(rrule: String?, zone: ZoneId): Schedule {
    val rule = rrule ?: return this
    val anchor = start ?: due ?: return this
    val aligned = Recurrence.alignStart(rule, anchor, zone)
    if (aligned == anchor) return this
    val end = due ?: return this
    if (start == null) return Schedule(aligned, null)
    val movedEnd = when {
        anchor is TaskDue.AtTime && aligned is TaskDue.AtTime && end is TaskDue.AtTime ->
            TaskDue.AtTime(end.instant.plus(Duration.between(anchor.instant, aligned.instant)))
        anchor is TaskDue.AllDay && aligned is TaskDue.AllDay && end is TaskDue.AllDay ->
            TaskDue.AllDay(end.date.plusDays(ChronoUnit.DAYS.between(anchor.date, aligned.date)))
        else -> aligned
    }
    return Schedule(movedEnd, aligned)
}
