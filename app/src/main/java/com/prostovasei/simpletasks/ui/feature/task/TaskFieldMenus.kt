package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.allDayReminder
import com.prostovasei.simpletasks.domain.model.parseReminder
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.ReminderIcon
import com.prostovasei.simpletasks.ui.common.reminderText
import com.prostovasei.simpletasks.ui.common.repeatSummary
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Пункты меню напоминаний (итерация 23): у каждого — колокольчик (с точкой — заранее, итерация 27), у заданных — галочка справа; заданные вручную —
 * сверху, дальше готовые варианты и «Вручную…» ([onCustom]). Отметка пункта меню не закрывает; [onDone] закрывает
 * его после «Вручную…» и «Без напоминания».
 */
@Composable
fun ReminderMenuItems(due: TaskDue, reminders: List<String>, onEdit: (FieldEdit) -> Unit, onDone: () -> Unit, onCustom: () -> Unit) {
    val colors = AppTheme.colors
    val allDay = due is TaskDue.AllDay
    val presets = Reminders.presets(due)
    val custom = reminders.filter { r -> presets.none { Reminders.sameMoment(it, r) } }
    val options = custom + presets.map { preset -> reminders.firstOrNull { Reminders.sameMoment(it, preset) } ?: preset }
    options.forEach { option ->
        val checked = option in reminders
        DropdownMenuItem(
            text = { Text(reminderText(option, allDay), color = colors.textPrimary) },
            leadingIcon = { MenuReminderIcon(if (Reminders.isEarly(option)) ReminderMark.EARLY else ReminderMark.AT_DUE) },
            trailingIcon = if (checked) { { Icon(Icons.Default.Check, null, tint = colors.accent) } } else null,
            // Меню не закрывается (0.23.2): можно отметить сразу несколько; закрывается нажатием вне меню.
            onClick = { onEdit(FieldEdit.SetReminders(if (checked) reminders - option else reminders + option)) },
            modifier = Modifier.testTag("$TAG_REMINDER_OPTION:$option"),
        )
    }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.reminder_custom), color = colors.textPrimary) },
        leadingIcon = { MenuReminderIcon(ReminderMark.EARLY) },
        onClick = { onDone(); onCustom() },
        modifier = Modifier.testTag(TAG_REMINDER_CUSTOM),
    )
    if (reminders.isNotEmpty()) MenuItem(R.string.reminder_none) { onDone(); onEdit(FieldEdit.SetReminders(emptyList())) }
}

@Composable
private fun MenuReminderIcon(mark: ReminderMark) {
    val colors = AppTheme.colors
    ReminderIcon(mark, AppTheme.sizes.icon, tint = colors.textSecondary, dotColor = colors.accent)
}

/**
 * Своё напоминание (итерация 23). У задачи со временем — за сколько дней, часов и минут до срока; у задачи
 * на весь день — за сколько дней и во сколько. Снизу — когда именно сработает.
 */
@Composable
fun CustomReminderDialog(due: TaskDue, start: TaskDue?, zone: ZoneId, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val allDay = due is TaskDue.AllDay
    var days by remember { mutableIntStateOf(if (allDay) 1 else 0) }
    var hours by remember { mutableIntStateOf(if (allDay) CUSTOM_ALL_DAY_HOUR else CUSTOM_TIMED_HOURS) }
    var minutes by remember { mutableIntStateOf(0) }
    val trigger = if (allDay) {
        Reminders.allDay(days, hours * MINUTES_PER_HOUR + minutes)
    } else {
        Reminders.timed((days * HOURS_PER_DAY + hours) * MINUTES_PER_HOUR.toLong() + minutes)
    }
    val fires = Reminders.instant(due, start, trigger, zone)?.atZone(zone)?.let { FIRES_FORMAT.format(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.reminder_custom_title), color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Stepper(stringResource(if (allDay) R.string.reminder_custom_days_before else R.string.reminder_custom_days), days, 0, CUSTOM_MAX_DAYS, 1, TAG_CUSTOM_DAYS) { days = it }
                Stepper(stringResource(if (allDay) R.string.reminder_custom_hour else R.string.reminder_custom_hours), hours, 0, HOURS_PER_DAY - 1, 1, TAG_CUSTOM_HOURS) { hours = it }
                Stepper(stringResource(if (allDay) R.string.reminder_custom_minute else R.string.reminder_custom_minutes), minutes, 0, MINUTES_PER_HOUR - CUSTOM_MINUTE_STEP, CUSTOM_MINUTE_STEP, TAG_CUSTOM_MINUTES) { minutes = it }
                fires?.let {
                    Text(
                        stringResource(R.string.reminder_custom_fires, it),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(TAG_CUSTOM_FIRES),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trigger) }, modifier = Modifier.testTag(TAG_CUSTOM_ADD)) {
                Text(stringResource(R.string.reminder_custom_add), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) }
        },
    )
}

