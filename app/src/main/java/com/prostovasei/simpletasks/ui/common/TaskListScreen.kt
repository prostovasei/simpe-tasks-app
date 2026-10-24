package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prostovasei.simpletasks.designsystem.components.EmptyTasks
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Плоский список задач с пустым состоянием — для «Входящих» и экрана списка. */
@Composable
fun FlatTaskList(
    tasks: List<TaskItem>,
    isLoading: Boolean,
    emptyText: String,
    onIntent: (TaskListIntent) -> Unit,
    showList: Boolean,
    modifier: Modifier = Modifier,
    onOpenTask: (String) -> Unit = {},
    emptyArrowLift: Dp = AppTheme.spacing.xxs,
) {
    if (!isLoading && tasks.isEmpty()) {
        EmptyTasks(emptyText, stringResource(R.string.empty_hint), modifier, emptyArrowLift)
        return
    }
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    // Итерация 26: как в виджете; слева день и время — подписей дней в плоском списке нет.
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppTheme.spacing.xs),
    ) {
        timelineItems(
            items = tasks,
            keyPrefix = "flat",
            days = days,
            nowMinute = null,
            isOverdue = { false },
            showList = showList,
            onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
            onOpen = { onOpenTask(it.taskId) },
            dayLabels = true,
        )
    }
}
