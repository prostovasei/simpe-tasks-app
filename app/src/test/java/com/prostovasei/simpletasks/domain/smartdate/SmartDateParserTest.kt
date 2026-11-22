package com.prostovasei.simpletasks.domain.smartdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Дата и время в названии (итерация 41, бэклог №29): что распознаём, что не трогаем, что остаётся от названия. */
class SmartDateParserTest {
    // Среда, 14 октября 2026, 09:00.
    private val today = LocalDate.of(2026, 10, 14)
    private val now = 9 * 60

    private fun parse(text: String) = SmartDateParser.parse(text, today, now)

    private fun clean(text: String): String {
        val found = checkNotNull(parse(text)) { "Не распознано: $text" }
        return SmartDateParser.strip(text, found.ranges)
    }

    private fun assertDate(text: String, date: LocalDate, minute: Int? = null) {
        val found = checkNotNull(parse(text)) { "Не распознано: $text" }
        assertEquals(text, date, found.date)
        assertEquals(text, minute, found.minute)
    }

    @Test
    fun relativeDays() {
        assertDate("Созвон сегодня", today)
        assertDate("Созвон завтра", today.plusDays(1))
        assertDate("Созвон послезавтра", today.plusDays(2))
        assertDate("Отчёт на завтра", today.plusDays(1))
        assertEquals("Отчёт", clean("Отчёт на завтра"))
    }

    /** #95: так голосовой ввод Google обычно записывает фразы со сроком. */
    @Test
    fun voicePhrases() {
        assertDate("Позвонить маме завтра в 10", today.plusDays(1), 10 * 60)
        assertDate("Позвонить маме завтра в 10:00", today.plusDays(1), 10 * 60)
        assertDate("Позвонить врачу завтра в 10 часов", today.plusDays(1), 10 * 60)
        assertDate("Забрать посылку завтра в 7 вечера", today.plusDays(1), 19 * 60)
        assertDate("Купить хлеб послезавтра в 9 утра", today.plusDays(2), 9 * 60)
        assertDate("Встреча в пятницу в 18:30", today.plusDays(2), 18 * 60 + 30)
        assertEquals("Позвонить маме", clean("Позвонить маме завтра в 10"))
        assertEquals("Забрать посылку", clean("Забрать посылку завтра в 7 вечера"))
        // Время сразу после даты, а дело после времени — «в N» здесь время, даже если дальше длинное слово.
        assertDate("Завтра в 10 позвонить маме", today.plusDays(1), 10 * 60)
        assertEquals("Заглавная буква названия сохраняется", "Позвонить маме", clean("Завтра в 10 позвонить маме"))
        assertEquals("купить молоко", clean("завтра купить молоко"))
        assertDate("В пятницу в 18 созвон", today.plusDays(2), 18 * 60)
        // Без даты рядом «в N слово» по-прежнему не время.
        assertDate("Завтра купить подарки в 5 магазинах", today.plusDays(1), null)
    }

    @Test
    fun dateAndTimeTogether() {
        assertDate("Созвон завтра 10:00", today.plusDays(1), 10 * 60)
        assertDate("Созвон завтра в 10:00", today.plusDays(1), 10 * 60)
        assertDate("Созвон в 10:30 завтра", today.plusDays(1), 10 * 60 + 30)
        assertEquals("Созвон", clean("Созвон завтра в 10:00"))
        assertEquals("Созвон с командой", clean("Созвон завтра в 10:00 с командой"))
    }

    @Test
    fun weekdays() {
        assertDate("Отчёт в пятницу", LocalDate.of(2026, 10, 16))
        assertDate("Отчёт к понедельнику", LocalDate.of(2026, 10, 19))
        assertDate("Отчёт в среду", today)
        assertDate("Отчёт в следующую среду", LocalDate.of(2026, 10, 21))
        assertDate("Отчёт в следующий понедельник", LocalDate.of(2026, 10, 19))
        assertDate("Отчёт в следующую пятницу", LocalDate.of(2026, 10, 23))
        assertEquals("Отчёт", clean("Отчёт в пятницу"))
    }

    @Test
    fun inDaysAndWeeks() {
        assertDate("Позвонить через 3 дня", today.plusDays(3))
        assertDate("Позвонить через день", today.plusDays(1))
        assertDate("Позвонить через неделю", today.plusWeeks(1))
        assertDate("Позвонить через 2 недели", today.plusWeeks(2))
        assertEquals("Позвонить", clean("Позвонить через 3 дня"))
    }

    @Test
    fun calendarDates() {
        assertDate("Билеты 15 ноября", LocalDate.of(2026, 11, 15))
        assertDate("Билеты 15.11", LocalDate.of(2026, 11, 15))
        assertDate("Билеты 3 мая", LocalDate.of(2027, 5, 3))
        assertDate("Билеты 01.02.2027", LocalDate.of(2027, 2, 1))
        assertDate("Сдать отчёт к 20 октября в 18:00", LocalDate.of(2026, 10, 20), 18 * 60)
        assertEquals("Билеты", clean("Билеты 15 ноября"))
    }

    @Test
    fun timeOnlyIsTodayOrTomorrowIfPassed() {
        assertDate("Позвонить маме в 18:00", today, 18 * 60)
        assertDate("Позвонить маме в 8:00", today.plusDays(1), 8 * 60)
        assertDate("Позвонить в 18", today, 18 * 60)
        assertDate("Позвонить в 7 вечера", today, 19 * 60)
        assertDate("Позвонить в 3 часа дня", today, 15 * 60)
        assertDate("Позвонить в 18 с работы", today, 18 * 60)
    }

    @Test
    fun ambiguousPhrasesAreLeftAlone() {
        assertNull(parse("Купить 5 яиц"))
        assertNull(parse("Вторник — фильм"))
        assertNull(parse("Книга про март"))
        assertNull(parse("Купить в 5 магазинах"))
        assertNull(parse("Завтрак с семьёй"))
        assertNull(parse("Пятница"))
        assertNull(parse("Купить 30 мандаринов"))
        assertNull(parse("Комната 25:00"))
    }

    @Test
    fun stripKeepsTheRestTidy() {
        assertEquals("Созвон, потом обед", clean("Созвон завтра, потом обед"))
        assertEquals("Созвон", clean("Созвон — завтра"))
        assertEquals("", clean("завтра"))
    }
}
