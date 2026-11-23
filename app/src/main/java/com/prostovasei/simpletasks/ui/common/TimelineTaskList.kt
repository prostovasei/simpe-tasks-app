package com.prostovasei.simpletasks.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.RepeatMark
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.theme.LocalCompactTimeline
import com.prostovasei.simpletasks.designsystem.theme.LocalTaskCanvas
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.widget.WidgetRow
import com.prostovasei.simpletasks.widget.WidgetTimeline
import kotlinx.coroutines.delay
import java.time.LocalTime

/*
 * Задачи в приложении — как в виджете (итерация 26, решение PM): слева время, по центру сплошная линия
 * с кружком приоритета (нажатие — «выполнено»), справа белая плашка: название, напоминание, повтор, список.
 */

/**
 * Строки дня: задачи по шкале [WidgetTimeline] и отметка «сейчас» ([nowMinute], только у сегодняшнего дня).
 * [dayLabels] — слева день и время (во «Входящих» и списках, где дней нет в подписях).
 */
fun LazyListScope.timelineItems(
    items: List<TaskItem>,
    keyPrefix: String,
    days: DayNames,
    nowMinute: Int?,
    showNowMarker: Boolean = true,
    isOverdue: (TaskItem) -> Boolean,
    showList: Boolean,
    onToggle: (TaskItem) -> Unit,
    onOpen: (TaskItem) -> Unit,
    dayLabels: Boolean = false,
    /** Режим выбора (итерация 40): null — выключен; иначе выбранные id. В режиме нажатие и кружок отмечают задачу. */
    selection: Set<String>? = null,
    /** Нажатие в режиме выбора и долгое нажатие вне его; null — выбирать на этом экране нельзя. */
    onSelect: ((TaskItem) -> Unit)? = null,
) {
    val rows = WidgetTimeline.rows(items, overdue = dayLabels, nowMinute = nowMinute.takeUnless { dayLabels }, days = days).filter { showNowMarker || it != WidgetRow.Now }
    // Ключ с днём: при переносе между днями Room обновляет списки не одновременно, и одна задача на миг бывает в двух днях сразу —
    // с одним лишь id это «Key was already used» и вылет.
    itemsIndexed(rows, key = { _, row -> if (row is WidgetRow.Task) "$keyPrefix:${row.item.id}" else "$keyPrefix-now" }) { index, row ->
        val top = index > 0
        val bottom = index < rows.lastIndex
        when (row) {
            is WidgetRow.Task -> {
                val item = row.item
                // Будущий повтор выбрать нельзя: это не задача, а её будущий раз.
                val selectable = onSelect != null && !item.isGhost
                val selecting = selection != null
                TimelineTaskRow(
                    row, top, bottom, isOverdue(item), showList,
                    onToggle = { if (selecting) { if (selectable) onSelect?.invoke(item) } else onToggle(item) },
                    onClick = { if (selecting) { if (selectable) onSelect?.invoke(item) } else onOpen(item) },
                    dayLabels = dayLabels,
                    selected = if (selecting && !item.isGhost) item.id in selection.orEmpty() else null,
                    onLongClick = if (selectable && !selecting) { { onSelect?.invoke(item) } } else null,
                )
            }
            WidgetRow.Now -> TimelineNowRow(nowMinute ?: 0, top, bottom)
        }
    }
}

/** Минута дня сейчас, обновляется раз в минуту — для отметки «сейчас». */
@Composable
fun rememberNowMinute(): Int {
    var minute by remember { mutableIntStateOf(LocalTime.now().let { it.hour * MINUTES_PER_HOUR + it.minute }) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            delay(((SECONDS_PER_MINUTE - now.second) * MILLIS_PER_SECOND).toLong())
            minute = LocalTime.now().let { it.hour * MINUTES_PER_HOUR + it.minute }
        }
    }
    return minute
}

/**
 * Строка задачи: время | линия с кружком | плашка. Период занимает обычную строку с кругом; оба времени остаются слева.
 * Выполненная (итерация 27) остаётся на месте до полуночи: кружок залит цветом приоритета с белой галочкой; #316 — без
 * зачёркивания, вся строка (время, кружок, плашка) полупрозрачная; переход «медленно тает» ([doneAlpha], [doneCheck]).
 */
