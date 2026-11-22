package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
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
 * Мини-календарь вида «Неделя» (итерация 44; 0.46.0 — макет `VariantK2`): «пн…вс» и шесть недель месяца, строки месяца со стрелками нет.
 * Строка недели нажимается целиком и выбирает неделю; неделя с днями соседнего месяца перелистывает календарь на него ([monthAfterPick]).
 * Выбранная неделя — серая полоса `accentSoft`, сегодня — кружок цвета акцента, дни соседних месяцев — бледные.
 */
@Composable
fun WeekCalendar(
    month: YearMonth,
    selectedWeek: LocalDate,
    today: LocalDate,
    onSelectWeek: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    inlineMonth: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val ru = Locale.forLanguageTag("ru")
    val selectWeekLabel = stringResource(R.string.week_select_calendar_week)
    val first = month.atDay(1).weekStart()
    // Мини-календарь живёт в ячейке недели, высота которой зависит от экрана: строки недель делят то, что осталось под «пн…вс».
    BoxWithConstraints(modifier.padding(horizontal = spacing.xxs, vertical = spacing.xxs).testTag("week-calendar")) {
        val fixed = SizeTokens.WEEKVIEW_CAL_WEEKDAYS.dp + spacing.xxs * 2
        val rowHeight = ((maxHeight - fixed) / WEEK_CALENDAR_ROWS).coerceIn(SizeTokens.WEEKVIEW_CAL_ROW_MIN.dp, SizeTokens.WEEKVIEW_CAL_ROW.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
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
                val title = "${weekCalendarFullDate.format(start)} — ${weekCalendarFullDate.format(start.plusDays(WEEK_LENGTH - 1L))}"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(AppTheme.shapes.large)
                        .background(if (selected) colors.accentSoft else colors.background)
                        .then(if (selected) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = selectWeekLabel) { onSelectWeek(start) }
                            .semantics { contentDescription = title })
                        .testTag("week-cal-row:$start"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(WEEK_LENGTH) { offset ->
                        val date = start.plusDays(offset.toLong())
                        val inMonth = YearMonth.from(date) == month
                        val suffix = if (inlineMonth && inMonth && date.dayOfMonth == 1) month.calendarMonthLabel() else null
                        val dateLabel = weekCalendarFullDate.format(date)
                        val target = if (selected) Modifier.fillMaxSize()
                            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.week_open_calendar_day)) { onOpenDay(date) }
                            .semantics { contentDescription = dateLabel }
                            .testTag("week-cal-day:$date") else Modifier
                        WeekCalendarDay(date, isToday = date == today, inMonth, Modifier.weight(1f).then(target), suffix)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCalendarDay(date: LocalDate, isToday: Boolean, inMonth: Boolean, modifier: Modifier, suffix: String?) {
    val colors = AppTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        val size: Dp = SizeTokens.WEEKVIEW_CAL_TODAY.dp
        if (suffix != null) {
            Text(
                "${date.dayOfMonth} $suffix",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = SizeTokens.WEEK_MONTH_INLINE_TEXT.sp),
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isToday) colors.onAccent else colors.textPrimary,
                modifier = if (isToday) Modifier.background(colors.accent, CircleShape) else Modifier,
                maxLines = 1,
            )
        } else if (isToday) {
            Box(Modifier.size(size).background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = colors.onAccent)
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
