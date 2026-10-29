package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    /** null — напоминание по умолчанию для срока (решение PM: включено у каждой новой задачи со сроком). */
    val customReminders: List<String>? = null,
    /** null — чек-листа нет; список — поля пунктов (последнее может быть пустым). */
    val checklist: List<String>? = null,
    val projects: List<ProjectOption> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
    /** Сейчас — минута от начала суток (красная линия и начальная прокрутка сетки). */
    val nowMinute: Int = 0,
    /** Начало интервала (итерация 17): тогда [due] — его конец; у простого времени и «на весь день» — пусто. */
    val start: TaskDue? = null,
    /** Занятое другими задачами время в видимых днях сетки (серые плашки). */
    val busy: List<BusySlot> = emptyList(),
    val isSaving: Boolean = false,
    /** Название подставлено из черновика (итерация 38): при открытии оно выделено, любая буква заменит его. */
    val draftRestored: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && projectId.isNotEmpty() && !isSaving
    val reminders: List<String> get() = customReminders ?: Reminders.default(due)

    val fields: SheetFields
        get() = SheetFields(due, start, projectId, priority, rrule, reminders, projects, today, zone, nowMinute, busy)
}

sealed interface QuickAddIntent {
    data class Title(val text: String) : QuickAddIntent
    data class Content(val text: String) : QuickAddIntent
    data class Edit(val edit: FieldEdit) : QuickAddIntent
    data class Grid(val action: GridAction) : QuickAddIntent
    data object AddChecklist : QuickAddIntent
    data class ChecklistItem(val index: Int, val text: String) : QuickAddIntent
    data class RemoveChecklistItem(val index: Int) : QuickAddIntent
    data object Save : QuickAddIntent

    /** Карточку закрыли без сохранения: название и описание уходят в черновик (итерация 38). */
    data object Dismiss : QuickAddIntent
}

sealed interface QuickAddEffect {
    data object Close : QuickAddEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    time: TimeProvider,
    private val drafts: TaskDrafts,
) : MviViewModel<QuickAddState, QuickAddIntent, QuickAddEffect>(
    QuickAddState(
        today = time.today(),
        zone = time.zone(),
        // Открыто с экрана «Все» — по умолчанию задача на сегодня.
        due = if (savedStateHandle.get<Boolean>("today") == true) TaskDue.AllDay(time.today()) else null,
        nowMinute = time.now().atZone(time.zone()).let { it.hour * 60 + it.minute },
        title = drafts.get()?.title.orEmpty(),
        content = drafts.get()?.content.orEmpty(),
        draftRestored = drafts.get() != null,
    ),
) {
    /** Задача сохранена или карточку закрыли — черновик уже решён, при уничтожении экрана его не трогаем. */
    private var finished = false

    /** Первый видимый день сетки: по нему подгружается занятое время (с запасом на день по краям). */
    private val gridFirst = MutableStateFlow(time.today())
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
        viewModelScope.launch {
            gridFirst.flatMapLatest { first ->
                val from = first.minusDays(1)
                val until = first.plusDays(WeekGridMath.VISIBLE_DAYS.toLong())
                val zone = currentState.zone
                tasks.observeTimedInRange(from.atStartOfDay(zone).toInstant(), until.plusDays(1).atStartOfDay(zone).toInstant())
                    .map { busySlots(it, zone, from, until) }
            }.collect { setState { copy(busy = it) } }
        }
    }

    override fun onIntent(intent: QuickAddIntent) {
        when (intent) {
            is QuickAddIntent.Title -> setState { copy(title = intent.text) }
            is QuickAddIntent.Content -> setState { copy(content = intent.text) }
            is QuickAddIntent.Edit -> setState { edit(intent.edit) }
            is QuickAddIntent.Grid -> when (val action = intent.action) {
                is GridAction.FirstDay -> gridFirst.value = action.first
                else -> setState { schedule(Schedule(due, start).after(action, zone)) }
            }
            QuickAddIntent.AddChecklist -> setState { copy(checklist = (checklist ?: emptyList()) + "") }
            is QuickAddIntent.ChecklistItem -> setState {
                copy(checklist = checklist?.mapIndexed { i, old -> if (i == intent.index) intent.text else old })
            }
            is QuickAddIntent.RemoveChecklistItem -> setState {
                copy(checklist = checklist?.filterIndexed { i, _ -> i != intent.index }?.takeIf { it.isNotEmpty() })
            }
            QuickAddIntent.Save -> save()
            QuickAddIntent.Dismiss -> dismiss()
        }
    }

    /** Новые срок и начало; повтор начинается с первого подходящего дня от выбранной даты (решение PM). */
    private fun QuickAddState.schedule(next: Schedule): QuickAddState {
        val aligned = next.alignedTo(rrule, zone)
        return copy(
            due = aligned.due,
            start = aligned.start,
            rrule = if (aligned.due == null) null else rrule,
            // Свои напоминания живут, пока не меняется вид срока («весь день» ↔ «со временем»).
            customReminders = customReminders.takeIf { aligned.due != null && due != null && aligned.due::class == due::class },
        )
    }

    private fun QuickAddState.edit(edit: FieldEdit): QuickAddState = when (edit) {
        is FieldEdit.Due -> schedule(Schedule(edit.due, null))
        is FieldEdit.Project -> copy(projectId = edit.projectId)
        is FieldEdit.SetPriority -> copy(priority = edit.priority)
        is FieldEdit.Repeat -> Schedule(due, start).alignedTo(edit.rrule, zone).let { copy(rrule = edit.rrule, due = it.due, start = it.start) }
        is FieldEdit.SetReminders -> copy(customReminders = edit.reminders)
    }

    private fun dismiss() {
        if (finished) return
        finished = true
        drafts.save(currentState.title, currentState.content)
    }

    /** Закрыли системой (смахнули окно, «Назад» мимо шторки) — черновик всё равно остаётся. */
    override fun onCleared() {
        dismiss()
        super.onCleared()
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
                    // Интервал: начало — выбранное время, срок — конец (как у задач из TickTick).
                    due = state.due,
                    start = state.start,
                    checklist = state.checklist.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
                    rrule = state.rrule,
                    reminders = state.reminders,
                ),
            )
            finished = true
            drafts.clear()
            sendEffect(QuickAddEffect.Close)
        }
    }
}
