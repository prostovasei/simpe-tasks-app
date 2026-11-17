package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import java.time.LocalDate

@Composable
fun WeekRoute(onOpenTask: (String) -> Unit, onOpenDay: (LocalDate) -> Unit, viewModel: WeekViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    WeekScreen(state, viewModel::onIntent, snackbar, onOpenTask, onOpenDay)
}

/**
 * Вид «Неделя» (итерация 44, макет `VariantK1`): сетка 2×4 — шесть дней, воскресенье и мини-календарь. Все ячейки одинаковые и
 * помещаются на экран без прокрутки (решение PM); не влезшие задачи — «+N» в строке даты, нажатие на неё открывает день.
 * Заголовка нет: неделя видна по календарю, «+» — в нижней панели.
 */
@Composable
fun WeekScreen(
    state: WeekState,
    onIntent: (TaskListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTask: (String) -> Unit = {},
    onOpenDay: (LocalDate) -> Unit = {},
) {
    TaskCanvasTheme {

        val colors = AppTheme.colors
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { UndoSnackbarHost(snackbarHostState) },
        ) { padding ->
            val weekStart = state.weekStart
            val today = state.today
            val month = state.month
            if (weekStart == null || today == null || month == null) return@Scaffold
            // Четыре строки одинаковой высоты делят весь экран: прокрутки нет.
            Column(Modifier.padding(padding).fillMaxSize().testTag("week-screen")) {
                state.days.chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        pair.forEach { day ->
                            WeekCell(day.date, day.items, day.date == today, { onOpenDay(day.date) }, Modifier.weight(1f).fillMaxHeight())
                        }
                        // Нечётный день (воскресенье) делит строку с мини-календарём.
                        if (pair.size == 1) {
                            WeekCalendar(
                                month = month,
                                selectedWeek = weekStart,
                                today = today,
                                onSelectWeek = { onIntent(SelectWeek(it)) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}
