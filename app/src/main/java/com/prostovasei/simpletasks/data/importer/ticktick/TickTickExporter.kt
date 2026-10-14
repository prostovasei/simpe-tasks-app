package com.prostovasei.simpletasks.data.importer.ticktick

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.ExportTaskRow
import com.prostovasei.simpletasks.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Выгружает все неудалённые задачи в CSV формата TickTick (docs/ARCHITECTURE.md §4):
 * поля, которые есть в приложении, берутся из БД, остальные — из исходной строки импорта (source_data).
 * Файл можно загрузить в TickTick или обратно в приложение (повторный импорт пропустит существующие задачи).
 */
internal class TickTickExporter @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
) {
    /** Возвращает количество выгруженных задач. */
    suspend fun export(output: OutputStream): Int {
        val (tasks, checklist) = db.withTransaction {
            db.taskDao().getAllForExport() to db.checklistDao().getAllActive().groupBy { it.taskId }
        }
        val tickTickIds = tasks.associate { it.task.id to (it.task.externalId ?: newTickTickId(it.task.id)) }
        val deviceZone = time.zone()

        val writer = output.bufferedWriter(Charsets.UTF_8)
        writer.write(BOM.toString())
        val csv = CsvWriter(writer)
        csv.writeRecord(listOf("Date: ${time.now().atOffset(ZoneOffset.UTC).toLocalDate()}+0000"))
        csv.writeRecord(listOf("Version: ${TickTickFormat.VERSION}"))
        csv.writeRecord(listOf(TickTickFormat.STATUS_LEGEND))
        csv.writeRecord(TickTickFormat.COLUMNS)
        tasks.forEach { row ->
            val values = rowValues(row, checklist[row.task.id].orEmpty(), tickTickIds, deviceZone)
            csv.writeRecord(TickTickFormat.COLUMNS.map { values[it].orEmpty() })
        }
        writer.flush()
        return tasks.size
    }

    private fun rowValues(
        row: ExportTaskRow,
        items: List<ChecklistItemEntity>,
        tickTickIds: Map<String, String>,
        deviceZone: ZoneId,
    ): Map<String, String> {
        val task = row.task
        val source: Map<String, String> = task.sourceData?.let { Json.decodeFromString<Map<String, String>>(it) }.orEmpty()
        val zone = source["Timezone"]?.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: deviceZone
        val kind = when {
            items.isNotEmpty() -> "CHECKLIST"
            source["Kind"] == "NOTE" -> "NOTE"
            else -> "TEXT"
        }
        val content = buildList {
            if (task.content.isNotEmpty()) addAll(task.content.split('\n'))
            items.forEach { add("${if (it.isDone) TickTickFormat.CHECKED else TickTickFormat.UNCHECKED}${it.title}") }
        }.joinToString(TickTickFormat.CONTENT_LINE_BREAK)

        fun date(millis: Long?): String = millis?.let {
            val instant = if (task.isAllDay) {
                // «Весь день» в TickTick — полночь даты в часовом поясе задачи.
                LocalDate.ofEpochDay(Math.floorDiv(it, DueDateCodec.MILLIS_PER_DAY)).atStartOfDay(zone).toInstant()
            } else {
                Instant.ofEpochMilli(it)
            }
            format(instant)
        }.orEmpty()

        val overrides = mapOf(
            "Folder Name" to row.folderName.orEmpty(),
            "List Name" to if (row.listIsInbox) TickTickFormat.INBOX else row.listName,
            "Title" to task.title,
            "Kind" to kind,
            "Content" to content,
            "Is Check list" to if (kind == "CHECKLIST") "Y" else "N",
            "Start Date" to date(task.startDate),
            "Due Date" to date(task.dueDate),
            "Reminder" to task.reminders.orEmpty(),
            "Repeat" to task.rrule.orEmpty(),
            "Priority" to task.priority.toString(),
            "Status" to task.status.toString(),
            "Created Time" to format(Instant.ofEpochMilli(task.createdAt)),
            "Completed Time" to if (task.status == TaskStatus.ACTIVE.value) "" else task.completedAt?.let { format(Instant.ofEpochMilli(it)) }.orEmpty(),
            "Order" to (task.sortOrder.toLong() * ORDER_STEP).toString(),
            "Timezone" to zone.id,
            "Is All Day" to if (task.dueDate == null) source["Is All Day"] ?: "false" else task.isAllDay.toString(),
            "taskId" to tickTickIds.getValue(task.id),
            "parentId" to task.parentId?.let { tickTickIds[it] }.orEmpty(),
        )
        return DEFAULTS + source + overrides
    }

    private fun format(instant: Instant): String =
        TickTickFormat.DATE_FORMAT.format(instant.truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC))

    /** id TickTick — 24 шестнадцатеричных символа; для задач, созданных в приложении, берём их из UUID. */
    private fun newTickTickId(uuid: String): String = uuid.replace("-", "").take(TICKTICK_ID_LENGTH)

    private companion object {
        val BOM = 0xFEFF.toChar()
        const val TICKTICK_ID_LENGTH = 24

        /** Шаг порядка: TickTick сортирует по возрастанию Order, значения произвольные. */
        const val ORDER_STEP = 1L shl 30

        val DEFAULTS = mapOf(
            "Tags" to "",
            "Is Floating" to "false",
            "Column Name" to "",
            "Column Order" to "",
            "View Mode" to "list",
            "projectKind" to "TASK",
        )
    }
}

/** Запись CSV: все поля в кавычках, кавычки внутри удваиваются, записи разделяются \n — как у TickTick. */
internal class CsvWriter(private val writer: Appendable) {
    fun writeRecord(fields: List<String>) {
        fields.forEachIndexed { index, field ->
            if (index > 0) writer.append(',')
            writer.append('"').append(field.replace("\"", "\"\"")).append('"')
        }
        writer.append('\n')
    }
}
