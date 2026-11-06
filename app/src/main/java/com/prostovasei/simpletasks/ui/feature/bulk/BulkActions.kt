package com.prostovasei.simpletasks.ui.feature.bulk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.model.Restoration
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.movedTo
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.feature.task.ProjectOption
import com.prostovasei.simpletasks.ui.feature.task.options
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Что сделали с выбранными задачами — от этого зависят текст и значок подсказки «Отмена». */
enum class BulkKind { Done, Tomorrow, OtherDay, Moved, Priority, Reopened, Deleted, Restored, WontDo }

/**
 * Итог массового действия (итерация 40): [count] задач; [date] — куда перенесли («На другой день»); [listName] — в какой список
 * (null — «Входящие»); [undo] возвращает всё как было.
 */
class BulkResult(
    val kind: BulkKind,
    val count: Int,
    val date: LocalDate? = null,
    val listName: String? = null,
    val undo: suspend () -> Unit,
)

/**
 * Массовое редактирование (итерация 40, бэклог №37): действия над выбранными задачами. Запись идёт в области приложения — режим
 * выбора закрывается сразу; подсказку «… · Отмена» показывает приложение (`AppNavHost`). Подтверждений нет: всё обратимо,
 * кроме «Удалить навсегда» в корзине (его подтверждает экран).
 */
@Singleton
class BulkActions @Inject constructor(
    private val tasks: TaskRepository,
    private val time: TimeProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<BulkResult>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<BulkResult> = _events.asSharedFlow()

    /** Повторяющиеся едут на следующий раз — как при одиночной отметке. */
    fun complete(ids: List<String>) = run {
        val done = ids.map { tasks.complete(it) }
        BulkResult(BulkKind.Done, done.size) { done.forEach { tasks.undo(it) } }
    }

    /** «Не буду делать» (итерация 49): обычные закрываются, у повторяющихся — текущий раз (серия едет дальше). */
    fun wontDo(ids: List<String>) = run {
        val results = ids.mapNotNull { tasks.wontDo(it) }
        BulkResult(BulkKind.WontDo, results.size) { results.forEach { tasks.undoWontDo(it) } }
    }

    fun toTomorrow(ids: List<String>) = run {
        val date = time.today().plusDays(1)
        edit(ids) { it.withDue(it.due.movedTo(date, time.zone())) }.result(BulkKind.Tomorrow)
    }

    /** Срок — на [date], время дня то же; у задачи без срока — весь день. */
    fun toDay(ids: List<String>, date: LocalDate) = run {
        edit(ids) { it.withDue(it.due.movedTo(date, time.zone())) }.result(BulkKind.OtherDay, date = date)
    }

    fun moveTo(ids: List<String>, projectId: String, listName: String?) = run {
        edit(ids) { it.copy(projectId = projectId) }.result(BulkKind.Moved, listName = listName)
    }

    fun setPriority(ids: List<String>, priority: Priority) = run {
        edit(ids) { it.copy(priority = priority) }.result(BulkKind.Priority)
    }

    /** Выполненные — снова в активные. */
    fun reopen(ids: List<String>) = run {
        val previous = ids.mapNotNull { tasks.restore(it) }
        BulkResult(BulkKind.Reopened, previous.size) { previous.forEach { tasks.undoRestore(it) } }
    }

    /** В корзину. */
    fun delete(ids: List<String>) = run {
        val deletions = ids.map { tasks.deleteTask(it) }
        BulkResult(BulkKind.Deleted, deletions.size) { deletions.forEach { tasks.undoDelete(it) } }
    }

    /** Из корзины — в прежние списки. */
    fun restoreFromTrash(ids: List<String>) = run {
        val restoration = Restoration(ids.mapNotNull { tasks.restoreFromTrash(it) }.flatMap { it.deletions })
        BulkResult(BulkKind.Restored, restoration.count) { tasks.undoRestoreFromTrash(restoration) }
    }

    /** Стирает навсегда (подтверждено на экране); «Отмены» нет. */
    fun deleteForever(ids: List<String>) {
        appScope.launch { ids.forEach { tasks.deleteForever(it) } }
    }

    fun undo(result: BulkResult) {
        appScope.launch { result.undo() }
    }

    private fun run(action: suspend () -> BulkResult) {
        appScope.launch {
            val result = action()
            if (result.count > 0) _events.tryEmit(result)
        }
    }

    /** Меняет задачи по [transform]; результат — какими они были, для «Отмены». */
    private suspend fun edit(ids: List<String>, transform: (Task) -> Task): Rescheduling {
        val previous = ids.mapNotNull { tasks.getTask(it) }
        previous.forEach { tasks.updateTask(transform(it)) }
        return Rescheduling(previous)
    }

    private fun Rescheduling.result(kind: BulkKind, date: LocalDate? = null, listName: String? = null) =
        BulkResult(kind, count, date, listName) { tasks.undoReschedule(this) }

    private companion object {
        const val EVENT_BUFFER = 4
    }
}

/** Доступ к [BulkActions] из Compose: действия ленты, списки для окна «В список» и сегодняшний день для календаря. */
@HiltViewModel
class BulkActionsHost @Inject constructor(
    val actions: BulkActions,
    projects: ProjectRepository,
    time: TimeProvider,
) : ViewModel() {
    val today: LocalDate = time.today()
    val projectOptions: StateFlow<List<ProjectOption>> =
        projects.observeProjectTree().map { it.options() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
