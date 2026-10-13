package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    /** Секции экрана «Все» относительно дня [today] в текущем часовом поясе. */
    fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks>

    /** «Входящие»: задачи системного списка и все задачи без срока (SPEC.md §4.2). */
    fun observeInbox(): Flow<List<Task>>

    fun observeProjectTasks(projectId: String): Flow<List<Task>>

    fun observeTaskDetails(taskId: String): Flow<TaskDetails?>

    /** Создаёт задачу вместе с пунктами чек-листа. Возвращает id задачи. */
    suspend fun createTask(task: NewTask): String

    /**
     * Создаёт задачу из импорта. Возвращает false, если задача с таким [externalId]
     * уже импортировалась (в том числе удалённая) — защита от дублей.
     */
    suspend fun createImportedTask(task: NewTask, externalId: String): Boolean

    /** Сохраняет редактируемые поля: список, название, описание, приоритет, срок, повтор, порядок. */
    suspend fun updateTask(task: Task)

    suspend fun setCompleted(taskId: String, completed: Boolean)

    /** Мягкое удаление задачи вместе с её чек-листом. */
    suspend fun deleteTask(taskId: String)
}
