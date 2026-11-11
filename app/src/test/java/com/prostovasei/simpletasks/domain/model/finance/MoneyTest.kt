package com.prostovasei.simpletasks.domain.model.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Рубли в копейках (финансы, #221): вывод, ввод и разбор введённого. */
class MoneyTest {
    private val s = ' '

    @Test
    fun formatsRublesWithThousandsAndOptionalKopecks() {
        assertEquals("0${s}₽", Money.format(0))
        assertEquals("850${s}₽", Money.format(85_000))
        assertEquals("42${s}300${s}₽", Money.format(4_230_000))
        assertEquals("1${s}234${s}567,05${s}₽", Money.format(123_456_705))
        assertEquals("42${s}300,50${s}₽", Money.format(4_230_050))
        assertEquals("−1${s}200${s}₽", Money.format(-120_000))
        assertEquals("+150${s}₽", Money.format(15_000, sign = true))
        assertEquals("0${s}₽", Money.format(0, sign = true))
    }

    @Test
    fun parsesTypedAmountToKopecks() {
        assertEquals(0L, Money.parse(""))
        assertEquals(4_230_000L, Money.parse("42300"))
        assertEquals(4_230_000L, Money.parse("42 300"))
        assertEquals(123_450L, Money.parse("1234,5"))
        assertEquals(123_405L, Money.parse("1234.05"))
        assertEquals(1_000L, Money.parse("10,"))
        assertNull(Money.parse("12,345"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
    }

    @Test
    fun inputKeepsOnlyDigitsOneCommaAndTwoDecimals() {
        assertEquals("1234,56", Money.filterInput("1 234,567"))
        assertEquals("12,3", Money.filterInput("12.3"))
        assertEquals("0,5", Money.filterInput(",5"))
        assertEquals("12,34", Money.filterInput("12,3,4"))
        assertEquals("", Money.filterInput("руб"))
        assertEquals("1234567890", Money.filterInput("12345678901234"))
    }

    @Test
    fun editTextRoundTrips() {
        assertEquals("42300", Money.input(4_230_000))
        assertEquals("42300,50", Money.input(4_230_050))
        assertEquals(4_230_050L, Money.parse(Money.input(4_230_050)))
    }
}
