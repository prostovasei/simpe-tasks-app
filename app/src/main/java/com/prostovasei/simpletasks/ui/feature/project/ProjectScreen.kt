package com.prostovasei.simpletasks.ui.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ColorDot
import com.prostovasei.simpletasks.designsystem.components.ColorPickerDialog
import com.prostovasei.simpletasks.designsystem.components.ConfirmDeleteDialog
import com.prostovasei.simpletasks.designsystem.components.NameDialog
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.designsystem.tokens.ListColorTokens
import com.prostovasei.simpletasks.ui.common.AddTaskButton
import com.prostovasei.simpletasks.ui.common.FlatTaskList
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent

@Composable
fun ProjectRoute(
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar) { effect -> if (effect == ProjectEffect.Close) onBack() }
    ProjectScreen(state, viewModel::onIntent, onBack, snackbar, onAddTask, onOpenTask)
}

@Composable
fun ProjectScreen(
    state: ProjectState,
    onIntent: (TaskListIntent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddTask: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val inboxName = stringResource(R.string.inbox_list_name)
    val title = if (state.isInbox) inboxName else state.name
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { AddTaskButton(onAddTask) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = AppTheme.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary)
                }
                parseListColor(state.color)?.let { ColorDot(it, Modifier.padding(end = AppTheme.spacing.xs)) }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!state.isInbox) ProjectMenu(onIntent)
            }
            FlatTaskList(
                tasks = state.tasks,
                isLoading = state.isLoading,
                emptyText = stringResource(R.string.project_empty),
                onIntent = onIntent,
                showList = false,
                onOpenTask = onOpenTask,
            )
        }
    }
    ProjectDialogs(state, onIntent)
}

@Composable
private fun ProjectMenu(onIntent: (TaskListIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(TAG_PROJECT_MENU)) {
        Icon(Icons.Default.MoreVert, stringResource(R.string.project_menu), tint = AppTheme.colors.textPrimary)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = AppTheme.colors.surface) {
        listOf(
            R.string.project_rename to ProjectDialog.Rename,
            R.string.project_color to ProjectDialog.Color,
            R.string.project_delete to ProjectDialog.Delete,
        ).forEach { (label, dialog) ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(label),
                        color = if (dialog == ProjectDialog.Delete) AppTheme.colors.danger else AppTheme.colors.textPrimary,
                    )
                },
                onClick = {
                    expanded = false
                    onIntent(ProjectIntent.OpenDialog(dialog))
                },
            )
        }
    }
}

@Composable
private fun ProjectDialogs(state: ProjectState, onIntent: (TaskListIntent) -> Unit) {
    val dismiss = { onIntent(ProjectIntent.DismissDialog) }
    when (state.dialog) {
        ProjectDialog.None -> Unit
        ProjectDialog.Rename -> NameDialog(
            title = stringResource(R.string.project_rename_title),
            initialName = state.name,
            confirmLabel = stringResource(R.string.action_save),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onIntent(ProjectIntent.Rename(it)) },
            onDismiss = dismiss,
        )
        ProjectDialog.Color -> ColorPickerDialog(
            title = stringResource(R.string.project_color_title),
            presets = ListColorTokens.presets.mapNotNull { hex -> parseListColor(hex)?.let { hex to it } },
            selected = state.color,
            noColorLabel = stringResource(R.string.project_no_color),
            cancelLabel = stringResource(R.string.action_cancel),
            onSelect = { onIntent(ProjectIntent.SetColor(it)) },
            onDismiss = dismiss,
        )
        ProjectDialog.Delete -> ConfirmDeleteDialog(
            title = stringResource(R.string.project_delete_title, state.name),
            text = stringResource(R.string.project_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onIntent(ProjectIntent.ConfirmDelete) },
            onDismiss = dismiss,
        )
    }
}

const val TAG_PROJECT_MENU = "project_menu"
