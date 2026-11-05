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
 * doneSince — начало сегодняшнего дня: выполненные с этого момента остаются в списках до полуночи (итерация 27).
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
        SELECT t.*, p.name AS list_name, p.is_inbox AS list_is_inbox, p.emoji AS list_emoji, g.name AS folder_name
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
        WHERE is_deleted = 0 AND (status = 0 OR (status = 2 AND completed_at >= :doneSince)) AND due_date IS NOT NULL
          AND ((is_all_day = 1 AND due_date < :allDayStart) OR (is_all_day = 0 AND due_date < :start))
        ORDER BY due_date, priority DESC, sort_order, created_at
        """,
    )
    fun observeOverdue(allDayStart: Long, start: Long, doneSince: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND (status = 0 OR (status = 2 AND completed_at >= :doneSince)) AND is_all_day = 1
          AND due_date >= :allDayStart AND due_date < :allDayEnd
        ORDER BY priority DESC, sort_order, created_at
        """,
    )
    fun observeTodayAllDay(allDayStart: Long, allDayEnd: Long, doneSince: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND (status = 0 OR (status = 2 AND completed_at >= :doneSince)) AND is_all_day = 0
          AND due_date >= :start AND due_date < :end
        ORDER BY COALESCE(start_date, due_date), priority DESC, sort_order, created_at
        """,
    )
    fun observeTodayTimed(start: Long, end: Long, doneSince: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND (status = 0 OR (status = 2 AND completed_at >= :doneSince)) AND due_date IS NOT NULL
          AND ((is_all_day = 1 AND due_date >= :allDayEnd) OR (is_all_day = 0 AND due_date >= :end))
        ORDER BY due_date, priority DESC, sort_order, created_at
        """,
    )
    fun observeUpcoming(allDayEnd: Long, end: Long, doneSince: Long): Flow<List<TaskEntity>>

    /**
     * Неделя (итерация 44): активные и выполненные задачи, у которых начало (или срок) попадает в промежуток недели —
     * для задач «на весь день» по границам [allDayStart, allDayEnd), для задач со временем — [start, end).
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE is_deleted = 0 AND status IN (0, 2) AND due_date IS NOT NULL
          AND ((is_all_day = 1 AND COALESCE(start_date, due_date) >= :allDayStart AND COALESCE(start_date, due_date) < :allDayEnd)
            OR (is_all_day = 0 AND COALESCE(start_date, due_date) >= :start AND COALESCE(start_date, due_date) < :end))
        ORDER BY COALESCE(start_date, due_date), priority DESC, sort_order, created_at
        """,
    )
    fun observeWeek(allDayStart: Long, allDayEnd: Long, start: Long, end: Long): Flow<List<TaskEntity>>

    /** Активные повторяющиеся задачи со сроком: их будущие разы показываются в неделе (итерация 44). */
    @Query("SELECT * FROM tasks WHERE is_deleted = 0 AND status = 0 AND rrule IS NOT NULL AND due_date IS NOT NULL")
    fun observeRepeating(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT t.* FROM tasks t
        JOIN projects p ON p.id = t.project_id
        WHERE t.is_deleted = 0 AND (t.status = 0 OR (t.status = 2 AND t.completed_at >= :doneSince)) AND (p.is_inbox = 1 OR t.due_date IS NULL)
        ORDER BY t.sort_order, t.created_at
        """,
    )
    fun observeInbox(doneSince: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE project_id = :projectId AND is_deleted = 0 AND (status = 0 OR (status = 2 AND completed_at >= :doneSince))
        ORDER BY sort_order, created_at
        """,
    )
    fun observeActiveByProject(projectId: String, doneSince: Long): Flow<List<TaskEntity>>

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
        UPDATE tasks SET status = :status, completed_at = :completedAt, updated_at = :now,
            is_repeat_done = CASE WHEN :status = 2 THEN is_repeat_done ELSE 0 END
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

    /** Возвращает то, что удалил `softDeleteWithSubtasks` в момент [deletedAt]: ранее удалённые подзадачи остаются удалёнными. */
    @Query(
        """
        UPDATE tasks SET is_deleted = 0, updated_at = :now
        WHERE is_deleted = 1 AND updated_at = :deletedAt AND id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun restoreWithSubtasks(id: String, deletedAt: Long, now: Long): Int

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :now WHERE project_id = :projectId AND is_deleted = 0")
    suspend fun softDeleteByProject(projectId: String, now: Long): Int

    // ---- Корзина (итерация 39, бэклог №35) ----

    /** Корни удалённых деревьев, новые сверху. Подзадачи, удалённые вместе с родителем (тот же момент), отдельно не показываются. */
    @Query(
        """
        SELECT t.* FROM tasks t
        WHERE t.is_deleted = 1 AND NOT EXISTS (
            SELECT 1 FROM tasks p WHERE p.id = t.parent_id AND p.is_deleted = 1 AND p.updated_at = t.updated_at
        )
        ORDER BY t.updated_at DESC, t.created_at DESC
        """,
    )
    fun observeTrash(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT t.* FROM tasks t
        WHERE t.is_deleted = 1 AND NOT EXISTS (
            SELECT 1 FROM tasks p WHERE p.id = t.parent_id AND p.is_deleted = 1 AND p.updated_at = t.updated_at
        )
        ORDER BY t.updated_at DESC, t.created_at DESC
        """,
    )
    suspend fun getTrash(): List<TaskEntity>

    @Query(
        """
        SELECT COUNT(*) FROM tasks t
        WHERE t.is_deleted = 1 AND NOT EXISTS (
            SELECT 1 FROM tasks p WHERE p.id = t.parent_id AND p.is_deleted = 1 AND p.updated_at = t.updated_at
        )
        """,
    )
    fun observeTrashCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE id = :id AND is_deleted = 1")
    fun observeTrashedById(id: String): Flow<TaskEntity?>

    /** Подзадачи, удалённые вместе с родителем в момент [deletedAt]. */
    @Query("SELECT * FROM tasks WHERE parent_id = :parentId AND is_deleted = 1 AND updated_at = :deletedAt ORDER BY sort_order, created_at")
    fun observeTrashedSubtasks(parentId: String, deletedAt: Long): Flow<List<TaskEntity>>

    /** Вернувшиеся из корзины задачи дерева из удалённого списка переезжают в [inboxId] («Входящие»). */
    @Query(
        """
        UPDATE tasks SET project_id = :inboxId, updated_at = :now
        WHERE project_id = :oldProjectId AND id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun moveTreeToProject(id: String, oldProjectId: String, inboxId: String, now: Long): Int

    /** Живые подзадачи стираемого дерева теряют родителя: внешний ключ не даёт удалить родителя, пока на него ссылаются. */
    @Query(
        """
        UPDATE tasks SET parent_id = NULL
        WHERE is_deleted = 0 AND parent_id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun detachLiveChildrenOfTree(id: String): Int

    @Query(
        """
        DELETE FROM tasks WHERE is_deleted = 1 AND id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun deleteTrashedTree(id: String): Int

    @Query("UPDATE tasks SET parent_id = NULL WHERE is_deleted = 0 AND parent_id IN (SELECT id FROM tasks WHERE is_deleted = 1)")
    suspend fun detachLiveChildrenOfTrash(): Int

    @Query("DELETE FROM tasks WHERE is_deleted = 1")
    suspend fun deleteAllTrashed(): Int

    /** Снова в корзину (отмена восстановления): всё дерево с прежним моментом удаления [at]. */
    @Query(
        """
        UPDATE tasks SET is_deleted = 1, updated_at = :at
        WHERE is_deleted = 0 AND id IN (
            WITH RECURSIVE tree(id) AS (
                SELECT :id
                UNION SELECT t.id FROM tasks t JOIN tree ON t.parent_id = tree.id
            )
            SELECT id FROM tree
        )
        """,
    )
    suspend fun trashTreeAt(id: String, at: Long): Int
}
