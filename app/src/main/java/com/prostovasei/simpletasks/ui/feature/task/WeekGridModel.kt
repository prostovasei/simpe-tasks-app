package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.upcomingRepeats
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Занятое время на сетке недели (итерация 16): задача в день [date] с минуты [startMinute] до [endMinute]
 * от начала суток. Короткие задачи (только время, без интервала) занимают один шаг сетки.
 */
data class BusySlot(
    val taskId: String,
    val title: String,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val isDone: Boolean,
)

/**
 * Геометрия сетки: сутки с 00:00 до 24:00 шагом 30 минут, листается по дням. В шторке — строка недели
 * из [VISIBLE_DAYS] дней, по нажатию на день — таймлайн на [TIMELINE_DAYS] дня (итерация 21).
 */
object WeekGridMath {
    const val STEP_MINUTES = 30
    const val DAY_MINUTES = 24 * 60
    const val VISIBLE_DAYS = 7
    const val TIMELINE_DAYS = 3

    /** Насколько далеко можно уйти свайпом от сегодняшнего дня (в днях). */
    const val MAX_DAYS_AWAY = 730

    /** Сколько секунд «пролетает» сетка после быстрого свайпа, прежде чем встать на границу дня. */
    private const val FLING_SECONDS = 0.15f

    /** Начало получасового шага под точкой [y] (px от начала суток) при высоте часа [hourPx]. */
    fun minuteAt(y: Float, hourPx: Float): Int {
        val step = floor(y / (hourPx * STEP_MINUTES / MINUTES_PER_HOUR)).toInt()
        return (step * STEP_MINUTES).coerceIn(0, DAY_MINUTES - STEP_MINUTES)
    }

    /** Номер дня от сегодняшнего (0 — сегодня) под точкой [x] при прокрутке [scrollDays] (в днях). */
    fun dayAt(x: Float, scrollDays: Float, dayPx: Float): Int = floor(x / dayPx + scrollDays).toInt()

    /** Куда встать после свайпа: на границу дня с учётом скорости [velocityPx] (px/с, влево — меньше нуля). */
    fun settleTarget(scrollDays: Float, velocityPx: Float, dayPx: Float): Float =
        (scrollDays - velocityPx / dayPx * FLING_SECONDS).roundToInt().toFloat().coerceIn(-MAX_DAYS_AWAY.toFloat(), MAX_DAYS_AWAY.toFloat())

    /**
     * С какого дня начать таймлайн, открытый нажатием на [day]: нажатый день — в центре (решение PM, вариант N2),
     * но не раньше сегодняшнего. Результат — сдвиг в днях от [today].
     */
    fun timelineScroll(today: LocalDate, day: LocalDate): Float {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, day) - TIMELINE_DAYS / 2
        return days.coerceIn(0L, MAX_DAYS_AWAY.toLong()).toFloat()
    }

    /**
     * Куда встанет перетаскиваемый блок ([minute], [duration] минут), сдвинутый на [dx]/[dy] px:
     * сдвиг в днях и новое начало — шагом 30 минут, весь блок в пределах суток.
     */
    fun moveTarget(minute: Int, duration: Int, dx: Float, dy: Float, dayPx: Float, stepPx: Float): Pair<Int, Int> {
        val days = (dx / dayPx).roundToInt()
        val start = (minute + (dy / stepPx).roundToInt() * STEP_MINUTES).coerceIn(0, (DAY_MINUTES - duration).coerceAtLeast(0))
        return days to start
    }

    /** Длительность после растягивания ручки на [dy] px: шаг 30 минут, не меньше шага и не дальше 24:00. */
    fun resizeTarget(minute: Int, duration: Int, dy: Float, stepPx: Float): Int =
        (duration + (dy / stepPx).roundToInt() * STEP_MINUTES).coerceIn(STEP_MINUTES, DAY_MINUTES - minute)

    private const val MINUTES_PER_HOUR = 60
}

/**
 * Задачи со временем → занятое время в днях с [from] по [until] включительно: сами задачи и будущие разы
 * повторяющихся. Интервал через полночь обрезается концом суток.
 */
fun busySlots(tasks: List<Task>, zone: ZoneId, from: LocalDate, until: LocalDate): List<BusySlot> =
    tasks.flatMap { task ->
        val repeats = if (task.rrule != null && !task.isCompleted) task.upcomingRepeats(zone, from.minusDays(1), until) else emptyList()
        (listOf(task) + repeats).mapNotNull { it.slot(zone) }
    }.filter { it.date in from..until }.distinct()

private fun Task.slot(zone: ZoneId): BusySlot? {
    val end = (due as? TaskDue.AtTime)?.instant ?: return null
    val begin = (start as? TaskDue.AtTime)?.instant ?: end
    val local = begin.atZone(zone)
    val startMinute = local.hour * 60 + local.minute
    val endLocal = end.atZone(zone)
    val endMinute = when {
        end == begin -> startMinute + WeekGridMath.STEP_MINUTES
        endLocal.toLocalDate() != local.toLocalDate() -> WeekGridMath.DAY_MINUTES
        else -> endLocal.hour * 60 + endLocal.minute
    }
    return BusySlot(id, title, local.toLocalDate(), startMinute, endMinute.coerceIn(startMinute + 1, WeekGridMath.DAY_MINUTES), isCompleted)
}
