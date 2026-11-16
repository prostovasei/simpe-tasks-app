package com.prostovasei.simpletasks.ui.feature.health

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.health.HealthBackupInfo
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.ui.feature.finance.BACKUP_MIME
import com.prostovasei.simpletasks.ui.feature.finance.FinanceField
import com.prostovasei.simpletasks.ui.feature.finance.FinanceWindow
import com.prostovasei.simpletasks.ui.feature.finance.ServiceHeader
import com.prostovasei.simpletasks.ui.feature.finance.TwoActions
import com.prostovasei.simpletasks.ui.feature.finance.WindowText
import com.prostovasei.simpletasks.ui.feature.finance.lastBackupText
import java.time.LocalDate

/** Страница «Здоровье» единых настроек (#257, макет 4.2): напоминание взвеситься и резервная копия. */
@Composable
fun HealthSettingsRoute(viewModel: HealthViewModel, onBack: () -> Unit, onBackup: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthEffects(viewModel)
    val context = LocalContext.current
    HealthSettingsScreen(
        state = state,
        onBack = onBack,
        onBackup = onBackup,
        onReminder = { viewModel.onIntent(HealthIntent.SetReminder(it)) },
        onPickTime = {
            TimePickerDialog(
                context,
                { _, hour, minute -> viewModel.onIntent(HealthIntent.SetReminderMinute(hour * MINUTES_IN_HOUR + minute)) },
                state.settings.reminderMinute / MINUTES_IN_HOUR,
                state.settings.reminderMinute % MINUTES_IN_HOUR,
                true,
            ).show()
        },
    )
}

@Composable
fun HealthSettingsScreen(state: HealthState, onBack: () -> Unit, onBackup: () -> Unit, onReminder: (Boolean) -> Unit, onPickTime: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val settings = state.settings
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HEALTH_SETTINGS)) {
        ServiceHeader(stringResource(R.string.health_settings_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Section(stringResource(R.string.health_reminder_section))
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).padding(horizontal = spacing.md, vertical = spacing.sm),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.health_reminder), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(R.string.health_reminder_hint), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Switch(checked = settings.reminderOn, onCheckedChange = onReminder, modifier = Modifier.testTag(TAG_HEALTH_REMINDER_SWITCH))
                }
                if (settings.reminderOn) {
                    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AppTheme.sizes.operationRow)
                            .clickable(role = Role.Button, onClick = onPickTime)
                            .padding(horizontal = spacing.md, vertical = spacing.sm)
                            .testTag(TAG_HEALTH_REMINDER_TIME),
                    ) {
                        Text(stringResource(R.string.health_reminder_time), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(clock(settings.reminderMinute), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
            }
            Section(stringResource(R.string.health_data_section))
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                val count = state.entries?.size ?: 0
                NavRow(
                    R.drawable.ic_finance_backup,
                    stringResource(R.string.backup_title),
                    stringResource(R.string.health_backup_subtitle, lastBackupText(state.lastBackupAt), count),
                    TAG_HEALTH_BACKUP,
                    last = true,
                    onClick = onBackup,
                )
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = AppTheme.spacing.md, bottom = AppTheme.spacing.xs))
}

@Composable
private fun NavRow(@DrawableRes icon: Int, title: String, subtitle: String, tag: String, last: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.operationRow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .testTag(tag),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.moduleIcon))
        Column(Modifier.weight(1f).padding(start = spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inactive)
    }
    if (!last) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
}

/** Страница «Резервная копия» (#257, макет 4.3): сохранить в файл и восстановить из файла — файл выбирается в системном окне. */
@Composable
fun HealthBackupRoute(viewModel: HealthViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthEffects(viewModel)
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri -> uri?.let { viewModel.onIntent(HealthIntent.ExportBackup(it.toString())) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.onIntent(HealthIntent.PickBackup(it.toString())) } }
    HealthBackupScreen(
        state = state,
        onBack = onBack,
        onSave = { saver.launch("simple-tasks-health-${state.today}.json") },
        onRestore = { picker.launch(arrayOf(BACKUP_MIME, "*/*")) },
        onConfirm = { viewModel.onIntent(HealthIntent.ConfirmRestore) },
        onCancel = { viewModel.onIntent(HealthIntent.CloseRestore) },
    )
}