/** Строка «Часов   −  2  +». */
@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, step: Int, tag: String, onChange: (Int) -> Unit) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min, modifier = Modifier.testTag("$tag:minus")) {
            Text(STEP_MINUS, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "%02d".format(value),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(tag),
        )
        TextButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max, modifier = Modifier.testTag("$tag:plus")) {
            Text(STEP_PLUS, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private const val CUSTOM_MAX_DAYS = 30
private const val CUSTOM_TIMED_HOURS = 2
private const val CUSTOM_ALL_DAY_HOUR = 9
private const val CUSTOM_MINUTE_STEP = 5
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private const val STEP_MINUS = "−"
private const val STEP_PLUS = "+"
private val FIRES_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM, HH:mm", Locale.forLanguageTag("ru"))

/** Пункты меню списков: квадратик цвета со значком, у выбранного — галочка. */
@Composable
fun ProjectMenuItems(projectId: String, projects: List<ProjectOption>, onEdit: (FieldEdit) -> Unit, onDone: () -> Unit) {
    val inboxName = stringResource(R.string.inbox_list_name)
    projects.forEach { option ->
        DropdownMenuItem(
            text = { Text(if (option.isInbox) inboxName else option.name, color = AppTheme.colors.textPrimary) },
            leadingIcon = {
                ListSquare(
                    color = parseListColor(option.color) ?: AppTheme.colors.inactive,
                    icon = ListIcons.drawable(if (option.isInbox) ListIcons.INBOX else option.icon),
                )
            },
            trailingIcon = if (option.id == projectId) { { Icon(Icons.Default.Check, null, tint = AppTheme.colors.accent) } } else null,
            onClick = { onDone(); onEdit(FieldEdit.Project(option.id)) },
            modifier = Modifier.testTag("$TAG_PROJECT_OPTION:${option.id}"),
        )
    }
}

/** Пункты меню приоритета; у выбранного — галочка. */
@Composable
fun PriorityMenuItems(priority: Priority, onEdit: (FieldEdit) -> Unit, onDone: () -> Unit) {
    listOf(
        R.string.priority_p1 to Priority.HIGH,
        R.string.priority_p2 to Priority.MEDIUM,
        R.string.priority_p3 to Priority.LOW,
        R.string.priority_p0 to Priority.NONE,
    ).forEach { (label, value) ->
        DropdownMenuItem(
            text = { Text(stringResource(label), color = AppTheme.colors.textPrimary) },
            trailingIcon = if (value == priority) { { Icon(Icons.Default.Check, null, tint = AppTheme.colors.accent) } } else null,
            onClick = { onDone(); onEdit(FieldEdit.SetPriority(value)) },
            modifier = Modifier.testTag("$TAG_PRIORITY_OPTION:${value.name}"),
        )
    }
}

/** Кнопка-«чип» в стиле дизайн-системы. */
@Composable
fun AppChip(label: String, selected: Boolean, tag: String, icon: ImageVector? = null, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
            iconColor = colors.textSecondary,
            selectedLeadingIconColor = colors.onAccent,
        ),
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun MenuItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label), color = AppTheme.colors.textPrimary) }, onClick = onClick)
}

@Composable
internal fun dayLabelText(day: DayLabel): String = when (day) {
    DayLabel.Today -> stringResource(R.string.day_today)
    DayLabel.Tomorrow -> stringResource(R.string.day_tomorrow)
    DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
    is DayLabel.Date -> DateTimeFormatter.ofPattern(if (day.showYear) "d MMM yyyy" else "d MMM", Locale.forLanguageTag("ru")).format(day.date)
}

const val TAG_REMINDER_OPTION = "reminder_option"
const val TAG_REMINDER_CUSTOM = "reminder_custom"
const val TAG_CUSTOM_DAYS = "custom_days"
const val TAG_CUSTOM_HOURS = "custom_hours"
const val TAG_CUSTOM_MINUTES = "custom_minutes"
const val TAG_CUSTOM_FIRES = "custom_fires"
const val TAG_CUSTOM_ADD = "custom_add"
const val TAG_PROJECT_OPTION = "project_option"
const val TAG_PRIORITY_OPTION = "priority_option"
