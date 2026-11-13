package com.prostovasei.simpletasks.ui.feature.finance

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

@Composable
fun FinanceSettingsRoute(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onPlaces: () -> Unit,
    onTrash: () -> Unit,
    onBank: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceEffects(viewModel)
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(FinanceIntent.RefreshBankAccess)
        onPauseOrDispose {}
    }
    FinanceSettingsScreen(state, onBack, onCategories, onPlaces, onTrash, onBackup = { viewModel.onIntent(FinanceIntent.OpenBackup) }, onBank)

    // Резервная копия: место сохранения и файл копии выбираются в системном окне.
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri -> uri?.let { viewModel.onIntent(FinanceIntent.ExportBackup(it.toString())) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.onIntent(FinanceIntent.PickBackup(it.toString())) } }
    when (val backup = state.backup) {
        null -> Unit
        BackupWindow.Actions -> BackupDialog(
            state.lastBackupAt,
            onSave = { saver.launch("simple-tasks-finance-${state.today}.json") },
            onRestore = { picker.launch(arrayOf(BACKUP_MIME, "*/*")) },
            onDismiss = { viewModel.onIntent(FinanceIntent.CloseBackup) },
        )
        is BackupWindow.Confirm -> RestoreConfirmDialog(backup.inFile, state.counts, onConfirm = { viewModel.onIntent(FinanceIntent.ConfirmRestore) }, onDismiss = { viewModel.onIntent(FinanceIntent.CloseBackup) })
    }
}

/**
 * Страница «Настройки финансов» (#239, макет 1.1; решение PM 13.11.2026): вместо окна-меню шестерёнки — отдельный экран со стрелкой назад.
 * Пункты: категории, места, корзина, резервная копия, уведомления банка — с подписями-счётчиками.
 */
@Composable
fun FinanceSettingsScreen(
    state: FinanceState,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onPlaces: () -> Unit,
    onTrash: () -> Unit,
    onBackup: () -> Unit,
    onBank: () -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_FINANCE_SETTINGS)) {
        ServiceHeader(stringResource(R.string.finance_settings), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                SettingsRow(R.drawable.ic_finance_categories, stringResource(R.string.categories_title), stringResource(R.string.menu_categories_count, state.categories.count { !it.archived }), TAG_MENU_CATEGORIES, onClick = onCategories)
                SettingsRow(R.drawable.ic_finance_place, stringResource(R.string.places_title), stringResource(R.string.menu_places_count, state.places.count { !it.archived }), TAG_MENU_PLACES, onClick = onPlaces)
                SettingsRow(R.drawable.ic_tool_delete, stringResource(R.string.finance_trash), stringResource(R.string.menu_trash_count, state.trash.size), TAG_MENU_TRASH, onClick = onTrash)
                SettingsRow(R.drawable.ic_finance_backup, stringResource(R.string.backup_title), lastBackupText(state.lastBackupAt), TAG_MENU_BACKUP, onClick = onBackup)
                SettingsRow(R.drawable.ic_bank_notifications, stringResource(R.string.bank_title), bankMenuSubtitle(state), TAG_MENU_BANK, last = true, onClick = onBank)
            }
        }
    }
}

@Composable
private fun SettingsRow(@DrawableRes icon: Int, title: String, subtitle: String, tag: String, last: Boolean = false, onClick: () -> Unit) {
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

const val TAG_FINANCE_SETTINGS = "finance_settings"
const val TAG_MENU_PLACES = "finance_menu_places"
