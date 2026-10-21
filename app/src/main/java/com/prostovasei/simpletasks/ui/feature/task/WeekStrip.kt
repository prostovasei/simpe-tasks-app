package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Строка недели в шторке задачи (итерация 21, макет PM N2): 7 дней с сегодняшнего, под каждым — столбик занятости
 * с 12 до 24 часов (серое — время других задач). Выбранный день — зелёный. Нажатие на день — [onDay]
 * (открывает таймлайн или, в режиме «Без времени», ставит день); ниже — «Без времени» и «Без даты».
 */
@Composable
fun WeekStrip(
    today: LocalDate,
    selectedDate: LocalDate?,
    busy: List<BusySlot>,
    dayOnly: Boolean,
    onDay: (LocalDate) -> Unit,
    onDayOnly: () -> Unit,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    Column(modifier.fillMaxWidth().padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(Modifier.fillMaxWidth()) {
            repeat(WeekGridMath.VISIBLE_DAYS) { index ->
                val date = today.plusDays(index.toLong())
                StripDay(
                    date = date,
                    label = when (index) {
                        0 -> stringResource(R.string.strip_day_today)
                        1 -> stringResource(R.string.strip_day_tomorrow)
                        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, RUSSIAN).replaceFirstChar { it.uppercase() }
                    },
                    isSelected = date == selectedDate,
                    busy = busy.filter { it.date == date },
                    onClick = { onDay(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            StripChip(stringResource(R.string.strip_no_time), active = dayOnly, tag = TAG_STRIP_NO_TIME, onClick = onDayOnly)
            if (selectedDate != null) StripChip(stringResource(R.string.quick_when_none), active = false, tag = TAG_STRIP_NO_DATE, onClick = onClearDate)
        }
    }
}

@Composable
private fun StripDay(date: LocalDate, label: String, isSelected: Boolean, busy: List<BusySlot>, onClick: () -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val text = if (isSelected) colors.onAccent else colors.textPrimary
    Column(
        modifier = modifier
            .padding(horizontal = sizes.hairline)
            .clip(AppTheme.shapes.medium)
            .background(if (isSelected) colors.accent else colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(vertical = AppTheme.spacing.xxs)
            .testTag("$TAG_STRIP_DAY:$date"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) text else colors.textSecondary)
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = text)
        val bar = if (isSelected) colors.onAccent.copy(alpha = BAR_ON_ACCENT) else colors.background
        val taken = if (isSelected) colors.onAccent else colors.inactive
        Box(
            Modifier
                .padding(top = sizes.cardGap)
                .size(sizes.stripBarWidth, sizes.stripBarHeight)
                .clip(AppTheme.shapes.small)
                .background(bar)
                .drawBehind {
                    busy.forEach { slot ->
                        val top = StripMath.fraction(slot.startMinute) * size.height
                        val bottom = StripMath.fraction(slot.endMinute) * size.height
                        if (bottom > top) {
                            drawRoundRect(taken, Offset(0f, top), Size(size.width, bottom - top), CornerRadius(sizes.hairline.toPx() * 2))
                        }
                    }
                },
        )
    }
}

@Composable
private fun StripChip(text: String, active: Boolean, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.medium
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (active) colors.onAccent else colors.accent,
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.accent else colors.surface)
            .border(AppTheme.sizes.checkboxStroke, colors.accent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xxs)
            .testTag(tag),
    )
}

/** Столбик занятости показывает вторую половину дня — с 12 до 24 часов (решение PM, макет N2). */
object StripMath {
    const val FROM_MINUTE = 12 * 60
    private const val SPAN = WeekGridMath.DAY_MINUTES - FROM_MINUTE

    /** Доля высоты столбика для минуты суток; утро — у верхнего края. */
    fun fraction(minute: Int): Float = ((minute - FROM_MINUTE).coerceIn(0, SPAN)).toFloat() / SPAN
}

private val RUSSIAN = Locale.forLanguageTag("ru")
private const val BAR_ON_ACCENT = 0.25f

const val TAG_STRIP_DAY = "strip_day"
const val TAG_STRIP_NO_TIME = "strip_no_time"
const val TAG_STRIP_NO_DATE = "strip_no_date"
