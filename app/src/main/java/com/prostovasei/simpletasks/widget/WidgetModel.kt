package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.common.upcomingWithRepeats
import com.prostovasei.simpletasks.ui.feature.task.ProjectOption
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Что показывает виджет (решение PM: по умолчанию «Все», переключается в самом виджете). */
sealed interface WidgetFilter {
    /** Просроченные и задачи на ближайшие 30 дней (с будущими повторами). */
    data object All : WidgetFilter
    data object Inbox : WidgetFilter
    data class ProjectList(val projectId: String) : WidgetFilter

    /** Вид «Неделя» (итерация 45): сетка из семи дней выбранной недели и мини-календарь; задачи всех списков, как во «Все». */
    data object Week : WidgetFilter

    fun encode(): String = when (this) {
        All -> "all"
        Inbox -> "inbox"
        Week -> "week"
        is ProjectList -> "project:$projectId"
    }

    companion object {
        fun decode(value: String?): WidgetFilter = when {
            value == "inbox" -> Inbox
            value == "week" -> Week
            value?.startsWith("project:") == true -> ProjectList(value.removePrefix("project:"))
            else -> All
        }
    }
}

/**
 * Блок ленты виджета: один день или задачи без даты (решение PM: лента по дням). Просроченные — тоже по своим дням,
 * как в приложении (итерация 27).
 */
sealed interface WidgetDay {
    data class On(val date: LocalDate) : WidgetDay
    data object NoDate : WidgetDay
}

data class WidgetDayBlock(val day: WidgetDay, val items: List<TaskItem>)

/** Вид «Неделя» в виджете: [weekStart] — понедельник, [month] — месяц мини-календаря, [days] — семь дней с задачами, [today] — сегодняшний день. */
data class WidgetWeek(val weekStart: LocalDate, val month: YearMonth, val days: List<WeekDay>, val today: LocalDate)

/** Вкладка списка внизу виджета. [name] и [color] — у своего списка (у «Все» и «Входящих» — null). */
/** Вкладка виджета; [emoji] — эмодзи списка (итерация 47, до этого — цвет). */
data class WidgetTab(val filter: WidgetFilter, val name: String? = null, val emoji: String? = null)

/** Прогресс дня (итерация 30): сколько из задач дня уже выполнено — у «Сегодня» в виджете. */
data class DayProgress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total

    companion object {
        fun of(items: List<TaskItem>): DayProgress? = items.takeIf { it.isNotEmpty() }?.let { DayProgress(it.count(TaskItem::isDone), it.size) }
    }
}

/**
 * Данные виджета. [title] — имя списка (null — «Все» или «Входящие», подпись из ресурсов);
 * [today] — дата в нижней строке и точка отсчёта для подписей дней; [tabs] — вкладки списков внизу.
 */
data class WidgetModel(
    val filter: WidgetFilter,
    val title: String?,
    val today: LocalDate,
    val days: List<WidgetDayBlock>,
    val tabs: List<WidgetTab> = emptyList(),
    /** Минута дня для отметки «сейчас» в блоке «Сегодня» (итерация 20); null — без отметки. */
    val nowMinute: Int? = null,
    /** Блок «Просрочено» свёрнут (итерация 37): прошедшие дни ленты «Все» скрыты. */
    val overdueCollapsed: Boolean = false,
    /** Данные вида «Неделя» (итерация 45); у остальных видов — null. */
    val week: WidgetWeek? = null,
) {
    /** В «Неделе» сетка есть всегда, даже без задач. */
    val isEmpty: Boolean get() = week == null && days.all { it.items.isEmpty() }

    /** Прошедшие дни с задачами — блок «Просрочено» ленты «Все». */
    val pastDays: List<WidgetDayBlock>
        get() = days.filter { block -> block.items.isNotEmpty() && (block.day as? WidgetDay.On)?.date?.let { it < today } == true }

    /** Невыполненных задач в прошедших днях — число у подписи «Просрочено». */
    val overdueCount: Int get() = pastDays.sumOf { block -> block.items.count { !it.isDone } }
}

/** Что рисует лента виджета (итерация 37): подпись «Просрочено» над прошедшими днями или день. */
sealed interface WidgetEntry {
    data class Overdue(val count: Int, val collapsed: Boolean) : WidgetEntry
    data class Day(val block: WidgetDayBlock) : WidgetEntry
}

/** Дни ленты; в «Все» над первым прошедшим днём — «Просрочено», свёрнутое скрывает прошедшие дни. */
fun WidgetModel.entries(): List<WidgetEntry> {
    val shown = days.filter { it.items.isNotEmpty() }
    val past = pastDays.takeIf { filter == WidgetFilter.All }.orEmpty()
    if (past.isEmpty()) return shown.map { WidgetEntry.Day(it) }
    return shown.flatMap { block ->
        buildList {
            if (block == past.first()) add(WidgetEntry.Overdue(overdueCount, overdueCollapsed))
            if (!(overdueCollapsed && block in past)) add(WidgetEntry.Day(block))
        }
    }
}

