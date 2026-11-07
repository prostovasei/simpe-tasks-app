package com.prostovasei.simpletasks.ui.feature.search

import com.prostovasei.simpletasks.ui.feature.bulk.selectionBottomInset
import com.prostovasei.simpletasks.ui.feature.bulk.rememberSelection
import com.prostovasei.simpletasks.ui.feature.bulk.Selectable
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionState
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionFrame
import androidx.compose.foundation.background
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.SectionHeader
import com.prostovasei.simpletasks.designsystem.components.TaskRow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.dueLines

@Composable
fun SearchRoute(onOpenTask: (String) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, viewModel::onIntent, onOpenTask)
}

/**
 * Раздел «Поиск» (итерация 15, решение PM): при открытии курсор сразу в поле и поднята клавиатура,
 * результаты появляются при вводе, найденное подсвечено. Выполненные — ниже, серые; их можно вернуть.
 */
@Composable
fun SearchScreen(state: SearchState, onIntent: (SearchIntent) -> Unit, onOpenTask: (String) -> Unit = {}, autoFocus: Boolean = true) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // Итерация 40: долгое нажатие на найденную задачу — режим выбора.
    val selection = rememberSelection()
    SelectionFrame(selection, (state.active + state.done).map { Selectable(it.item.id, it.isDone, it.item.isRecurring, it.item.due != null) }) {
    Column(Modifier.fillMaxSize().background(colors.background)) {
        TextField(
            value = state.query,
            onValueChange = { onIntent(SearchIntent.Query(it)) },
            placeholder = { Text(stringResource(R.string.search_hint), color = colors.textSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onIntent(SearchIntent.Query("")) }, modifier = Modifier.testTag(TAG_SEARCH_CLEAR)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear), tint = colors.textSecondary)
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onIntent(SearchIntent.Submit)
                keyboard?.hide()
            }),
            shape = AppTheme.shapes.large,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedIndicatorColor = colors.surface,
                unfocusedIndicatorColor = colors.surface,
                cursorColor = colors.accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm)
                .focusRequester(focus)
                .testTag(TAG_SEARCH_FIELD),
        )
        when {
            state.query.isBlank() && state.recents.isNotEmpty() -> RecentList(state.recents, onIntent)
            state.query.isBlank() -> EmptyState(stringResource(R.string.search_start))
            state.isLoading -> Unit
            state.found == 0 -> EmptyState(stringResource(R.string.search_nothing))
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = selectionBottomInset(selection)),
                verticalArrangement = Arrangement.spacedBy(AppTheme.sizes.cardGap),
            ) {
                item(key = "found") {
                    SectionHeader(pluralStringResource(R.plurals.search_found, state.found, state.found), Modifier.animateItem())
                }
                items(state.active, key = { it.item.id }) { ResultRow(it, state.query, onIntent, openAndRemember(onIntent, onOpenTask), Modifier.animateItem(), selection) }
                if (state.done.isNotEmpty()) {
                    item(key = "done") { SectionHeader(stringResource(R.string.search_done), Modifier.animateItem()) }
                    items(state.done, key = { "done:${it.item.id}" }) { ResultRow(it, state.query, onIntent, openAndRemember(onIntent, onOpenTask), Modifier.animateItem(), selection) }
                }
            }
        }
    }
    }
}

/** Открытие найденной задачи запоминает запрос в недавних (итерация 38). */
private fun openAndRemember(onIntent: (SearchIntent) -> Unit, onOpenTask: (String) -> Unit): (String) -> Unit = { id ->
    onIntent(SearchIntent.Submit)
    onOpenTask(id)
}

/** «Недавние»: пока поле пустое — последние запросы; нажатие ищет, крестик убирает один, «Очистить» — все (итерация 38). */
@Composable
private fun RecentList(recents: List<String>, onIntent: (SearchIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    LazyColumn(Modifier.fillMaxSize().testTag(TAG_SEARCH_RECENTS)) {
        item(key = "recent-head") {
            Row(Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.md, top = spacing.md, bottom = spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.search_recent).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.search_recent_clear),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    modifier = Modifier.clickable { onIntent(SearchIntent.ClearRecents) }.padding(vertical = spacing.xs).testTag(TAG_SEARCH_RECENT_CLEAR),
                )
            }
        }
        items(recents, key = { "recent:$it" }) { query ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SizeTokens.RECENT_ROW.dp)
                    .clickable { onIntent(SearchIntent.PickRecent(query)) }
                    .padding(start = spacing.md)
                    .testTag("search_recent:$query"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_history), null, tint = colors.textSecondary, modifier = Modifier.size(SizeTokens.RECENT_ICON.dp))
                Text(query, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.textPrimary, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = spacing.md))
                IconButton(onClick = { onIntent(SearchIntent.RemoveRecent(query)) }, modifier = Modifier.size(SizeTokens.RECENT_CLEAR_TOUCH.dp).testTag("search_recent_remove:$query")) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_recent_remove, query), tint = colors.textSecondary, modifier = Modifier.size(AppTheme.sizes.metaIcon))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, query: String, onIntent: (SearchIntent) -> Unit, onOpenTask: (String) -> Unit, modifier: Modifier, selection: SelectionState) {
    val selecting = selection.active
    val item = result.item
    TaskRow(
        title = item.title,
        priority = item.priority,
        listEmoji = item.listEmoji,
        isCompleting = result.isDone,
        animateCompletion = false,
        isRecurring = item.isRecurring && !result.isDone,
        reminder = item.reminder.takeIf { !result.isDone },
        trailing = if (result.isDone) emptyList() else dueLines(item),
        onToggle = { if (selecting) selection.toggle(item.id) else onIntent(SearchIntent.Toggle(item.id, result.isDone)) },
        onClick = { if (selecting) selection.toggle(item.id) else onOpenTask(item.id) },
        selected = if (selecting) item.id in selection.ids else null,
        onLongClick = if (selecting) null else { { selection.toggle(item.id) } },
        highlight = query,
        subtitle = result.match,
        testTag = "search:${item.id}",
        modifier = modifier,
    )
}

const val TAG_SEARCH_FIELD = "search_field"
const val TAG_SEARCH_CLEAR = "search_clear"
const val TAG_SEARCH_RECENTS = "search_recents"
const val TAG_SEARCH_RECENT_CLEAR = "search_recent_clear"
