package com.prostovasei.simpletasks.ui.feature.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ScreenTitle
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.AddTaskButton
import com.prostovasei.simpletasks.ui.common.FlatTaskList
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent

@Composable
fun InboxRoute(onAddTask: () -> Unit, onOpenTask: (String) -> Unit, viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    InboxScreen(state, viewModel::onIntent, snackbar, onAddTask, onOpenTask)
}

@Composable
fun InboxScreen(
    state: InboxState,
    onIntent: (TaskListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddTask: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { AddTaskButton(onAddTask) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle(stringResource(R.string.inbox_title))
            FlatTaskList(
                tasks = state.tasks,
                isLoading = state.isLoading,
                emptyText = stringResource(R.string.inbox_empty),
                onIntent = onIntent,
                showList = true,
                onOpenTask = onOpenTask,
            )
        }
    }
}
