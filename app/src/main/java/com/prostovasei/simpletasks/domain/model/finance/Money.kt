package com.prostovasei.simpletasks.domain.model.finance

/**
 * Рубли (финансы, #217): суммы хранятся в копейках целым числом — без ошибок округления. Вывод — «42 300 ₽», копейки только
 * если они есть («42 300,50 ₽»); разделитель тысяч и пробел перед «₽» — неразрывные.
 */
object Money {
    private const val NBSP = ' '
    private const val KOPECKS = 100

    /** Наибольшая целая часть при вводе: 10 цифр — до 9 999 999 999 ₽. */
    const val MAX_DIGITS = 10
    private const val FRACTION_DIGITS = 2

    fun format(kopecks: Long, sign: Boolean = false): String = "${amount(kopecks, sign)}$NBSP₽"

    /** Число без знака рубля — для поля ввода и крупной суммы. */
    fun amount(kopecks: Long, sign: Boolean = false): String {
        val abs = kotlin.math.abs(kopecks)
        val rubles = (abs / KOPECKS).toString().reversed().chunked(3).joinToString(NBSP.toString()).reversed()
        val rest = (abs % KOPECKS).toInt()
        val prefix = when {
            kopecks < 0 -> "−"
            sign && kopecks > 0 -> "+"
            else -> ""
        }
        return prefix + rubles + if (rest == 0) "" else ",%02d".format(rest)
    }

    /** Текст поля для правки суммы: без разделителей тысяч («42300,5» → «42300,50»). */
    fun input(kopecks: Long): String {
        val rest = (kopecks % KOPECKS).toInt()
        return (kopecks / KOPECKS).toString() + if (rest == 0) "" else ",%02d".format(rest)
    }

    /** Что остаётся в поле при вводе: цифры и одна запятая (точка — тоже запятая), не больше двух знаков после неё. */
    fun filterInput(text: String): String {
        val out = StringBuilder()
        var comma = false
        var fraction = 0
        var digits = 0
        for (c in text) {
            when {
                c.isDigit() && !comma && digits < MAX_DIGITS -> { out.append(c); digits++ }
                c.isDigit() && comma && fraction < FRACTION_DIGITS -> { out.append(c); fraction++ }
                (c == ',' || c == '.') && !comma -> { if (out.isEmpty()) out.append('0'); out.append(','); comma = true }
            }
        }
        return out.toString()
    }

    /** Копейки из введённого текста; пустое поле — 0, непонятный текст — null. */
    fun parse(text: String): Long? {
        val clean = text.filterNot { it.isWhitespace() || it == NBSP }.replace('.', ',')
        if (clean.isEmpty()) return 0
        if (!Regex("""\d{1,$MAX_DIGITS}(,\d{0,$FRACTION_DIGITS})?""").matches(clean)) return null
        val parts = clean.split(',')
        val fraction = parts.getOrNull(1).orEmpty().padEnd(FRACTION_DIGITS, '0')
        return parts[0].toLong() * KOPECKS + fraction.toLong()
    }
}
