package com.prostovasei.simpletasks.ui.feature.wontdo

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.feature.completed.CompletedDay
import com.prostovasei.simpletasks.ui.feature.completed.CompletedViewModel
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Экран «Не буду делать» (итерация 49): задачи по дню отметки, новые сверху; [repeats] — id закрытых разов серии (подпись «повтор»). */
data class WontDoState(val days: List<CompletedDay> = emptyList(), val repeats: Set<String> = emptySet(), val isLoading: Boolean = true)

sealed interface WontDoIntent {
    /** Нажатие на ✕ — вернуть задачу в работу. */
    data class Reopen(val taskId: String) : WontDoIntent
    data object Undo : WontDoIntent
}

sealed interface WontDoEffect {
    data object Reopened : WontDoEffect
}

/** Экран «Не буду делать» (итерация 49, макет iteration49.py): как «Выполненные», но вместо галочки — ✕; ✕ возвращает задачу. */
@HiltViewModel
class WontDoViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<WontDoState, WontDoIntent, WontDoEffect>(WontDoState()) {

    /** Последняя возвращённая задача — для «Отмены». */
    private var lastReopened: Task? = null

    init {
        viewModelScope.launch {
            combine(tasks.observeWontDo(), projects.observeProjectTree(), dayTicker.today()) { list, tree, today ->
                WontDoState(
                    days = CompletedViewModel.groupByDay(list, tree.projectsById(), today, time.zone()),
                    repeats = list.filter { it.isRepeatDone || it.rrule != null }.mapTo(mutableSetOf()) { it.id },
                    isLoading = false,
                )
            }.collect { setState { it } }
        }
    }

    override fun onIntent(intent: WontDoIntent) {
        when (intent) {
            is WontDoIntent.Reopen -> viewModelScope.launch {
                lastReopened = tasks.restore(intent.taskId)
                if (lastReopened != null) sendEffect(WontDoEffect.Reopened)
            }
            WontDoIntent.Undo -> {
                val previous = lastReopened ?: return
                lastReopened = null
                viewModelScope.launch { tasks.undoRestore(previous) }
            }
        }
    }
}
