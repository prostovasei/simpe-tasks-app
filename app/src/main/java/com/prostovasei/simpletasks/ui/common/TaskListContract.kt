package com.prostovasei.simpletasks.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import kotlinx.coroutines.launch

/** Действия со списком задач — общие для «Все», «Входящих» и экрана списка. */
interface TaskListIntent {
    data class ToggleComplete(val taskId: String) : TaskListIntent
    data class Undo(val taskId: String) : TaskListIntent
}

interface TaskListEffect {
    /** Показать Snackbar «Задача выполнена · Отмена». */
    data class ShowUndo(val taskId: String) : TaskListEffect
}

/**
 * Единый сборщик эффектов экрана со списком задач: «Задача выполнена · Отмена» обрабатывается здесь,
 * остальные эффекты передаются в [onOtherEffect]. Эффекты читаются одним сборщиком — иначе часть потеряется.
 */
@Composable
fun TaskListEffects(
    viewModel: MviViewModel<*, TaskListIntent, TaskListEffect>,
    snackbar: SnackbarHostState,
    onOtherEffect: (TaskListEffect) -> Unit = {},
) {
    val message = stringResource(R.string.task_completed)
    val action = stringResource(R.string.task_undo)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TaskListEffect.ShowUndo -> launch {
                    snackbar.showUndo(message, action) { viewModel.onIntent(TaskListIntent.Undo(effect.taskId)) }
                }
                else -> onOtherEffect(effect)
            }
        }
    }
}
