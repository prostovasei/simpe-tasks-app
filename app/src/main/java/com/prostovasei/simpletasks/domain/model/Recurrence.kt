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
    val due = due ?: return null
    val anchor = start ?: due
    val next = Recurrence.next(rule, anchor, zone, today) ?: return null
    val newDue = when {
        anchor is TaskDue.AtTime && next.anchor is TaskDue.AtTime && due is TaskDue.AtTime ->
            TaskDue.AtTime(due.instant.plus(Duration.between(anchor.instant, next.anchor.instant)))
        anchor is TaskDue.AllDay && next.anchor is TaskDue.AllDay && due is TaskDue.AllDay ->
            TaskDue.AllDay(due.date.plusDays(ChronoUnit.DAYS.between(anchor.date, next.anchor.date)))
        else -> next.anchor
    }
    return withDue(newDue).copy(rrule = next.rrule)
}
