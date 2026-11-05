package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.reminderText
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Окно уведомлений (итерация 31, ТЗ PM). Можно отметить несколько. Блоки «Основные» (готовые варианты) и «Созданные
 * вручную»: общая история ([ReminderHistory]), назначенные задаче свои варианты (даже выпавшие из истории) и только что
 * созданные — сверху. Справа — когда сработает. Снятие галочки вариант не удаляет. «Вручную…» добавляет отмеченный
 * вариант; в историю он попадает только по общему «Готово». Без даты — подсказка «Сначала выберите день».
 */
@Composable
fun RemindersDialog(due: TaskDue?, start: TaskDue?, zone: ZoneId, reminders: List<String>, onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(R.string.reminders_title)
    if (due == null) {
        SheetDialog(title, TAG_REMINDERS_DIALOG, onDismiss, onDone = null, dismissLabel = R.string.dialog_understood) {
            DialogText(stringResource(R.string.quick_reminder_need_day), TAG_REMINDER_NEED_DAY)
        }
        return
    }
    val context = LocalContext.current
    val allDay = due is TaskDue.AllDay
    val history = remember(allDay) { ReminderHistory.load(context, allDay) }
    var draft by remember { mutableStateOf(reminders) }
    // Созданные в этом окне через «Вручную…» — по порядку создания.
    var created by remember { mutableStateOf(emptyList<String>()) }
    var custom by remember { mutableStateOf(false) }
    val presets = Reminders.presets(due)
    val isPreset = { r: String -> presets.any { Reminders.sameMoment(it, r) } }
    val manual = (created.reversed() + history + reminders.filterNot(isPreset))
        .fold(emptyList<String>()) { list, r -> if (list.any { Reminders.sameMoment(it, r) }) list else list + r }
    val toggle = { option: String ->
        draft = if (draft.any { Reminders.sameMoment(it, option) }) draft.filterNot { Reminders.sameMoment(it, option) } else draft + option
    }

    SheetDialog(
        title = title,
        tag = TAG_REMINDERS_DIALOG,
        onDismiss = onDismiss,
        onDone = {
            ReminderHistory.add(context, allDay, created)
            onConfirm(draft)
        },
    ) {
        @Composable
        fun Option(option: String) {
            // Вариант показывается в записи задачи, если она отличается («-PT60M» вместо «-PT1H»).
            val shown = draft.firstOrNull { Reminders.sameMoment(it, option) } ?: option
            DialogOption(
                title = reminderText(shown, allDay),
                selected = draft.any { Reminders.sameMoment(it, option) },
                tag = "$TAG_REMINDER_OPTION:$option",
                onClick = { toggle(shown) },
                hint = firesHint(due, start, option, zone),
                multi = true,
            )
        }
        DialogSection(stringResource(R.string.reminders_main))
        presets.forEach { Option(it) }
        DialogDivider()
        DialogSection(stringResource(R.string.reminders_manual))
        manual.forEach { Option(it) }
        TextButton(
            onClick = { custom = true },
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm).testTag(TAG_REMINDER_CUSTOM),
        ) {
            Icon(Icons.Default.Add, null, tint = AppTheme.colors.accent)
            Text(stringResource(R.string.reminder_custom), color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = AppTheme.spacing.xs))
        }
    }
    if (custom) {
        CustomReminderDialog(
            due = due,
            start = start,
            zone = zone,
            onConfirm = { trigger ->
                custom = false
                if (!isPreset(trigger)) created = created.filterNot { Reminders.sameMoment(it, trigger) } + trigger
                if (draft.none { Reminders.sameMoment(it, trigger) }) draft = draft + trigger
            },
            onDismiss = { custom = false },
        )
    }
}

/** Когда сработает: «17:30» в день срока, иначе «вт 18:00». */
private fun firesHint(due: TaskDue, start: TaskDue?, trigger: String, zone: ZoneId): String? {
    val at = Reminders.instant(due, start, trigger, zone)?.atZone(zone) ?: return null
    val day = when (due) {
        is TaskDue.AllDay -> due.date
        is TaskDue.AtTime -> due.instant.atZone(zone).toLocalDate()
    }
    return (if (at.toLocalDate() == day) HINT_TIME else HINT_DAY_TIME).format(at)
}

