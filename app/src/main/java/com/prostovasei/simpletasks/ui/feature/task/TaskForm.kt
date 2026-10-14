package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Список для выбора в форме задачи. */
data class ProjectOption(val id: String, val name: String, val color: String?, val isInbox: Boolean)

fun ProjectTree.options(): List<ProjectOption> =
    (listOfNotNull(inbox) + groups.flatMap { it.projects } + ungrouped).map {
        ProjectOption(it.project.id, it.project.name, it.project.color, it.project.isInbox)
    }

/** Изменения полей задачи, общие для быстрого создания и экрана задачи. */
sealed interface FieldEdit {
    data class Due(val due: TaskDue?) : FieldEdit
    data class Project(val projectId: String) : FieldEdit
    data class SetPriority(val priority: Priority) : FieldEdit
    data class Repeat(val rrule: String?) : FieldEdit
    data class SetReminders(val reminders: List<String>) : FieldEdit
}

/** Срок → календарная дата в поясе пользователя. */
fun TaskDue.localDate(zone: ZoneId): LocalDate = when (this) {
    is TaskDue.AllDay -> date
    is TaskDue.AtTime -> instant.atZone(zone).toLocalDate()
}

fun TaskDue.localTime(zone: ZoneId): LocalTime? = (this as? TaskDue.AtTime)?.instant?.atZone(zone)?.toLocalTime()

/** Новая дата: время задачи (если было) сохраняется. */
fun withDate(current: TaskDue?, date: LocalDate, zone: ZoneId): TaskDue {
    val time = current?.localTime(zone) ?: return TaskDue.AllDay(date)
    return TaskDue.AtTime(date.atTime(time).atZone(zone).toInstant())
}

/** Новое время (null — «весь день»): дата задачи сохраняется, без даты — берётся [today]. */
fun withTime(current: TaskDue?, time: LocalTime?, zone: ZoneId, today: LocalDate): TaskDue {
    val date = current?.localDate(zone) ?: today
    return if (time == null) TaskDue.AllDay(date) else TaskDue.AtTime(date.atTime(time).atZone(zone).toInstant())
}
