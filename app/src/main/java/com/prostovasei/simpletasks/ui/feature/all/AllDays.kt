package com.prostovasei.simpletasks.ui.feature.all

import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DaySort
import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate

/**
 * День ленты «Все» (итерация 13, макет PM): подпись «ПТ, 16 окт» + метка [relative] («Сегодня», «Завтра», «Вчера»).
 * [isOverdue] — прошедший день с невыполненными задачами: подпись красная.
 */
data class DayGroup(
    val date: LocalDate,
    val relative: DayLabel?,
    val isOverdue: Boolean,
    val items: List<TaskItem>,
    /** Задачи, просроченные к этому моменту (в том числе сегодняшние с прошедшим временем). */
    val overdueIds: Set<String>,
)

/** Лента по дням: просроченные — каждая в своём дне, затем сегодня (весь день, потом по времени) и предстоящие. */
fun AllState.dayGroups(): List<DayGroup> {
    val today = today ?: return emptyList()
    fun TaskItem.date(): LocalDate = when (val day = due?.day) {
        DayLabel.Yesterday -> today.minusDays(1)
        DayLabel.Tomorrow -> today.plusDays(1)
        is DayLabel.Date -> day.date
        DayLabel.Today, null -> today
    }
    fun relative(date: LocalDate): DayLabel? = when (date) {
        today -> DayLabel.Today
        today.plusDays(1) -> DayLabel.Tomorrow
        today.minusDays(1) -> DayLabel.Yesterday
        else -> null
    }
    val overdueIds = overdue.map { it.id }.toSet()
    val (overdueToday, overduePast) = overdue.partition { it.date() == today }
    val past = overduePast.groupBy { it.date() }.toSortedMap().map { (date, items) ->
        DayGroup(date, relative(date), isOverdue = true, items = DaySort.apply(items, daySort), overdueIds = overdueIds)
    }
    val todayItems = overdueToday + todayAllDay + todayTimed
    val todayGroup = listOfNotNull(
        todayItems.takeIf { it.isNotEmpty() }?.let { DayGroup(today, DayLabel.Today, isOverdue = false, items = DaySort.apply(it, daySort), overdueIds = overdueIds) },
    )
    val future = upcoming.groupBy { it.date() }.map { (date, items) ->
        DayGroup(date, relative(date), isOverdue = false, items = DaySort.apply(items, daySort), overdueIds = overdueIds)
    }
    return past + todayGroup + future
}
