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
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.allDayReminder
import com.prostovasei.simpletasks.domain.model.parseReminder
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.reminderText
import com.prostovasei.simpletasks.ui.common.repeatSummary
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Пункты меню напоминаний: готовые варианты и уже заданные (с галочками); [onDone] закрывает меню. */
@Composable
fun ReminderMenuItems(due: TaskDue, reminders: List<String>, onEdit: (FieldEdit) -> Unit, onDone: () -> Unit) {
    val allDay = due is TaskDue.AllDay
    val options = reminders + Reminders.presets(due).filter { preset -> reminders.none { Reminders.sameMoment(it, preset) } }
    options.forEach { option ->
        val checked = option in reminders
        DropdownMenuItem(
            text = { Text(reminderText(option, allDay), color = AppTheme.colors.textPrimary) },
            leadingIcon = { if (checked) Icon(Icons.Default.Check, null, tint = AppTheme.colors.accent) else Spacer(Modifier.size(AppTheme.sizes.icon)) },
            onClick = {
                onDone()
                onEdit(FieldEdit.SetReminders(if (checked) reminders - option else reminders + option))
            },
            modifier = Modifier.testTag("$TAG_REMINDER_OPTION:$option"),
        )
    }
    if (reminders.isNotEmpty()) MenuItem(R.string.reminder_none) { onDone(); onEdit(FieldEdit.SetReminders(emptyList())) }
}

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
const val TAG_PROJECT_OPTION = "project_option"
const val TAG_PRIORITY_OPTION = "priority_option"
