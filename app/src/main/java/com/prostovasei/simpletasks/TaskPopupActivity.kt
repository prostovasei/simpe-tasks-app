package com.prostovasei.simpletasks

import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.prostovasei.simpletasks.ui.feature.task.TaskDeletionsHost
import com.prostovasei.simpletasks.ui.common.showUndo
import com.prostovasei.simpletasks.ui.feature.all.OverdueMovesHost
import com.prostovasei.simpletasks.ui.navigation.MoveOverdueDestination
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.task.LocalSheetOrigin
import com.prostovasei.simpletasks.ui.feature.task.QuickAddRoute
import com.prostovasei.simpletasks.ui.feature.task.TaskDetailRoute
import com.prostovasei.simpletasks.ui.navigation.QuickAddDestination
import com.prostovasei.simpletasks.ui.navigation.VoiceDestination
import com.prostovasei.simpletasks.ui.feature.voice.VoiceRoute
import com.prostovasei.simpletasks.ui.navigation.DayDestination
import com.prostovasei.simpletasks.ui.feature.week.DaySheetRoute
import com.prostovasei.simpletasks.ui.navigation.TaskDetailDestination
import com.prostovasei.simpletasks.ui.navigation.SnoozeDestination
import com.prostovasei.simpletasks.ui.feature.snooze.SnoozeRoute
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueFormatter
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.prostovasei.simpletasks.ui.feature.voice.VoiceTasksSaver
import com.prostovasei.simpletasks.ui.feature.voice.VoiceListModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Задача, создание задачи или шторка дня (итерация 51) поверх рабочего стола (решение PM): открывается из виджета и уведомлений,
 * само приложение не запускается, «Назад» сразу возвращает туда, откуда открыли.
 * Отдельная задача Android (taskAffinity), в списке недавних не показывается.
 */
@AndroidEntryPoint
class TaskPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val start = when {
            intent.getBooleanExtra(MainActivity.EXTRA_MOVE_OVERDUE, false) -> MoveOverdueDestination
            // #95: микрофон в виджете — окно голосового ввода, затем окно новой задачи с текстом.
            intent.getBooleanExtra(MainActivity.EXTRA_VOICE, false) -> VoiceDestination(today = intent.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, false))
            else -> MainActivity.routeFrom(intent)
        }
        if (start == null) {
            finish()
            return
        }
        setContent {
            AppTheme {
                // Р1: окно растёт из плашки или «+», на которые нажали в виджете (рабочий стол передаёт их место).
                TaskPopup(start, onDismiss = ::finish, origin = intent.sourceBounds)
            }
        }
    }
}

/**
 * Окно поверх рабочего стола: задача и новая задача — та же шторка с сеткой, что и в приложении (итерация 19).
 * Нажатие на затемнение или «Назад» закрывает окно.
 */
