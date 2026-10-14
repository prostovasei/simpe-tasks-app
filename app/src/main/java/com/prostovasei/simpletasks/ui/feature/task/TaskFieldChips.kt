package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.designsystem.components.ColorDot
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.allDayReminder
import com.prostovasei.simpletasks.domain.model.parseReminder
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.reminderText
import com.prostovasei.simpletasks.ui.common.repeatSummary
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ряд кнопок свойств задачи (SPEC.md §4.4): Дата, Время, Напоминание, Список, Приоритет, Повтор.
 * Выбор приходит в [onEdit] готовым значением; меню и диалоги — состояние самого экрана.
 */
@Composable
fun TaskFieldChips(
    due: TaskDue?,
    projectId: String,
    priority: Priority,
    rrule: String?,
    reminders: List<String>,
    projects: List<ProjectOption>,
    today: LocalDate,
    zone: ZoneId,
    onEdit: (FieldEdit) -> Unit,
    modifier: Modifier = Modifier,
    extraChips: @Composable () -> Unit = {},
) {
    var dialog by remember { mutableStateOf(ChipDialog.None) }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        DateChip(due, today, zone, onEdit) { dialog = ChipDialog.Date }
        TimeChip(due, today, zone, onEdit) { dialog = ChipDialog.Time }
        if (due != null) ReminderChip(due, reminders, onEdit)
        ProjectChip(projectId, projects, onEdit)
        PriorityChip(priority, onEdit)
        AppChip(
            label = repeatSummary(rrule) ?: stringResource(R.string.chip_repeat),
            selected = rrule != null,
            tag = TAG_CHIP_REPEAT,
            onClick = { dialog = ChipDialog.Repeat },
        )
        extraChips()
    }
    when (dialog) {
        ChipDialog.None -> Unit
        ChipDialog.Date -> DatePickDialog(
            initial = due?.localDate(zone) ?: today,
            onPick = { onEdit(FieldEdit.Due(withDate(due, it, zone))) },
            onDismiss = { dialog = ChipDialog.None },
        )
        ChipDialog.Time -> TimePickDialog(
            initial = due?.localTime(zone) ?: LocalTime.of(DEFAULT_HOUR, 0),
            onPick = { onEdit(FieldEdit.Due(withTime(due, it, zone, today))) },
            onDismiss = { dialog = ChipDialog.None },
        )
        ChipDialog.Repeat -> RepeatDialog(
            rrule = rrule,
            due = due?.localDate(zone),
            onConfirm = { onEdit(FieldEdit.Repeat(it)); dialog = ChipDialog.None },
            onDismiss = { dialog = ChipDialog.None },
        )
    }
}

private enum class ChipDialog { None, Date, Time, Repeat }

@Composable
private fun DateChip(due: TaskDue?, today: LocalDate, zone: ZoneId, onEdit: (FieldEdit) -> Unit, onPickDate: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val label = DueFormatter.format(due, null, today, zone)?.day?.let { dayLabelText(it) } ?: stringResource(R.string.chip_date)
    Box {
        AppChip(label, selected = due != null, tag = TAG_CHIP_DATE) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            MenuItem(R.string.date_today) { menu = false; onEdit(FieldEdit.Due(withDate(due, today, zone))) }
            MenuItem(R.string.date_tomorrow) { menu = false; onEdit(FieldEdit.Due(withDate(due, today.plusDays(1), zone))) }
            MenuItem(R.string.date_pick) { menu = false; onPickDate() }
            if (due != null) MenuItem(R.string.date_none) { menu = false; onEdit(FieldEdit.Due(null)) }
        }
    }
}

@Composable
private fun TimeChip(due: TaskDue?, today: LocalDate, zone: ZoneId, onEdit: (FieldEdit) -> Unit, onPickTime: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val time = due?.localTime(zone)
    val label = when {
        time != null -> TIME.format(time)
        due != null -> stringResource(R.string.time_all_day)
        else -> stringResource(R.string.chip_time)
    }
    Box {
        AppChip(label, selected = time != null, tag = TAG_CHIP_TIME) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            MenuItem(R.string.time_all_day) { menu = false; onEdit(FieldEdit.Due(withTime(due, null, zone, today))) }
            MenuItem(R.string.time_pick) { menu = false; onPickTime() }
        }
    }
}

