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

    private fun parse(text: String, pkg: String = bank, title: String? = null) = BankPurchaseParser.parse(pkg, title, listOf(text))

    private val pay = "com.yandex.pay"

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
        assertEquals(68_000L, BankPurchaseParser.parse(bank, null, listOf(null, "Покупка на 680.00 RUB. Доступно 1.00 RUB")))
    }

    // ---------- Яндекс Пэй (#235): заголовок «−N ₽ с карты Пэй», текст «Оплата …». Названия и суммы вымышлены. ----------

    @Test
    fun payPaymentTakesAmountFromTitle() {
        assertEquals(160_000L, parse("Оплата Ростелеком. Баланс: 5745 ₽", pay, "−1600 ₽ с карты Пэй"))
        assertEquals(40_000L, parse("Оплата Yota. Баланс: 7345 ₽", pay, "−400 ₽ с карты Пэй"))
    }

    @Test
    fun payTitleVariantsAreRead() {
        assertEquals(160_000L, parse("Оплата Магазин. Баланс: 1 ₽", pay, "-1 600 ₽ с карты Пэй"))
        assertEquals(160_050L, parse("Оплата Магазин. Баланс: 1 ₽", pay, "−1600,50 ₽ с карты Пэй"))
        assertEquals(160_050L, parse("Оплата Магазин. Баланс: 1 ₽", pay, "−1\u00A0600.5 ₽ с карты Пэй".replace(".5", ".50")))
        assertEquals(5L, parse("Оплата Магазин. Баланс: 1 ₽", pay, "–0,05 ₽ с карты Пэй"))
    }

    @Test
    fun payTransfersAreStillSkipped() {
        assertNull(parse("Перевод на Сейв \"КОПИЛКА\". Баланс карты: 4845 ₽", pay, "−20 ₽ с карты Пэй"))
        assertNull(parse("Пополнение с карты Пэй. Баланс накопительного счёта: 120 ₽", pay, "+20 ₽ на Сейв \"КОПИЛКА\""))
        assertNull(parse("Перевод на карту Пэй. Баланс Сейва: 0 ₽", pay, "−5000 ₽ с Сейва \"МОЖНО\""))
        assertNull(parse("Пополнение с Сейва \"МОЖНО\". Баланс карты: 5545 ₽", pay, "+5000 ₽ на карту Пэй"))
    }

    @Test
    fun payWithoutAmountOrWithIncomeIsSkipped() {
        assertNull(parse("Оплата Ростелеком. Баланс: 5745 ₽", pay, null))
        assertNull(parse("Оплата Ростелеком. Баланс: 5745 ₽", pay, "Нужно проверить операцию"))
        assertNull(parse("Оплата Ростелеком. Баланс: 5745 ₽", pay, "+1600 ₽ на карту Пэй"))
        assertNull(parse("Оплата Ростелеком. Баланс: 5745 ₽", pay, "−0 ₽ с карты Пэй"))
        assertNull(parse("Оплаты нет. Баланс: 5745 ₽", pay, "−1600 ₽ с карты Пэй"))
        assertNull(parse("Оплата Ростелеком. Баланс: 5745 ₽", "ru.sberbankmobile", "−1600 ₽ с карты Пэй"))
    }

    @Test
    fun ruYandexAppsAreReadToo() {
        assertTrue(BankPurchaseParser.isBankApp("ru.yandex.bank"))
        assertTrue(BankPurchaseParser.isBankApp("ru.yandex"))
        assertFalse(BankPurchaseParser.isBankApp("ru.yandexx.fake"))
        assertEquals(40_000L, parse("Оплата Yota. Баланс: 7345 ₽", "ru.yandex.mobile.pay", "−400 ₽ с карты Пэй"))
    }
}
