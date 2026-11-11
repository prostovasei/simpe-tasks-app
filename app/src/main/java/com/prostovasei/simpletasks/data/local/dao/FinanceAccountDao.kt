package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.AccountWithBalance
import com.prostovasei.simpletasks.data.local.entity.FinanceAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceAccountDao {
    @Insert
    suspend fun insert(account: FinanceAccountEntity)

    /** Остаток: начальный + доходы − расходы − переводы со счёта + переводы на счёт; операции в корзине не считаются. */
    @Query("SELECT a.*, $BALANCE AS balance FROM finance_accounts a ORDER BY a.sort_order, a.created_at")
    fun observeAll(): Flow<List<AccountWithBalance>>

    @Query("SELECT a.*, $BALANCE AS balance FROM finance_accounts a WHERE a.id = :id")
    suspend fun getWithBalance(id: String): AccountWithBalance?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM finance_accounts")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE finance_accounts SET name = :name, type = :type, emoji = :emoji, initial_balance = :initialBalance, updated_at = :now WHERE id = :id")
    suspend fun update(id: String, name: String, type: String, emoji: String?, initialBalance: Long, now: Long): Int

    private companion object {
        const val BALANCE = "a.initial_balance" +
            " + COALESCE((SELECT SUM(CASE o.kind WHEN 'income' THEN o.amount ELSE -o.amount END) FROM finance_operations o WHERE o.account_id = a.id AND o.is_deleted = 0), 0)" +
            " + COALESCE((SELECT SUM(o.amount) FROM finance_operations o WHERE o.to_account_id = a.id AND o.is_deleted = 0), 0)"
    }
}
