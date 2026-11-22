package com.prostovasei.simpletasks.ui.feature.week

import com.prostovasei.simpletasks.domain.model.Project
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.WeekFilter
import com.prostovasei.simpletasks.domain.model.WeekTasks
import com.prostovasei.simpletasks.domain.model.upcomingRepeats
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.sortDay
import com.prostovasei.simpletasks.ui.common.sortTime
import com.prostovasei.simpletasks.ui.common.toItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val weekCalendarLocale: Locale = Locale.forLanguageTag("ru")
internal val weekCalendarFullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", weekCalendarLocale)

private val calendarMonthDescriptionFormat = DateTimeFormatter.ofPattern("LLLL uuuu", weekCalendarLocale)

internal fun YearMonth.calendarMonthDescription(): String = format(calendarMonthDescriptionFormat)

internal fun YearMonth.calendarMonthLabel(): String =
    month.getDisplayName(TextStyle.SHORT_STANDALONE, weekCalendarLocale).removeSuffix(".")

/** Дней в неделе. */
const val WEEK_LENGTH = 7

/** Понедельник недели, в которую входит [date]. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Семь дней недели с [weekStart]: задачи недели по их дню (начало интервала или срок), выполненные — на своём дне; будущие разы
 * повторяющихся (после сегодняшнего дня) — бледными копиями. Внутри дня: «весь день», потом по времени, настоящие раньше будущих повторов.
 */
/** Неделя без задач списков, которые отсеял [filter] (#243); неизвестный список (удалён) — как «Без списка» не считается, задача остаётся. */
fun WeekTasks.filtered(filter: WeekFilter, projects: Map<String, Project>): WeekTasks {
    if (!filter.isActive) return this
    fun keep(task: Task) = projects[task.projectId]?.let { filter.allows(it.id, it.isInbox) } ?: true
    return WeekTasks(inRange.filter(::keep), repeating.filter(::keep))
}

fun buildWeek(weekStart: LocalDate, week: WeekTasks, projects: Map<String, Project>, today: LocalDate, zone: ZoneId, pending: Set<String>): List<WeekDay> {
    val last = weekStart.plusDays(WEEK_LENGTH - 1L)
    val real = week.inRange.map { it to it.toItem(projects, today, zone, pending) }
    // Будущие разы — только после сегодняшнего дня; прошлые разы уже стали выполненными задачами.
    val after = maxOf(today, weekStart.minusDays(1))
    val ghosts = if (after >= last) {
        emptyList()
    } else {
        week.repeating.flatMap { task ->
            task.upcomingRepeats(zone, after, last).mapIndexed { index, repeat -> repeat to repeat.toItem(projects, today, zone, pending, ghostIndex = index) }
        }
    }
    val byDay = (real + ghosts)
        .sortedWith(compareBy({ it.first.sortDay(zone) }, { it.first.sortTime() }, { it.second.isGhost }))
        .groupBy({ it.first.sortDay(zone) }, { it.second })
    return (0 until WEEK_LENGTH).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        WeekDay(date, byDay[date].orEmpty())
    }
}

/**
 * Месяц мини-календаря после выбора недели [weekStart] (0.46.0, решение PM: строки месяца со стрелками нет, месяц листается сам).
 * Неделя с днями другого месяца (бледными) переводит календарь на этот месяц; неделя целиком из [shown] его не меняет.
 * Так выбор первой строки листает назад, последних — вперёд, а выбранная неделя всегда остаётся на виду.
 */
fun monthAfterPick(shown: YearMonth, weekStart: LocalDate): YearMonth =
    (0 until WEEK_LENGTH).map { YearMonth.from(weekStart.plusDays(it.toLong())) }.firstOrNull { it != shown } ?: shown

/** Время задачи для ячейки: начало («10:00»), без конца интервала; у «весь день» — null. */
fun TaskItem.startTime(): String? = due?.time?.substringBefore('–')
