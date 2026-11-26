package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** #357: порядок задач списка — все варианты, направления и ничьи. */
class TaskSorterTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 11, 27)

    private fun task(id: String, title: String, due: TaskDue? = null, priority: Priority = Priority.NONE, created: Long = 0, order: Int = 0) = Task(
        id = id, projectId = "p", title = title, content = "", status = TaskStatus.ACTIVE, priority = priority, due = due, start = null,
        completedAt = null, sortOrder = order, rrule = null, reminders = emptyList(), parentId = null,
        createdAt = Instant.ofEpochSecond(created), updatedAt = Instant.ofEpochSecond(created),
    )

    private fun at(date: LocalDate, hour: Int) = TaskDue.AtTime(date.atTime(hour, 0).toInstant(ZoneOffset.UTC))

    private val tasks = listOf(
        task("nodate", "Разобрать почту", created = 5, order = 3),
        task("tomorrow", "Презентация", TaskDue.AllDay(day.plusDays(1)), Priority.HIGH, created = 1, order = 0),
        task("today10", "Созвон", at(day, 10), created = 4, order = 2),
        task("todayAll", "Билеты", TaskDue.AllDay(day), created = 2, order = 1),
        task("todayAllB", "Авиа", TaskDue.AllDay(day), created = 3, order = 4),
        task("old", "Отчёт", TaskDue.AllDay(day.minusDays(1)), created = 6, order = 5),
    )

    private fun ids(sort: TaskSort) = TaskSorter.sort(tasks, sort, zone).map { it.id }

    @Test
    fun dueAscendingAllDayFirstTiesByNameUndatedLast() =
        assertEquals(listOf("old", "todayAllB", "todayAll", "today10", "tomorrow", "nodate"), ids(TaskSort(SortBy.DUE, false)))

    @Test
    fun dueDescendingKeepsUndatedLastAndNameOrder() =
        assertEquals(listOf("tomorrow", "today10", "todayAllB", "todayAll", "old", "nodate"), ids(TaskSort(SortBy.DUE, true)))

    @Test
    fun priorityHighFirstThenDue() =
        assertEquals(listOf("tomorrow", "old", "todayAllB", "todayAll", "today10", "nodate"), ids(TaskSort(SortBy.PRIORITY)))

    @Test
    fun priorityAscendingNormalFirst() =
        assertEquals("tomorrow", ids(TaskSort(SortBy.PRIORITY, false)).last())

    @Test
    fun createdNewestFirstOrOldest() {
        assertEquals(listOf("old", "nodate", "today10", "todayAllB", "todayAll", "tomorrow"), ids(TaskSort(SortBy.CREATED)))
        assertEquals("tomorrow", ids(TaskSort(SortBy.CREATED, false)).first())
    }

    @Test
    fun manualFollowsSortOrder() =
        assertEquals(listOf("tomorrow", "todayAll", "today10", "nodate", "todayAllB", "old"), ids(TaskSort(SortBy.MANUAL)))

    @Test
    fun sortSurvivesEncoding() {
        listOf(TaskSort(SortBy.DUE), TaskSort(SortBy.PRIORITY, false), TaskSort(SortBy.MANUAL), TaskSort(SortBy.TIME, true))
            .forEach { assertEquals(it, TaskSort.decode(it.encode())) }
        assertEquals(null, TaskSort.decode("BROKEN|1"))
        assertEquals(TaskSort(SortBy.DUE, true), TaskSort(SortBy.DUE).flipped())
        assertEquals(TaskSort(SortBy.MANUAL), TaskSort(SortBy.MANUAL).flipped())
    }
}
