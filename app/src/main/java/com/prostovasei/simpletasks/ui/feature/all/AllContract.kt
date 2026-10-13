package com.prostovasei.simpletasks.ui.feature.all

import com.prostovasei.simpletasks.ui.common.TaskItem

/** Экран «Все» (SPEC.md §4.1). */
data class AllState(
    val overdue: List<TaskItem> = emptyList(),
    val todayAllDay: List<TaskItem> = emptyList(),
    val todayTimed: List<TaskItem> = emptyList(),
    val upcoming: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && todayAllDay.isEmpty() && todayTimed.isEmpty() && upcoming.isEmpty()
}
