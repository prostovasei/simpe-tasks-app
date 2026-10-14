package com.prostovasei.simpletasks.domain.model

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Повторяющаяся задача после выполнения: новый срок и правило (у «N раз» уменьшается остаток). */
data class NextOccurrence(val anchor: TaskDue, val rrule: String)

/**
 * Движок повторов (Итерация 05b): правило RRULE считает готовая библиотека lib-recur,
 * здесь — только правила приложения.
 *
 * - Следующая дата ищется после текущего срока; пропущенные дни (раньше [today]) пропускаются,
 *   чтобы задача, просроченная на месяц, не требовала отметки за каждый пропущенный раз.
 * - COUNT («N раз») — сколько раз осталось, включая текущий; уменьшается на число пройденных повторений.
 * - UNTIL («до даты») соблюдает библиотека.
 */
object Recurrence {
    /** Предохранитель: ежедневный повтор, просроченный на 30 лет. */
    private const val MAX_STEPS = 11_000

    /**
     * Следующее повторение для срока (или начала интервала) [anchor].
     * null — повторы закончились или правило не удаётся прочитать.
     */
    fun next(rrule: String, anchor: TaskDue, zone: ZoneId, today: LocalDate): NextOccurrence? {
        val parts = rrule.trim().removePrefix("RRULE:").split(';').filter { it.isNotBlank() }
        val count = parts.firstOrNull { it.startsWith("COUNT=") }?.substringAfter('=')?.toIntOrNull()
        val rule = runCatching {
            RecurrenceRule(parts.filterNot { it.startsWith("COUNT=") }.joinToString(";"), RecurrenceRule.RfcMode.RFC2445_LAX)
        }.getOrNull() ?: return null

        val iterator = rule.iterator(anchor.toDateTime(zone))
        var passed = 0
        var steps = 0
        while (iterator.hasNext() && steps++ < MAX_STEPS) {
            val candidate = iterator.nextDateTime().toTaskDue(anchor, zone)
            if (!candidate.isAfter(anchor)) continue
            passed++
            if (count != null && passed >= count) return null
            if (candidate.localDate(zone) < today) continue
            val newRule = if (count == null) rrule else parts.joinToString(";") { if (it.startsWith("COUNT=")) "COUNT=${count - passed}" else it }
            return NextOccurrence(candidate, newRule)
        }
        return null
    }

    /**
     * Будущие повторы после [anchor] с датой в промежутке ([after], [until]] — для показа наперёд
     * (решение PM: ближайшие 30 дней). Учитывает «N раз» и «до даты».
     */
    fun upcoming(rrule: String, anchor: TaskDue, zone: ZoneId, after: LocalDate, until: LocalDate): List<TaskDue> {
        val parts = rrule.trim().removePrefix("RRULE:").split(';').filter { it.isNotBlank() }
        val count = parts.firstOrNull { it.startsWith("COUNT=") }?.substringAfter('=')?.toIntOrNull()
        val rule = runCatching {
            RecurrenceRule(parts.filterNot { it.startsWith("COUNT=") }.joinToString(";"), RecurrenceRule.RfcMode.RFC2445_LAX)
        }.getOrNull() ?: return emptyList()

        val iterator = rule.iterator(anchor.toDateTime(zone))
        val result = mutableListOf<TaskDue>()
        var passed = 0
        var steps = 0
        while (iterator.hasNext() && steps++ < MAX_STEPS) {
            val candidate = iterator.nextDateTime().toTaskDue(anchor, zone)
            if (!candidate.isAfter(anchor)) continue
            passed++
            if (count != null && passed >= count) break
            val date = candidate.localDate(zone)
            if (date > until) break
            if (date > after) result += candidate
        }
        return result
    }

    private fun TaskDue.toDateTime(zone: ZoneId): DateTime = when (this) {
        is TaskDue.AllDay -> DateTime(date.year, date.monthValue - 1, date.dayOfMonth)
        is TaskDue.AtTime -> instant.atZone(zone).toLocalDateTime().let {
            DateTime(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute, it.second)
        }
    }

    private fun DateTime.toTaskDue(like: TaskDue, zone: ZoneId): TaskDue = when (like) {
        is TaskDue.AllDay -> TaskDue.AllDay(LocalDate.of(year, month + 1, dayOfMonth))
        is TaskDue.AtTime -> TaskDue.AtTime(LocalDateTime.of(year, month + 1, dayOfMonth, hours, minutes, seconds).atZone(zone).toInstant())
    }

    private fun TaskDue.isAfter(other: TaskDue): Boolean = when {
        this is TaskDue.AllDay && other is TaskDue.AllDay -> date > other.date
        this is TaskDue.AtTime && other is TaskDue.AtTime -> instant > other.instant
        else -> false
    }

    private fun TaskDue.localDate(zone: ZoneId): LocalDate = when (this) {
        is TaskDue.AllDay -> date
        is TaskDue.AtTime -> instant.atZone(zone).toLocalDate()
    }
}

/**
 * Задача, перенесённая на следующее повторение, или null, если повторов больше нет.
 * Для интервала повтор считается от начала, конец сдвигается на то же время.
 */
fun Task.nextOccurrence(zone: ZoneId, today: LocalDate): Task? {
    val rule = rrule ?: return null
    val anchor = start ?: due ?: return null
    val next = Recurrence.next(rule, anchor, zone, today) ?: return null
    return movedTo(next.anchor)?.copy(rrule = next.rrule)
}

/** Будущие повторы задачи с датой после [today] и не позже [until] — только для показа. */
fun Task.upcomingRepeats(zone: ZoneId, today: LocalDate, until: LocalDate): List<Task> {
    val rule = rrule ?: return emptyList()
    val anchor = start ?: due ?: return emptyList()
    return Recurrence.upcoming(rule, anchor, zone, after = today, until = until).mapNotNull { movedTo(it) }
}

/** Задача с началом (или сроком) в [anchor]; конец интервала сдвигается на то же время. */
private fun Task.movedTo(anchor: TaskDue): Task? {
    val due = due ?: return null
    val current = start ?: due
    val newDue = when {
        current is TaskDue.AtTime && anchor is TaskDue.AtTime && due is TaskDue.AtTime ->
            TaskDue.AtTime(due.instant.plus(Duration.between(current.instant, anchor.instant)))
        current is TaskDue.AllDay && anchor is TaskDue.AllDay && due is TaskDue.AllDay ->
            TaskDue.AllDay(due.date.plusDays(ChronoUnit.DAYS.between(current.date, anchor.date)))
        else -> anchor
    }
    return withDue(newDue)
}
