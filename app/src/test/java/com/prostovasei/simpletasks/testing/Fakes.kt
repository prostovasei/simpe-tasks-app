package com.prostovasei.simpletasks.testing

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.nextOccurrence
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.Moved
import com.prostovasei.simpletasks.domain.model.SortCounts
import com.prostovasei.simpletasks.domain.model.WontDo
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.model.Restoration
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.domain.model.withDue
import com.prostovasei.simpletasks.domain.model.movedTo
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.WeekTasks
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val EPOCH = Instant.parse("2026-01-01T00:00:00Z")

fun task(
    id: String,
    title: String = id,
    projectId: String = "inbox",
    due: TaskDue? = null,
    start: TaskDue? = null,
    priority: Priority = Priority.NONE,
    status: TaskStatus = TaskStatus.ACTIVE,
) = Task(
    id = id, projectId = projectId, title = title, content = "", status = status, priority = priority, due = due,
    start = start, completedAt = null, sortOrder = 0, rrule = null, reminders = emptyList(), parentId = null,
    createdAt = EPOCH, updatedAt = EPOCH,
)

fun project(id: String, name: String = id, isInbox: Boolean = false, groupId: String? = null, color: String? = null, icon: String? = null, emoji: String? = null) =
    Project(id, groupId, name, color, sortOrder = 0, isInbox = isInbox, createdAt = EPOCH, updatedAt = EPOCH, icon = icon, emoji = emoji)

