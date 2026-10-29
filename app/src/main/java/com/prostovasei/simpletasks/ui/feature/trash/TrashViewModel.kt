package com.prostovasei.simpletasks.ui.feature.trash

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
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

/** Раздел «Корзина» (итерация 39): удалённые задачи как в обычном списке; [deletedOn] — когда каждую удалили. */
data class TrashState(
    val items: List<TaskItem> = emptyList(),
    val deletedOn: Map<String, LocalDate> = emptyMap(),
    val isLoading: Boolean = true,
)

sealed interface TrashIntent {
    data object RestoreAll : TrashIntent

    /** Подтверждённое «Удалить все»: стирает корзину навсегда. */
    data object DeleteAll : TrashIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    private val restores: TrashRestores,
) : MviViewModel<TrashState, TrashIntent, Nothing>(TrashState()) {

    init {
        viewModelScope.launch {
            dayTicker.today().flatMapLatest { day ->
                combine(tasks.observeTrash(), projects.observeProjectTree()) { trash, tree -> Triple(day, trash, tree) }
            }.collect { (day, trash, tree) ->
                val byId = tree.projectsById()
                val zone = time.zone()
                setState {
                    copy(
                        items = trash.map { it.toItem(byId, day, zone, emptySet()) },
                        deletedOn = trash.associate { it.id to it.updatedAt.atZone(zone).toLocalDate() },
                        isLoading = false,
                    )
                }
            }
        }
    }

    override fun onIntent(intent: TrashIntent) {
        when (intent) {
            TrashIntent.RestoreAll -> restores.restoreAll()
            TrashIntent.DeleteAll -> viewModelScope.launch { tasks.emptyTrash() }
        }
    }
}
