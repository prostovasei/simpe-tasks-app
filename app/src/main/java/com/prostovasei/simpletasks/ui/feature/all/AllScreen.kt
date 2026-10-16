package com.prostovasei.simpletasks.ui.feature.all

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.BlockHeader
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.ScreenTitle
import com.prostovasei.simpletasks.designsystem.components.SectionHeader
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.CardPosition
import com.prostovasei.simpletasks.ui.common.AddTaskButton
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskItemRow
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.TaskListEffects

@Composable
fun AllRoute(onAddTask: () -> Unit, onOpenTask: (String) -> Unit, viewModel: AllViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    AllScreen(state, viewModel::onIntent, snackbar, onAddTask, onOpenTask)
}

@Composable
fun AllScreen(
    state: AllState,
    onIntent: (TaskListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddTask: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { AddTaskButton(onAddTask) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle(stringResource(R.string.all_title))
            if (!state.isLoading && state.isEmpty) {
                EmptyState(stringResource(R.string.all_empty))
                return@Column
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.overdue.isNotEmpty()) {
                    item(key = "h-overdue") { SectionHeader(stringResource(R.string.all_overdue), color = colors.overdue) }
                    tasks(state.overdue, onIntent, onOpenTask, showToday = true, isOverdue = true)
                }
                if (state.todayAllDay.isNotEmpty() || state.todayTimed.isNotEmpty()) {
                    item(key = "h-today") { SectionHeader(stringResource(R.string.all_today)) }
                    if (state.todayAllDay.isNotEmpty()) {
                        item(key = "h-all-day") { BlockHeader(stringResource(R.string.all_today_all_day)) }
                        tasks(state.todayAllDay, onIntent, onOpenTask, showToday = false)
                    }
                    if (state.todayTimed.isNotEmpty()) {
                        item(key = "h-timed") { BlockHeader(stringResource(R.string.all_today_timed)) }
                        tasks(state.todayTimed, onIntent, onOpenTask, showToday = false)
                    }
                }
                if (state.upcoming.isNotEmpty()) {
                    item(key = "h-upcoming") { SectionHeader(stringResource(R.string.all_upcoming)) }
                    tasks(state.upcoming, onIntent, onOpenTask, showToday = true)
                }
            }
        }
    }
}

private fun LazyListScope.tasks(
    items: List<TaskItem>,
    onIntent: (TaskListIntent) -> Unit,
    onOpenTask: (String) -> Unit,
    showToday: Boolean,
    isOverdue: Boolean = false,
) {
    itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
        TaskItemRow(
            item = item,
            onToggle = { onIntent(TaskListIntent.ToggleComplete(item.id, item.isRecurring)) },
            showToday = showToday,
            isOverdue = isOverdue,
            onClick = { onOpenTask(item.taskId) },
            position = CardPosition.of(index, items.size),
        )
    }
}
