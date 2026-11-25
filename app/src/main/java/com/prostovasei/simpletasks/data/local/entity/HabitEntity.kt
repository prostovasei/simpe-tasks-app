package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Привычка (#340; миграция 13 → 14). [schedule] — `DAILY` / `WEEKDAYS` / `PER_WEEK`; [weekdays] — маска дней (пн = 1 … вс = 64)
 * для `WEEKDAYS`; [perWeek] — сколько раз в неделю для `PER_WEEK`. Архивная привычка не показывается на экране, история хранится.
 */
@Entity(tableName = "habits", indices = [Index(name = "idx_habits_sort", value = ["sort_order"])])
data class HabitEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String?,
    @ColumnInfo(name = "schedule") val schedule: String,
    @ColumnInfo(name = "weekdays") val weekdays: Int,
    @ColumnInfo(name = "per_week") val perWeek: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "archived") val archived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Отметка «сделано» привычки за день (#340). [date] — дни от 1970 года; удаление привычки удаляет и отметки. */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habit_id", "date"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habit_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(name = "idx_habit_checks_date", value = ["date"])],
)
data class HabitCheckEntity(
    @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
