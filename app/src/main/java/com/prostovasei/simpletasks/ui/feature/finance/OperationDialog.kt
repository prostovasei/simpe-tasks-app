package com.prostovasei.simpletasks.ui.feature.finance

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    OperationKind.CORRECTION -> R.string.operation_correction
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

private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

/** Дата в окне операции (#237): «ср. 15 окт 2026». */
fun shortDate(date: LocalDate): String = "${WEEKDAYS[date.dayOfWeek.value - 1]}. ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]} ${date.year}"

/**
 * Окно операции (этап 2, #225; редизайн #237, вариант «Плотный» — design/approved/237, решение PM 13.11.2026): без заголовка и «×».
 * Сверху тип (при правке — нет), в строке сумма и счёт, категории иконками-эмодзи, в одной строке дата и место («Место платежа» серым,
 * по нажатию — выбор), комментарий. Внизу значки: корзина (только при правке), назад (закрыть, черновик остаётся) и «✓ сохранить».
 * У перевода — «Со счёта» и «На счёт», без категории и места. Поля живут в [FinanceState.form], не в окне: черновик не теряется.
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
    var placePicker by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    if (!editing) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    OperationWindow(
        tag = TAG_OPERATION_WINDOW,
        onDismiss = { onIntent(FinanceIntent.CloseForm) },
        bar = {
            IconBar(
                onBack = { onIntent(FinanceIntent.CloseForm) },
                onSave = { onIntent(FinanceIntent.SaveForm) },
                saveEnabled = form.canSave,
                backTag = TAG_OPERATION_BACK,
                saveTag = TAG_OPERATION_SAVE,
                onTrash = if (editing) ({ onIntent(FinanceIntent.TrashOperation) }) else null,
            )
        },
    ) {
        if (!editing) {
            Segments(
                options = OperationKind.MANUAL.map { it to stringResource(it.label()) },
                selected = form.kind,
                tag = TAG_OPERATION_KIND,
                onSelect = { onIntent(FinanceIntent.SetKind(it)) },
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
        if (transfer) {
            AmountField(form.amount, onChange = { text -> edit { copy(amount = text) } }, tag = TAG_OPERATION_AMOUNT, modifier = Modifier.focusRequester(focus))
            AccountField(stringResource(R.string.operation_from), accounts, form.accountId, TAG_OPERATION_ACCOUNT) { id -> edit { copy(accountId = id) } }
            AccountField(stringResource(R.string.operation_to), accounts.filter { it.id != form.accountId }, form.toAccountId, TAG_OPERATION_TO) { id -> edit { copy(toAccountId = id) } }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmountField(form.amount, onChange = { text -> edit { copy(amount = text) } }, tag = TAG_OPERATION_AMOUNT, modifier = Modifier.weight(1f).focusRequester(focus), align = TextAlign.Start)
                AccountPill(accounts, form.accountId, TAG_OPERATION_ACCOUNT) { id -> edit { copy(accountId = id) } }
            }
            CategoryTiles(
                chips = state.categoryChips(form),
                selected = state.categories.firstOrNull { it.id == form.categoryId },
                onSelect = { id -> edit { copy(categoryId = id) } },
                onAll = { allCategories = true },
                onCreate = { newCategory = true },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth().padding(top = spacing.xs, bottom = spacing.xxs)) {
            val dateText = shortDate(form.date)
            val caption = when (form.date) {
                state.today -> stringResource(R.string.finance_today)
                state.today.minusDays(1) -> stringResource(R.string.finance_yesterday)
                else -> null
            }
            PairBox(Modifier.weight(1f), TAG_OPERATION_DATE, "${stringResource(R.string.operation_date)}: $dateText", onClick = { datePicker = true }) {
                Text(dateText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (caption != null) Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            if (!transfer) {
                val hint = stringResource(R.string.operation_place_hint)
                PairBox(Modifier.weight(1f), TAG_OPERATION_PLACE, "${stringResource(R.string.operation_place)}: ${form.place.ifBlank { hint }}", onClick = { placePicker = true }) {
                    Text(
                        form.place.ifBlank { hint },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (form.place.isBlank()) FontWeight.Normal else FontWeight.Bold,
                        color = if (form.place.isBlank()) colors.inactive else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        FinanceField(stringResource(R.string.operation_comment)) {
            FieldInput(form.comment, { text -> edit { copy(comment = text) } }, stringResource(R.string.operation_optional), TAG_OPERATION_COMMENT)
        }
    }

    if (placePicker) {
        PlaceDialog(
            initial = form.place,
            places = state.hints.places,
            onApply = { place ->
                edit { copy(place = place) }
                placePicker = false
            },
            onDismiss = { placePicker = false },
        )
    }
    if (allCategories) {
        CategoryPicker(
            kind = form.kind,
            categories = state.categories.filter { it.kind == form.kind && (!it.archived || it.id == form.categoryId) }.sortedByName { it.name },
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
            onSave = { name, emoji ->
                onIntent(FinanceIntent.CreateCategory(name, form.kind, select = true, emoji = emoji))
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

/** Окно по центру без заголовка и «×» (#237): содержимое прокручивается, значки [bar] внизу всегда видны над клавиатурой. */
@Composable
private fun OperationWindow(tag: String, onDismiss: () -> Unit, bar: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = spacing.md, vertical = spacing.sm)
                .widthIn(max = AppTheme.sizes.dialogMaxWidth)
                .fillMaxWidth()
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                .testTag(tag),
        ) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = spacing.md), content = content)
            bar()
        }
    }
}

