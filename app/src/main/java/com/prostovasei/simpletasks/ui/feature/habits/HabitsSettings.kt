package com.prostovasei.simpletasks.ui.feature.habits

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.habits.HabitBackupInfo
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.ui.feature.finance.FinanceField
import com.prostovasei.simpletasks.ui.feature.finance.FinanceWindow
import com.prostovasei.simpletasks.ui.feature.finance.ServiceHeader
import com.prostovasei.simpletasks.ui.feature.finance.TwoActions
import com.prostovasei.simpletasks.ui.feature.finance.WindowText
import com.prostovasei.simpletasks.ui.feature.finance.lastBackupText
import com.prostovasei.simpletasks.ui.feature.health.MINUTES_IN_HOUR
import com.prostovasei.simpletasks.ui.feature.health.clock

/** Страница «Привычки» единых настроек (#340, ТЗ §8–9): напоминание, архив, резервная копия. */
@Composable
fun HabitsSettingsRoute(viewModel: HabitsViewModel, onBack: () -> Unit, onBackup: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HabitsEffects(viewModel)
    val context = LocalContext.current
    HabitsSettingsScreen(
        state = state,
        onBack = onBack,
        onBackup = onBackup,
        onIntent = viewModel::onIntent,
        onPickTime = {
            TimePickerDialog(
                context,
                { _, hour, minute -> viewModel.onIntent(HabitsIntent.SetReminderMinute(hour * MINUTES_IN_HOUR + minute)) },
                state.settings.reminderMinute / MINUTES_IN_HOUR,
                state.settings.reminderMinute % MINUTES_IN_HOUR,
                true,
            ).show()
        },
    )
}

@Composable
fun HabitsSettingsScreen(state: HabitsState, onBack: () -> Unit, onBackup: () -> Unit, onIntent: (HabitsIntent) -> Unit, onPickTime: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val settings = state.settings
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HABITS_SETTINGS)) {
        ServiceHeader(stringResource(R.string.module_habits), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Section(stringResource(R.string.habits_reminder_section))
            Group {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).padding(horizontal = spacing.md, vertical = spacing.sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.habits_reminder), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(R.string.habits_reminder_hint), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Switch(checked = settings.reminderOn, onCheckedChange = { onIntent(HabitsIntent.SetReminder(it)) }, modifier = Modifier.testTag(TAG_HABITS_REMINDER_SWITCH))
                }
                if (settings.reminderOn) {
                    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).clickable(role = Role.Button, onClick = onPickTime)
                            .padding(horizontal = spacing.md, vertical = spacing.sm).testTag(TAG_HABITS_REMINDER_TIME),
                    ) {
                        Text(stringResource(R.string.health_reminder_time), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(clock(settings.reminderMinute), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
            }
            Section(stringResource(R.string.habits_archive_section))
            Group {
                val archived = state.archived
                if (archived.isEmpty()) {
                    Text(stringResource(R.string.habits_archive_empty), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
                        modifier = Modifier.padding(spacing.md).testTag(TAG_HABITS_ARCHIVE_EMPTY))
                }
                archived.forEachIndexed { index, habit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).clickable(role = Role.Button) { onIntent(HabitsIntent.Open(habit.id)) }
                            .padding(horizontal = spacing.md, vertical = spacing.sm).testTag("$TAG_HABITS_ARCHIVED:${habit.id}"),
                    ) {
                        Text(habit.emojiShown, fontSize = SizeTokens.HABIT_EMOJI_SP.sp)
                        Column(Modifier.weight(1f).padding(start = spacing.sm)) {
                            Text(habit.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(scheduleText(habit.schedule), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                    if (index < archived.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                }
            }
            Section(stringResource(R.string.health_data_section))
            Group {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).clickable(role = Role.Button, onClick = onBackup)
                        .padding(horizontal = spacing.md, vertical = spacing.sm).testTag(TAG_HABITS_BACKUP),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(R.string.habits_backup_subtitle, lastBackupText(state.lastBackupAt), state.habits?.size ?: 0), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
    // Карточка архивной привычки — та же, что в разделе: «Вернуть» и «Удалить».
    state.open?.let { HabitCard(it, state, onIntent) }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.spacing.md, bottom = AppTheme.spacing.xs))
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(AppTheme.colors.surface)) { content() }
}

/** Страница «Резервная копия» «Привычек» (#340): сохранить в файл и восстановить из файла. */
@Composable
fun HabitsBackupRoute(viewModel: HabitsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HabitsEffects(viewModel)
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri -> uri?.let { viewModel.onIntent(HabitsIntent.ExportBackup(it.toString())) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.onIntent(HabitsIntent.PickBackup(it.toString())) } }
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HABITS_BACKUP_SCREEN)) {
        ServiceHeader(stringResource(R.string.backup_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Group {
                BackupRow(stringResource(R.string.health_backup_save), stringResource(R.string.habits_backup_save_hint), TAG_HABITS_BACKUP_SAVE) {
                    saver.launch("simple-tasks-habits-${state.today}.json")
                }
                HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                BackupRow(stringResource(R.string.health_backup_restore), stringResource(R.string.habits_backup_restore_hint), TAG_HABITS_BACKUP_RESTORE) {
                    picker.launch(arrayOf(BACKUP_MIME, "*/*"))
                }
            }
            Text(lastBackupText(state.lastBackupAt), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(spacing.md))
        }
    }
    state.restore?.let { RestoreDialog(it.info, state, viewModel::onIntent) }
}

