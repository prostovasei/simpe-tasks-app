package com.prostovasei.simpletasks.domain.model

import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class TaskEditsTest {
    private fun at(text: String) = TaskDue.AtTime(Instant.parse(text))

    @Test
    fun intervalMovesWithDue() {
        val call = task("t", start = at("2026-10-14T03:00:00Z"), due = at("2026-10-14T04:00:00Z"))
        val moved = call.withDue(at("2026-10-15T06:00:00Z"))
        assertEquals(at("2026-10-15T05:00:00Z"), moved.start)
        assertEquals(Duration.ofHours(1), Duration.between((moved.start as TaskDue.AtTime).instant, (moved.due as TaskDue.AtTime).instant))
    }

    @Test
    fun allDayRangeMovesByDays() {
        val trip = task("t", start = TaskDue.AllDay(LocalDate.of(2026, 10, 10)), due = TaskDue.AllDay(LocalDate.of(2026, 10, 12)))
        val moved = trip.withDue(TaskDue.AllDay(LocalDate.of(2026, 10, 20)))
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 18)), moved.start)
    }

    @Test
    fun changingKindOrRemovingDueDropsInterval() {
        val call = task("t", start = at("2026-10-14T03:00:00Z"), due = at("2026-10-14T04:00:00Z")).copy(rrule = "FREQ=DAILY")
        assertNull(call.withDue(TaskDue.AllDay(LocalDate.of(2026, 10, 14))).start)
        val noDate = call.withDue(null)
        assertNull(noDate.start)
        assertNull("Без срока повтор не имеет смысла", noDate.rrule)
    }

    @Test
    fun remindersParse() {
        assertEquals(Duration.ZERO, parseReminder("PT0S"))
        assertEquals(Duration.ofDays(-1), parseReminder("-P1D"))
        assertEquals(Duration.ofDays(-7), parseReminder("-PT10080M"))
        assertNull(parseReminder("за час"))
    }
}
