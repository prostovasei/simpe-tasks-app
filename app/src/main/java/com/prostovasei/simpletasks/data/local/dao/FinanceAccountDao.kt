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

    /** Остаток: начальный + доходы + корректировки (со знаком) − расходы − переводы со счёта + переводы на счёт; корзина не считается. */
    @Query("SELECT a.*, $BALANCE AS balance FROM finance_accounts a ORDER BY a.sort_order, a.created_at")
    fun observeAll(): Flow<List<AccountWithBalance>>

    @Query("SELECT a.*, $BALANCE AS balance FROM finance_accounts a WHERE a.id = :id")
    suspend fun getWithBalance(id: String): AccountWithBalance?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM finance_accounts")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE finance_accounts SET name = :name, type = :type, emoji = :emoji, updated_at = :now WHERE id = :id")
    suspend fun update(id: String, name: String, type: String, emoji: String?, now: Long): Int

    @Query("UPDATE finance_accounts SET initial_balance = :initialBalance, updated_at = :now WHERE id = :id")
    suspend fun setInitialBalance(id: String, initialBalance: Long, now: Long): Int

    @Query("SELECT * FROM finance_accounts ORDER BY sort_order, created_at")
    suspend fun getAll(): List<FinanceAccountEntity>

    @Insert
    suspend fun insertAll(accounts: List<FinanceAccountEntity>)

    @Query("DELETE FROM finance_accounts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM finance_accounts")
    fun observeCount(): Flow<Int>

    private companion object {
        const val BALANCE = "a.initial_balance" +
            " + COALESCE((SELECT SUM(CASE o.kind WHEN 'income' THEN o.amount WHEN 'correction' THEN o.amount ELSE -o.amount END) FROM finance_operations o WHERE o.account_id = a.id AND o.is_deleted = 0), 0)" +
            " + COALESCE((SELECT SUM(o.amount) FROM finance_operations o WHERE o.to_account_id = a.id AND o.is_deleted = 0), 0)"
    }
}
