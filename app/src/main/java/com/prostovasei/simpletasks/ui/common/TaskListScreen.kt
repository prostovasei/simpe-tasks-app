package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prostovasei.simpletasks.designsystem.components.EmptyState

/** Плоский список задач с пустым состоянием — для «Входящих» и экрана списка. */
@Composable
fun FlatTaskList(
    tasks: List<TaskItem>,
    isLoading: Boolean,
    emptyText: String,
    onIntent: (TaskListIntent) -> Unit,
    showList: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isLoading && tasks.isEmpty()) {
        EmptyState(emptyText, modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        items(tasks, key = { it.id }) { item ->
            TaskItemRow(item = item, onToggle = { onIntent(TaskListIntent.ToggleComplete(item.id)) }, showList = showList)
        }
    }
}
