package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Insert
    suspend fun insert(item: ChecklistItemEntity)

    @Insert
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getById(id: String): ChecklistItemEntity?

    @Query("SELECT * FROM checklist_items WHERE task_id = :taskId AND is_deleted = 0 ORDER BY sort_order, created_at")
    fun observeByTask(taskId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT id FROM checklist_items WHERE task_id = :taskId AND is_deleted = 0")
    suspend fun getActiveIds(taskId: String): List<String>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM checklist_items WHERE task_id = :taskId AND is_deleted = 0")
    suspend fun nextSortOrder(taskId: String): Int

    @Query("UPDATE checklist_items SET title = :title, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun rename(id: String, title: String, now: Long): Int

    @Query("UPDATE checklist_items SET is_done = :done, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun setDone(id: String, done: Boolean, now: Long): Int

    @Query("UPDATE checklist_items SET sort_order = :sortOrder, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun setSortOrder(id: String, sortOrder: Int, now: Long): Int

    @Query("UPDATE checklist_items SET is_deleted = 1, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun softDelete(id: String, now: Long): Int

    @Query("UPDATE checklist_items SET is_deleted = 1, updated_at = :now WHERE task_id = :taskId AND is_deleted = 0")
    suspend fun softDeleteByTask(taskId: String, now: Long): Int

    @Query(
        """
        UPDATE checklist_items SET is_deleted = 1, updated_at = :now
        WHERE is_deleted = 0 AND task_id IN (SELECT id FROM tasks WHERE project_id = :projectId)
        """,
    )
    suspend fun softDeleteByProject(projectId: String, now: Long): Int
}
