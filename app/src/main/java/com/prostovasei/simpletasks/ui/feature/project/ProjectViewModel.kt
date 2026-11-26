package com.prostovasei.simpletasks.ui.feature.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.mergeWithPending
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tasks: TaskRepository,
    private val projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    sorts: com.prostovasei.simpletasks.domain.repository.TaskSortStore,
) : MviViewModel<ProjectState, TaskListIntent, TaskListEffect>(ProjectState()) {

    /** Аргумент маршрута ProjectDestination(projectId). */
    private val projectId: String = checkNotNull(savedStateHandle["projectId"]) { "Не передан projectId" }
    private val completion = CompletionTracker(viewModelScope, tasks, time)
    private var closed = false
    private val sorting = com.prostovasei.simpletasks.ui.common.ListSorting(viewModelScope, tasks, sorts, com.prostovasei.simpletasks.domain.model.SortScope.Project(projectId))

    init {
        viewModelScope.launch {
            combine(
                projects.observeProject(projectId),
                dayTicker.today().flatMapLatest { day -> tasks.observeProjectTasks(projectId, day).map { day to it } },
                completion.pending,
                sorting.observe(),
            ) { project, (day, list), pending, sort ->
                if (project == null) return@combine null
                val byId = mapOf(project.id to project)
                // #357: порядок по сортировке списка; подписи дней строит экран.
                val sorted = com.prostovasei.simpletasks.domain.model.TaskSorter.sort(list, sort, time.zone())
                ProjectUpdate(project, pending, sorted.map { it.toItem(byId, day, time.zone(), pending) }, sort, day)
            }.collect { data ->
                if (data == null) {
                    close()
                    return@collect
                }
                val (project, pending, fresh, sort, day) = data
                setState {
                    copy(
                        name = project.name,
                        groupId = project.groupId,
                        color = project.color,
                        emoji = project.emojiShown,
                        isInbox = project.isInbox,
                        tasks = mergeWithPending(this.tasks, fresh, pending),
                        isLoading = false,
                        sort = sort,
                        today = day,
                    )
                }
            }
        }
    }

    override fun onIntent(intent: TaskListIntent) {
        val sorted = sorting.handle(
            intent, { currentState.tasks }, { currentState.sort }, { currentState.today },
            setItems = { items -> setState { copy(tasks = items) } }, setAsk = { ask -> setState { copy(askManual = ask) } },
        )
        if (sorted) return
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            is ProjectIntent.OpenDialog -> setState { copy(dialog = intent.dialog) }
            ProjectIntent.DismissDialog -> setState { copy(dialog = ProjectDialog.None) }
            is ProjectIntent.Rename -> {
                val state = currentState
                update { projects.updateProject(projectId, intent.name, state.groupId, state.color) }
            }
            is ProjectIntent.SetEmoji -> update { projects.setProjectEmoji(projectId, intent.emoji) }
            ProjectIntent.ConfirmDelete -> {
                setState { copy(dialog = ProjectDialog.None) }
                if (!currentState.isInbox) viewModelScope.launch { projects.deleteProject(projectId) }
            }
            else -> Unit
        }
    }

    private fun update(action: suspend () -> Unit) {
        setState { copy(dialog = ProjectDialog.None) }
        viewModelScope.launch { action() }
    }

    private fun close() {
        if (closed) return
        closed = true
        sendEffect(ProjectEffect.Close)
    }
}

private data class ProjectUpdate(
    val project: com.prostovasei.simpletasks.domain.model.Project,
    val pending: Set<String>,
    val items: List<com.prostovasei.simpletasks.ui.common.TaskItem>,
    val sort: com.prostovasei.simpletasks.domain.model.TaskSort,
    val day: java.time.LocalDate,
)
