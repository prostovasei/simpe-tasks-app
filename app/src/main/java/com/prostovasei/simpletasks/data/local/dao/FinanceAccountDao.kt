package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.FinanceAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceAccountDao {
    @Insert
    suspend fun insert(account: FinanceAccountEntity)

    @Query("SELECT * FROM finance_accounts ORDER BY sort_order, created_at")
    fun observeAll(): Flow<List<FinanceAccountEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM finance_accounts")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE finance_accounts SET name = :name, type = :type, emoji = :emoji, initial_balance = :initialBalance, updated_at = :now WHERE id = :id")
    suspend fun update(id: String, name: String, type: String, emoji: String?, initialBalance: Long, now: Long): Int
}
