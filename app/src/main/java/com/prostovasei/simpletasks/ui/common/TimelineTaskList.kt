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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import com.prostovasei.simpletasks.designsystem.tokens.TaskRowTokens
import com.prostovasei.simpletasks.widget.WidgetDates
import kotlinx.coroutines.delay
import java.time.LocalTime

/*
 * Задачи в приложении — как в виджете (итерация 26, решение PM): слева время, по центру сплошная линия
 * с кружком приоритета (нажатие — «выполнено»), справа белая плашка: название, напоминание, повтор, список.
 */

/**
 * Строки дня: задачи по шкале [WidgetTimeline]. #331: линии «сейчас» нет ([nowMinute] лишь гасит прошедшие задачи сегодня).
 * [dayLabels] — справа вместе со временем стоит день: в «Входящих», списках и блоке «Просрочено» (#334).
 * [today] — от него считаются «сегодня», «завтра» и дни недели просроченных.
 */
fun LazyListScope.timelineItems(
    items: List<TaskItem>,
    keyPrefix: String,
    days: DayNames,
    nowMinute: Int?,
    @Suppress("UNUSED_PARAMETER") showNowMarker: Boolean = true,
    isOverdue: (TaskItem) -> Boolean,
    showList: Boolean,
    onToggle: (TaskItem) -> Unit,
    onOpen: (TaskItem) -> Unit,
    dayLabels: Boolean = false,
    /** Режим выбора (итерация 40): null — выключен; иначе выбранные id. В режиме нажатие и кружок отмечают задачу. */
    selection: Set<String>? = null,
    /** Нажатие в режиме выбора и долгое нажатие вне его; null — выбирать на этом экране нельзя. */
    onSelect: ((TaskItem) -> Unit)? = null,
    /** #314: появление строк по очереди (окно дня); null — строки появляются сразу. */
    rowMotion: (@Composable (index: Int, content: @Composable () -> Unit) -> Unit)? = null,
    today: LocalDate = LocalDate.now(),
) {
    val rows = WidgetTimeline.rows(items, overdue = dayLabels, nowMinute = nowMinute.takeUnless { dayLabels }, days = days)
        .filterIsInstance<WidgetRow.Task>()
    // Ключ с днём: при переносе между днями Room обновляет списки не одновременно, и одна задача на миг бывает в двух днях сразу —
    // с одним лишь id это «Key was already used» и вылет.
    itemsIndexed(rows, key = { _, row -> "$keyPrefix:${row.item.id}" }) { index, row ->
        val item = row.item
        val selectable = onSelect != null && !item.isGhost
        val selecting = selection != null
        val content: @Composable () -> Unit = {
            TimelineTaskRow(
                row, top = index > 0, bottom = index < rows.lastIndex, isOverdue = isOverdue(item), showList = showList,
                onToggle = { if (selecting) { if (selectable) onSelect?.invoke(item) } else onToggle(item) },
                onClick = { if (selecting) { if (selectable) onSelect?.invoke(item) } else onOpen(item) },
                dayLabels = dayLabels,
                selected = if (selecting && !item.isGhost) item.id in selection.orEmpty() else null,
                onLongClick = if (selectable && !selecting) { { onSelect?.invoke(item) } } else null,
                today = today,
            )
        }
        if (rowMotion != null) rowMotion(index, content) else content()
    }
}

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

/** Что стоит справа (#334): время «14:00–16:00» одной строкой; у просроченной в «Просрочено» — день недели или дата; в списках — день и время. */
@Composable
fun taskRightLabel(row: WidgetRow.Task, isOverdue: Boolean, dayLabels: Boolean, today: LocalDate): String? {
    val item = row.item
    if (!dayLabels) return row.start?.let { start -> row.end?.let { "$start–$it" } ?: start }
    val due = item.due ?: return null
    val date = when (val day = due.day) {
        DayLabel.Today -> today
        DayLabel.Tomorrow -> today.plusDays(1)
        DayLabel.Yesterday -> today.minusDays(1)
        is DayLabel.Date -> day.date
    }
    if (isOverdue || date < today) return WidgetDates.overdueDay(date, today)
    return WidgetDates.listDay(
        date, due.time?.substringBefore("–"), today,
        stringResource(R.string.day_today).lowercase(), stringResource(R.string.day_tomorrow).lowercase(),
    )
}

/**
 * Строка задачи (#334, конструкторы PM 25.11.2026): кружок 18 → эмодзи списка 16 → название 14 sp полужирное → колокольчик 14 → время
 * 13 sp справа. Высокий приоритет — ярким фиолетовым, выполненные и прошедшие — «ещё светлее»; выполненная тает к этому цвету (#316).
 * У повторяющейся вместо кружка — знак повтора (стрелки-треугольники, один рисунок везде). Линий шкалы нет (#331).
 */
