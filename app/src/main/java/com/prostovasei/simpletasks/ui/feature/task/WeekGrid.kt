package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.roundToInt

/** Прокрутка сетки по дням (дробная: 0 — первым виден сегодняшний день); её двигают и свайп, и кнопки. */
@Stable
class GridScroll(initial: Float) {
    var days by mutableFloatStateOf(initial)
}

/**
 * Сетка дней (итерация 16, макет PM; с итерации 21 — таймлайн на [visibleDays] дня): шапка дней и сутки шагом 30 минут.
 * Свайп вбок листает плавно по дням, вверх-вниз — по часам. Нажатие на день — «на весь день»,
 * нажатие на сетку — время; зелёный блок — выбранное время, серые — уже занятое другими задачами.
 * Зелёный блок после долгого нажатия перетаскивается, за ручку снизу — растягивается в интервал (итерация 17).
 * [nowMinute] — сейчас (минута от начала суток): к нему прокручивается сетка, если день не выбран или сегодня.
 */
@Composable
fun WeekGrid(
    scroll: GridScroll,
    today: LocalDate,
    nowMinute: Int,
    selectedDate: LocalDate?,
    selectedMinute: Int?,
    selectedDuration: Int,
    busy: List<BusySlot>,
    onDayTap: (LocalDate) -> Unit,
    onSlotTap: (LocalDate, Int) -> Unit,
    onMove: (LocalDate, Int) -> Unit,
    onResize: (Int) -> Unit,
    onClearTime: () -> Unit,
    onFirstDayChange: (LocalDate) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    visibleDays: Int = WeekGridMath.VISIBLE_DAYS,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val density = LocalDensity.current
    var scrollDays by scroll::days
    val first by remember { derivedStateOf { floor(scrollDays).toInt() } }
    val currentOnFirstDay by rememberUpdatedState(onFirstDayChange)
    val currentOnInteract by rememberUpdatedState(onInteract)
    val currentOnSlotTap by rememberUpdatedState(onSlotTap)
    LaunchedEffect(first) { currentOnFirstDay(today.plusDays(first.toLong())) }

    BoxWithConstraints(modifier.background(colors.surface)) {
        val dayWidth = (maxWidth - sizes.weekGutter) / visibleDays
        val dayPx = with(density) { dayWidth.toPx() }
        val hourPx = with(density) { sizes.weekHour.toPx() }
        val gapPx = with(density) { sizes.cardGap.toPx() }
        val visible = (first - 1)..(first + visibleDays)
        val limit = WeekGridMath.MAX_DAYS_AWAY.toFloat()
        val drag = rememberDraggableState { delta -> scrollDays = (scrollDays - delta / dayPx).coerceIn(-limit, limit) }
        val horizontal = Modifier.draggable(
            state = drag,
            orientation = Orientation.Horizontal,
            onDragStarted = { currentOnInteract() },
            onDragStopped = { velocity ->
                val target = WeekGridMath.settleTarget(scrollDays, velocity, dayPx)
                animate(scrollDays, target, initialVelocity = -velocity / dayPx, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                    scrollDays = value
                }
            },
        )
        val todayTint = colors.accentSoft.copy(alpha = TODAY_TINT)
        val columnX = { index: Int -> ((index - scrollDays) * dayPx).roundToInt() }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(sizes.weekHeader).then(horizontal)) {
                Spacer(Modifier.width(sizes.weekGutter))
                Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                    for (index in visible) {
                        val date = today.plusDays(index.toLong())
                        key(date) {
                            DayHeader(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                width = dayWidth,
                                tint = todayTint,
                                onClick = { currentOnInteract(); onDayTap(date) },
                                modifier = Modifier.offset { IntOffset(columnX(index), 0) },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = sizes.hairline, color = colors.divider)

            val vertical = rememberScrollState()
            LaunchedEffect(Unit) {
                withFrameNanos { }
                val target = selectedMinute ?: if (selectedDate == null || selectedDate == today) nowMinute else MORNING_MINUTE
                vertical.scrollTo(((target / MINUTES_PER_HOUR - 1f) * hourPx).roundToInt().coerceAtLeast(0))
            }
            Row(Modifier.weight(1f).fillMaxWidth().verticalScroll(vertical)) {
                HourLabels(Modifier.width(sizes.weekGutter))
                Box(
                    Modifier
                        .weight(1f)
                        .height(sizes.weekHour * HOURS)
                        .clipToBounds()
                        .then(horizontal)
                        .pointerInput(dayPx, hourPx, today) {
                            detectTapGestures { point ->
                                currentOnInteract()
                                val day = WeekGridMath.dayAt(point.x, scrollDays, dayPx)
                                currentOnSlotTap(today.plusDays(day.toLong()), WeekGridMath.minuteAt(point.y, hourPx))
                            }
                        }
                        .drawBehind {
                            val line = sizes.hairline.toPx()
                            val todayLeft = -scrollDays * dayPx
                            drawRect(todayTint, topLeft = Offset(todayLeft, 0f), size = Size(dayPx, size.height))
                            for (hour in 0..HOURS) {
                                val y = hour * hourPx
                                drawLine(colors.divider, Offset(0f, y), Offset(size.width, y), line)
                                if (hour < HOURS) drawLine(colors.divider.copy(alpha = HALF_HOUR_ALPHA), Offset(0f, y + hourPx / 2), Offset(size.width, y + hourPx / 2), line)
                            }
                            val firstColumn = floor(scrollDays).toInt()
                            for (index in firstColumn..firstColumn + visibleDays + 1) {
                                val x = (index - scrollDays) * dayPx
                                drawLine(colors.divider, Offset(x, 0f), Offset(x, size.height), line)
                            }
                            val nowY = nowMinute / MINUTES_PER_HOUR * hourPx
                            drawLine(colors.accent, Offset(todayLeft, nowY), Offset(todayLeft + dayPx, nowY), sizes.checkboxStroke.toPx())
                        }
                        .testTag(TAG_WEEK_GRID),
                ) {
                    busy.forEach { slot ->
                        val index = ChronoUnit.DAYS.between(today, slot.date).toInt()
                        if (index in visible) {
                            key(slot) {
                                BusyBlock(
                                    slot = slot,
                                    width = dayWidth - sizes.cardGap,
                                    height = sizes.weekHour * ((slot.endMinute - slot.startMinute) / MINUTES_PER_HOUR) - sizes.hairline,
                                    modifier = Modifier.offset {
                                        IntOffset(columnX(index) + (gapPx / 2).roundToInt(), (slot.startMinute / MINUTES_PER_HOUR * hourPx).roundToInt())
                                    },
                                )
                            }
                        }
                    }
                    if (selectedDate != null && selectedMinute != null) {
                        val index = ChronoUnit.DAYS.between(today, selectedDate).toInt()
                        if (index in visible) {
                            SelectedBlock(
                                index = index,
                                minute = selectedMinute,
                                duration = selectedDuration,
                                dayPx = dayPx,
                                hourPx = hourPx,
                                scrollDays = { scrollDays },
                                width = dayWidth - sizes.cardGap,
                                onMove = { shift, minute -> onMove(selectedDate.plusDays(shift.toLong()), minute) },
                                onResize = onResize,
                                onClear = onClearTime,
                                onInteract = onInteract,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, isToday: Boolean, isSelected: Boolean, width: Dp, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    val color = if (isSelected) colors.accent else colors.textPrimary
    Row(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .then(if (isToday) Modifier.background(tint) else Modifier)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
            .testTag("$TAG_GRID_DAY:$date"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            DAY_NUMBER.format(date),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.width(AppTheme.sizes.cardGap))
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, RUSSIAN),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) colors.accent else colors.textSecondary,
        )
    }
}

/** Часы слева: подпись у верхней линии каждого часа. */
@Composable
private fun HourLabels(modifier: Modifier) {
    Column(modifier) {
        repeat(HOURS) { hour ->
            Box(Modifier.fillMaxWidth().height(AppTheme.sizes.weekHour)) {
                Text(
                    HOUR_LABEL.format(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = AppTheme.sizes.cardGap),
                )
            }
        }
    }
}

/** Серая плашка уже занятого времени (от часа — со временем начала); выполненная — бледная и зачёркнутая. */
@Composable
private fun BusyBlock(slot: BusySlot, width: Dp, height: Dp, modifier: Modifier) {
    val colors = AppTheme.colors
    val showTime = slot.endMinute - slot.startMinute >= MINUTES_PER_HOUR
    Column(
        modifier = modifier
            .size(width, height)
            .clip(AppTheme.shapes.small)
            .background(colors.divider.copy(alpha = if (slot.isDone) BUSY_DONE_ALPHA else BUSY_ALPHA))
            .padding(horizontal = AppTheme.sizes.cardGap),
    ) {
        Text(
            slot.title,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            textDecoration = if (slot.isDone) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showTime) {
            Text(
                timeText(slot.startMinute),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/**
 * Зелёный блок выбранного времени ([duration] минут, колонка [index] от сегодня). Долгое нажатие и перетаскивание —
 * новое начало (встаёт на шаг 30 минут, можно в другой день); ручка снизу — длительность; крестик — убрать время.
 */
@Composable
private fun SelectedBlock(
    index: Int,
    minute: Int,
    duration: Int,
    dayPx: Float,
    hourPx: Float,
    scrollDays: () -> Float,
    width: Dp,
    onMove: (Int, Int) -> Unit,
    onResize: (Int) -> Unit,
    onClear: () -> Unit,
    onInteract: () -> Unit,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val density = LocalDensity.current
    val clearLabel = stringResource(R.string.quick_clear_time)
    val stepPx = hourPx * WeekGridMath.STEP_MINUTES / MINUTES_PER_HOUR
    var drag by remember { mutableStateOf<Offset?>(null) }
    var stretch by remember { mutableStateOf<Float?>(null) }
    val current by rememberUpdatedState(SlotPosition(minute, duration))
    val dragTarget = drag?.let { WeekGridMath.moveTarget(current.minute, current.duration, it.x, it.y, dayPx, stepPx) }
    val shownMinute = dragTarget?.second ?: minute
    val shownDuration = stretch?.let { WeekGridMath.resizeTarget(minute, duration, it, stepPx) } ?: duration
    val shift = dragTarget?.first ?: 0
    val height = with(density) { (shownDuration / MINUTES_PER_HOUR * hourPx).toDp() } - sizes.hairline
    val active = drag != null || stretch != null
    val gapPx = with(density) { sizes.cardGap.toPx() }

    Box(
        Modifier.offset {
            IntOffset(((index + shift - scrollDays()) * dayPx + gapPx / 2).roundToInt(), (shownMinute / MINUTES_PER_HOUR * hourPx).roundToInt())
        },
    ) {
        Box(
            Modifier
                .size(width, height)
                .then(if (active) Modifier.shadow(sizes.navShadow, AppTheme.shapes.small) else Modifier)
                .clip(AppTheme.shapes.small)
                .background(colors.accent)
                .pointerInput(dayPx, stepPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onInteract(); drag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            drag = (drag ?: Offset.Zero) + amount
                        },
                        onDragEnd = {
                            val (days, start) = WeekGridMath.moveTarget(current.minute, current.duration, drag?.x ?: 0f, drag?.y ?: 0f, dayPx, stepPx)
                            drag = null
                            if (days != 0 || start != current.minute) onMove(days, start)
                        },
                        onDragCancel = { drag = null },
                    )
                }
                .testTag(TAG_GRID_SELECTION),
            contentAlignment = Alignment.Center,
        ) {
            // Интервал — начало и конец в две строки: в узкую колонку «10:00–11:30» не помещается.
            val label = if (shownDuration > WeekGridMath.STEP_MINUTES) "${timeText(shownMinute)}\n${timeText(shownMinute + shownDuration)}" else timeText(shownMinute)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = colors.onAccent, maxLines = 2)
        }
        if (!active) {
            // Крестик сидит на правом верхнем углу блока; зона нажатия больше самого значка.
            Box(
                Modifier
                    .offset(x = width - sizes.weekClear, y = -sizes.weekClear)
                    .size(sizes.weekClear * 2)
                    .clickable(onClick = onClear)
                    .semantics { contentDescription = clearLabel }
                    .testTag(TAG_GRID_CLEAR),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(sizes.weekClear)
                        .background(colors.surface, CircleShape)
                        .border(sizes.hairline, colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = colors.accent, modifier = Modifier.size(sizes.metaIcon))
                }
            }
        }
        if (drag == null) {
            // Ручка по центру нижнего края: тянется сразу, без долгого нажатия; нажатие на неё не ставит новое время.
            Box(
                Modifier
                    .offset(x = width / 2 - sizes.weekClear, y = height - sizes.weekClear)
                    .size(sizes.weekClear * 2)
                    .pointerInput(stepPx) {
                        detectVerticalDragGestures(
                            onDragStart = { onInteract(); stretch = 0f },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                // Первый сдвиг приходит без порога касания — добавляем его, чтобы ручка шла за пальцем.
                                val slop = if (stretch == 0f) sign(amount) * viewConfiguration.touchSlop else 0f
                                stretch = (stretch ?: 0f) + amount + slop
                            },
                            onDragEnd = {
                                val minutes = WeekGridMath.resizeTarget(current.minute, current.duration, stretch ?: 0f, stepPx)
                                stretch = null
                                if (minutes != current.duration) onResize(minutes)
                            },
                            onDragCancel = { stretch = null },
                        )
                    }
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(TAG_GRID_RESIZE),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(sizes.weekKnob)
                        .background(colors.surface, CircleShape)
                        .border(sizes.checkboxStroke, colors.accent, CircleShape),
                )
            }
        }
    }
}

private data class SlotPosition(val minute: Int, val duration: Int)

private fun timeText(minute: Int): String = "%02d:%02d".format(minute / 60 % 24, minute % 60)

private val RUSSIAN = Locale.forLanguageTag("ru")
private val DAY_NUMBER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd")
private const val HOUR_LABEL = "%02d"
private const val HOURS = 24
private const val MINUTES_PER_HOUR = 60f
private const val MORNING_MINUTE = 8 * 60
private const val TODAY_TINT = 0.45f
private const val HALF_HOUR_ALPHA = 0.5f
private const val BUSY_ALPHA = 0.7f
private const val BUSY_DONE_ALPHA = 0.35f

const val TAG_WEEK_GRID = "week_grid"
const val TAG_GRID_DAY = "grid_day"
const val TAG_GRID_SELECTION = "grid_selection"
const val TAG_GRID_CLEAR = "grid_clear_time"
const val TAG_GRID_RESIZE = "grid_resize"
