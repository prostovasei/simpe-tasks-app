package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.alignedToRepeat
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.repository.ChecklistRepository
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Экран задачи (SPEC.md §4.5): правка всех полей с автосохранением, чек-лист, подзадачи. */
data class TaskDetailState(
    val task: Task? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val subtasks: List<Task> = emptyList(),
    val projects: List<ProjectOption> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
    val showDeleteConfirm: Boolean = false,
    /** Пункт чек-листа, который переименовывается (диалог). */
    val renamingItem: ChecklistItem? = null,
)

sealed interface TaskDetailIntent {
    data class Title(val text: String) : TaskDetailIntent
    data class Content(val text: String) : TaskDetailIntent
    data class Edit(val edit: FieldEdit) : TaskDetailIntent
    data object ToggleCompleted : TaskDetailIntent
    data object RequestDelete : TaskDetailIntent
    data object DismissDelete : TaskDetailIntent
    data object ConfirmDelete : TaskDetailIntent
    data class AddItem(val title: String) : TaskDetailIntent
    data class ToggleItem(val itemId: String) : TaskDetailIntent
    data class StartRenameItem(val itemId: String) : TaskDetailIntent
    data class RenameItem(val title: String) : TaskDetailIntent
    data object DismissRename : TaskDetailIntent
    data class DeleteItem(val itemId: String) : TaskDetailIntent
    data class MoveItem(val itemId: String, val up: Boolean) : TaskDetailIntent
    data class ToggleSubtask(val taskId: String) : TaskDetailIntent
}

sealed interface TaskDetailEffect {
    data object Close : TaskDetailEffect

