package com.prostovasei.simpletasks.ui.feature.week

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Полный список дня недели (итерация 44): [date] — день, [items] — все его задачи, как в ячейке недели, но без ограничения по высоте. */
data class DayState(
    val date: LocalDate? = null,
    val today: LocalDate? = null,
    val items: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<DayState, TaskListIntent, TaskListEffect>(DayState()) {

    /** Аргумент маршрута DayDestination(date). */
    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle["date"]) { "Не передан date" })

    private val completion = CompletionTracker(viewModelScope, tasks, time)

    init {
        viewModelScope.launch {
            combine(dayTicker.today(), tasks.observeWeek(date.weekStart()), projects.observeProjectTree(), completion.pending) { today, week, tree, pending ->
                val items = buildWeek(date.weekStart(), week, tree.projectsById(), today, time.zone(), pending).first { it.date == date }.items
                DayState(date = date, today = today, items = items, isLoading = false)
            }.collect { fresh -> setState { fresh } }
        }
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            else -> Unit
        }
    }
}
