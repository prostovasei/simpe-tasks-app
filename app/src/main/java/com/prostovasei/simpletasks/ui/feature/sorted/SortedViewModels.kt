package com.prostovasei.simpletasks.ui.feature.sorted

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.SortCounts
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.CompletionTracker
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffect
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Группа экрана: список (у «Когда-нибудь») или день (у «Без списка») и его задачи. */
data class SortedGroup(
    val key: String,
    /** Список группы («Когда-нибудь»): эмодзи и название; у «Без списка» — null. */
    val list: Project? = null,
    /** День группы («Без списка»); у «Когда-нибудь» — null. */
    val date: LocalDate? = null,
    val tasks: List<TaskItem>,
)

data class SortedState(
    val groups: List<SortedGroup> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val isLoading: Boolean = true,
) {
    /** Открытых задач на экране — число справа от заголовка. */
    val openCount: Int get() = groups.sumOf { group -> group.tasks.count { !it.showsDone } }
}

/** Общее для «Когда-нибудь» и «Без списка» (итерация 52): задачи из [source], отметка кружком с «Отменой». */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class SortedViewModel(
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    source: (LocalDate) -> Flow<List<Task>>,
) : MviViewModel<SortedState, TaskListIntent, TaskListEffect>(SortedState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)

    init {
        viewModelScope.launch {
            val list = dayTicker.today().flatMapLatest { day -> source(day).map { day to it } }
            combine(list, projects.observeProjectTree(), completion.pending) { (day, found), tree, pending ->
                SortedState(groups = group(found, tree, day, time.zone(), pending), today = day, isLoading = false)
            }.collect { fresh -> setState { fresh } }
        }
    }

    protected abstract fun group(tasks: List<Task>, tree: ProjectTree, today: LocalDate, zone: ZoneId, pending: Set<String>): List<SortedGroup>

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleComplete -> completion.toggle(intent.taskId, intent.recurring, intent.done) { sendEffect(it) }
            is TaskListIntent.Undo -> completion.undo(intent.taskId)
            else -> Unit
        }
    }
}

/**
 * «Когда-нибудь» (итерация 52, макет iteration52.py): задачи со списком, но без даты, группами по спискам в порядке экрана «Списки».
 */
@HiltViewModel
class SomedayViewModel @Inject constructor(
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    time: TimeProvider,
) : SortedViewModel(tasks, projects, dayTicker, time, tasks::observeSomeday) {
    override fun group(tasks: List<Task>, tree: ProjectTree, today: LocalDate, zone: ZoneId, pending: Set<String>): List<SortedGroup> =
        somedayGroups(tasks, tree, today, zone, pending)
}

/** «Без списка» (итерация 52): задачи без списка, но с датой, группами по дням. */
@HiltViewModel
class NoListViewModel @Inject constructor(
    tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    time: TimeProvider,
) : SortedViewModel(tasks, projects, dayTicker, time, tasks::observeNoList) {
    override fun group(tasks: List<Task>, tree: ProjectTree, today: LocalDate, zone: ZoneId, pending: Set<String>): List<SortedGroup> =
        noListGroups(tasks, tree, today, zone, pending)
}

/** Группы «Когда-нибудь»: списки в порядке экрана «Списки» (папки, потом списки без папки), внутри — порядок задач. */
fun somedayGroups(tasks: List<Task>, tree: ProjectTree, today: LocalDate, zone: ZoneId, pending: Set<String>): List<SortedGroup> {
    val byId = tree.projectsById()
    val order = (tree.groups.flatMap { it.projects } + tree.ungrouped).map { it.project.id }
    val byList = tasks.groupBy { it.projectId }
    return (order + (byList.keys - order.toSet())).mapNotNull { id ->
        val found = byList[id] ?: return@mapNotNull null
        SortedGroup(key = "list:$id", list = byId[id], tasks = found.map { it.toItem(byId, today, zone, pending) })
    }
}

/** Группы «Без списка»: дни по возрастанию (прошедшие — сверху, как в ленте «Все»). */
fun noListGroups(tasks: List<Task>, tree: ProjectTree, today: LocalDate, zone: ZoneId, pending: Set<String>): List<SortedGroup> {
    val byId = tree.projectsById()
    return tasks.groupBy { task ->
        when (val anchor = task.start ?: task.due) {
            is TaskDue.AllDay -> anchor.date
            is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
            null -> today
        }
    }.toSortedMap().map { (date, found) ->
        SortedGroup(key = "day:$date", date = date, tasks = found.map { it.toItem(byId, today, zone, pending) })
    }
}

/** Счётчики разделов для нижней панели и «Списков» (итерация 52). */
@HiltViewModel
class SortCountsHost @Inject constructor(tasks: TaskRepository) : androidx.lifecycle.ViewModel() {
    val counts: Flow<SortCounts> = tasks.observeSortCounts()
}
