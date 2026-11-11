package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun OperationKind.label(): Int = when (this) {
    OperationKind.EXPENSE -> R.string.operation_expense
    OperationKind.INCOME -> R.string.operation_income
    OperationKind.TRANSFER -> R.string.operation_transfer
}

private val FIELD_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** «10.11.2026 · сегодня» / «09.11.2026 · вчера» / «01.11.2026». */
@Composable
fun dateLabel(date: LocalDate, today: LocalDate): String {
    val text = date.format(FIELD_DATE)
    return when (date) {
        today -> "$text · ${stringResource(R.string.finance_today)}"
        today.minusDays(1) -> "$text · ${stringResource(R.string.finance_yesterday)}"
        else -> text
    }
}

/**
 * Окно операции (этап 2, #225; макеты design/approved/217, экраны 1.2–1.6, 2.3): по центру экрана — тип, крупная сумма, счёт,
 * категория (до пяти кнопок и «Все категории»), место с подсказками, комментарий, дата. У перевода — «Со счёта» и «На счёт».
 * Правка — то же окно без переключателя типа, с корзиной в заголовке. Поля живут в [FinanceState.form], не в окне: черновик
 * не теряется при закрытии.
 */
@Composable
fun OperationDialog(state: FinanceState, form: OperationForm, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val accounts = state.accounts.orEmpty()
    val editing = form.editingId != null
    val transfer = form.kind == OperationKind.TRANSFER
    val edit: (OperationForm.() -> OperationForm) -> Unit = { onIntent(FinanceIntent.EditForm(it)) }
    var allCategories by rememberSaveable { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var placeFocused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    if (!editing) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    FinanceWindow(
        title = stringResource(if (editing) form.kind.label() else R.string.operation_new_title),
        tag = TAG_OPERATION_WINDOW,
        onDismiss = { onIntent(FinanceIntent.CloseForm) },
        headerAction = if (editing) {
            {
                val label = stringResource(R.string.operation_trash)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppTheme.sizes.financeClose)
                        .clip(AppTheme.shapes.large)
                        .clickable(role = Role.Button) { onIntent(FinanceIntent.TrashOperation) }
                        .semantics { contentDescription = label }
                        .testTag(TAG_OPERATION_TRASH),
                ) { Icon(painterResource(R.drawable.ic_tool_delete), contentDescription = null, tint = colors.textSecondary) }
            }
        } else {
            null
        },
        actions = { PrimaryAction(stringResource(R.string.action_save), TAG_OPERATION_SAVE, onClick = { onIntent(FinanceIntent.SaveForm) }, enabled = form.canSave) },
    ) {
        if (!editing) {
            Segments(
                options = OperationKind.entries.map { it to stringResource(it.label()) },
                selected = form.kind,
                tag = TAG_OPERATION_KIND,
                onSelect = { onIntent(FinanceIntent.SetKind(it)) },
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
        AmountField(form.amount, onChange = { text -> edit { copy(amount = text) } }, tag = TAG_OPERATION_AMOUNT, modifier = Modifier.focusRequester(focus))
        AccountField(stringResource(if (transfer) R.string.operation_from else R.string.operation_account), accounts, form.accountId, TAG_OPERATION_ACCOUNT) { id ->
            edit { copy(accountId = id) }
        }
        if (transfer) {
            AccountField(stringResource(R.string.operation_to), accounts.filter { it.id != form.accountId }, form.toAccountId, TAG_OPERATION_TO) { id -> edit { copy(toAccountId = id) } }
        } else {
            CategoryChips(
                chips = state.categoryChips(form),
                selectedId = form.categoryId,
                onSelect = { id -> edit { copy(categoryId = id) } },
                onAll = { allCategories = true },
                onCreate = { newCategory = true },
            )
            FinanceField(stringResource(R.string.operation_place)) {
                FieldInput(form.place, { text -> edit { copy(place = text) } }, stringResource(R.string.operation_optional), TAG_OPERATION_PLACE, Modifier.onFocusChanged { placeFocused = it.isFocused })
            }
            if (placeFocused) PlaceHints(state.hints.places, form.place) { place -> edit { copy(place = place) } }
        }
        FinanceField(stringResource(R.string.operation_comment)) {
            FieldInput(form.comment, { text -> edit { copy(comment = text) } }, stringResource(R.string.operation_optional), TAG_OPERATION_COMMENT)
        }
        FinanceField(stringResource(R.string.operation_date)) {
            val label = dateLabel(form.date, state.today)
            PickValue(label, TAG_OPERATION_DATE, "${stringResource(R.string.operation_date)}: $label") { datePicker = true }
        }
    }

    if (allCategories) {
        CategoryPicker(
            kind = form.kind,
            categories = state.categories.filter { it.kind == form.kind && (!it.archived || it.id == form.categoryId) },
            selectedId = form.categoryId,
            onSelect = { id ->
                edit { copy(categoryId = id) }
                allCategories = false
            },
            onCreate = {
                allCategories = false
                newCategory = true
            },
            onDismiss = { allCategories = false },
        )
    }
    if (newCategory) {
        CategoryNameDialog(
            title = stringResource(if (form.kind == OperationKind.INCOME) R.string.category_new_income else R.string.category_new_expense),
            initialName = "",
            saveLabel = stringResource(R.string.action_create),
            onSave = { name ->
                onIntent(FinanceIntent.CreateCategory(name, form.kind, select = true))
                newCategory = false
            },
            onDismiss = { newCategory = false },
        )
    }
    if (datePicker) {
        DateDialog(form.date, onPick = { date ->
            edit { copy(date = date) }
            datePicker = false
        }, onDismiss = { datePicker = false })
    }
}

/** Строка выбора счёта: эмодзи и название, по нажатию — список счетов. Счёт один — выбирать не из чего, стрелка всё равно открывает список. */
@Composable
private fun AccountField(label: String, accounts: List<Account>, selectedId: String?, tag: String, onSelect: (String) -> Unit) {
    val colors = AppTheme.colors
    var menu by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val text = selected?.let { "${it.emojiShown} ${it.name}" } ?: stringResource(if (accounts.isEmpty()) R.string.operation_no_second_account else R.string.operation_pick_account)
    FinanceField(label) {
        Box {
            PickValue(text, tag, "$label: $text") { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.emojiShown}  ${account.name}", color = colors.textPrimary, fontWeight = if (account.id == selectedId) FontWeight.Bold else FontWeight.SemiBold) },
                        onClick = {
                            onSelect(account.id)
                            menu = false
                        },
                        modifier = Modifier.testTag("$tag:${account.id}"),
                    )
                }
            }
        }
    }
}

