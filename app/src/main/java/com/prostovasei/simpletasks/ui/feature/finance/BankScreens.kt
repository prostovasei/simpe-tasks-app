package com.prostovasei.simpletasks.ui.feature.finance

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import com.prostovasei.simpletasks.notifications.bank.AndroidNotificationAccess
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Подпись строки «Уведомления банка» в меню шестерёнки (макет 1.1). */
@Composable
fun bankMenuSubtitle(state: FinanceState): String = when {
    state.bank.enabled && !state.bankAccess -> stringResource(R.string.bank_menu_no_access)
    state.bankActive -> stringResource(R.string.bank_menu_on, state.bankAccount?.name.orEmpty())
    else -> stringResource(R.string.bank_menu_off)
}

@Composable
fun BankSettingsRoute(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Доступ выдаётся в настройках Android — после возврата проверяем снова.
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(FinanceIntent.RefreshBankAccess)
        onPauseOrDispose {}
    }
    BankSettingsScreen(state, viewModel::onIntent, onBack, onGiveAccess = {
        try {
            context.startActivity(AndroidNotificationAccess.settingsIntent(context))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(AndroidNotificationAccess.listIntent())
        }
    })
}

/**
 * «Уведомления банка» (#233; макеты 1.2–1.4): выключатель, два шага — доступ к уведомлениям и счёт для Яндекс Банка —
 * и пояснение, что читается и что хранится. Включить можно, только когда оба шага сделаны.
 */
@Composable
fun BankSettingsScreen(state: FinanceState, onIntent: (FinanceIntent) -> Unit, onBack: () -> Unit, onGiveAccess: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val account = state.bankAccount
    val ready = state.bankAccess && account != null
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_BANK_SETTINGS)) {
        ServiceHeader(stringResource(R.string.bank_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.md)) {
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppTheme.sizes.operationRow)
                        .clickable(enabled = ready || state.bank.enabled, role = Role.Switch) { onIntent(FinanceIntent.SetBankEnabled(!state.bankActive)) }
                        .padding(horizontal = spacing.md),
                ) {
                    Text(stringResource(R.string.bank_switch), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.bankActive,
                        onCheckedChange = { onIntent(FinanceIntent.SetBankEnabled(it)) },
                        enabled = ready || state.bank.enabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
                        modifier = Modifier.testTag(TAG_BANK_SWITCH),
                    )
                }
            }
            when {
                state.bank.enabled && !state.bankAccess -> Card(Modifier.padding(top = spacing.sm)) {
                    Row(Modifier.padding(spacing.md).testTag(TAG_BANK_REVOKED)) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.accountAddIcon))
                        Text(stringResource(R.string.bank_revoked), style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.padding(start = spacing.sm))
                    }
                }
                state.bankActive -> WindowText(stringResource(R.string.bank_working), Modifier.padding(horizontal = spacing.xxs, vertical = spacing.xxs))
            }
            Text(
                stringResource(R.string.bank_steps),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = spacing.xxs, top = spacing.md, bottom = spacing.xs),
            )
            Card {
                Step(stringResource(R.string.bank_access_title), stringResource(R.string.bank_access_hint)) {
                    if (state.bankAccess) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag(TAG_BANK_ACCESS_OK)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.accountAddIcon))
                            Text(stringResource(R.string.bank_access_ok), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                    } else {
                        Text(
                            stringResource(R.string.bank_access_give),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onAccent,
                            modifier = Modifier
                                .clip(AppTheme.shapes.medium)
                                .background(colors.accent)
                                .clickable(role = Role.Button, onClick = onGiveAccess)
                                .padding(horizontal = spacing.sm, vertical = spacing.xs)
                                .testTag(TAG_BANK_ACCESS),
                        )
                    }
                }
                HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                Step(stringResource(R.string.bank_account_title), stringResource(R.string.bank_account_hint)) {
                    var menu by remember { mutableStateOf(false) }
                    val text = account?.let { "${it.emojiShown} ${it.name}" } ?: stringResource(R.string.bank_account_pick)
                    Box {
                        PickValue(text, TAG_BANK_ACCOUNT, text) { menu = true }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
                            state.accounts.orEmpty().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.emojiShown}  ${option.name}", color = colors.textPrimary, fontWeight = if (option.id == account?.id) FontWeight.Bold else FontWeight.SemiBold) },
                                    onClick = {
                                        onIntent(FinanceIntent.SetBankAccount(option.id))
                                        menu = false
                                    },
                                    modifier = Modifier.testTag("$TAG_BANK_ACCOUNT:${option.id}"),
                                )
                            }
                        }
                    }
                }
            }
            WindowText(stringResource(R.string.bank_privacy), Modifier.padding(horizontal = spacing.xxs, vertical = spacing.xs))
        }
    }
}

@Composable
private fun Step(title: String, hint: String, value: @Composable () -> Unit) {
    val spacing = AppTheme.spacing
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).padding(horizontal = spacing.md, vertical = spacing.xs)) {
        Column(Modifier.weight(1f).padding(end = spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
        }
        value()
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(AppTheme.colors.surface)) { content() }
}

