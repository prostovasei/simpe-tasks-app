package com.prostovasei.simpletasks.domain.model

import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RemindersTest {
    private val moscow = ZoneId.of("Europe/Moscow")
    private fun local(d: Int, h: Int, m: Int = 0): Instant = LocalDateTime.of(2026, 10, d, h, m).atZone(moscow).toInstant()

    @Test
    fun timedRemindersCountFromDue() {
        val t = task("t", due = TaskDue.AtTime(local(15, 18))).copy(reminders = listOf("PT0S", "-PT30M", "-P1D"))
        assertEquals(listOf(local(14, 18), local(15, 17, 30), local(15, 18)), Reminders.instants(t, moscow))
    }

    @Test
    fun intervalRemindsFromStart() {
        val t = task("t", start = TaskDue.AtTime(local(15, 10)), due = TaskDue.AtTime(local(15, 11))).copy(reminders = listOf("PT0S"))
        assertEquals(listOf(local(15, 10)), Reminders.instants(t, moscow))
    }

    @Test
    fun allDayRemindersCountFromMidnight() {
        val t = task("t", due = TaskDue.AllDay(LocalDate.of(2026, 10, 15)))
            .copy(reminders = listOf("P0DT10H0M0S", "-P0DT14H0M0S", "P0DT9H0M0S"))
        assertEquals(listOf(local(14, 10), local(15, 9), local(15, 10)), Reminders.instants(t, moscow))
    }

    @Test
    fun noDueNoReminders() {
        assertTrue(Reminders.instants(task("t").copy(reminders = listOf("PT0S")), moscow).isEmpty())
    }

    @Test
    fun sameMomentIgnoresSpelling() {
        assertTrue(Reminders.sameMoment("PT0S", "-PT0S"))
        assertTrue(Reminders.sameMoment("-P1D", "-PT1440M"))
    }

    @Test
    fun allDayOffsetsReadable() {
        assertEquals(AllDayReminder(daysBefore = 0, minuteOfDay = 600), allDayReminder(Duration.parse("P0DT10H0M0S")))
        assertEquals(AllDayReminder(daysBefore = 1, minuteOfDay = 600), allDayReminder(Duration.parse("-P0DT14H0M0S")))
        assertEquals(AllDayReminder(daysBefore = 7, minuteOfDay = 600), allDayReminder(Duration.parse("-P6DT14H0M0S")))
    }

    @Test
    fun defaultsFollowDueKind() {
        assertEquals(listOf("P0DT10H0M0S"), Reminders.default(TaskDue.AllDay(LocalDate.of(2026, 10, 15))))
        assertEquals(listOf("PT0S"), Reminders.default(TaskDue.AtTime(local(15, 18))))
        assertTrue(Reminders.default(null).isEmpty())
    }

    @Test
    fun dueChangesAdjustReminders() {
        val allDay = TaskDue.AllDay(LocalDate.of(2026, 10, 15))
        val noDate = task("t")
        assertEquals("Назначили срок — напоминание по умолчанию", listOf("P0DT10H0M0S"), noDate.withDue(allDay).reminders)

        val custom = task("t", due = allDay).copy(reminders = listOf("-P0DT14H0M0S"))
        assertEquals("Тот же вид срока — напоминания не трогаем", listOf("-P0DT14H0M0S"), custom.withDue(TaskDue.AllDay(LocalDate.of(2026, 10, 20))).reminders)
        assertEquals("Добавили время — напоминание в момент срока", listOf("PT0S"), custom.withDue(TaskDue.AtTime(local(15, 18))).reminders)
        assertTrue("Сняли срок — напоминаний нет", custom.withDue(null).reminders.isEmpty())

        val off = task("t", due = allDay)
        assertTrue("Отключённое напоминание не включается само при смене вида", off.withDue(TaskDue.AtTime(local(15, 18))).reminders.isEmpty())
    }
}
