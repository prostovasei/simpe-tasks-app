package com.prostovasei.simpletasks.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.RepeatFrequency
import com.prostovasei.simpletasks.domain.model.RepeatRule
import com.prostovasei.simpletasks.domain.model.parseReminder
import java.time.DayOfWeek
import java.time.LocalDate
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

/** Напоминание по-русски: «В момент срока», «За 7 дней», «За 1 час». Неизвестный формат — как есть. */
@Composable
@ReadOnlyComposable
fun reminderText(trigger: String): String {
    val offset = parseReminder(trigger) ?: return trigger
    if (offset.isZero) return stringResource(R.string.reminder_at_due)
    val minutes = offset.abs().toMinutes().toInt()
    val amount = when {
        minutes % (7 * 24 * 60) == 0 -> (minutes / (7 * 24 * 60)).let { pluralStringResource(R.plurals.unit_weeks, it, it) }
        minutes % (24 * 60) == 0 -> (minutes / (24 * 60)).let { pluralStringResource(R.plurals.unit_days, it, it) }
        minutes % 60 == 0 -> (minutes / 60).let { pluralStringResource(R.plurals.unit_hours, it, it) }
        else -> pluralStringResource(R.plurals.unit_minutes, minutes, minutes)
    }
    return stringResource(if (offset.isNegative) R.string.reminder_before else R.string.reminder_after, amount)
}
