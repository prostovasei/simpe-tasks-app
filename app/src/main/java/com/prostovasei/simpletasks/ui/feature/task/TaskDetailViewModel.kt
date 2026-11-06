package com.prostovasei.simpletasks.ui.feature.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.atOccurrence
import com.prostovasei.simpletasks.domain.model.repeatsBefore
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.alignedToRepeat
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.repository.ChecklistRepository
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskOpenKey
import com.prostovasei.simpletasks.widget.WidgetDates
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Задача в шторке с сеткой недели (SPEC.md §4.5, итерация 19): правка всех полей с автосохранением, чек-лист, подзадачи. */
data class TaskDetailState(
    val task: Task? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val subtasks: List<Task> = emptyList(),
    val projects: List<ProjectOption> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
    /** Сейчас — минута от начала суток (линия и начальная прокрутка сетки). */
    val nowMinute: Int = 0,
    /** Занятое другими задачами время в видимых днях сетки. */
    val busy: List<BusySlot> = emptyList(),
    /** Пункт чек-листа, который переименовывается (диалог). */
    val renamingItem: ChecklistItem? = null,
    /** Срок из названия (итерация 41): подсветка фразы и чип под названием. */
    val smart: SmartTitle = SmartTitle(),
    /** Открыт будущий повтор (итерация 49): дата этого раза; null — сама задача (у серии — ближайший раз). */
    val occurrence: TaskDue? = null,
    /** Окно поверх шторки: «Удалить серию», «Незакрытые повторы» (итерация 49), «Изменить повторяющуюся задачу» (итерация 50). */
    val dialog: DetailDialog? = null,
    /**
     * Новая дата повторяющейся задачи, пока не решено, к чему её применить (итерация 50): выбор в окне даты не пишется сразу —
     * после «Готово» спрашивается «Текущий повтор» / «Все повторы с этого момента». null — выбора нет.
     */
    val pending: Schedule? = null,
) {
    /** Что показывают поля даты: у будущего повтора — его дата. */
    val shown: Task?
        get() = task?.let { t -> occurrence?.let { t.atOccurrence(it) } ?: t }

    val fields: SheetFields?
        get() = shown?.let {
            val due = if (pending != null) pending.due else it.due
            val start = if (pending != null) pending.start else it.start
            SheetFields(due, start, it.projectId, it.priority, it.rrule, it.reminders, projects, today, zone, nowMinute, busy)
        }
}

/** Окна шторки (итерация 49, макет iteration49.py). */
sealed interface DetailDialog {
    /** «Удалить» у повторяющейся: «Удалить серию» / «Не буду делать» / «Отмена». */
    data object DeleteSeries : DetailDialog

    /** «Не буду делать» у будущего раза: [dates] — незакрытые повторы перед ним; «Пропустить все» / «Выполнить все» / «Отмена». */
    data class Unclosed(val dates: List<TaskDue>) : DetailDialog

    /**
     * Новая дата у повторяющейся (итерация 50): «Текущий повтор» / «Все повторы с этого момента» / «Отмена». [from] и [to] — дата раза
     * до и после («вс, 08 ноя», «пн, 09 ноя 18:00»); [nearest] — у будущего раза дата ближайшего: «Текущий повтор» тогда недоступен.
     */
    data class ChangeRepeat(val from: String, val to: String, val nearest: String? = null) : DetailDialog
}

sealed interface TaskDetailIntent {
    data class Title(val text: String) : TaskDetailIntent

    /** «✕» на чипе срока из названия (итерация 41). */
    data object DismissSmartDate : TaskDetailIntent
    data class Content(val text: String) : TaskDetailIntent
    data class Edit(val edit: FieldEdit) : TaskDetailIntent
    data class Grid(val action: GridAction) : TaskDetailIntent
    data object ToggleCompleted : TaskDetailIntent
    /**
     * «Удалить» в меню «⋮»: сразу, без вопроса — под шторкой появится «Задача удалена · Отмена» (итерация 36). У повторяющейся —
     * сначала окно «Удалить серию повторов?» (итерация 49).
     */
    data object RequestDelete : TaskDetailIntent

    /** «Удалить серию» в окне: вся серия в корзину. */
    data object ConfirmDeleteSeries : TaskDetailIntent

    /** «Не буду делать» (итерация 49): в «⋮» и в окне удаления серии. */
    data object WontDo : TaskDetailIntent

