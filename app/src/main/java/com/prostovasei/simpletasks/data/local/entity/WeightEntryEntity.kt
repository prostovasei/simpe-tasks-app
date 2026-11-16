package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Запись веса (#257; миграция 10 → 11). [date] — день числом дней от 1970 года, уникален: одна запись на дату;
 * [tenths] — вес в десятых долях килограмма.
 */
@Entity(tableName = "weight_entries", indices = [Index(name = "idx_weight_entries_date", value = ["date"], unique = true)])
data class WeightEntryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "tenths") val tenths: Int,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
