package com.prostovasei.simpletasks.ui.feature.week

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Вид «Неделя» (итерация 44). Пока неделя не выбрана в мини-календаре, показывается текущая и переходит на новую в полночь;
 * месяц календаря — месяц сегодняшнего дня, пока его не пролистали.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeekViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<WeekState, TaskListIntent, TaskListEffect>(WeekState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)
    private val selected = MutableStateFlow<LocalDate?>(null)
    private val shownMonth = MutableStateFlow<YearMonth?>(null)

    init {
        viewModelScope.launch {
            val week = combine(dayTicker.today(), selected) { today, picked -> today to (picked ?: today.weekStart()) }
                .flatMapLatest { (today, start) -> tasks.observeWeek(start).map { Triple(today, start, it) } }
            combine(week, projects.observeProjectTree(), completion.pending, shownMonth) { (today, start, data), tree, pending, month ->
                val days = buildWeek(start, data, tree.projectsById(), today, time.zone(), pending)
                WeekState(weekStart = start, today = today, days = days, month = month ?: YearMonth.from(today), isLoading = false)
            }.collect { fresh -> setState { fresh } }
        }
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            is SelectWeek -> selected.value = intent.weekStart
            is ShiftMonth -> {
                val today = currentState.today ?: return
                // От пролистанного месяца, а не от показанного: два нажатия подряд не должны слиться в одно.
                shownMonth.update { (it ?: YearMonth.from(today)).plusMonths(intent.delta.toLong()) }
            }
            else -> Unit
        }
    }
}
