package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.prostovasei.simpletasks.data.local.entity.FinanceCategoryEntity
import com.prostovasei.simpletasks.data.local.entity.FinanceOperationEntity
import com.prostovasei.simpletasks.data.local.entity.OperationRow
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceOperationDao {
    @Insert
    suspend fun insert(operation: FinanceOperationEntity)

    @Update
    suspend fun update(operation: FinanceOperationEntity)

    @Query("SELECT * FROM finance_operations WHERE id = :id")
    suspend fun getById(id: String): FinanceOperationEntity?

    @Query("$ROWS WHERE o.is_deleted = 0 ORDER BY o.date DESC, o.created_at DESC")
    fun observeActive(): Flow<List<OperationRow>>

    @Query("UPDATE finance_operations SET is_deleted = 1, deleted_at = :now, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun trash(id: String, now: Long): Int

    @Query("$ROWS WHERE o.is_deleted = 1 ORDER BY o.deleted_at DESC, o.created_at DESC")
    fun observeTrash(): Flow<List<OperationRow>>

    @Query("UPDATE finance_operations SET is_deleted = 0, deleted_at = NULL, updated_at = :now WHERE id = :id AND is_deleted = 1")
    suspend fun restore(id: String, now: Long): Int

    @Query("DELETE FROM finance_operations WHERE id = :id AND is_deleted = 1")
    suspend fun deleteTrashed(id: String): Int

    @Query("DELETE FROM finance_operations WHERE is_deleted = 1")
    suspend fun emptyTrash(): Int

    @Query("SELECT * FROM finance_operations ORDER BY created_at")
    suspend fun getAll(): List<FinanceOperationEntity>

    @Insert
    suspend fun insertAll(operations: List<FinanceOperationEntity>)

    @Query("DELETE FROM finance_operations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM finance_operations")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM finance_categories ORDER BY sort_order, created_at")
    suspend fun getAllCategories(): List<FinanceCategoryEntity>

    @Insert
    suspend fun insertAllCategories(categories: List<FinanceCategoryEntity>)

    @Query("DELETE FROM finance_categories")
    suspend fun deleteAllCategories()

    @Query("SELECT COUNT(*) FROM finance_categories")
    fun observeCategoryCount(): Flow<Int>

    /** Счёт последней введённой вручную операции (пустой список — операций нет); корректировки сверки не считаются. */
    @Query("SELECT account_id FROM finance_operations WHERE is_deleted = 0 AND kind != 'correction' ORDER BY created_at DESC LIMIT 1")
    fun observeLastAccount(): Flow<List<String>>

    /** Категории по давности последнего использования: недавние первыми. */
    @Query("SELECT category_id FROM finance_operations WHERE is_deleted = 0 AND category_id IS NOT NULL GROUP BY category_id ORDER BY MAX(created_at) DESC")
    fun observeRecentCategories(): Flow<List<String>>

    /** Ранее введённые места: недавние первыми. */
    @Query("SELECT place FROM finance_operations WHERE is_deleted = 0 AND place IS NOT NULL AND place != '' GROUP BY place ORDER BY MAX(created_at) DESC LIMIT 200")
    fun observePlaces(): Flow<List<String>>

    @Insert
    suspend fun insertCategory(category: FinanceCategoryEntity)

    @Query("SELECT * FROM finance_categories ORDER BY sort_order, created_at")
    fun observeCategories(): Flow<List<FinanceCategoryEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM finance_categories")
    suspend fun nextCategoryOrder(): Int

    @Query("UPDATE finance_categories SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun renameCategory(id: String, name: String, now: Long): Int

    @Query("UPDATE finance_categories SET is_archived = :archived, updated_at = :now WHERE id = :id")
    suspend fun setCategoryArchived(id: String, archived: Boolean, now: Long): Int

    private companion object {
        const val ROWS = "SELECT o.*, a.name AS account_name, t.name AS to_account_name, c.name AS category_name FROM finance_operations o" +
            " JOIN finance_accounts a ON a.id = o.account_id" +
            " LEFT JOIN finance_accounts t ON t.id = o.to_account_id" +
            " LEFT JOIN finance_categories c ON c.id = o.category_id"
    }
}
