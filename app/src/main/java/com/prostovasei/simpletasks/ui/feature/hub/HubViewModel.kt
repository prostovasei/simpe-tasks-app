package com.prostovasei.simpletasks.ui.feature.hub

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
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.feature.week.buildWeek
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Задачи хаба (#329): просроченные, сегодня и завтра — как у дня в «Неделе». */
data class HubState(
    val today: LocalDate? = null,
    val overdue: List<TaskItem> = emptyList(),
    /** #350: ближайшая неделя — сегодня и ещё шесть дней, по порядку. */
    val days: List<Pair<LocalDate, List<TaskItem>>> = emptyList(),
    val isLoading: Boolean = true,
) {
    val todayItems: List<TaskItem> get() = days.getOrNull(0)?.second.orEmpty()
    val tomorrowItems: List<TaskItem> get() = days.getOrNull(1)?.second.orEmpty()
    val isEmpty: Boolean get() = !isLoading && overdue.isEmpty() && days.all { it.second.isEmpty() }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HubViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    private val sorts: com.prostovasei.simpletasks.domain.repository.TaskSortStore,
) : MviViewModel<HubState, TaskListIntent, TaskListEffect>(HubState()) {

    private val completion = CompletionTracker(viewModelScope, tasks, time)

    init {
        viewModelScope.launch {
            dayTicker.today().flatMapLatest { today ->
                val last = today.plusDays(HUB_DAYS - 1L)
                combine(
                    tasks.observeAllScreen(today),
                    tasks.observeWeek(today.weekStart()),
                    tasks.observeWeek(last.weekStart()),
                    projects.observeProjectTree(),
                    completion.pending,
                ) { all, week, nextWeek, tree, pending ->
                    val byId = tree.projectsById()
                    val zone = time.zone()
                    fun day(date: LocalDate, source: com.prostovasei.simpletasks.domain.model.WeekTasks) =
                        buildWeek(date.weekStart(), source, byId, today, zone, pending).first { it.date == date }.items
                    HubState(
                        today = today,
                        overdue = all.overdue.map { it.toItem(byId, today, zone, pending) },
                        days = (0 until HUB_DAYS).map { offset ->
                            val date = today.plusDays(offset.toLong())
                            date to day(date, if (date.weekStart() == today.weekStart()) week else nextWeek)
                        },
                        isLoading = false,
                    )
                }
            }.combine(sorts.observe(com.prostovasei.simpletasks.domain.model.SortScope.Upcoming)) { fresh, sort ->
                // #357: порядок внутри дня — как в «Предстоящем».
                fresh.copy(
                    overdue = com.prostovasei.simpletasks.ui.common.DaySort.byDay(fresh.overdue, sort),
                    days = fresh.days.map { (date, items) -> date to com.prostovasei.simpletasks.ui.common.DaySort.apply(items, sort) },
                )
            }.collect { fresh -> setState { fresh } }
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

/** #350: хаб показывает задачи на неделю вперёд (сегодня + 6 дней). */
const val HUB_DAYS = 7
