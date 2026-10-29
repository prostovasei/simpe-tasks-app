package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Удаление задачи с «Отменой» (итерация 36, бэклог №34): шторка закрывается, а подсказку «Задача удалена · Отмена» показывает тот,
 * кто остался под ней, — экран приложения или прозрачное окно поверх рабочего стола. Событие получают только те, кто сейчас на экране.
 */
@Singleton
class TaskDeletions @Inject constructor(
    private val tasks: TaskRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<Deletion>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<Deletion> = _events.asSharedFlow()

    fun emit(deletion: Deletion) {
        _events.tryEmit(deletion)
    }

    /** Возвращает удалённое; запись идёт в области приложения, чтобы окно могло закрыться сразу. */
    fun undo(deletion: Deletion) {
        appScope.launch { tasks.undoDelete(deletion) }
    }

    private companion object {
        const val EVENT_BUFFER = 4
    }
}

/** Доступ к [TaskDeletions] из Compose: подсказка «Отмена» в приложении и в окне поверх рабочего стола. */
@HiltViewModel
class TaskDeletionsHost @Inject constructor(val deletions: TaskDeletions) : ViewModel()
