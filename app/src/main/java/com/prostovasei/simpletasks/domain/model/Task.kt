package com.prostovasei.simpletasks.domain.model

import java.time.Instant

data class Task(
    val id: String,
    val projectId: String,
    val title: String,
    val content: String,
    val status: TaskStatus,
    val priority: Priority,
    val due: TaskDue?,
    /** Начало интервала «10:00–11:00»; null — у задачи только срок. */
    val start: TaskDue?,
    val completedAt: Instant?,
    val sortOrder: Int,
    val rrule: String?,
    /** Напоминания в формате iCalendar относительно срока: «PT0S» — в момент срока, «-P1D» — за день. */
    val reminders: List<String>,
    val parentId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED
}

/** Задача для поиска: с текстом пунктов чек-листа (итерация 15). */
data class SearchableTask(val task: Task, val checklist: List<String>)

/** Данные для создания задачи. [checklist] — пункты чек-листа, добавляемые вместе с задачей. */
data class NewTask(
    val projectId: String,
    val title: String,
    val content: String = "",
    val priority: Priority = Priority.NONE,
    val due: TaskDue? = null,
    /** Начало интервала (итерация 17): тогда [due] — его конец; того же вида, что [due]. */
    val start: TaskDue? = null,
    val checklist: List<String> = emptyList(),
    /** Правило повтора RRULE (см. [RepeatRule]). */
    val rrule: String? = null,
    /** Напоминания (см. [Reminders]); без срока не сохраняются. */
    val reminders: List<String> = emptyList(),
)

data class ChecklistItem(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Задача вместе с чек-листом — для экрана Task Detail. */
data class TaskDetails(
    val task: Task,
    val checklist: List<ChecklistItem>,
)

/** Задача в корзине (итерация 39): [details] — она сама с пунктами чек-листа, [subtasks] — подзадачи, удалённые вместе с ней; удалена в [deletedAt]. */
data class TrashedTask(val details: TaskDetails, val subtasks: List<Task>, val deletedAt: Instant)

/**
 * Неделя (итерация 44): [inRange] — активные и выполненные задачи, у которых начало (или срок) попадает в семь дней недели;
 * [repeating] — все активные повторяющиеся: будущие разы считает приложение.
 */
data class WeekTasks(val inRange: List<Task>, val repeating: List<Task>)

/** Секции экрана «Все» (SPEC.md §4.1). Только активные задачи. */
data class AllScreenTasks(
    val overdue: List<Task>,
    val todayAllDay: List<Task>,
    val todayTimed: List<Task>,
    val upcoming: List<Task>,
)
