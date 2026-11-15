package com.prostovasei.simpletasks.ui.feature.week

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import com.prostovasei.simpletasks.ui.common.weekNodeIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Ячейка дня в виде «Неделя» (итерация 44; 0.46.0 — макет `VariantK2`; 0.55 — как в виджете: «ПН 9» капсом, эмодзи списка слева от времени): строка даты (у сегодня число в кружке цвета акцента)
 * и задачи компактными строками: время, кружок приоритета, название, эмодзи справа. Высота ячейки задана снаружи (все ячейки недели одинаковые), поэтому
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
            .padding(horizontal = SizeTokens.WEEK_EDGE.dp, vertical = spacing.xs)
            .testTag("week-day:$date"),
    ) {
        val rowHeight = SizeTokens.WEEKVIEW_TASK.dp + SizeTokens.WEEKVIEW_TASK_GAP.dp
        val room = maxHeight - SizeTokens.WEEKVIEW_HEADER.dp
        val capacity = if (maxHeight == Dp.Infinity) items.size else maxOf(0, (room / rowHeight).toInt())
        val shown = items.take(capacity)
        Column {
            WeekDayHeader(date, isToday, hidden = items.size - shown.size, onClick = onOpenDay)
            shown.forEach { item ->
                WeekTaskRow(item, onToggle = { onToggle(item) }, onOpen = { onOpen(item) })
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
        // Итерация 47 (решение PM): день недели и число одним нежирным начертанием и серые; сегодня — жирное и чёрное, без кружка.
        val weight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
        val color = if (isToday) colors.textPrimary else colors.textSecondary
        // Итерация 55 (как в виджете): «ПН 9» капсом, мельче.
        Text(
            "${weekday.uppercase()} ${date.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = weight,
            color = color,
            modifier = if (isToday) Modifier.testTag("week-today:$date") else Modifier.testTag("week-date:$date"),
        )
        if (hidden > 0) {
            Spacer(Modifier.weight(1f))
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

/**
 * Строка задачи в ячейке (0.46.0, `VariantK2`; итерация 47 — `VariantH2`): белый фон без плашек;
 * кружок приоритета (нажатие — «выполнено» или «вернуть»), у повторяющейся — знак повтора; время слева и название фиолетовые у высокого приоритета (просрочка не красная).
 * Выполненная — бледно-фиолетовая у высокого приоритета, иначе серая; без зачёркивания, кружок залит тусклым цветом приоритета; будущий повтор — как обычная, но без нажатия на знак.
 */
@Composable
private fun WeekTaskRow(item: TaskItem, onToggle: () -> Unit, onOpen: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val done = item.showsDone
    val high = item.priority == Priority.HIGH
    val priorityColor = Color(if (done) TaskCanvasTokens.priorityDone.light else TaskCanvasTokens.priority.light)
    val titleColor = if (high) priorityColor else if (done) Color(TaskCanvasTokens.done.light) else colors.textPrimary
    val timeColor = if (high) priorityColor else if (done) Color(TaskCanvasTokens.done.light) else colors.textSecondary
    val ring = if (high) priorityColor else colors.inactive
    val doneFill = if (high) priorityColor else Color(TaskCanvasTokens.done.light)
    val timeStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val timeMeasurer = rememberTextMeasurer()
    val timeWidth = with(LocalDensity.current) { timeMeasurer.measure("00:00", timeStyle).size.width.toDp() }
    val label = stringResource(if (done) R.string.task_uncomplete else R.string.widget_complete)
    Row(
        Modifier
            .padding(top = SizeTokens.WEEKVIEW_TASK_GAP.dp)
            .fillMaxWidth()
            .height(SizeTokens.WEEKVIEW_TASK.dp)
            .clip(AppTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .testTag("week-task:${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.startTime().orEmpty(),
            style = timeStyle,
            color = timeColor,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.width(timeWidth).clickable(onClick = onOpen)
                .testTag("week-time:${item.id}"),
        )
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        // Горизонтальных полей вокруг видимого значка нет, высота зоны прежняя.
        Box(
            Modifier
                .size(width = SizeTokens.WEEKVIEW_CHECK.dp, height = SizeTokens.WEEKVIEW_TASK.dp)
                .then(
                    if (item.isGhost) Modifier else Modifier.clickable(onClick = onToggle).semantics { contentDescription = label }.testTag("week-check:${item.id}"),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(item.weekNodeIcon()), null,
                tint = if (done) doneFill else ring,
                modifier = Modifier.size(SizeTokens.WEEKVIEW_CHECK.dp)
                    .then(if (item.isRecurring) Modifier.testTag("week-repeat:${item.id}") else Modifier),
            )
            if (done && !item.isRecurring) {
                Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.onListSquare,
                    modifier = Modifier.size(SizeTokens.WEEKVIEW_CHECK_ICON.dp))
            }
        }
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clickable(onClick = onOpen).testTag("week-title:${item.id}"),
        )
        // #249: эмодзи остаётся справа, время — в отдельной колонке слева.
        Text(item.listEmoji, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(start = spacing.xxs).alpha(if (done) TaskCanvasTokens.DONE_EMOJI_ALPHA else 1f).testTag("week-emoji:${item.id}"))

    }
}
