package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.project
import com.prostovasei.simpletasks.testing.task
import com.prostovasei.simpletasks.ui.feature.task.ProjectOption
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.domain.model.Priority
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
    fun allShowsOverdueByDayThenNext14DaysWithRepeats() {
        val all = AllScreenTasks(
            overdue = listOf(task("late", due = day(-2))),
            todayAllDay = listOf(task("gym", due = day(0)).copy(rrule = "FREQ=WEEKLY")),
            todayTimed = emptyList(),
            upcoming = listOf(task("soon", due = day(5)), task("last", due = day(13)), task("far", due = day(14))),
        )
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)

        assertEquals(
            "Просроченная — в своём дне (итерация 27); итерация 42: сегодня и ещё 13 дней, повторы тоже только в эти дни",
            listOf(
                WidgetDay.On(today.minusDays(2)) to listOf("late"),
                WidgetDay.On(today) to listOf("gym"),
                WidgetDay.On(today.plusDays(5)) to listOf("soon"),
                WidgetDay.On(today.plusDays(7)) to listOf("gym@0"),
                WidgetDay.On(today.plusDays(13)) to listOf("last"),
            ),
            model.days.map { block -> block.day to block.items.map { it.id } },
        )
        assertEquals(today, model.today)
    }

    /** Итерация 42: лимитов плашек больше нет — все просроченные и все задачи списка (строки рабочий стол подгружает сам). */
    @Test
    fun noLimitsOnOverdueAndLists() {
        val overdue = (15 downTo 1).map { task("late$it", due = day(-it.toLong())) }
        val all = WidgetModels.all(AllScreenTasks(overdue, emptyList(), emptyList(), emptyList()), projects, today, ZoneOffset.UTC)
        assertEquals(15, all.days.sumOf { it.items.size })
        assertEquals(15, all.overdueCount)
        val tasks = (1..45).map { task("n$it", projectId = "work") }
        val list = WidgetModels.list(WidgetFilter.ProjectList("work"), "Работа", tasks, projects, today, ZoneOffset.UTC)
        assertEquals(45, list.days.sumOf { it.items.size })
    }

    /** Итерация 42: лента построчно — подпись дня, строки шкалы с непрерывной линией, хвост под панелью в конце. */
    @Test
    fun linesFlattenDaysWithContinuousRail() {
        val all = AllScreenTasks(emptyList(), listOf(task("a", due = day(0)), task("b", due = day(0))), emptyList(), listOf(task("c", due = day(1))))
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)
        val lines = model.lines(com.prostovasei.simpletasks.ui.common.DayNames("Сегодня", "Завтра", "Вчера"))
        assertEquals(
            listOf("title:$today", "task:a:$today", "task:b:$today", "title:${today.plusDays(1)}", "task:c:${today.plusDays(1)}", "tail"),
            lines.map { it.key },
        )
        val rails = lines.filterIsInstance<WidgetLine.Task>().map { it.line }
        assertEquals(listOf(Line(false, true), Line(true, false), Line(false, false)), rails)
        assertTrue("Первая подпись — с отступом сверху", (lines.first() as WidgetLine.Title).isFirst)
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
    fun listGroupsByDayWithPastDaysFirstAndNoDateLast() {
        val tasks = listOf(
            task("none", projectId = "work"),
            task("next", projectId = "work", due = day(3)),
            task("late", projectId = "work", due = day(-1)),
            task("now", projectId = "work", due = day(0)),
        )
        val model = WidgetModels.list(WidgetFilter.ProjectList("work"), "Работа", tasks, projects, today, ZoneOffset.UTC)
        assertEquals("Работа", model.title)
        assertEquals(
            listOf(WidgetDay.On(today.minusDays(1)), WidgetDay.On(today), WidgetDay.On(today.plusDays(3)), WidgetDay.NoDate),
            model.days.map { it.day },
        )
    }

    @Test
    fun shortDates() {
        assertEquals("05 окт", WidgetDates.short(LocalDate.of(2026, 10, 5)))
        assertEquals("16 окт", WidgetDates.short(LocalDate.of(2026, 10, 16)))
        assertEquals("01 мая", WidgetDates.short(LocalDate.of(2026, 5, 1)))
        assertEquals("пт, 16 окт", WidgetDates.withWeekday(LocalDate.of(2026, 10, 16)))
        assertEquals("пн, 19 окт", WidgetDates.withWeekday(LocalDate.of(2026, 10, 19)))
    }

    private val lists = listOf(
        ProjectOption("inbox", "Inbox", null, isInbox = true),
        ProjectOption("work", "Работа", "#3D8BFD", isInbox = false),
        ProjectOption("home", "Дом", "#46A758", isInbox = false),
        ProjectOption("health", "Здоровье", null, isInbox = false),
    )

    @Test
    fun tabsShowAllInboxAndFirstTwoLists() {
        assertEquals(
            listOf(WidgetTab(WidgetFilter.All), WidgetTab(WidgetFilter.Inbox), WidgetTab(WidgetFilter.ProjectList("work"), "Работа", "#3D8BFD"), WidgetTab(WidgetFilter.ProjectList("home"), "Дом", "#46A758")),
            WidgetModels.tabs(WidgetFilter.All, lists),
        )
    }

    @Test
    fun listPickedFromMoreReplacesSecondListTab() {
        val tabs = WidgetModels.tabs(WidgetFilter.ProjectList("health"), lists).map { it.filter }
        assertEquals(listOf(WidgetFilter.All, WidgetFilter.Inbox, WidgetFilter.ProjectList("work"), WidgetFilter.ProjectList("health")), tabs)
    }

    @Test
    fun noOwnListsOnlyAllAndInbox() {
        assertEquals(listOf(WidgetFilter.All, WidgetFilter.Inbox), WidgetModels.tabs(WidgetFilter.Inbox, lists.take(1)).map { it.filter })
    }

    @Test
    fun filterIsStoredAsText() {
        listOf(WidgetFilter.All, WidgetFilter.Inbox, WidgetFilter.ProjectList("p-1")).forEach {
            assertEquals(it, WidgetFilter.decode(it.encode()))
        }
        assertEquals("По умолчанию — «Все»", WidgetFilter.All, WidgetFilter.decode(null))
    }

    @Test
    fun dayProgressCountsDone() {
        fun item(id: String, done: Boolean) = TaskItem(id, id, Priority.NONE, null, null, null, isInboxList = true, isDone = done)
        assertEquals(DayProgress(2, 3), DayProgress.of(listOf(item("a", true), item("b", false), item("c", true))))
        assertEquals(2f / 3, DayProgress(2, 3).fraction, 0.001f)
        assertEquals(null, DayProgress.of(emptyList()))
    }

    @Test
    fun overdueHeaderStandsAboveFirstPastDayAndCollapseHidesPastDays() {
        val all = AllScreenTasks(
            overdue = listOf(task("late1", due = day(-3)), task("late2", due = day(-1)), task("done", due = day(-1)).copy(status = com.prostovasei.simpletasks.domain.model.TaskStatus.COMPLETED)),
            todayAllDay = listOf(task("gym", due = day(0))),
            todayTimed = emptyList(),
            upcoming = emptyList(),
        )
        val model = WidgetModels.all(all, projects, today, ZoneOffset.UTC)

        assertEquals("Выполненная не считается", 2, model.overdueCount)
        val shape = { m: WidgetModel -> m.entries().map { if (it is WidgetEntry.Day) (it.block.day as WidgetDay.On).date.toString() else "overdue" } }
        assertEquals(listOf("overdue", "2026-10-11", "2026-10-13", "2026-10-14"), shape(model))
        assertEquals(listOf("overdue", "2026-10-14"), shape(model.copy(overdueCollapsed = true)))
        assertEquals(
            "В списке подписи «Просрочено» нет",
            listOf("2026-10-11", "2026-10-13", "2026-10-14"),
            shape(model.copy(filter = WidgetFilter.Inbox)),
        )
    }
}
