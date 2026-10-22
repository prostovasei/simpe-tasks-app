package com.prostovasei.simpletasks.domain.model

import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun badgeShowsKindOfReminder() {
        val timed = TaskDue.AtTime(local(15, 18))
        val allDay = TaskDue.AllDay(LocalDate.of(2026, 10, 15))
        assertEquals(ReminderBadge.AtDue, Reminders.badge(timed, listOf("PT0S")))
        assertEquals(ReminderBadge.Before(BeforeLabel.MINUTES_30), Reminders.badge(timed, listOf("-PT30M")))
        assertEquals(ReminderBadge.Before(BeforeLabel.DAYS_3), Reminders.badge(timed, listOf("-PT72H")))
        assertEquals("Старый вариант «за 5 минут» — вручную", ReminderBadge.Custom, Reminders.badge(timed, listOf("-PT5M")))
        assertEquals(ReminderBadge.AtDayPart(DayPart.MORNING), Reminders.badge(allDay, listOf("P0DT10H0M0S")))
        assertEquals(ReminderBadge.AtDayPart(DayPart.NIGHT), Reminders.badge(allDay, listOf("P0DT22H0M0S")))
        assertEquals(ReminderBadge.Before(BeforeLabel.DAY_1), Reminders.badge(allDay, listOf("-P0DT14H0M0S")))
        assertEquals(ReminderBadge.Custom, Reminders.badge(allDay, listOf("-P6DT14H0M0S")))
        assertEquals(ReminderBadge.Several(2), Reminders.badge(timed, listOf("PT0S", "-PT1H")))
        assertEquals("Одинаковые в разной записи — одно", ReminderBadge.AtDue, Reminders.badge(timed, listOf("PT0S", "-PT0S")))
        assertNull(Reminders.badge(timed, emptyList()))
        assertNull(Reminders.badge(null, listOf("PT0S")))
    }

    @Test
    fun customRemindersInTickTickFormat() {
        assertEquals("PT0S", Reminders.timed(0))
        assertEquals("-P2DT5H30M0S", Reminders.timed(2 * 1440L + 5 * 60 + 30))
        assertEquals("P0DT9H0M0S", Reminders.allDay(0, 9 * 60))
        assertEquals("-P0DT15H0M0S", Reminders.allDay(1, 9 * 60))
        assertEquals(AllDayReminder(daysBefore = 3, minuteOfDay = 8 * 60 + 45), allDayReminder(Duration.parse(Reminders.allDay(3, 8 * 60 + 45))))
        val t = task("t", due = TaskDue.AtTime(local(25, 18)))
        assertEquals(local(23, 12, 30), Reminders.instant(t.due!!, null, Reminders.timed(2 * 1440L + 5 * 60 + 30), moscow))
    }
}
