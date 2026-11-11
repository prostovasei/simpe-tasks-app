package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Категория финансов (этап 2, #225; миграция 6 → 7). [kind] — expense или income. */
@Entity(tableName = "finance_categories")
data class FinanceCategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * Операция финансов (этап 2, #225; миграция 6 → 7). [amount] — положительные копейки; [kind] — expense / income / transfer;
 * [date] — день операции числом дней от 01.01.1970 (без часового пояса). У перевода заполнен [toAccountId].
 * [isDeleted] — в корзине: не влияет на остатки.
 */
@Entity(
    tableName = "finance_operations",
    foreignKeys = [
        ForeignKey(entity = FinanceAccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"]),
        ForeignKey(entity = FinanceAccountEntity::class, parentColumns = ["id"], childColumns = ["to_account_id"]),
        ForeignKey(entity = FinanceCategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"]),
    ],
    indices = [
        Index(name = "idx_finance_operations_date", value = ["date"]),
        Index(name = "idx_finance_operations_account", value = ["account_id"]),
        Index(name = "idx_finance_operations_to_account", value = ["to_account_id"]),
        Index(name = "idx_finance_operations_category", value = ["category_id"]),
    ],
)
data class FinanceOperationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "to_account_id") val toAccountId: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "place") val place: String? = null,
    @ColumnInfo(name = "comment") val comment: String? = null,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Счёт с остатком сейчас: начальный остаток плюс операции не из корзины. */
data class AccountWithBalance(
    @Embedded val account: FinanceAccountEntity,
    @ColumnInfo(name = "balance") val balance: Long,
)

/** Операция с названиями счёта, счёта назначения и категории — для списка. */
data class OperationRow(
    @Embedded val operation: FinanceOperationEntity,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "to_account_name") val toAccountName: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
)