@Composable
fun TaskPopup(start: Any, onDismiss: () -> Unit, origin: android.graphics.Rect? = null, navController: NavHostController = rememberNavController()) {
    // Задачу удалили (итерация 36) или перенесли просроченные (итерация 37): шторка закрылась, окно остаётся прозрачным на время
    // подсказки «Задача удалена · Отмена», нажатие мимо подсказки закрывает окно сразу (отмена тогда уже невозможна).
    val deletions = hiltViewModel<TaskDeletionsHost>().deletions
    val moves = hiltViewModel<OverdueMovesHost>().moves
    var offer by remember { mutableStateOf<UndoOffer?>(null) }
    val deletedText = stringResource(R.string.task_deleted)
    val movedText = stringResource(R.string.overdue_moved)
    LaunchedEffect(deletions) { deletions.events.collect { deletion -> offer = UndoOffer(deletedText, UndoKind.Deleted) { deletions.undo(deletion) } } }
    val undoMessages = com.prostovasei.simpletasks.ui.feature.task.UndoMessages.load()
    LaunchedEffect(deletions) { deletions.notices.collect { notice -> offer = UndoOffer(undoMessages.text(notice.message), notice.kind) { deletions.undo(notice) } } }
    LaunchedEffect(moves) {
        moves.events.collect { moved ->
            if (moved.count == 0) onDismiss() else offer = UndoOffer(movedText.format(moved.count), UndoKind.Moved) { moves.undo(moved) }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    // #310: сохранение продиктованных задач и место окна голоса, из которого вырастает окно задачи.
    val voiceSaver = hiltViewModel<VoiceTasksSaver>()
    var voiceOrigin by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val popupScope = rememberCoroutineScope()
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    val addedText: (Int) -> String = { n -> resources.getQuantityString(R.plurals.voice_added, n, n) }
    val undoText = stringResource(R.string.task_undo)
    LaunchedEffect(offer) {
        val shown = offer ?: return@LaunchedEffect
        snackbar.showUndo(shown.text, undoText, shown.kind, shown.undo)
        onDismiss()
    }
    val close: () -> Unit = { if (offer == null && !navController.popBackStack()) onDismiss() }
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    val today = java.time.LocalDate.now()
    val zone = java.time.ZoneId.systemDefault()
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            // Растёт из виджета только первое окно; подзадача, открытая из него, выезжает снизу.
            composable<TaskDetailDestination> { entry ->
                val openDate = entry.toRoute<TaskDetailDestination>().openDate
                CompositionLocalProvider(LocalSheetOrigin provides origin.takeIf { navController.previousBackStackEntry == null }) {
                    TaskDetailRoute(onClose = close, onOpenTask = { navController.navigate(TaskDetailDestination(it)) }, openDate = openDate)
                }
            }
            // Итерация 53: «Отложить» из уведомления — окно выбора; «Выбрать дату и время…» — задача сразу с окном даты.
            composable<SnoozeDestination> { entry ->
                val taskId = entry.toRoute<SnoozeDestination>().taskId
                SnoozeRoute(
                    taskId = taskId,
                    onDismiss = onDismiss,
                    onSnoozed = { text, undo -> offer = UndoOffer(text, UndoKind.Tomorrow, undo) },
                    onPickDate = {
                        navController.navigate(TaskDetailDestination(taskId, openDate = true)) { popUpTo<SnoozeDestination> { inclusive = true } }
                    },
                    subtitle = { task -> listOfNotNull(task.title, DueFormatter.format(task.due, task.start, today, zone)?.let(days::format)).joinToString(" · ") },
                )
            }
            composable<MoveOverdueDestination> {
                // Перенос запускается один раз: при повороте экрана окно не переносит заново.
                var started by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!started) {
                        started = true
                        moves.run()
                    }
                }
            }
            // Итерация 51: нажатие на дату дня в «Неделе» виджета — шторка дня поверх рабочего стола, без приложения.
            composable<DayDestination> {
                DaySheetRoute(
                    onClose = close,
                    onOpenTask = { navController.navigate(TaskDetailDestination(it)) },
                    onAdd = { navController.navigate(QuickAddDestination(date = it.toString())) },
                )
            }
            composable<VoiceDestination> { entry ->
                val today = entry.toRoute<VoiceDestination>().today
                // #310: несколько задач голосом. Нажатие на задачу — окно задачи вырастает из окна голоса; галочка — все задачи
                // сохраняются, окно закрывается, подсказка «Добавлено N задач · Отмена».
                VoiceRoute(
                    onClose = onDismiss,
                    today = today,
                    smartDates = voiceSaver.smartDates,
                    onEdit = { task, from ->
                        voiceOrigin = from
                        navController.navigate(QuickAddDestination(today = today, voiceText = task.phrase, voiceTask = task.id))
                    },
                    onSaveAll = { items ->
                        popupScope.launch {
                            val ids = voiceSaver.save(items)
                            offer = UndoOffer(addedText(ids.size), UndoKind.Done) { voiceSaver.undo(ids) }
                        }
                    },
                )
            }
            composable<QuickAddDestination> { entry ->
                val voiceTask = entry.toRoute<QuickAddDestination>().voiceTask
                val from = if (voiceTask != null) voiceOrigin else origin.takeIf { navController.previousBackStackEntry == null }
                CompositionLocalProvider(LocalSheetOrigin provides from) {
                    QuickAddRoute(onClose = close, onSaved = {
                        // Сохранённая из окна задачи продиктованная задача уходит из списка окна голоса.
                        val voice = navController.previousBackStackEntry
                        if (voiceTask != null && voice != null) ViewModelProvider(voice)[VoiceListModel::class.java].saved(voiceTask)
                    })
                }
            }
        }
        if (offer != null) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss))
            UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = AppTheme.spacing.lg))
        }
    }
}

/** Подсказка с «Отменой» в прозрачном окне: [text] и что сделать по нажатию. */
private class UndoOffer(val text: String, val kind: UndoKind, val undo: () -> Unit)
