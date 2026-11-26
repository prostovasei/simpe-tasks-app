package com.prostovasei.simpletasks.ui.feature.all

import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate

/** Экран «Все» (SPEC.md §4.1). */
data class AllState(
    val overdue: List<TaskItem> = emptyList(),
    val todayAllDay: List<TaskItem> = emptyList(),
    val todayTimed: List<TaskItem> = emptyList(),
    val upcoming: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    /** Сегодняшняя дата — для подписей дней «ПТ, 16 окт». */
    val today: LocalDate? = null,
    /** Блок «Просрочено» свёрнут (итерация 37). */
    val overdueCollapsed: Boolean = false,
    /** #357: порядок внутри дня (кнопка ↑↓); тот же — в хабе и виджете. */
    val daySort: com.prostovasei.simpletasks.domain.model.TaskSort = com.prostovasei.simpletasks.domain.model.TaskSort.DAY_DEFAULT,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && todayAllDay.isEmpty() && todayTimed.isEmpty() && upcoming.isEmpty()
}
