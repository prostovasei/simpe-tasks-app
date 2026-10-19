package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Шкала дня в виджете (итерация 20): подписи времени, отметка «сейчас», прошедшие задачи, следующее обновление. */
class WidgetTimelineTest {
    private val days = DayNames("Сегодня", "Завтра", "Вчера")

    private fun item(id: String, time: String?, day: DayLabel = DayLabel.Today) =
        TaskItem(id, id, Priority.NONE, DueText(day, time), null, null, isInboxList = true)

    private val allDay = item("net", null)
    private val office = item("office", "08:30")
    private val sync = item("sync", "11:00")
    private val food = item("food", "14:00–15:00")

    private fun List<WidgetRow>.ids() = map { (it as? WidgetRow.Task)?.item?.id ?: "now" }

    @Test
    fun nowGoesBeforeFirstTaskNotStartedYet() {
        val rows = WidgetTimeline.rows(listOf(allDay, office, sync, food), overdue = false, nowMinute = 10 * 60 + 17, days = days)
        assertEquals(listOf("net", "office", "now", "sync", "food"), rows.ids())
        val tasks = rows.filterIsInstance<WidgetRow.Task>().associateBy { it.item.id }
        assertEquals(true, tasks.getValue("office").past)
        assertEquals(false, tasks.getValue("sync").past)
        // Весь день: слева пусто, кружок не бледнеет.
        assertEquals(WidgetRow.Task(allDay, null, null), tasks.getValue("net"))
        // Период: начало и конец столбиком.
        assertEquals(WidgetRow.Task(food, "14:00", "15:00", isPeriod = true), tasks.getValue("food"))
    }

    @Test
    fun periodInProgressIsNotPastAndNowGoesAfterIt() {
        val rows = WidgetTimeline.rows(listOf(food, item("bus", "17:00")), overdue = false, nowMinute = 14 * 60 + 30, days = days)
        assertEquals(listOf("food", "now", "bus"), rows.ids())
        assertEquals(false, (rows.first() as WidgetRow.Task).past)
    }

    @Test
    fun nowAtEndWhenEverythingStartedAndAbsentWithoutTimedTasks() {
        assertEquals(listOf("office", "now"), WidgetTimeline.rows(listOf(office), false, 20 * 60, days).ids())
        assertEquals(listOf("net"), WidgetTimeline.rows(listOf(allDay), false, 20 * 60, days).ids())
        // Не «Сегодня» — без отметки и без бледных задач.
        val tomorrow = WidgetTimeline.rows(listOf(office), false, null, days)
        assertEquals(listOf(WidgetRow.Task(office, "08:30", null)), tomorrow)
    }

    @Test
    fun overdueShowsDayAndStartTime() {
        val late = item("late", "10:00–11:00", DayLabel.Yesterday)
        assertEquals(listOf(WidgetRow.Task(late, "Вчера", "10:00")), WidgetTimeline.rows(listOf(late), overdue = true, nowMinute = 600, days = days))
    }

    @Test
    fun nextChangeIsNearestStartOrEnd() {
        val items = listOf(allDay, office, sync, food)
        assertEquals(11 * 60, WidgetTimeline.nextChange(items, 10 * 60 + 17))
        // Конец периода: задача становится прошедшей через минуту после конца.
        assertEquals(15 * 60 + 1, WidgetTimeline.nextChange(items, 14 * 60 + 5))
        assertNull(WidgetTimeline.nextChange(items, 16 * 60))
        // Период через полночь не становится прошедшим сегодня.
        assertEquals(23 * 60, WidgetTimeline.nextChange(listOf(item("night", "23:00–01:00")), 22 * 60))
        assertNull(WidgetTimeline.nextChange(listOf(item("night", "23:00–01:00")), 23 * 60 + 30))
    }
}
