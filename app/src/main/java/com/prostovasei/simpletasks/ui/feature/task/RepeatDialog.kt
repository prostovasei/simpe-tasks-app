package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.RepeatFrequency
import com.prostovasei.simpletasks.domain.model.RepeatPreset
import com.prostovasei.simpletasks.domain.model.RepeatRule
import com.prostovasei.simpletasks.ui.common.repeatSummary
import com.prostovasei.simpletasks.ui.common.shortDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Окно повтора (итерация 31, образец PM): Нет / Ежедневно / Еженедельно / Ежемесячно / Ежегодно / Каждый рабочий день /
 * Произвольно…, подписи — по дате задачи. Выбор меняет черновик, задача меняется по «Готово». «Произвольно…» открывает
 * конструктор; его «Готово» возвращает правило сюда, «Отмена» — без изменений. Правило, которого нет среди готовых
 * (в том числе сложное из TickTick), отмечено как «Произвольно…» с описанием и не меняется, пока не выбран другой вариант.
 */
@Composable
fun RepeatDialog(rrule: String?, due: LocalDate?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(R.string.repeat_title)
    if (due == null) {
        SheetDialog(title, TAG_REPEAT_DIALOG, onDismiss, onDone = null, dismissLabel = R.string.dialog_understood) {
            DialogText(stringResource(R.string.repeat_needs_date))
        }
        return
    }
    var draft by remember { mutableStateOf(rrule) }
    var custom by remember { mutableStateOf(false) }
    val chosen = RepeatPreset.of(draft, due)
    SheetDialog(title, TAG_REPEAT_DIALOG, onDismiss, onDone = { onConfirm(draft) }) {
        @Composable
        fun Option(preset: RepeatPreset, label: Int, subtitle: String? = null) {
            DialogOption(stringResource(label), chosen == preset, "$TAG_REPEAT_OPTION:${preset.name}", { draft = preset.rule() }, subtitle)
        }
        Option(RepeatPreset.NONE, R.string.repeat_option_none)
        DialogDivider()
        Option(RepeatPreset.DAILY, R.string.repeat_option_daily)
        Option(RepeatPreset.WEEKLY, R.string.repeat_option_weekly, stringResource(R.string.repeat_option_weekly_on, stringArrayResource(R.array.repeat_weekdays_on)[due.dayOfWeek.ordinal]))
        Option(RepeatPreset.MONTHLY, R.string.repeat_option_monthly, stringResource(R.string.repeat_option_monthly_on, due.dayOfMonth))
        Option(RepeatPreset.YEARLY, R.string.repeat_option_yearly, YEARLY_FORMAT.format(due))
        DialogDivider()
        Option(RepeatPreset.WORKDAYS, R.string.repeat_option_workdays, stringResource(R.string.repeat_option_workdays_on))
        DialogDivider()
        DialogOption(
            title = stringResource(R.string.repeat_option_custom),
            selected = chosen == RepeatPreset.CUSTOM,
            tag = "$TAG_REPEAT_OPTION:${RepeatPreset.CUSTOM.name}",
            onClick = { custom = true },
            subtitle = if (chosen == RepeatPreset.CUSTOM) repeatSummary(draft) else stringResource(R.string.repeat_option_custom_hint),
        )
    }
    if (custom) {
        CustomRepeatDialog(
            rrule = draft,
            due = due,
            onConfirm = {
                draft = it
                custom = false
            },
            onDismiss = { custom = false },
        )
    }
}

/**
 * «Произвольно…» — простой конструктор повтора (решение PM): частота, «каждые N», дни недели.
 * Сложное правило из TickTick показывается текстом и не меняется, пока не задан новый повтор.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomRepeatDialog(rrule: String?, due: LocalDate?, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val existing = RepeatRule.parse(rrule)
    val editable = existing?.takeIf { it.isSimple(due) }
    var frequency by remember { mutableStateOf(editable?.frequency ?: RepeatFrequency.DAILY) }
    var interval by remember { mutableIntStateOf(editable?.interval ?: 1) }
    var days by remember { mutableStateOf(editable?.weekDays ?: emptySet()) }
    // Правило конструктора не содержит числа месяца: повтор идёт от даты задачи.
    val built = RepeatRule(frequency, interval, if (frequency == RepeatFrequency.WEEKLY) days else emptySet()).toRRule()
    val colors = AppTheme.colors

    SheetDialog(stringResource(R.string.repeat_custom_title), TAG_REPEAT_CUSTOM_DIALOG, onDismiss, onDone = { onConfirm(built) }) {
        Column(Modifier.padding(horizontal = AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
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
            Text(repeatSummary(built).orEmpty(), color = colors.accent, fontWeight = FontWeight.Bold, modifier = Modifier.testTag(TAG_REPEAT_PREVIEW))
        }
    }
}

private const val MAX_INTERVAL = 99
private val YEARLY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))
const val TAG_REPEAT_DIALOG = "repeat_dialog"
const val TAG_REPEAT_CUSTOM_DIALOG = "repeat_custom"
const val TAG_REPEAT_OPTION = "repeat_option"
const val TAG_REPEAT_MINUS = "repeat_minus"
const val TAG_REPEAT_PLUS = "repeat_plus"
const val TAG_REPEAT_INTERVAL = "repeat_interval"
const val TAG_REPEAT_PREVIEW = "repeat_preview"
