package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    /** Секции экрана «Все» относительно дня [today] в текущем часовом поясе. Задачи с интервалом — по времени начала. */
    fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks>

    /** «Входящие»: задачи системного списка и все задачи без срока (SPEC.md §4.2). */
    fun observeInbox(): Flow<List<Task>>

    fun observeProjectTasks(projectId: String): Flow<List<Task>>

    fun observeTaskDetails(taskId: String): Flow<TaskDetails?>

    /** Подзадачи (активные сначала, затем выполненные). */
    fun observeSubtasks(parentId: String): Flow<List<Task>>

    /** Создаёт задачу вместе с пунктами чек-листа. Возвращает id задачи. */
    suspend fun createTask(task: NewTask): String

    /** Сохраняет редактируемые поля: список, название, описание, приоритет, срок и начало, повтор, напоминания, родителя, порядок. */
    suspend fun updateTask(task: Task)

    suspend fun setCompleted(taskId: String, completed: Boolean)

    /** Мягкое удаление задачи вместе с её подзадачами и их чек-листами. */
    suspend fun deleteTask(taskId: String)
}
