package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.BankPurchaseEntity
import com.prostovasei.simpletasks.data.local.entity.PendingPurchaseRow
import kotlinx.coroutines.flow.Flow

/** Покупки из уведомлений банка (#233). */
@Dao
interface BankPurchaseDao {
    /** -1 — уведомление с таким ключом уже было. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(purchase: BankPurchaseEntity): Long

    /**
     * Похожий расход (§4.5): тот же счёт, сумма и день, не в корзине и ещё не связанный с другой распознанной покупкой.
     */
    @Query(
        "SELECT o.id FROM finance_operations o WHERE o.is_deleted = 0 AND o.kind = 'expense' AND o.account_id = :accountId" +
            " AND o.amount = :amount AND o.date = :date" +
            " AND o.id NOT IN (SELECT operation_id FROM bank_purchases WHERE operation_id IS NOT NULL) ORDER BY o.created_at LIMIT 1",
    )
    suspend fun findDuplicate(accountId: String, amount: Long, date: Long): String?

    @Query("$ROWS WHERE p.state = ${BankPurchaseEntity.PENDING} ORDER BY p.posted_at DESC")
    fun observePending(): Flow<List<PendingPurchaseRow>>

    @Query("$ROWS WHERE p.id = :id")
    suspend fun getRow(id: String): PendingPurchaseRow?

    @Query("SELECT * FROM bank_purchases WHERE id = :id AND state = ${BankPurchaseEntity.PENDING}")
    suspend fun getPending(id: String): BankPurchaseEntity?

    /** Ответ на покупку: только для ещё не разобранной. Возвращает 0, если на неё уже ответили. */
    @Query("UPDATE bank_purchases SET state = :state, operation_id = :operationId, updated_at = :now WHERE id = :id AND state = ${BankPurchaseEntity.PENDING}")
    suspend fun answer(id: String, state: Int, operationId: String?, now: Long): Int

    /** Разобранные покупки старше [before] больше не нужны: их уведомления давно не повторяются. */
    @Query("DELETE FROM bank_purchases WHERE state != ${BankPurchaseEntity.PENDING} AND updated_at < :before")
    suspend fun pruneAnswered(before: Long)

    @Query("DELETE FROM bank_purchases")
    suspend fun deleteAll()

    private companion object {
        const val ROWS = "SELECT p.*, a.name AS account_name, a.emoji AS account_emoji, a.type AS account_type, o.id AS dup_id, c.name AS dup_category, o.amount AS dup_amount, o.date AS dup_date" +
            " FROM bank_purchases p JOIN finance_accounts a ON a.id = p.account_id" +
            " LEFT JOIN finance_operations o ON o.id = p.duplicate_id AND o.is_deleted = 0" +
            " LEFT JOIN finance_categories c ON c.id = o.category_id"
    }
}
