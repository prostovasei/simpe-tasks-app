package com.prostovasei.simpletasks.ui.feature.trash

import com.prostovasei.simpletasks.ui.feature.bulk.selectionBottomInset
import com.prostovasei.simpletasks.ui.feature.bulk.rememberSelection
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionScope
import com.prostovasei.simpletasks.ui.feature.bulk.Selectable
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionFrame
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog
import com.prostovasei.simpletasks.widget.WidgetDates

@Composable
fun TrashRoute(onBack: () -> Unit, onOpenTask: (String) -> Unit, viewModel: TrashViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrashScreen(state, viewModel::onIntent, onBack, onOpenTask)
}

/**
 * «Корзина» (итерация 39, бэклог №35): удалённые задачи как в обычном списке, новые сверху, с подписью «удалена 28 окт».
 * Нажатие открывает задачу (без правки); сверху «Восстановить все» и «Удалить все» (последнее — после подтверждения).
 */
@Composable
fun TrashScreen(state: TrashState, onIntent: (TrashIntent) -> Unit, onBack: () -> Unit, onOpenTask: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    // Итерация 40: долгое нажатие на плашку — выбор; лента: «Восстановить» и «Удалить навсегда».
    val selection = rememberSelection()
    Scaffold(containerColor = colors.background, contentWindowInsets = WindowInsets(0)) { padding ->
        SelectionFrame(selection, state.items.map { Selectable(it.id, isDone = false) }, Modifier.padding(padding), scope = SelectionScope.Trash) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary)
                }
                Text(stringResource(R.string.trash_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            when {
                state.isLoading -> Unit
                state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(horizontal = spacing.xl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag(TAG_TRASH_EMPTY)) {
                        Text(stringResource(R.string.trash_empty), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Text(
                            stringResource(R.string.trash_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = spacing.xxs),
                        )
                    }
                }
                else -> {
                    Row(Modifier.fillMaxWidth().padding(start = spacing.md, end = spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.trash_count, state.items.size).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onIntent(TrashIntent.RestoreAll) }, modifier = Modifier.testTag(TAG_TRASH_RESTORE_ALL)) {
                            Text(stringResource(R.string.trash_restore_all), color = colors.accent, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { confirmDeleteAll = true }, modifier = Modifier.testTag(TAG_TRASH_DELETE_ALL)) {
                            Text(stringResource(R.string.trash_delete_all), color = colors.danger, fontWeight = FontWeight.Bold)
                        }
                    }
                    val deletedText = state.items.map { item ->
                        val on = state.deletedOn[item.id]
                        if (on == null) item else item.copy(subtitle = stringResource(R.string.trash_deleted_on, WidgetDates.short(on)))
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = spacing.xs, bottom = selectionBottomInset(selection)), verticalArrangement = Arrangement.Top) {
                        timelineItems(
                            items = deletedText,
                            keyPrefix = "trash",
                            days = days,
                            nowMinute = null,
                            isOverdue = { false },
                            showList = true,
                            // Кружок в корзине ничего не отмечает: задача сначала возвращается.
                            onToggle = {},
                            onOpen = { onOpenTask(it.taskId) },
                            dayLabels = true,
                            selection = selection.takeIf { it.active }?.ids,
                            onSelect = { selection.toggle(it.id) },
                        )
                    }
                }
            }
        }
        }
    }
    if (confirmDeleteAll) {
        SheetDialog(
            title = stringResource(R.string.trash_delete_all_title),
            tag = TAG_TRASH_DELETE_ALL_DIALOG,
            onDismiss = { confirmDeleteAll = false },
            onDone = {
                confirmDeleteAll = false
                onIntent(TrashIntent.DeleteAll)
            },
            doneLabel = R.string.trash_delete_all_confirm,
            danger = true,
            subtitle = stringResource(R.string.trash_delete_all_text, state.items.size),
        ) {}
    }
}

const val TAG_TRASH = "lists_trash"
const val TAG_TRASH_EMPTY = "trash_empty"
const val TAG_TRASH_RESTORE_ALL = "trash_restore_all"
const val TAG_TRASH_DELETE_ALL = "trash_delete_all"
const val TAG_TRASH_DELETE_ALL_DIALOG = "trash_delete_all_dialog"
