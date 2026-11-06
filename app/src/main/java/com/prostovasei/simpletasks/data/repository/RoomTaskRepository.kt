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
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.WontDo
import com.prostovasei.simpletasks.domain.model.atOccurrence
import com.prostovasei.simpletasks.domain.model.repeatsBefore
import com.prostovasei.simpletasks.domain.model.WeekTasks
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.model.Restoration
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.movedTo
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.nextOccurrence
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val projectDao = db.projectDao()

    override fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks> {
        val day = DayWindow.of(today, time.zone())
        return combine(
            taskDao.observeOverdue(day.allDayStart, day.start, day.start),
            taskDao.observeTodayAllDay(day.allDayStart, day.allDayEnd, day.start),
            taskDao.observeTodayTimed(day.start, day.end, day.start),
            taskDao.observeUpcoming(day.allDayEnd, day.end, day.start),
        ) { overdue, allDay, timed, upcoming ->
            AllScreenTasks(
                overdue = overdue.map { it.toDomain() },
                todayAllDay = allDay.map { it.toDomain() },
                todayTimed = timed.map { it.toDomain() },
                upcoming = upcoming.map { it.toDomain() },
            )
        }.distinctUntilChanged()
    }

    override fun observeWeek(weekStart: LocalDate): Flow<WeekTasks> {
        val first = DayWindow.of(weekStart, time.zone())
        val last = DayWindow.of(weekStart.plusDays(WEEK_DAYS - 1L), time.zone())
        return combine(
            taskDao.observeWeek(first.allDayStart, last.allDayEnd, first.start, last.end),
            taskDao.observeRepeating(),
        ) { inRange, repeating -> WeekTasks(inRange.map { it.toDomain() }, repeating.map { it.toDomain() }) }.distinctUntilChanged()
    }

    override fun observeInbox(today: LocalDate): Flow<List<Task>> =
        taskDao.observeInbox(doneSince(today)).map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeProjectTasks(projectId: String, today: LocalDate): Flow<List<Task>> =
        taskDao.observeActiveByProject(projectId, doneSince(today)).map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

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
                isRepeatDone = true,
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

    override suspend fun wontDo(taskId: String, occurrence: TaskDue?, earlier: EarlierRepeats): WontDo? = db.withTransaction {
        val entity = taskDao.getById(taskId)?.takeIf { !it.isDeleted && it.status == TaskStatus.ACTIVE.value }
            ?: return@withTransaction null
        val task = entity.toDomain()
        val now = nowMillis()
        val zone = time.zone()
        val today = time.today()
        val anchor = task.start ?: task.due
        if (task.rrule == null || anchor == null) {
            taskDao.setStatus(taskId, TaskStatus.ABANDONED.value, completedAt = now, now = now)
            return@withTransaction WontDo(taskId, previous = task, closedWhole = true, occurrence = anchor)
        }
        // Будущий раз (как в TickTick): сначала закрываются незакрытые повторы перед ним — пропуском или выполнением.
        val before = occurrence?.takeIf { it != anchor }?.let { task.repeatsBefore(it, zone, today) }.orEmpty()
        val copies = mutableListOf<String>()
        if (earlier == EarlierRepeats.COMPLETE) before.forEach { copies += insertClosedCopy(entity, it, TaskStatus.COMPLETED, now) }
        // Раз, который закрываем: идём по серии, чтобы у «N раз» остаток считался верно; если правило дало другую дату — берём дату раза.
        var target: Task = task
        before.forEach { target = target.nextOccurrence(zone, today) ?: target }
        val targetAnchor = target.start ?: target.due
        val next: Task?
        if (occurrence != null && targetAnchor != occurrence) {
            next = target
            target = task.atOccurrence(occurrence) ?: task
        } else {
            next = target.nextOccurrence(zone, today)
        }
        if (next == null) {
            // Последний раз серии: закрывается сама задача.
            updateTask(target)
            taskDao.setStatus(taskId, TaskStatus.ABANDONED.value, completedAt = now, now = now)
            return@withTransaction WontDo(
                taskId, previous = task, closedWhole = true, copyIds = copies, occurrence = target.start ?: target.due,
                skipped = if (earlier == EarlierRepeats.SKIP) before.size else 0, completed = if (earlier == EarlierRepeats.COMPLETE) before.size else 0,
            )
        }
        copies += insertClosedCopy(entity, target, TaskStatus.ABANDONED, now)
        // Как при выполнении: у серии чек-лист начинается заново.
        val checked = checklistDao.getByTask(taskId).filter { it.isDone }.map { it.id }
        checked.forEach { checklistDao.setDone(it, done = false, now = now) }
        updateTask(next)
        WontDo(
            taskId, previous = task, closedWhole = false, copyIds = copies, checkedItemIds = checked, occurrence = target.start ?: target.due,
            skipped = if (earlier == EarlierRepeats.SKIP) before.size else 0, completed = if (earlier == EarlierRepeats.COMPLETE) before.size else 0,
        )
    }

    /** Закрытый раз серии — отдельная задача без правила, со сроком раза [occurrence] и копией чек-листа серии. */
    private suspend fun insertClosedCopy(series: TaskEntity, occurrence: Task, status: TaskStatus, now: Long): String {
        val copyId = ids.newId()
        taskDao.insert(
            series.copy(
                id = copyId, status = status.value, completedAt = now, rrule = null, isRepeatDone = true,
                externalId = null, sourceData = null, createdAt = now, updatedAt = now,
            ),
        )
        updateTask(occurrence.copy(id = copyId, rrule = null))
        val items = checklistDao.getByTask(series.id)
        checklistDao.insertAll(items.map { it.copy(id = ids.newId(), taskId = copyId, createdAt = now, updatedAt = now) })
        return copyId
    }

    override suspend fun undoWontDo(result: WontDo) {
        val now = nowMillis()
        db.withTransaction {
            result.copyIds.forEach {
                checklistDao.deleteHardByTask(it)
                taskDao.deleteHard(it)
            }
            updateTask(result.previous)
            if (result.closedWhole) taskDao.setStatus(result.taskId, TaskStatus.ACTIVE.value, completedAt = null, now = now)
            result.checkedItemIds.forEach { checklistDao.setDone(it, done = true, now = now) }
        }
    }

    override fun observeWontDo(): Flow<List<Task>> =
        taskDao.observeWontDo().map { tasks -> tasks.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeWontDoCount(): Flow<Int> = taskDao.observeWontDoCount().distinctUntilChanged()

    override suspend fun moveToTomorrow(taskId: String) {
        val task = getTask(taskId) ?: return
        updateTask(task.withDue(task.due.movedTo(time.today().plusDays(1), time.zone())))
    }

    override suspend fun moveOverdueToToday(): Rescheduling {
        val today = time.today()
        val zone = time.zone()
        val day = DayWindow.of(today, zone)
        val overdue = taskDao.observeOverdue(day.allDayStart, day.start, day.start).first().map { it.toDomain() }.filter { task ->
            val anchor = task.start ?: task.due
            val date = when (anchor) {
                is TaskDue.AllDay -> anchor.date
                is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
                null -> null
            }
            !task.isCompleted && date != null && date < today
        }
        db.withTransaction { overdue.forEach { updateTask(it.withDue(it.due.movedTo(today, zone))) } }
        return Rescheduling(overdue)
    }

    override suspend fun undoReschedule(rescheduling: Rescheduling) {
        db.withTransaction { rescheduling.previous.forEach { updateTask(it) } }
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
        val task = getTask(taskId)?.takeIf { it.status != TaskStatus.ACTIVE } ?: return null
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
            val status = previous.status.takeIf { it != TaskStatus.ACTIVE } ?: TaskStatus.COMPLETED
            taskDao.setStatus(previous.id, status.value, completedAt = previous.completedAt?.toEpochMilli() ?: now, now = now)
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

    override suspend fun deleteTask(taskId: String): Deletion {
        val now = nowMillis()
        db.withTransaction {
            checklistDao.softDeleteByTaskTree(taskId, now)
            taskDao.softDeleteWithSubtasks(taskId, now)
        }
        return Deletion(taskId, now)
    }

    override suspend fun undoDelete(deletion: Deletion) {
        val now = nowMillis()
        db.withTransaction {
            taskDao.restoreWithSubtasks(deletion.taskId, deletion.at, now)
            checklistDao.restoreByTaskTree(deletion.taskId, deletion.at, now)
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

    /** Начало дня [today]: выполненные с этого момента остаются в списках до полуночи (итерация 27). */
    private fun doneSince(today: LocalDate) = today.atStartOfDay(time.zone()).toInstant().toEpochMilli()

    // ---- Корзина (итерация 39, бэклог №35) ----

    override fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrash().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeTrashCount(): Flow<Int> = taskDao.observeTrashCount().distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeTrashed(taskId: String): Flow<TrashedTask?> = taskDao.observeTrashedById(taskId).flatMapLatest { entity ->
        if (entity == null) {
            flowOf(null)
        } else {
            combine(
                checklistDao.observeTrashedByTask(taskId, entity.updatedAt),
                taskDao.observeTrashedSubtasks(taskId, entity.updatedAt),
            ) { items, subtasks ->
                val task = entity.toDomain()
                TrashedTask(TaskDetails(task, items.map { it.toDomain() }), subtasks.map { it.toDomain() }, task.updatedAt)
            }
        }
    }.distinctUntilChanged()

    override suspend fun restoreFromTrash(taskId: String): Restoration? = db.withTransaction {
        val entity = taskDao.getById(taskId)?.takeIf { it.isDeleted } ?: return@withTransaction null
        Restoration(listOf(restoreTree(entity)))
    }

    override suspend fun restoreAllFromTrash(): Restoration = db.withTransaction {
        Restoration(taskDao.getTrash().map { restoreTree(it) })
    }

    /** Возвращает дерево [root]; задача удалённого списка переезжает во «Входящие». */
    private suspend fun restoreTree(root: TaskEntity): Deletion {
        val deletion = Deletion(root.id, root.updatedAt)
        undoDelete(deletion)
        val project = projectDao.getById(root.projectId)
        if (project == null || project.isDeleted) {
            projectDao.getInbox()?.let { taskDao.moveTreeToProject(root.id, root.projectId, it.id, nowMillis()) }
        }
        return deletion
    }

    override suspend fun undoRestoreFromTrash(restoration: Restoration) {
        db.withTransaction {
            restoration.deletions.forEach { deletion ->
                checklistDao.trashByTaskTreeAt(deletion.taskId, deletion.at)
                taskDao.trashTreeAt(deletion.taskId, deletion.at)
            }
        }
    }

    override suspend fun deleteForever(taskId: String) {
        db.withTransaction {
            if (taskDao.getById(taskId)?.isDeleted != true) return@withTransaction
            taskDao.detachLiveChildrenOfTree(taskId)
            checklistDao.deleteHardByTaskTree(taskId)
            taskDao.deleteTrashedTree(taskId)
        }
    }

    override suspend fun emptyTrash(): Int = db.withTransaction {
        val count = taskDao.getTrash().size
        taskDao.detachLiveChildrenOfTrash()
        checklistDao.deleteAllOfTrashedTasks()
        taskDao.deleteAllTrashed()
        count
    }
}

/** Дней в неделе (итерация 44). */
private const val WEEK_DAYS = 7