/** Задачи в памяти; даты сравниваются в UTC. */
class FakeTaskRepository(private val today: LocalDate = LocalDate.of(2026, 10, 14)) : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val completedCalls = mutableListOf<Pair<String, Boolean>>()
    val undone = mutableListOf<Completion>()
    val movedToTomorrow = mutableListOf<String>()

    override fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks> = tasks.map { all ->
        val active = all.filter { it.shownOn(today) && it.due != null }
        fun TaskDue.date() = when (this) {
            is TaskDue.AllDay -> date
            is TaskDue.AtTime -> instant.atZone(ZoneOffset.UTC).toLocalDate()
        }
        AllScreenTasks(
            overdue = active.filter { it.due!!.date() < today },
            todayAllDay = active.filter { (it.due as? TaskDue.AllDay)?.date == today },
            todayTimed = active.filter { val due = it.due; due is TaskDue.AtTime && due.date() == today },
            upcoming = active.filter { it.due!!.date() > today },
        )
    }

    /** Неделя из семи дней: как в БД — активные и выполненные за любой день, плюс активные повторяющиеся. */
    override fun observeWeek(weekStart: LocalDate): Flow<WeekTasks> = tasks.map { all ->
        fun Task.day(): LocalDate? = when (val anchor = start ?: due) {
            is TaskDue.AllDay -> anchor.date
            is TaskDue.AtTime -> anchor.instant.atZone(ZoneOffset.UTC).toLocalDate()
            null -> null
        }
        WeekTasks(
            inRange = all.filter { it.status != TaskStatus.ABANDONED && it.due != null && it.day()?.let { d -> d >= weekStart && d < weekStart.plusDays(7) } == true },
            repeating = all.filter { it.status == TaskStatus.ACTIVE && it.rrule != null && it.due != null },
        )
    }

    override fun observeInbox(today: LocalDate): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.shownOn(today) && it.projectId == "inbox" && it.due == null } }

    override fun observeSomeday(today: LocalDate): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.shownOn(today) && it.projectId != "inbox" && it.due == null } }

    override fun observeNoList(today: LocalDate): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.shownOn(today) && it.projectId == "inbox" && it.due != null } }

    override fun observeSortCounts(): Flow<SortCounts> = tasks.map { all ->
        val open = all.filter { it.status == TaskStatus.ACTIVE }
        SortCounts(
            inbox = open.count { it.projectId == "inbox" && it.due == null },
            someday = open.count { it.projectId != "inbox" && it.due == null },
            noList = open.count { it.projectId == "inbox" && it.due != null },
        )
    }

    override fun observeProjectTasks(projectId: String, today: LocalDate): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.shownOn(today) && it.projectId == projectId } }

    /** Как в БД: активные и выполненные сегодня (итерация 27). */
    private fun Task.shownOn(today: LocalDate): Boolean =
        status == TaskStatus.ACTIVE || (status == TaskStatus.COMPLETED && completedAt?.atZone(ZoneOffset.UTC)?.toLocalDate()?.let { it >= today } == true)

    override fun observeTaskDetails(taskId: String): Flow<TaskDetails?> =
        tasks.map { all -> all.firstOrNull { it.id == taskId }?.let { TaskDetails(it, emptyList()) } }

    override fun observeSubtasks(parentId: String): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.parentId == parentId } }

    val created = mutableListOf<NewTask>()

    override suspend fun createTask(task: NewTask): String {
        created += task
        val id = "new${created.size}"
        tasks.update { it + task(id, title = task.title, projectId = task.projectId, due = task.due, start = task.start, priority = task.priority).copy(content = task.content, rrule = task.rrule, reminders = task.reminders) }
        return id
    }
    val updated = mutableListOf<Task>()

    override suspend fun updateTask(task: Task) {
        updated += task
        tasks.update { all -> all.map { if (it.id == task.id) task else it } }
    }

    val restored = mutableListOf<String>()

    /** Пункты чек-листа для поиска: id задачи → пункты. */
    val checklists = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override fun observeSearchable(): Flow<List<SearchableTask>> =
        combine(tasks, checklists) { all, lists -> all.map { SearchableTask(it, lists[it.id].orEmpty()) } }

    override fun observeTimedInRange(from: Instant, to: Instant): Flow<List<Task>> = tasks.map { all ->
        all.filter { task ->
            val end = (task.due as? TaskDue.AtTime)?.instant ?: return@filter false
            val begin = (task.start as? TaskDue.AtTime)?.instant ?: end
            (begin < to && end >= from) || (task.rrule != null && task.status == TaskStatus.ACTIVE)
        }
    }

    override fun observeCompleted(): Flow<List<Task>> = tasks.map { all -> all.filter { it.status == TaskStatus.COMPLETED } }

    override suspend fun restore(taskId: String): Task? {
        val task = tasks.value.firstOrNull { it.id == taskId && it.status != TaskStatus.ACTIVE } ?: return null
        restored += taskId
        tasks.update { all -> all.map { if (it.id == taskId) it.copy(status = TaskStatus.ACTIVE, rrule = null, completedAt = null) else it } }
        return task
    }

    override suspend fun undoRestore(previous: Task) {
        tasks.update { all -> all.map { if (it.id == previous.id) previous else it } }
    }

    override suspend fun setCompleted(taskId: String, completed: Boolean) {
        completedCalls += taskId to completed
        val status = if (completed) TaskStatus.COMPLETED else TaskStatus.ACTIVE
        val at = if (completed) doneAt() else null
        tasks.update { all -> all.map { if (it.id == taskId) it.copy(status = status, completedAt = at) else it } }
    }

    /** Удалённые и ещё не возвращённые задачи — для проверки «Отмены». */
    val deleted = mutableMapOf<Deletion, Task>()
    private var deletions = 0L

    override suspend fun deleteTask(taskId: String): Deletion {
        val deletion = Deletion(taskId, ++deletions)
        tasks.value.firstOrNull { it.id == taskId }?.let {
            deleted[deletion] = it
            trash.update { all -> all + it.copy(updatedAt = Instant.ofEpochMilli(deletion.at)) }
        }
        tasks.update { all -> all.filterNot { it.id == taskId } }
        return deletion
    }

    override suspend fun moveOverdueToToday(): Rescheduling {
        val overdue = tasks.value.filter { task ->
            val anchor = task.start ?: task.due
            !task.isCompleted && anchor is TaskDue.AllDay && anchor.date < today
        }
        tasks.update { all -> all.map { task -> if (task in overdue) task.withDue(TaskDue.AllDay(today)) else task } }
        return Rescheduling(overdue)
    }

    override suspend fun moveTasks(ids: List<String>, date: LocalDate): Rescheduling {
        val moved = tasks.value.filter { it.id in ids }
        tasks.update { all -> all.map { task -> if (task.id in ids) task.withDue(task.due.movedTo(date, ZoneOffset.UTC)) else task } }
        return Rescheduling(moved)
    }

    override suspend fun undoReschedule(rescheduling: Rescheduling) {
        val back = rescheduling.previous.associateBy { it.id }
        tasks.update { all -> all.map { back[it.id] ?: it } }
    }

    override suspend fun undoDelete(deletion: Deletion) {
        val task = deleted.remove(deletion) ?: return
        trash.update { all -> all.filterNot { it.id == task.id } }
        tasks.update { all -> all + task }
    }

    // ---- Корзина (итерация 39) ----

    val trash = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTrash(): Flow<List<Task>> = trash

    override fun observeTrashCount(): Flow<Int> = trash.map { it.size }

    override fun observeTrashed(taskId: String): Flow<TrashedTask?> = trash.map { list ->
        list.firstOrNull { it.id == taskId }?.let { TrashedTask(TaskDetails(it, emptyList()), emptyList(), it.updatedAt) }
    }

    override suspend fun restoreFromTrash(taskId: String): Restoration? {
        val task = trash.value.firstOrNull { it.id == taskId } ?: return null
        trash.update { all -> all.filterNot { it.id == taskId } }
        tasks.update { all -> all + task }
        return Restoration(listOf(Deletion(taskId, task.updatedAt.toEpochMilli())))
    }

    override suspend fun restoreAllFromTrash(): Restoration =
        Restoration(trash.value.mapNotNull { restoreFromTrash(it.id)?.deletions?.single() })

    override suspend fun undoRestoreFromTrash(restoration: Restoration) {
        restoration.deletions.forEach { deletion ->
            val task = tasks.value.firstOrNull { it.id == deletion.taskId } ?: return@forEach
            tasks.update { all -> all.filterNot { it.id == task.id } }
            trash.update { all -> all + task }
        }
    }

    override suspend fun deleteForever(taskId: String) {
        trash.update { all -> all.filterNot { it.id == taskId } }
    }

    override suspend fun emptyTrash(): Int = trash.value.size.also { trash.value = emptyList() }

    override suspend fun getTask(taskId: String): Task? = tasks.value.firstOrNull { it.id == taskId }

    override fun observeDataChanges(): Flow<Unit> = tasks.map { }

    override fun observeWithReminders(): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.status == TaskStatus.ACTIVE && it.due != null && it.reminders.isNotEmpty() } }

    /** Повтор считается по UTC от [today]. */
    override suspend fun complete(taskId: String): Completion {
        val task = getTask(taskId) ?: return Completion.Done(taskId)
        val next = task.nextOccurrence(ZoneOffset.UTC, today)
        if (next == null) {
            setCompleted(taskId, completed = true)
            return Completion.Done(taskId)
        }
        completedCalls += taskId to true
        // Как в БД: выполненный раз — отдельная задача со старым сроком, остаётся в списке до полуночи.
        val copy = task.copy(id = "done-$taskId", status = TaskStatus.COMPLETED, completedAt = doneAt(), rrule = null)
        tasks.update { all -> all.map { if (it.id == taskId) next else it } + copy }
        return Completion.Rescheduled(taskId, previous = task, next = next, doneCopyId = copy.id, checkedItemIds = emptyList())
    }

    override suspend fun undo(completion: Completion) {
        undone += completion
        when (completion) {
            is Completion.Done -> setCompleted(completion.taskId, completed = false)
            is Completion.Rescheduled -> tasks.update { all ->
                all.filterNot { it.id == completion.doneCopyId }.map { if (it.id == completion.taskId) completion.previous else it }
            }
        }
    }

    /** Вызовы «Не буду делать»: задача, раз (null — ближайший) и что сделать с повторами перед ним. */
    val wontDoCalls = mutableListOf<Triple<String, TaskDue?, EarlierRepeats>>()
    val undoneWontDo = mutableListOf<WontDo>()

    override suspend fun wontDo(taskId: String, occurrence: TaskDue?, earlier: EarlierRepeats): WontDo? {
        val task = getTask(taskId)?.takeIf { it.status == TaskStatus.ACTIVE } ?: return null
        wontDoCalls += Triple(taskId, occurrence, earlier)
        val next = task.nextOccurrence(ZoneOffset.UTC, today)
        if (task.rrule == null || next == null) {
            tasks.update { all -> all.map { if (it.id == taskId) it.copy(status = TaskStatus.ABANDONED, completedAt = doneAt()) else it } }
            return WontDo(taskId, task, closedWhole = true, occurrence = task.start ?: task.due)
        }
        val copy = task.copy(id = "wont-$taskId", status = TaskStatus.ABANDONED, completedAt = doneAt(), rrule = null, isRepeatDone = true)
        tasks.update { all -> all.map { if (it.id == taskId) next else it } + copy }
        return WontDo(taskId, task, closedWhole = false, copyIds = listOf(copy.id), occurrence = occurrence ?: task.start ?: task.due)
    }

    override suspend fun undoWontDo(result: WontDo) {
        undoneWontDo += result
        tasks.update { all -> all.filterNot { it.id in result.copyIds }.map { if (it.id == result.taskId) result.previous else it } }
    }

    override fun observeWontDo(): Flow<List<Task>> = tasks.map { all -> all.filter { it.status == TaskStatus.ABANDONED } }

    override fun observeWontDoCount(): Flow<Int> = observeWontDo().map { it.size }

    /** Время выполнения в фейке — полдень [today] по UTC. */
    private fun doneAt(): Instant = today.atStartOfDay(ZoneOffset.UTC).plusHours(12).toInstant()

    override suspend fun moveToTomorrow(taskId: String): Moved? {
        movedToTomorrow += taskId
        return null
    }

    /** Вызовы «Отложить» (итерация 53): id и новый срок. */
    val snoozed = mutableListOf<Pair<String, TaskDue>>()

    override suspend fun snooze(taskId: String, due: TaskDue, start: TaskDue?): Moved? {
        snoozed += taskId to due
        val previous = tasks.value.firstOrNull { it.id == taskId } ?: return null
        tasks.update { all -> all.map { if (it.id == taskId) it.copy(due = due, start = start) else it } }
        return Moved(taskId, previous = previous, day = start ?: due)
    }

    /** Вызовы «Текущий повтор»: id и новый срок. */
    val movedRepeats = mutableListOf<Pair<String, TaskDue>>()

    /** Вызовы «Все повторы с этого момента» у будущего раза: id, дата раза и новый срок. */
    val movedRepeatsFrom = mutableListOf<Triple<String, TaskDue, TaskDue?>>()
    val undoneMoves = mutableListOf<Moved>()

    override suspend fun moveRepeat(taskId: String, due: TaskDue, start: TaskDue?): Moved? {
        movedRepeats += taskId to due
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return null
        val next = task.nextOccurrence(ZoneOffset.UTC, LocalDate.MIN) ?: task
        tasks.update { all -> all.map { if (it.id == taskId) next else it } }
        return Moved(taskId, previous = task, day = due)
    }

    override suspend fun moveRepeatsFrom(taskId: String, occurrence: TaskDue, due: TaskDue?, start: TaskDue?): Moved? {
        movedRepeatsFrom += Triple(taskId, occurrence, due)
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return null
        return Moved(taskId, previous = task, day = due)
    }

    override suspend fun undoMove(moved: Moved) {
        undoneMoves += moved
        tasks.update { all -> all.map { if (it.id == moved.taskId) moved.previous else it } }
    }
}

