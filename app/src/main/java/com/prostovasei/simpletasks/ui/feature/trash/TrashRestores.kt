package com.prostovasei.simpletasks.ui.feature.trash

import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.domain.model.Restoration
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
 * Восстановление из корзины с «Отменой» (итерация 39, бэклог №35): возвращает задачи, а подсказку «Восстановлено: N · Отмена»
 * показывает приложение (`AppNavHost`) — и когда вернули из раздела, и когда из открытой задачи. Запись идёт в области приложения,
 * чтобы экран мог закрыться сразу.
 */
@Singleton
class TrashRestores @Inject constructor(
    private val tasks: TaskRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<Restoration>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<Restoration> = _events.asSharedFlow()

    fun restore(taskId: String) {
        appScope.launch { tasks.restoreFromTrash(taskId)?.let { _events.tryEmit(it) } }
    }

    fun restoreAll() {
        appScope.launch {
            val restoration = tasks.restoreAllFromTrash()
            if (restoration.count > 0) _events.tryEmit(restoration)
        }
    }

    fun undo(restoration: Restoration) {
        appScope.launch { tasks.undoRestoreFromTrash(restoration) }
    }

    private companion object {
        const val EVENT_BUFFER = 4
    }
}

/** Доступ к [TrashRestores] из Compose: подсказка «Отмена» в приложении. */
@HiltViewModel
class TrashRestoresHost @Inject constructor(val restores: TrashRestores) : ViewModel()
