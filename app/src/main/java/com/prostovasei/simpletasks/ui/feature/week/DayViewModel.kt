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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<DayState, TaskListIntent, TaskListEffect>(DayState()) {

    /** Аргумент маршрута DayDestination(date). */
    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle["date"]) { "Не передан date" })

    /** День, с которого открыли окно дня (#314: дальше его листают). */
    val startDate: LocalDate get() = date

    private val completion = CompletionTracker(viewModelScope, tasks, time)
    private val days = mutableMapOf<LocalDate, StateFlow<DayState>>()

    init {
        viewModelScope.launch { day(date).collect { fresh -> setState { fresh } } }
    }

    /**
     * Список любого дня — #314: окно дня листается свайпом. Данные те же, что у [date]; выполнение — общий [completion],
     * поэтому «медленно тает» работает на любом дне. Поток дня живёт, пока его кто-то читает, и ещё 5 с.
     */
    fun day(day: LocalDate): StateFlow<DayState> = days.getOrPut(day) {
        combine(dayTicker.today(), tasks.observeWeek(day.weekStart()), projects.observeProjectTree(), completion.pending) { today, week, tree, pending ->
            val items = buildWeek(day.weekStart(), week, tree.projectsById(), today, time.zone(), pending).first { it.date == day }.items
            DayState(date = day, today = today, items = items, isLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_MS), DayState(date = day))
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            else -> Unit
        }
    }
}

private const val KEEP_MS = 5_000L
