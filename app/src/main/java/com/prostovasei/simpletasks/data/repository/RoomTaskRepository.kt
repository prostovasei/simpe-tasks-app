package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.DayWindow
import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.TaskEntity
import com.prostovasei.simpletasks.data.mapper.toDomain
import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.nextOccurrence
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RoomTaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : TaskRepository {

    private val taskDao = db.taskDao()
    private val checklistDao = db.checklistDao()

    override fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks> {
        val day = DayWindow.of(today, time.zone())
        return combine(
            taskDao.observeOverdue(day.allDayStart, day.start),
            taskDao.observeTodayAllDay(day.allDayStart, day.allDayEnd),
            taskDao.observeTodayTimed(day.start, day.end),
            taskDao.observeUpcoming(day.allDayEnd, day.end),
        ) { overdue, allDay, timed, upcoming ->
            AllScreenTasks(
                overdue = overdue.map { it.toDomain() },
                todayAllDay = allDay.map { it.toDomain() },
                todayTimed = timed.map { it.toDomain() },
                upcoming = upcoming.map { it.toDomain() },
            )
        }.distinctUntilChanged()
    }

    override fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInbox().map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeProjectTasks(projectId: String): Flow<List<Task>> =
        taskDao.observeActiveByProject(projectId).map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeTaskDetails(taskId: String): Flow<TaskDetails?> =
        combine(taskDao.observeById(taskId), checklistDao.observeByTask(taskId)) { task, items ->
            task?.let { TaskDetails(it.toDomain(), items.map { item -> item.toDomain() }) }
        }.distinctUntilChanged()

    override fun observeSubtasks(parentId: String): Flow<List<Task>> =
        taskDao.observeSubtasks(parentId).map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun createTask(task: NewTask): String {
        val id = ids.newId()
        db.withTransaction {
            taskDao.insert(newEntity(id, task))
            insertChecklist(id, task.checklist)
        }
        return id
    }

    override suspend fun updateTask(task: Task) {
        require(task.title.isNotBlank()) { "Название задачи не может быть пустым" }
        val due = DueDateCodec.encode(task.due)
        val start = DueDateCodec.encode(task.start)
        require(task.start == null || start.isAllDay == due.isAllDay) { "Начало и срок должны быть одного типа" }
        taskDao.update(
            id = task.id,
            projectId = task.projectId,
            title = task.title,
            content = task.content,
            priority = task.priority.value,
            dueDate = due.dueDate,
            startDate = start.dueDate,
            isAllDay = due.isAllDay,
            rrule = task.rrule,
            reminders = task.reminders.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            parentId = task.parentId,
            sortOrder = task.sortOrder,
            now = nowMillis(),
        )
    }

    override suspend fun getTask(taskId: String): Task? = taskDao.getById(taskId)?.takeUnless { it.isDeleted }?.toDomain()

    override fun observeDataChanges(): Flow<Unit> = taskDao.observeDataChanges().map { }

    override fun observeWithReminders(): Flow<List<Task>> =
        taskDao.observeWithReminders().map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun complete(taskId: String): Completion = db.withTransaction {
        val entity = taskDao.getById(taskId)?.takeIf { !it.isDeleted && it.status == TaskStatus.ACTIVE.value }
            ?: return@withTransaction Completion.Done(taskId)
        val task = entity.toDomain()
        val next = task.nextOccurrence(time.zone(), time.today())
        if (next == null) {
            setCompleted(taskId, completed = true)
            return@withTransaction Completion.Done(taskId)
        }
        val now = nowMillis()
        // Выполненный раз — отдельная задача со старым сроком и отмеченным чек-листом.
        val copyId = ids.newId()
        taskDao.insert(
            entity.copy(
                id = copyId,
                status = TaskStatus.COMPLETED.value,
                completedAt = now,
                rrule = null,
                externalId = null,
                sourceData = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val items = checklistDao.getByTask(taskId)
        checklistDao.insertAll(items.map { it.copy(id = ids.newId(), taskId = copyId, createdAt = now, updatedAt = now) })
        val checked = items.filter { it.isDone }.map { it.id }
        checked.forEach { checklistDao.setDone(it, done = false, now = now) }
        updateTask(next)
        Completion.Rescheduled(taskId, previous = task, next = next, doneCopyId = copyId, checkedItemIds = checked)
    }

    override suspend fun undo(completion: Completion) {
        when (completion) {
            is Completion.Done -> setCompleted(completion.taskId, completed = false)
            is Completion.Rescheduled -> db.withTransaction {
                val now = nowMillis()
                checklistDao.deleteHardByTask(completion.doneCopyId)
                taskDao.deleteHard(completion.doneCopyId)
                updateTask(completion.previous)
                completion.checkedItemIds.forEach { checklistDao.setDone(it, done = true, now = now) }
            }
        }
    }

    override suspend fun moveToTomorrow(taskId: String) {
        val task = getTask(taskId) ?: return
        val zone = time.zone()
        val tomorrow = time.today().plusDays(1)
        val newDue = when (val due = task.due) {
            null, is TaskDue.AllDay -> TaskDue.AllDay(tomorrow)
            is TaskDue.AtTime -> TaskDue.AtTime(due.instant.atZone(zone).toLocalTime().atDate(tomorrow).atZone(zone).toInstant())
        }
        updateTask(task.withDue(newDue))
    }

    override fun observeTimedInRange(from: Instant, to: Instant): Flow<List<Task>> =
        taskDao.observeTimedInRange(from.toEpochMilli(), to.toEpochMilli()).map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeSearchable(): Flow<List<SearchableTask>> =
        taskDao.observeSearchable().map { rows ->
            rows.map { row -> SearchableTask(row.task.toDomain(), row.checklistText?.split('\n').orEmpty()) }
        }.distinctUntilChanged()

    override fun observeCompleted(): Flow<List<Task>> =
        taskDao.observeCompleted().map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun restore(taskId: String): Task? {
        val task = getTask(taskId)?.takeIf { it.isCompleted } ?: return null
        db.withTransaction {
            if (task.rrule != null) updateTask(task.copy(rrule = null))
            taskDao.setStatus(taskId, TaskStatus.ACTIVE.value, completedAt = null, now = nowMillis())
        }
        return task
    }

    override suspend fun undoRestore(previous: Task) {
        val now = nowMillis()
        db.withTransaction {
            updateTask(previous)
            taskDao.setStatus(previous.id, TaskStatus.COMPLETED.value, completedAt = previous.completedAt?.toEpochMilli() ?: now, now = now)
        }
    }

    override suspend fun setCompleted(taskId: String, completed: Boolean) {
        val now = nowMillis()
        if (completed) {
            taskDao.setStatus(taskId, TaskStatus.COMPLETED.value, completedAt = now, now = now)
        } else {
            taskDao.setStatus(taskId, TaskStatus.ACTIVE.value, completedAt = null, now = now)
        }
    }

    override suspend fun deleteTask(taskId: String) {
        val now = nowMillis()
        db.withTransaction {
            checklistDao.softDeleteByTaskTree(taskId, now)
            taskDao.softDeleteWithSubtasks(taskId, now)
        }
    }

    private suspend fun newEntity(id: String, task: NewTask): TaskEntity {
        require(task.title.isNotBlank()) { "Название задачи не может быть пустым" }
        val now = nowMillis()
        val due = DueDateCodec.encode(task.due)
        val start = DueDateCodec.encode(task.start?.takeIf { task.due != null })
        require(task.start == null || task.due == null || start.isAllDay == due.isAllDay) { "Начало и срок должны быть одного типа" }
        return TaskEntity(
            id = id,
            projectId = task.projectId,
            title = task.title,
            content = task.content,
            priority = task.priority.value,
            dueDate = due.dueDate,
            startDate = start.dueDate,
            isAllDay = due.isAllDay,
            sortOrder = taskDao.nextSortOrder(task.projectId),
            rrule = task.rrule?.takeIf { task.due != null },
            reminders = task.reminders.takeIf { task.due != null && it.isNotEmpty() }?.joinToString("\n"),
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun insertChecklist(taskId: String, titles: List<String>) {
        if (titles.isEmpty()) return
        val now = nowMillis()
        checklistDao.insertAll(
            titles.mapIndexed { index, title ->
                require(title.isNotBlank()) { "Пункт чек-листа не может быть пустым" }
                ChecklistItemEntity(
                    id = ids.newId(),
                    taskId = taskId,
                    title = title,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }

    private fun nowMillis() = time.now().toEpochMilli()
}
