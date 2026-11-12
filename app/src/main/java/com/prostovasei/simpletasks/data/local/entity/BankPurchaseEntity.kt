package com.prostovasei.simpletasks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Покупка из уведомления банка (#233; миграция 7 → 8). Хранятся только сумма, счёт и время — без текста уведомления.
 * [notificationKey] — ключ уведомления банка (пакет, номер и время показа), чтобы повтор не создал вторую покупку.
 * [state]: [PENDING] — в «Не разобрано», [RECORDED] — записана операцией [operationId], [SKIPPED] — пропущена.
 * [duplicateId] — похожий расход, найденный при появлении покупки. Связей FOREIGN KEY нет: список временный,
 * в резервную копию не входит и очищается при её восстановлении.
 */
@Entity(
    tableName = "bank_purchases",
    indices = [
        Index(name = "idx_bank_purchases_key", value = ["notification_key"], unique = true),
        Index(name = "idx_bank_purchases_state", value = ["state"]),
    ],
)
data class BankPurchaseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "notification_key") val notificationKey: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "state", defaultValue = "0") val state: Int = PENDING,
    @ColumnInfo(name = "operation_id") val operationId: String? = null,
    @ColumnInfo(name = "duplicate_id") val duplicateId: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val PENDING = 0
        const val RECORDED = 1
        const val SKIPPED = 2
    }
}

/** Покупка с названием счёта и похожим расходом (если он ещё не в корзине). */
data class PendingPurchaseRow(
    @Embedded val purchase: BankPurchaseEntity,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "dup_id") val duplicateId: String?,
    @ColumnInfo(name = "dup_category") val duplicateCategory: String?,
    @ColumnInfo(name = "dup_amount") val duplicateAmount: Long?,
    @ColumnInfo(name = "dup_date") val duplicateDate: Long?,
)
