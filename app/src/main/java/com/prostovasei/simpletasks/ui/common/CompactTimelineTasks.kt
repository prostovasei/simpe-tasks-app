package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.widget.WidgetRow

@Composable
private fun canvasTimeStyle() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

@Composable
private fun canvasTimeWidth(): Dp {
    val measure = rememberTextMeasurer().measure("00:00", canvasTimeStyle())
    return with(LocalDensity.current) { measure.size.width.toDp() }
}

/** Only AllScreen provides this style: project/search/inbox retain their geometry. */
@Composable
internal fun LazyItemScope.CompactTimelineTaskRow(
    row: WidgetRow.Task, top: Boolean, bottom: Boolean,
    onToggle: () -> Unit, onClick: () -> Unit, showList: Boolean,
    selected: Boolean?, onLongClick: (() -> Unit)?,
) {
    val colors = AppTheme.colors
    val item = row.item
    val tag = "task:${item.id}"
    val text = Color(item.canvasTextColor(row.past).light)
    val node = when {
        selected == true -> colors.accent
        item.showsDone -> text
        item.priority == Priority.HIGH -> text
        else -> colors.inactive
    }
    val label = stringResource(when {
        selected != null -> if (selected) R.string.select_unmark else R.string.select_mark
        item.showsDone -> R.string.task_uncomplete
        else -> R.string.widget_complete
    })
    val height = ((if (row.isPeriod) SizeTokens.WIDGET_PERIOD_ROW else SizeTokens.WIDGET_ROW) +
        if (item.subtitle != null) SizeTokens.TRASH_ROW_EXTRA else 0).dp
    Row(Modifier.animateItem().fillMaxWidth().height(height)
        .padding(start = AppTheme.spacing.sm, end = AppTheme.spacing.xs).testTag(tag)) {
        Column(Modifier.width(canvasTimeWidth()).fillMaxHeight().testTag("$tag:time"),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
            row.start?.let { Text(it, style = canvasTimeStyle(), color = text, maxLines = 1) }
            row.end?.let { Text(it, style = canvasTimeStyle(), color = text, maxLines = 1) }
        }
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Column(Modifier.width(SizeTokens.WEEKVIEW_CHECK.dp).fillMaxHeight()
            .then(if (item.isGhost) Modifier else Modifier.clickable(onClick = onToggle)
                .semantics { contentDescription = label }.testTag("$tag:check")),
            horizontalAlignment = Alignment.CenterHorizontally) {
            CanvasSegment(top, if (row.isPeriod) Modifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp) else Modifier.weight(1f))
            val filled = selected ?: item.showsDone
            val resource = if (selected == null) item.canvasNodeIcon(row.isPeriod) else when {
                row.isPeriod -> if (filled) R.drawable.ic_task_period_fill else R.drawable.ic_task_period_ring
                filled -> R.drawable.ic_week_node_fill
                else -> R.drawable.ic_week_node_ring
            }
            Box((if (row.isPeriod) Modifier.weight(1f).fillMaxWidth() else Modifier.size(SizeTokens.WEEKVIEW_CHECK.dp))
                .testTag("$tag:node"), contentAlignment = Alignment.Center) {
                Image(painterResource(resource), null, colorFilter = ColorFilter.tint(node), contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize().then(if (item.isRecurring && selected == null) Modifier.testTag("$tag:repeat") else Modifier))
                if (filled && (!item.isRecurring || selected != null)) {
                    Icon(painterResource(R.drawable.ic_node_check), null, tint = colors.onListSquare,
                        modifier = Modifier.size(SizeTokens.WEEKVIEW_CHECK_ICON.dp))
                }
            }
            CanvasSegment(bottom, if (row.isPeriod) Modifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp) else Modifier.weight(1f))
        }
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Row(Modifier.weight(1f).fillMaxHeight().padding(vertical = SizeTokens.WIDGET_CARD_INSET.dp)
            .then(if (selected == true) Modifier.background(colors.selected, AppTheme.shapes.medium)
                .border(SizeTokens.CHECKBOX_STROKE.dp, colors.accent, AppTheme.shapes.medium) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).testTag("$tag:content").padding(end = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("$tag:subtitle")) }
            }
            item.reminder?.let {
                ReminderIcon(it, SizeTokens.WIDGET_REMINDER_ICON.dp, colors.textSecondary, colors.accent,
                    Modifier.padding(start = AppTheme.spacing.xxs).alpha(if (item.showsDone) DONE_ALPHA else 1f).testTag("$tag:reminder"))
            }
            if (showList) {
                Spacer(Modifier.width(AppTheme.spacing.xs))
                Box(Modifier.alpha(if (item.showsDone) TaskCanvasTokens.DONE_EMOJI_ALPHA else 1f)) { ListBadge(item.listEmoji) }
            }
        }
    }
}

@Composable
private fun CanvasSegment(visible: Boolean, modifier: Modifier) {
    Box(modifier.width(AppTheme.sizes.hairline).background(if (visible) AppTheme.colors.divider else Color.Transparent))
}

@Composable
internal fun LazyItemScope.CompactTimelineNowRow(nowMinute: Int, top: Boolean, bottom: Boolean) {
    val colors = AppTheme.colors
    Row(Modifier.animateItem().fillMaxWidth().height(SizeTokens.WIDGET_NOW_ROW.dp)
        .padding(start = AppTheme.spacing.sm, end = AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text("%02d:%02d".format(nowMinute / 60, nowMinute % 60), style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = colors.accent, textAlign = TextAlign.End,
            modifier = Modifier.width(canvasTimeWidth()))
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Box(Modifier.width(SizeTokens.WEEKVIEW_CHECK.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                CanvasSegment(top, Modifier.weight(1f)); CanvasSegment(bottom, Modifier.weight(1f))
            }
            Box(Modifier.size(SizeTokens.WIDGET_NOW_DOT.dp).background(colors.accent, AppTheme.shapes.extraLarge))
        }
        Spacer(Modifier.width(SizeTokens.WEEK_CONTENT_GAP.dp))
        Box(Modifier.weight(1f).height(AppTheme.sizes.hairline).background(colors.accent))
    }
}
