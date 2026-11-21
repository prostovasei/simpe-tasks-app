package com.prostovasei.simpletasks.ui.feature.health

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.model.health.WeightMath
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import com.prostovasei.simpletasks.ui.common.showUndo
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Раздел «Здоровье» → «Вес» (#257, макеты design/approved/257, решения PM 16.11.2026). Белый плоский экран, как у «Недели» (#247).
 * [snackbar] — общая подсказка приложения: после удаления записи на ней «Отмена».
 */
@Composable
fun HealthRoute(viewModel: HealthViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthEffects(viewModel, snackbar)
    TaskCanvasTheme { HealthScreen(state, viewModel::onIntent) }
}

/** Подсказки «Здоровья»: «Запись удалена · Отмена» и результаты резервной копии — на главном экране и на страницах настроек. */
@Composable
fun HealthEffects(viewModel: HealthViewModel, snackbar: SnackbarHostState? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val undo = stringResource(R.string.task_undo)
    val deleted = stringResource(R.string.health_deleted)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HealthEffect.EntrySaved -> Unit
                is HealthEffect.EntryDeleted -> snackbar?.let { host ->
                    scope.launch { host.showUndo(deleted.format(dayMonth(effect.entry.date)), undo, UndoKind.Deleted) { viewModel.onIntent(HealthIntent.UndoDelete(effect.entry)) } }
                }
                HealthEffect.BackupSaved -> Toast.makeText(context, R.string.health_backup_saved, Toast.LENGTH_SHORT).show()
                HealthEffect.BackupFailed -> Toast.makeText(context, R.string.health_backup_failed, Toast.LENGTH_LONG).show()
                HealthEffect.BackupInvalid -> Toast.makeText(context, R.string.health_backup_invalid, Toast.LENGTH_LONG).show()
                HealthEffect.BackupRestored -> Toast.makeText(context, R.string.health_backup_restored, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun HealthScreen(state: HealthState, onIntent: (HealthIntent) -> Unit) {
    val colors = AppTheme.colors
    val entries = state.entries
    Box(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HEALTH)) {
        when {
            entries == null -> Unit
            entries.isEmpty() -> HealthEmpty { onIntent(HealthIntent.Add) }
            else -> WeightContent(state, entries, onIntent)
        }
    }
    state.form?.let { WeightDialog(state, it, onIntent) }
    state.goalForm?.let { GoalDialog(state, it, onIntent) }
}

@Composable
private fun HealthEmpty(onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl).testTag(TAG_HEALTH_EMPTY),
    ) {
        Icon(painterResource(R.drawable.ic_health_weight), contentDescription = null, tint = colors.inactive, modifier = Modifier.size(AppTheme.sizes.financeEmptyIcon))
        Text(stringResource(R.string.health_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(top = spacing.sm))
        Text(
            stringResource(R.string.health_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs, bottom = spacing.lg),
        )
        com.prostovasei.simpletasks.ui.feature.finance.PrimaryAction(stringResource(R.string.health_add), TAG_HEALTH_ADD_FIRST, onAdd, plus = true)
    }
}

@Composable
private fun WeightContent(state: HealthState, entries: List<WeightEntry>, onIntent: (HealthIntent) -> Unit) {
    val spacing = AppTheme.spacing
    val settings = state.settings
    val last = entries.last()
    val inPeriod = WeightMath.inPeriod(entries, state.period, state.today)
    val newestFirst = entries.asReversed()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = spacing.md).testTag(TAG_HEALTH_LIST), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = spacing.lg)) {
        item(key = "hero") { Hero(last, WeightMath.change(entries, state.period, state.today), state) }
        item(key = "goal") { GoalRow(state, entries) { onIntent(HealthIntent.OpenGoal) } }
        item(key = "periods") { PeriodSwitcher(state.period) { onIntent(HealthIntent.SelectPeriod(it)) } }
        item(key = "chart") {
            WeightChart(inPeriod, state.period, state.today, settings.goalTenths, state.tip, { onIntent(HealthIntent.TapPoint(it)) }, Modifier.padding(top = spacing.xs))
        }
        item(key = "records") {
            Text(stringResource(R.string.health_records), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xxs))
        }
        items(newestFirst, key = { it.date.toEpochDay() }) { entry ->
            val index = newestFirst.indexOf(entry)
            val previous = newestFirst.getOrNull(index + 1)
            HistoryRow(entry, previous, state.today, highlighted = state.tip == entry.date) { onIntent(HealthIntent.OpenEntry(entry.date)) }
        }
    }
}

