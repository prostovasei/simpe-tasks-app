package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prostovasei.simpletasks.ui.common.doneAlpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** #263: равные ячейки недели; эмодзи → название → начало справа, любое нажатие открывает день. */
@Composable
fun WeekCell(
    date: LocalDate,
    items: List<TaskItem>,
    isToday: Boolean,
    onOpenDay: () -> Unit,
    modifier: Modifier = Modifier,
    calendarMonth: YearMonth? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val divider = colors.divider
    val line = AppTheme.sizes.hairline
    BoxWithConstraints(
        modifier
            .clickable(onClick = onOpenDay)
            .drawBehind {
                val w = line.toPx()
                drawLine(divider, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
                drawLine(divider, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            }
            .padding(horizontal = SizeTokens.WEEK_EDGE.dp, vertical = spacing.xs)
            .testTag("week-day:$date"),
    ) {
        val rowHeight = SizeTokens.WEEKVIEW_TASK.dp + SizeTokens.WEEKVIEW_TASK_GAP.dp
        val room = maxHeight - SizeTokens.WEEKVIEW_HEADER.dp
        val capacity = if (maxHeight == Dp.Infinity) items.size else maxOf(0, (room / rowHeight).toInt())
        val shown = items.take(capacity)
        Column(Modifier.fillMaxSize()) {
            WeekDayHeader(date, isToday, hidden = items.size - shown.size, calendarMonth = calendarMonth)
            shown.forEach { item ->
                WeekTaskRow(item)
            }
            Spacer(Modifier.weight(1f).fillMaxWidth().testTag("week-blank:$date"))
        }
    }
}

@Composable
private fun WeekDayHeader(date: LocalDate, isToday: Boolean, hidden: Int, calendarMonth: YearMonth?) {
    val colors = AppTheme.colors
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru"))
    Row(
        Modifier.fillMaxWidth().height(SizeTokens.WEEKVIEW_HEADER.dp).testTag("week-day-open:$date"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Итерация 47 (решение PM): день недели и число одним нежирным начертанием и серые; сегодня — жирное и чёрное, без кружка.
        val weight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
        val color = if (isToday) colors.textPrimary else colors.textSecondary
        // Итерация 55 (как в виджете): «ПН 9» капсом, мельче.
        Text(
            "${weekday.uppercase()} ${date.dayOfMonth}" + if (isToday) " ${stringResource(R.string.week_today_suffix)}" else "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = weight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(if (isToday) Modifier.testTag("week-today:$date") else Modifier.testTag("week-date:$date")),
        )
        if (calendarMonth != null) {
            val description = stringResource(R.string.week_calendar_month, calendarMonth.calendarMonthDescription())
            Text(calendarMonth.calendarMonthLabel(), style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs).semantics { contentDescription = description },
                maxLines = 1)
        }
        if (hidden > 0) {
            Text(
                "+$hidden",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                modifier = Modifier
                    .testTag("week-more:$date"),
            )
        }
    }
}

/** #263: строка не имеет собственных действий — вся ячейка открывает день. */
@Composable
private fun WeekTaskRow(item: TaskItem) {
    val colors = AppTheme.colors
    val done = item.showsDone
    val high = item.priority == Priority.HIGH
    // #316: выполненная — те же цвета, вся строка полупрозрачная.
    val titleColor = if (high) Color(TaskCanvasTokens.weekPriority.light) else colors.textPrimary
    Row(
        Modifier.padding(top = SizeTokens.WEEKVIEW_TASK_GAP.dp).fillMaxWidth().alpha(doneAlpha(done))
            .height(SizeTokens.WEEKVIEW_TASK.dp).testTag("week-task:${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.listEmoji, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            modifier = Modifier.testTag("week-emoji:${item.id}"))
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Text(item.title, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (high) FontWeight.ExtraBold else FontWeight.Bold,
            color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("week-title:${item.id}"))
        item.startTime()?.let { time ->
            Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
            Text(time, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = if (high) FontWeight.ExtraBold else FontWeight.Bold,
                color = titleColor, maxLines = 1, modifier = Modifier.testTag("week-time:${item.id}"))
        }
    }
}
