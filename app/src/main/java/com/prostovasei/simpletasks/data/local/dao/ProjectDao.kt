package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectWithOpenCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: ProjectEntity)

    @Query(
        """
        SELECT p.*,
            (SELECT COUNT(*) FROM tasks t
             WHERE t.project_id = p.id AND t.status = 0 AND t.is_deleted = 0) AS open_count
        FROM projects p
        WHERE p.is_deleted = 0
        ORDER BY p.sort_order, p.created_at
        """,
    )
    fun observeActiveWithOpenCount(): Flow<List<ProjectWithOpenCount>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id AND is_deleted = 0")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE name = :name AND is_inbox = 0 AND is_deleted = 0 ORDER BY created_at LIMIT 1")
    suspend fun getActiveByName(name: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE is_inbox = 1 AND is_deleted = 0 ORDER BY created_at LIMIT 1")
    suspend fun getInbox(): ProjectEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM projects WHERE group_id IS :groupId AND is_deleted = 0")
    suspend fun nextSortOrder(groupId: String?): Int

    @Query(
        """
        UPDATE projects SET name = :name, group_id = :groupId, color = :color, updated_at = :now
        WHERE id = :id AND is_deleted = 0
        """,
    )
    suspend fun update(id: String, name: String, groupId: String?, color: String?, now: Long): Int

    @Query("UPDATE projects SET group_id = NULL, updated_at = :now WHERE group_id = :groupId AND is_deleted = 0")
    suspend fun ungroup(groupId: String, now: Long): Int

    @Query("UPDATE projects SET is_deleted = 1, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun softDelete(id: String, now: Long): Int
}