/** Напоминания: готовые варианты и уже заданные (например, из TickTick) — с галочками; нажатие включает или выключает. */
@Composable
private fun ReminderChip(due: TaskDue, reminders: List<String>, onEdit: (FieldEdit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val allDay = due is TaskDue.AllDay
    val first = reminders.firstOrNull()?.let { reminderChipText(it, allDay) }
    val label = when {
        first == null -> stringResource(R.string.reminder_none)
        reminders.size == 1 -> first
        else -> stringResource(R.string.reminder_more, first, reminders.size - 1)
    }
    val options = reminders + Reminders.presets(due).filter { preset -> reminders.none { Reminders.sameMoment(it, preset) } }
    Box {
        AppChip(label, selected = reminders.isNotEmpty(), tag = TAG_CHIP_REMINDER, icon = Icons.Default.Notifications) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            options.forEach { option ->
                val checked = option in reminders
                DropdownMenuItem(
                    text = { Text(reminderText(option, allDay), color = AppTheme.colors.textPrimary) },
                    leadingIcon = { if (checked) Icon(Icons.Default.Check, null, tint = AppTheme.colors.accent) else Spacer(Modifier.size(AppTheme.sizes.icon)) },
                    onClick = {
                        menu = false
                        onEdit(FieldEdit.SetReminders(if (checked) reminders - option else reminders + option))
                    },
                    modifier = Modifier.testTag("$TAG_REMINDER_OPTION:$option"),
                )
            }
            if (reminders.isNotEmpty()) MenuItem(R.string.reminder_none) { menu = false; onEdit(FieldEdit.SetReminders(emptyList())) }
        }
    }
}

/** Короткая подпись для кнопки: «10:00» (в день срока), «В срок», иначе полный текст. */
@Composable
private fun reminderChipText(trigger: String, allDay: Boolean): String {
    val offset = parseReminder(trigger) ?: return trigger
    if (allDay) {
        val (daysBefore, minuteOfDay) = allDayReminder(offset)
        if (daysBefore == 0) return TIME.format(LocalTime.ofSecondOfDay(minuteOfDay * 60L))
    } else if (offset.isZero) {
        return stringResource(R.string.reminder_at_due_short)
    }
    return reminderText(trigger, allDay)
}

@Composable
private fun ProjectChip(projectId: String, projects: List<ProjectOption>, onEdit: (FieldEdit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val inboxName = stringResource(R.string.inbox_list_name)
    fun ProjectOption.title() = if (isInbox) inboxName else name
    val current = projects.firstOrNull { it.id == projectId }
    Box {
        AppChip(current?.title() ?: inboxName, selected = current?.isInbox == false, tag = TAG_CHIP_LIST) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            projects.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title(), color = AppTheme.colors.textPrimary) },
                    leadingIcon = parseListColor(option.color)?.let { color -> { ColorDot(color) } },
                    onClick = { menu = false; onEdit(FieldEdit.Project(option.id)) },
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(priority: Priority, onEdit: (FieldEdit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val label = when (priority) {
        Priority.HIGH -> "P1"
        Priority.MEDIUM -> "P2"
        Priority.LOW -> "P3"
        Priority.NONE -> stringResource(R.string.chip_priority)
    }
    Box {
        AppChip(label, selected = priority != Priority.NONE, tag = TAG_CHIP_PRIORITY) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            listOf(
                R.string.priority_p1 to Priority.HIGH,
                R.string.priority_p2 to Priority.MEDIUM,
                R.string.priority_p3 to Priority.LOW,
                R.string.priority_p0 to Priority.NONE,
            ).forEach { (label, value) -> MenuItem(label) { menu = false; onEdit(FieldEdit.SetPriority(value)) } }
        }
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
private fun dayLabelText(day: DayLabel): String = when (day) {
    DayLabel.Today -> stringResource(R.string.day_today)
    DayLabel.Tomorrow -> stringResource(R.string.day_tomorrow)
    DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
    is DayLabel.Date -> DateTimeFormatter.ofPattern(if (day.showYear) "d MMM yyyy" else "d MMM", Locale.forLanguageTag("ru")).format(day.date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // DatePicker работает с полуночью даты по UTC — тот же формат, что у задач «на весь день».
    val state = rememberDatePickerState(initialSelectedDateMillis = DueDateCodec.allDayKey(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(Math.floorDiv(it, DueDateCodec.MILLIS_PER_DAY))) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); onDismiss() }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state = state) },
        containerColor = AppTheme.colors.surface,
    )
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val DEFAULT_HOUR = 9

const val TAG_CHIP_DATE = "chip_date"
const val TAG_CHIP_TIME = "chip_time"
const val TAG_CHIP_LIST = "chip_list"
const val TAG_CHIP_PRIORITY = "chip_priority"
const val TAG_CHIP_REPEAT = "chip_repeat"
const val TAG_CHIP_REMINDER = "chip_reminder"
const val TAG_REMINDER_OPTION = "reminder_option"
