package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.Task
import java.time.LocalDate
import java.time.ZoneId

/** Задача в виде, готовом для отрисовки строкой списка. */
data class TaskItem(
    val id: String,
    val title: String,
    val priority: Priority,
    val due: DueText?,
    val listName: String?,
    val listColor: String?,
    val isInboxList: Boolean,
    /** Отмечена только что: показывается зачёркнутой и полупрозрачной 2,5 с (SPEC.md §4.1). */
    val isCompleting: Boolean = false,
    val hasChecklist: Boolean = false,
    /** Повторяющаяся: при отметке переезжает на следующий раз. */
    val isRecurring: Boolean = false,
)

fun Task.toItem(projects: Map<String, Project>, today: LocalDate, zone: ZoneId, pending: Set<String>): TaskItem {
    val project = projects[projectId]
    return TaskItem(
        id = id,
        title = title,
        priority = priority,
        due = DueFormatter.format(due, start, today, zone),
        listName = project?.name,
        listColor = project?.color,
        isInboxList = project?.isInbox == true,
        isCompleting = id in pending,
        isRecurring = rrule != null && due != null,
    )
}

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