/** Списки и папки в памяти. */
class FakeProjectRepository : ProjectRepository {
    val tree = MutableStateFlow(ProjectTree(inbox = ProjectWithCount(project("inbox", "Inbox", isInbox = true), 0), groups = emptyList(), ungrouped = emptyList()))
    val calls = mutableListOf<String>()
    private var nextId = 1

    private fun all() = tree.value.let { listOfNotNull(it.inbox) + it.groups.flatMap { g -> g.projects } + it.ungrouped }

    override fun observeProjectTree(): Flow<ProjectTree> = tree
    override fun observeProject(projectId: String): Flow<Project?> =
        tree.map { all().firstOrNull { it.project.id == projectId }?.project }

    override suspend fun getInbox(): Project = tree.value.inbox!!.project

    override suspend fun createGroup(name: String, emoji: String?): String {
        val id = "g${nextId++}"
        calls += if (emoji == null) "createGroup:$name" else "createGroup:$name:$emoji"
        tree.update { it.copy(groups = it.groups + GroupWithProjects(ProjectGroup(id, name, 0, EPOCH, EPOCH, emoji), emptyList())) }
        return id
    }

    override suspend fun updateGroup(groupId: String, name: String, emoji: String?) {
        calls += "updateGroup:$groupId:$name:$emoji"
    }

