package com.prostovasei.simpletasks.domain.model.finance

import com.prostovasei.simpletasks.testing.operation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Аналитика расходов за месяц (этап 3, #229): периоды сравнения, категории, доли и проценты. */
class MonthAnalyticsTest {
    private val today = LocalDate.parse("2026-11-10")
    private val november = YearMonth.of(2026, 11)

    private val operations = listOf(
        operation("n1", 2_100_000, "2026-11-03", category = "Продукты"),
        operation("n2", 1_000_000, "2026-11-10", category = "Кафе"),
        operation("n3", 680_000, "2026-11-05", category = "Подарки"),
        operation("n4", 500_000, "2026-11-01"),
        // Не расходы и не этот период.
        operation("n5", 6_500_000, "2026-11-02", OperationKind.INCOME, category = "Зарплата"),
        operation("n6", 500_000, "2026-11-04", OperationKind.TRANSFER, to = "Наличные"),
        operation("n7", 990_000, "2026-11-11", category = "Продукты"),
        operation("o1", 1_800_000, "2026-10-10", category = "Продукты"),
        operation("o2", 1_100_000, "2026-10-02", category = "Кафе"),
        operation("o3", 400_000, "2026-10-01"),
        operation("o4", 700_000, "2026-10-09", category = "Транспорт"),
        operation("o5", 5_000_000, "2026-10-11", category = "Продукты"),
        operation("s1", 300_000, "2026-09-15", category = "Кафе"),
    )

    /** Текущий месяц — по сегодняшний день, сравнение с теми же днями прошлого месяца. */
    @Test
    fun currentMonthComparesSameDaysOfPreviousMonth() {
        val a = MonthAnalytics.of(operations, november, today)

        assertTrue(a.partial)
        assertEquals(10 to 10, a.lastDay to a.previousLastDay)
        // 11 ноября ещё «не наступило» для периода 1–10; 11 октября не входит в 1–10 октября.
        assertEquals(4_280_000L, a.total)
        assertEquals(4_000_000L, a.previousTotal)
        assertEquals(
            listOf(
                CategorySpend("c-Продукты", "Продукты", 2_100_000, 1_800_000, 49),
                CategorySpend("c-Кафе", "Кафе", 1_000_000, 1_100_000, 23),
                CategorySpend("c-Подарки", "Подарки", 680_000, 0, 16),
                CategorySpend(null, null, 500_000, 400_000, 12),
            ),
            a.categories,
        )
    }

    /** Завершённый месяц сравнивается целиком; категории без расходов за месяц («Подарки» в октябре) не показываются. */
    @Test
    fun finishedMonthComparesWholeMonths() {
        val a = MonthAnalytics.of(operations, YearMonth.of(2026, 10), today)

        assertFalse(a.partial)
        assertEquals(31 to 30, a.lastDay to a.previousLastDay)
        assertEquals(9_000_000L, a.total)
        assertEquals(300_000L, a.previousTotal)
        assertEquals(listOf("Продукты", "Кафе", "Транспорт", null), a.categories.map { it.name })
        assertEquals(listOf(0L, 300_000L, 0L, 0L), a.categories.map { it.previous })
        assertEquals(listOf(76, 12, 8, 4), a.categories.map { it.share })
    }

    /** В прошлом месяце дней меньше: 1–31 марта сравнивается с 1–28 февраля. */
    @Test
    fun shorterPreviousMonthIsCappedAtItsLastDay() {
        val ops = listOf(operation("m", 100, "2027-03-31"), operation("f", 70, "2027-02-28"), operation("j", 5, "2027-01-31"))
        val a = MonthAnalytics.of(ops, YearMonth.of(2027, 3), LocalDate.parse("2027-03-31"))

        assertTrue(a.partial)
        assertEquals(31 to 28, a.lastDay to a.previousLastDay)
        assertEquals(100L to 70L, a.total to a.previousTotal)
    }

    @Test
    fun emptyMonthHasNoCategories() {
        val a = MonthAnalytics.of(operations, YearMonth.of(2026, 12), LocalDate.parse("2027-01-05"))
        assertEquals(0L, a.total)
        assertEquals(5_270_000L, a.previousTotal)
        assertTrue(a.categories.isEmpty())
    }

    @Test
    fun percentHasOneDecimalAndNoneForZeroBase() {
        assertEquals("+9,2%", MonthAnalytics.percent(42_800, 39_200))
        assertEquals("−9,1%", MonthAnalytics.percent(10_000, 11_000))
        assertEquals("+16,7%", MonthAnalytics.percent(21_000, 18_000))
        assertEquals("−100,0%", MonthAnalytics.percent(0, 39_200))
        assertEquals("+0,0%", MonthAnalytics.percent(500, 500))
        assertNull(MonthAnalytics.percent(6_800, 0))
    }
}
