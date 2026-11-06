package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.Rescheduling
import com.prostovasei.simpletasks.domain.model.Restoration
import com.prostovasei.simpletasks.domain.model.TrashedTask
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDetails
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.WontDo
import com.prostovasei.simpletasks.domain.model.WeekTasks
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface TaskRepository {
    /**
     * Секции экрана «Все» относительно дня [today] в текущем часовом поясе. Задачи с интервалом — по времени начала.
     * Здесь и во «Входящих» и списках — активные и выполненные сегодня (до полуночи, итерация 27).
     */
    fun observeAllScreen(today: LocalDate): Flow<AllScreenTasks>

    /**
     * Неделя из семи дней, начиная с [weekStart] (итерация 44): активные и выполненные задачи недели (выполненные — за любой день)
     * и все активные повторяющиеся — для будущих разов.
     */
    fun observeWeek(weekStart: LocalDate): Flow<WeekTasks>

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
     * «Не буду делать» (итерация 49). Обычная задача закрывается без выполнения. У повторяющейся закрывается раз: ближайший
     * ([occurrence] = null) или будущий [occurrence] — тогда незакрытые повторы перед ним пропускаются или выполняются ([earlier]);
     * серия переезжает на следующий раз, а закрытый раз сохраняется отдельной задачей. null — задача уже закрыта или удалена.
     */
    suspend fun wontDo(taskId: String, occurrence: TaskDue? = null, earlier: EarlierRepeats = EarlierRepeats.SKIP): WontDo?

    /** Отменяет «Не буду делать»: созданные записи стираются, задача возвращается как была. */
    suspend fun undoWontDo(result: WontDo)

    /** Задачи «Не буду делать», новые сверху (экран «Не буду делать», итерация 49). */
    fun observeWontDo(): Flow<List<Task>>

    /** Сколько задач «Не буду делать» — для строки на экране «Списки». */
    fun observeWontDoCount(): Flow<Int>

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
     * Возвращает выполненную задачу (или «не буду делать», итерация 49) в активные. Закрытый раз повторяющейся задачи возвращается
     * обычной задачей без повтора — серия не меняется (решение PM). Возвращает задачу до изменения — для «Отмены».
     */
    suspend fun restore(taskId: String): Task?

    /** «Отмена» возврата: задача снова закрыта (выполнена или «не буду делать»), с прежним временем и повтором. */
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

    // ---- Корзина (итерация 39, бэклог №35) ----

    /** Удалённые задачи (корни деревьев), новые сверху; момент удаления — [Task.updatedAt]. */
    fun observeTrash(): Flow<List<Task>>

    fun observeTrashCount(): Flow<Int>

    /** Задача из корзины для просмотра; null — её там нет (вернули или стёрли). */
    fun observeTrashed(taskId: String): Flow<TrashedTask?>

    /**
     * Возвращает задачу из корзины вместе с подзадачами и чек-листами: в прежний список, а если списка уже нет — во «Входящие».
     * Результат нужен для [undoRestoreFromTrash]; null — задачи в корзине нет.
     */
    suspend fun restoreFromTrash(taskId: String): Restoration?

    suspend fun restoreAllFromTrash(): Restoration

    /** «Отмена» восстановления: задачи снова в корзине с прежним временем удаления. */
    suspend fun undoRestoreFromTrash(restoration: Restoration)

    /** Стирает задачу из корзины навсегда вместе с подзадачами и чек-листами. */
    suspend fun deleteForever(taskId: String)

    /** Стирает всю корзину навсегда; возвращает, сколько задач (корней) было. */
    suspend fun emptyTrash(): Int
}
