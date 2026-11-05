package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица tasks — задачи (SPEC.md §2.3) + поля для импорта из TickTick без потерь (docs/ARCHITECTURE.md §4.1).
 *
 * Формат due_date и start_date:
 * - is_all_day = 1 → полночь календарной даты по UTC (дата не «уезжает» при смене часового пояса);
 * - is_all_day = 0 → реальный момент времени в мс UTC.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
        ),
    ],
    indices = [
        Index(value = ["due_date", "status", "is_deleted"], name = "idx_tasks_due_status"),
        Index(value = ["project_id", "status", "is_deleted"], name = "idx_tasks_project"),
        // Защита от дублей при повторном импорте из TickTick (аудит R6).
        Index(value = ["external_id"], name = "idx_tasks_external_id", unique = true),
        Index(value = ["parent_id"], name = "idx_tasks_parent"),
    ],
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content", defaultValue = "''") val content: String = "",
    /** 0 — активна, 2 — выполнена, -1 — отменена. */
    @ColumnInfo(name = "status", defaultValue = "0") val status: Int = 0,
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int = 0,
    @ColumnInfo(name = "due_date", defaultValue = "NULL") val dueDate: Long? = null,
    /** Начало интервала (10:00 в «10:00–11:00»); NULL — без интервала. */
    @ColumnInfo(name = "start_date", defaultValue = "NULL") val startDate: Long? = null,
    @ColumnInfo(name = "is_all_day", defaultValue = "1") val isAllDay: Boolean = true,
    @ColumnInfo(name = "completed_at", defaultValue = "NULL") val completedAt: Long? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "rrule", defaultValue = "NULL") val rrule: String? = null,
    /** Напоминания в формате TickTick/iCalendar (PT0S, -P1D…), по одному на строку. */
    @ColumnInfo(name = "reminders", defaultValue = "NULL") val reminders: String? = null,
    /** Родительская задача для подзадач. */
    @ColumnInfo(name = "parent_id", defaultValue = "NULL") val parentId: String? = null,
    /** id задачи в TickTick; NULL для задач, созданных в приложении. */
    @ColumnInfo(name = "external_id", defaultValue = "NULL") val externalId: String? = null,
    /** Исходная строка CSV TickTick (JSON «колонка → значение») для выгрузки без потерь. */
    @ColumnInfo(name = "source_data", defaultValue = "NULL") val sourceData: String? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Задача со списком и папкой — для выгрузки в CSV. */
data class ExportTaskRow(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "list_name") val listName: String,
    @ColumnInfo(name = "list_is_inbox") val listIsInbox: Boolean,
    @ColumnInfo(name = "folder_name") val folderName: String?,
    /** Эмодзи списка (итерация 47): при экспорте возвращается в начало названия, как его хранит TickTick. */
    @ColumnInfo(name = "list_emoji") val listEmoji: String? = null,
)

/** Задача с текстом её чек-листа (пункты через перевод строки) — для поиска (итерация 15). */
data class SearchTaskRow(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "checklist_text") val checklistText: String?,
)

/** Сводка по базе для экрана «Настройки». */
data class DataStatsRow(
    @ColumnInfo(name = "total_tasks") val totalTasks: Int,
    @ColumnInfo(name = "active_tasks") val activeTasks: Int,
    @ColumnInfo(name = "lists") val lists: Int,
)
