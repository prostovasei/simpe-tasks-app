package com.prostovasei.simpletasks.ui.feature.all

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.keepPending
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.common.upcomingWithRepeats
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
class AllViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<AllState, TaskListIntent, TaskListEffect>(AllState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)

    init {
        viewModelScope.launch {
            val sections = dayTicker.today().flatMapLatest { day -> tasks.observeAllScreen(day).map { day to it } }
            combine(sections, projects.observeProjectTree(), completion.pending) { (day, all), tree, pending ->
                val byId = tree.projectsById()
                val zone = time.zone()
                val everything = all.overdue + all.todayAllDay + all.todayTimed + all.upcoming
                pending to listOf(all.overdue, all.todayAllDay, all.todayTimed).map { section -> section.map { it.toItem(byId, day, zone, pending) } } +
                    listOf(upcomingWithRepeats(all.upcoming, everything, byId, day, zone, pending))
            }.collect { (pending, fresh) ->
                setState {
                    val merged = listOf(overdue, todayAllDay, todayTimed, upcoming)
                        .zip(fresh) { previous, current -> mergeWithPending(previous, current, pending) }
                    copy(overdue = merged[0], todayAllDay = merged[1], todayTimed = merged[2], upcoming = merged[3], isLoading = false)
                }
            }
        }
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            else -> Unit
        }
    }
}

/** Свежий список + только что отмеченные задачи на прежних местах, с актуальным признаком «отмечается». */
fun mergeWithPending(previous: List<TaskItem>, current: List<TaskItem>, pending: Set<String>): List<TaskItem> =
    keepPending(previous, current, pending, TaskItem::id).map { it.copy(isCompleting = it.id in pending) }

/** Все списки дерева по id (включая «Входящие»). */
fun ProjectTree.projectsById(): Map<String, Project> =
    (listOfNotNull(inbox) + groups.flatMap { it.projects } + ungrouped).associate { it.project.id to it.project }
