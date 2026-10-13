package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.ProjectGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectGroupDao {
    @Insert
    suspend fun insert(group: ProjectGroupEntity)

    @Query("SELECT * FROM project_groups WHERE is_deleted = 0 ORDER BY sort_order, created_at")
    fun observeActive(): Flow<List<ProjectGroupEntity>>

    @Query("SELECT * FROM project_groups WHERE id = :id")
    suspend fun getById(id: String): ProjectGroupEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM project_groups WHERE is_deleted = 0")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE project_groups SET name = :name, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun rename(id: String, name: String, now: Long): Int

    @Query("UPDATE project_groups SET is_deleted = 1, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun softDelete(id: String, now: Long): Int
}
