package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Недель в сетке мини-календаря: месяц всегда занимает шесть строк, чтобы высота не прыгала. */
const val WEEK_CALENDAR_ROWS = 6

/**
 * Мини-календарь вида «Неделя» (итерация 44, макет `VariantK1`): месяц с листанием «‹ ›», строка недели нажимается целиком и
 * выбирает неделю; выбранная неделя — полоса `accentSoft`, сегодня — зелёный кружок, дни соседних месяцев — бледные.
 */
@Composable
fun WeekCalendar(
    month: YearMonth,
    selectedWeek: LocalDate,
    today: LocalDate,
    onSelectWeek: (LocalDate) -> Unit,
    onShiftMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val ru = Locale.forLanguageTag("ru")
    val first = month.atDay(1).weekStart()
    // Мини-календарь живёт в ячейке недели, высота которой зависит от экрана: строки недель делят то, что осталось под заголовком.
    BoxWithConstraints(modifier.padding(horizontal = spacing.xxs, vertical = spacing.xxs)) {
    val fixed = SizeTokens.WEEKVIEW_CAL_HEAD.dp + SizeTokens.WEEKVIEW_CAL_WEEKDAYS.dp + spacing.xxs * 2
    val rowHeight = ((maxHeight - fixed) / WEEK_CALENDAR_ROWS).coerceIn(SizeTokens.WEEKVIEW_CAL_ROW_MIN.dp, SizeTokens.WEEKVIEW_CAL_ROW.dp)
    Column {
        Row(Modifier.fillMaxWidth().height(SizeTokens.WEEKVIEW_CAL_HEAD.dp), verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(stringResource(R.string.week_prev_month), "week-cal-prev", { onShiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.textSecondary)
            }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, ru)} ${month.year}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("week-cal-title"),
            )
            MonthArrow(stringResource(R.string.week_next_month), "week-cal-next", { onShiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().height(SizeTokens.WEEKVIEW_CAL_WEEKDAYS.dp)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, ru),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inactive,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        repeat(WEEK_CALENDAR_ROWS) { index ->
            val start = first.plusDays(index * WEEK_LENGTH.toLong())
            val selected = start == selectedWeek
            val title = "${start.dayOfMonth} ${start.month.getDisplayName(TextStyle.SHORT, ru)}"
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(AppTheme.shapes.large)
                    .background(if (selected) colors.accentSoft else colors.background)
                    .clickable { onSelectWeek(start) }
                    .semantics { contentDescription = title }
                    .testTag("week-cal-row:$start"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(WEEK_LENGTH) { offset ->
                    val date = start.plusDays(offset.toLong())
                    WeekCalendarDay(date, isToday = date == today, inMonth = YearMonth.from(date) == month, Modifier.weight(1f))
                }
            }
        }
    }
    }
}

@Composable
private fun MonthArrow(description: String, tag: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .size(SizeTokens.WEEKVIEW_CAL_ARROW.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun WeekCalendarDay(date: LocalDate, isToday: Boolean, inMonth: Boolean, modifier: Modifier) {
    val colors = AppTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        val size: Dp = SizeTokens.WEEKVIEW_CAL_TODAY.dp
        if (isToday) {
            Box(Modifier.size(size).background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colors.onAccent)
            }
        } else {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (inMonth) colors.textPrimary else colors.inactive,
            )
        }
    }
}
