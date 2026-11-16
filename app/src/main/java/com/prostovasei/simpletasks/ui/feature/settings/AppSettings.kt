package com.prostovasei.simpletasks.ui.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.finance.ServiceHeader
import com.prostovasei.simpletasks.widget.WidgetLog

/**
 * Единые настройки (#241, макет 3.1; решение PM 13.11.2026): главная страница с тремя разделами — «Задачи», «Финансы», «Приложение».
 * Открывается из меню кнопки модуля в нижней панели одинаково из любого модуля; стрелка назад возвращает туда, откуда пришли.
 */
@Composable
fun AppSettingsRoute(onBack: () -> Unit, onTasks: () -> Unit, onFinance: () -> Unit, onHealth: () -> Unit, onApp: () -> Unit) {
    AppSettingsScreen(AppVersion.current, onBack, onTasks, onFinance, onHealth, onApp)
}

@Composable
fun AppSettingsScreen(appVersion: AppVersion, onBack: () -> Unit, onTasks: () -> Unit, onFinance: () -> Unit, onHealth: () -> Unit, onApp: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_APP_SETTINGS)) {
        ServiceHeader(stringResource(R.string.settings_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                SectionRow(R.drawable.ic_module_tasks, stringResource(R.string.settings_home_tasks), stringResource(R.string.settings_home_tasks_hint), TAG_SETTINGS_TASKS, onClick = onTasks)
                SectionRow(R.drawable.ic_module_finance, stringResource(R.string.settings_home_finance), stringResource(R.string.settings_home_finance_hint), TAG_SETTINGS_FINANCE, onClick = onFinance)
                SectionRow(R.drawable.ic_module_health, stringResource(R.string.settings_home_health), stringResource(R.string.settings_home_health_hint), TAG_SETTINGS_HEALTH, onClick = onHealth)
                SectionRow(
                    R.drawable.ic_settings_app,
                    stringResource(R.string.settings_home_app),
                    stringResource(R.string.settings_home_app_hint, appVersion.name),
                    TAG_SETTINGS_APP,
                    last = true,
                    onClick = onApp,
                )
            }
        }
    }
}

@Composable
private fun SectionRow(@androidx.annotation.DrawableRes icon: Int, title: String, subtitle: String, tag: String, last: Boolean = false, onClick: () -> Unit) {
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

/** Раздел «Приложение» (#241, макет 3.4): обновление и версия — они относятся ко всему приложению, а не к задачам. */
@Composable
fun AppInfoRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AppInfoScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        widgetLog = WidgetLogActions(
            read = { WidgetLog.read(context) },
            share = { text ->
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
                context.startActivity(android.content.Intent.createChooser(send, null))
            },
            clear = { WidgetLog.clear(context) },
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppInfoScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    appVersion: AppVersion = AppVersion.current,
    widgetLog: WidgetLogActions = WidgetLogActions.None,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var logText by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { UndoSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag(TAG_APP_INFO)) {
            ServiceHeader(stringResource(R.string.settings_home_app), onBack)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                UpdateSection(state.update, onIntent)
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
            confirmButton = { TextButton(onClick = { widgetLog.share(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.widget_log_share)) } },
            dismissButton = {
                TextButton(onClick = {
                    widgetLog.clear()
                    logText = null
                }) { Text(stringResource(R.string.widget_log_clear)) }
            },
        )
    }
}

const val TAG_APP_SETTINGS = "app_settings"
const val TAG_APP_INFO = "app_info"
const val TAG_SETTINGS_TASKS = "settings_tasks"
const val TAG_SETTINGS_FINANCE = "settings_finance"
const val TAG_SETTINGS_HEALTH = "settings_health"
const val TAG_SETTINGS_APP = "settings_app"
