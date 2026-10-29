package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.shortDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * Окно даты и времени (итерации 33–34, макеты `VariantC3`/`VariantC4`): в шторке задачи два поля — дата и время; по нажатию
 * окно на весь экран: сетка на 3 дня со временем, блок «2 недели» и внизу «←» (отмена), «без даты», листание недель,
 * «Календарь» (2 месяца) и галочка.
 */

/** День для поля даты: «чт, 29 окт» (другой год — с годом); без даты — null. */
fun sheetDayText(fields: SheetFields): String? {
    val date = fields.selectedDate ?: return null
    val year = if (date.year != fields.today.year) " ${date.year}" else ""
    return "${shortDay(date.dayOfWeek)}, ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}$year"
}

/** Время для поля времени: «11:30» или период «10:00–11:30»; без времени — null. */
fun sheetTimeText(fields: SheetFields): String? {
    val minute = fields.selectedMinute ?: return null
    return if (fields.start is TaskDue.AtTime) "${clock(minute)}–${clock(minute + fields.durationMinutes)}" else clock(minute)
}

/** Когда создана задача — для меню «⋮»: «чт, 01 окт 2026». */
fun createdText(date: LocalDate): String = "${shortDay(date.dayOfWeek)}, %02d ${MONTHS[date.monthValue - 1]} ${date.year}".format(date.dayOfMonth)

private fun clock(minute: Int): String = "%02d:%02d".format(minute / MINUTES_PER_HOUR % HOURS_PER_DAY, minute % MINUTES_PER_HOUR)

/**
 * Два поля вместо плиток дней (итерация 34): слева дата, рядом время; каждое со своим крестиком — у даты он убирает дату вместе
 * со временем, у времени — только время. Нажатие на любое поле открывает окно даты. Без даты — серая подпись «Без даты и времени».
 */
@Composable
fun DateRow(
    fields: SheetFields,
    onOpenDate: () -> Unit,
    onClearDate: () -> Unit,
    onClearTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val day = sheetDayText(fields)
    val time = sheetTimeText(fields)
    Row(modifier.fillMaxWidth().height(AppTheme.sizes.stripHeight), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (day == null) {
            DateField(R.drawable.ic_widget_all, stringResource(R.string.date_empty), colors.textSecondary, TAG_DATE_FIELD, TAG_DATE_TEXT, null, onOpenDate)
        } else {
            DateField(R.drawable.ic_widget_all, day, colors.accent, TAG_DATE_FIELD, TAG_DATE_TEXT, ClearAction(R.string.clear_date, TAG_CLEAR_DATE, onClearDate), onOpenDate)
            if (time != null) {
                DateField(R.drawable.ic_time, time, colors.accent, TAG_TIME_FIELD, TAG_TIME_TEXT, ClearAction(R.string.quick_clear_time, TAG_CLEAR_TIME, onClearTime), onOpenDate)
            }
        }
    }
}

/** Поле даты или времени: значок и текст открывают окно; крестик стоит рядом, вне нажатия на поле. */
@Composable
private fun DateField(@DrawableRes icon: Int, text: String, tint: Color, tag: String, textTag: String, clear: ClearAction?, onClick: () -> Unit) {
    val sizes = AppTheme.sizes
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.heightIn(min = sizes.stripHeight).clickable(role = Role.Button, onClick = onClick).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(sizes.stripIcon))
            Text(text, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.testTag(textTag))
        }
        clear?.let { ClearBadge(it) }
    }
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

    /** Подпись двух недель: «26 окт – 08 ноя» (число с нулём, решение PM). */
    fun range(first: LocalDate): String {
        val last = first.plusDays(DAYS - 1)
        return "%02d ${MONTHS[first.monthValue - 1]} – %02d ${MONTHS[last.monthValue - 1]}".format(first.dayOfMonth, last.dayOfMonth)
    }
}

/** Часть светлой подложки под тремя днями, что сейчас видны в сетке: первый, средний или последний из трёх. */
private enum class Band { NONE, FIRST, MIDDLE, LAST }

