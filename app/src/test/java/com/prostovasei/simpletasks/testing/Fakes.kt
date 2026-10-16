package com.prostovasei.simpletasks.testing

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.nextOccurrence
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

fun project(id: String, name: String = id, isInbox: Boolean = false, groupId: String? = null, color: String? = null) =
    Project(id, groupId, name, color, sortOrder = 0, isInbox = isInbox, createdAt = EPOCH, updatedAt = EPOCH)

/** Задачи в памяти; даты сравниваются в UTC. */
class FakeTaskRepository(private val today: LocalDate = LocalDate.of(2026, 10, 14)) : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val completedCalls = mutableListOf<Pair<String, Boolean>>()
    val undone = mutableListOf<Completion>()
    val movedToTomorrow = mutableListOf<String>()

    override fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks> = tasks.map { all ->
        val active = all.filter { it.status == TaskStatus.ACTIVE && it.due != null }
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

    override fun observeInbox(): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.status == TaskStatus.ACTIVE && (it.projectId == "inbox" || it.due == null) } }

    override fun observeProjectTasks(projectId: String): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.status == TaskStatus.ACTIVE && it.projectId == projectId } }

    override fun observeTaskDetails(taskId: String): Flow<TaskDetails?> =
        tasks.map { all -> all.firstOrNull { it.id == taskId }?.let { TaskDetails(it, emptyList()) } }

    override fun observeSubtasks(parentId: String): Flow<List<Task>> =
        tasks.map { all -> all.filter { it.parentId == parentId } }

    val created = mutableListOf<NewTask>()

    override suspend fun createTask(task: NewTask): String {
        created += task
        val id = "new${created.size}"
        tasks.update { it + task(id, title = task.title, projectId = task.projectId, due = task.due, priority = task.priority).copy(content = task.content, rrule = task.rrule, reminders = task.reminders) }
        return id
    }
    val updated = mutableListOf<Task>()

    override suspend fun updateTask(task: Task) {
        updated += task
        tasks.update { all -> all.map { if (it.id == task.id) task else it } }
    }

    val restored = mutableListOf<String>()

    override fun observeCompleted(): Flow<List<Task>> = tasks.map { all -> all.filter { it.status == TaskStatus.COMPLETED } }

    override suspend fun restore(taskId: String): Task? {
        val task = tasks.value.firstOrNull { it.id == taskId && it.status == TaskStatus.COMPLETED } ?: return null
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
        tasks.update { all -> all.map { if (it.id == taskId) it.copy(status = status) else it } }
    }

    override suspend fun deleteTask(taskId: String) {
        tasks.update { all -> all.filterNot { it.id == taskId } }
    }

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
        tasks.update { all -> all.map { if (it.id == taskId) next else it } }
        return Completion.Rescheduled(taskId, previous = task, next = next, doneCopyId = "done-$taskId", checkedItemIds = emptyList())
    }

    override suspend fun undo(completion: Completion) {
        undone += completion
        when (completion) {
            is Completion.Done -> setCompleted(completion.taskId, completed = false)
            is Completion.Rescheduled -> tasks.update { all -> all.map { if (it.id == completion.taskId) completion.previous else it } }
        }
    }

    override suspend fun moveToTomorrow(taskId: String) {
        movedToTomorrow += taskId
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

    override suspend fun createGroup(name: String): String {
        val id = "g${nextId++}"
        calls += "createGroup:$name"
        tree.update { it.copy(groups = it.groups + GroupWithProjects(ProjectGroup(id, name, 0, EPOCH, EPOCH), emptyList())) }
        return id
    }

    override suspend fun renameGroup(groupId: String, name: String) {
        calls += "renameGroup:$groupId:$name"
    }

    override suspend fun deleteGroup(groupId: String) {
        calls += "deleteGroup:$groupId"
    }

    override suspend fun createProject(name: String, groupId: String?, color: String?): String {
        val id = "p${nextId++}"
        calls += "createProject:$name:$groupId"
        tree.update { it.copy(ungrouped = it.ungrouped + ProjectWithCount(project(id, name, groupId = groupId, color = color), 0)) }
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
