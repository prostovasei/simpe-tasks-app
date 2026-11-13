package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.ui.feature.project.EmojiPickerDialog

@Composable
fun CategoriesRoute(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesScreen(state.categories, viewModel::onIntent, onBack)
}

/**
 * Экран «Категории» (этап 2, #225; экраны 3.2–3.3): отдельный экран со стрелкой назад. Два списка — расходы и доходы,
 * «+» добавляет в открытый. Нажатие на категорию — окно с названием и «В архив»; архив внизу, стрелка возвращает из архива.
 * Удаления нет: архивная категория остаётся в старых операциях.
 */
@Composable
fun CategoriesScreen(categories: List<Category>, onIntent: (FinanceIntent) -> Unit, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var kind by rememberSaveable { mutableStateOf(OperationKind.EXPENSE) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val (archived, active) = categories.filter { it.kind == kind }.partition { it.archived }
    Column(Modifier.fillMaxSize().background(colors.background).testTag(TAG_CATEGORIES)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = spacing.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary) }
            Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
            val add = stringResource(R.string.category_add)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppTheme.sizes.financeClose)
                    .clip(AppTheme.shapes.large)
                    .background(colors.accent)
                    .clickable(role = Role.Button) { creating = true }
                    .semantics { contentDescription = add }
                    .testTag(TAG_CATEGORY_ADD),
            ) { Icon(Icons.Default.Add, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(AppTheme.sizes.accountAddIcon)) }
        }
        Segments(
            options = listOf(OperationKind.EXPENSE to stringResource(R.string.categories_expenses), OperationKind.INCOME to stringResource(R.string.categories_incomes)),
            selected = kind,
            tag = TAG_CATEGORY_KIND,
            onSelect = { kind = it },
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = spacing.md, end = spacing.md, bottom = spacing.lg)) {
            if (active.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(R.string.categories_empty), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(spacing.md))
                }
            } else {
                item(key = "active") { Card { active.forEachIndexed { i, category -> CategoryRow(category, last = i == active.lastIndex) { editingId = category.id } } } }
            }
            if (archived.isNotEmpty()) {
                item(key = "archive-title") {
                    Text(stringResource(R.string.categories_archive), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(start = spacing.xxs, top = spacing.md, bottom = spacing.xs))
                }
                item(key = "archive") {
                    Card { archived.forEachIndexed { i, category -> CategoryRow(category, last = i == archived.lastIndex) { onIntent(FinanceIntent.ArchiveCategory(category.id, archived = false)) } } }
                }
            }
            item(key = "note") {
                Text(stringResource(R.string.categories_note), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = spacing.xxs, vertical = spacing.sm))
            }
        }
    }
    if (creating) {
        CategoryNameDialog(
            title = stringResource(if (kind == OperationKind.INCOME) R.string.category_new_income else R.string.category_new_expense),
            initialName = "",
            saveLabel = stringResource(R.string.action_create),
            onSave = { name, emoji ->
                onIntent(FinanceIntent.CreateCategory(name, kind, emoji = emoji))
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    categories.firstOrNull { it.id == editingId }?.let { category ->
        CategoryNameDialog(
            title = stringResource(if (category.kind == OperationKind.INCOME) R.string.category_income else R.string.category_expense),
            initialName = category.name,
            initialEmoji = category.emoji,
            saveLabel = stringResource(R.string.action_save),
            onSave = { name, emoji ->
                onIntent(FinanceIntent.UpdateCategory(category.id, name, emoji))
                editingId = null
            },
            onArchive = {
                onIntent(FinanceIntent.ArchiveCategory(category.id, archived = true))
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(AppTheme.colors.surface)) { content() }
}

/** Строка категории: действующая — стрелка «открыть», архивная — серое название и значок «вернуть». */
@Composable
private fun CategoryRow(category: Category, last: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val restore = stringResource(R.string.category_restore)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.operationRow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md)
            .testTag("$TAG_CATEGORY:${category.id}"),
    ) {
        CategoryMark(category.emoji, category.name)
        Text(
            category.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (category.archived) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = spacing.sm),
        )
        if (category.archived) {
            Icon(painterResource(R.drawable.ic_snack_restore), contentDescription = restore, tint = colors.textSecondary, modifier = Modifier.size(AppTheme.sizes.moduleIcon))
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inactive)
        }
    }
    if (!last) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
}

/** Значок категории (#237): эмодзи или, если его нет, серая плитка с первой буквой названия. */
@Composable
fun CategoryMark(emoji: String?, name: String) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(AppTheme.sizes.financeMark).clip(AppTheme.shapes.medium).background(if (emoji == null) colors.selected else Color.Transparent),
    ) {
        Text(emoji ?: name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (emoji == null) colors.textSecondary else colors.textPrimary)
    }
}

