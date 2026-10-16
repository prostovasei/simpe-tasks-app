package com.prostovasei.simpletasks.ui.feature.all

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Лента «Все» по дням (итерация 13): просроченные — каждая в своём дне. */
class DayGroupsTest {
    private val today = LocalDate.of(2026, 10, 16)

    private fun item(id: String, day: DayLabel, time: String? = null) =
        TaskItem(id, id, Priority.NONE, DueText(day, time), null, null, isInboxList = true)

    @Test
    fun overdueSplitByDaysThenTodayThenUpcoming() {
        val state = AllState(
            overdue = listOf(
                item("y", DayLabel.Yesterday),
                item("old", DayLabel.Date(LocalDate.of(2026, 10, 10), false)),
                item("morning", DayLabel.Today, "08:00"),
            ),
            todayAllDay = listOf(item("a", DayLabel.Today)),
            todayTimed = listOf(item("t", DayLabel.Today, "18:00")),
            upcoming = listOf(item("tm", DayLabel.Tomorrow), item("far", DayLabel.Date(LocalDate.of(2026, 10, 20), false))),
            today = today,
        )

        val groups = state.dayGroups()

        assertEquals(
            listOf(LocalDate.of(2026, 10, 10), today.minusDays(1), today, today.plusDays(1), LocalDate.of(2026, 10, 20)),
            groups.map { it.date },
        )
        assertEquals(listOf(true, true, false, false, false), groups.map { it.isOverdue })
        assertEquals(listOf(null, DayLabel.Yesterday, DayLabel.Today, DayLabel.Tomorrow, null), groups.map { it.relative })
        assertEquals(listOf("morning", "a", "t"), groups[2].items.map { it.id })
        assertEquals(setOf("y", "old", "morning"), groups[2].overdueIds)
    }

    @Test
    fun noDateWhileLoading() {
        assertEquals(emptyList<DayGroup>(), AllState(todayAllDay = listOf(item("a", DayLabel.Today))).dayGroups())
    }
}
