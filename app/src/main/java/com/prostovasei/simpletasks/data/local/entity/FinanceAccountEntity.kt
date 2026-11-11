package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Счёт финансов (этап 1, #221; таблица добавлена миграцией 5 → 6). Суммы — в копейках. */
@Entity(tableName = "finance_accounts")
data class FinanceAccountEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    /** Код [com.prostovasei.simpletasks.domain.model.finance.AccountType]: cash / card / savings. */
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "emoji") val emoji: String? = null,
    @ColumnInfo(name = "initial_balance", defaultValue = "0") val initialBalance: Long = 0,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
