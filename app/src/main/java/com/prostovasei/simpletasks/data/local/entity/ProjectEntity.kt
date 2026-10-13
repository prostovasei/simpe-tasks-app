package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Таблица projects — списки задач (SPEC.md §2.2). */
@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ProjectGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
        ),
    ],
    indices = [
        // Нет в SPEC.md §2.5: без него Room сканирует таблицу при поиске списков папки (аудит R3).
        Index(value = ["group_id", "is_deleted"], name = "idx_projects_group"),
    ],
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color", defaultValue = "NULL") val color: String? = null,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_inbox", defaultValue = "0") val isInbox: Boolean = false,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Список со счётчиком открытых задач — для экрана «Списки». */
data class ProjectWithOpenCount(
    @Embedded val project: ProjectEntity,
    @ColumnInfo(name = "open_count") val openCount: Int,
)