    override suspend fun deleteGroup(groupId: String) {
        calls += "deleteGroup:$groupId"
    }

    override suspend fun createProject(name: String, groupId: String?, color: String?, emoji: String?): String {
        val id = "p${nextId++}"
        calls += if (emoji == null) "createProject:$name:$groupId" else "createProject:$name:$groupId:$emoji"
        tree.update { it.copy(ungrouped = it.ungrouped + ProjectWithCount(project(id, name, groupId = groupId, color = color).copy(emoji = emoji), 0)) }
        return id
    }

    override suspend fun updateProject(projectId: String, name: String, groupId: String?, color: String?) {
        calls += "updateProject:$projectId:$name:$groupId:$color"
        tree.update { t ->
            t.copy(ungrouped = t.ungrouped.map {
                if (it.project.id == projectId) it.copy(project = it.project.copy(name = name, groupId = groupId, color = color)) else it
            })
        }
    }

    override suspend fun setProjectEmoji(projectId: String, emoji: String?) {
        calls += "emoji:$projectId:$emoji"
    }

    override suspend fun deleteProject(projectId: String) {
        calls += "deleteProject:$projectId"
        tree.update { t -> t.copy(ungrouped = t.ungrouped.filterNot { it.project.id == projectId }) }
    }
}

/** Пункты чек-листа в памяти; изменения видны через [FakeTaskRepository.observeTaskDetails] не нужны — тесты читают [items]. */
class FakeChecklistRepository : com.prostovasei.simpletasks.domain.repository.ChecklistRepository {
    val calls = mutableListOf<String>()
    override suspend fun addItem(taskId: String, title: String): String { calls += "add:$taskId:$title"; return "i${calls.size}" }
    override suspend fun renameItem(itemId: String, title: String) { calls += "rename:$itemId:$title" }
    override suspend fun setDone(itemId: String, done: Boolean) { calls += "done:$itemId:$done" }
    override suspend fun deleteItem(itemId: String) { calls += "delete:$itemId" }
    override suspend fun reorder(taskId: String, orderedItemIds: List<String>) { calls += "reorder:$taskId:${orderedItemIds.joinToString(",")}" }
}

