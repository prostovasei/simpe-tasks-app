package com.prostovasei.simpletasks.ui.feature.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.ui.feature.finance.FieldInput
import com.prostovasei.simpletasks.ui.feature.finance.FinanceField
import com.prostovasei.simpletasks.ui.feature.finance.FinanceWindow
import com.prostovasei.simpletasks.ui.feature.finance.PickValue
import com.prostovasei.simpletasks.ui.feature.finance.TwoActions
import com.prostovasei.simpletasks.ui.feature.finance.WindowText
import java.time.LocalDate

private const val WHOLE_FROM = Weight.MIN_TENTHS / 10
private const val WHOLE_TO = Weight.MAX_TENTHS / 10
private const val COMMENT_MAX = 80

/** Два барабана веса «82 , 4 кг» (#257). Целые — 30..250, десятые — 0..9; на 250 кг десятых нет. */
@Composable
fun WeightDrums(tenths: Int, onChange: (Int) -> Unit, tagPrefix: String, modifier: Modifier = Modifier) {
    val sizes = AppTheme.sizes
    val colors = AppTheme.colors
    val whole = tenths / 10
    val tenth = tenths % 10
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Drum(WHOLE_FROM, WHOLE_TO, whole, { onChange(Weight.clamp(it * 10 + tenth)) }, "$tagPrefix:whole", stringResource(R.string.health_drum_whole), sizes.healthDrumWhole)
        Text(",", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs))
        Drum(0, 9, tenth, { onChange(Weight.clamp(whole * 10 + it)) }, "$tagPrefix:tenth", stringResource(R.string.health_drum_tenth), sizes.healthDrumTenth)
        Text(stringResource(R.string.health_kg), style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, modifier = Modifier.padding(start = AppTheme.spacing.sm))
    }
}

/**
 * Окно ввода веса (#257, макеты 2.1–2.4): барабаны, дата (нажатие — календарь), комментарий. Одна запись на дату: если на выбранный день
 * вес уже есть, под датой подсказка «заменится». У правки в заголовке корзина. Поля живут в [HealthState.form], не в окне.
 */
@Composable
fun WeightDialog(state: HealthState, form: WeightForm, onIntent: (HealthIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var calendar by rememberSaveable { mutableStateOf(false) }
    val entries = state.entries.orEmpty()
    val editing = form.editingDate != null
    val last = entries.lastOrNull()
    val clash = entries.firstOrNull { it.date == form.date && it.date != form.editingDate }
    val edit: (WeightForm.() -> WeightForm) -> Unit = { onIntent(HealthIntent.EditForm(it)) }
    val close = { onIntent(HealthIntent.CloseForm) }

    FinanceWindow(
        title = if (editing) stringResource(R.string.health_entry_title, dayMonth(form.editingDate!!)) else stringResource(R.string.health_weight_title),
        tag = TAG_WEIGHT_WINDOW,
        onDismiss = close,
        headerAction = if (editing) {
            {
                val delete = stringResource(R.string.health_delete)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppTheme.sizes.financeClose)
                        .clip(AppTheme.shapes.large)
                        .clickable(role = Role.Button) { onIntent(HealthIntent.DeleteForm) }
                        .semantics { contentDescription = delete }
                        .testTag(TAG_WEIGHT_DELETE),
                ) { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger) }
            }
        } else {
            null
        },
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.health_save), TAG_WEIGHT_WINDOW, close, { onIntent(HealthIntent.SaveForm) }) },
    ) {
        WeightDrums(form.tenths, { edit { withTenths(it) } }, TAG_WEIGHT_DRUM, Modifier.padding(top = spacing.xs))
        if (!editing && last != null) WindowText(stringResource(R.string.health_prefill, Weight.format(last.tenths)), Modifier.fillMaxWidth().testTag(TAG_WEIGHT_PREFILL))
        FinanceField(stringResource(R.string.health_date)) {
            PickValue(dayLabelFull(form.date, state.today), TAG_WEIGHT_DATE, stringResource(R.string.health_date)) { calendar = true }
        }
        clash?.let {
            Text(
                stringResource(R.string.health_replace_warning, dayMonth(form.date), Weight.format(it.tenths)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(vertical = spacing.xs)
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.medium)
                    .background(colors.accentSoft)
                    .padding(spacing.xs)
                    .testTag(TAG_WEIGHT_CLASH),
            )
        }
        FinanceField(stringResource(R.string.health_comment)) {
            FieldInput(form.comment, { edit { copy(comment = it) } }, stringResource(R.string.health_comment_hint), TAG_WEIGHT_COMMENT, maxLength = COMMENT_MAX)
        }
    }
    if (calendar) {
        DateCalendarDialog(
            selected = form.date,
            today = state.today,
            marked = entries.mapTo(HashSet()) { it.date },
            onPick = {
                edit { copy(date = it) }
                calendar = false
            },
            onDismiss = { calendar = false },
        )
    }
}