@Composable
fun LazyItemScope.TimelineTaskRow(
    row: WidgetRow.Task,
    @Suppress("UNUSED_PARAMETER") top: Boolean,
    @Suppress("UNUSED_PARAMETER") bottom: Boolean,
    isOverdue: Boolean,
    showList: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    dayLabels: Boolean = false,
    /** Режим выбора (итерация 40): null — выключен; true — строка выбрана (галочка в кружке, рамка на строке). */
    selected: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    today: LocalDate = LocalDate.now(),
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val item = row.item
    val testTag = "task:${item.id}"
    val done = doneCheck(item.showsDone)
    // #316: выполненная «тает» — цвет плавно уходит к бледному.
    val melt = ((1f - doneAlpha(item.showsDone)) / (1f - DONE_ALPHA)).coerceIn(0f, 1f)
    val base = if (item.priority == Priority.HIGH) colors.taskRow.priority else colors.taskRow.title
    // #342 (решение PM): прошедшая невыполненная за сегодня не бледнеет — у неё красное время.
    val late = row.past && !item.showsDone
    val textColor = lerp(base, colors.taskRow.done, melt)
    val iconAlpha = 1f - melt * (1f - TaskRowTokens.FADED_ICON_ALPHA)
    val right = taskRightLabel(row, isOverdue, dayLabels, today)
    val height = (SizeTokens.WIDGET_ROW + if (item.subtitle != null) SizeTokens.TRASH_ROW_EXTRA else 0).dp
    Row(
        Modifier
            .animateItem()
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = spacing.xs)
            .clip(AppTheme.shapes.medium)
            .then(if (selected == true) Modifier.background(colors.selected).border(SizeTokens.CHECKBOX_STROKE.dp, colors.accent, AppTheme.shapes.medium) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(end = spacing.xs)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Check(item, textColor, done, testTag, onToggle, selected)
        if (showList) {
            Text(
                item.listEmoji,
                fontSize = SizeTokens.TASK_EMOJI_SP.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(SizeTokens.TASK_EMOJI.dp).alpha(iconAlpha),
            )
            Spacer(Modifier.width(SizeTokens.TASK_EMOJI_GAP.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = SizeTokens.TASK_TITLE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("$testTag:title"),
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
        item.reminder?.let {
            ReminderIcon(
                mark = it,
                size = SizeTokens.TASK_BELL.dp,
                tint = colors.textSecondary,
                earlyColor = colors.accent,
                modifier = Modifier.padding(start = SizeTokens.TASK_RIGHT_GAP.dp).alpha(iconAlpha).testTag("$testTag:reminder"),
            )
        }
        right?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                fontSize = SizeTokens.TASK_TIME_SP.sp,
                fontWeight = FontWeight.Medium,
                color = if ((isOverdue || late) && !item.showsDone) colors.overdue else textColor,
                maxLines = 1,
                modifier = Modifier.padding(start = SizeTokens.TASK_RIGHT_GAP.dp).testTag("$testTag:time"),
            )
        }
    }
}

/**
 * Кружок выполнения 18 dp; нажатие — «выполнено» или, у выполненной, вернуть. [done] — доля перехода: кольцо заливается и
 * появляется белая галочка. У повторяющейся — знак повтора без галочки. В режиме выбора — галочка цвета акцента.
 */
@Composable
private fun Check(item: TaskItem, textColor: Color, done: Float, testTag: String, onToggle: () -> Unit, selected: Boolean?) {
    val colors = AppTheme.colors
    val ring = when {
        selected == true -> colors.accent
        item.priority == Priority.HIGH || item.showsDone -> textColor
        else -> if (textColor == colors.taskRow.done) textColor else colors.inactive
    }
    val label = stringResource(
        when {
            selected != null -> if (selected) R.string.select_unmark else R.string.select_mark
            item.showsDone -> R.string.task_uncomplete
            else -> R.string.widget_complete
        },
    )
    val tap = if (item.isGhost) Modifier else Modifier.clickable(onClick = onToggle).semantics { contentDescription = label }.testTag("$testTag:check")
    Box(
        Modifier.fillMaxHeight().then(tap).padding(start = AppTheme.spacing.xs, end = SizeTokens.TASK_CHECK_GAP.dp),
        contentAlignment = Alignment.Center,
    ) {
        val size = Modifier.size(SizeTokens.TASK_CHECK.dp)
        if (item.isRecurring && selected == null) {
            Icon(painterResource(R.drawable.ic_task_repeat_round), null, tint = ring, modifier = size.testTag("$testTag:repeat"))
        } else {
            Node(ring, if (selected != null) (if (selected) 1f else 0f) else done, size.testTag("$testTag:node"))
        }
    }
}

/** Кружок: кольцо цвета [ring]; при [done] → 1 заливается цветом [fill], по центру белая галочка. */
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

/** Переход к выполненной (решение PM: плавно, 0,3 с) и бледность выполненной плашки (70%). */

private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val MILLIS_PER_SECOND = 1000
