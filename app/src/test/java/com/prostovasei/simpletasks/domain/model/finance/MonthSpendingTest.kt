package com.prostovasei.simpletasks.domain.model.finance

import com.prostovasei.simpletasks.testing.operation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthSpendingTest {
    @Test
    fun roundsSharesToExactlyOneHundredInRankOrder() {
        val rows = listOf(8900L, 6860L, 5270L, 4220L, 3150L).mapIndexed { index, amount ->
            MonthSpending.Rank(index.toString(), amount)
        }
        assertEquals(listOf(31, 24, 19, 15, 11), sharePercentages(rows, 28_400))
        assertEquals(listOf(34, 33, 33), sharePercentages(rows.take(3).map { it.copy(amount = 1) }, 3))
        assertEquals(listOf(100, 0), sharePercentages(
            listOf(MonthSpending.Rank("a", Long.MAX_VALUE - 1), MonthSpending.Rank("b", 1)), Long.MAX_VALUE))
        assertEquals("−4k", financeThousands(-350_000))
        assertEquals("0k", financeThousands(-49_999))
    }

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
        assertEquals(listOf("Остальное", "Магазин"), result.places.map { it.name })
        assertEquals(result.total, result.places.sumOf { it.amount })
        assertEquals(result.total, result.categories.sumOf { it.amount })
        assertNull(MonthSpending.of(expenses, YearMonth.from(today), today.withDayOfMonth(30)).forecast)
    }
}
