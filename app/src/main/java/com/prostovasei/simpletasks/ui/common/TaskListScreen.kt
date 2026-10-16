package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.theme.CardPosition

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
) {
    if (!isLoading && tasks.isEmpty()) {
        EmptyState(emptyText, modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        itemsIndexed(tasks, key = { _, it -> it.id }) { index, item ->
            TaskItemRow(
                item = item,
                onToggle = { onIntent(TaskListIntent.ToggleComplete(item.id, item.isRecurring)) },
                showList = showList,
                onClick = { onOpenTask(item.id) },
                position = CardPosition.of(index, tasks.size),
            )
        }
    }
}