/** «Сегодня, 16 ноября» / «Вчера, 15 ноября» / «9 ноября». */
@Composable
internal fun dayLabelFull(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.health_today_date, dayMonth(date))
    today.minusDays(1) -> stringResource(R.string.health_yesterday_date, dayMonth(date))
    else -> if (date.year == today.year) dayMonth(date) else dayMonthYear(date)
}

private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Календарь выбора даты записи (#257, макет 2.3): месяц со стрелками, точка — день с записью, будущие дни недоступны.
 */
@Composable
fun DateCalendarDialog(selected: LocalDate, today: LocalDate, marked: Set<LocalDate>, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var month by rememberSaveable { mutableStateOf(selected.withDayOfMonth(1).toEpochDay()) }
    var chosen by rememberSaveable { mutableStateOf(selected.toEpochDay()) }
    val first = LocalDate.ofEpochDay(month)
    val canNext = first.plusMonths(1).isBefore(today.plusDays(1))
    FinanceWindow(
        title = stringResource(R.string.health_calendar_title),
        tag = TAG_CALENDAR,
        onDismiss = onDismiss,
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.health_calendar_pick), TAG_CALENDAR, onDismiss, { onPick(LocalDate.ofEpochDay(chosen)) }) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs)) {
            Text(monthTitle(first), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f).testTag(TAG_CALENDAR_MONTH))
            Arrow("‹", stringResource(R.string.health_calendar_prev), TAG_CALENDAR_PREV, true) { month = first.minusMonths(1).toEpochDay() }
            Arrow("›", stringResource(R.string.health_calendar_next), TAG_CALENDAR_NEXT, canNext) { month = first.plusMonths(1).toEpochDay() }
        }
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.inactive, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        val offset = first.dayOfWeek.value - 1
        val days = first.lengthOfMonth()
        val rows = (offset + days + WEEK - 1) / WEEK
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(WEEK) { col ->
                    val day = row * WEEK + col - offset + 1
                    if (day in 1..days) {
                        val date = first.withDayOfMonth(day)
                        DayCell(date, enabled = !date.isAfter(today), picked = date.toEpochDay() == chosen, dot = date in marked, modifier = Modifier.weight(1f)) { chosen = date.toEpochDay() }
                    } else {
                        Box(Modifier.weight(1f).heightIn(min = AppTheme.sizes.healthCalCell))
                    }
                }
            }
        }
        WindowText(stringResource(R.string.health_calendar_hint))
    }
}

private const val WEEK = 7

@Composable
private fun Arrow(glyph: String, description: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.sizes.financeClose)
            .clip(AppTheme.shapes.large)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) { Text(glyph, style = MaterialTheme.typography.titleLarge, color = if (enabled) colors.textPrimary else colors.inactive) }
}

@Composable
private fun DayCell(date: LocalDate, enabled: Boolean, picked: Boolean, dot: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(modifier.heightIn(min = AppTheme.sizes.healthCalCell), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppTheme.sizes.healthCalCell)
                .clip(AppTheme.shapes.large)
                .background(if (picked) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .testTag("$TAG_CALENDAR_DAY:$date"),
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (picked) FontWeight.Bold else FontWeight.Normal,
                color = if (picked) colors.onAccent else if (enabled) colors.textPrimary else colors.inactive,
            )
            if (dot) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AppTheme.spacing.xxs)
                        .size(AppTheme.sizes.healthPoint)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (picked) colors.onAccent else if (enabled) colors.textPrimary else colors.inactive),
                )
            }
        }
    }
}