@Composable
fun HealthBackupScreen(state: HealthState, onBack: () -> Unit, onSave: () -> Unit, onRestore: () -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HEALTH_BACKUP_SCREEN)) {
        ServiceHeader(stringResource(R.string.backup_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                NavRow(R.drawable.ic_finance_backup, stringResource(R.string.health_backup_save), stringResource(R.string.health_backup_save_hint), TAG_HEALTH_BACKUP_SAVE, onClick = onSave)
                NavRow(R.drawable.ic_finance_backup, stringResource(R.string.health_backup_restore), stringResource(R.string.health_backup_restore_hint), TAG_HEALTH_BACKUP_RESTORE, last = true, onClick = onRestore)
            }
            Text(lastBackupText(state.lastBackupAt), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(spacing.md).testTag(TAG_HEALTH_BACKUP_LAST))
        }
    }
    state.restore?.let { RestoreDialog(it.info, state, onConfirm, onCancel) }
}

/** Подтверждение восстановления (#257, макет 4.3): сравнение «в копии / сейчас»; после «Заменить» данные «Здоровья» заменяются целиком. */
@Composable
private fun RestoreDialog(info: HealthBackupInfo, state: HealthState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = AppTheme.colors
    val entries = state.entries.orEmpty()
    val now = state.settings
    FinanceWindow(
        title = stringResource(R.string.health_restore_title),
        tag = TAG_HEALTH_RESTORE,
        onDismiss = onCancel,
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.backup_replace), TAG_HEALTH_RESTORE, onCancel, onConfirm) },
    ) {
        CompareRow("", stringResource(R.string.backup_in_file), stringResource(R.string.backup_now), header = true)
        CompareRow(stringResource(R.string.health_restore_entries), info.entries.toString(), entries.size.toString())
        CompareRow(stringResource(R.string.health_restore_period), period(info.from, info.to), period(entries.firstOrNull()?.date, entries.lastOrNull()?.date))
        CompareRow(stringResource(R.string.health_restore_goal), goalAndHeight(info.settings), goalAndHeight(now))
        CompareRow(stringResource(R.string.health_restore_reminder), reminder(info.settings), reminder(now))
        WindowText(stringResource(R.string.health_restore_warning))
    }
}

@Composable
private fun CompareRow(label: String, inFile: String, now: String, header: Boolean = false) {
    val colors = AppTheme.colors
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    FinanceField(label) {
        Row {
            Text(inFile, style = style, fontWeight = if (header) FontWeight.Bold else FontWeight.SemiBold, color = if (header) colors.textSecondary else colors.textPrimary, modifier = Modifier.padding(end = AppTheme.spacing.md))
            Text(now, style = style, color = if (header) colors.textSecondary else colors.textPrimary)
        }
    }
}

@Composable
private fun period(from: LocalDate?, to: LocalDate?): String =
    if (from == null || to == null) stringResource(R.string.health_restore_none) else "${dayMonthShort(from)} – ${dayMonthShort(to)}"

@Composable
private fun goalAndHeight(s: HealthSettings): String {
    val goal = s.goalTenths?.let(Weight::format) ?: "—"
    val height = s.heightCm?.toString() ?: "—"
    return "$goal / $height"
}

@Composable
private fun reminder(s: HealthSettings): String = if (s.reminderOn) clock(s.reminderMinute) else stringResource(R.string.health_restore_off)

const val TAG_HEALTH_SETTINGS = "health_settings"
const val TAG_HEALTH_REMINDER_SWITCH = "health_reminder_switch"
const val TAG_HEALTH_REMINDER_TIME = "health_reminder_time"
const val TAG_HEALTH_BACKUP = "health_backup"
const val TAG_HEALTH_BACKUP_SCREEN = "health_backup_screen"
const val TAG_HEALTH_BACKUP_SAVE = "health_backup_save"
const val TAG_HEALTH_BACKUP_RESTORE = "health_backup_restore"
const val TAG_HEALTH_BACKUP_LAST = "health_backup_last"
const val TAG_HEALTH_RESTORE = "health_restore"
