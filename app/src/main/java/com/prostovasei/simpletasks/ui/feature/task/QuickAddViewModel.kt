package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Быстрое создание задачи (SPEC.md §4.4). */
data class QuickAddState(
    val title: String = "",
    val content: String = "",
    val projectId: String = "",
    val priority: Priority = Priority.NONE,
    val due: TaskDue? = null,
    val rrule: String? = null,
    /** null — чек-листа нет; список — поля пунктов (последнее может быть пустым). */
    val checklist: List<String>? = null,
    val projects: List<ProjectOption> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && projectId.isNotEmpty() && !isSaving
}

sealed interface QuickAddIntent {
    data class Title(val text: String) : QuickAddIntent
    data class Content(val text: String) : QuickAddIntent
    data class Edit(val edit: FieldEdit) : QuickAddIntent
    data object AddChecklist : QuickAddIntent
    data class ChecklistItem(val index: Int, val text: String) : QuickAddIntent
    data class RemoveChecklistItem(val index: Int) : QuickAddIntent
    data object Save : QuickAddIntent
}

sealed interface QuickAddEffect {
    data object Close : QuickAddEffect
}

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    time: TimeProvider,
) : MviViewModel<QuickAddState, QuickAddIntent, QuickAddEffect>(
    QuickAddState(
        today = time.today(),
        zone = time.zone(),
        // Открыто с экрана «Все» — по умолчанию задача на сегодня.
        due = if (savedStateHandle.get<Boolean>("today") == true) TaskDue.AllDay(time.today()) else null,
    ),
) {
    /** Список, из которого открыто создание; без него — «Входящие». */
    private val presetProjectId: String? = savedStateHandle["projectId"]

    init {
        viewModelScope.launch {
            projects.observeProjectTree().collect { tree ->
                val options = tree.options()
                setState {
                    val chosen = projectId.takeIf { id -> options.any { it.id == id } }
                        ?: presetProjectId?.takeIf { id -> options.any { it.id == id } }
                        ?: tree.inbox?.project?.id.orEmpty()
                    copy(projects = options, projectId = chosen)
                }
            }
        }
    }

    override fun onIntent(intent: QuickAddIntent) {
        when (intent) {
            is QuickAddIntent.Title -> setState { copy(title = intent.text) }
            is QuickAddIntent.Content -> setState { copy(content = intent.text) }
            is QuickAddIntent.Edit -> setState {
                when (val edit = intent.edit) {
                    is FieldEdit.Due -> copy(due = edit.due, rrule = if (edit.due == null) null else rrule)
                    is FieldEdit.Project -> copy(projectId = edit.projectId)
                    is FieldEdit.SetPriority -> copy(priority = edit.priority)
                    is FieldEdit.Repeat -> copy(rrule = edit.rrule)
                }
            }
            QuickAddIntent.AddChecklist -> setState { copy(checklist = (checklist ?: emptyList()) + "") }
            is QuickAddIntent.ChecklistItem -> setState {
                copy(checklist = checklist?.mapIndexed { i, old -> if (i == intent.index) intent.text else old })
            }
            is QuickAddIntent.RemoveChecklistItem -> setState {
                copy(checklist = checklist?.filterIndexed { i, _ -> i != intent.index }?.takeIf { it.isNotEmpty() })
            }
            QuickAddIntent.Save -> save()
        }
    }

    private fun save() {
        val state = currentState
        if (!state.canSave) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            tasks.createTask(
                NewTask(
                    projectId = state.projectId,
                    title = state.title.trim(),
                    content = state.content.trim(),
                    priority = state.priority,
                    due = state.due,
                    checklist = state.checklist.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
                    rrule = state.rrule,
                ),
            )
            sendEffect(QuickAddEffect.Close)
        }
    }
}