object WidgetModels {
    /**
     * «Все»: просроченные → сегодня → ещё 13 дней вместе с будущими повторами, по дням (итерация 42, решение PM: 14 дней;
     * 0.41.1 — 7 дней и лимиты, до него — 30 дней). Лимиты плашек не нужны: строки — обычные элементы Android (~1 КБ),
     * страховка размера пакета — не больше [MAX_LINES] строк ленты.
     */
    fun all(all: AllScreenTasks, projects: Map<String, Project>, today: LocalDate, zone: ZoneId): WidgetModel {
        val until = today.plusDays(DAYS - 1L)
        val everything = all.overdue + all.todayAllDay + all.todayTimed + all.upcoming
        val upcoming = all.upcoming.filter { it.localDate(zone)?.let { date -> date <= until } == true }
        fun List<Task>.items() = map { it.toItem(projects, today, zone, emptySet()) }
        val rest = (all.todayAllDay + all.todayTimed).items() +
            upcomingWithRepeats(upcoming, everything, projects, today, zone, emptySet()).filter { item -> item.due?.day?.toDate(today)?.let { it <= until } != false }
        return WidgetModel(WidgetFilter.All, null, today, byDay(all.overdue.items(), rest, today))
    }

    /** «Входящие» или список: задачи как на экране приложения, разложенные по дням; без даты — в конце. */
    fun list(filter: WidgetFilter, title: String?, tasks: List<Task>, projects: Map<String, Project>, today: LocalDate, zone: ZoneId) =
        WidgetModel(filter, title, today, byDay(emptyList(), tasks.map { it.toItem(projects, today, zone, emptySet()) }, today))

    /** Сколько дней ленты «Все», считая сегодня (итерация 42, решение PM). */
    const val DAYS = 14

    /**
     * Вкладки внизу (решение PM, итерация 09): «Все», «Входящие» и 2 первых своих списка в порядке экрана «Списки».
     * Выбранный через «⋯» список не из первых двух встаёт на место второго — выбранное всегда видно.
     */
    fun tabs(selected: WidgetFilter, lists: List<ProjectOption>): List<WidgetTab> {
        val own = lists.filterNot { it.isInbox }
        val shown = own.take(LIST_TABS).toMutableList()
        if (selected is WidgetFilter.ProjectList && shown.none { it.id == selected.projectId }) {
            own.find { it.id == selected.projectId }?.let { if (shown.size == LIST_TABS) shown[LIST_TABS - 1] = it else shown += it }
        }
        return listOf(WidgetTab(WidgetFilter.All), WidgetTab(WidgetFilter.Inbox)) +
            shown.map { WidgetTab(WidgetFilter.ProjectList(it.id), it.name, it.emoji) }
    }

    private const val LIST_TABS = 2

    /**
     * Блоки по дням: просроченные — каждый в своём дне (итерация 27) → дни по порядку → без даты.
     * Внутри блока порядок задач сохраняется.
     */
    fun byDay(overdue: List<TaskItem>, rest: List<TaskItem>, today: LocalDate): List<WidgetDayBlock> {
        val groups = (overdue + rest).groupBy { item -> item.due?.day?.toDate(today)?.let { WidgetDay.On(it) } ?: WidgetDay.NoDate }
        val order = compareBy<WidgetDay> { if (it is WidgetDay.On) 0 else 1 }.thenBy { (it as? WidgetDay.On)?.date }
        return groups.keys.sortedWith(order).map { WidgetDayBlock(it, groups.getValue(it)) }
    }

    private fun DayLabel.toDate(today: LocalDate): LocalDate = when (this) {
        DayLabel.Today -> today
        DayLabel.Tomorrow -> today.plusDays(1)
        DayLabel.Yesterday -> today.minusDays(1)
        is DayLabel.Date -> date
    }

    private fun Task.localDate(zone: ZoneId): LocalDate? = when (val anchor = start ?: due) {
        is TaskDue.AllDay -> anchor.date
        is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
        null -> null
    }
}

/** Короткие подписи дат виджета: «16 окт», «пт, 16 окт». */
object WidgetDates {
    private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

    fun short(date: LocalDate): String = "%02d %s".format(date.dayOfMonth, MONTHS[date.monthValue - 1])

    /** «пт, 16 окт» — нижняя строка и подписи дней (там — заглавными). */
    fun withWeekday(date: LocalDate): String = "${WEEKDAYS[date.dayOfWeek.value - 1]}, ${short(date)}"

    /** «сб, 7 ноя» или без дня недели «7 ноя» — дата в нижней строке виджета (итерация 51, число без нуля). */
    fun bar(date: LocalDate, withWeekday: Boolean): String {
        val day = "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"
        return if (withWeekday) "${WEEKDAYS[date.dayOfWeek.value - 1]}, $day" else day
    }

    /** «ПТ, 16 окт» — подписи дней в ленте приложения (итерация 13). */
    fun header(date: LocalDate): String = "${WEEKDAYS[date.dayOfWeek.value - 1].uppercase()}, ${short(date)}"
}
