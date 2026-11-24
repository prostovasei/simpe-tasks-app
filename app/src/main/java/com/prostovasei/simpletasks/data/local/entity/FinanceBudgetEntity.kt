package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Бюджет категории на месяц (#327; миграция 12 → 13). [month] — «YYYY-MM», [amount] — копейки.
 * Строка с [amount] = 0 — бюджет у категории снят, но месяц уже заведён: прошлые бюджеты на него не переносятся.
 * Удаление категории навсегда удаляет и её бюджеты.
 */
@Entity(
    tableName = "finance_budgets",
    primaryKeys = ["month", "category_id"],
    foreignKeys = [ForeignKey(entity = FinanceCategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(name = "idx_finance_budgets_category", value = ["category_id"])],
)
data class FinanceBudgetEntity(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