/** Финансы в памяти (#221, #225): счета, операции (остатки пересчитываются), категории и подсказки. */
class FakeFinanceRepository : com.prostovasei.simpletasks.domain.repository.FinanceRepository {
    val accounts = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.Account>>(emptyList())
    val operations = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.Operation>>(emptyList())
    val categories = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.Category>>(emptyList())
    val hints = MutableStateFlow(com.prostovasei.simpletasks.domain.model.finance.FormHints())
    val calls = mutableListOf<String>()

    override fun observeAccounts(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.Account>> = accounts
    override fun observeOperations(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.Operation>> = operations
    override fun observeCategories(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.Category>> = categories
    override fun observeFormHints(): Flow<com.prostovasei.simpletasks.domain.model.finance.FormHints> = hints

    override suspend fun createAccount(draft: com.prostovasei.simpletasks.domain.model.finance.AccountDraft): String {
        val id = "a${accounts.value.size + 1}"
        calls += "create:${draft.name}:${draft.type}:${draft.emoji}:${draft.balance}"
        accounts.value += account(id, draft.name, draft.balance, draft.type, draft.emoji)
        return id
    }

    override suspend fun updateAccount(id: String, draft: com.prostovasei.simpletasks.domain.model.finance.AccountDraft) {
        calls += "update:$id:${draft.name}:${draft.type}:${draft.emoji}:${draft.balance}"
        accounts.value = accounts.value.map { if (it.id == id) account(id, draft.name, draft.balance, draft.type, draft.emoji) else it }
    }

    private fun name(accountId: String?) = accounts.value.firstOrNull { it.id == accountId }?.name

    private fun com.prostovasei.simpletasks.domain.model.finance.OperationDraft.toOperation(id: String) = com.prostovasei.simpletasks.domain.model.finance.Operation(
        id, kind, amount, accountId, name(accountId).orEmpty(), toAccountId, name(toAccountId), categoryId, categories.value.firstOrNull { it.id == categoryId }?.name,
        place?.takeIf { it.isNotBlank() }, comment?.takeIf { it.isNotBlank() }, date,
    )

    override suspend fun createOperation(draft: com.prostovasei.simpletasks.domain.model.finance.OperationDraft): String {
        val id = "o${operations.value.size + 1}"
        calls += "operation:${draft.kind}:${draft.amount}:${draft.accountId}:${draft.toAccountId}:${draft.categoryId}:${draft.place}:${draft.comment}:${draft.date}"
        operations.value = listOf(draft.toOperation(id)) + operations.value
        return id
    }

    override suspend fun updateOperation(id: String, draft: com.prostovasei.simpletasks.domain.model.finance.OperationDraft) {
        calls += "updateOperation:$id:${draft.kind}:${draft.amount}:${draft.accountId}:${draft.categoryId}:${draft.date}"
        operations.value = operations.value.map { if (it.id == id) draft.toOperation(id) else it }
    }

    val trash = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.Operation>>(emptyList())
    var backupJson = "{backup}"
    var backupCounts: com.prostovasei.simpletasks.domain.model.finance.FinanceCounts? = com.prostovasei.simpletasks.domain.model.finance.FinanceCounts(3, 13, 214)

    override suspend fun trashOperation(id: String) {
        calls += "trash:$id"
        trash.value = operations.value.filter { it.id == id } + trash.value
        operations.value = operations.value.filterNot { it.id == id }
    }

    override fun observeTrash(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.Operation>> = trash

    override suspend fun restoreOperation(id: String) {
        calls += "restore:$id"
        operations.value = trash.value.filter { it.id == id } + operations.value
        trash.value = trash.value.filterNot { it.id == id }
    }

    override suspend fun deleteOperationForever(id: String) {
        calls += "deleteForever:$id"
        trash.value = trash.value.filterNot { it.id == id }
    }

    override suspend fun emptyTrash(): Int {
        calls += "emptyTrash"
        return trash.value.size.also { trash.value = emptyList() }
    }

    override suspend fun setInitialBalance(id: String, initialBalance: Long) { calls += "initial:$id:$initialBalance" }

    override suspend fun correctBalance(id: String, realBalance: Long, date: java.time.LocalDate): Long {
        calls += "correct:$id:$realBalance:$date"
        return 0
    }

    override fun observeCounts(): Flow<com.prostovasei.simpletasks.domain.model.finance.FinanceCounts> = MutableStateFlow(com.prostovasei.simpletasks.domain.model.finance.FinanceCounts(accounts.value.size, categories.value.size, operations.value.size))

    override suspend fun exportBackup(): String = backupJson.also { calls += "export" }

    override fun inspectBackup(json: String): com.prostovasei.simpletasks.domain.model.finance.FinanceCounts = backupCounts ?: throw com.prostovasei.simpletasks.domain.model.finance.InvalidFinanceBackupException("плохой файл")

    override suspend fun restoreBackup(json: String) {
        if (backupCounts == null) throw com.prostovasei.simpletasks.domain.model.finance.InvalidFinanceBackupException("плохой файл")
        calls += "restoreBackup:$json"
    }

    override suspend fun createCategory(name: String, kind: com.prostovasei.simpletasks.domain.model.finance.OperationKind, emoji: String?): String {
        val id = "c${categories.value.size + 1}"
        calls += if (emoji == null) "category:$name:$kind" else "category:$name:$kind:$emoji"
        categories.value += com.prostovasei.simpletasks.domain.model.finance.Category(id, name, kind, archived = false, emoji = emoji)
        return id
    }

    override suspend fun updateCategory(id: String, name: String, emoji: String?) {
        calls += if (emoji == null) "update:$id:$name" else "update:$id:$name:$emoji"
        categories.value = categories.value.map { if (it.id == id) it.copy(name = name, emoji = emoji) else it }
    }

    override suspend fun setCategoryArchived(id: String, archived: Boolean) {
        calls += "archive:$id:$archived"
        categories.value = categories.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }

    override suspend fun deleteCategoryForever(id: String) {
        calls += "deleteCategory:$id"
        categories.value = categories.value.filterNot { it.id == id }
    }

    val places = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.Place>>(emptyList())

    override fun observePlaces(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.Place>> = places

    override suspend fun createPlace(name: String): String {
        val id = "p${places.value.size + 1}"
        calls += "place:$name"
        places.value += com.prostovasei.simpletasks.domain.model.finance.Place(id, name, archived = false)
        return id
    }

    override suspend fun renamePlace(id: String, name: String) {
        calls += "renamePlace:$id:$name"
        places.value = places.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun setPlaceArchived(id: String, archived: Boolean) {
        calls += "archivePlace:$id:$archived"
        places.value = places.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }

    override suspend fun deletePlaceForever(id: String) {
        calls += "deletePlace:$id"
        places.value = places.value.filterNot { it.id == id }
    }
}

fun account(
    id: String,
    name: String,
    balance: Long,
    type: com.prostovasei.simpletasks.domain.model.finance.AccountType = com.prostovasei.simpletasks.domain.model.finance.AccountType.CARD,
    emoji: String? = null,
) = com.prostovasei.simpletasks.domain.model.finance.Account(id, name, type, emoji, balance, balance, java.time.Instant.parse("2026-11-01T09:00:00Z"))

fun category(id: String, name: String, kind: com.prostovasei.simpletasks.domain.model.finance.OperationKind = com.prostovasei.simpletasks.domain.model.finance.OperationKind.EXPENSE, archived: Boolean = false, emoji: String? = null) =
    com.prostovasei.simpletasks.domain.model.finance.Category(id, name, kind, archived, emoji)

fun place(id: String, name: String, archived: Boolean = false, uses: Int = 0) = com.prostovasei.simpletasks.domain.model.finance.Place(id, name, archived, uses)

fun operation(
    id: String,
    amount: Long,
    date: String,
    kind: com.prostovasei.simpletasks.domain.model.finance.OperationKind = com.prostovasei.simpletasks.domain.model.finance.OperationKind.EXPENSE,
    category: String? = null,
    account: String = "Карта",
    to: String? = null,
    place: String? = null,
    comment: String? = null,
    categoryEmoji: String? = null,
) = com.prostovasei.simpletasks.domain.model.finance.Operation(
    id, kind, amount, "a1", account, to?.let { "a2" }, to, category?.let { "c-$it" }, category, place, comment, java.time.LocalDate.parse(date),
    categoryEmoji = categoryEmoji, accountEmoji = "💳", toAccountEmoji = to?.let { "👛" },
)

/** Покупки из уведомлений банка (#233) для тестов экрана: всё в памяти, вызовы записываются в [calls]. */
class FakeBankPurchases : com.prostovasei.simpletasks.domain.repository.BankPurchases {
    val settings = MutableStateFlow(com.prostovasei.simpletasks.domain.model.finance.BankSettings())
    val pending = MutableStateFlow<List<com.prostovasei.simpletasks.domain.model.finance.PendingPurchase>>(emptyList())
    var access = false
    val calls = mutableListOf<String>()
    val recorded = mutableListOf<com.prostovasei.simpletasks.domain.model.finance.OperationDraft>()

    override fun observeSettings(): Flow<com.prostovasei.simpletasks.domain.model.finance.BankSettings> = settings
    override fun hasAccess(): Boolean = access
    override suspend fun setEnabled(enabled: Boolean) {
        settings.value = settings.value.copy(enabled = enabled)
    }
    override suspend fun setAccount(accountId: String) {
        settings.value = settings.value.copy(accountId = accountId)
    }
    override fun observePending(): Flow<List<com.prostovasei.simpletasks.domain.model.finance.PendingPurchase>> = pending
    override suspend fun onPurchase(key: String, amount: Long, at: java.time.Instant) = null
    override suspend fun record(id: String) {
        calls += "record:$id"
        pending.value = pending.value.filterNot { it.id == id }
    }
    override suspend fun recordAs(id: String, draft: com.prostovasei.simpletasks.domain.model.finance.OperationDraft) {
        calls += "recordAs:$id"
        recorded += draft
        pending.value = pending.value.filterNot { it.id == id }
    }
    override suspend fun skip(id: String) {
        calls += "skip:$id"
        pending.value = pending.value.filterNot { it.id == id }
    }
}
