package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface TaskRepository {
    /**
     * Секции экрана «Все» относительно дня [today] в текущем часовом поясе. Задачи с интервалом — по времени начала.
     * Здесь и во «Входящих» и списках — активные и выполненные сегодня (до полуночи, итерация 27).
     */
    fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks>

    /** «Входящие»: задачи системного списка и все задачи без срока (SPEC.md §4.2). */
    fun observeInbox(today: LocalDate): Flow<List<Task>>

    fun observeProjectTasks(projectId: String, today: LocalDate): Flow<List<Task>>

    fun observeTaskDetails(taskId: String): Flow<TaskDetails?>

    /** Подзадачи (активные сначала, затем выполненные). */
    fun observeSubtasks(parentId: String): Flow<List<Task>>

    /** Создаёт задачу вместе с пунктами чек-листа. Возвращает id задачи. */
    suspend fun createTask(task: NewTask): String

    /** Сохраняет редактируемые поля: список, название, описание, приоритет, срок и начало, повтор, напоминания, родителя, порядок. */
    suspend fun updateTask(task: Task)

    suspend fun getTask(taskId: String): Task?

    /** Сигнал о любом изменении задач или списков (для виджета). */
    fun observeDataChanges(): Flow<Unit>

    /** Активные задачи со сроком и напоминаниями — для расписания уведомлений. */
    fun observeWithReminders(): Flow<List<Task>>

    /**
     * Отмечает задачу выполненной. Повторяющаяся задача переносится на следующий раз,
     * выполненный раз сохраняется отдельной задачей (см. [Completion.Rescheduled]).
     */
    suspend fun complete(taskId: String): Completion

    /** Отменяет отметку: возвращает задачу в активные или повторяющуюся — на прежний срок. */
    suspend fun undo(completion: Completion)

    /**
     * Задачи со временем, идущие в промежутке [from, to), и все активные повторяющиеся со временем —
     * для сетки недели в окне новой задачи (итерация 16).
     */
    fun observeTimedInRange(from: Instant, to: Instant): Flow<List<Task>>

    /** Все задачи, кроме удалённых, с пунктами чек-листа — для поиска (итерация 15). */
    fun observeSearchable(): Flow<List<SearchableTask>>

    /** Выполненные задачи, новые сверху (экран «Выполненные», итерация 11). */
    fun observeCompleted(): Flow<List<Task>>

    /**
     * Возвращает выполненную задачу в активные. Выполненный раз повторяющейся задачи возвращается обычной
     * задачей без повтора — серия не меняется (решение PM). Возвращает задачу до изменения — для «Отмены».
     */
    suspend fun restore(taskId: String): Task?

    /** «Отмена» возврата: задача снова выполнена, с прежним временем выполнения и повтором. */
    suspend fun undoRestore(previous: Task)

    /** Снимает отметку «выполнена» (без переносов). */
    suspend fun setCompleted(taskId: String, completed: Boolean)

    /** «На завтра» из уведомления: срок — завтра, время дня то же. */
    suspend fun moveToTomorrow(taskId: String)

    /**
     * «Перенести на сегодня» (итерация 37): все невыполненные задачи прошедших дней — на сегодня, время дня то же.
     * Результат нужен для [undoReschedule]; пустой, если переносить нечего.
     */
    suspend fun moveOverdueToToday(): Rescheduling

    /** «Отмена» переноса: задачи снова такие, какими были до него. */
    suspend fun undoReschedule(rescheduling: Rescheduling)

    /** Мягкое удаление задачи вместе с её подзадачами и их чек-листами; результат нужен для [undoDelete]. */
    suspend fun deleteTask(taskId: String): Deletion

    /** «Отмена» удаления: возвращает задачу, подзадачи и чек-листы, удалённые именно этим [deletion]. */
    suspend fun undoDelete(deletion: Deletion)
}
