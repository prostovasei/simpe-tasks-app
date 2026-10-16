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
    fun allShowsOverdueThenDaysForNext30DaysWithRepeats() {
        val all = AllScreenTasks(
            overdue = listOf(task("late", due = day(-2))),
            todayAllDay = listOf(task("gym", due = day(0)).copy(rrule = "FREQ=WEEKLY")),
            todayTimed = emptyList(),
            upcoming = listOf(task("soon", due = day(5)), task("far", due = day(31))),
        )
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)

        assertEquals(
            "Через 31 день — уже не показываем; еженедельный повтор — по дням",
            listOf(
                WidgetDay.Overdue to listOf("late"),
                WidgetDay.On(today) to listOf("gym"),
                WidgetDay.On(today.plusDays(5)) to listOf("soon"),
                WidgetDay.On(today.plusDays(7)) to listOf("gym@0"),
                WidgetDay.On(today.plusDays(14)) to listOf("gym@1"),
                WidgetDay.On(today.plusDays(21)) to listOf("gym@2"),
                WidgetDay.On(today.plusDays(28)) to listOf("gym@3"),
            ),
            model.days.map { block -> block.day to block.items.map { it.id } },
        )
        assertEquals(today, model.today)
    }

    @Test
    fun sameDayTasksShareOneBlock() {
        val all = AllScreenTasks(emptyList(), listOf(task("a", due = day(0)), task("b", due = day(0))), emptyList(), listOf(task("c", due = day(1)), task("d", due = day(1))))
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), model.days.map { block -> block.items.map { it.id } })
    }

    @Test
    fun emptyWidget() {
        val model = WidgetModels.all(AllScreenTasks(emptyList(), emptyList(), emptyList(), emptyList()), projects, today, ZoneOffset.UTC)
        assertTrue(model.isEmpty)
    }

    @Test
    fun listGroupsByDayWithOverdueFirstAndNoDateLast() {
        val tasks = listOf(
            task("none", projectId = "work"),
            task("next", projectId = "work", due = day(3)),
            task("late", projectId = "work", due = day(-1)),
            task("now", projectId = "work", due = day(0)),
        )
        val model = WidgetModels.list(WidgetFilter.ProjectList("work"), "Работа", tasks, projects, today, ZoneOffset.UTC)
        assertEquals("Работа", model.title)
        assertEquals(
            listOf(WidgetDay.Overdue, WidgetDay.On(today), WidgetDay.On(today.plusDays(3)), WidgetDay.NoDate),
            model.days.map { it.day },
        )
    }

    @Test
    fun shortDates() {
        assertEquals("05 окт", WidgetDates.short(LocalDate.of(2026, 10, 5)))
        assertEquals("16 окт", WidgetDates.short(LocalDate.of(2026, 10, 16)))
        assertEquals("01 мая", WidgetDates.short(LocalDate.of(2026, 5, 1)))
        assertEquals("понедельник", WidgetDates.weekday(LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun filterIsStoredAsText() {
        listOf(WidgetFilter.All, WidgetFilter.Inbox, WidgetFilter.ProjectList("p-1")).forEach {
            assertEquals(it, WidgetFilter.decode(it.encode()))
        }
        assertEquals("По умолчанию — «Все»", WidgetFilter.All, WidgetFilter.decode(null))
    }
}
