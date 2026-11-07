package com.prostovasei.simpletasks.ui.feature.sorted

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.DayHeader
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.AddTaskButton
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.rememberNowMinute
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.widget.WidgetDates

/** Какой из двух разделов (итерация 52). */
enum class SortedKind { Someday, NoList }

@Composable
fun SomedayRoute(onBack: () -> Unit, onOpenTask: (String) -> Unit, onAddTask: () -> Unit, viewModel: SomedayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    SortedScreen(SortedKind.Someday, state, viewModel::onIntent, onBack, onOpenTask, onAddTask, snackbar)
}

@Composable
fun NoListRoute(onBack: () -> Unit, onOpenTask: (String) -> Unit, onAddTask: () -> Unit, viewModel: NoListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    SortedScreen(SortedKind.NoList, state, viewModel::onIntent, onBack, onOpenTask, onAddTask, snackbar, nowMinute = rememberNowMinute())
}

/**
 * «Когда-нибудь» и «Без списка» (итерация 52, макет iteration52.py): «назад», заголовок и число открытых задач; ниже группы —
 * у «Когда-нибудь» по спискам (эмодзи, название, число), у «Без списка» по дням (подпись дня, как на «Все»). Кружок отмечает,
 * плашка открывает задачу, «+» — новая задача: в «Когда-нибудь» без даты со списком, в «Без списка» — на сегодня.
 */
@Composable
fun SortedScreen(
    kind: SortedKind,
    state: SortedState,
    onIntent: (TaskListIntent) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    nowMinute: Int? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { UndoSnackbarHost(snackbarHostState) },
        floatingActionButton = { AddTaskButton(onAddTask) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag(if (kind == SortedKind.Someday) TAG_SOMEDAY_SCREEN else TAG_NO_LIST_SCREEN)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = spacing.md)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary)
                }
                Text(
                    text = stringResource(if (kind == SortedKind.Someday) R.string.someday_title else R.string.no_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (state.openCount > 0) {
                    Text(
                        state.openCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.testTag(TAG_SORTED_COUNT),
                    )
                }
            }
            when {
                state.isLoading -> Unit
                state.groups.isEmpty() -> EmptyState(stringResource(if (kind == SortedKind.Someday) R.string.someday_empty else R.string.no_list_empty))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    // Снизу место под «+», чтобы он не закрывал последнюю задачу.
                    contentPadding = PaddingValues(bottom = AppTheme.sizes.navBar + spacing.lg),
                    verticalArrangement = Arrangement.Top,
                ) {
                    state.groups.forEach { group ->
                        item(key = "head:${group.key}") {
                            val list = group.list
                            val date = group.date
                            if (list != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.xxs)
                                        .testTag("$TAG_SORTED_GROUP:${list.id}"),
                                ) {
                                    ListBadge(list.emojiShown)
                                    Text(
                                        list.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(start = spacing.xs),
                                    )
                                    Text(
                                        group.tasks.count { !it.showsDone }.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(start = spacing.xs),
                                    )
                                }
                            } else if (date != null) {
                                val relative = when (val label = DueFormatter.dayLabel(date, state.today)) {
                                    is DayLabel.Date -> null
                                    else -> days.day(label)
                                }
                                DayHeader(WidgetDates.header(date), relative, isOverdue = date < state.today)
                            }
                        }
                        timelineItems(
                            items = group.tasks,
                            keyPrefix = group.key,
                            days = days,
                            nowMinute = nowMinute.takeIf { group.date == state.today },
                            isOverdue = { group.date != null && group.date < state.today && !it.showsDone },
                            showList = true,
                            onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
                            onOpen = { onOpenTask(it.taskId) },
                        )
                    }
                }
            }
        }
    }
}

const val TAG_SOMEDAY_SCREEN = "someday_screen"
const val TAG_NO_LIST_SCREEN = "no_list_screen"
const val TAG_SORTED_COUNT = "sorted_count"
const val TAG_SORTED_GROUP = "sorted_group"
