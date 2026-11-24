package com.prostovasei.simpletasks.ui.feature.finance

import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareSizes
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareColors
import com.prostovasei.simpletasks.domain.model.finance.financeThousands
import com.prostovasei.simpletasks.domain.model.finance.Money

@Composable
fun FinanceRoute(viewModel: FinanceViewModel, onOpenUnsorted: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceEffects(viewModel)
    // Подпись «Уведомления банка» на странице настроек должна быть свежей после возврата из настроек Android.
    androidx.lifecycle.compose.LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(FinanceIntent.RefreshBankAccess)
        onPauseOrDispose {}
    }
    FinanceScreen(state, onOpenUnsorted, viewModel::onIntent)
}

/** Подсказки финансов (сохранено, корзина, копия): на главном экране и на странице настроек, где открывается резервная копия. */
@Composable
fun FinanceEffects(viewModel: FinanceViewModel, onAccountSaved: () -> Unit = {}) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FinanceEffect.AccountSaved -> onAccountSaved()
                FinanceEffect.OperationSaved -> Toast.makeText(context, R.string.operation_saved, Toast.LENGTH_SHORT).show()
                FinanceEffect.OperationTrashed -> Toast.makeText(context, R.string.operation_trashed, Toast.LENGTH_SHORT).show()
                FinanceEffect.OperationRestored -> Toast.makeText(context, R.string.operation_restored, Toast.LENGTH_SHORT).show()
                FinanceEffect.BackupSaved -> Toast.makeText(context, R.string.backup_saved, Toast.LENGTH_SHORT).show()
                FinanceEffect.BackupFailed -> Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show()
                FinanceEffect.BackupInvalid -> Toast.makeText(context, R.string.backup_invalid, Toast.LENGTH_LONG).show()
                FinanceEffect.BackupRestored -> Toast.makeText(context, R.string.backup_restored, Toast.LENGTH_SHORT).show()
                FinanceEffect.PurchaseRecorded -> Toast.makeText(context, R.string.bank_recorded, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** Счета, аналитика и операции в одном прокручиваемом списке (#267). */
@Composable
fun FinanceScreen(state: FinanceState, onOpenUnsorted: () -> Unit = {}, onIntent: (FinanceIntent) -> Unit) = TaskCanvasTheme {
    val colors = AppTheme.colors
    val accounts = state.accounts
    Box(Modifier.fillMaxSize().background(colors.surface).testTag(TAG_FINANCE)) {
        when {
            accounts == null -> Unit
            accounts.isEmpty() -> FinanceEmpty { onIntent(FinanceIntent.NewAccount) }
            else -> {
                val list = rememberLazyListState()
                LaunchedEffect(state.tab, state.operations != null) {
                    if (state.tab == FinanceTab.OPERATIONS && state.operations != null) {
                        list.scrollToItem(2)
                        onIntent(FinanceIntent.SelectTab(FinanceTab.ANALYTICS))
                    }
                }
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag(TAG_OPERATIONS)) {
                    item("accounts") { AccountsBlock(accounts, state.total, onIntent) }
                    item("analytics") { if (state.operations != null) AnalyticsBlock(state, onIntent) }
                    state.operations?.let {
                        operationsFeed(it, state.today, state.pending.size, onOpenUnsorted) { id ->
                            onIntent(FinanceIntent.OpenOperation(id))
                        }
                    }
                }
            }
        }
    }
    FinanceDialogs(state, onIntent)
}

@Composable
internal fun FinanceDialogs(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    state.form?.let { OperationDialog(state, it, onIntent) }
    when (val window = state.window) {
        AccountWindow.None -> Unit
        AccountWindow.Create -> AccountDialog(null, onSave = { onIntent(FinanceIntent.SaveAccount(it)) }, onDismiss = { onIntent(FinanceIntent.CloseWindow) })
        is AccountWindow.Edit -> AccountDialog(window.account, onSave = { onIntent(FinanceIntent.SaveAccount(it)) }, onDismiss = { onIntent(FinanceIntent.CloseWindow) }, onReconcile = { onIntent(FinanceIntent.OpenReconcile) })
    }
    BudgetDialog(state, onIntent)
    state.reconcile?.let { ReconcileDialogs(it, state.today, onIntent) }
    state.correction?.let { CorrectionDialog(it, state.today, onTrash = { onIntent(FinanceIntent.TrashOperation) }, onDismiss = { onIntent(FinanceIntent.CloseCorrection) }) }
}

@Composable
private fun AccountsBlock(accounts: List<Account>, total: Long, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val add = stringResource(R.string.finance_add_account)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = spacing.md + spacing.xxs, end = spacing.md, top = spacing.xs, bottom = spacing.xs)) {
        Text(
            "${financeThousands(total)} ₽",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f).testTag(TAG_ACCOUNTS_TOTAL).semantics { contentDescription = Money.format(total) },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sizes.accountAdd)
                .clip(AppTheme.shapes.medium)
                .background(colors.surface)
                .clickable(role = Role.Button) { onIntent(FinanceIntent.NewAccount) }
                .semantics { contentDescription = add }
                .testTag(TAG_ACCOUNT_ADD),
        ) { Icon(Icons.Default.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(sizes.accountAddIcon)) }
    }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag(TAG_ACCOUNTS)) {
        val columns = (maxWidth.value / (FinanceShareSizes.accountMin * fontScale)).toInt().coerceIn(1, 4)
        val tileWidth = (maxWidth - spacing.xs * (columns - 1)) / columns
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            accounts.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    group.forEach { account ->
                        AccountTile(account, Modifier.width(tileWidth)) { onIntent(FinanceIntent.OpenAccount(account.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTile(account: Account, modifier: Modifier, onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    Row(
        modifier.clip(CircleShape).background(FinanceShareColors.accountSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${account.name}, ${Money.format(account.balance)}" }
            .padding(horizontal = spacing.xs, vertical = spacing.xs)
            .testTag("$TAG_ACCOUNT:${account.id}"),
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(account.emojiShown, style = MaterialTheme.typography.labelSmall)
        Text(financeThousands(account.balance), style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, color = AppTheme.colors.textPrimary)
    }
}

@Composable
private fun FinanceEmpty(onCreate: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl).testTag(TAG_FINANCE_EMPTY),
    ) {
        Icon(painterResource(R.drawable.ic_finance_wallet), contentDescription = null, tint = colors.inactive, modifier = Modifier.size(AppTheme.sizes.financeEmptyIcon))
        Text(stringResource(R.string.finance_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(top = spacing.md))
        Text(
            stringResource(R.string.finance_empty_text),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs, bottom = spacing.lg),
        )
        PrimaryAction(stringResource(R.string.finance_create_account), TAG_FINANCE_CREATE, onCreate, plus = true)
    }
}

/** Чёрная кнопка во всю ширину: «Создать счёт», «Сохранить». [plus] — значок «+» слева. */
@Composable
internal fun PrimaryAction(label: String, tag: String, onClick: () -> Unit, enabled: Boolean = true, plus: Boolean = false, danger: Boolean = false) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.large)
            .background(if (!enabled) colors.divider else if (danger) colors.danger else colors.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = AppTheme.spacing.sm)
            .testTag(tag),
    ) { ActionLabel(label, if (enabled) colors.onAccent else colors.textSecondary, plus) }
}

@Composable
private fun RowScope.ActionLabel(label: String, color: androidx.compose.ui.graphics.Color, plus: Boolean) {
    if (plus) Icon(Icons.Default.Add, contentDescription = null, tint = color, modifier = Modifier.padding(end = AppTheme.spacing.xs).size(AppTheme.sizes.accountAddIcon))
    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
}

/** До скольких счетов карточки делят ширину экрана поровну. */

const val TAG_FINANCE = "finance"
const val TAG_FINANCE_EMPTY = "finance_empty"
const val TAG_FINANCE_CREATE = "finance_create_account"
const val TAG_ACCOUNTS = "finance_accounts"
const val TAG_ACCOUNTS_TOTAL = "finance_accounts_total"
const val TAG_ACCOUNT_ADD = "finance_account_add"
const val TAG_ACCOUNT = "account"
