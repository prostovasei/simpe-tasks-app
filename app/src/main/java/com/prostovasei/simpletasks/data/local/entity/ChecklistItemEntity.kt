package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Таблица checklist_items — пункты чек-листа (SPEC.md §2.4). */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
        ),
    ],
    indices = [
        Index(value = ["task_id", "is_deleted"], name = "idx_checklist_task"),
    ],
)
data class ChecklistItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "is_done", defaultValue = "0") val isDone: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
