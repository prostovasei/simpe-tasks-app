package com.prostovasei.simpletasks.ui.feature.settings

import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.provider.Settings
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.BuildConfig
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.widget.WidgetLog
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportReport

/** Экран «Настройки» с ViewModel. */
@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.settings_cleared)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.DataCleared -> snackbar.showSnackbar(clearedMessage)
            }
        }
    }
    // Уведомления могли выключить в системе — проверяем при каждом возвращении на экран.
    val context = LocalContext.current
    var notificationsOff by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        notificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    SettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbar,
        notificationsOff = notificationsOff,
        onEnableNotifications = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        widgetLog = WidgetLogActions(
            read = { WidgetLog.read(context) },
            share = { text ->
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            },
            clear = { WidgetLog.clear(context) },
        ),
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    appVersion: AppVersion = AppVersion.current,
    notificationsOff: Boolean = false,
    onEnableNotifications: () -> Unit = {},
    widgetLog: WidgetLogActions = WidgetLogActions.None,
) {
    var logText by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onIntent(SettingsIntent.FileSelected(it.toString())) }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)) { uri ->
        uri?.let { onIntent(SettingsIntent.ExportTo(it.toString())) }
    }
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            if (notificationsOff) {
                SectionTitle(R.string.settings_notifications_title)
                Text(
                    text = stringResource(R.string.settings_notifications_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.overdue,
                )
                Button(onClick = onEnableNotifications, modifier = Modifier.testTag(TAG_NOTIFICATIONS_BUTTON)) {
                    Text(stringResource(R.string.settings_notifications_enable))
                }
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = spacing.xs))
            }

            SectionTitle(R.string.settings_import_title)
            Text(
                text = stringResource(R.string.settings_import_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !state.isBusy,
                modifier = Modifier.testTag(TAG_IMPORT_BUTTON),
            ) {
                Text(stringResource(R.string.settings_import_button))
            }
            ImportStatus(state.import)

            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = spacing.xs))

            SectionTitle(R.string.settings_export_title)
            Text(
                text = stringResource(R.string.settings_export_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            OutlinedButton(
                onClick = { saver.launch(state.backupFileName) },
                enabled = !state.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                modifier = Modifier.testTag(TAG_EXPORT_BUTTON),
            ) {
                Text(stringResource(R.string.settings_export_button))
            }
            ExportStatus(state.export)

            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = spacing.xs))

            SectionTitle(R.string.settings_data_title)
            state.stats?.let { stats ->
                BodyText(stringResource(R.string.settings_stats_tasks, stats.totalTasks, stats.activeTasks))
                BodyText(stringResource(R.string.settings_stats_lists, stats.lists))
            }
            OutlinedButton(
                onClick = { onIntent(SettingsIntent.ClearRequested) },
                enabled = !state.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                modifier = Modifier.testTag(TAG_CLEAR_BUTTON),
            ) {
                Text(stringResource(if (state.isClearing) R.string.settings_clearing else R.string.settings_clear_button))
            }

            // Долгое нажатие на версию — журнал виджета (скрытый пункт для диагностики, доработка 0.9.1).
            Text(
                text = stringResource(R.string.settings_version, appVersion.name, appVersion.build),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier
                    .padding(top = spacing.lg)
                    .combinedClickable(onClick = {}, onLongClick = { logText = widgetLog.read() })
                    .testTag(TAG_VERSION),
            )
        }
    }

    logText?.let { text ->
        AlertDialog(
            onDismissRequest = { logText = null },
            title = { Text(stringResource(R.string.widget_log_title)) },
            text = {
                Text(
                    text.ifBlank { stringResource(R.string.widget_log_empty) },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = AppTheme.sizes.pickerMaxHeight).verticalScroll(rememberScrollState()).testTag(TAG_WIDGET_LOG),
                )
            },
            confirmButton = {
                TextButton(onClick = { widgetLog.share(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.widget_log_share)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    widgetLog.clear()
                    logText = null
                }) { Text(stringResource(R.string.widget_log_clear)) }
            },
        )
    }

    if (state.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.ClearDismissed) },
            title = { Text(stringResource(R.string.settings_clear_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(SettingsIntent.ClearConfirmed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.danger),
                ) { Text(stringResource(R.string.settings_clear_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.ClearDismissed) }) {
                    Text(stringResource(R.string.settings_clear_dialog_cancel))
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
        )
    }
}

