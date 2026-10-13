package com.prostovasei.simpletasks.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportReport

/** Экран «Настройки» с ViewModel. До Итерации 04 — стартовый экран приложения. */
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
    SettingsScreen(state = state, onIntent = viewModel::onIntent, snackbarHostState = snackbar)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onIntent(SettingsIntent.FileSelected(it.toString())) }
    }
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
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
        }
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

const val TAG_IMPORT_BUTTON = "settings_import"
const val TAG_CLEAR_BUTTON = "settings_clear"

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