@Composable
private fun BackupRow(title: String, hint: String, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).clickable(role = Role.Button, onClick = onClick).padding(horizontal = spacing.md, vertical = spacing.sm).testTag(tag)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Text(hint, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

/** Подтверждение восстановления: «в копии / сейчас»; после «Заменить» привычки заменяются целиком. */
@Composable
private fun RestoreDialog(info: HabitBackupInfo, state: HabitsState, onIntent: (HabitsIntent) -> Unit) {
    val cancel = { onIntent(HabitsIntent.CloseRestore) }
    FinanceWindow(
        title = stringResource(R.string.habits_restore_title),
        tag = TAG_HABITS_RESTORE,
        onDismiss = cancel,
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.backup_replace), TAG_HABITS_RESTORE, cancel, { onIntent(HabitsIntent.ConfirmRestore) }) },
    ) {
        CompareRow("", stringResource(R.string.backup_in_file), stringResource(R.string.backup_now), header = true)
        CompareRow(stringResource(R.string.module_habits), info.habits.toString(), (state.habits?.size ?: 0).toString())
        CompareRow(stringResource(R.string.habits_restore_checks), info.checks.toString(), state.habits.orEmpty().sumOf { it.checks.size }.toString())
        CompareRow(stringResource(R.string.health_restore_reminder), reminder(info.settings), reminder(state.settings))
        WindowText(stringResource(R.string.habits_restore_warning))
    }
}

@Composable
private fun CompareRow(label: String, inFile: String, now: String, header: Boolean = false) {
    val colors = AppTheme.colors
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    FinanceField(label) {
        Row {
            Text(inFile, style = style, fontWeight = if (header) FontWeight.Bold else FontWeight.SemiBold, color = if (header) colors.textSecondary else colors.textPrimary)
            androidx.compose.foundation.layout.Spacer(Modifier.width(AppTheme.spacing.md))
            Text(now, style = style, color = if (header) colors.textSecondary else colors.textPrimary)
        }
    }
}

@Composable
private fun reminder(s: HabitSettings): String = if (s.reminderOn) clock(s.reminderMinute) else stringResource(R.string.health_restore_off)

private const val BACKUP_MIME = "application/json"

const val TAG_HABITS_SETTINGS = "habits_settings"
const val TAG_HABITS_REMINDER_SWITCH = "habits_reminder_switch"
const val TAG_HABITS_REMINDER_TIME = "habits_reminder_time"
const val TAG_HABITS_ARCHIVE_EMPTY = "habits_archive_empty"
const val TAG_HABITS_ARCHIVED = "habits_archived"
const val TAG_HABITS_BACKUP = "habits_backup"
const val TAG_HABITS_BACKUP_SCREEN = "habits_backup_screen"
const val TAG_HABITS_BACKUP_SAVE = "habits_backup_save"
const val TAG_HABITS_BACKUP_RESTORE = "habits_backup_restore"
const val TAG_HABITS_RESTORE = "habits_restore"
