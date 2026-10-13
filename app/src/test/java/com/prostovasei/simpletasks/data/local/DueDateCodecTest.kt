package com.prostovasei.simpletasks.data.local

import com.prostovasei.simpletasks.domain.model.TaskDue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DueDateCodecTest {

    @Test
    fun allDayIsStoredAsUtcMidnightOfCalendarDate() {
        val encoded = DueDateCodec.encode(TaskDue.AllDay(LocalDate.of(2026, 10, 13)))
        assertEquals(Instant.parse("2026-10-13T00:00:00Z").toEpochMilli(), encoded.dueDate)
        assertTrue(encoded.isAllDay)
    }

    @Test
    fun timedIsStoredAsExactInstant() {
        val instant = Instant.parse("2026-10-13T12:34:56Z")
        val encoded = DueDateCodec.encode(TaskDue.AtTime(instant))
        assertEquals(instant.toEpochMilli(), encoded.dueDate)
        assertEquals(false, encoded.isAllDay)
    }

    @Test
    fun noDueDateUsesSpecDefaults() {
        val encoded = DueDateCodec.encode(null)
        assertNull(encoded.dueDate)
        assertTrue(encoded.isAllDay)
        assertNull(DueDateCodec.decode(null, isAllDay = true))
    }

    @Test
    fun roundTripKeepsValue() {
        listOf(
            TaskDue.AllDay(LocalDate.of(2026, 1, 1)),
            TaskDue.AllDay(LocalDate.of(1969, 12, 31)),
            TaskDue.AtTime(Instant.parse("2026-10-13T23:59:00Z")),
        ).forEach { due ->
            val encoded = DueDateCodec.encode(due)
            assertEquals(due, DueDateCodec.decode(encoded.dueDate, encoded.isAllDay))
        }
    }

    @Test
    fun allDayWindowDoesNotDependOnTimeZone() {
        val date = LocalDate.of(2026, 10, 13)
        val east = DayWindow.of(date, ZoneId.of("Pacific/Kiritimati"))
        val west = DayWindow.of(date, ZoneId.of("Etc/GMT+12"))
        assertEquals(east.allDayStart, west.allDayStart)
        assertEquals(east.allDayEnd, west.allDayEnd)
    }

    @Test
    fun timedWindowFollowsUserTimeZone() {
        val window = DayWindow.of(LocalDate.of(2026, 10, 13), ZoneId.of("Europe/Moscow"))
        assertEquals(Instant.parse("2026-10-12T21:00:00Z").toEpochMilli(), window.start)
        assertEquals(Instant.parse("2026-10-13T21:00:00Z").toEpochMilli(), window.end)
    }

    @Test
    fun daylightSavingDayHas23Hours() {
        val window = DayWindow.of(LocalDate.of(2026, 3, 29), ZoneId.of("Europe/Berlin"))
        assertEquals(23 * 3_600_000L, window.end - window.start)
    }
}
