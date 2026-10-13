package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица tasks — задачи (SPEC.md §2.3).
 *
 * Формат due_date:
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
    ],
    indices = [
        Index(value = ["due_date", "status", "is_deleted"], name = "idx_tasks_due_status"),
        Index(value = ["project_id", "status", "is_deleted"], name = "idx_tasks_project"),
        // Защита от дублей при повторном импорте из TickTick (аудит R6).
        Index(value = ["external_id"], name = "idx_tasks_external_id", unique = true),
    ],
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content", defaultValue = "''") val content: String = "",
    @ColumnInfo(name = "status", defaultValue = "0") val status: Int = 0,
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int = 0,
    @ColumnInfo(name = "due_date", defaultValue = "NULL") val dueDate: Long? = null,
    @ColumnInfo(name = "is_all_day", defaultValue = "1") val isAllDay: Boolean = true,
    @ColumnInfo(name = "completed_at", defaultValue = "NULL") val completedAt: Long? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "rrule", defaultValue = "NULL") val rrule: String? = null,
    /** id задачи в TickTick; NULL для задач, созданных в приложении. */
    @ColumnInfo(name = "external_id", defaultValue = "NULL") val externalId: String? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
