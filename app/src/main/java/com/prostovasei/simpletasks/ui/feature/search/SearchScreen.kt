package com.prostovasei.simpletasks.ui.feature.search

import androidx.compose.foundation.background
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
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.dueLines
import com.prostovasei.simpletasks.ui.common.listSquareColor

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
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
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
            state.query.isBlank() -> EmptyState(stringResource(R.string.search_start))
            state.isLoading -> Unit
            state.found == 0 -> EmptyState(stringResource(R.string.search_nothing))
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.sizes.cardGap),
            ) {
                item(key = "found") {
                    SectionHeader(pluralStringResource(R.plurals.search_found, state.found, state.found), Modifier.animateItem())
                }
                items(state.active, key = { it.item.id }) { ResultRow(it, state.query, onIntent, onOpenTask, Modifier.animateItem()) }
                if (state.done.isNotEmpty()) {
                    item(key = "done") { SectionHeader(stringResource(R.string.search_done), Modifier.animateItem()) }
                    items(state.done, key = { "done:${it.item.id}" }) { ResultRow(it, state.query, onIntent, onOpenTask, Modifier.animateItem()) }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, query: String, onIntent: (SearchIntent) -> Unit, onOpenTask: (String) -> Unit, modifier: Modifier) {
    val item = result.item
    TaskRow(
        title = item.title,
        priority = item.priority,
        listColor = listSquareColor(item),
        listIcon = ListIcons.drawable(item.listIcon),
        isCompleting = result.isDone,
        animateCompletion = false,
        isRecurring = item.isRecurring && !result.isDone,
        trailing = if (result.isDone) emptyList() else dueLines(item),
        onToggle = { onIntent(SearchIntent.Toggle(item.id, result.isDone)) },
        onClick = { onOpenTask(item.id) },
        highlight = query,
        subtitle = result.match,
        testTag = "search:${item.id}",
        modifier = modifier,
    )
}

const val TAG_SEARCH_FIELD = "search_field"
const val TAG_SEARCH_CLEAR = "search_clear"
