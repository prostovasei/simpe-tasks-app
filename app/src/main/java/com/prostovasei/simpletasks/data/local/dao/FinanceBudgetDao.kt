package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.FinanceBudgetEntity
import kotlinx.coroutines.flow.Flow

/** Бюджеты категорий по месяцам (#327). */
@Dao
interface FinanceBudgetDao {
    @Query("SELECT * FROM finance_budgets ORDER BY month, category_id")
    fun observeAll(): Flow<List<FinanceBudgetEntity>>

    @Query("SELECT * FROM finance_budgets ORDER BY month, category_id")
    suspend fun getAll(): List<FinanceBudgetEntity>

    @Query("SELECT * FROM finance_budgets WHERE month = :month")
    suspend fun getMonth(month: String): List<FinanceBudgetEntity>

    /** Последний заведённый месяц раньше [month] — откуда переносятся бюджеты. */
    @Query("SELECT MAX(month) FROM finance_budgets WHERE month < :month")
    suspend fun lastMonthBefore(month: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<FinanceBudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<FinanceBudgetEntity>)

    @Query("DELETE FROM finance_budgets")
    suspend fun deleteAll()
}
