package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WidgetModelTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val projects = mapOf("inbox" to project("inbox", "Inbox", isInbox = true), "work" to project("work", "Работа"))
    private fun day(offset: Long) = TaskDue.AllDay(today.plusDays(offset))

    @Test
    fun allShowsOverdueTodayAndNext30DaysWithRepeats() {
        val all = AllScreenTasks(
            overdue = listOf(task("late", due = day(-2))),
            todayAllDay = listOf(task("gym", due = day(0)).copy(rrule = "FREQ=WEEKLY")),
            todayTimed = emptyList(),
            upcoming = listOf(task("soon", due = day(5)), task("far", due = day(31))),
        )
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)

        assertEquals(listOf(WidgetSectionKind.OVERDUE, WidgetSectionKind.TODAY, WidgetSectionKind.UPCOMING), model.sections.map { it.kind })
        assertEquals(listOf("late"), model.sections[0].items.map { it.id })
        assertEquals(listOf("gym"), model.sections[1].items.map { it.id })
        assertEquals(
            "Через 31 день — уже не показываем; еженедельный повтор — 4 раза",
            listOf("soon", "gym@0", "gym@1", "gym@2", "gym@3"),
            model.sections[2].items.map { it.id },
        )
        assertEquals("Будущие повторы не считаются", 3, model.openCount)
    }

    @Test
    fun emptySectionsAreHidden() {
        val model = WidgetModels.all(AllScreenTasks(emptyList(), emptyList(), emptyList(), emptyList()), projects, today, ZoneOffset.UTC)
        assertTrue(model.isEmpty)
        assertEquals(0, model.openCount)
    }

    @Test
    fun listModelHasOneSection() {
        val model = WidgetModels.list(WidgetFilter.ProjectList("work"), "Работа", listOf(task("a", projectId = "work"), task("b", projectId = "work")), projects, today, ZoneOffset.UTC)
        assertEquals("Работа", model.title)
        assertEquals(listOf(null), model.sections.map { it.kind })
        assertEquals(2, model.openCount)
    }

    @Test
    fun filterIsStoredAsText() {
        listOf(WidgetFilter.All, WidgetFilter.Inbox, WidgetFilter.ProjectList("p-1")).forEach {
            assertEquals(it, WidgetFilter.decode(it.encode()))
        }
        assertEquals("По умолчанию — «Все»", WidgetFilter.All, WidgetFilter.decode(null))
    }
}
