package com.prostovasei.simpletasks.domain.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Распознавание покупок Яндекс Банка (#233, requirements.md §2.1). Тексты — вымышленные копии видов со скриншота PM:
 * суммы, магазины и номера карт придуманы.
 */
class BankPurchaseParserTest {
    private val bank = "com.yandex.bank"

    private fun parse(text: String, pkg: String = bank) = BankPurchaseParser.parse(pkg, listOf(text))

    @Test
    fun purchaseWithCardIsExpense() {
        assertEquals(68_000L, parse("Покупка на 680.00 RUB, карта *1234. Доступно 4865.00 RUB"))
    }

    @Test
    fun purchaseWithoutCardIsExpense() {
        assertEquals(180_000L, parse("Покупка на 1800.00 RUB. Доступно 545.00 RUB"))
    }

    @Test
    fun kopecksAndBigAmountsAreKept() {
        assertEquals(123_450L, parse("Покупка на 1234.50 RUB, карта *0000. Доступно 10.00 RUB"))
        assertEquals(12_345_699L, parse("Покупка на 123 456.99 RUB. Доступно 1.00 RUB"))
        assertEquals(99L, parse("Покупка на 0.99 RUB"))
    }

    @Test
    fun transfersAndTopUpsAreSkipped() {
        assertNull(parse("Перевод на Сейв \"КОПИЛКА\". Баланс карты: 4845 ₽"))
        assertNull(parse("Пополнение с карты Пэй. Баланс накопительного счёта: 120 ₽"))
        assertNull(parse("Перевод на карту Пэй. Баланс Сейва: 0 ₽"))
        assertNull(parse("Пополнение с Сейва \"МОЖНО\". Баланс карты: 5545 ₽"))
    }

    @Test
    fun unknownOrBrokenTextsAreSkipped() {
        assertNull(parse("Покупка на 680 ₽"))
        assertNull(parse("Покупка на 680.00 USD, карта *1234"))
        assertNull(parse("Отмена покупки на 680.00 RUB"))
        assertNull(parse("Покупка на 680.0 RUB"))
        assertNull(parse("Покупка на 0.00 RUB"))
        assertNull(parse("Покупка на 680.00 RUBLES"))
        assertNull(parse(""))
    }

    @Test
    fun onlyYandexAppsAreRead() {
        val text = "Покупка на 680.00 RUB, карта *1234. Доступно 4865.00 RUB"
        assertNull(parse(text, "ru.sberbankmobile"))
        assertNull(parse(text, "com.yandexx.fake"))
        assertEquals(68_000L, parse(text, "com.yandex.pay"))
        assertTrue(BankPurchaseParser.isBankApp("com.yandex.bank"))
        assertFalse(BankPurchaseParser.isBankApp("com.tinkoff"))
    }

    @Test
    fun expandedTextIsUsedWhenShortTextIsEmpty() {
        assertEquals(68_000L, BankPurchaseParser.parse(bank, listOf(null, "Покупка на 680.00 RUB. Доступно 1.00 RUB")))
    }
}
