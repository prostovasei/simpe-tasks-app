package com.prostovasei.simpletasks.ui.feature.finance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.Money

@Composable
fun FinanceRoute(viewModel: FinanceViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FinanceEffect.OperationSaved -> Toast.makeText(context, R.string.operation_saved, Toast.LENGTH_SHORT).show()
                FinanceEffect.OperationTrashed -> Toast.makeText(context, R.string.operation_trashed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    FinanceScreen(state, viewModel::onIntent)
}

/**
 * Экран финансов (этап 1 — #221, этап 2 — #225; макеты design/approved/217/service-screens.html): без заголовка модуля.
 * Счетов нет — «Начнём со счёта»; есть — карточки счетов сверху, под ними аналитика расходов за месяц (этап 3 — #229), и вкладка «Операции».
 */
@Composable
fun FinanceScreen(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val accounts = state.accounts
    Box(Modifier.fillMaxSize().background(colors.background).testTag(TAG_FINANCE)) {
        when {
            accounts == null -> Unit
            accounts.isEmpty() -> FinanceEmpty { onIntent(FinanceIntent.NewAccount) }
            state.tab == FinanceTab.OPERATIONS -> state.operations?.let { OperationsList(it, state.today) { id -> onIntent(FinanceIntent.OpenOperation(id)) } }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                AccountsBlock(accounts, state.total, onIntent)
                AnalyticsBlock(state, onIntent)
                Box(Modifier.padding(bottom = AppTheme.spacing.lg))
            }
        }
    }
    state.form?.let { OperationDialog(state, it, onIntent) }
    when (val window = state.window) {
        AccountWindow.None -> Unit
        AccountWindow.Create -> AccountDialog(null, onSave = { onIntent(FinanceIntent.SaveAccount(it)) }, onDismiss = { onIntent(FinanceIntent.CloseWindow) })
        is AccountWindow.Edit -> AccountDialog(window.account, onSave = { onIntent(FinanceIntent.SaveAccount(it)) }, onDismiss = { onIntent(FinanceIntent.CloseWindow) })
    }
}

/** Блок «Счета · сумма» с маленьким «+»: до трёх счетов — карточки во всю ширину, больше — листаются вбок, следующая выглядывает. */
@Composable
private fun AccountsBlock(accounts: List<Account>, total: Long, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val add = stringResource(R.string.finance_add_account)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = spacing.md + spacing.xxs, end = spacing.md, top = spacing.xs, bottom = spacing.xs)) {
        Text(stringResource(R.string.finance_accounts), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(
            " · ${Money.format(total)}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            maxLines = 1,
            modifier = Modifier.weight(1f).testTag(TAG_ACCOUNTS_TOTAL),
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
    val open: (Account) -> Unit = { onIntent(FinanceIntent.OpenAccount(it.id)) }
    if (accounts.size <= WIDE_TILES) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag(TAG_ACCOUNTS)) {
            accounts.forEach { AccountTile(it, Modifier.weight(1f), open) }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = spacing.md).testTag(TAG_ACCOUNTS),
        ) {
            accounts.forEach { AccountTile(it, Modifier.widthIn(min = sizes.accountTile), open) }
        }
    }
}

@Composable
private fun AccountTile(account: Account, modifier: Modifier, onClick: (Account) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        modifier
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .clickable(role = Role.Button) { onClick(account) }
            .padding(start = spacing.sm, end = spacing.xs, top = spacing.sm, bottom = spacing.sm)
            .testTag("$TAG_ACCOUNT:${account.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(account.emojiShown, style = MaterialTheme.typography.labelMedium)
            Text(
                account.name,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = spacing.xxs),
            )
        }
        Text(
            Money.format(account.balance),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
}

/** Первый запуск (экран 2.1): счетов нет — значок, пояснение и чёрная кнопка «Создать счёт». Готовых счетов и категорий нет. */
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
internal fun PrimaryAction(label: String, tag: String, onClick: () -> Unit, enabled: Boolean = true, plus: Boolean = false) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.large)
            .background(if (enabled) colors.accent else colors.divider)
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
private const val WIDE_TILES = 3

const val TAG_FINANCE = "finance"
const val TAG_FINANCE_EMPTY = "finance_empty"
const val TAG_FINANCE_CREATE = "finance_create_account"
const val TAG_ACCOUNTS = "finance_accounts"
const val TAG_ACCOUNTS_TOTAL = "finance_accounts_total"
const val TAG_ACCOUNT_ADD = "finance_account_add"
const val TAG_ACCOUNT = "account"
