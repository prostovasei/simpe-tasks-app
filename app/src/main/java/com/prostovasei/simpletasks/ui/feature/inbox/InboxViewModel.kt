package com.prostovasei.simpletasks.ui.feature.inbox

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.mergeWithPending
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxState(
    val tasks: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    /** #357: сортировка «Входящих», сегодняшний день и окно «Переключить на ручную?». */
    val sort: com.prostovasei.simpletasks.domain.model.TaskSort = com.prostovasei.simpletasks.domain.model.TaskSort.LIST_DEFAULT,
    val today: java.time.LocalDate? = null,
    val askManual: Boolean = false,
)

/** «Входящие» (SPEC.md §4.2; итерация 52 — решение PM): только неразобранное — задачи без даты и без списка. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    sorts: com.prostovasei.simpletasks.domain.repository.TaskSortStore,
) : MviViewModel<InboxState, TaskListIntent, TaskListEffect>(InboxState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)
    private val sorting = com.prostovasei.simpletasks.ui.common.ListSorting(viewModelScope, tasks, sorts, com.prostovasei.simpletasks.domain.model.SortScope.Inbox)

    init {
        viewModelScope.launch {
            val inbox = dayTicker.today().flatMapLatest { day -> tasks.observeInbox(day).map { day to it } }
            combine(inbox, projects.observeProjectTree(), completion.pending, sorting.observe()) { (day, list), tree, pending, sort ->
                val byId = tree.projectsById()
                val sorted = com.prostovasei.simpletasks.domain.model.TaskSorter.sort(list, sort, time.zone())
                InboxUpdate(pending, sorted.map { it.toItem(byId, day, time.zone(), pending) }, sort, day)
            }.collect { (pending, fresh, sort, day) ->
                setState { copy(tasks = mergeWithPending(this.tasks, fresh, pending), isLoading = false, sort = sort, today = day) }
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
            else -> Unit
        }
    }
}

private data class InboxUpdate(
    val pending: Set<String>,
    val items: List<TaskItem>,
    val sort: com.prostovasei.simpletasks.domain.model.TaskSort,
    val day: java.time.LocalDate,
)
