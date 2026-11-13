package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Место из справочника мест (#239; миграция 9 → 10). Операции хранят место текстом ([FinanceOperationEntity.place]) — справочник
 * связан с ними по названию: переименование и удаление места меняют текст в операциях. [isArchived] — не предлагается при выборе.
 */
@Entity(tableName = "finance_places", indices = [Index(name = "idx_finance_places_name", value = ["name"], unique = true)])
data class FinancePlaceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Место с числом операций (вместе с корзиной) — для предупреждения об удалении. */
data class PlaceRow(
    @Embedded val place: FinancePlaceEntity,
    @ColumnInfo(name = "uses") val uses: Int,
)

/** Категория с числом операций (вместе с корзиной) — для предупреждения об удалении (#239). */
data class CategoryRow(
    @Embedded val category: FinanceCategoryEntity,
    @ColumnInfo(name = "uses") val uses: Int,
)
