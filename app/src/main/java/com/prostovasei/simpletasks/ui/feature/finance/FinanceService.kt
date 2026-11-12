package com.prostovasei.simpletasks.ui.feature.finance

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Строка окна-меню: значок, название и подпись под ним; нажимается целиком. */
@Composable
fun MenuRow(@DrawableRes icon: Int, title: String, subtitle: String, tag: String, last: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).clickable(role = Role.Button, onClick = onClick).padding(vertical = spacing.xs).testTag(tag),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.moduleIcon))
        Column(Modifier.padding(start = spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
    if (!last) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
}

/** Две кнопки окна в ряд: серая слева, чёрная справа («Отмена» / «Удалить»). */
@Composable
fun TwoActions(cancel: String, confirm: String, tag: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Box(Modifier.weight(1f)) { SecondaryAction(cancel, "$tag:cancel", onCancel) }
        Box(Modifier.weight(1f)) { PrimaryAction(confirm, "$tag:confirm", onConfirm) }
    }
}

@Composable
fun WindowText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary, modifier = modifier.padding(vertical = AppTheme.spacing.xs))
}

/** Меню шестерёнки финансов (этап 4, #231; экран 3.1): единственный вход в категории, корзину и резервную копию. */
@Composable
fun FinanceMenu(state: FinanceState, onCategories: () -> Unit, onTrash: () -> Unit, onBackup: () -> Unit, onDismiss: () -> Unit, onBank: () -> Unit = {}) {
    val active = state.categories.filterNot { it.archived }
    FinanceWindow(title = stringResource(R.string.module_finance), tag = TAG_FINANCE_MENU, onDismiss = onDismiss, actions = {}) {
        MenuRow(R.drawable.ic_finance_categories, stringResource(R.string.categories_title), stringResource(R.string.menu_categories_count, active.size), TAG_MENU_CATEGORIES, onClick = onCategories)
        MenuRow(R.drawable.ic_tool_delete, stringResource(R.string.finance_trash), stringResource(R.string.menu_trash_count, state.trash.size), TAG_MENU_TRASH, onClick = onTrash)
        MenuRow(R.drawable.ic_finance_backup, stringResource(R.string.backup_title), lastBackupText(state.lastBackupAt), TAG_MENU_BACKUP, onClick = onBackup)
        // #233: уведомления банка — четвёртая строка (макет 1.1).
        MenuRow(R.drawable.ic_bank_notifications, stringResource(R.string.bank_title), bankMenuSubtitle(state), TAG_MENU_BANK, last = true, onClick = onBank)
    }
}

private val BACKUP_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")

@Composable
private fun lastBackupText(at: Long?): String =
    if (at == null) stringResource(R.string.backup_never) else stringResource(R.string.backup_last, Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(BACKUP_TIME))

@Composable
private fun countsText(counts: FinanceCounts) = stringResource(R.string.backup_counts, counts.accounts, counts.categories, counts.operations)

/**
 * Окно резервной копии (экран 3.7): сохранить в файл или восстановить из файла — место и файл выбираются в системном окне.
 */
@Composable
fun BackupDialog(lastBackupAt: Long?, onSave: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(
        title = stringResource(R.string.backup_title),
        tag = TAG_BACKUP_WINDOW,
        onDismiss = onDismiss,
        actions = {
            PrimaryAction(stringResource(R.string.backup_save), TAG_BACKUP_SAVE, onSave)
            SecondaryAction(stringResource(R.string.backup_restore), TAG_BACKUP_RESTORE, onRestore)
        },
    ) {
        WindowText(stringResource(R.string.backup_text))
        WindowText(lastBackupText(lastBackupAt), Modifier.testTag(TAG_BACKUP_LAST))
    }
}

/** Подтверждение восстановления (экран 3.8): что в копии и что сейчас; после «Заменить» данные финансов заменяются целиком. */
@Composable
fun RestoreConfirmDialog(inFile: FinanceCounts, current: FinanceCounts, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(
        title = stringResource(R.string.backup_confirm_title),
        tag = TAG_RESTORE_WINDOW,
        onDismiss = onDismiss,
        actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.backup_replace), TAG_RESTORE_WINDOW, onDismiss, onConfirm) },
    ) {
        FinanceField(stringResource(R.string.backup_in_file)) { Text(countsText(inFile), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary) }
        FinanceField(stringResource(R.string.backup_now)) { Text(countsText(current), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textPrimary) }
        WindowText(stringResource(R.string.backup_confirm_text))
    }
}

/** Подтверждение необратимого действия: текст и «Отмена» / [confirm]. */
@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, tag: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(title = title, tag = tag, onDismiss = onDismiss, actions = { TwoActions(stringResource(R.string.action_cancel), confirm, tag, onDismiss, onConfirm) }) {
        WindowText(text)
    }
}

const val BACKUP_MIME = "application/json"

const val TAG_FINANCE_MENU = "finance_menu"
const val TAG_MENU_CATEGORIES = "finance_menu_categories"
const val TAG_MENU_TRASH = "finance_menu_trash"
const val TAG_MENU_BACKUP = "finance_menu_backup"
const val TAG_BACKUP_WINDOW = "backup_window"
const val TAG_BACKUP_SAVE = "backup_save"
const val TAG_BACKUP_RESTORE = "backup_restore"
const val TAG_BACKUP_LAST = "backup_last"
const val TAG_RESTORE_WINDOW = "restore_window"
