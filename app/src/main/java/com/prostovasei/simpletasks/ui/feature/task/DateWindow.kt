package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.ReminderIcon
import com.prostovasei.simpletasks.ui.common.shortDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * Окно даты и времени (итерация 33, решения PM, макеты `VariantC2`/`VariantC3`): в шторке задачи вместо плиток дней и значка
 * месяца — одна полоска, а по её нажатию — окно на весь экран: сетка на 3 дня со временем, блок «2 недели» со стрелками
 * и кнопкой «Календарь» (2 месяца), внизу «без даты», «без времени», повтор и уведомления.
 */

/** Дата и время для полоски: «чт, 29 окт · 11:30» или «… · 10:00–11:30»; без времени — только день; без даты — null. */
fun sheetDateText(fields: SheetFields): String? {
    val date = fields.selectedDate ?: return null
    val year = if (date.year != fields.today.year) " ${date.year}" else ""
    val day = "${shortDay(date.dayOfWeek)}, ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}$year"
    val minute = fields.selectedMinute ?: return day
    val isPeriod = fields.start is TaskDue.AtTime
    val time = if (isPeriod) "${clock(minute)}–${clock(minute + fields.durationMinutes)}" else clock(minute)
    return "$day · $time"
}

private fun clock(minute: Int): String = "%02d:%02d".format(minute / MINUTES_PER_HOUR % HOURS_PER_DAY, minute % MINUTES_PER_HOUR)

/**
 * Полоска даты в шторке: слева дата и время (нажать — окно даты) и крестик «убрать дату», справа повтор и уведомления
 * (нажать — их окна). Без даты повтор и уведомления бледные; выбранные — зелёные на белой плашке; колокольчик — со знаком
 * «заранее» или «несколько».
 */
@Composable
fun DateStrip(
    fields: SheetFields,
    onOpenDate: () -> Unit,
    onClearDate: () -> Unit,
    onOpenRepeat: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val spacing = AppTheme.spacing
    val text = sheetDateText(fields)
    val has = text != null
    val shape = AppTheme.shapes.extraLarge
    val openLabel = stringResource(R.string.strip_pick_date)
    val hint = stringResource(R.string.date_strip_hint)
    Row(
        modifier
            .fillMaxWidth()
            .height(sizes.stripHeight)
            .clip(shape)
            .background(if (has) colors.selected else colors.surface)
            .border(sizes.hairline, if (has) colors.selected else colors.divider, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onOpenDate)
                .semantics { contentDescription = openLabel }
                .padding(start = spacing.md)
                .testTag(TAG_DATE_STRIP),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Icon(painterResource(R.drawable.ic_widget_all), null, tint = if (has) colors.accent else colors.textPrimary, modifier = Modifier.size(sizes.stripIcon))
            Text(
                text ?: hint,
                color = if (has) colors.accent else colors.inactive,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (has) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).testTag(TAG_DATE_STRIP_TEXT),
            )
            if (has) ClearBadge(ClearAction(R.string.clear_date, TAG_CLEAR_DATE, onClearDate))
        }
        Box(Modifier.size(sizes.hairline, sizes.stripDivider).background(if (has) colors.accentSoft else colors.divider))
        Row(Modifier.padding(horizontal = spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
            val repeatSelected = has && fields.rrule != null
            StripButton(repeatSelected, TAG_STRIP_REPEAT, R.string.quick_tool_repeat, onOpenRepeat) {
                Icon(
                    painterResource(R.drawable.ic_tool_repeat),
                    null,
                    tint = stripTint(repeatSelected, has),
                    modifier = Modifier.size(sizes.stripIcon),
                )
            }
            val mark = Reminders.mark(fields.due, fields.reminders)
            StripButton(mark != null, TAG_STRIP_REMINDER, R.string.quick_tool_reminder, onOpenReminders) {
                if (mark != null) {
                    ReminderIcon(mark, sizes.stripIcon, tint = colors.accent, earlyColor = colors.accent)
                } else {
                    Icon(painterResource(R.drawable.ic_tool_reminder), null, tint = stripTint(false, has), modifier = Modifier.size(sizes.stripIcon))
                }
            }
        }
    }
}