/**
 * Окно категории: новая (только «Создать») или правка («В архив» и «Сохранить»). Сверху — эмодзи (#237, необязательный): нажатие открывает выбор,
 * «Убрать» снимает значок; ниже название.
 */
@Composable
fun CategoryNameDialog(
    title: String,
    initialName: String,
    saveLabel: String,
    onSave: (name: String, emoji: String?) -> Unit,
    onDismiss: () -> Unit,
    onArchive: (() -> Unit)? = null,
    initialEmoji: String? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var emoji by rememberSaveable { mutableStateOf(initialEmoji) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    if (initialName.isEmpty()) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    FinanceWindow(
        title = title,
        tag = TAG_CATEGORY_WINDOW,
        onDismiss = onDismiss,
        actions = {
            if (onArchive != null) SecondaryAction(stringResource(R.string.category_archive), TAG_CATEGORY_ARCHIVE, onArchive)
            PrimaryAction(saveLabel, TAG_CATEGORY_SAVE, onClick = { onSave(name.trim(), emoji) }, enabled = name.isNotBlank())
        },
    ) {
        FinanceField(stringResource(R.string.category_emoji)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (emoji != null) {
                    Text(
                        stringResource(R.string.category_emoji_remove),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.clip(AppTheme.shapes.medium).clickable(role = Role.Button) { emoji = null }.padding(AppTheme.spacing.xs).testTag(TAG_CATEGORY_EMOJI_CLEAR),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AppTheme.sizes.financeEmoji)
                        .clip(AppTheme.shapes.large)
                        .background(AppTheme.colors.selected)
                        .clickable(role = Role.Button) { picking = true }
                        .testTag(TAG_CATEGORY_EMOJI),
                ) {
                    Text(emoji ?: "＋", style = MaterialTheme.typography.titleLarge, color = if (emoji == null) AppTheme.colors.textSecondary else AppTheme.colors.textPrimary)
                }
            }
        }
        FinanceField(stringResource(R.string.account_name)) {
            FieldInput(name, { name = it }, stringResource(R.string.category_name_hint), TAG_CATEGORY_NAME, Modifier.focusRequester(focus), bold = true, maxLength = CATEGORY_NAME_MAX)
        }
    }
    if (picking) {
        EmojiPickerDialog(
            selected = emoji.orEmpty(),
            onConfirm = {
                emoji = it.trim().takeIf { value -> value.isNotEmpty() }
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** «Все категории» из окна операции (экран 3.4): полный список, «Без категории» и создание новой, не выходя из операции. */
@Composable
fun CategoryPicker(kind: OperationKind, categories: List<Category>, selectedId: String?, onSelect: (String?) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(
        title = stringResource(if (kind == OperationKind.INCOME) R.string.categories_of_income else R.string.categories_of_expense),
        tag = TAG_CATEGORY_PICKER,
        onDismiss = onDismiss,
        actions = { SecondaryAction(stringResource(R.string.category_new), TAG_CATEGORY_PICKER_NEW, onCreate, plus = true) },
    ) {
        PickRow(stringResource(R.string.category_none), selectedId == null, "$TAG_CATEGORY_PICKER:none") { onSelect(null) }
        categories.forEach { category -> PickRow(category.name, category.id == selectedId, "$TAG_CATEGORY_PICKER:${category.id}", mark = { CategoryMark(category.emoji, category.name) }) { onSelect(category.id) } }
    }
}

@Composable
private fun PickRow(label: String, selected: Boolean, tag: String, mark: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.financeField).clickable(role = Role.RadioButton, onClick = onClick).testTag(tag),
    ) {
        mark?.let {
            it()
            Box(Modifier.size(AppTheme.spacing.sm))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(AppTheme.sizes.moduleIcon))
    }
    HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
}

private const val CATEGORY_NAME_MAX = 30

const val TAG_CATEGORIES = "finance_categories"
const val TAG_CATEGORY = "category"
const val TAG_CATEGORY_ADD = "category_add"
const val TAG_CATEGORY_KIND = "category_kind"
const val TAG_CATEGORY_WINDOW = "category_window"
const val TAG_CATEGORY_NAME = "category_name"
const val TAG_CATEGORY_SAVE = "category_save"
const val TAG_CATEGORY_EMOJI = "category_emoji"
const val TAG_CATEGORY_EMOJI_CLEAR = "category_emoji_clear"
const val TAG_CATEGORY_ARCHIVE = "category_archive"
const val TAG_CATEGORY_PICKER = "category_picker"
const val TAG_CATEGORY_PICKER_NEW = "category_picker_new"
