package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.Manrope
import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Плитки дней в шторке задачи (итерация 22, макет PM «Z1, время сверху»): слева значок календаря (не уезжает),
 * дальше [DayStripMath.DAYS] дней с сегодняшнего — свайпом, справа виден край следующего. Плитка квадратная:
 * «чт» и «22» крупно, «окт» мелко, время — бледное, сверху справа (период — начало над концом).
 * Выбранный день зелёный; дата из календаря за пределами двух недель — зелёной плиткой сразу после значка.
 */
@Composable
fun DayTiles(
    today: LocalDate,
    selectedDate: LocalDate?,
    time: List<String>,
    onDay: (LocalDate) -> Unit,
    onCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val days = remember(today) { (0L until DayStripMath.DAYS).map { today.plusDays(it) } }
    val outside = selectedDate?.takeIf { it !in days }
    val list = rememberLazyListState()
    // Крупный системный шрифт: плитка растёт, но не больше чем в MAX_SCALE раз, а текст в ней считается
    // от размера плитки (0.22.2) — так он всегда помещается, какими бы ни были шрифт и его высота строки.
    val density = LocalDensity.current
    val tile = AppTheme.sizes.dayTile * density.fontScale.coerceIn(1f, MAX_SCALE)
    val text = remember(tile, density) { TileText.of(tile, density) }
    // Выбранный день дальше третьей плитки — подвинуть ленту, чтобы он был виден.
    LaunchedEffect(selectedDate) {
        val index = selectedDate?.let { ChronoUnit.DAYS.between(today, it).toInt() } ?: -1
        if (index >= VISIBLE_TILES) list.animateScrollToItem(index - 1)
    }
    Row(modifier.fillMaxWidth().padding(start = spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
        CalendarTile(tile, onCalendar)
        if (outside != null) DayTile(outside, time, selected = true, tile, text, onClick = { onDay(outside) })
        LazyRow(
            state = list,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = PaddingValues(end = spacing.md),
            modifier = Modifier.weight(1f).testTag(TAG_DAY_TILES),
        ) {
            items(days.size) { index ->
                val date = days[index]
                val selected = date == selectedDate
                DayTile(date, if (selected) time else emptyList(), selected, tile, text, onClick = { onDay(date) })
            }
        }
    }
}

@Composable
private fun CalendarTile(tile: Dp, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val label = stringResource(R.string.strip_pick_date)
    Box(
        Modifier
            .size(tile)
            .clip(AppTheme.shapes.large)
            .background(colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(TAG_DAY_CALENDAR),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_widget_all), null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.toolIcon))
    }
}

@Composable
private fun DayTile(date: LocalDate, time: List<String>, selected: Boolean, tile: Dp, text: TileText, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val main = if (selected) colors.onAccent else colors.textPrimary
    val minor = if (selected) colors.onAccent.copy(alpha = MINOR_ON_ACCENT) else colors.textSecondary
    val big = text.day.copy(color = main)
    Row(
        Modifier
            .size(tile)
            .clip(AppTheme.shapes.large)
            .background(if (selected) colors.accent else colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = AppTheme.spacing.xs, end = AppTheme.spacing.xs, top = tile * TOP_PAD)
            .testTag("$TAG_DAY_TILE:$date"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, RUSSIAN), style = big, maxLines = 1)
            Text("%02d".format(date.dayOfMonth), style = big, maxLines = 1)
            Text(
                MONTHS[date.monthValue - 1],
                style = text.month,
                color = minor,
                maxLines = 1,
            )
        }
        // Время — бледнее и тоньше даты, верх — на одной линии с днём недели.
        Column(Modifier.padding(top = tile * TIME_TOP), horizontalAlignment = Alignment.End) {
            time.forEach {
                Text(
                    it,
                    style = text.time,
                    color = minor,
                    maxLines = 1,
                    modifier = Modifier.testTag(TAG_DAY_TIME),
                )
            }
        }
    }
}

/** Лента дней: сколько дней листается и сколько плиток видно целиком. */
object DayStripMath {
    const val DAYS = 14L

    /** Время на плитке: «15:00» или период — начало над концом; без времени — пусто. */
    fun timeLines(fields: SheetFields): List<String> {
        val minute = fields.selectedMinute ?: return emptyList()
        val start = clock(minute)
        val isPeriod = fields.start is TaskDue.AtTime
        return if (isPeriod) listOf(start, clock(minute + fields.durationMinutes)) else listOf(start)
    }

    private fun clock(minute: Int): String = "%02d:%02d".format(minute / MINUTES_PER_HOUR % HOURS_PER_DAY, minute % MINUTES_PER_HOUR)

    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
}

/**
 * Текст плитки — доли её стороны (0.22.2): «чт» и «22» — по 0,24 стороны, «окт» — 0,13, время — 0,115.
 * Даже с полной высотой строки шрифта (~1,37 размера) это ~0,9 стороны вместе с отступом сверху — запас
 * на любой шрифт. Размеры переводятся в sp без системного масштаба шрифта: он уже учтён в размере плитки.
 */
private class TileText(val day: androidx.compose.ui.text.TextStyle, val month: androidx.compose.ui.text.TextStyle, val time: androidx.compose.ui.text.TextStyle) {
    companion object {
        fun of(tile: Dp, density: Density): TileText = with(density) {
            fun style(size: Float, line: Float, weight: FontWeight) = androidx.compose.ui.text.TextStyle(
                fontFamily = Manrope,
                fontSize = (tile * size).toSp(),
                lineHeight = (tile * line).toSp(),
                fontWeight = weight,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            )
            TileText(
                day = style(DAY_SIZE, DAY_LINE, FontWeight.ExtraBold),
                month = style(MONTH_SIZE, MONTH_LINE, FontWeight.Bold),
                time = style(TIME_SIZE, TIME_LINE, FontWeight.Medium),
            )
        }
    }
}

private const val MAX_SCALE = 1.15f
private const val DAY_SIZE = 0.24f
private const val DAY_LINE = 0.27f
private const val MONTH_SIZE = 0.13f
private const val MONTH_LINE = 0.15f
private const val TIME_SIZE = 0.115f
private const val TIME_LINE = 0.14f
private const val TOP_PAD = 0.07f
private const val TIME_TOP = 0.02f
private const val VISIBLE_TILES = 3
private val RUSSIAN = Locale.forLanguageTag("ru")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private const val MINOR_ON_ACCENT = 0.72f

const val TAG_DAY_TILES = "day_tiles"
const val TAG_DAY_TILE = "day_tile"
const val TAG_DAY_CALENDAR = "day_calendar"
const val TAG_DAY_TIME = "day_time"
