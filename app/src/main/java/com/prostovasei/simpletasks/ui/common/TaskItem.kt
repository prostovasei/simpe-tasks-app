package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.upcomingRepeats
import java.time.LocalDate
import java.time.ZoneId

/** Задача в виде, готовом для отрисовки строкой списка. */
data class TaskItem(
    val id: String,
    val title: String,
    val priority: Priority,
    val due: DueText?,
    val listName: String?,
    /** Эмодзи списка (итерация 47) — вместо цвета и значка. */
    val listEmoji: String,
    val isInboxList: Boolean,
    /** Отмечена только что, а БД ещё не ответила: показывается выполненной сразу (итерация 27). */
    val isCompleting: Boolean = false,
    /** Выполнена (сегодня): остаётся в списке до полуночи — с галочкой, зачёркнутая и бледнее (итерация 27). */
    val isDone: Boolean = false,
    val hasChecklist: Boolean = false,
    /** Повторяющаяся: при отметке переезжает на следующий раз. */
    val isRecurring: Boolean = false,
    /** Будущий повтор задачи [ghostOf] — только для показа: бледный, без галочки. */
    val ghostOf: String? = null,
    /** Колокольчик (итерация 27): в момент срока или со стрелкой назад — заранее (0.27.1); null — напоминаний нет. */
    val reminder: ReminderMark? = null,
    /** Мелкая подпись под названием (итерация 39): в корзине — «удалена 28 окт». */
    val subtitle: String? = null,
    /** У будущего повтора — его начало или срок (итерация 49): окно задачи открывает именно этот раз. */
    val occurrence: TaskDue? = null,
) {
    val isGhost: Boolean get() = ghostOf != null

    /** Показывается выполненной: уже в БД или только что отмечена. */
    val showsDone: Boolean get() = isDone || isCompleting

    /** Что открывает нажатие: задачу, а у будущего повтора — серию вместе с датой раза ([TaskOpenKey], итерация 49). */
    val taskId: String get() = ghostOf?.let { TaskOpenKey.of(it, occurrence) } ?: id
}

fun Task.toItem(projects: Map<String, Project>, today: LocalDate, zone: ZoneId, pending: Set<String>, ghostIndex: Int? = null): TaskItem {
    val project = projects[projectId]
    return TaskItem(
        id = if (ghostIndex == null) id else "$id@$ghostIndex",
        title = title,
        priority = priority,
        due = DueFormatter.format(due, start, today, zone),
        listName = project?.name,
        listEmoji = project?.let { ListEmoji.ofTask(it.isInbox, due != null, it.emojiShown) } ?: ListEmoji.DEFAULT,
        isInboxList = project?.isInbox == true,
        isCompleting = id in pending,
        isDone = isCompleted,
        // Выполненный раз повтора — тоже со знаком повтора (проверка PM 0.47.0): у копии правила нет, есть отметка.
        isRecurring = (rrule != null && due != null) || isRepeatDone,
        ghostOf = id.takeIf { ghostIndex != null },
        reminder = Reminders.mark(due, reminders),
        occurrence = (start ?: due).takeIf { ghostIndex != null },
    )
}

/** Сколько дней вперёд показываются будущие повторы (решение PM). */
const val REPEATS_AHEAD_DAYS = 30L

/**
 * «Предстоящие» вместе с будущими повторами (на [REPEATS_AHEAD_DAYS] дней) всех повторяющихся задач [all],
 * по порядку: день, сначала «весь день», потом по времени.
 */
fun upcomingWithRepeats(
    upcoming: List<Task>,
    all: List<Task>,
    projects: Map<String, Project>,
    today: LocalDate,
    zone: ZoneId,
    pending: Set<String>,
): List<TaskItem> {
    val until = today.plusDays(REPEATS_AHEAD_DAYS)
    val real = upcoming.map { it to it.toItem(projects, today, zone, pending) }
    val ghosts = all.filter { it.rrule != null }.flatMap { task ->
        task.upcomingRepeats(zone, today, until).mapIndexed { index, repeat -> repeat to repeat.toItem(projects, today, zone, pending, ghostIndex = index) }
    }
    return (real + ghosts).sortedWith(compareBy({ it.first.sortDay(zone) }, { it.first.sortTime() }, { it.second.isGhost })).map { it.second }
}

/** Порядок в «Предстоящих»: день → «весь день» раньше задач со временем → время начала. */
internal fun Task.sortDay(zone: ZoneId): LocalDate = when (val anchor = start ?: due) {
    is TaskDue.AllDay -> anchor.date
    is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
    null -> LocalDate.MAX
}

internal fun Task.sortTime(): Long = ((start ?: due) as? TaskDue.AtTime)?.instant?.toEpochMilli() ?: Long.MIN_VALUE

/**
 * Оставляет в списке задачи, отмеченные только что ([pending]): из БД они уже пропали (статус «выполнена»),
 * но на экране должны исчезнуть только через 2,5 с. Такие задачи остаются на прежнем месте.
 */
fun <T> keepPending(previous: List<T>, current: List<T>, pending: Set<String>, id: (T) -> String): List<T> {
    if (pending.isEmpty()) return current
    val result = current.toMutableList()
    val currentIds = current.mapTo(mutableSetOf(), id)
    previous.forEachIndexed { index, item ->
        val itemId = id(item)
        if (itemId in pending && itemId !in currentIds) {
            val anchor = previous.subList(0, index).lastOrNull { prev -> result.any { id(it) == id(prev) } }
            val position = anchor?.let { a -> result.indexOfFirst { id(it) == id(a) } + 1 } ?: 0
            result.add(position, item)
        }
    }
    return result
}
