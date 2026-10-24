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

data class InboxState(val tasks: List<TaskItem> = emptyList(), val isLoading: Boolean = true)

/** «Входящие» (SPEC.md §4.2): задачи системного списка и задачи без срока из всех списков (решение PM). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<InboxState, TaskListIntent, TaskListEffect>(InboxState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)

    init {
        viewModelScope.launch {
            val inbox = dayTicker.today().flatMapLatest { day -> tasks.observeInbox(day).map { day to it } }
            combine(inbox, projects.observeProjectTree(), completion.pending) { (day, list), tree, pending ->
                val byId = tree.projectsById()
                pending to list.map { it.toItem(byId, day, time.zone(), pending) }
            }.collect { (pending, fresh) ->
                setState { copy(tasks = mergeWithPending(this.tasks, fresh, pending), isLoading = false) }
            }
        }
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            else -> Unit
        }
    }
}
