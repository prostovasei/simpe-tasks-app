package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.TaskSort
import java.time.LocalDate

/**
 * Порядок внутри дня «Предстоящего» (#357): задачи дня приходят по времени (весь день, потом по времени) — это «по времени ↑».
 * ↓ разворачивает день; «по приоритету» — высокий выше, внутри прежний порядок; «по дате создания» — новые выше.
 */
object DaySort {
    fun apply(items: List<TaskItem>, sort: TaskSort): List<TaskItem> = when (sort.by) {
        SortBy.TIME, SortBy.DUE, SortBy.MANUAL -> if (sort.descending) items.reversed() else items
        SortBy.PRIORITY -> items.sortedBy { if (sort.descending) -it.priority.value else it.priority.value }
        SortBy.CREATED -> items.sortedBy { if (sort.descending) -it.createdAt else it.createdAt }
    }

    /** Подряд идущие задачи одного дня сортируются внутри своего дня (просроченные в хабе — несколько дней в одном блоке). */
    fun byDay(items: List<TaskItem>, sort: TaskSort): List<TaskItem> {
        if (sort == TaskSort.DAY_DEFAULT || items.size < 2) return items
        val result = ArrayList<TaskItem>(items.size)
        var start = 0
        while (start < items.size) {
            var end = start + 1
            while (end < items.size && items[end].due?.day == items[start].due?.day) end++
            result += apply(items.subList(start, end), sort)
            start = end
        }
        return result
    }
}

/** Блок экрана списка (#357): «Просрочено», день, «Без даты» или общий список без подписи. */
data class TaskGroup(val kind: Kind, val date: LocalDate?, val items: List<TaskItem>) {
    enum class Kind { OVERDUE, DAY, NO_DATE, FLAT }

    val key: String get() = when (kind) {
        Kind.OVERDUE -> "overdue"
        Kind.DAY -> "day-$date"
        Kind.NO_DATE -> "nodate"
        Kind.FLAT -> "flat"
    }
}

/** Экран списка по блокам: активные (и только что отмеченные — на месте) и свёрнутые «Выполнено». */
data class SortedTasks(val groups: List<TaskGroup>, val done: List<TaskItem>) {
    val active: List<TaskItem> get() = groups.flatMap { it.items }
}

/**
 * Раскладка уже упорядоченных задач (#357): при «по сроку» — подписи дней, просроченные одним блоком, без даты — блок в конце;
 * при остальных вариантах — один список без подписей. Выполненные (не тающие) — в «Выполнено».
 */
fun sortedTasks(items: List<TaskItem>, sort: TaskSort, today: LocalDate?): SortedTasks {
    val (done, active) = items.partition { it.isDone && !it.isCompleting }
    if (sort.by != SortBy.DUE || today == null) return SortedTasks(listOf(TaskGroup(TaskGroup.Kind.FLAT, null, active)).filter { it.items.isNotEmpty() }, done)
    val groups = mutableListOf<TaskGroup>()
    val noDate = active.filter { it.due == null }
    active.filter { it.due != null }.forEach { item ->
        val date = item.dayDate(today)
        val kind = if (date < today) TaskGroup.Kind.OVERDUE else TaskGroup.Kind.DAY
        val last = groups.lastOrNull()
        if (last != null && last.kind == kind && (kind == TaskGroup.Kind.OVERDUE || last.date == date)) {
            groups[groups.lastIndex] = last.copy(items = last.items + item)
        } else {
            groups += TaskGroup(kind, date.takeIf { kind == TaskGroup.Kind.DAY }, listOf(item))
        }
    }
    if (noDate.isNotEmpty()) groups += TaskGroup(TaskGroup.Kind.NO_DATE, null, noDate)
    return SortedTasks(groups, done)
}

/** День задачи по подписи срока. */
fun TaskItem.dayDate(today: LocalDate): LocalDate = when (val day = due?.day) {
    DayLabel.Yesterday -> today.minusDays(1)
    DayLabel.Tomorrow -> today.plusDays(1)
    is DayLabel.Date -> day.date
    DayLabel.Today, null -> today
}