/**
 * «Вручную» (итерация 23, с 31 — в едином окне). У задачи со временем — за сколько дней, часов и минут до срока; у задачи
 * на весь день — за сколько дней и во сколько. Снизу — когда именно сработает. «Готово» возвращает вариант в окно уведомлений.
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
    SheetDialog(
        title = stringResource(R.string.reminder_custom_title),
        tag = TAG_CUSTOM_DIALOG,
        onDismiss = onDismiss,
        onDone = { onConfirm(trigger) },
        subtitle = stringResource(if (allDay) R.string.reminder_custom_subtitle_all_day else R.string.reminder_custom_subtitle),
    ) {
        Column(Modifier.padding(horizontal = AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
            Stepper(stringResource(if (allDay) R.string.reminder_custom_days_before else R.string.reminder_custom_days), days, 0, CUSTOM_MAX_DAYS, 1, TAG_CUSTOM_DAYS) { days = it }
            Stepper(stringResource(if (allDay) R.string.reminder_custom_hour else R.string.reminder_custom_hours), hours, 0, HOURS_PER_DAY - 1, 1, TAG_CUSTOM_HOURS) { hours = it }
            Stepper(stringResource(if (allDay) R.string.reminder_custom_minute else R.string.reminder_custom_minutes), minutes, 0, MINUTES_PER_HOUR - CUSTOM_MINUTE_STEP, CUSTOM_MINUTE_STEP, TAG_CUSTOM_MINUTES) { minutes = it }
            fires?.let {
                Text(
                    stringResource(R.string.reminder_custom_fires, it),
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = AppTheme.spacing.xs).testTag(TAG_CUSTOM_FIRES),
                )
            }
        }
    }
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
private val RU = Locale.forLanguageTag("ru")
private val FIRES_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM, HH:mm", RU)
private val HINT_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", RU)
private val HINT_DAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm", RU)

/** Окно списков (итерация 31): квадратик цвета со значком, выбранный — галочкой; применяется по «Готово». */
@Composable
fun ProjectDialog(projectId: String, projects: List<ProjectOption>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(projectId) }
    val inboxName = stringResource(R.string.inbox_list_name)
    SheetDialog(stringResource(R.string.quick_tool_list), TAG_PROJECT_DIALOG, onDismiss, onDone = { onConfirm(draft) }) {
        projects.forEach { option ->
            DialogOption(
                title = if (option.isInbox) inboxName else option.name,
                selected = option.id == draft,
                tag = "$TAG_PROJECT_OPTION:${option.id}",
                onClick = { draft = option.id },
                leading = {
                    ListSquare(
                        color = parseListColor(option.color) ?: AppTheme.colors.inactive,
                        icon = ListIcons.drawable(if (option.isInbox) ListIcons.INBOX else option.icon),
                    )
                },
            )
        }
    }
}

/** Окно приоритета (итерация 31): флажок цвета приоритета, выбранный — галочкой; применяется по «Готово». */
@Composable
fun PriorityDialog(priority: Priority, onConfirm: (Priority) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(priority) }
    // Цвета — как у кружков задач в ленте.
    val rings = AppTheme.colors.checkRings
    SheetDialog(stringResource(R.string.quick_tool_priority), TAG_PRIORITY_DIALOG, onDismiss, onDone = { onConfirm(draft) }) {
        listOf(
            Triple(R.string.priority_p1, Priority.HIGH, rings.high),
            Triple(R.string.priority_p2, Priority.MEDIUM, rings.medium),
            Triple(R.string.priority_p3, Priority.LOW, rings.low),
            Triple(R.string.priority_p0, Priority.NONE, rings.none),
        ).forEach { (label, value, color) ->
            DialogOption(
                title = stringResource(label),
                selected = value == draft,
                tag = "$TAG_PRIORITY_OPTION:${value.name}",
                onClick = { draft = value },
                leading = { Icon(painterResource(R.drawable.ic_tool_priority), null, tint = color, modifier = Modifier.size(AppTheme.sizes.icon)) },
            )
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
internal fun dayLabelText(day: DayLabel): String = when (day) {
    DayLabel.Today -> stringResource(R.string.day_today)
    DayLabel.Tomorrow -> stringResource(R.string.day_tomorrow)
    DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
    is DayLabel.Date -> DateTimeFormatter.ofPattern(if (day.showYear) "d MMM yyyy" else "d MMM", Locale.forLanguageTag("ru")).format(day.date)
}

const val TAG_REMINDERS_DIALOG = "reminders_dialog"
const val TAG_REMINDER_OPTION = "reminder_option"
const val TAG_REMINDER_CUSTOM = "reminder_custom"
const val TAG_CUSTOM_DIALOG = "custom_reminder"
const val TAG_CUSTOM_DAYS = "custom_days"
const val TAG_CUSTOM_HOURS = "custom_hours"
const val TAG_CUSTOM_MINUTES = "custom_minutes"
const val TAG_CUSTOM_FIRES = "custom_fires"
const val TAG_CUSTOM_ADD = "$TAG_CUSTOM_DIALOG:$TAG_DIALOG_DONE"
const val TAG_PROJECT_DIALOG = "project_dialog"
const val TAG_PROJECT_OPTION = "project_option"
const val TAG_PRIORITY_DIALOG = "priority_dialog"
const val TAG_PRIORITY_OPTION = "priority_option"
