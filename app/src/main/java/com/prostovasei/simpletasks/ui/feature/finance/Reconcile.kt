package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.Operation
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** День создания счёта — к нему относится начальный остаток. */
fun Account.openedOn(): String = createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(DATE)

@Composable
private fun Value(text: String, tag: String, bold: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = AppTheme.colors.textPrimary, modifier = Modifier.testTag(tag))
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm))
}

/**
 * Сверка остатка (этап 4, #231; экраны 2.6–2.8, решение PM — все три способа): выбор способа, окно начального остатка
 * и окно реального остатка. Способ «Найти операцию» просто открывает список операций.
 */
@Composable
fun ReconcileDialogs(step: Reconcile, today: LocalDate, onIntent: (FinanceIntent) -> Unit) {
    val account = step.account
    val close = { onIntent(FinanceIntent.CloseReconcile) }
    when (step) {
        is Reconcile.Choose -> FinanceWindow(title = stringResource(R.string.reconcile_title), tag = TAG_RECONCILE, onDismiss = close, actions = {}) {
            MenuRow(R.drawable.ic_finance_operations, stringResource(R.string.reconcile_find), stringResource(R.string.reconcile_find_text), TAG_RECONCILE_FIND) { onIntent(FinanceIntent.FindOperation) }
            MenuRow(R.drawable.ic_snack_restore, stringResource(R.string.reconcile_initial), stringResource(R.string.reconcile_initial_text), TAG_RECONCILE_INITIAL) { onIntent(FinanceIntent.PickReconcile(Reconcile.Initial(account))) }
            MenuRow(R.drawable.ic_finance_wallet, stringResource(R.string.reconcile_real), stringResource(R.string.reconcile_real_text), TAG_RECONCILE_REAL, last = true) { onIntent(FinanceIntent.PickReconcile(Reconcile.Real(account))) }
            WindowText(stringResource(R.string.reconcile_app_balance, account.name, Money.format(account.balance)))
        }
        is Reconcile.Initial -> {
            var amount by rememberSaveable { mutableStateOf(Money.input(account.initialBalance).takeIf { account.initialBalance >= 0 }.orEmpty()) }
            val value = Money.parse(amount)
            FinanceWindow(
                title = stringResource(R.string.account_initial_balance),
                tag = TAG_RECONCILE,
                onDismiss = close,
                actions = { PrimaryAction(stringResource(R.string.action_save), TAG_RECONCILE_SAVE, enabled = value != null && value != account.initialBalance, onClick = { value?.let { onIntent(FinanceIntent.SaveInitialBalance(it)) } }) },
            ) {
                Caption(stringResource(R.string.reconcile_initial_caption, account.name, account.openedOn()))
                AmountField(amount, { amount = it }, TAG_RECONCILE_AMOUNT)
                FinanceField(stringResource(R.string.reconcile_was)) { Value(Money.format(account.initialBalance), TAG_RECONCILE_WAS) }
                // Операции не меняются: остаток сейчас сдвигается ровно на столько же, на сколько начальный.
                FinanceField(stringResource(R.string.reconcile_will_be)) { Value(Money.format(account.balance - account.initialBalance + (value ?: account.initialBalance)), TAG_RECONCILE_RESULT, bold = true) }
                WindowText(stringResource(R.string.reconcile_initial_note))
            }
        }
        is Reconcile.Real -> {
            var amount by rememberSaveable { mutableStateOf(Money.input(account.balance).takeIf { account.balance >= 0 }.orEmpty()) }
            val value = Money.parse(amount)
            val difference = (value ?: account.balance) - account.balance
            FinanceWindow(
                title = stringResource(R.string.reconcile_real_title),
                tag = TAG_RECONCILE,
                onDismiss = close,
                actions = { PrimaryAction(stringResource(R.string.action_save), TAG_RECONCILE_SAVE, enabled = value != null && difference != 0L, onClick = { value?.let { onIntent(FinanceIntent.SaveRealBalance(it)) } }) },
            ) {
                Caption(stringResource(R.string.reconcile_real_caption, account.name))
                AmountField(amount, { amount = it }, TAG_RECONCILE_AMOUNT)
                FinanceField(stringResource(R.string.reconcile_in_app)) { Value(Money.format(account.balance), TAG_RECONCILE_WAS) }
                FinanceField(stringResource(R.string.operation_correction)) { Value(Money.format(difference, sign = true), TAG_RECONCILE_RESULT, bold = true) }
                FinanceField(stringResource(R.string.operation_date)) { Value(dateLabel(today, today), "$TAG_RECONCILE:date") }
                WindowText(stringResource(R.string.reconcile_real_note))
            }
        }
    }
}

/** Корректировка из списка операций: только просмотр и корзина в заголовке — править её нельзя, можно удалить и сверить заново. */
@Composable
fun CorrectionDialog(operation: Operation, today: LocalDate, onTrash: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(
        title = stringResource(R.string.operation_correction),
        tag = TAG_CORRECTION_WINDOW,
        onDismiss = onDismiss,
        actions = { SecondaryAction(stringResource(R.string.operation_trash), TAG_OPERATION_TRASH, onTrash) },
    ) {
        FinanceField(stringResource(R.string.operation_account)) { Value(operation.accountName, "$TAG_CORRECTION_WINDOW:account", bold = true) }
        FinanceField(stringResource(R.string.account_amount)) { Value(Money.format(operation.amount, sign = true), "$TAG_CORRECTION_WINDOW:amount", bold = true) }
        FinanceField(stringResource(R.string.operation_date)) { Value(dateLabel(operation.date, today), "$TAG_CORRECTION_WINDOW:date") }
        WindowText(stringResource(R.string.reconcile_real_note))
    }
}

const val TAG_RECONCILE = "reconcile"
const val TAG_RECONCILE_FIND = "reconcile_find"
const val TAG_RECONCILE_INITIAL = "reconcile_initial"
const val TAG_RECONCILE_REAL = "reconcile_real"
const val TAG_RECONCILE_AMOUNT = "reconcile_amount"
const val TAG_RECONCILE_WAS = "reconcile_was"
const val TAG_RECONCILE_RESULT = "reconcile_result"
const val TAG_RECONCILE_SAVE = "reconcile_save"
const val TAG_CORRECTION_WINDOW = "correction_window"
