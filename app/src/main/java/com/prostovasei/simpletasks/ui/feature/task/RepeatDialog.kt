package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.RepeatFrequency
import com.prostovasei.simpletasks.domain.model.RepeatRule
import com.prostovasei.simpletasks.ui.common.repeatSummary
import com.prostovasei.simpletasks.ui.common.shortDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Простой конструктор повтора (решение PM): частота, «каждые N», дни недели.
 * Сложное правило из TickTick показывается текстом и не меняется, пока не задан новый повтор.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepeatDialog(rrule: String?, due: LocalDate?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    val existing = RepeatRule.parse(rrule)
    val editable = existing?.takeIf { it.isSimple(due) }
    var frequency by remember { mutableStateOf(editable?.frequency ?: RepeatFrequency.DAILY) }
    var interval by remember { mutableIntStateOf(editable?.interval ?: 1) }
    var days by remember { mutableStateOf(editable?.weekDays ?: emptySet()) }
    // Правило конструктора не содержит числа месяца: повтор идёт от даты задачи.
    val built = RepeatRule(frequency, interval, if (frequency == RepeatFrequency.WEEKLY) days else emptySet()).toRRule()
    val colors = AppTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                if (due == null) {
                    Text(stringResource(R.string.repeat_needs_date), color = colors.textSecondary)
                    return@Column
                }
                if (existing != null && editable == null) {
                    Text(repeatSummary(rrule).orEmpty(), color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.repeat_complex_hint), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    listOf(
                        RepeatFrequency.DAILY to R.string.repeat_day,
                        RepeatFrequency.WEEKLY to R.string.repeat_week,
                        RepeatFrequency.MONTHLY to R.string.repeat_month,
                        RepeatFrequency.YEARLY to R.string.repeat_year,
                    ).forEach { (value, label) ->
                        AppChip(stringResource(label), selected = frequency == value, tag = "repeat_freq:${value.code}") { frequency = value }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    Text(stringResource(R.string.repeat_every), color = colors.textPrimary)
                    OutlinedButton(onClick = { if (interval > 1) interval-- }, modifier = Modifier.testTag(TAG_REPEAT_MINUS)) { Text("−") }
                    Text(interval.toString(), color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.testTag(TAG_REPEAT_INTERVAL))
                    OutlinedButton(onClick = { if (interval < MAX_INTERVAL) interval++ }, modifier = Modifier.testTag(TAG_REPEAT_PLUS)) { Text("+") }
                }
                if (frequency == RepeatFrequency.WEEKLY) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
                        DayOfWeek.entries.forEach { day ->
                            AppChip(shortDay(day), selected = day in days, tag = "repeat_day:${day.name}") {
                                days = if (day in days) days - day else days + day
                            }
                        }
                    }
                }
                Text(repeatSummary(built).orEmpty(), color = colors.accent, modifier = Modifier.testTag(TAG_REPEAT_PREVIEW))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(built) }, enabled = due != null) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            Row {
                if (rrule != null) TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.repeat_none), color = colors.danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

private const val MAX_INTERVAL = 99
const val TAG_REPEAT_MINUS = "repeat_minus"
const val TAG_REPEAT_PLUS = "repeat_plus"
const val TAG_REPEAT_INTERVAL = "repeat_interval"
const val TAG_REPEAT_PREVIEW = "repeat_preview"
