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
    /** [done] — задача уже выполнена: нажатие возвращает её (итерация 27). */
    data class ToggleComplete(val taskId: String, val recurring: Boolean = false, val done: Boolean = false) : TaskListIntent
    data class Undo(val taskId: String) : TaskListIntent
}

interface TaskListEffect {
    /** Snackbar «Задача выполнена · Отмена»; у повторяющейся — «Следующий раз: [next] · Отмена». */
    data class ShowUndo(val taskId: String, val next: DueText? = null) : TaskListEffect
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
    val rescheduled = stringResource(R.string.task_rescheduled)
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TaskListEffect.ShowUndo -> launch {
                    val text = effect.next?.let { rescheduled.format(days.format(it)) } ?: message
                    snackbar.showUndo(text, action) { viewModel.onIntent(TaskListIntent.Undo(effect.taskId)) }
                }
                else -> onOtherEffect(effect)
            }
        }
    }
}
