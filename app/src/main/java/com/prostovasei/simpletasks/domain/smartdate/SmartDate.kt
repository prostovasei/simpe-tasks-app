package com.prostovasei.simpletasks.domain.smartdate

import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Срок, найденный в названии задачи (итерация 41, бэклог №29): [ranges] — где в тексте стоят фразы (подсветка и вырезание),
 * [date] — день, [minute] — время от начала суток (null — весь день).
 */
data class SmartDate(val ranges: List<IntRange>, val date: LocalDate, val minute: Int?) {
    fun due(zone: ZoneId): TaskDue =
        if (minute == null) TaskDue.AllDay(date) else TaskDue.AtTime(date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant())

    /** Тексты фраз — по ним узнаётся фраза, которая уже была в названии при открытии задачи. */
    fun phrases(text: String): List<String> = ranges.map { text.substring(it).lowercase() }
}

/**
 * Разбор даты и времени в названии (решения PM, итерация 41). Только явные фразы — лучше пропустить, чем испортить название:
 * «сегодня / завтра / послезавтра», «в пятницу», «в следующий вторник», «через 3 дня», «через неделю», «15 ноября», «15.11»,
 * «10:00», «в 18», «в 9 утра». Число без слов («5 яиц»), день недели без предлога («Вторник — фильм»), месяц без числа — не трогаем.
 * Только время: сегодня, а если оно уже прошло — завтра. Только дата: весь день.
 */
object SmartDateParser {

    fun parse(text: String, today: LocalDate, nowMinute: Int): SmartDate? {
        val lower = text.lowercase().replace('ё', 'е')
        val date = findDate(lower, today)
        val time = findTime(lower, exclude = date?.range)
        if (date == null && time == null) return null
        val day = date?.date ?: if (time!!.minute > nowMinute) today else today.plusDays(1)
        return SmartDate(merge(lower, listOfNotNull(date?.range, time?.range).sortedBy { it.first }), day, time?.minute)
    }

    /** «завтра в 10:00» — одна подсветка, если между фразами только пробелы. */
    private fun merge(s: String, ranges: List<IntRange>): List<IntRange> {
        if (ranges.size < 2) return ranges
        val (a, b) = ranges
        return if (s.substring(a.last + 1, b.first).isBlank()) listOf(a.first..b.last) else ranges
    }

    /** Название без фраз: лишние пробелы схлопываются, по краям убираются пробелы и знаки «,» «-». */
    fun strip(text: String, ranges: List<IntRange>): String {
        val out = StringBuilder()
        var at = 0
        for (r in ranges.sortedBy { it.first }) {
            if (r.first < at || r.last >= text.length) continue
            out.append(text, at, r.first).append(' ')
            at = r.last + 1
        }
        out.append(text.substring(at))
        return out.toString().replace(SPACES, " ").replace(SPACE_BEFORE_PUNCT, "$1").trim().trim(',', '-', '—', ' ').trim()
    }

    private data class Found(val range: IntRange, val date: LocalDate)
    private data class FoundTime(val range: IntRange, val minute: Int)