/** Текущий вес крупно и изменение за период (макет 1.1). */
@Composable
private fun Hero(last: WeightEntry, change: Int?, state: HealthState) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().padding(top = spacing.sm, bottom = spacing.xs).testTag(TAG_HEALTH_HERO)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Weight.format(last.tenths), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.testTag(TAG_HEALTH_WEIGHT))
            Text(stringResource(R.string.health_kg), style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, modifier = Modifier.padding(start = spacing.xxs, bottom = spacing.xs))
        }
        val since = stringResource(R.string.health_last_entry, sinceLabel(last.date, state.today))
        val period = stringResource(periodChangeLabel(state.period))
        Text(
            buildAnnotatedString {
                if (change != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (change < 0) colors.income else colors.textPrimary)) {
                        append(stringResource(R.string.health_change_kg, Weight.formatDelta(change)))
                    }
                    append(" $period · ")
                }
                append(since)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.testTag(TAG_HEALTH_CHANGE),
        )
    }
}

private fun periodChangeLabel(period: WeightPeriod): Int = when (period) {
    WeightPeriod.MONTH -> R.string.health_change_month
    WeightPeriod.QUARTER -> R.string.health_change_quarter
    WeightPeriod.YEAR -> R.string.health_change_year
    WeightPeriod.ALL -> R.string.health_change_all
}

/**
 * Строка цели (макет 3.2): цель и остаток, прогноз даты, ИМТ. Нет ни цели, ни роста — «Задать цель и рост». Нажатие — окно цели.
 */
@Composable
private fun GoalRow(state: HealthState, entries: List<WeightEntry>, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val settings = state.settings
    val goal = settings.goalTenths
    val bmiText = WeightMath.bmi(entries.last().tenths, settings.heightCm)
    val (title, sub) = healthGoalText(androidx.compose.ui.platform.LocalContext.current, entries, settings, state.today) {
        dayMonthYearIfOther(it, state.today)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.sm)
            .testTag(TAG_HEALTH_GOAL),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (goal == null && bmiText == null) colors.textSecondary else colors.textPrimary, modifier = Modifier.testTag(TAG_HEALTH_GOAL_TITLE))
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.testTag(TAG_HEALTH_GOAL_SUB)) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inactive)
    }
    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
}

private fun dayMonthYearIfOther(date: LocalDate, today: LocalDate): String = if (date.year == today.year) dayMonth(date) else dayMonthYear(date)

/** «Месяц / 3 мес / Год / Всё» (макет 1.1): выбранный — на серой подложке и жирный. */
@Composable
private fun PeriodSwitcher(selected: WeightPeriod, onSelect: (WeightPeriod) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs), modifier = Modifier.fillMaxWidth().padding(top = spacing.sm)) {
        WeightPeriod.entries.forEach { period ->
            val active = period == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AppTheme.sizes.financeChip)
                    .clip(AppTheme.shapes.large)
                    .background(if (active) colors.selected else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(period) })
                    .testTag("$TAG_HEALTH_PERIOD:${period.name}"),
            ) {
                Text(
                    stringResource(periodLabel(period)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun periodLabel(period: WeightPeriod): Int = when (period) {
    WeightPeriod.MONTH -> R.string.health_period_month
    WeightPeriod.QUARTER -> R.string.health_period_quarter
    WeightPeriod.YEAR -> R.string.health_period_year
    WeightPeriod.ALL -> R.string.health_period_all
}

/** Запись истории (макет 1.1): дата и комментарий, разница с предыдущей записью (снижение — зелёным), вес. Нажатие — правка. */
@Composable
private fun HistoryRow(entry: WeightEntry, previous: WeightEntry?, today: LocalDate, highlighted: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val delta = previous?.let { entry.tenths - it.tenths }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) colors.selected else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = AppTheme.sizes.operationRow)
            .padding(vertical = spacing.xs)
            .testTag("$TAG_HEALTH_ROW:${entry.date}"),
    ) {
        Column(Modifier.weight(1f)) {
            Text(dayLabel(entry.date, today), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            entry.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1) }
        }
        delta?.let {
            Text(
                Weight.formatDelta(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (it < 0) colors.income else colors.textSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(AppTheme.sizes.healthDeltaWidth).padding(end = spacing.xs),
            )
        }
        Text(Weight.format(entry.tenths), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.End, modifier = Modifier.padding(start = spacing.xs))
    }
    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
}

const val TAG_HEALTH = "health"
const val TAG_HEALTH_EMPTY = "health_empty"
const val TAG_HEALTH_LIST = "health_list"
const val TAG_HEALTH_ADD_FIRST = "health_add_first"
const val TAG_HEALTH_HERO = "health_hero"
const val TAG_HEALTH_WEIGHT = "health_weight"
const val TAG_HEALTH_CHANGE = "health_change"
const val TAG_HEALTH_GOAL = "health_goal"
const val TAG_HEALTH_GOAL_TITLE = "health_goal_title"
const val TAG_HEALTH_GOAL_SUB = "health_goal_sub"
const val TAG_HEALTH_PERIOD = "health_period"
const val TAG_HEALTH_ROW = "health_row"
const val TAG_HEALTH_TAB = "health_tab"
const val TAG_HEALTH_ADD = "health_add"
