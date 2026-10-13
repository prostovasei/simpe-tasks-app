package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Параметры дня для выборок экрана «Все» (см. DayWindow):
 * allDayStart/allDayEnd — границы дня для задач «на весь день» (UTC-полночь даты),
 * start/end — границы дня в часовом поясе пользователя для задач со временем.
 */
@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity)

    /** Возвращает -1, если задача с таким external_id уже есть. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(task: TaskEntity): Long

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id AND is_deleted = 0")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status = 0 AND due_date IS NOT NULL
          AND ((is_all_day = 1 AND due_date < :allDayStart) OR (is_all_day = 0 AND due_date < :start))
        ORDER BY due_date, priority DESC, sort_order, created_at
        """,
    )
    fun observeOverdue(allDayStart: Long, start: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status = 0 AND is_all_day = 1
          AND due_date >= :allDayStart AND due_date < :allDayEnd
        ORDER BY priority DESC, sort_order, created_at
        """,
    )
    fun observeTodayAllDay(allDayStart: Long, allDayEnd: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status = 0 AND is_all_day = 0
          AND due_date >= :start AND due_date < :end
        ORDER BY due_date, priority DESC, sort_order, created_at
        """,
    )
    fun observeTodayTimed(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status = 0 AND due_date IS NOT NULL
          AND ((is_all_day = 1 AND due_date >= :allDayEnd) OR (is_all_day = 0 AND due_date >= :end))
        ORDER BY due_date, priority DESC, sort_order, created_at
        """,
    )
    fun observeUpcoming(allDayEnd: Long, end: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT t.* FROM tasks t
        JOIN projects p ON p.id = t.project_id
        WHERE t.is_deleted = 0 AND t.status = 0 AND (p.is_inbox = 1 OR t.due_date IS NULL)
        ORDER BY t.sort_order, t.created_at
        """,
    )
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE project_id = :projectId AND is_deleted = 0 AND status = 0
        ORDER BY sort_order, created_at
        """,
    )
    fun observeActiveByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM tasks WHERE project_id = :projectId AND is_deleted = 0")
    suspend fun nextSortOrder(projectId: String): Int

    @Query(
        """
        UPDATE tasks SET project_id = :projectId, title = :title, content = :content, priority = :priority,
            due_date = :dueDate, is_all_day = :isAllDay, rrule = :rrule, sort_order = :sortOrder, updated_at = :now
        WHERE id = :id AND is_deleted = 0
        """,
    )
    suspend fun update(
        id: String,
        projectId: String,
        title: String,
        content: String,
        priority: Int,
        dueDate: Long?,
        isAllDay: Boolean,
        rrule: String?,
        sortOrder: Int,
        now: Long,
    ): Int

    @Query(
        """
        UPDATE tasks SET status = :status, completed_at = :completedAt, updated_at = :now
        WHERE id = :id AND is_deleted = 0
        """,
    )
    suspend fun setStatus(id: String, status: Int, completedAt: Long?, now: Long): Int

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :now WHERE id = :id AND is_deleted = 0")
    suspend fun softDelete(id: String, now: Long): Int

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :now WHERE project_id = :projectId AND is_deleted = 0")
    suspend fun softDeleteByProject(projectId: String, now: Long): Int
}
