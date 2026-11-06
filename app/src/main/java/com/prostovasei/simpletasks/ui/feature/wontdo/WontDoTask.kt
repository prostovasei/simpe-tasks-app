package com.prostovasei.simpletasks.ui.feature.wontdo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.feature.task.TaskDeletions
import com.prostovasei.simpletasks.ui.feature.task.UndoMessage
import com.prostovasei.simpletasks.ui.feature.task.UndoNotice
import com.prostovasei.simpletasks.ui.feature.task.rememberSheetMotion
import com.prostovasei.simpletasks.ui.feature.trash.ReadonlyKind
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskEffect
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskIntent
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskScreen
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskState
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun WontDoTaskRoute(onClose: () -> Unit, viewModel: WontDoTaskViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = rememberSheetMotion(onClose)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == TrashTaskEffect.Close) motion.close() }
    }
    TrashTaskScreen(state, viewModel::onIntent, motion.close, progress = motion.progress, kind = ReadonlyKind.WontDo)
}

/**
 * Задача «Не буду делать» для просмотра (итерация 49): та же шторка, что у задачи в корзине, — плашка «Не буду делать · отмечено …»,
 * в «⋮» — «Вернуть в работу» (раз серии возвращается обычной задачей) и «Удалить» (в корзину). Подсказку «Отмена» показывает экран
 * под шторкой.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WontDoTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    private val deletions: TaskDeletions,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : MviViewModel<TrashTaskState, TrashTaskIntent, TrashTaskEffect>(TrashTaskState()) {
    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private var closed = false

    init {
        viewModelScope.launch {
            dayTicker.today().flatMapLatest { day ->
                combine(tasks.observeTaskDetails(taskId), tasks.observeSubtasks(taskId), projects.observeProjectTree()) { details, subtasks, tree ->
                    Triple(day, details?.let { TrashedTask(it, subtasks, it.task.completedAt ?: it.task.updatedAt) }, tree)
                }
            }.collect { (day, shown, tree) ->
                if (shown == null || shown.details.task.status != TaskStatus.ABANDONED) {
                    close()
                } else {
                    val zone = time.zone()
                    setState {
                        copy(
                            trashed = shown,
                            item = shown.details.task.toItem(tree.projectsById(), day, zone, emptySet()),
                            deletedOn = shown.deletedAt.atZone(zone).toLocalDate(),
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    override fun onIntent(intent: TrashTaskIntent) {
        when (intent) {
            TrashTaskIntent.Restore -> appScope.launch {
                val previous = tasks.restore(taskId) ?: return@launch
                deletions.notify(UndoNotice(UndoMessage.Reopened, UndoKind.Restored) { tasks.undoRestore(previous) })
            }
            TrashTaskIntent.DeleteForever -> appScope.launch {
                deletions.emit(tasks.deleteTask(taskId))
            }
        }
    }

    private fun close() {
        if (closed) return
        closed = true
        sendEffect(TrashTaskEffect.Close)
    }
}
