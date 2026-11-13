package com.prostovasei.simpletasks.domain.bank

/**
 * Распознавание покупки по уведомлению банка (#233, #235; design/approved/233/requirements.md §2.1). Решение PM: «где написано
 * „Покупка“ — это расход». Два вида:
 * 1. Яндекс Банк: текст начинается с «Покупка на 680.00 RUB» (дальше может быть «, карта *1234» и «. Доступно … RUB»);
 * 2. Яндекс Пэй (0.63.0): заголовок «−1600 ₽ с карты Пэй», текст «Оплата Ростелеком. Баланс: 5745 ₽» — сумма из заголовка.
 * Переводы, пополнения и всё незнакомое пропускаются — лучше пропустить, чем записать ошибку. Читаются только приложения Яндекса
 * (решение PM §9 п.5). Текст и заголовок уведомления никуда не сохраняются.
 */
object BankPurchaseParser {
    /** Сумма — число с точкой и двумя знаками; группы разрядов пробелами допускаются на случай крупных покупок. */
    private val PURCHASE = Regex("""^Покупка на (\d{1,3}(?:[ \u00A0\u202F]?\d{3})*|\d+)\.(\d{2}) RUB(?:$|[.,])""")

    /** Заголовок Пэй: знак минус (U+2212, дефис или тире), сумма с необязательными копейками, «₽»: «−1600 ₽ с карты Пэй», «−1 600,50 ₽». */
    private val PAY_TITLE = Regex("""^[\u2212\u2013\u2014-]\s*(\d{1,3}(?:[ \u00A0\u202F]?\d{3})*|\d+)(?:[.,](\d{1,2}))?\s*₽""")

    private const val PAY_TEXT = "Оплата "
    private const val YANDEX = "com.yandex"
    private const val YANDEX_RU = "ru.yandex"

    /** Уведомления этого приложения могут быть покупками; остальные отбрасываются сразу, без чтения текста. */
    fun isBankApp(packageName: String): Boolean =
        packageName == YANDEX || packageName.startsWith("$YANDEX.") || packageName == YANDEX_RU || packageName.startsWith("$YANDEX_RU.")

    /** Сумма покупки в копейках или null — не покупка. [title] — заголовок, [texts] — текст уведомления (обычный и развёрнутый). */
    fun parse(packageName: String, title: CharSequence?, texts: List<CharSequence?>): Long? {
        if (!isBankApp(packageName)) return null
        val bodies = texts.mapNotNull { it?.toString()?.trim() }
        return bodies.firstNotNullOfOrNull(::amount) ?: payAmount(title?.toString()?.trim(), bodies)
    }

    /** Сумма из текста «Покупка на 1800.00 RUB…» в копейках; не покупка или нулевая сумма — null. */
    fun amount(text: String): Long? {
        val match = PURCHASE.find(text) ?: return null
        return kopecks(match.groupValues[1], match.groupValues[2])
    }

    /** Яндекс Пэй: заголовок с суммой и текст «Оплата …». Заголовок без «Оплата» в тексте (перевод, пополнение) — не покупка. */
    private fun payAmount(title: String?, bodies: List<String>): Long? {
        if (title == null || bodies.none { it.startsWith(PAY_TEXT) }) return null
        val match = PAY_TITLE.find(title) ?: return null
        return kopecks(match.groupValues[1], match.groupValues[2])
    }

    /** Рубли и необязательные копейки («5» — 50 копеек, «05» — 5); нулевая сумма — null. */
    private fun kopecks(rubles: String, fraction: String): Long? {
        val whole = rubles.filter(Char::isDigit).toLongOrNull() ?: return null
        val kop = fraction.padEnd(2, '0').toLongOrNull() ?: 0L
        return (whole * KOPECKS + kop).takeIf { it > 0 }
    }

    private const val KOPECKS = 100L
}
