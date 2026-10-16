package com.prostovasei.simpletasks.ui.feature.completed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.TaskRow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.showUndo
import kotlinx.coroutines.launch

@Composable
fun CompletedRoute(onBack: () -> Unit, onOpenTask: (String) -> Unit, viewModel: CompletedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = stringResource(R.string.completed_restored)
    val action = stringResource(R.string.task_undo)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CompletedEffect.Restored -> launch { snackbar.showUndo(message, action) { viewModel.onIntent(CompletedIntent.Undo) } }
            }
        }
    }
    CompletedScreen(state, viewModel::onIntent, onBack, onOpenTask, snackbar)
}

/** «Выполненные»: по дням, новые сверху; галочка снята — задача вернулась в свой список. */
@Composable
fun CompletedScreen(
    state: CompletedState,
    onIntent: (CompletedIntent) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    val inbox = stringResource(R.string.inbox_list_name)
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary)
                }
                Text(
                    text = stringResource(R.string.completed_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            when {
                state.isLoading -> Unit
                state.days.isEmpty() -> EmptyState(stringResource(R.string.completed_empty))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    state.days.forEach { day ->
                        item(key = "day-${days.day(day.day)}") {
                            Text(
                                text = days.day(day.day).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = spacing.md, top = spacing.md, bottom = spacing.xxs),
                            )
                        }
                        items(day.tasks, key = { it.id }) { task ->
                            val list = if (task.isInboxList) inbox else task.listName
                            TaskRow(
                                title = task.title,
                                priority = Priority.NONE,
                                meta = listOfNotNull(list, task.time).joinToString(" · "),
                                metaColor = null,
                                listColor = parseListColor(task.listColor),
                                isCompleting = true,
                                onToggle = { onIntent(CompletedIntent.Restore(task.id)) },
                                onClick = { onOpenTask(task.id) },
                                testTag = "completed:${task.id}",
                            )
                        }
                    }
                }
            }
        }
    }
}
