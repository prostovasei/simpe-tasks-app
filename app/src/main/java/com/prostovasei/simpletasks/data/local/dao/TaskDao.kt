package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.DataStatsRow
import com.prostovasei.simpletasks.data.local.entity.ExportTaskRow
import com.prostovasei.simpletasks.data.local.entity.SearchTaskRow
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

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM tasks WHERE is_deleted = 0) AS total_tasks,
            (SELECT COUNT(*) FROM tasks WHERE is_deleted = 0 AND status = 0) AS active_tasks,
            (SELECT COUNT(*) FROM projects WHERE is_deleted = 0) AS lists
        """,
    )
    fun observeStats(): Flow<DataStatsRow>

    @Query(
        """
        SELECT t.*, p.name AS list_name, p.is_inbox AS list_is_inbox, g.name AS folder_name
        FROM tasks t
        JOIN projects p ON p.id = t.project_id
        LEFT JOIN project_groups g ON g.id = p.group_id AND g.is_deleted = 0
        WHERE t.is_deleted = 0
        ORDER BY p.is_inbox DESC, p.sort_order, p.created_at, t.sort_order, t.created_at
        """,
    )
    suspend fun getAllForExport(): List<ExportTaskRow>

    @Query("SELECT id FROM tasks WHERE external_id = :externalId")
    suspend fun getIdByExternalId(externalId: String): String?

    @Query("UPDATE tasks SET parent_id = :parentId WHERE id = :id AND parent_id IS NULL")
    suspend fun linkParent(id: String, parentId: String): Int

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
        ORDER BY COALESCE(start_date, due_date), priority DESC, sort_order, created_at
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

    /**
     * Задачи со временем для сетки недели (итерация 16): попадающие в промежуток [start, end) и все активные
     * повторяющиеся — их будущие разы считает приложение. Время — по началу интервала, если оно есть.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND is_all_day = 0 AND due_date IS NOT NULL
          AND ((COALESCE(start_date, due_date) < :end AND due_date >= :start) OR (rrule IS NOT NULL AND status = 0))
        """,
    )
    fun observeTimedInRange(start: Long, end: Long): Flow<List<TaskEntity>>

    /**
     * Все задачи, кроме удалённых, с текстом чек-листа — для поиска. Фильтр по словам — в приложении:
     * LIKE в SQLite не различает регистр только у латиницы.
     */
    @Query(
        """
        SELECT t.*, (
            SELECT GROUP_CONCAT(c.title, char(10)) FROM checklist_items c WHERE c.task_id = t.id AND c.is_deleted = 0
        ) AS checklist_text
        FROM tasks t
        WHERE t.is_deleted = 0
        """,
    )
    fun observeSearchable(): Flow<List<SearchTaskRow>>

    /** Выполненные задачи: новые сверху (у импортированных без времени выполнения — по времени изменения). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status = 2
        ORDER BY COALESCE(completed_at, updated_at) DESC
        """,
    )
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE parent_id = :parentId AND is_deleted = 0
        ORDER BY status != 0, sort_order, created_at
        """,
    )
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM tasks WHERE project_id = :projectId AND is_deleted = 0")
    suspend fun nextSortOrder(projectId: String): Int

    @Query(
        """
        UPDATE tasks SET project_id = :projectId, title = :title, content = :content, priority = :priority,
            due_date = :dueDate, start_date = :startDate, is_all_day = :isAllDay, rrule = :rrule,
            reminders = :reminders, parent_id = :parentId, sort_order = :sortOrder, updated_at = :now
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
        startDate: Long?,
        isAllDay: Boolean,
        rrule: String?,
        reminders: String?,
        parentId: String?,
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

    /** Активные задачи со сроком и напоминаниями — для расписания уведомлений. */
    @Query("SELECT * FROM tasks WHERE is_deleted = 0 AND status = 0 AND due_date IS NOT NULL AND reminders IS NOT NULL")
    fun observeWithReminders(): Flow<List<TaskEntity>>

    /** Срабатывает при любой записи в задачи или списки — для обновления виджета. */
    @Query("SELECT (SELECT COUNT(*) FROM tasks) + (SELECT COUNT(*) FROM projects)")
    fun observeDataChanges(): Flow<Int>

    /** Физическое удаление — только для только что созданной копии при «Отмене». */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteHard(id: String)

    /** Мягко удаляет задачу и всё её поддерево подзадач. */
    @Query(
        """
        UPDATE tasks SET is_deleted = 1, updated_at = :now
        WHERE is_deleted = 0 AND id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun softDeleteWithSubtasks(id: String, now: Long): Int

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :now WHERE project_id = :projectId AND is_deleted = 0")
    suspend fun softDeleteByProject(projectId: String, now: Long): Int
}
