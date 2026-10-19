package com.prostovasei.simpletasks.widget

import android.content.Context
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.feature.all.projectsById
import com.prostovasei.simpletasks.ui.feature.task.options
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Зависимости для виджета: Glance создаёт его сам, без Hilt-внедрения. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun tasks(): TaskRepository
    fun projects(): ProjectRepository
    fun time(): TimeProvider
    fun dayTicker(): DayTicker
}

fun Context.widgetEntryPoint(): WidgetEntryPoint = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/** Данные виджета для выбранного [filter]; обновляются при любых изменениях задач, в начале и конце сегодняшних задач и в полночь. */
@OptIn(ExperimentalCoroutinesApi::class)
fun WidgetEntryPoint.observeModel(filter: WidgetFilter): Flow<WidgetModel> = dayTicker().today().flatMapLatest { today ->
    val zone = time().zone()
    val tree = projects().observeProjectTree()
    val model = when (filter) {
        WidgetFilter.All -> combine(tasks().observeAllScreen(today), tree) { all, t -> WidgetModels.all(all, t.projectsById(), today, zone) to t }
        WidgetFilter.Inbox -> combine(tasks().observeInbox(), tree) { list, t -> WidgetModels.list(filter, null, list, t.projectsById(), today, zone) to t }
        is WidgetFilter.ProjectList -> combine(tasks().observeProjectTasks(filter.projectId), tree) { list, t ->
            val projects = t.projectsById()
            // Список удалили — виджет возвращается к «Все».
            (projects[filter.projectId]?.let { WidgetModels.list(filter, it.name, list, projects, today, zone) }
                ?: WidgetModel(WidgetFilter.All, null, today, emptyList())) to t
        }
    }
    model.map { (m, t) ->
        val now = time().now().atZone(zone)
        val nowMinute = if (now.toLocalDate() == today) now.hour * MINUTES_PER_HOUR + now.minute else null
        m.copy(tabs = WidgetModels.tabs(m.filter, t.options()), nowMinute = nowMinute)
    }
}

private const val MINUTES_PER_HOUR = 60
