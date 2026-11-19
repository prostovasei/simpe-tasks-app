package com.prostovasei.simpletasks.domain.model.finance

import com.prostovasei.simpletasks.testing.operation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthSpendingTest {
    @Test
    fun projectsFromCalendarDaysAndGroupsUnassignedSpending() {
        val today = LocalDate.of(2026, 11, 19)
        val expenses = listOf(
            operation("a", 1_240_000, "2026-11-01", place = "Магазин"),
            operation("b", 1_600_000, "2026-11-19"),
            operation("future", 500_000, "2026-11-20"),
            operation("income", 500_000, "2026-11-19", OperationKind.INCOME),
        )
        val result = MonthSpending.of(expenses, YearMonth.from(today), today)
        assertEquals(19, result.cumulative.size)
        assertEquals(1_240_000L, result.cumulative[17])
        assertEquals(2_840_000L, result.total)
        assertEquals(4_484_211L, result.forecast)
        assertEquals(5_000_000L, result.ceiling)
        assertEquals(listOf("Магазин", "Остальное"), result.places.map { it.name })
        assertEquals(result.total, result.places.sumOf { it.amount })
        assertEquals(result.total, result.categories.sumOf { it.amount })
        assertNull(MonthSpending.of(expenses, YearMonth.from(today), today.withDayOfMonth(30)).forecast)
    }
}
