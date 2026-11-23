package com.prostovasei.simpletasks.data.mapper

import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectGroupEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectWithOpenCount
import com.prostovasei.simpletasks.data.local.entity.TaskEntity
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskStatus
import java.time.Instant

internal fun TaskEntity.toDomain() = Task(
    id = id,
    projectId = projectId,
    title = title,
    content = content,
    status = TaskStatus.fromValue(status),
    priority = Priority.fromValue(priority),
    due = DueDateCodec.decode(dueDate, isAllDay),
    start = DueDateCodec.decode(startDate, isAllDay),
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    sortOrder = sortOrder,
    rrule = rrule,
    reminders = reminders?.split('\n')?.filter { it.isNotBlank() }.orEmpty(),
    parentId = parentId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    isRepeatDone = isRepeatDone,
    skipped = SkippedDates.decode(skippedDates),
)

/** #192: отменённые даты серии в колонке tasks.skipped_dates — ISO-даты через запятую. */
internal object SkippedDates {
    fun encode(dates: Set<java.time.LocalDate>): String? = dates.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

    fun decode(value: String?): Set<java.time.LocalDate> =
        value?.split(',')?.mapNotNull { runCatching { java.time.LocalDate.parse(it.trim()) }.getOrNull() }?.toSet().orEmpty()
}

internal fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id = id,
    taskId = taskId,
    title = title,
    isDone = isDone,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun ProjectEntity.toDomain() = Project(
    id = id,
    groupId = groupId,
    name = name,
    color = color,
    sortOrder = sortOrder,
    isInbox = isInbox,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    icon = icon,
    emoji = emoji,
)

internal fun ProjectWithOpenCount.toDomain() = ProjectWithCount(project.toDomain(), openCount)

internal fun ProjectGroupEntity.toDomain() = ProjectGroup(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    emoji = emoji,
)