    private fun findDate(s: String, today: LocalDate): Found? {
        val candidates = listOfNotNull(
            RELATIVE.find(s)?.let { m ->
                val shift = when (m.groupValues[2]) { "сегодня" -> 0L; "завтра" -> 1L; else -> 2L }
                Found(m.range, today.plusDays(shift))
            },
            WEEKDAY.find(s)?.let { m ->
                val dow = weekday(m.groupValues[3]) ?: return@let null
                // «в пятницу» — ближайшая пятница (сегодня тоже); «в следующую пятницу» — пятница следующей недели.
                var d = if (m.groupValues[2].isNotEmpty()) today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)) else today
                while (d.dayOfWeek != dow) d = d.plusDays(1)
                Found(m.range, d)
            },
            IN_DAYS.find(s)?.let { m ->
                val n = m.groupValues[1].ifEmpty { "1" }.toLong()
                val unit = m.groupValues[2]
                Found(m.range, if (unit.startsWith("нед")) today.plusWeeks(n) else today.plusDays(n))
            },
            DAY_MONTH.find(s)?.let { m ->
                val day = m.groupValues[2].toInt()
                val month = month(m.groupValues[3]) ?: return@let null
                dateOrNull(today, day, month, null)?.let { Found(m.range, it) }
            },
            NUMERIC.find(s)?.let { m ->
                val year = m.groupValues[4].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
                dateOrNull(today, m.groupValues[2].toInt(), m.groupValues[3].toInt(), year)?.let { Found(m.range, it) }
            },
        )
        return candidates.minByOrNull { it.range.first }
    }

    private fun findTime(s: String, exclude: IntRange?): FoundTime? {
        val clock = CLOCK.findAll(s).firstOrNull { m -> exclude == null || m.range.first > exclude.last || m.range.last < exclude.first }
        clock?.let { m ->
            val h = m.groupValues[2].toInt()
            val min = m.groupValues[3].toInt()
            if (h < HOURS && min < MINUTES) return FoundTime(m.range, adjust(h, m.groupValues[4]) * MINUTES + min)
        }
        for (m in HOUR.findAll(s)) {
            if (exclude != null && m.range.first <= exclude.last && m.range.last >= exclude.first) continue
            val h = m.groupValues[1].toInt()
            if (h >= HOURS) continue
            val explicit = m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty()
            if (!explicit && !freeAfter(s, m.range.last + 1)) continue
            return FoundTime(m.range, adjust(h, m.groupValues[3]) * MINUTES)
        }
        return null
    }

    /** «в 5 магазинах» — не время: после «в N» должны идти конец, знак, короткое слово или другая фраза срока. */
    private fun freeAfter(s: String, from: Int): Boolean {
        val rest = s.substring(from).trimStart()
        if (rest.isEmpty() || !rest[0].isLetter()) return true
        val word = rest.takeWhile { it.isLetter() }
        return word.length <= 2 || word in DATE_WORDS || WEEKDAY.matchesAt(rest, 0)
    }

    private fun adjust(hour: Int, suffix: String): Int = when {
        suffix.startsWith("веч") || suffix.startsWith("дня") -> if (hour < HALF_DAY) hour + HALF_DAY else hour
        suffix.startsWith("ноч") -> if (hour == HALF_DAY) 0 else hour
        else -> hour
    }

    private fun dateOrNull(today: LocalDate, day: Int, month: Int, year: Int?): LocalDate? {
        if (month !in 1..MONTHS) return null
        val y = year ?: today.year
        val d = runCatching { LocalDate.of(y, month, day) }.getOrNull() ?: return null
        return if (year == null && d.isBefore(today)) runCatching { LocalDate.of(y + 1, month, day) }.getOrNull() else d
    }

    private fun weekday(word: String): DayOfWeek? = when {
        word.startsWith("понедельник") -> DayOfWeek.MONDAY
        word.startsWith("вторник") -> DayOfWeek.TUESDAY
        word.startsWith("сред") -> DayOfWeek.WEDNESDAY
        word.startsWith("четверг") -> DayOfWeek.THURSDAY
        word.startsWith("пятниц") -> DayOfWeek.FRIDAY
        word.startsWith("суббот") -> DayOfWeek.SATURDAY
        word.startsWith("воскресень") -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun month(word: String): Int? = MONTH_STEMS.indexOfFirst { word.startsWith(it) }.takeIf { it >= 0 }?.plus(1)

    private const val B = "(?<![\\p{L}\\d])"
    private const val E = "(?![\\p{L}\\d])"
    private val RELATIVE = Regex("$B((?:на|к)\\s+)?(сегодня|послезавтра|завтра)$E")
    private val WEEKDAY = Regex(
        "$B(в|во|на|к|ко)\\s+(?:(следующ(?:ий|ую|ее|ей|ему))\\s+)?" +
            "(понедельник[а-я]*|вторник[а-я]*|сред[уеа]|четверг[а-я]*|пятниц[уеа]|суббот[уеа]|воскресень[еюя])$E",
    )
    private val IN_DAYS = Regex("${B}через\\s+(?:(\\d{1,3})\\s+)?(дн(?:я|ей)|день|недел[июь]|недель)$E")
    private val DAY_MONTH = Regex(
        "$B((?:на|к|до|в)\\s+)?(\\d{1,2})\\s+(январ[ья]|феврал[ья]|марта?|апрел[ья]|ма[йя]|июн[ья]|июл[ья]|августа?|сентябр[ья]|октябр[ья]|ноябр[ья]|декабр[ья])$E",
    )
    private val NUMERIC = Regex("$B((?:на|к|до|в)\\s+)?(\\d{1,2})\\.(\\d{2})(?:\\.(\\d{2}|\\d{4}))?$E")
    private val CLOCK = Regex("$B((?:в|к|на)\\s+)?(\\d{1,2}):(\\d{2})(?:\\s+(утра|дня|вечера|ночи))?$E")
    private val HOUR = Regex("$B(?:в|к)\\s+(\\d{1,2})(\\s+час(?:а|ов)?)?(?:\\s+(утра|дня|вечера|ночи))?$E")
    private val SPACES = Regex("\\s+")
    private val SPACE_BEFORE_PUNCT = Regex("\\s+([,.!?;:])")
    private val MONTH_STEMS = listOf("янв", "фев", "мар", "апр", "ма", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    private val DATE_WORDS = setOf("сегодня", "завтра", "послезавтра", "через", "утра", "вечера", "ночи", "дня", "час")
    private const val HOURS = 24
    private const val MINUTES = 60
    private const val HALF_DAY = 12
    private const val MONTHS = 12
}
