package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.TaskDue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskFormTest {
    private val zone = ZoneId.of("Asia/Krasnoyarsk")
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun newDateKeepsTime() {
        val at10 = TaskDue.AtTime(Instant.parse("2026-10-14T03:00:00Z"))
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-20T03:00:00Z")), withDate(at10, LocalDate.of(2026, 10, 20), zone))
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 20)), withDate(TaskDue.AllDay(today), LocalDate.of(2026, 10, 20), zone))
        assertEquals(TaskDue.AllDay(today), withDate(null, today, zone))
    }

    @Test
    fun newTimeKeepsDate() {
        val tomorrow = TaskDue.AllDay(today.plusDays(1))
        // 18:30 по Красноярску = 11:30 UTC.
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z")), withTime(tomorrow, LocalTime.of(18, 30), zone, today))
        assertEquals(tomorrow, withTime(TaskDue.AtTime(Instant.parse("2026-10-15T11:30:00Z")), null, zone, today))
        assertEquals(TaskDue.AtTime(Instant.parse("2026-10-14T02:00:00Z")), withTime(null, LocalTime.of(9, 0), zone, today))
    }
}