/**
 * Категории окна: подпись и кнопки по три в ряд — до пяти категорий и «Все категории». Нажатие на выбранную снимает выбор
 * (операция «Без категории»). Категорий нет — одна кнопка «Создать категорию» и пояснение.
 */
@Composable
private fun CategoryChips(chips: List<Category>, selectedId: String?, onSelect: (String?) -> Unit, onAll: () -> Unit, onCreate: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Text(stringResource(R.string.operation_category), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs))
    if (chips.isEmpty()) {
        Chip(stringResource(R.string.category_create), false, TAG_OPERATION_CATEGORY_CREATE, Modifier.fillMaxWidth(), soft = true, onClick = onCreate)
        Text(stringResource(R.string.category_none_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.xs))
        return
    }
    val cells: List<Category?> = chips + listOf(null)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs), modifier = Modifier.padding(bottom = spacing.xs).testTag(TAG_OPERATION_CATEGORIES)) {
        cells.chunked(CHIPS_IN_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                row.forEach { category ->
                    if (category == null) {
                        Chip(stringResource(R.string.category_all), false, TAG_OPERATION_CATEGORY_ALL, Modifier.weight(1f), soft = true, onClick = onAll)
                    } else {
                        val active = category.id == selectedId
                        Chip(category.name, active, "$TAG_OPERATION_CATEGORY:${category.id}", Modifier.weight(1f)) { onSelect(if (active) null else category.id) }
                    }
                }
                repeat(CHIPS_IN_ROW - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, tag: String, modifier: Modifier = Modifier, soft: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AppTheme.sizes.financeChip)
            .clip(AppTheme.shapes.medium)
            .background(if (active) colors.accent else if (soft) colors.selected else colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(horizontal = AppTheme.spacing.xxs)
            .testTag(tag),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color = if (active) colors.onAccent else if (soft) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Подсказки места под полем, пока оно в фокусе: ранее введённые места, подходящие под набранное (без точного совпадения). */
@Composable
private fun PlaceHints(places: List<String>, typed: String, onPick: (String) -> Unit) {
    val query = typed.trim()
    val hints = places.filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }.take(PLACE_HINTS)
    if (hints.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = AppTheme.spacing.xxs).testTag(TAG_OPERATION_PLACE_HINTS),
    ) {
        hints.forEach { place -> Chip(place, false, "$TAG_OPERATION_PLACE_HINTS:$place", Modifier.padding(horizontal = AppTheme.spacing.xxs)) { onPick(place) } }
    }
}

/** Календарь выбора дня операции. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(date: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
                modifier = Modifier.testTag(TAG_OPERATION_DATE_DONE),
            ) { Text(stringResource(R.string.sheet_done), color = colors.textPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) } },
    ) { DatePicker(picker, showModeToggle = false) }
}

private const val CHIPS_IN_ROW = 3
private const val PLACE_HINTS = 5

const val TAG_OPERATION_WINDOW = "operation_window"
const val TAG_OPERATION_KIND = "operation_kind"
const val TAG_OPERATION_AMOUNT = "operation_amount"
const val TAG_OPERATION_ACCOUNT = "operation_account"
const val TAG_OPERATION_TO = "operation_to"
const val TAG_OPERATION_CATEGORIES = "operation_categories"
const val TAG_OPERATION_CATEGORY = "operation_category"
const val TAG_OPERATION_CATEGORY_ALL = "operation_category_all"
const val TAG_OPERATION_CATEGORY_CREATE = "operation_category_create"
const val TAG_OPERATION_PLACE = "operation_place"
const val TAG_OPERATION_PLACE_HINTS = "operation_place_hints"
const val TAG_OPERATION_COMMENT = "operation_comment"
const val TAG_OPERATION_DATE = "operation_date"
const val TAG_OPERATION_DATE_DONE = "operation_date_done"
const val TAG_OPERATION_SAVE = "operation_save"
const val TAG_OPERATION_TRASH = "operation_trash"
