package com.prostovasei.simpletasks.ui.feature.finance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** #336: сумма в день в полосе лимита хаба — «≈потрачено/день» и «можно/день», округлённые до сотен. */
class DailyNotesTest {
    private val today = LocalDate.of(2026, 11, 25)
    private val month = YearMonth.from(today)

    @Test
    fun spentAndLeftPerDay() {
        // 54 000 ₽ за 25 дней ≈ 2 200 ₽; осталось 26 000 ₽ на 6 дней (25–30) ≈ 4 300 ₽.
        val notes = DailyNotes.of(5_400_000, 8_000_000, month, today, 0.675f)
        assertEquals("≈2 200/день", notes.spent)
        assertEquals("4 300/день", notes.left)
    }

    @Test
    fun shortFillOrOverspendHideNotes() {
        assertEquals(null, DailyNotes.of(500_000, 8_000_000, month, today, 0.06f).spent)
        assertEquals(null, DailyNotes.of(9_000_000, 8_000_000, month, today, 1f).left)
        assertEquals(null, DailyNotes.of(5_400_000, null, month, today, 0f).left)
    }

    @Test
    fun pastMonthHasNoLeftNote() {
        assertEquals(null, DailyNotes.of(5_400_000, 8_000_000, YearMonth.of(2026, 10), today, 0.675f).left)
    }
}
