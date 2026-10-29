package com.prostovasei.simpletasks.ui.feature.trash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Задача в корзине для просмотра (итерация 39): править нельзя, в «⋮» — «Восстановить» и «Удалить навсегда». */
data class TrashTaskState(
    val trashed: TrashedTask? = null,
    /** Задача в виде строки списка: срок, список, повтор, колокольчик. */
    val item: TaskItem? = null,
    val deletedOn: LocalDate? = null,
    val isLoading: Boolean = true,
)

sealed interface TrashTaskIntent {
    data object Restore : TrashTaskIntent

    /** Подтверждённое «Удалить навсегда». */
    data object DeleteForever : TrashTaskIntent
}

sealed interface TrashTaskEffect {
    /** Задачи в корзине больше нет (вернули или стёрли) — закрыть окно. */
    data object Close : TrashTaskEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrashTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    private val restores: TrashRestores,
) : MviViewModel<TrashTaskState, TrashTaskIntent, TrashTaskEffect>(TrashTaskState()) {
    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    init {
        viewModelScope.launch {
            dayTicker.today().flatMapLatest { day ->
                combine(tasks.observeTrashed(taskId), projects.observeProjectTree()) { trashed, tree -> Triple(day, trashed, tree) }
            }.collect { (day, trashed, tree) ->
                if (trashed == null) {
                    sendEffect(TrashTaskEffect.Close)
                } else {
                    val zone = time.zone()
                    setState {
                        copy(
                            trashed = trashed,
                            item = trashed.details.task.toItem(tree.projectsById(), day, zone, emptySet()),
                            deletedOn = trashed.deletedAt.atZone(zone).toLocalDate(),
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    override fun onIntent(intent: TrashTaskIntent) {
        when (intent) {
            TrashTaskIntent.Restore -> restores.restore(taskId)
            TrashTaskIntent.DeleteForever -> viewModelScope.launch { tasks.deleteForever(taskId) }
        }
    }
}
