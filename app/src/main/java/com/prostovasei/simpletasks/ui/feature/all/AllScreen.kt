package com.prostovasei.simpletasks.ui.feature.all

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.prostovasei.simpletasks.designsystem.components.DayHeader
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItemRow
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.widget.WidgetDates

@Composable
fun AllRoute(onOpenTask: (String) -> Unit, viewModel: AllViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    AllScreen(state, viewModel::onIntent, snackbar, onOpenTask)
}

/** Лента «Все» по дням (итерация 13, макет PM): без заголовка экрана, «+» — в нижней панели. */
@Composable
fun AllScreen(
    state: AllState,
    onIntent: (TaskListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTask: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!state.isLoading && state.isEmpty) {
            EmptyState(stringResource(R.string.all_empty), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.sizes.cardGap),
        ) {
            state.dayGroups().forEach { group ->
                item(key = "day-${group.date}") {
                    DayHeader(
                        text = WidgetDates.header(group.date),
                        relative = group.relative?.let(days::day),
                        isOverdue = group.isOverdue,
                    )
                }
                items(group.items, key = { it.id }) { item ->
                    TaskItemRow(
                        item = item,
                        onToggle = { onIntent(TaskListIntent.ToggleComplete(item.id, item.isRecurring)) },
                        showDay = false,
                        isOverdue = item.id in group.overdueIds,
                        onClick = { onOpenTask(item.taskId) },
                    )
                }
            }
        }
    }
}