@Composable
fun LazyItemScope.TimelineTaskRow(
    row: WidgetRow.Task,
    top: Boolean,
    bottom: Boolean,
    isOverdue: Boolean,
    showList: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    dayLabels: Boolean = false,
    /** Режим выбора (итерация 40): null — выключен; true — строка выбрана (зелёная галочка в кружке, рамка на плашке). */
    selected: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
) {
    if (LocalCompactTimeline.current) {
        CompactTimelineTaskRow(row, top, bottom, onToggle, onClick, showList, selected, onLongClick)
        return
    }
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val item = row.item
    val testTag = "task:${item.id}"
    val done = doneCheck(item.showsDone)
    val fade = doneAlpha(item.showsDone)
    // Итерация 47 (решение PM): будущий повтор не тусклый — тусклая только прошедшая задача.
    val faded = row.past
    val baseHeight = SizeTokens.WIDGET_ROW
    val height = (baseHeight + if (item.subtitle != null) SizeTokens.TRASH_ROW_EXTRA else 0).dp
    Row(
        Modifier
            .animateItem()
            .fillMaxWidth()
            .height(height)
            .padding(end = spacing.xs)
            .testTag(testTag),
    ) {
        Box(Modifier.alpha(fade)) { TimeColumn(row, isOverdue, faded, dayLabels) }
        Rail(row, top, bottom, faded, done, testTag, onToggle, selected, fade)
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = SizeTokens.WIDGET_CARD_INSET.dp)
                .alpha(fade)
                .clip(AppTheme.shapes.medium)
                .background(if (selected == true) colors.selected else if (LocalTaskCanvas.current) Color.Transparent else colors.surface)
                .then(if (selected == true) Modifier.border(SizeTokens.CHECKBOX_STROKE.dp, colors.accent, AppTheme.shapes.medium) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (faded) colors.textSecondary else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("$testTag:subtitle"),
                    )
                }
            }
            // Значки справа налево (итерация 27): список, колокольчик. Повтор с итерации 47 — знаком вместо кружка на шкале.
            item.reminder?.let {
                ReminderIcon(
                    mark = it,
                    size = SizeTokens.WIDGET_REMINDER_ICON.dp,
                    tint = colors.textSecondary,
                    earlyColor = colors.accent,
                    modifier = Modifier.padding(start = spacing.xxs).testTag("$testTag:reminder"),
                )
            }
            if (showList) {
                Spacer(Modifier.width(spacing.xs))
                ListBadge(item.listEmoji)
            }
        }
    }
}

