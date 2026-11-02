package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.GHOST_ALPHA
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Ячейка дня в виде «Неделя» (итерация 44, макет `VariantK1`): строка даты «пн 26» (у сегодня — зелёная, число в метке) и задачи
 * плашками: кружок приоритета, название, время справа. Высота ячейки задана снаружи (все ячейки недели одинаковые), поэтому
 * показывается столько плашек, сколько влезает; остальное — «+N» справа в строке даты. Нажатие на строку даты открывает полный
 * список дня. Правая и нижняя линии — сетка между ячейками.
 */
@Composable
fun WeekCell(
    date: LocalDate,
    items: List<TaskItem>,
    isToday: Boolean,
    onToggle: (TaskItem) -> Unit,
    onOpen: (TaskItem) -> Unit,
    onOpenDay: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val divider = colors.divider
    val line = AppTheme.sizes.hairline
    BoxWithConstraints(
        modifier
            .drawBehind {
                val w = line.toPx()
                drawLine(divider, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
                drawLine(divider, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            }
            .padding(horizontal = spacing.xxs, vertical = spacing.xs)
            .testTag("week-day:$date"),
    ) {
        val rowHeight = SizeTokens.WEEKVIEW_TASK.dp + spacing.xxs
        val room = maxHeight - SizeTokens.WEEKVIEW_HEADER.dp
        val capacity = if (maxHeight == Dp.Infinity) items.size else maxOf(0, (room / rowHeight).toInt())
        val shown = items.take(capacity)
        Column {
            WeekDayHeader(date, isToday, hidden = items.size - shown.size, onClick = onOpenDay)
            shown.forEach { item ->
                WeekTaskRow(item, overdue = today != null && date < today && !item.showsDone, onToggle = { onToggle(item) }, onOpen = { onOpen(item) })
            }
        }
    }
}

@Composable
private fun WeekDayHeader(date: LocalDate, isToday: Boolean, hidden: Int, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru"))
    Row(
        Modifier.fillMaxWidth().height(SizeTokens.WEEKVIEW_HEADER.dp).clickable(onClick = onClick).testTag("week-day-open:$date"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            weekday,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isToday) colors.accent else colors.textPrimary,
        )
        if (isToday) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onAccent,
                modifier = Modifier
                    .padding(start = AppTheme.spacing.xxs)
                    .background(colors.accent, CircleShape)
                    .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.sizes.hairline * 2),
            )
        } else {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = AppTheme.spacing.xxs),
            )
        }
        if (hidden > 0) {
            Spacer(Modifier.weight(1f))
            Text(
                "+$hidden",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                modifier = Modifier
                    .background(colors.accentSoft, CircleShape)
                    .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.sizes.hairline)
                    .testTag("week-more:$date"),
            )
        }
    }
}

/**
 * Плашка задачи в ячейке: кружок приоритета (нажатие — «выполнено» или «вернуть»), название, время справа.
 * Выполненная — зачёркнутая и бледнее, кружок залит; будущий повтор — бледный и без нажатия на кружок.
 */
@Composable
private fun WeekTaskRow(item: TaskItem, overdue: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val done = item.showsDone
    val ring = when {
        item.isGhost && !done -> colors.inactive
        item.priority == Priority.HIGH -> colors.checkRings.high
        item.priority == Priority.MEDIUM -> colors.checkRings.medium
        item.priority == Priority.LOW -> colors.checkRings.low
        else -> colors.inactive
    }
    val label = stringResource(if (done) R.string.task_uncomplete else R.string.widget_complete)
    Row(
        Modifier
            .padding(top = spacing.xxs)
            .fillMaxWidth()
            .height(SizeTokens.WEEKVIEW_TASK.dp)
            .alpha(if (item.isGhost) GHOST_ALPHA else if (done) DONE_ALPHA else 1f)
            .clip(AppTheme.shapes.medium)
            .background(colors.surface)
            .clickable(onClick = onOpen)
            .testTag("week-task:${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Зона нажатия кружка — вся высота плашки, сам кружок — WEEKVIEW_CHECK.
        Box(
            Modifier
                .size(SizeTokens.WEEKVIEW_TASK.dp)
                .then(
                    if (item.isGhost) Modifier else Modifier.clickable(onClick = onToggle).semantics { contentDescription = label }.testTag("week-check:${item.id}"),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(SizeTokens.WEEKVIEW_CHECK.dp).border(SizeTokens.CHECKBOX_STROKE.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
                if (done) {
                    Box(Modifier.size(SizeTokens.WEEKVIEW_CHECK.dp).background(ring, CircleShape))
                    Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.WEEKVIEW_CHECK_ICON.dp))
                }
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (item.isGhost) colors.textSecondary else colors.textPrimary,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        item.startTime()?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (overdue) colors.overdue else colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.padding(start = spacing.xxs, end = spacing.xs),
            )
        } ?: Box(Modifier.size(spacing.xs))
    }
}
