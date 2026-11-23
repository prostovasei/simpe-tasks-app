package com.prostovasei.simpletasks.ui.feature.voice

import androidx.lifecycle.ViewModel
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.domain.smartdate.SmartDateParser
import com.prostovasei.simpletasks.ui.feature.task.SmartDateSetting
import com.prostovasei.simpletasks.core.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Продиктованная задача (#310, путь А — без ИИ): [phrase] — фраза как сказана (её получает окно задачи при правке, срок
 * в ней подсвечивается), [title] — название без фразы срока, [due] — срок из фразы или по умолчанию (вид «Все» — сегодня).
 */
data class VoiceTask(val id: Long, val phrase: String, val title: String, val due: TaskDue?) {
    fun date(zone: ZoneId): LocalDate? = when (due) {
        is TaskDue.AllDay -> due.date
        is TaskDue.AtTime -> due.instant.atZone(zone).toLocalDate()
        null -> null
    }

    /** Время справа в строке: «10:00»; у задачи на весь день — нет. */
    fun time(zone: ZoneId): String? = (due as? TaskDue.AtTime)?.instant?.atZone(zone)?.toLocalTime()?.let { "%02d:%02d".format(it.hour, it.minute) }
}

/** День в списке над окном (вариант PM 7 «По датам»): [date] null — «Без даты», он всегда последний. */
data class VoiceDay(val date: LocalDate?, val tasks: List<VoiceTask>)

internal object VoiceTasks {
    /**
     * Фраза → задача: срок разбирается тем же [SmartDateParser], что и при наборе; фраза срока вырезается из названия.
     * [smart] = false — разбор дат выключен в настройках: название как сказано. [today] — у вида «Все» задача без срока на сегодня.
     */
    fun parse(id: Long, phrase: String, today: LocalDate, nowMinute: Int, zone: ZoneId, smart: Boolean, defaultToday: Boolean): VoiceTask {
        val text = VoiceErrors.capitalize(phrase.trim())
        val found = if (smart) SmartDateParser.parse(text, today, nowMinute) else null
        val title = found?.let { SmartDateParser.strip(text, it.ranges) }?.takeIf { it.isNotBlank() } ?: text
        val due = found?.due(zone) ?: if (defaultToday) TaskDue.AllDay(today) else null
        return VoiceTask(id, text, title, due)
    }

    /** Дни по порядку, внутри дня — по времени (без времени — первыми), затем как продиктовано; «Без даты» — в конце. */
    fun days(tasks: List<VoiceTask>, zone: ZoneId): List<VoiceDay> =
        tasks.groupBy { it.date(zone) }
            .toSortedMap(compareBy(nullsLast()) { it })
            .map { (date, items) -> VoiceDay(date, items.sortedWith(compareBy(nullsFirst()) { (it.due as? TaskDue.AtTime)?.instant })) }
}

/**
 * Список продиктованных задач окна голоса (#310). Живёт, пока окно голоса в стеке: при правке задачи в окне задачи
 * экран голоса уходит, а список остаётся. [listening] — продолжать ли запись при возврате: после правки — нет, окно на паузе.
 */
class VoiceListModel : ViewModel() {
    private val mutable = MutableStateFlow<List<VoiceTask>>(emptyList())
    val tasks: StateFlow<List<VoiceTask>> = mutable.asStateFlow()
    private var nextId = 0L
    var listening = true

    fun add(make: (Long) -> VoiceTask): VoiceTask = make(++nextId).also { task -> mutable.update { it + task } }

    /** «×» у задачи: возвращает место, чтобы «Отмена» вернула её туда же. */
    fun remove(id: Long): Pair<Int, VoiceTask>? {
        val list = mutable.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return null
        mutable.value = list.toMutableList().also { it.removeAt(index) }
        return index to list[index]
    }

    fun restore(index: Int, task: VoiceTask) = mutable.update { it.toMutableList().apply { add(index.coerceIn(0, size), task) } }

    /** Задачу сохранили из окна задачи — она уходит из списка. */
    fun saved(id: Long) = mutable.update { list -> list.filterNot { it.id == id } }

    fun clear() { mutable.value = emptyList() }
}

/** Сохранение всех продиктованных задач галочкой окна голоса и «Отмена» в подсказке (#310). */
@HiltViewModel
class VoiceTasksSaver @Inject constructor(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val setting: SmartDateSetting,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    /** Разбирать ли срок во фразах — та же настройка, что и для набора. */
    val smartDates: Boolean get() = setting.enabled.value

    /** Задачи уходят во «Входящие», как у «+» виджета; напоминание — по умолчанию для срока, как у новой задачи. Возвращает id. */
    suspend fun save(items: List<VoiceTask>): List<String> {
        val inbox = projects.observeProjectTree().first().inbox?.project?.id ?: return emptyList()
        return items.map { item ->
            tasks.createTask(NewTask(projectId = inbox, title = item.title, due = item.due, reminders = Reminders.default(item.due)))
        }
    }

    /**
     * «Отмена» в подсказке: окно сразу закрывается, поэтому удаление идёт в области приложения, а не окна.
     * Только что созданные задачи удаляются насовсем и в корзину не попадают (навсегда удаляются только задачи из корзины).
     */
    fun undo(ids: List<String>) {
        appScope.launch { remove(ids) }
    }

    internal suspend fun remove(ids: List<String>) = ids.forEach { id ->
        tasks.deleteTask(id)
        tasks.deleteForever(id)
    }
}
