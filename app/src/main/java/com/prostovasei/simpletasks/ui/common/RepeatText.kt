package com.prostovasei.simpletasks.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.RepeatFrequency
import com.prostovasei.simpletasks.domain.model.RepeatRule
import com.prostovasei.simpletasks.domain.model.allDayReminder
import com.prostovasei.simpletasks.domain.model.parseReminder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RU = Locale.forLanguageTag("ru")
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RU)

/** Повтор по-русски: «Каждые 2 недели: вт, чт», «Каждый месяц, 14-го», «Каждый год, 5 июня · 3 раза». */
@Composable
@ReadOnlyComposable
fun repeatSummary(rrule: String?): String? {
    val rule = RepeatRule.parse(rrule) ?: return null
    if (rule.unsupported.isNotEmpty()) return stringResource(R.string.repeat_complex)
    val base = when (rule.frequency) {
        RepeatFrequency.DAILY -> if (rule.interval == 1) stringResource(R.string.repeat_daily) else pluralStringResource(R.plurals.repeat_days, rule.interval, rule.interval)
        RepeatFrequency.WEEKLY -> if (rule.interval == 1) stringResource(R.string.repeat_weekly) else pluralStringResource(R.plurals.repeat_weeks, rule.interval, rule.interval)
        RepeatFrequency.MONTHLY -> if (rule.interval == 1) stringResource(R.string.repeat_monthly) else pluralStringResource(R.plurals.repeat_months, rule.interval, rule.interval)
        RepeatFrequency.YEARLY -> if (rule.interval == 1) stringResource(R.string.repeat_yearly) else pluralStringResource(R.plurals.repeat_years, rule.interval, rule.interval)
    }
    val details = buildList {
        if (rule.weekDays.isNotEmpty()) add(rule.weekDays.sorted().joinToString(", ") { shortDay(it) })
        val month = rule.month
        val day = rule.monthDay
        when {
            month != null && day != null -> add(DAY_MONTH.format(LocalDate.of(2000, month, day)))
            day != null -> add(stringResource(R.string.repeat_on_day, day))
        }
    }
    val limits = buildList {
        rule.count?.let { add(pluralStringResource(R.plurals.repeat_count, it, it)) }
        rule.until?.let { add(stringResource(R.string.repeat_until, DAY_MONTH.format(it))) }
    }
    val withDetails = if (details.isEmpty()) base else "$base: ${details.joinToString(", ")}"
    return (listOf(withDetails) + limits).joinToString(" · ")
}

/** «пн», «вт»… */
fun shortDay(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, RU).removeSuffix(".")

/**
 * Напоминание по-русски. Для задачи со временем: «В момент срока», «За 30 минут», «За 1 день».
 * Для задачи на весь день: «В день срока в 10:00», «За 1 день в 10:00». Неизвестный формат — как есть.
 */
@Composable
@ReadOnlyComposable
fun reminderText(trigger: String, allDay: Boolean = false): String {
    val offset = parseReminder(trigger) ?: return trigger
    if (allDay) {
        val (daysBefore, minuteOfDay) = allDayReminder(offset)
        val time = REMINDER_TIME.format(LocalTime.ofSecondOfDay(minuteOfDay * 60L))
        return when {
            daysBefore == 0 -> stringResource(R.string.reminder_on_day, time)
            daysBefore > 0 -> stringResource(R.string.reminder_before_at, daysAmount(daysBefore), time)
            else -> stringResource(R.string.reminder_after_at, daysAmount(-daysBefore), time)
        }
    }
    if (offset.isZero) return stringResource(R.string.reminder_at_due)
    val minutes = offset.abs().toMinutes().toInt()
    val amount = when {
        minutes % (24 * 60) == 0 -> daysAmount(minutes / (24 * 60))
        minutes % 60 == 0 -> (minutes / 60).let { pluralStringResource(R.plurals.unit_hours, it, it) }
        else -> pluralStringResource(R.plurals.unit_minutes, minutes, minutes)
    }
    return stringResource(if (offset.isNegative) R.string.reminder_before else R.string.reminder_after, amount)
}

/** «1 неделю», «3 дня». */
@Composable
@ReadOnlyComposable
private fun daysAmount(days: Int): String =
    if (days % 7 == 0) pluralStringResource(R.plurals.unit_weeks, days / 7, days / 7) else pluralStringResource(R.plurals.unit_days, days, days)

private val REMINDER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
