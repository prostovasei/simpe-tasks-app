package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.TaskDue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DueFormatterTest {
    private val today = LocalDate.of(2026, 10, 13)
    private val zone = ZoneId.of("Asia/Krasnoyarsk")

    private fun at(text: String) = TaskDue.AtTime(Instant.parse(text))

    @Test
    fun relativeDays() {
        assertEquals(DayLabel.Today, DueFormatter.format(TaskDue.AllDay(today), null, today, zone)!!.day)
        assertEquals(DayLabel.Tomorrow, DueFormatter.format(TaskDue.AllDay(today.plusDays(1)), null, today, zone)!!.day)
        assertEquals(DayLabel.Yesterday, DueFormatter.format(TaskDue.AllDay(today.minusDays(1)), null, today, zone)!!.day)
        assertEquals(DayLabel.Date(LocalDate.of(2026, 11, 5), showYear = false), DueFormatter.format(TaskDue.AllDay(LocalDate.of(2026, 11, 5)), null, today, zone)!!.day)
        assertEquals(DayLabel.Date(LocalDate.of(2027, 1, 2), showYear = true), DueFormatter.format(TaskDue.AllDay(LocalDate.of(2027, 1, 2)), null, today, zone)!!.day)
    }

    @Test
    fun allDayHasNoTime() {
        assertNull(DueFormatter.format(TaskDue.AllDay(today), null, today, zone)!!.time)
        assertNull(DueFormatter.format(null, null, today, zone))
    }

    @Test
    fun timeIsShownInUserTimeZone() {
        // 03:00 UTC — это 10:00 в Красноярске.
        assertEquals(DueText(DayLabel.Today, "10:00"), DueFormatter.format(at("2026-10-13T03:00:00Z"), null, today, zone))
    }

    @Test
    fun intervalShowsStartAndEndAndUsesStartDay() {
        val text = DueFormatter.format(at("2026-10-13T04:00:00Z"), at("2026-10-13T03:00:00Z"), today, zone)
        assertEquals(DueText(DayLabel.Today, "10:00–11:00"), text)

        // Интервал через полночь: день — по началу (решение PM).
        val overnight = DueFormatter.format(at("2026-10-13T18:00:00Z"), at("2026-10-13T16:00:00Z"), today, zone)
        assertEquals(DueText(DayLabel.Today, "23:00–01:00"), overnight)
    }
}
