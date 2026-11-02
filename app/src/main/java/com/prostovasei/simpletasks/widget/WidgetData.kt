package com.prostovasei.simpletasks.widget

import android.content.Context
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.feature.all.OverdueFold
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.feature.task.options
import com.prostovasei.simpletasks.ui.feature.week.buildWeek
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** Зависимости для виджета: Glance создаёт его сам, без Hilt-внедрения. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun tasks(): TaskRepository
    fun projects(): ProjectRepository
    fun time(): TimeProvider
    fun dayTicker(): DayTicker
    fun overdueFold(): OverdueFold
}

fun Context.widgetEntryPoint(): WidgetEntryPoint = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/**
 * Выбор в виде «Неделя» (итерация 45), свой у каждого виджета: [start] — понедельник выбранной недели, [month] — месяц мини-календаря;
 * null — текущая неделя и месяц сегодняшнего дня.
 */
data class WeekPick(val start: LocalDate? = null, val month: YearMonth? = null)

/** Данные виджета для выбранного [filter]; обновляются при любых изменениях задач, в начале и конце сегодняшних задач и в полночь. */
@OptIn(ExperimentalCoroutinesApi::class)
fun WidgetEntryPoint.observeModel(filter: WidgetFilter, pick: WeekPick = WeekPick()): Flow<WidgetModel> = dayTicker().today().flatMapLatest { today ->
    val zone = time().zone()
    val tree = projects().observeProjectTree()
    val model = when (filter) {
        WidgetFilter.All -> combine(tasks().observeAllScreen(today), tree) { all, t -> WidgetModels.all(all, t.projectsById(), today, zone) to t }
        WidgetFilter.Inbox -> combine(tasks().observeInbox(today), tree) { list, t -> WidgetModels.list(filter, null, list, t.projectsById(), today, zone) to t }
        WidgetFilter.Week -> {
            val start = pick.start ?: today.weekStart()
            combine(tasks().observeWeek(start), tree) { week, t ->
                val days = buildWeek(start, week, t.projectsById(), today, zone, emptySet())
                WidgetModel(WidgetFilter.Week, null, today, emptyList(), week = WidgetWeek(start, pick.month ?: YearMonth.from(today), days, today)) to t
            }
        }
        is WidgetFilter.ProjectList -> combine(tasks().observeProjectTasks(filter.projectId, today), tree) { list, t ->
            val projects = t.projectsById()
            // Список удалили — виджет возвращается к «Все».
            (projects[filter.projectId]?.let { WidgetModels.list(filter, it.name, list, projects, today, zone) }
                ?: WidgetModel(WidgetFilter.All, null, today, emptyList())) to t
        }
    }
    model.map { (m, t) ->
        val now = time().now().atZone(zone)
        val nowMinute = if (now.toLocalDate() == today) now.hour * MINUTES_PER_HOUR + now.minute else null
        m.copy(tabs = WidgetModels.tabs(m.filter, t.options()), nowMinute = nowMinute, overdueCollapsed = overdueFold().collapsed.value)
    }
}

private const val MINUTES_PER_HOUR = 60