@Composable
private fun ImportStatus(import: ImportState) {
    val colors = AppTheme.colors
    when (import) {
        ImportState.Idle -> Unit
        is ImportState.InProgress -> {
            val progressModifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.sizes.progressHeight)
            if (import.total == 0) {
                LinearProgressIndicator(modifier = progressModifier, color = colors.accent, trackColor = colors.surface)
                BodyText(stringResource(R.string.settings_import_reading))
            } else {
                LinearProgressIndicator(
                    progress = { import.processed.toFloat() / import.total },
                    modifier = progressModifier,
                    color = colors.accent,
                    trackColor = colors.surface,
                )
                BodyText(stringResource(R.string.settings_import_progress, import.processed, import.total))
            }
        }
        is ImportState.Done -> ImportReportView(import.report)
        is ImportState.Failed -> Text(
            text = stringResource(
                when (import.reason) {
                    ImportFailure.NOT_TICKTICK_FILE -> R.string.settings_import_error_format
                    ImportFailure.READ_ERROR -> R.string.settings_import_error_read
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.danger,
        )
    }
}

@Composable
private fun ExportStatus(export: ExportState) {
    when (export) {
        ExportState.Idle -> Unit
        ExportState.InProgress -> BodyText(stringResource(R.string.settings_export_in_progress))
        is ExportState.Done -> BodyText(stringResource(R.string.settings_export_done, export.tasks))
        ExportState.Failed -> Text(
            text = stringResource(R.string.settings_export_error),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.danger,
        )
    }
}

@Composable
private fun ImportReportView(report: ImportReport) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        Text(
            text = stringResource(R.string.settings_import_done),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
        )
        BodyText(stringResource(R.string.settings_report_added, report.tasksAdded))
        BodyText(stringResource(R.string.settings_report_skipped, report.tasksSkipped))
        BodyText(stringResource(R.string.settings_report_lists, report.listsCreated))
        if (report.foldersCreated > 0) BodyText(stringResource(R.string.settings_report_folders, report.foldersCreated))
        BodyText(stringResource(R.string.settings_report_checklist, report.checklistItems))
        BodyText(stringResource(R.string.settings_report_subtasks, report.subtasksLinked))
        if (report.rowsFailed > 0) {
            Text(
                text = stringResource(R.string.settings_report_failed, report.rowsFailed),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.danger,
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.textPrimary,
        modifier = Modifier.padding(top = AppTheme.spacing.xs),
    )
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textPrimary)
}

/** Версия приложения для подвала экрана; в тестах подменяется, чтобы скриншоты не зависели от номера сборки. */
/** Журнал виджета: прочитать, отправить (например, в чат с Claude), очистить. */
data class WidgetLogActions(val read: () -> String, val share: (String) -> Unit, val clear: () -> Unit) {
    companion object {
        val None = WidgetLogActions(read = { "" }, share = {}, clear = {})
    }
}

const val TAG_VERSION = "settings_version"
const val TAG_WIDGET_LOG = "widget_log"

data class AppVersion(val name: String, val build: Int) {
    companion object {
        val current = AppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
}

private const val CSV_MIME_TYPE = "text/csv"
const val TAG_IMPORT_BUTTON = "settings_import"
const val TAG_EXPORT_BUTTON = "settings_export"
const val TAG_CLEAR_BUTTON = "settings_clear"
const val TAG_NOTIFICATIONS_BUTTON = "settings_notifications"

internal val previewReport = ImportReport(
    tasksAdded = 3399,
    tasksSkipped = 0,
    listsCreated = 11,
    foldersCreated = 0,
    checklistItems = 1186,
    subtasksLinked = 22,
    rowsFailed = 0,
)

@Preview(name = "Итог импорта", showBackground = true)
@Composable
private fun SettingsDonePreview() {
    AppTheme {
        SettingsScreen(
            state = SettingsState(stats = DataStats(3399, 145, 12), import = ImportState.Done(previewReport)),
            onIntent = {},
        )
    }
}
