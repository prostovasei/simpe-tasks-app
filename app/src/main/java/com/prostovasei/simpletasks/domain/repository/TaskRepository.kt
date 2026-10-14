package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
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

    suspend fun getTask(taskId: String): Task?

    /** Активные задачи со сроком и напоминаниями — для расписания уведомлений. */
    fun observeWithReminders(): Flow<List<Task>>

    /**
     * Отмечает задачу выполненной. Повторяющаяся задача переносится на следующий раз,
     * выполненный раз сохраняется отдельной задачей (см. [Completion.Rescheduled]).
     */
    suspend fun complete(taskId: String): Completion

    /** Отменяет отметку: возвращает задачу в активные или повторяющуюся — на прежний срок. */
    suspend fun undo(completion: Completion)

    /** Снимает отметку «выполнена» (без переносов). */
    suspend fun setCompleted(taskId: String, completed: Boolean)

    /** «На завтра» из уведомления: срок — завтра, время дня то же. */
    suspend fun moveToTomorrow(taskId: String)

    /** Мягкое удаление задачи вместе с её подзадачами и их чек-листами. */
    suspend fun deleteTask(taskId: String)
}