/** Заголовок служебного экрана: стрелка назад и название (как у корзины финансов). */
@Composable
internal fun ServiceHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = AppTheme.spacing.sm)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Строка «Не разобрано · N» над списком операций (макет 2.2); показывается, только когда есть неразобранные покупки. */
@Composable
fun UnsortedRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = AppTheme.sizes.operationRow)
            .padding(horizontal = spacing.md)
            .testTag(TAG_UNSORTED_ROW),
    ) {
        Icon(painterResource(R.drawable.ic_bank_notifications), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.accountAddIcon))
        Text(stringResource(R.string.bank_unsorted), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f).padding(start = spacing.sm))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onAccent,
            modifier = Modifier.clip(AppTheme.shapes.medium).background(colors.accent).padding(horizontal = spacing.xs, vertical = spacing.xxs),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inactive, modifier = Modifier.padding(start = spacing.xs).size(AppTheme.sizes.accountAddIcon))
    }
}

@Composable
fun UnsortedRoute(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UnsortedScreen(state.pending, state.today, viewModel::onIntent, onBack, onEdit = { id ->
        viewModel.onIntent(FinanceIntent.EditPurchase(id))
        onBack()
    })
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * «Не разобрано» (#233; макет 2.3): покупки без ответа — сумма, счёт, день и время, пометка дубля и три действия.
 * Сами не исчезают; «Изменить» возвращает к финансам с окном нового расхода.
 */
@Composable
fun UnsortedScreen(pending: List<PendingPurchase>, today: LocalDate, onIntent: (FinanceIntent) -> Unit, onBack: () -> Unit, onEdit: (String) -> Unit, zone: ZoneId = ZoneId.systemDefault()) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_UNSORTED)) {
        ServiceHeader(stringResource(R.string.bank_unsorted), onBack)
        if (pending.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl)) {
                Text(stringResource(R.string.bank_unsorted_empty), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center)
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.xs, bottom = spacing.lg)) {
            item(key = "list") {
                Card {
                    pending.forEachIndexed { i, purchase ->
                        PurchaseItem(purchase, today, zone, onIntent, onEdit)
                        if (i != pending.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
                    }
                }
            }
            item(key = "note") { WindowText(stringResource(R.string.bank_unsorted_note), Modifier.padding(horizontal = spacing.xxs, vertical = spacing.xs)) }
        }
    }
}

@Composable
private fun PurchaseItem(purchase: PendingPurchase, today: LocalDate, zone: ZoneId, onIntent: (FinanceIntent) -> Unit, onEdit: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().padding(spacing.md).testTag("$TAG_PURCHASE:${purchase.id}")) {
        // Та же плашка, что в «Операциях» (#245): «?» и «Без категории», серая строка — банк, счёт и время, справа сумма.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            UnknownMark()
            Column(Modifier.weight(1f).padding(horizontal = spacing.xs)) {
                Text(stringResource(R.string.category_none), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    stringResource(R.string.bank_purchase_caption, accountLabel(purchase.accountEmoji, purchase.accountName), shortDay(purchase.date, today), purchase.postedAt.atZone(zone).format(TIME)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("− ${Money.format(purchase.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
        }
        purchase.duplicate?.let { dup ->
            Text(
                stringResource(R.string.bank_duplicate, dup.categoryName ?: stringResource(R.string.category_none), Money.format(dup.amount), shortDay(dup.date, today)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xxs).testTag("$TAG_PURCHASE_DUPLICATE:${purchase.id}"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.padding(top = spacing.sm)) {
            Box(Modifier.weight(1f)) { SecondaryAction(stringResource(R.string.bank_skip), "$TAG_PURCHASE_SKIP:${purchase.id}", { onIntent(FinanceIntent.SkipPurchase(purchase.id)) }) }
            Box(Modifier.weight(1f)) { SecondaryAction(stringResource(R.string.bank_edit), "$TAG_PURCHASE_EDIT:${purchase.id}", { onEdit(purchase.id) }) }
            Box(Modifier.weight(1f)) { PrimaryAction(stringResource(R.string.bank_record), "$TAG_PURCHASE_RECORD:${purchase.id}", { onIntent(FinanceIntent.RecordPurchase(purchase.id)) }) }
        }
    }
}

const val TAG_BANK_SETTINGS = "bank_settings"
const val TAG_BANK_SWITCH = "bank_switch"
const val TAG_BANK_REVOKED = "bank_revoked"
const val TAG_BANK_ACCESS = "bank_access"
const val TAG_BANK_ACCESS_OK = "bank_access_ok"
const val TAG_BANK_ACCOUNT = "bank_account"
const val TAG_MENU_BANK = "finance_menu_bank"
const val TAG_UNSORTED_ROW = "unsorted_row"
const val TAG_UNSORTED = "unsorted"
const val TAG_PURCHASE = "purchase"
const val TAG_PURCHASE_DUPLICATE = "purchase_duplicate"
const val TAG_PURCHASE_SKIP = "purchase_skip"
const val TAG_PURCHASE_EDIT = "purchase_edit"
const val TAG_PURCHASE_RECORD = "purchase_record"
