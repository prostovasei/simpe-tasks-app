package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.TaskDue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Ключ открытия будущего повтора (итерация 49): id серии и дата раза туда и обратно. */
class TaskOpenKeyTest {
    @Test
    fun roundTrips() {
        val day = TaskDue.AllDay(LocalDate.of(2026, 11, 22))
        val time = TaskDue.AtTime(Instant.parse("2026-11-22T11:30:00Z"))
        assertEquals("a-1" to day, TaskOpenKey.parse(TaskOpenKey.of("a-1", day)))
        assertEquals("a-1" to time, TaskOpenKey.parse(TaskOpenKey.of("a-1", time)))
        assertEquals("a-1" to null, TaskOpenKey.parse(TaskOpenKey.of("a-1", null)))
    }

    @Test
    fun brokenSuffixKeepsWholeKey() {
        assertEquals("a@x" to null, TaskOpenKey.parse("a@x"))
    }
}
