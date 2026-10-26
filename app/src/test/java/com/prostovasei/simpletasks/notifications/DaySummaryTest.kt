package com.prostovasei.simpletasks.notifications

import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Утренняя сводка (итерация 30): план дня, время следующей сводки и что прочитает голос. */
@RunWith(RobolectricTestRunner::class)
class DaySummaryTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.parse("2026-10-26")

    private fun at(time: String) = TaskDue.AtTime(Instant.parse("2026-10-26T${time}:00Z"))

    @Test
    fun planSortsTimedAndSkipsDone() {
        val all = AllScreenTasks(
            overdue = listOf(task("o1", due = TaskDue.AllDay(today.minusDays(1))), task("o2", status = TaskStatus.COMPLETED)),
            todayAllDay = listOf(task("buy", "Купить продукты", due = TaskDue.AllDay(today))),
            todayTimed = listOf(
                task("late", "Позвонить врачу", due = at("12:30")),
                task("call", "Созвон", due = at("11:00"), start = at("10:00")),
                task("done", "Зарядка", due = at("08:00"), status = TaskStatus.COMPLETED),
            ),
            upcoming = emptyList(),
        )
        val plan = DayPlan.of(all, zone)
        assertEquals(listOf(DayPlan.TimedTask("10:00", "Созвон"), DayPlan.TimedTask("12:30", "Позвонить врачу")), plan.timed)
        assertEquals(listOf("Купить продукты"), plan.allDay)
        assertEquals(1, plan.overdue)
        assertEquals(3, plan.total)
    }

    @Test
    fun nextAtIsTodayOrTomorrow() {
        val before = Instant.parse("2026-10-26T07:30:00Z")
        val after = Instant.parse("2026-10-26T08:00:00Z")
        assertEquals(Instant.parse("2026-10-26T08:00:00Z"), DaySummary.nextAt(before, zone, 8 * 60))
        assertEquals(Instant.parse("2026-10-27T08:00:00Z"), DaySummary.nextAt(after, zone, 8 * 60))
    }

    @Test
    fun speechReadsPlanInOrder() {
        val summary = DaySummary(ApplicationProvider.getApplicationContext(), FakeTaskRepository(), FakeTimeProvider(Instant.parse("2026-10-26T08:00:00Z"), zone))
        val plan = DayPlan(listOf(DayPlan.TimedTask("10:00", "Созвон")), listOf("Купить продукты", "Почта"), overdue = 2)
        assertEquals(
            listOf(
                "Доброе утро! На сегодня 3 задачи.",
                "В 10:00 — Созвон.",
                "Без времени: Купить продукты, Почта.",
                "Ещё просрочено: 2 задачи.",
                "Хорошего дня!",
            ),
            summary.speech(plan, hour = 8),
        )
        assertTrue(summary.speech(DayPlan(emptyList(), emptyList(), 0), hour = 20).first().startsWith("Добрый вечер! На сегодня задач нет."))
    }
}
