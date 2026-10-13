package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Задача из строки TickTick, приведённая к нашей модели. */
internal data class ImportedTask(
    val externalId: String,
    val parentExternalId: String?,
    val folderName: String?,
    val listName: String,
    val title: String,
    val content: String,
    val checklist: List<ImportedChecklistItem>,
    val status: TaskStatus,
    val priority: Priority,
    val dueDate: Long?,
    val startDate: Long?,
    val isAllDay: Boolean,
    val completedAt: Long?,
    val createdAt: Long?,
    val rrule: String?,
    val reminders: String?,
    val order: Long?,
    val sourceData: String,
)

internal data class ImportedChecklistItem(val title: String, val isDone: Boolean)

/** Правила переноса строки TickTick (docs/ARCHITECTURE.md §4.1). */
internal object TickTickRowMapper {
    private const val CHECKED = '▪'
    private const val UNCHECKED = '▫'
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxx")
    private val LINE_BREAK = Regex("\r\n|\r|\n")

    /** @param fallbackZone пояс устройства — если у задачи пояс не указан или неизвестен. */
    fun map(row: TickTickRow, fallbackZone: ZoneId): ImportedTask {
        val externalId = row["taskId"].trim()
        require(externalId.isNotEmpty()) { "Пустой taskId" }

        val zone = row["Timezone"].takeIf { it.isNotBlank() }
            ?.let { runCatching { ZoneId.of(it.trim()) }.getOrNull() }
            ?: fallbackZone
        val due = parseInstant(row["Due Date"])
        val start = parseInstant(row["Start Date"])
        val isAllDay = when (row["Is All Day"].trim().lowercase()) {
            "true" -> true
            "false" -> false
            // Колонка пустая: «весь день», если срока нет или он ровно в полночь по поясу задачи.
            else -> due == null || due.atZone(zone).toLocalTime() == LocalTime.MIDNIGHT
        }
        val status = TaskStatus.fromValue(row["Status"].trim().toIntOrNull() ?: 0)

        val (content, checklist) = splitContent(row["Content"], isChecklist = row["Kind"].trim() == "CHECKLIST")
        val title = row["Title"].trim().ifEmpty { content.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "—" }

        return ImportedTask(
            externalId = externalId,
            parentExternalId = row["parentId"].trim().ifEmpty { null },
            folderName = row["Folder Name"].trim().ifEmpty { null },
            listName = row["List Name"].trim(),
            title = title,
            content = content,
            checklist = checklist,
            status = status,
            priority = Priority.fromValue(row["Priority"].trim().toIntOrNull() ?: 0),
            dueDate = due?.let { encode(it, isAllDay, zone) },
            startDate = start?.let { encode(it, isAllDay, zone) },
            isAllDay = isAllDay,
            completedAt = if (status == TaskStatus.ACTIVE) null else parseInstant(row["Completed Time"])?.toEpochMilli(),
            createdAt = parseInstant(row["Created Time"])?.toEpochMilli(),
            rrule = row["Repeat"].trim().ifEmpty { null },
            reminders = row["Reminder"].split(LINE_BREAK).map { it.trim() }.filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }?.joinToString("\n"),
            order = row["Order"].trim().toLongOrNull(),
            sourceData = Json.encodeToString(row.values),
        )
    }

    /** Задача «на весь день» хранится как календарная дата в поясе задачи, со временем — как момент. */
    private fun encode(instant: Instant, isAllDay: Boolean, zone: ZoneId): Long =
        if (isAllDay) {
            DueDateCodec.encode(TaskDue.AllDay(instant.atZone(zone).toLocalDate())).dueDate!!
        } else {
            instant.toEpochMilli()
        }

    private fun parseInstant(value: String): Instant? {
        val text = value.trim()
        if (text.isEmpty()) return null
        return try {
            OffsetDateTime.parse(text, DATE_FORMAT).toInstant()
        } catch (_: DateTimeParseException) {
            OffsetDateTime.parse(text).toInstant()
        }
    }

    /** У чек-листов TickTick пункты — строки с ▪ (выполнен) и ▫ (не выполнен); остальные строки — описание. */
    private fun splitContent(raw: String, isChecklist: Boolean): Pair<String, List<ImportedChecklistItem>> {
        val lines = raw.split(LINE_BREAK)
        if (!isChecklist) return lines.joinToString("\n").trimEnd() to emptyList()

        val description = mutableListOf<String>()
        val items = mutableListOf<ImportedChecklistItem>()
        lines.forEach { line ->
            when {
                line.startsWith(CHECKED) -> line.drop(1).trim().takeIf { it.isNotEmpty() }
                    ?.let { items += ImportedChecklistItem(it, isDone = true) }
                line.startsWith(UNCHECKED) -> line.drop(1).trim().takeIf { it.isNotEmpty() }
                    ?.let { items += ImportedChecklistItem(it, isDone = false) }
                else -> description += line
            }
        }
        return description.joinToString("\n").trim() to items
    }
}
