package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.core.di.ApplicationScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.UndoKind
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

    private val _notices = MutableSharedFlow<UndoNotice>(extraBufferCapacity = EVENT_BUFFER)

    /** Другие действия шторки с «Отменой» (итерация 49): «Не буду делать», «Вернуть в работу». */
    val notices: SharedFlow<UndoNotice> = _notices.asSharedFlow()

    fun emit(deletion: Deletion) {
        _events.tryEmit(deletion)
    }

    fun notify(notice: UndoNotice) {
        _notices.tryEmit(notice)
    }

    /** Выполняет [undo] в области приложения: окно может закрыться сразу. */
    fun undo(notice: UndoNotice) {
        appScope.launch { notice.undo() }
    }

    /** Возвращает удалённое; запись идёт в области приложения, чтобы окно могло закрыться сразу. */
    fun undo(deletion: Deletion) {
        appScope.launch { tasks.undoDelete(deletion) }
    }

    private companion object {
        const val EVENT_BUFFER = 4
    }
}

/** Подсказка «… · Отмена» со значком [kind]; текст собирает экран по [message] ([UndoMessages]); [undo] возвращает как было. */
class UndoNotice(val message: UndoMessage, val kind: UndoKind, val undo: suspend () -> Unit)

/** Что сказать в подсказке (итерация 49). */
sealed interface UndoMessage {
    /** «Не буду делать[ · вс, 08 ноя][ · пропущено: N | выполнено: N]». */
    data class WontDo(val day: String?, val skipped: Int = 0, val completed: Int = 0) : UndoMessage

    /** «Задача возвращена». */
    data object Reopened : UndoMessage

    /** «Перенесено на пн, 09 ноя» (итерация 50). */
    data class Moved(val day: String) : UndoMessage

    /** «Отложено на 18:00» (итерация 54: «Отложить» в окне задачи). */
    data class Snoozed(val whenText: String) : UndoMessage
}

/** Тексты подсказок [UndoMessage]; строки берутся один раз в композиции. */
class UndoMessages private constructor(private val strings: Map<Int, String>) {
    fun text(message: UndoMessage): String = when (message) {
        is UndoMessage.WontDo -> when {
            message.day == null -> strings.getValue(R.string.wontdo_done)
            message.skipped > 0 -> strings.getValue(R.string.wontdo_done_skipped).format(message.day, message.skipped)
            message.completed > 0 -> strings.getValue(R.string.wontdo_done_completed).format(message.day, message.completed)
            else -> strings.getValue(R.string.wontdo_done_on).format(message.day)
        }
        UndoMessage.Reopened -> strings.getValue(R.string.wontdo_reopened)
        is UndoMessage.Moved -> strings.getValue(R.string.repeat_moved).format(message.day)
        is UndoMessage.Snoozed -> strings.getValue(R.string.snooze_done).format(message.whenText)
    }

    companion object {
        private val IDS = listOf(R.string.wontdo_done, R.string.wontdo_done_on, R.string.wontdo_done_skipped, R.string.wontdo_done_completed, R.string.wontdo_reopened, R.string.repeat_moved, R.string.snooze_done)

        @Composable
        fun load(): UndoMessages = UndoMessages(IDS.associateWith { stringResource(it) })
    }
}

/** Доступ к [TaskDeletions] из Compose: подсказка «Отмена» в приложении и в окне поверх рабочего стола. */
@HiltViewModel
class TaskDeletionsHost @Inject constructor(val deletions: TaskDeletions) : ViewModel()
