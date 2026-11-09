package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Таблица project_groups — папки списков (SPEC.md §2.1). */
@Entity(tableName = "project_groups")
data class ProjectGroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    /** Эмодзи папки (итерация 56), null — 📁. Колонка добавлена миграцией 4 → 5. */
    @ColumnInfo(name = "emoji") val emoji: String? = null,
)
