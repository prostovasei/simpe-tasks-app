package com.prostovasei.simpletasks.ui.feature.health

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightMath
import com.prostovasei.simpletasks.domain.model.health.Forecast
import com.prostovasei.simpletasks.domain.model.health.BmiClass
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val MONTHS_GENITIVE = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val MONTHS_SHORT = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

/** «16 ноября». */
fun dayMonth(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"

/** «16 ноя» — подписи оси графика. */
fun dayMonthShort(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS_SHORT[date.monthValue - 1]}"

/** «16 ноября 2026» — для дат не этого года. */
fun dayMonthYear(date: LocalDate): String = "${dayMonth(date)} ${date.year}"

/** «Ноябрь 2026». */
fun monthTitle(date: LocalDate): String = MONTHS_NOMINATIVE[date.monthValue - 1] + " " + date.year

private val MONTHS_NOMINATIVE = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")

/** Минуты дня → «08:00». */
fun clock(minute: Int): String = "%02d:%02d".format(minute / MINUTES_IN_HOUR, minute % MINUTES_IN_HOUR)

const val MINUTES_IN_HOUR = 60

/** «16 ноября · сегодня» / «15 ноября · вчера» / «9 ноября». */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "${dayMonth(date)} · ${stringResource(R.string.finance_today)}"
    today.minusDays(1) -> "${dayMonth(date)} · ${stringResource(R.string.finance_yesterday)}"
    else -> if (date.year == today.year) dayMonth(date) else dayMonthYear(date)
}

/** «сегодня», «вчера», «3 дн. назад», дальше — дата. */
@Composable
fun sinceLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0 -> stringResource(R.string.finance_today)
        days == 1L -> stringResource(R.string.finance_yesterday)
        days < SINCE_DAYS -> stringResource(R.string.health_days_ago, days.toInt())
        else -> if (date.year == today.year) dayMonth(date) else dayMonthYear(date)
    }
}

private const val SINCE_DAYS = 14

@Composable
fun bmiName(kind: BmiClass): String = stringResource(bmiNameResource(kind))

internal fun bmiNameResource(kind: BmiClass): Int = when (kind) {
        BmiClass.UNDER -> R.string.health_bmi_under
        BmiClass.NORMAL -> R.string.health_bmi_normal
        BmiClass.OVER -> R.string.health_bmi_over
        BmiClass.OBESE -> R.string.health_bmi_obese
    }


internal fun healthGoalText(
    context: android.content.Context,
    entries: List<com.prostovasei.simpletasks.domain.model.health.WeightEntry>,
    settings: com.prostovasei.simpletasks.domain.model.health.HealthSettings,
    today: LocalDate,
    formatDate: (LocalDate) -> String,
): Pair<String, String?> {
    val latest = entries.last().tenths
    val bmi = WeightMath.bmi(latest, settings.heightCm)
    val bmiText = bmi?.let { context.getString(R.string.health_bmi, it.text, context.getString(bmiNameResource(it.kind))) }
    val goal = settings.goalTenths
    val title: String
    val sub: String?
    if (goal == null) {
        title = bmiText ?: context.getString(R.string.health_goal_set)
        sub = context.getString(if (bmiText != null) R.string.health_goal_set_only else R.string.health_goal_set_hint)
    } else {
        val forecast = WeightMath.forecast(entries, goal, today)
        if (forecast == Forecast.Reached) {
            title = context.getString(R.string.health_goal_reached, Weight.format(goal))
            sub = listOfNotNull(context.getString(R.string.health_goal_now, Weight.format(latest)), bmiText).joinToString(" · ")
        } else {
            title = context.getString(R.string.health_goal_line, Weight.format(goal), Weight.format(kotlin.math.abs(latest - goal)))
            val forecastText = when (forecast) {
                is Forecast.At -> context.getString(R.string.health_forecast_at, formatDate(forecast.date))
                Forecast.NotEnoughData -> context.getString(R.string.health_forecast_wait)
                is Forecast.WrongWay -> context.getString(
                    when (forecast.rising) {
                        true -> R.string.health_forecast_rising
                        false -> R.string.health_forecast_falling
                        null -> R.string.health_forecast_flat
                    },
                )
                else -> null
            }
            sub = listOfNotNull(forecastText, bmiText).joinToString(" · ").ifEmpty { null }
        }
    }
    return title to sub
}
