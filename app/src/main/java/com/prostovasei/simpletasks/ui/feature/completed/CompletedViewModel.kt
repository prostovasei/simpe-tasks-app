package com.prostovasei.simpletasks.ui.feature.completed

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Выполненная задача в списке: [time] — когда выполнена («10:42»), если известно. */
data class CompletedTask(
    val id: String,
    val title: String,
    val listName: String?,
    /** Эмодзи списка (итерация 47). */
    val listEmoji: String,
    val isInboxList: Boolean,
    val time: String?,
)

/** Выполненные за один день. */
data class CompletedDay(val day: DayLabel, val tasks: List<CompletedTask>)

data class CompletedState(val days: List<CompletedDay> = emptyList(), val isLoading: Boolean = true)

sealed interface CompletedIntent {
    /** Снять галочку — вернуть задачу в её список. */
    data class Restore(val taskId: String) : CompletedIntent
    data object Undo : CompletedIntent
}

sealed interface CompletedEffect {
    data object Restored : CompletedEffect
}

/** Экран «Выполненные» (итерация 11): все выполненные задачи по дням, новые сверху; снять галочку — вернуть. */
@HiltViewModel
class CompletedViewModel @Inject constructor(
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
) : MviViewModel<CompletedState, CompletedIntent, CompletedEffect>(CompletedState()) {

    /** Последняя возвращённая задача — для «Отмены». */
    private var lastRestored: Task? = null

    init {
        viewModelScope.launch {
            combine(tasks.observeCompleted(), projects.observeProjectTree(), dayTicker.today()) { list, tree, today ->
                groupByDay(list, tree.projectsById(), today, time.zone())
            }.collect { days -> setState { copy(days = days, isLoading = false) } }
        }
    }

    override fun onIntent(intent: CompletedIntent) {
        when (intent) {
            is CompletedIntent.Restore -> viewModelScope.launch {
                lastRestored = tasks.restore(intent.taskId)
                if (lastRestored != null) sendEffect(CompletedEffect.Restored)
            }
            CompletedIntent.Undo -> {
                val previous = lastRestored ?: return
                lastRestored = null
                viewModelScope.launch { tasks.undoRestore(previous) }
            }
        }
    }

    companion object {
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /** Группы по дню выполнения; у импортированных без времени выполнения — по времени изменения. */
        fun groupByDay(list: List<Task>, projects: Map<String, Project>, today: LocalDate, zone: ZoneId): List<CompletedDay> =
            list.groupBy { (it.completedAt ?: it.updatedAt).atZone(zone).toLocalDate() }
                .map { (date, dayTasks) ->
                    CompletedDay(
                        day = DueFormatter.dayLabel(date, today),
                        tasks = dayTasks.map { task ->
                            val project = projects[task.projectId]
                            CompletedTask(
                                id = task.id,
                                title = task.title,
                                listName = project?.name,
                                listEmoji = project?.let { ListEmoji.ofTask(it.isInbox, task.due != null, it.emojiShown) } ?: ListEmoji.DEFAULT,
                                isInboxList = project?.isInbox == true,
                                time = task.completedAt?.atZone(zone)?.let(TIME::format),
                            )
                        },
                    )
                }
    }
}
