package com.prostovasei.simpletasks.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Плавающая кнопка «+» — быстрое создание задачи (SPEC.md §4.4). */
@Composable
fun AddTaskButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = AppTheme.colors.accent,
        contentColor = AppTheme.colors.onAccent,
        modifier = Modifier.testTag(TAG_ADD_TASK),
    ) { Icon(Icons.Default.Add, stringResource(R.string.add_task)) }
}

const val TAG_ADD_TASK = "add_task"