/** Значок в полоске: выбранный — зелёный, доступный — серый, без даты — совсем бледный. */
@Composable
private fun stripTint(selected: Boolean, available: Boolean): Color {
    val colors = AppTheme.colors
    return when {
        selected -> colors.accent
        available -> colors.textSecondary
        else -> colors.inactive.copy(alpha = DIMMED_STRIP_ALPHA)
    }
}

@Composable
private fun StripButton(selected: Boolean, tag: String, label: Int, onClick: () -> Unit, content: @Composable () -> Unit) {
    val description = stringResource(label)
    Box(
        Modifier
            .size(AppTheme.sizes.stripButton)
            .clip(AppTheme.shapes.large)
            .background(if (selected) AppTheme.colors.surface else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Блок «2 недели»: недели с понедельника, их первая неделя и листание на 2 недели. */
object WeekBlockMath {
    const val DAYS = 14L

    fun monday(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    /**
     * Первая неделя блока. Сначала — текущая; пока выбранный день внутри блока, он не прыгает. Выбранный день вне блока
     * (свайпом ушли дальше) — блок показывает его неделю первой, но не раньше текущей недели.
     */
    fun first(today: LocalDate, selected: LocalDate?, current: LocalDate?): LocalDate {
        val base = monday(today)
        val start = current ?: base
        if (selected == null || (selected >= start && selected < start.plusDays(DAYS))) return start
        return maxOf(monday(selected), base)
    }
}

/** Часть светлой подложки под тремя днями, что сейчас видны в сетке: первый, средний или последний из трёх. */
private enum class Band { NONE, FIRST, MIDDLE, LAST }

/**
 * Блок «2 недели» окна даты (макет `VariantC2`): над ним «‹ 26 окт – 8 ноя ›» и кнопка «Календарь», ниже недели
 * пн–вс. Прошедшие дни бледные и недоступны, сегодня в кольце, выбранный зелёный; три дня сетки — под светлой подложкой.
 */
@Composable
fun WeekBlock(
    today: LocalDate,
    selected: LocalDate?,
    gridFirst: LocalDate,
    first: LocalDate,
    onFirstChange: (LocalDate) -> Unit,
    onPick: (LocalDate) -> Unit,
    onCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val last = first.plusDays(WeekBlockMath.DAYS - 1)
    val canGoBack = first > WeekBlockMath.monday(today)
    val previous = stringResource(R.string.date_block_previous)
    val next = stringResource(R.string.date_block_next)
    Column(modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs)) {
        Row(Modifier.fillMaxWidth().padding(bottom = spacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            // «Календарь» занимает своё место первым, а стрелки и подпись делят остаток — на узком экране сжимается подпись.
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                RoundArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, previous, TAG_DATE_PREV, canGoBack) { onFirstChange(first.minusDays(WeekBlockMath.DAYS)) }
                Text(
                    "${first.dayOfMonth} ${MONTHS[first.monthValue - 1]} – ${last.dayOfMonth} ${MONTHS[last.monthValue - 1]}",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).testTag(TAG_DATE_RANGE),
                )
                RoundArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, next, TAG_DATE_NEXT, true) { onFirstChange(first.plusDays(WeekBlockMath.DAYS)) }
            }
            CalendarChip(onCalendar)
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    shortDay(day),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        repeat(2) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(DayOfWeek.entries.size) { column ->
                    val date = first.plusDays((week * DayOfWeek.entries.size + column).toLong())
                    val offset = java.time.temporal.ChronoUnit.DAYS.between(gridFirst, date).toInt()
                    val band = when (offset) {
                        0 -> Band.FIRST
                        1 -> Band.MIDDLE
                        WeekGridMath.TIMELINE_DAYS - 1 -> Band.LAST
                        else -> Band.NONE
                    }
                    DayCell(date, today, date == selected, band, showMonth = true, onClick = { onPick(date) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RoundArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(AppTheme.sizes.dateArrow)
            .clip(CircleShape)
            .background(colors.surface)
            .border(AppTheme.sizes.hairline, colors.divider, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) colors.textPrimary else colors.inactive.copy(alpha = DIMMED_STRIP_ALPHA))
    }
}

@Composable
private fun CalendarChip(onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        Modifier
            .height(AppTheme.sizes.dateArrow)
            .clip(CircleShape)
            .background(colors.selected)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.sm)
            .testTag(TAG_DATE_CALENDAR),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(painterResource(R.drawable.ic_widget_all), null, tint = colors.accent, modifier = Modifier.size(AppTheme.sizes.reminderIcon))
        Text(stringResource(R.string.date_block_calendar), color = colors.accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Ячейка дня: кружок с числом; прошедший день бледный и не нажимается; [band] — подложка под днями сетки. */
@Composable
private fun DayCell(date: LocalDate, today: LocalDate, selected: Boolean, band: Band, showMonth: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val past = date < today
    val bandShape = when (band) {
        Band.NONE, Band.MIDDLE -> RoundedCornerShape(0)
        Band.FIRST -> RoundedCornerShape(topStartPercent = HALF, bottomStartPercent = HALF)
        Band.LAST -> RoundedCornerShape(topEndPercent = HALF, bottomEndPercent = HALF)
    }
    Box(
        modifier
            .height(sizes.dateCell)
            .then(if (band != Band.NONE) Modifier.background(colors.selected, bandShape) else Modifier)
            .clickable(enabled = !past, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("$TAG_DATE_CELL:$date"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(sizes.dateDay)
                .clip(CircleShape)
                .then(
                    when {
                        selected -> Modifier.background(colors.accent)
                        date == today -> Modifier.border(sizes.checkboxStroke, colors.accent, CircleShape)
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    selected -> colors.onAccent
                    past -> colors.inactive.copy(alpha = DIMMED_STRIP_ALPHA)
                    else -> colors.textPrimary
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            )
        }
        // Первое число месяца подписано месяцем, чтобы в блоке было видно, где он кончился.
        if (showMonth && date.dayOfMonth == 1) {
            Text(
                MONTHS[date.monthValue - 1],
                color = colors.accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Календарь на ближайшие 2 месяца (решение PM): день выбирается сразу по нажатию, окно закрывается — «Готово» нет,
 * кнопка только «Закрыть». Прошедшие дни бледные и не нажимаются.
 */
@Composable
fun CalendarDialog(today: LocalDate, selected: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    SheetDialog(
        title = stringResource(R.string.calendar_title),
        tag = TAG_CALENDAR_DIALOG,
        onDismiss = onDismiss,
        onDone = null,
        subtitle = stringResource(R.string.calendar_subtitle),
        dismissLabel = R.string.update_dialog_close,
    ) {
        Column(Modifier.padding(horizontal = AppTheme.spacing.md)) {
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        shortDay(day),
                        color = AppTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val month = YearMonth.from(today)
            listOf(month, month.plusMonths(1)).forEach { MonthGrid(it, today, selected, onPick) }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, selected: LocalDate?, onPick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val blanks = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells = List(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Text(
        monthTitle(month),
        color = AppTheme.colors.textPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = AppTheme.spacing.sm, bottom = AppTheme.spacing.xxs, start = AppTheme.spacing.xs).testTag("$TAG_CALENDAR_MONTH:$month"),
    )
    cells.chunked(DayOfWeek.entries.size).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            repeat(DayOfWeek.entries.size) { column ->
                val date = week.getOrNull(column)
                if (date == null) {
                    Box(Modifier.weight(1f).height(AppTheme.sizes.dateCell))
                } else {
                    DayCell(date, today, date == selected, Band.NONE, showMonth = false, onClick = { onPick(date) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** «Октябрь 2026». */
private fun monthTitle(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN).replaceFirstChar { it.uppercase() } + " " + month.year

private val RUSSIAN = Locale.forLanguageTag("ru")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private const val HALF = 50
private const val DIMMED_STRIP_ALPHA = 0.6f

const val TAG_DATE_STRIP = "date_strip"
const val TAG_DATE_STRIP_TEXT = "date_strip_text"
const val TAG_STRIP_REPEAT = "strip_repeat"
const val TAG_STRIP_REMINDER = "strip_reminder"
const val TAG_DATE_PREV = "date_prev"
const val TAG_DATE_NEXT = "date_next"
const val TAG_DATE_RANGE = "date_range"
const val TAG_DATE_CALENDAR = "date_calendar"
const val TAG_DATE_CELL = "date_cell"
const val TAG_CALENDAR_DIALOG = "calendar_dialog"
const val TAG_CALENDAR_MONTH = "calendar_month"
