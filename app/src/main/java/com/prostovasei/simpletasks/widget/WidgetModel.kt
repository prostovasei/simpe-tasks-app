package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.REPEATS_AHEAD_DAYS
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.toItem
import com.prostovasei.simpletasks.ui.common.upcomingWithRepeats
import java.time.LocalDate
import java.time.ZoneId

/** Что показывает виджет (решение PM: по умолчанию «Все», переключается в самом виджете). */
sealed interface WidgetFilter {
    /** Просроченные и задачи на ближайшие 30 дней (с будущими повторами). */
    data object All : WidgetFilter
    data object Inbox : WidgetFilter
    data class ProjectList(val projectId: String) : WidgetFilter

    fun encode(): String = when (this) {
        All -> "all"
        Inbox -> "inbox"
        is ProjectList -> "project:$projectId"
    }

    companion object {
        fun decode(value: String?): WidgetFilter = when {
            value == "inbox" -> Inbox
            value?.startsWith("project:") == true -> ProjectList(value.removePrefix("project:"))
            else -> All
        }
    }
}

enum class WidgetSectionKind { OVERDUE, TODAY, UPCOMING }

data class WidgetSection(val kind: WidgetSectionKind?, val items: List<TaskItem>)

/**
 * Данные виджета. [title] — имя списка (null — «Все» или «Входящие», подпись из ресурсов);
 * [openCount] — открытые задачи в виджете (без будущих повторов).
 */
data class WidgetModel(
    val filter: WidgetFilter,
    val title: String?,
    val sections: List<WidgetSection>,
) {
    val openCount: Int get() = sections.sumOf { section -> section.items.count { !it.isGhost } }
    val isEmpty: Boolean get() = sections.all { it.items.isEmpty() }
}

object WidgetModels {
    /** «Все»: просроченные → сегодня → ближайшие 30 дней вместе с будущими повторами. */
    fun all(all: AllScreenTasks, projects: Map<String, Project>, today: LocalDate, zone: ZoneId): WidgetModel {
        val until = today.plusDays(REPEATS_AHEAD_DAYS)
        val everything = all.overdue + all.todayAllDay + all.todayTimed + all.upcoming
        val upcoming = all.upcoming.filter { it.localDate(zone)?.let { date -> date <= until } == true }
        fun List<Task>.items() = map { it.toItem(projects, today, zone, emptySet()) }
        return WidgetModel(
            filter = WidgetFilter.All,
            title = null,
            sections = listOf(
                WidgetSection(WidgetSectionKind.OVERDUE, all.overdue.items()),
                WidgetSection(WidgetSectionKind.TODAY, (all.todayAllDay + all.todayTimed).items()),
                WidgetSection(WidgetSectionKind.UPCOMING, upcomingWithRepeats(upcoming, everything, projects, today, zone, emptySet())),
            ).filter { it.items.isNotEmpty() },
        )
    }

    /** «Входящие» или список: задачи как на экране приложения. */
    fun list(filter: WidgetFilter, title: String?, tasks: List<Task>, projects: Map<String, Project>, today: LocalDate, zone: ZoneId) =
        WidgetModel(filter, title, listOf(WidgetSection(null, tasks.map { it.toItem(projects, today, zone, emptySet()) })))

    private fun Task.localDate(zone: ZoneId): LocalDate? = when (val anchor = start ?: due) {
        is TaskDue.AllDay -> anchor.date
        is TaskDue.AtTime -> anchor.instant.atZone(zone).toLocalDate()
        null -> null
    }
}
