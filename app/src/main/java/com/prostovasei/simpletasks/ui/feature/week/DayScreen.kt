package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.rememberNowMinute
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.widget.WidgetDates

@Composable
fun DayRoute(onBack: () -> Unit, onOpenTask: (String) -> Unit, viewModel: DayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    DayScreen(state, viewModel::onIntent, onBack, snackbar, onOpenTask, nowMinute = rememberNowMinute())
}

/**
 * Полный список дня (итерация 44): открывается нажатием на строку даты в ячейке недели. Вложенный экран с «Назад»: подпись дня,
 * число задач и лента как на «Все» — «весь день», потом по времени, кружок отмечает, плашка открывает задачу.
 */
@Composable
fun DayScreen(
    state: DayState,
    onIntent: (TaskListIntent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTask: (String) -> Unit = {},
    nowMinute: Int? = null,
) {
    val colors = AppTheme.colors
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    val date = state.date
    val today = state.today
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { UndoSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().testTag("day-screen")) {
            Row(Modifier.fillMaxWidth().padding(end = AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.project_back), tint = colors.textPrimary)
                }
                if (date != null && today != null) {
                    val relative = when (val label = DueFormatter.dayLabel(date, today)) {
                        is DayLabel.Date -> null
                        else -> days.day(label)
                    }
                    Text(
                        WidgetDates.header(date),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (date < today) colors.overdue else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("day-title"),
                    )
                    relative?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .padding(start = AppTheme.spacing.xs)
                                .background(colors.surface, AppTheme.shapes.small)
                                .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.sizes.hairline),
                        )
                    }
                }
                Text(
                    state.items.size.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f).padding(start = AppTheme.spacing.xs).testTag("day-count"),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (!state.isLoading && state.items.isEmpty()) {
                EmptyState(stringResource(R.string.day_empty))
            } else if (date != null && today != null) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = AppTheme.spacing.xs)) {
                    timelineItems(
                        items = state.items,
                        keyPrefix = "day-$date",
                        days = days,
                        nowMinute = nowMinute.takeIf { date == today },
                        isOverdue = { date < today && !it.showsDone },
                        showList = true,
                        onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
                        onOpen = { onOpenTask(it.taskId) },
                    )
                }
            }
        }
    }
}
