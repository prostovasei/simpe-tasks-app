package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
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
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val days = remember(today) { (0L until DayStripMath.DAYS).map { today.plusDays(it) } }
    val outside = selectedDate?.takeIf { it !in days }
    val list = rememberLazyListState()
    val density = LocalDensity.current
    // Выбранный день дальше третьей плитки — подвинуть ленту, чтобы он был виден. Дата дальше двух недель стоит
    // отдельной плиткой после значка — лента тогда в начале, с сегодняшнего дня (0.27.1: раньше уезжала в конец).
    LaunchedEffect(selectedDate) {
        val index = selectedDate?.takeIf { it in days }?.let { ChronoUnit.DAYS.between(today, it).toInt() } ?: -1
        if (index >= VISIBLE_TILES) list.animateScrollToItem(index - 1) else if (outside != null) list.scrollToItem(0)
    }
    // Размер плитки — от ширины экрана (0.22.3): значок, 3 дня целиком и узкий край четвёртого на любом телефоне.
    // Текст в плитке — доли её стороны (0.22.2), поэтому всегда помещается.
    BoxWithConstraints(modifier.fillMaxWidth().padding(start = spacing.md)) {
    val tile = ((maxWidth - spacing.xs * (VISIBLE_TILES + 1)) / (VISIBLE_TILES + 1 + PEEK))
        .coerceIn(AppTheme.sizes.dayTile * MIN_SCALE, AppTheme.sizes.dayTile * MAX_SCALE)
    val text = remember(tile, density) { TileText.of(tile, density) }
    // Сверху — место под крестик выбранного дня, он выступает за угол плитки (итерация 24).
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.Bottom) {
        CalendarTile(tile, onCalendar)
        if (outside != null) DayTile(outside, time, selected = true, tile, text, onClear, onClick = { onDay(outside) })
        LazyRow(
            state = list,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            contentPadding = PaddingValues(top = AppTheme.sizes.weekClear, end = spacing.md),
            modifier = Modifier.weight(1f).testTag(TAG_DAY_TILES),
        ) {
            items(days.size) { index ->
                val date = days[index]
                val selected = date == selectedDate
                DayTile(date, if (selected) time else emptyList(), selected, tile, text, onClear, Modifier.zIndex(if (selected) 1f else 0f), onClick = { onDay(date) })
            }
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
            .background(colors.surface)
            .border(AppTheme.sizes.hairline, colors.divider, AppTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(TAG_DAY_CALENDAR),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_widget_all), null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.toolIcon))
    }
}

@Composable
private fun DayTile(
    date: LocalDate,
    time: List<String>,
    selected: Boolean,
    tile: Dp,
    text: TileText,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier) {
        DayTileBody(date, time, selected, tile, text, onClick)
        // Крестик у выбранного дня — снаружи в правом верхнем углу, как у кнопок панели (итерация 24): убрать дату и время.
        if (selected) {
            val clear = AppTheme.sizes.weekClear
            ClearBadge(ClearAction(R.string.clear_date, TAG_CLEAR_DATE, onClear), Modifier.align(Alignment.TopEnd).offset(x = clear, y = -clear))
        }
    }
}

@Composable
private fun DayTileBody(date: LocalDate, time: List<String>, selected: Boolean, tile: Dp, text: TileText, onClick: () -> Unit) {
    val colors = AppTheme.colors
    // Итерация 26: невыбранные — белые с тонкой рамкой, выбранный — светло-зелёный с зелёным текстом.
    val main = if (selected) colors.accent else colors.textPrimary
    val minor = if (selected) colors.accent.copy(alpha = MINOR_ON_ACCENT) else colors.textSecondary
    val big = text.day.copy(color = main)
    Row(
        Modifier
            .size(tile)
            .clip(AppTheme.shapes.large)
            .background(if (selected) colors.selected else colors.surface)
            .border(AppTheme.sizes.hairline, if (selected) colors.selected else colors.divider, AppTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = AppTheme.spacing.xs, end = AppTheme.spacing.xs, top = tile * TOP_PAD)
            .testTag("$TAG_DAY_TILE:$date"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Строки фиксированной высоты (0.22.3): «чт», «22» и «окт» плотно друг под другом на любом телефоне.
        Column(Modifier.alignBy { it[FirstBaseline] - text.dayTop }) {
            Line(tile * DAY_LINE) { Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, RUSSIAN), style = big, maxLines = 1, modifier = it) }
            Line(tile * DAY_LINE) { Text("%02d".format(date.dayOfMonth), style = big, maxLines = 1, modifier = it) }
            Line(tile * MONTH_LINE) { Text(MONTHS[date.monthValue - 1], style = text.month, color = minor, maxLines = 1, modifier = it) }
        }
        // Время — бледнее и тоньше даты; верх цифр — на одной линии с верхом «чт».
        if (time.isNotEmpty()) {
            Column(Modifier.alignBy { it[FirstBaseline] - text.timeTop }, horizontalAlignment = Alignment.End) {
                time.forEach { line ->
                    Line(tile * TIME_LINE) { Text(line, style = text.time, color = minor, maxLines = 1, modifier = it.then(Modifier.testTag(TAG_DAY_TIME))) }
                }
            }
        }
    }
}

/** Строка заданной высоты: текст по центру, выступы шрифта сверху и снизу не раздвигают строки. */
@Composable
private fun Line(height: Dp, content: @Composable (Modifier) -> Unit) {
    Box(Modifier.height(height), contentAlignment = Alignment.CenterStart) {
        content(Modifier.wrapContentHeight(unbounded = true))
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
private class TileText(
    val day: androidx.compose.ui.text.TextStyle,
    val month: androidx.compose.ui.text.TextStyle,
    val time: androidx.compose.ui.text.TextStyle,
    /** От базовой линии до верха строчных «чт» и до верха цифр времени, px — по ним выравнивается время. */
    val dayTop: Int,
    val timeTop: Int,
) {
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
                dayTop = (tile * DAY_SIZE * X_HEIGHT).roundToPx(),
                timeTop = (tile * TIME_SIZE * DIGIT_HEIGHT).roundToPx(),
            )
        }
    }
}

private const val MAX_SCALE = 1.3f
private const val DAY_SIZE = 0.26f
private const val DAY_LINE = 0.27f
private const val MONTH_SIZE = 0.14f
private const val MONTH_LINE = 0.18f
private const val TIME_SIZE = 0.155f
private const val TIME_LINE = 0.19f
private const val TOP_PAD = 0.13f

/** Высота строчных букв и цифр шрифта Manrope в долях размера — для выравнивания времени по верху «чт». */
private const val X_HEIGHT = 0.54f
private const val DIGIT_HEIGHT = 0.72f

/** Сколько видно от четвёртого дня и пределы размера плитки относительно `DAY_TILE`. */
private const val PEEK = 0.3f
private const val MIN_SCALE = 0.9f
private const val VISIBLE_TILES = 3
private val RUSSIAN = Locale.forLanguageTag("ru")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private const val MINOR_ON_ACCENT = 0.72f

const val TAG_DAY_TILES = "day_tiles"
const val TAG_DAY_TILE = "day_tile"
const val TAG_DAY_CALENDAR = "day_calendar"
const val TAG_DAY_TIME = "day_time"