    /** Выбор в окне «Незакрытые повторы»: что сделать с повторами перед будущим разом. */
    data class CloseEarlier(val earlier: EarlierRepeats) : TaskDetailIntent

    /** Выбор в окне «Изменить повторяющуюся задачу» (итерация 50): [all] = false — «Текущий повтор», true — «Все повторы с этого момента». */
    data class ChangeRepeat(val all: Boolean) : TaskDetailIntent

    /** «Отмена» в окне. */
    data object DismissDialog : TaskDetailIntent
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

    /** Будущий повтор нельзя выполнить раньше ближайшего (итерация 49): подсказка с датой ближайшего [nearest]. */
    data class FutureDone(val nearest: String) : TaskDetailEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    private val checklist: ChecklistRepository,
    projects: ProjectRepository,
    dayTicker: DayTicker,
    private val time: TimeProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val deletions: TaskDeletions,
    private val smartDates: SmartDateSetting,
) : MviViewModel<TaskDetailState, TaskDetailIntent, TaskDetailEffect>(
    TaskDetailState(
        today = time.today(),
        zone = time.zone(),
        nowMinute = time.now().atZone(time.zone()).let { it.hour * 60 + it.minute },
    ),
) {

    /** Ключ открытия: id задачи, у будущего повтора — ещё и дата раза ([TaskOpenKey], итерация 49). */
    private val openKey: Pair<String, TaskDue?> = TaskOpenKey.parse(checkNotNull(savedStateHandle["taskId"]) { "Не передан taskId" })
    private val taskId: String = openKey.first

    /** Последняя сохранённая версия: с ней сравнивается черновик. */
    private var saved: Task? = null
    private var textSaveJob: Job? = null

    /** Есть правка текста, которая ещё не записана. */
    private var textDirty = false
    private var lastWrite: Job? = null
    private var closed = false

    /** Первый видимый день сетки: по нему подгружается занятое время (с запасом на день по краям). */
    private val gridFirst = MutableStateFlow(time.today())

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
                if (saved == null) {
                    saved = fromDb
                    // Будущий повтор (итерация 49): если это и есть ближайший раз — открываем как обычно.
                    val future = openKey.second?.takeIf { fromDb.rrule != null && it != (fromDb.start ?: fromDb.due) }
                    setState { copy(smart = SmartTitle.opened(fromDb.title, today, nowMinute()), occurrence = future) }
                }
            }
        }
        viewModelScope.launch { tasks.observeSubtasks(taskId).collect { setState { copy(subtasks = it) } } }
        viewModelScope.launch { projects.observeProjectTree().collect { tree -> setState { copy(projects = tree.options()) } } }
        viewModelScope.launch { dayTicker.today().collect { setState { copy(today = it) } } }
        viewModelScope.launch {
            gridFirst.flatMapLatest { first ->
                val from = first.minusDays(1)
                val until = first.plusDays(WeekGridMath.VISIBLE_DAYS.toLong())
                val zone = currentState.zone
                tasks.observeTimedInRange(from.atStartOfDay(zone).toInstant(), until.plusDays(1).atStartOfDay(zone).toInstant())
                    .map { all -> busySlots(all.filter { it.id != taskId }, zone, from, until) }
            }.collect { setState { copy(busy = it) } }
        }
    }

    override fun onIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.Title -> {
                editText { copy(title = intent.text) }
                val task = currentState.task ?: return
                val (next, schedule) = currentState.smart.update(
                    intent.text, smartDates.enabled.value, currentState.today, nowMinute(), time.zone(), Schedule(task.due, task.start),
                )
                setState { copy(smart = next) }
                schedule?.let(::reschedule)
            }
            TaskDetailIntent.DismissSmartDate -> {
                val (next, schedule) = currentState.smart.dismiss()
                setState { copy(smart = next) }
                schedule?.let(::reschedule)
            }
            is TaskDetailIntent.Content -> editText { copy(content = intent.text) }
            is TaskDetailIntent.Edit -> editNow(seriesDates = intent.edit is FieldEdit.Due || intent.edit is FieldEdit.Repeat) {
                when (val edit = intent.edit) {
                    // Повтор начинается с первого подходящего дня от выбранной даты (решение PM).
                    is FieldEdit.Due -> withDue(edit.due).alignedToRepeat(time.zone())
                    is FieldEdit.Project -> copy(projectId = edit.projectId)
                    is FieldEdit.SetPriority -> copy(priority = edit.priority)
                    is FieldEdit.Repeat -> copy(rrule = edit.rrule).alignedToRepeat(time.zone())
                    is FieldEdit.SetReminders -> copy(reminders = edit.reminders)
                }
            }
            is TaskDetailIntent.Grid -> when (val action = intent.action) {
                is GridAction.FirstDay -> gridFirst.value = action.first
                GridAction.Done -> applyPending()
                // Повторяющаяся (итерация 50): выбор копится до «Готово», «←» его просто забывает.
                is GridAction.Restore -> if (isRepeating()) setState { copy(pending = null) } else editSchedule(action)
                else -> if (isRepeating()) {
                    setState { copy(pending = (pending ?: shownSchedule()).after(action, time.zone())) }
                } else {
                    editSchedule(action)
                }
            }
            TaskDetailIntent.ToggleCompleted -> toggleCompleted()
            TaskDetailIntent.RequestDelete -> {
                val task = currentState.task ?: return
                if (task.rrule != null && task.status == TaskStatus.ACTIVE) setState { copy(dialog = DetailDialog.DeleteSeries) } else delete()
            }
            TaskDetailIntent.ConfirmDeleteSeries -> {
                setState { copy(dialog = null) }
                delete()
            }
            TaskDetailIntent.WontDo -> requestWontDo()
            is TaskDetailIntent.CloseEarlier -> {
                setState { copy(dialog = null) }
                wontDo(intent.earlier)
            }
            is TaskDetailIntent.ChangeRepeat -> changeRepeat(intent.all)
            // «Отмена» в окне «Изменить повторяющуюся задачу» возвращает прежнюю дату.
            TaskDetailIntent.DismissDialog -> setState { copy(dialog = null, pending = if (dialog is DetailDialog.ChangeRepeat) null else pending) }
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
        if (currentState.occurrence != null) {
            // Будущий повтор нельзя выполнить раньше ближайшего (как в TickTick).
            val nearest = (task.start ?: task.due)?.let(::dayText).orEmpty()
            sendEffect(TaskDetailEffect.FutureDone(nearest))
            return
        }
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

    private fun isRepeating(): Boolean = currentState.task?.let { it.rrule != null && it.status == TaskStatus.ACTIVE } == true

    private fun shownSchedule(): Schedule = currentState.shown.let { Schedule(it?.due, it?.start) }

    /** Выбор на сетке у обычной задачи — сохраняется сразу, как раньше. */
    private fun editSchedule(action: GridAction) = editNow(seriesDates = true) {
        val next = Schedule(due, start).after(action, time.zone())
        withDue(next.due).copy(start = next.start).alignedToRepeat(time.zone())
    }

    /**
     * «Готово» у повторяющейся (итерация 50): дата не изменилась — ничего; дату убрали — у всей серии, без окна (как раньше);
     * иначе окно «Изменить повторяющуюся задачу».
     */
    private fun applyPending() {
        val pending = currentState.pending ?: return
        val task = currentState.task ?: return
        val before = shownSchedule()
        when {
            pending == before -> setState { copy(pending = null) }
            pending.due == null -> {
                setState { copy(pending = null) }
                applyToSeries(pending)
            }
            else -> {
                val nearest = currentState.occurrence?.let { (task.start ?: task.due)?.let(::dayText) }
                val from = (before.start ?: before.due)?.let(::dayText).orEmpty()
                val to = (pending.start ?: pending.due)?.let(::whenText).orEmpty()
                setState { copy(dialog = DetailDialog.ChangeRepeat(from, to, nearest)) }
            }
        }
    }

    /** Новая дата — всей серии, как до итерации 50: повтор начинается с первого подходящего дня. */
    private fun applyToSeries(schedule: Schedule) = editNow(seriesDates = true) {
        withDue(schedule.due).copy(start = schedule.start).alignedToRepeat(time.zone())
    }

    /**
     * Выбор в окне «Изменить повторяющуюся задачу». У ближайшего раза «Все повторы» сдвигают серию (шторка остаётся), «Текущий
     * повтор» делает раз обычной задачей; у будущего «Все повторы» делят серию. В обоих последних случаях шторка закрывается,
     * а экран под ней показывает «Перенесено на … · Отмена».
     */
    private fun changeRepeat(all: Boolean) {
        val pending = currentState.pending ?: return
        val occurrence = currentState.occurrence
        setState { copy(dialog = null, pending = null) }
        if (all && occurrence == null) return applyToSeries(pending)
        val due = pending.due ?: return
        if (!all && occurrence != null) return
        textSaveJob?.cancel()
        persist()
        val previous = lastWrite
        saved = null
        appScope.launch {
            previous?.join()
            val result = if (all) tasks.moveRepeatsFrom(taskId, occurrence!!, due, pending.start) else tasks.moveRepeat(taskId, due, pending.start)
            if (result != null) {
                val day = result.day?.let(::dayText).orEmpty()
                deletions.notify(UndoNotice(UndoMessage.Moved(day), UndoKind.Calendar) { tasks.undoMove(result) })
            }
            close()
        }
    }

    /** «пн, 09 ноя 18:00» — дата и время для окна выбора. */
    private fun whenText(due: TaskDue): String = when (due) {
        is TaskDue.AllDay -> dayText(due)
        is TaskDue.AtTime -> dayText(due) + " " + due.instant.atZone(time.zone()).toLocalTime().format(TIME_FORMAT)
    }

    private fun nowMinute(): Int = time.now().atZone(time.zone()).let { it.hour * 60 + it.minute }

    /** Срок из названия или возврат прежнего — как выбор на сетке: сохраняется сразу. */
    private fun reschedule(schedule: Schedule) = editNow { withDue(schedule.due).copy(start = schedule.start).alignedToRepeat(time.zone()) }

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

    /**
     * Дата, список, приоритет и повтор сохраняются сразу. [seriesDates] — правка даты или повтора: у будущего повтора она меняет
     * серию, и окно дальше показывает серию (итерация 49: правка одного будущего раза — не в этой итерации).
     */
    private fun editNow(seriesDates: Boolean = false, change: Task.() -> Task) {
        val task = currentState.task ?: return
        setState { copy(task = task.change(), occurrence = if (seriesDates) null else occurrence) }
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
        // Фраза срока из названия в базу не попадает (итерация 41); на экране она остаётся до закрытия шторки.
        val toSave = if (draft.title.isBlank()) draft.copy(title = base.title) else draft.copy(title = currentState.smart.clean(draft.title))
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

    /** «Не буду делать»: у будущего раза с незакрытыми повторами перед ним — сначала окно выбора, иначе сразу. */
    private fun requestWontDo() {
        val task = currentState.task ?: return
        val occurrence = currentState.occurrence
        val before = occurrence?.let { task.repeatsBefore(it, time.zone(), time.today()) }.orEmpty()
        if (before.isNotEmpty()) {
            setState { copy(dialog = DetailDialog.Unclosed(before.mapNotNull { it.start ?: it.due })) }
        } else {
            setState { copy(dialog = null) }
            wontDo(EarlierRepeats.SKIP)
        }
    }

    /**
     * Закрывает задачу (или раз повтора) без выполнения; черновик сначала дописывается. Шторка закрывается, подсказку
     * «Не буду делать · … · Отмена» показывает экран под ней.
     */
    private fun wontDo(earlier: EarlierRepeats) {
        val occurrence = currentState.occurrence
        textSaveJob?.cancel()
        persist()
        val previous = lastWrite
        saved = null
        appScope.launch {
            previous?.join()
            val result = tasks.wontDo(taskId, occurrence, earlier) ?: return@launch close()
            val day = result.occurrence?.takeIf { result.previous.rrule != null }?.let(::dayText)
            deletions.notify(UndoNotice(UndoMessage.WontDo(day, result.skipped, result.completed), UndoKind.WontDo) { tasks.undoWontDo(result) })
            close()
        }
    }

    /** «вс, 08 ноя» — как в подписях дней. */
    fun dayText(due: TaskDue): String = WidgetDates.withWeekday(
        when (due) {
            is TaskDue.AllDay -> due.date
            is TaskDue.AtTime -> due.instant.atZone(time.zone()).toLocalDate()
        },
    )

    /** Удаляет без вопроса; шторка закрывается, когда запись в базе готова и событие для «Отмены» отправлено. */
    private fun delete() {
        textSaveJob?.cancel()
        saved = null
        appScope.launch {
            deletions.emit(tasks.deleteTask(taskId))
            close()
        }
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
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

private fun Task.editableEquals(other: Task) =
    title == other.title && content == other.content && projectId == other.projectId && priority == other.priority &&
        due == other.due && start == other.start && rrule == other.rrule && reminders == other.reminders
