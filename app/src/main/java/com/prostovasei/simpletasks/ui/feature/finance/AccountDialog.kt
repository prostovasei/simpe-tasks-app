package com.prostovasei.simpletasks.ui.feature.finance

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.ui.feature.project.EmojiPickerDialog

@StringRes
fun AccountType.label(): Int = when (this) {
    AccountType.CASH -> R.string.account_type_cash
    AccountType.CARD -> R.string.account_type_card
    AccountType.SAVINGS -> R.string.account_type_savings
}

/**
 * Окно финансов по центру экрана (решение PM: не нижняя шторка; макеты design/approved/217): заголовок и «×» сверху,
 * содержимое прокручивается, кнопки [actions] внизу всегда видны. «Назад» и нажатие мимо — как «×».
 */
@Composable
fun FinanceWindow(title: String, tag: String, onDismiss: () -> Unit, actions: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = spacing.md, vertical = spacing.lg)
                .widthIn(max = sizes.dialogMaxWidth)
                .fillMaxWidth()
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                .testTag(tag),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = spacing.md, end = spacing.xs, top = spacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                val close = stringResource(R.string.finance_close)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(sizes.financeClose)
                        .clip(AppTheme.shapes.large)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .semantics { contentDescription = close }
                        .testTag("$tag:$TAG_WINDOW_CLOSE"),
                ) { Icon(Icons.Default.Close, contentDescription = null, tint = colors.textSecondary) }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = spacing.md), content = content)
            Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs), content = actions)
        }
    }
}

/** Строка окна: подпись слева, значение справа, тонкая линия снизу. */
@Composable
fun FinanceField(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.financeField)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(end = AppTheme.spacing.sm))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { value() }
    }
    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
}

/** Значение-выбор в строке окна: жирный текст и стрелка «есть варианты». */
@Composable
private fun PickValue(text: String, tag: String, description: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(AppTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = AppTheme.spacing.xs, top = AppTheme.spacing.xs, bottom = AppTheme.spacing.xs)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.inactive, modifier = Modifier.size(AppTheme.sizes.accountAddIcon))
    }
}

/**
 * Окно счёта (экраны 2.2 и 2.5): название, тип, необязательный эмодзи и остаток сейчас. [account] = null — новый счёт.
 * Пустое название заменяется названием типа («Карта»). Этап 1: операций нет, поэтому остаток правится прямо здесь;
 * с этапа 4 его заменит сверка «Остаток не сходится». Удаления и архива счёта нет — решение PM.
 */
@Composable
fun AccountDialog(account: Account?, onSave: (AccountDraft) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var name by rememberSaveable { mutableStateOf(account?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(account?.type ?: AccountType.CARD) }
    var emoji by rememberSaveable { mutableStateOf(account?.emoji) }
    var amount by rememberSaveable { mutableStateOf(account?.let { Money.input(it.balance) }.orEmpty()) }
    var typeMenu by remember { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf(false) }
    val typeLabel = stringResource(type.label())
    val balance = Money.parse(amount)
    val focus = remember { FocusRequester() }
    if (account == null) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    FinanceWindow(
        title = stringResource(if (account == null) R.string.account_new_title else R.string.account_edit_title),
        tag = TAG_ACCOUNT_WINDOW,
        onDismiss = onDismiss,
        actions = {
            PrimaryAction(
                label = stringResource(if (account == null) R.string.finance_create_account else R.string.action_save),
                tag = TAG_ACCOUNT_SAVE,
                enabled = balance != null,
                onClick = { balance?.let { onSave(AccountDraft(name.trim().ifBlank { typeLabel }, type, emoji, it)) } },
            )
        },
    ) {
        FinanceField(stringResource(R.string.account_name)) {
            val style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.End)
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(NAME_MAX) },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().testTag(TAG_ACCOUNT_NAME),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (name.isEmpty()) Text(typeLabel, style = style, color = colors.inactive)
                        inner()
                    }
                },
            )
        }
        FinanceField(stringResource(R.string.account_type)) {
            Box {
                PickValue(typeLabel, TAG_ACCOUNT_TYPE, "${stringResource(R.string.account_type)}: $typeLabel") { typeMenu = true }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }, containerColor = colors.surface) {
                    AccountType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.defaultEmoji}  ${stringResource(option.label())}", color = colors.textPrimary, fontWeight = if (option == type) FontWeight.Bold else FontWeight.SemiBold) },
                            onClick = {
                                type = option
                                typeMenu = false
                            },
                            modifier = Modifier.testTag("$TAG_ACCOUNT_TYPE:${option.name}"),
                        )
                    }
                }
            }
        }
        FinanceField(stringResource(R.string.account_emoji)) {
            val shown = emoji ?: type.defaultEmoji
            PickValue(shown, TAG_ACCOUNT_EMOJI, "${stringResource(R.string.account_emoji)}: $shown") { picker = true }
        }
        Text(
            stringResource(R.string.account_balance),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.md),
        )
        // 0.58.1 (замечание PM): поле во всю ширину, текст по центру, «₽» дописывается к тексту. Раньше ширина поля бралась
        // по «естественному» размеру (IntrinsicSize.Min) — Compose считал её неверно, и сумма при открытии счёта обрезалась до пустоты.
        val style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center)
        BasicTextField(
            value = amount,
            onValueChange = { amount = Money.filterInput(it) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = RubleSuffix,
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm).focusRequester(focus).testTag(TAG_ACCOUNT_AMOUNT),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (amount.isEmpty()) Text("0$RUBLE", style = style, color = colors.inactive, modifier = Modifier.fillMaxWidth())
                    inner()
                }
            },
        )
        if (account == null) {
            Text(stringResource(R.string.account_balance_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
    if (picker) {
        EmojiPickerDialog(emoji ?: type.defaultEmoji, onConfirm = { picked -> emoji = picked.takeIf { it != type.defaultEmoji }; picker = false }, onDismiss = { picker = false })
    }
}

/** Знак рубля после суммы — неразрывный пробел и «₽». */
private const val RUBLE = "\u00A0₽"

/** Показывает «42300 ₽», а в поле хранит только «42300»: курсор не заходит за знак рубля. */
private object RubleSuffix : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = AnnotatedString(text.text + RUBLE)
        return TransformedText(
            shown,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = minOf(offset, text.length)
            },
        )
    }
}

/** Наибольшая длина названия счёта. */
private const val NAME_MAX = 40

const val TAG_ACCOUNT_WINDOW = "account_window"
const val TAG_WINDOW_CLOSE = "close"
const val TAG_ACCOUNT_NAME = "account_name"
const val TAG_ACCOUNT_TYPE = "account_type"
const val TAG_ACCOUNT_EMOJI = "account_emoji"
const val TAG_ACCOUNT_AMOUNT = "account_amount"
const val TAG_ACCOUNT_SAVE = "account_save"