    /** Повторяющаяся задача выполнена и перенесена: «Следующий раз: …». */
    data class Rescheduled(val next: DueText) : TaskDetailEffect
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    private val checklist: ChecklistRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : MviViewModel<TaskDetailState, TaskDetailIntent, TaskDetailEffect>(TaskDetailState(today = time.today(), zone = time.zone())) {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"]) { "Не передан taskId" }

    /** Последняя сохранённая версия: с ней сравнивается черновик. */
    private var saved: Task? = null
    private var textSaveJob: Job? = null

    /** Есть правка текста, которая ещё не записана. */
    private var textDirty = false
    private var lastWrite: Job? = null
    private var closed = false

    init {
        viewModelScope.launch {
            tasks.observeTaskDetails(taskId).collect { details ->
                if (details == null) {
                    if (saved != null || currentState.task == null) close()
                    return@collect
                }
                val fromDb = details.task
                setState {
                    // Поля, которые человек правит, берутся из БД только при первой загрузке —
                    // иначе собственное автосохранение сбрасывало бы курсор в поле ввода.
                    val merged = task?.copy(status = fromDb.status, completedAt = fromDb.completedAt, updatedAt = fromDb.updatedAt) ?: fromDb
                    copy(task = merged, checklist = details.checklist)
                }
                if (saved == null) saved = fromDb
            }
        }
        viewModelScope.launch { tasks.observeSubtasks(taskId).collect { setState { copy(subtasks = it) } } }
        viewModelScope.launch { projects.observeProjectTree().collect { tree -> setState { copy(projects = tree.options()) } } }
        viewModelScope.launch { dayTicker.today().collect { setState { copy(today = it) } } }
    }

    override fun onIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.Title -> editText { copy(title = intent.text) }
            is TaskDetailIntent.Content -> editText { copy(content = intent.text) }
            is TaskDetailIntent.Edit -> editNow {
                when (val edit = intent.edit) {
                    // Повтор начинается с первого подходящего дня от выбранной даты (решение PM).
                    is FieldEdit.Due -> withDue(edit.due).alignedToRepeat(time.zone())
                    is FieldEdit.Project -> copy(projectId = edit.projectId)
                    is FieldEdit.SetPriority -> copy(priority = edit.priority)
                    is FieldEdit.Repeat -> copy(rrule = edit.rrule).alignedToRepeat(time.zone())
                    is FieldEdit.SetReminders -> copy(reminders = edit.reminders)
                }
            }
            TaskDetailIntent.ToggleCompleted -> toggleCompleted()
            TaskDetailIntent.RequestDelete -> setState { copy(showDeleteConfirm = true) }
            TaskDetailIntent.DismissDelete -> setState { copy(showDeleteConfirm = false) }
            TaskDetailIntent.ConfirmDelete -> {
                setState { copy(showDeleteConfirm = false) }
                textSaveJob?.cancel()
                saved = null
                appScope.launch { tasks.deleteTask(taskId) }
                close()
            }
            is TaskDetailIntent.AddItem -> intent.title.trim().takeIf { it.isNotEmpty() }?.let { title ->
                viewModelScope.launch { checklist.addItem(taskId, title) }
            }
            is TaskDetailIntent.ToggleItem -> currentState.checklist.firstOrNull { it.id == intent.itemId }?.let { item ->
                viewModelScope.launch { checklist.setDone(item.id, !item.isDone) }
            }
            is TaskDetailIntent.StartRenameItem -> setState { copy(renamingItem = checklist.firstOrNull { it.id == intent.itemId }) }
            TaskDetailIntent.DismissRename -> setState { copy(renamingItem = null) }
            is TaskDetailIntent.RenameItem -> {
                val item = currentState.renamingItem ?: return
                setState { copy(renamingItem = null) }
                intent.title.trim().takeIf { it.isNotEmpty() }?.let { viewModelScope.launch { checklist.renameItem(item.id, it) } }
            }
            is TaskDetailIntent.DeleteItem -> viewModelScope.launch { checklist.deleteItem(intent.itemId) }
            is TaskDetailIntent.MoveItem -> moveItem(intent.itemId, intent.up)
            is TaskDetailIntent.ToggleSubtask -> currentState.subtasks.firstOrNull { it.id == intent.taskId }?.let { sub ->
                viewModelScope.launch {
                    if (sub.status == TaskStatus.ACTIVE) tasks.complete(sub.id) else tasks.setCompleted(sub.id, completed = false)
                }
            }
        }
    }

    /**
     * Отметка задачи. Сначала дописывается черновик, потом задача отмечается. Повторяющаяся переезжает
     * на следующий раз: новый срок сразу показывается на экране, галочка не ставится.
     */
    private fun toggleCompleted() {
        val task = currentState.task ?: return
        textSaveJob?.cancel()
        persist()
        val previous = lastWrite
        lastWrite = appScope.launch {
            previous?.join()
            if (task.status != TaskStatus.ACTIVE) {
                tasks.setCompleted(task.id, completed = false)
                return@launch
            }
            val result = tasks.complete(task.id)
            if (result is Completion.Rescheduled) {
                val next = result.next
                saved = saved?.copy(due = next.due, start = next.start, rrule = next.rrule, reminders = next.reminders)
                setState { copy(task = this.task?.copy(due = next.due, start = next.start, rrule = next.rrule, reminders = next.reminders)) }
                sendEffect(TaskDetailEffect.Rescheduled(DueFormatter.format(next.due, next.start, time.today(), time.zone()) ?: return@launch))
            }
        }
    }

    /** Название и описание сохраняются через полсекунды после последнего изменения. */
    private fun editText(change: Task.() -> Task) {
        val task = currentState.task ?: return
        setState { copy(task = task.change()) }
        textDirty = true
        textSaveJob?.cancel()
        textSaveJob = viewModelScope.launch {
            delay(TEXT_SAVE_DELAY_MS)
            persist()
        }
    }

    /** Дата, список, приоритет и повтор сохраняются сразу. */
    private fun editNow(change: Task.() -> Task) {
        val task = currentState.task ?: return
        setState { copy(task = task.change()) }
        textSaveJob?.cancel()
        persist()
    }

    /**
     * Пишет черновик, если он отличается от сохранённого. Запись идёт в области приложения и по очереди:
     * последняя правка сохранится, даже если экран уже закрыт.
     */
    private fun persist() {
        textDirty = false
        val draft = currentState.task ?: return
        val base = saved ?: return
        val toSave = if (draft.title.isBlank()) draft.copy(title = base.title) else draft.copy(title = draft.title.trim())
        if (toSave.editableEquals(base)) return
        saved = toSave
        val previous = lastWrite
        lastWrite = appScope.launch {
            previous?.join()
            tasks.updateTask(toSave)
        }
    }

    private fun moveItem(itemId: String, up: Boolean) {
        val items = currentState.checklist
        val index = items.indexOfFirst { it.id == itemId }
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in items.indices) return
        val reordered = items.toMutableList().apply { add(target, removeAt(index)) }
        setState { copy(checklist = reordered) }
        viewModelScope.launch { checklist.reorder(taskId, reordered.map { it.id }) }
    }

    private fun close() {
        if (closed) return
        closed = true
        sendEffect(TaskDetailEffect.Close)
    }

    override fun onCleared() {
        // Экран закрыт до истечения задержки (viewModelScope к этому моменту уже отменён) — сохраняем последний текст.
        if (textDirty) persist()
        super.onCleared()
    }

    private companion object {
        const val TEXT_SAVE_DELAY_MS = 500L
    }
}

private fun Task.editableEquals(other: Task) =
    title == other.title && content == other.content && projectId == other.projectId && priority == other.priority &&
        due == other.due && start == other.start && rrule == other.rrule && reminders == other.reminders
