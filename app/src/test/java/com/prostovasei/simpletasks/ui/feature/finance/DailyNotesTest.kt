package com.prostovasei.simpletasks.ui.feature.finance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** #336/#338: сумма в день под полосой лимита хаба — «потрачено/день» и «можно/день», округлённые до сотен. */
class DailyNotesTest {
    private val today = LocalDate.of(2026, 11, 25)
    private val month = YearMonth.from(today)

    @Test
    fun spentAndLeftPerDay() {
        // 54 000 ₽ за 25 дней ≈ 2 200 ₽; осталось 26 000 ₽ на 6 дней (25–30) ≈ 4 300 ₽.
        val notes = DailyNotes.of(5_400_000, 8_000_000, month, today)
        assertEquals("2 200/день", notes.spent)
        assertEquals("4 300/день", notes.left)
    }

    @Test
    fun noSpendingOrOverspendHideNotes() {
        // #338: подписи под полосой — малые траты тоже показываются, пропадают только без трат.
        assertEquals("200/день", DailyNotes.of(500_000, 8_000_000, month, today).spent)
        assertEquals(null, DailyNotes.of(0, 8_000_000, month, today).spent)
        assertEquals(null, DailyNotes.of(9_000_000, 8_000_000, month, today).left)
        assertEquals(null, DailyNotes.of(5_400_000, null, month, today).left)
    }

    @Test
    fun pastMonthHasNoLeftNote() {
        assertEquals(null, DailyNotes.of(5_400_000, 8_000_000, YearMonth.of(2026, 10), today).left)
    }
}
