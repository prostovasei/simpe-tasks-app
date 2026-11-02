package com.prostovasei.simpletasks.ui.feature.week

import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import java.time.LocalDate
import java.time.YearMonth

/** День недели: дата и его задачи (все, с выполненными и будущими повторами) в порядке «весь день», затем по времени. */
data class WeekDay(val date: LocalDate, val items: List<TaskItem>)

/** Вид «Неделя» (итерация 44): семь дней выбранной недели и месяц мини-календаря. */
data class WeekState(
    /** Понедельник выбранной недели; null, пока не пришла сегодняшняя дата. */
    val weekStart: LocalDate? = null,
    val today: LocalDate? = null,
    val days: List<WeekDay> = emptyList(),
    /** Месяц, показанный в мини-календаре. */
    val month: YearMonth? = null,
    val isLoading: Boolean = true,
)

/** Выбор недели в мини-календаре: [weekStart] — понедельник нажатой недели. */
data class SelectWeek(val weekStart: LocalDate) : TaskListIntent

/** Листание месяцев мини-календаря: [delta] = -1 — назад, 1 — вперёд. */
data class ShiftMonth(val delta: Int) : TaskListIntent