/**
 * Нижняя строка окна из значков (#237): [onTrash] (корзина — только при правке, отдельно от остальных), назад и широкое «✓ сохранить».
 * «Сохранить» недоступно, пока [saveEnabled] = false.
 */
@Composable
private fun IconBar(onBack: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean, backTag: String, saveTag: String, onTrash: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm)) {
        if (onTrash != null) {
            val label = stringResource(R.string.operation_trash)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = sizes.financeBarSide, height = sizes.financeAction)
                    .clip(AppTheme.shapes.large)
                    .background(colors.selected)
                    .clickable(role = Role.Button, onClick = onTrash)
                    .semantics { contentDescription = label }
                    .testTag(TAG_OPERATION_TRASH),
            ) { Icon(painterResource(R.drawable.ic_tool_delete), contentDescription = null, tint = colors.textSecondary) }
        }
        val back = stringResource(R.string.project_back)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = sizes.financeBarSide, height = sizes.financeAction)
                .clip(AppTheme.shapes.large)
                .background(colors.selected)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = back }
                .testTag(backTag),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.textPrimary) }
        val save = stringResource(R.string.action_save)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = sizes.financeAction)
                .clip(AppTheme.shapes.large)
                .background(if (saveEnabled) colors.accent else colors.divider)
                .clickable(enabled = saveEnabled, role = Role.Button, onClick = onSave)
                .semantics { contentDescription = save }
                .testTag(saveTag),
        ) { Icon(Icons.Default.Check, contentDescription = null, tint = if (saveEnabled) colors.onAccent else colors.textSecondary) }
    }
}

/** Серая плитка в строке «дата · место»: нажимается целиком, внутри — название поля не нужно, значение само говорит за себя. */
@Composable
private fun PairBox(modifier: Modifier, tag: String, description: String, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = AppTheme.sizes.financeField)
            .clip(AppTheme.shapes.large)
            .background(AppTheme.colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
            .testTag(tag),
        content = content,
    )
}

/** Строка выбора счёта: эмодзи и название, по нажатию — список счетов. Счёт один — выбирать не из чего, стрелка всё равно открывает список. */
@Composable
private fun AccountField(label: String, accounts: List<Account>, selectedId: String?, tag: String, onSelect: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val text = selected?.let { "${it.emojiShown} ${it.name}" } ?: stringResource(if (accounts.isEmpty()) R.string.operation_no_second_account else R.string.operation_pick_account)
    FinanceField(label) {
        Box {
            PickValue(text, tag, "$label: $text") { menu = true }
            AccountMenu(menu, { menu = false }, accounts, selectedId, tag, onSelect)
        }
    }
}

/** Счёт рядом с суммой (#237): таблетка «💳 Карта ⌄» и список счетов по нажатию. */
@Composable
private fun AccountPill(accounts: List<Account>, selectedId: String, tag: String, onSelect: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var menu by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val text = selected?.let { "${it.emojiShown} ${it.name}" } ?: stringResource(R.string.operation_pick_account)
    val description = "${stringResource(R.string.operation_account)}: $text"
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(AppTheme.shapes.medium)
                .background(colors.selected)
                .clickable(role = Role.Button) { menu = true }
                .semantics { contentDescription = description }
                .padding(start = spacing.sm, top = spacing.xs, bottom = spacing.xs, end = spacing.xxs)
                .testTag(tag),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.inactive, modifier = Modifier.size(AppTheme.sizes.accountAddIcon))
        }
        AccountMenu(menu, { menu = false }, accounts, selectedId, tag, onSelect)
    }
}

@Composable
private fun AccountMenu(expanded: Boolean, onDismiss: () -> Unit, accounts: List<Account>, selectedId: String?, tag: String, onSelect: (String) -> Unit) {
    val colors = AppTheme.colors
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = colors.surface) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { Text("${account.emojiShown}  ${account.name}", color = colors.textPrimary, fontWeight = if (account.id == selectedId) FontWeight.Bold else FontWeight.SemiBold) },
                onClick = {
                    onSelect(account.id)
                    onDismiss()
                },
                modifier = Modifier.testTag("$tag:${account.id}"),
            )
        }
    }
}

/**
 * Категории иконками (#237): до пяти плиток-эмодзи и «⋯» (все категории); у категории без эмодзи — плитка с первой буквой. Выбранная — чёрная,
 * под рядом её название («Без категории», пока не выбрана). Нажатие на выбранную снимает выбор. Категорий нет — «Создать категорию» и пояснение.
 */
