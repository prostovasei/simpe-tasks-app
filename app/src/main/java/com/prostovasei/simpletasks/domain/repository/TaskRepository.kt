package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Completion
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.Moved
import com.prostovasei.simpletasks.domain.model.SortCounts
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

    /** «Входящие» (итерация 52): только неразобранное — без даты и без списка (служебный список «Входящие»); выполненные сегодня — тоже. */
    fun observeInbox(today: LocalDate): Flow<List<Task>>

    /** «Когда-нибудь» (итерация 52): задачи со списком, но без даты; выполненные сегодня — тоже. */
    fun observeSomeday(today: LocalDate): Flow<List<Task>>

    /** «Без списка» (итерация 52): задачи без списка, но с датой, по сроку; выполненные сегодня — тоже. */
    fun observeNoList(today: LocalDate): Flow<List<Task>>

    /** Сколько открытых задач во «Входящих», «Когда-нибудь» и «Без списка» (итерация 52). */
    fun observeSortCounts(): Flow<SortCounts>

    fun observeProjectTasks(projectId: String, today: LocalDate): Flow<List<Task>>

    fun observeTaskDetails(taskId: String): Flow<TaskDetails?>

    /** Подзадачи (активные сначала, затем выполненные). */
    fun observeSubtasks(parentId: String): Flow<List<Task>>

    /** Создаёт задачу вместе с пунктами чек-листа. Возвращает id задачи. */
    suspend fun createTask(task: NewTask): String

    /** Ручной порядок (#357): задачи [ids] получают порядок по позиции в списке. */
    suspend fun reorderTasks(ids: List<String>) {}

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

    /**
     * «На завтра» из уведомления и полоски: срок — завтра, время дня то же. У повторяющейся (итерация 50) переносится только этот
     * раз — как [moveRepeat], серия остаётся на своём расписании. null — задачи нет.
     */
    suspend fun moveToTomorrow(taskId: String): Moved?

    /**
     * «Отложить» из уведомления и полоски (итерация 53): срок [due] и начало [start] задачи — новые (варианты считает
     * [com.prostovasei.simpletasks.domain.model.Snooze]). У повторяющейся — только этот раз, как [moveRepeat]. null — задачи нет.
     */
    suspend fun snooze(taskId: String, due: TaskDue, start: TaskDue?): Moved?

    /**
     * «Текущий повтор» (итерация 50, вариант А): ближайший раз серии становится обычной задачей с новым сроком [due] и началом [start]
     * (с чек-листом и напоминаниями), серия переезжает на следующий раз. У последнего раза серия сама становится обычной задачей.
     * null — задача не повторяющаяся, закрыта или удалена.
     */
    suspend fun moveRepeat(taskId: String, due: TaskDue, start: TaskDue?): Moved?

    /**
     * «Все повторы с этого момента» у будущего раза [occurrence] (итерация 50): серия делится — старая заканчивается накануне
     * этого раза (UNTIL), новая начинается с [due]/[start] с тем же правилом (у «N раз» — с оставшимся числом).
     * null — задача не повторяющаяся, закрыта или удалена.
     */
    suspend fun moveRepeatsFrom(taskId: String, occurrence: TaskDue, due: TaskDue?, start: TaskDue?): Moved?

    /**
     * Перенос на день [date], время дня то же (режим выбора: «На завтра», «На другой день»; 0.51.1): у повторяющейся — только этот раз
     * ([moveRepeat]), серия на своём расписании. Отмена — [undoReschedule].
     */
    suspend fun moveTasks(ids: List<String>, date: LocalDate): Rescheduling

    /** «Отмена» переноса: созданная запись стирается, задача возвращается как была. */
    suspend fun undoMove(moved: Moved)

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
