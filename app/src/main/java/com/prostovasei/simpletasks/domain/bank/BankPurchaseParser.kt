package com.prostovasei.simpletasks.domain.bank

/**
 * Распознавание покупки по уведомлению банка (#233, design/approved/233/requirements.md §2.1). Решение PM: «где написано
 * „Покупка“ — это расход». Покупка — текст, начинающийся с «Покупка на 680.00 RUB»: дальше может быть «, карта *1234»
 * и «. Доступно … RUB». Переводы, пополнения и всё незнакомое пропускаются — лучше пропустить, чем записать ошибку.
 * Читаются только приложения Яндекса (решение PM §9 п.5). Текст уведомления никуда не сохраняется.
 */
object BankPurchaseParser {
    /** Сумма — число с точкой и двумя знаками; группы разрядов пробелами допускаются на случай крупных покупок. */
    private val PURCHASE = Regex("""^Покупка на (\d{1,3}(?:[   ]?\d{3})*|\d+)\.(\d{2}) RUB(?:$|[.,])""")

    private const val YANDEX = "com.yandex"

    /** Уведомления этого приложения могут быть покупками; остальные отбрасываются сразу, без чтения текста. */
    fun isBankApp(packageName: String): Boolean = packageName == YANDEX || packageName.startsWith("$YANDEX.")

    /** Сумма покупки в копейках или null — не покупка. [texts] — текст уведомления (обычный и развёрнутый). */
    fun parse(packageName: String, texts: List<CharSequence?>): Long? {
        if (!isBankApp(packageName)) return null
        return texts.firstNotNullOfOrNull { it?.toString()?.trim()?.let(::amount) }
    }

    /** Сумма из текста «Покупка на 1800.00 RUB…» в копейках; не покупка или нулевая сумма — null. */
    fun amount(text: String): Long? {
        val match = PURCHASE.find(text) ?: return null
        val rubles = match.groupValues[1].filter(Char::isDigit).toLongOrNull() ?: return null
        val kopecks = rubles * KOPECKS + match.groupValues[2].toLong()
        return kopecks.takeIf { it > 0 }
    }

    private const val KOPECKS = 100L
}
