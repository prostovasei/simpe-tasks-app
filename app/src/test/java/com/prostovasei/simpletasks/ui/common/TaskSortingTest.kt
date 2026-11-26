package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.TaskSort
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** #357: раскладка экрана списка по блокам и порядок внутри дня «Предстоящего». */
class TaskSortingTest {
    private val today = LocalDate.of(2026, 11, 27)

    private fun item(id: String, day: DayLabel?, time: String? = null, priority: Priority = Priority.NONE, created: Long = 0, done: Boolean = false, completing: Boolean = false) =
        TaskItem(id, id, priority, day?.let { DueText(it, time) }, "Работа", "💼", false, isDone = done, isCompleting = completing, createdAt = created)

    @Test
    fun dueSortGroupsOverdueDaysNoDateAndDone() {
        val items = listOf(
            item("a", DayLabel.Date(today.minusDays(3), false)), item("b", DayLabel.Yesterday),
            item("c", DayLabel.Today), item("d", DayLabel.Today, "10:00"), item("e", DayLabel.Tomorrow),
            item("f", null), item("done", DayLabel.Today, done = true), item("melting", DayLabel.Tomorrow, done = true, completing = true),
        )
        val sorted = sortedTasks(items, TaskSort.LIST_DEFAULT, today)
        assertEquals(listOf("overdue", "day-$today", "day-${today.plusDays(1)}", "nodate"), sorted.groups.map { it.key })
        assertEquals(listOf("a", "b"), sorted.groups[0].items.map { it.id })
        assertEquals(listOf("e", "melting"), sorted.groups[2].items.map { it.id })
        assertEquals(listOf("done"), sorted.done.map { it.id })
    }

    @Test
    fun otherSortsAreOneFlatList() {
        val sorted = sortedTasks(listOf(item("a", DayLabel.Today), item("b", null), item("c", null, done = true)), TaskSort(SortBy.PRIORITY), today)
        assertEquals(listOf("flat"), sorted.groups.map { it.key })
        assertEquals(listOf("a", "b"), sorted.active.map { it.id })
        assertEquals(listOf("c"), sorted.done.map { it.id })
    }

    @Test
    fun dayOrderFollowsUpcomingSort() {
        val day = listOf(
            item("all", DayLabel.Today, created = 1), item("nine", DayLabel.Today, "09:00", Priority.HIGH, created = 3), item("ten", DayLabel.Today, "10:00", created = 2),
        )
        assertEquals(day, DaySort.apply(day, TaskSort.DAY_DEFAULT))
        assertEquals(listOf("ten", "nine", "all"), DaySort.apply(day, TaskSort(SortBy.TIME, true)).map { it.id })
        assertEquals(listOf("nine", "all", "ten"), DaySort.apply(day, TaskSort(SortBy.PRIORITY)).map { it.id })
        assertEquals(listOf("nine", "ten", "all"), DaySort.apply(day, TaskSort(SortBy.CREATED)).map { it.id })
    }

    @Test
    fun hubOverdueIsSortedWithinEachDay() {
        val overdue = listOf(
            item("y1", DayLabel.Yesterday), item("y2", DayLabel.Yesterday, priority = Priority.HIGH), item("old", DayLabel.Date(today.minusDays(5), false)),
        )
        assertEquals(listOf("y2", "y1", "old"), DaySort.byDay(overdue, TaskSort(SortBy.PRIORITY)).map { it.id })
    }

    @Test
    fun pickSelectsWithDefaultDirectionAndFlipsSelected() {
        assertEquals(TaskSort(SortBy.PRIORITY, true), TaskSort.LIST_DEFAULT.pick(SortBy.PRIORITY))
        assertEquals(TaskSort(SortBy.DUE, true), TaskSort.LIST_DEFAULT.pick(SortBy.DUE))
        assertEquals(listOf(SortBy.TIME, SortBy.PRIORITY, SortBy.CREATED), sortOptions(upcoming = true))
    }
}