/** Окно цели и роста (#257, макет 3.1): барабаны, ИМТ пересчитывается сразу; цель и рост задаются и убираются отдельно. */
@Composable
fun GoalDialog(state: HealthState, form: GoalForm, onIntent: (HealthIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val edit: (GoalForm.() -> GoalForm) -> Unit = { onIntent(HealthIntent.EditGoal(it)) }
    val close = { onIntent(HealthIntent.CloseGoal) }
    val latest = state.entries?.lastOrNull()?.tenths
    FinanceWindow(
        title = stringResource(R.string.health_goal_title),
        tag = TAG_GOAL_WINDOW,
        onDismiss = close,
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.health_save), TAG_GOAL_WINDOW, close, { onIntent(HealthIntent.SaveGoal) }) },
    ) {
        SectionLabel(stringResource(R.string.health_goal_label))
        if (form.goalTenths == null) {
            AddRow(stringResource(R.string.health_goal_add), TAG_GOAL_ADD) { edit { copy(goalTenths = latest ?: Weight.DEFAULT_TENTHS) } }
        } else {
            WeightDrums(form.goalTenths, { edit { copy(goalTenths = it) } }, TAG_GOAL_DRUM)
            RemoveRow(stringResource(R.string.health_goal_remove), TAG_GOAL_REMOVE) { edit { copy(goalTenths = null) } }
        }
        SectionLabel(stringResource(R.string.health_height_label))
        if (form.heightCm == null) {
            AddRow(stringResource(R.string.health_height_add), TAG_HEIGHT_ADD) { edit { copy(heightCm = Weight.DEFAULT_HEIGHT) } }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Drum(Weight.MIN_HEIGHT, Weight.MAX_HEIGHT, form.heightCm, { edit { copy(heightCm = it) } }, TAG_HEIGHT_DRUM, stringResource(R.string.health_height_label), AppTheme.sizes.healthDrumWhole)
                Text(stringResource(R.string.health_cm), style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, modifier = Modifier.padding(start = spacing.sm))
            }
            RemoveRow(stringResource(R.string.health_height_remove), TAG_HEIGHT_REMOVE) { edit { copy(heightCm = null) } }
        }
        val now = latest?.let { com.prostovasei.simpletasks.domain.model.health.WeightMath.bmi(it, form.heightCm) }
        val atGoal = form.goalTenths?.let { com.prostovasei.simpletasks.domain.model.health.WeightMath.bmi(it, form.heightCm) }
        if (now != null) {
            val text = buildString {
                append(stringResource(R.string.health_bmi_now, now.text, bmiName(now.kind)))
                if (atGoal != null) append(" ").append(stringResource(R.string.health_bmi_at_goal, Weight.format(form.goalTenths!!), atGoal.text))
            }
            WindowText(text, Modifier.testTag(TAG_GOAL_BMI))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = AppTheme.spacing.sm))
}

@Composable
private fun AddRow(text: String, tag: String, onClick: () -> Unit) {
    com.prostovasei.simpletasks.ui.feature.finance.SecondaryAction(text, tag, onClick, plus = true)
}

@Composable
private fun RemoveRow(text: String, tag: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AppTheme.colors.danger,
        modifier = Modifier.clip(AppTheme.shapes.medium).clickable(role = Role.Button, onClick = onClick).padding(vertical = AppTheme.spacing.xs).testTag(tag),
    )
}

const val TAG_WEIGHT_WINDOW = "weight_window"
const val TAG_WEIGHT_DRUM = "weight_drum"
const val TAG_WEIGHT_DATE = "weight_date"
const val TAG_WEIGHT_COMMENT = "weight_comment"
const val TAG_WEIGHT_PREFILL = "weight_prefill"
const val TAG_WEIGHT_CLASH = "weight_clash"
const val TAG_WEIGHT_DELETE = "weight_delete"
const val TAG_CALENDAR = "weight_calendar"
const val TAG_CALENDAR_MONTH = "weight_calendar_month"
const val TAG_CALENDAR_PREV = "weight_calendar_prev"
const val TAG_CALENDAR_NEXT = "weight_calendar_next"
const val TAG_CALENDAR_DAY = "weight_calendar_day"
const val TAG_GOAL_WINDOW = "goal_window"
const val TAG_GOAL_DRUM = "goal_drum"
const val TAG_GOAL_ADD = "goal_add"
const val TAG_GOAL_REMOVE = "goal_remove"
const val TAG_GOAL_BMI = "goal_bmi"
const val TAG_HEIGHT_DRUM = "height_drum"
const val TAG_HEIGHT_ADD = "height_add"
const val TAG_HEIGHT_REMOVE = "height_remove"