/** Колонка времени: по правому краю; у периода конец — серым под началом; у просроченной — красным. */
@Composable
private fun TimeColumn(row: WidgetRow.Task, isOverdue: Boolean, faded: Boolean, dayLabels: Boolean) {
    val colors = AppTheme.colors
    val color = when {
        isOverdue -> colors.overdue
        faded -> colors.textSecondary
        else -> colors.textPrimary
    }
    val style = if (dayLabels) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val measuredTime = rememberTextMeasurer().measure("00:00", style.copy(fontWeight = FontWeight.Bold))
    val timeWidth = with(LocalDensity.current) { measuredTime.size.width.toDp() }
    Column(
        // С днём («Завтра», «16 окт») колонка шире и мельче — как у просроченных в виджете.
        // Итерация 41: колонка дня растёт вместе с системным шрифтом — «Сегодня» помещается и с крупным шрифтом.
        // Множитель шрифта, а не sp: Android 14 увеличивает крупные sp слабее мелкого текста, и колонка отставала бы от подписи.
        Modifier.width(if (dayLabels) (SizeTokens.TIMELINE_DAY_COLUMN * LocalDensity.current.fontScale).dp else maxOf(SizeTokens.WIDGET_TIME.dp, timeWidth)).fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        row.start?.let { Text(it, style = style, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
        row.end?.let {
            Text(it, style = style, fontWeight = FontWeight.Bold, color = if (isOverdue) colors.overdue else colors.textSecondary, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Линия шкалы с кружком приоритета (в том числе у периода); нажатие — «выполнено» или, у выполненной,
 * вернуть. [done] — доля перехода к выполненной: кольцо заливается цветом и появляется белая галочка. Итерация 47: у повторяющейся
 * задачи вместо кружка — знак повтора ([RepeatMark]: круглый, в том числе у периода), у выполненной он остаётся без галочки,
 * тусклым; выполненная задача высокого приоритета заливается тусклым фиолетовым.
 */
@Composable
private fun Rail(row: WidgetRow.Task, top: Boolean, bottom: Boolean, faded: Boolean, done: Float, testTag: String, onToggle: () -> Unit, selected: Boolean? = null, fade: Float = 1f) {
    val colors = AppTheme.colors
    val item = row.item
    // Итерация 27.1 (вариант PM «А»): кружок всегда цвета приоритета, просрочка — красными подписью дня и временем.
    val ring = when {
        faded && !item.showsDone -> colors.inactive
        item.priority == Priority.HIGH -> colors.checkRings.high
        else -> colors.inactive
    }
    // #316: кружок заливается полным цветом приоритета, бледность даёт прозрачность всей задачи.
    val fill = ring
    // Проверка PM (0.47.0): у выполненной повторяющейся галочки нет — знак повтора остаётся, тусклым цветом выполненной.
    val repeat = item.isRecurring && selected == null
    val repeatColor = if (item.showsDone) fill else ring
    val label = stringResource(
        when {
            selected != null -> if (selected) R.string.select_unmark else R.string.select_mark
            item.showsDone -> R.string.task_uncomplete
            else -> R.string.widget_complete
        },
    )
    val tap = if (item.isGhost) {
        Modifier
    } else {
        Modifier.clickable(onClick = onToggle).semantics { contentDescription = label }.testTag("$testTag:check")
    }
    Column(
        Modifier.width(SizeTokens.WIDGET_RAIL.dp).fillMaxHeight().then(tap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Segment(top, Modifier.weight(1f))
        if (repeat) {
            RepeatMark(repeatColor, Modifier.size(SizeTokens.WIDGET_NODE.dp).alpha(fade).testTag("$testTag:repeat"))
        } else {
            Node(if (selected == true) colors.accent else ring,
                if (selected != null) (if (selected) 1f else 0f) else done,
                Modifier.size(SizeTokens.WIDGET_NODE.dp).alpha(fade).testTag("$testTag:node"), fill)
        }
        Segment(bottom, Modifier.weight(1f))
    }
}

/** Кружок: кольцо цвета [ring]; при [done] → 1 заливается цветом [fill] (тусклый у высокого приоритета), по центру белая галочка. */
@Composable
private fun Node(ring: Color, done: Float, modifier: Modifier, fill: Color = ring) {
    Box(modifier.border(SizeTokens.CHECKBOX_STROKE.dp, if (done >= 1f) fill else ring, CircleShape), contentAlignment = Alignment.Center) {
        if (done > 0f) {
            Box(Modifier.matchParentSize().alpha(done).background(fill, CircleShape))
            Icon(
                painterResource(R.drawable.ic_node_check),
                null,
                tint = AppTheme.colors.onAccent,
                modifier = Modifier.size(SizeTokens.NODE_CHECK.dp).alpha(done),
            )
        }
    }
}

@Composable
private fun Segment(visible: Boolean, modifier: Modifier) {
    Box(modifier.width(AppTheme.sizes.hairline).background(if (visible) AppTheme.colors.divider else Color.Transparent))
}

/** Отметка «сейчас»: зелёное время слева, точка на линии и линия до правого края. */
@Composable
fun LazyItemScope.TimelineNowRow(nowMinute: Int, top: Boolean, bottom: Boolean) {
    if (LocalCompactTimeline.current) { CompactTimelineNowRow(nowMinute, top, bottom); return }
    val colors = AppTheme.colors
    Row(
        Modifier.animateItem().fillMaxWidth().height(SizeTokens.WIDGET_NOW_ROW.dp).padding(end = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d:%02d".format(nowMinute / MINUTES_PER_HOUR, nowMinute % MINUTES_PER_HOUR),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.accent,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(SizeTokens.WIDGET_TIME.dp),
        )
        Box(Modifier.width(SizeTokens.WIDGET_RAIL.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Segment(top, Modifier.weight(1f))
                Segment(bottom, Modifier.weight(1f))
            }
            Box(Modifier.size(SizeTokens.WIDGET_NOW_DOT.dp).background(colors.accent, CircleShape))
        }
        Box(Modifier.weight(1f).height(AppTheme.sizes.hairline).background(colors.accent))
    }
}

/** Переход к выполненной (решение PM: плавно, 0,3 с) и бледность выполненной плашки (70%). */

private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val MILLIS_PER_SECOND = 1000
