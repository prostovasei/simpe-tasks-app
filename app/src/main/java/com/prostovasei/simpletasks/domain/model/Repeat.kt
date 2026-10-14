package com.prostovasei.simpletasks.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Частота повтора. [code] — значение FREQ в RRULE (RFC 5545). */
enum class RepeatFrequency(val code: String) {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY"),
}

/**
 * Правило повтора в формате RRULE (как в TickTick и календарях), разобранное на части.
 * Правило хранится строкой слово в слово; эта модель нужна, чтобы показать его и собрать новое
 * в простом конструкторе (каждые N дней/недель/месяцев/лет + дни недели).
 */
data class RepeatRule(
    val frequency: RepeatFrequency,
    val interval: Int = 1,
    val weekDays: Set<DayOfWeek> = emptySet(),
    val monthDay: Int? = null,
    val month: Int? = null,
    val count: Int? = null,
    val until: LocalDate? = null,
    /** Части правила, которые конструктор не умеет менять (например, «последняя пятница»). */
    val unsupported: Map<String, String> = emptyMap(),
) {
    /**
     * Правило можно править простым конструктором без потери смысла: нет неизвестных частей, ограничений
     * по числу раз или дате, а число месяца / месяц совпадают со сроком задачи (так их записывает TickTick).
     */
    fun isSimple(due: LocalDate?): Boolean =
        unsupported.isEmpty() && count == null && until == null &&
            (monthDay == null || monthDay == due?.dayOfMonth) &&
            (month == null || month == due?.monthValue)

    /** RRULE без префикса «RRULE:», порядок частей фиксированный. */
    fun toRRule(): String = buildList {
        add("FREQ=${frequency.code}")
        if (interval != 1) add("INTERVAL=$interval")
        if (weekDays.isNotEmpty()) add("BYDAY=" + weekDays.sorted().joinToString(",") { DAY_CODES.getValue(it) })
        month?.let { add("BYMONTH=$it") }
        monthDay?.let { add("BYMONTHDAY=$it") }
        count?.let { add("COUNT=$it") }
        until?.let { add("UNTIL=%04d%02d%02d".format(it.year, it.monthValue, it.dayOfMonth)) }
        unsupported.forEach { (key, value) -> add("$key=$value") }
    }.joinToString(";")

    companion object {
        private val DAY_CODES = mapOf(
            DayOfWeek.MONDAY to "MO", DayOfWeek.TUESDAY to "TU", DayOfWeek.WEDNESDAY to "WE", DayOfWeek.THURSDAY to "TH",
            DayOfWeek.FRIDAY to "FR", DayOfWeek.SATURDAY to "SA", DayOfWeek.SUNDAY to "SU",
        )
        private val CODE_DAYS = DAY_CODES.entries.associate { (day, code) -> code to day }

        /** Ключи, которые не меняют смысл правила для одного пользователя (начало недели). */
        private val IGNORED = setOf("WKST")

        /** Разбирает строку RRULE (с префиксом «RRULE:» или без). null — не похоже на RRULE. */
        fun parse(text: String?): RepeatRule? {
            val body = text?.trim()?.removePrefix("RRULE:")?.takeIf { it.isNotEmpty() } ?: return null
            val parts = body.split(';').filter { it.isNotBlank() }.associate { part ->
                val (key, value) = part.split('=', limit = 2).let { it[0].trim().uppercase() to it.getOrElse(1) { "" }.trim() }
                key to value
            }
            val frequency = RepeatFrequency.entries.firstOrNull { it.code == parts["FREQ"]?.uppercase() } ?: return null
            val unsupported = mutableMapOf<String, String>()
            val days = parts["BYDAY"]?.split(',')?.map { it.trim().uppercase() }.orEmpty()
            val weekDays = days.mapNotNull { CODE_DAYS[it] }.toSet()
            if (days.size != weekDays.size) parts["BYDAY"]?.let { unsupported["BYDAY"] = it }
            val known = setOf("FREQ", "INTERVAL", "BYDAY", "BYMONTHDAY", "BYMONTH", "COUNT", "UNTIL") + IGNORED
            parts.filterKeys { it !in known }.forEach { (k, v) -> unsupported[k] = v }

            /** Число из части правила; вне [range] или не число — часть считается неподдерживаемой и сохраняется как есть. */
            fun int(key: String, range: IntRange): Int? {
                val raw = parts[key] ?: return null
                val value = raw.toIntOrNull()?.takeIf { it in range }
                if (value == null) unsupported[key] = raw
                return value
            }
            val monthDay = int("BYMONTHDAY", 1..31)
            val month = int("BYMONTH", 1..12)
            return RepeatRule(
                frequency = frequency,
                interval = int("INTERVAL", 1..999) ?: 1,
                weekDays = if ("BYDAY" in unsupported) emptySet() else weekDays,
                monthDay = monthDay,
                month = month,
                count = int("COUNT", 1..9999),
                until = parts["UNTIL"]?.let { raw -> parseUntil(raw).also { if (it == null) unsupported["UNTIL"] = raw } },
                unsupported = unsupported,
            )
        }

        private fun parseUntil(value: String): LocalDate? = runCatching {
            LocalDate.of(value.substring(0, 4).toInt(), value.substring(4, 6).toInt(), value.substring(6, 8).toInt())
        }.getOrNull()
    }
}