/**
 * Блок «2 недели» окна даты (макет `VariantC4`): недели пн–вс с понедельника [first]. Прошедшие дни бледные и недоступны,
 * сегодня в кольце, выбранный зелёный; три дня сетки — под светлой подложкой. Листание и подпись — в нижней строке ([WindowBar]).
 */
@Composable
fun WeekBlock(
    today: LocalDate,
    selected: LocalDate?,
    gridFirst: LocalDate,
    first: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs)) {
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

/**
 * Нижняя строка окна даты (макет `VariantC4`): «←» отменяет изменения и возвращает в шторку, «без даты», листание недель
 * «‹ 26 окт – 08 ноя ›» (на первой странице назад нельзя), «Календарь» значком и зелёная галочка. Кнопки сжимаются на узком
 * экране, чтобы подпись недель помещалась.
 */
@Composable
fun WindowBar(
    first: LocalDate,
    canGoBack: Boolean,
    onCancel: () -> Unit,
    onNoDate: () -> Unit,
    onFirstChange: (LocalDate) -> Unit,
    onCalendar: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val colors = AppTheme.colors
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs)) {
        val buttons = WINDOW_BUTTONS
        // Подпись недель растёт вместе с крупным шрифтом — под неё отводится больше места, кнопки сжимаются.
        val range = AppTheme.sizes.dateRangeMin * LocalDensity.current.fontScale
        val fit = ((maxWidth - spacing.xs * buttons - range) / buttons).coerceIn(AppTheme.sizes.dateButtonMin, AppTheme.sizes.toolButton)
        CompositionLocalProvider(LocalToolButtonSize provides fit) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ToolButton(R.string.detail_back, active = false, tag = TAG_TIMELINE_BACK, content = {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.textSecondary)
                }, onClick = onCancel)
                ToolButton(R.string.quick_when_none, active = false, tag = TAG_TIMELINE_NO_DATE, icon = R.drawable.ic_no_date, onClick = onNoDate)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Chevron(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.date_block_previous), TAG_DATE_PREV, canGoBack) { onFirstChange(first.minusDays(WeekBlockMath.DAYS)) }
                    Text(
                        WeekBlockMath.range(first),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).testTag(TAG_DATE_RANGE),
                    )
                    Chevron(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.date_block_next), TAG_DATE_NEXT, true) { onFirstChange(first.plusDays(WeekBlockMath.DAYS)) }
                }
                ToolButton(R.string.date_block_calendar, active = false, tag = TAG_DATE_CALENDAR, icon = R.drawable.ic_widget_all, tint = colors.accent, onClick = onCalendar)
                SheetConfirmButton(R.string.sheet_done, enabled = true, tag = TAG_TIMELINE_DONE, onClick = onDone)
            }
        }
    }
}

@Composable
private fun Chevron(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier
            .size(AppTheme.sizes.dateArrow)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) colors.textPrimary else colors.inactive.copy(alpha = DIMMED_ALPHA))
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
                    past -> colors.inactive.copy(alpha = DIMMED_ALPHA)
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

/** Кнопок в нижней строке окна даты: «←», «без даты», «Календарь», галочка. */
private const val WINDOW_BUTTONS = 4
private const val DIMMED_ALPHA = 0.6f

const val TAG_DATE_FIELD = "date_field"
const val TAG_DATE_TEXT = "date_text"
const val TAG_TIME_FIELD = "time_field"
const val TAG_TIME_TEXT = "time_text"
const val TAG_CLEAR_TIME = "clear_time"
const val TAG_TIMELINE_BACK = "timeline_back"
const val TAG_DATE_PREV = "date_prev"
const val TAG_DATE_NEXT = "date_next"
const val TAG_DATE_RANGE = "date_range"
const val TAG_DATE_CALENDAR = "date_calendar"
const val TAG_DATE_CELL = "date_cell"
const val TAG_CALENDAR_DIALOG = "calendar_dialog"
const val TAG_CALENDAR_MONTH = "calendar_month"