@Composable
private fun CategoryTiles(chips: List<Category>, selected: Category?, onSelect: (String?) -> Unit, onAll: () -> Unit, onCreate: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    if (chips.isEmpty()) {
        Chip(stringResource(R.string.category_create), false, TAG_OPERATION_CATEGORY_CREATE, Modifier.fillMaxWidth().padding(top = spacing.xs), soft = true, onClick = onCreate)
        Text(stringResource(R.string.category_none_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.xs))
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth().padding(top = spacing.xs).testTag(TAG_OPERATION_CATEGORIES)) {
        chips.forEach { category ->
            val active = category.id == selected?.id
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AppTheme.sizes.financeEmoji)
                    .clip(AppTheme.shapes.large)
                    .background(if (active) colors.accent else colors.selected)
                    .clickable(role = Role.Button) { onSelect(if (active) null else category.id) }
                    .semantics {
                        this.selected = active
                        contentDescription = category.name
                    }
                    .testTag("$TAG_OPERATION_CATEGORY:${category.id}"),
            ) {
                Text(
                    category.emoji ?: category.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (active) colors.onAccent else colors.textSecondary,
                )
            }
        }
        val all = stringResource(R.string.category_all)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = AppTheme.sizes.financeEmoji)
                .clip(AppTheme.shapes.large)
                .background(colors.selected)
                .clickable(role = Role.Button, onClick = onAll)
                .semantics { contentDescription = all }
                .testTag(TAG_OPERATION_CATEGORY_ALL),
        ) { Text("⋯", style = MaterialTheme.typography.titleLarge, color = colors.textSecondary) }
        repeat(MAX_TILES - chips.size - 1) { Box(Modifier.weight(1f)) }
    }
    Text(
        selected?.name ?: stringResource(R.string.category_none_selected),
        style = MaterialTheme.typography.labelSmall,
        color = colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = spacing.xxs, top = spacing.xs).testTag(TAG_OPERATION_CATEGORY_NAME),
    )
}

/**
 * Выбор места (#237): поле ввода, под ним ранее введённые места, подходящие под набранное. Нажатие на место подставляет его сразу,
 * «✓» — набранное, «назад» закрывает без изменений.
 */
@Composable
private fun PlaceDialog(initial: String, places: List<String>, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var text by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val query = text.trim()
    val hints = places.filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }.take(PLACE_HINTS)
    OperationWindow(tag = TAG_PLACE_DIALOG, onDismiss = onDismiss, bar = {
        IconBar(onBack = onDismiss, onSave = { onApply(text.trim()) }, saveEnabled = true, backTag = TAG_PLACE_BACK, saveTag = TAG_PLACE_DONE)
    }) {
        val style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
        BasicTextField(
            value = text,
            onValueChange = { text = it.take(FIELD_MAX) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(TAG_PLACE_INPUT),
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.md).clip(AppTheme.shapes.large).background(colors.background).padding(spacing.sm),
                ) {
                    if (text.isEmpty()) Text(stringResource(R.string.operation_place_hint), style = style.copy(fontWeight = FontWeight.Normal), color = colors.inactive)
                    inner()
                }
            },
        )
        hints.forEach { place ->
            Text(
                place,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onApply(place) }.heightIn(min = AppTheme.sizes.financeField).padding(top = spacing.sm, start = spacing.xxs).testTag("$TAG_PLACE_HINT:$place"),
            )
            HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
        }
        Text(stringResource(R.string.operation_place_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(vertical = spacing.sm))
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

private const val MAX_TILES = 6
private const val PLACE_HINTS = 6

const val TAG_OPERATION_WINDOW = "operation_window"
const val TAG_OPERATION_KIND = "operation_kind"
const val TAG_OPERATION_AMOUNT = "operation_amount"
const val TAG_OPERATION_ACCOUNT = "operation_account"
const val TAG_OPERATION_TO = "operation_to"
const val TAG_OPERATION_CATEGORIES = "operation_categories"
const val TAG_OPERATION_CATEGORY = "operation_category"
const val TAG_OPERATION_CATEGORY_NAME = "operation_category_name"
const val TAG_OPERATION_CATEGORY_ALL = "operation_category_all"
const val TAG_OPERATION_CATEGORY_CREATE = "operation_category_create"
const val TAG_OPERATION_PLACE = "operation_place"
const val TAG_OPERATION_COMMENT = "operation_comment"
const val TAG_OPERATION_DATE = "operation_date"
const val TAG_OPERATION_DATE_DONE = "operation_date_done"
const val TAG_OPERATION_SAVE = "operation_save"
const val TAG_OPERATION_BACK = "operation_back"
const val TAG_OPERATION_TRASH = "operation_trash"
const val TAG_PLACE_DIALOG = "place_dialog"
const val TAG_PLACE_INPUT = "place_input"
const val TAG_PLACE_HINT = "place_hint"
const val TAG_PLACE_DONE = "place_done"
const val TAG_PLACE_BACK = "place_back"
