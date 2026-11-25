package com.prostovasei.simpletasks.ui.feature.all

import androidx.compose.foundation.layout.Column
import com.prostovasei.simpletasks.ui.feature.bulk.selectionBottomInset
import com.prostovasei.simpletasks.ui.feature.bulk.rememberSelection
import com.prostovasei.simpletasks.ui.feature.bulk.Selectable
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionFrame
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.DayHeader
import com.prostovasei.simpletasks.designsystem.components.EmptyTasks
import com.prostovasei.simpletasks.designsystem.components.OverdueHeader
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.rememberNowMinute
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.widget.WidgetDates

@Composable
fun AllRoute(onOpenTask: (String) -> Unit, viewModel: AllViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    AllScreen(state, viewModel::onIntent, snackbar, onOpenTask, nowMinute = rememberNowMinute())
}

/** Лента «Предстоящее» по дням (итерация 13, макет PM; итерация 55 — заголовок «Предстоящее»), «+» — в нижней панели. */
@Composable
fun AllScreen(
    state: AllState,
    onIntent: (TaskListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTask: (String) -> Unit = {},
    nowMinute: Int? = null,
) {
    TaskCanvasTheme(compactTimeline = true) {

        val colors = AppTheme.colors
        val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { UndoSnackbarHost(snackbarHostState) },
        ) { padding ->
            if (!state.isLoading && state.isEmpty) {
                Column(Modifier.padding(padding)) {
                    EmptyTasks(stringResource(R.string.all_empty), stringResource(R.string.empty_hint))
                }
                return@Scaffold
            }
            // Итерация 40: долгое нажатие на плашку — режим выбора (будущие повторы не выбираются).
            val selection = rememberSelection()
            val allItems = state.overdue + state.todayAllDay + state.todayTimed + state.upcoming
            SelectionFrame(selection, allItems.filterNot { it.isGhost }.map { Selectable(it.id, it.isDone, it.isRecurring, it.due != null) }, Modifier.padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = AppTheme.spacing.xs, bottom = selectionBottomInset(selection)),
            ) {
                // #241 (решение PM): заголовка «Предстоящее» над лентой больше нет — название страницы видно по значку в нижней панели.
                val groups = state.dayGroups()
                val today = state.today ?: java.time.LocalDate.now()
                // #334 (решение PM): просроченные — одним блоком «Просрочено» без разбивки по дням; справа вместо времени — день недели.
                // Нажатие на подпись сворачивает блок, тонкая →| переносит всё на сегодня, число — справа.
                val (pastGroups, dayGroups) = groups.partition { it.isOverdue }
                if (pastGroups.isNotEmpty()) {
                    val overdueItems = pastGroups.flatMap { it.items }
                    val overdueCount = overdueItems.count { !it.showsDone }
                    item(key = "overdue-header") {
                        OverdueHeader(
                            count = overdueCount,
                            collapsed = state.overdueCollapsed,
                            foldDescription = stringResource(if (state.overdueCollapsed) R.string.overdue_fold_closed else R.string.overdue_fold_open),
                            moveDescription = stringResource(R.string.overdue_move_today),
                            onToggle = { onIntent(TaskListIntent.ToggleOverdue) },
                            onMoveToday = { onIntent(TaskListIntent.MoveOverdue) }.takeIf { overdueCount > 0 },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (!state.overdueCollapsed) {
                        timelineItems(
                            items = overdueItems,
                            keyPrefix = "overdue",
                            days = days,
                            nowMinute = null,
                            isOverdue = { true },
                            showList = true,
                            onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
                            onOpen = { onOpenTask(it.taskId) },
                            dayLabels = true,
                            selection = selection.takeIf { it.active }?.ids,
                            onSelect = { selection.toggle(it.id) },
                            today = today,
                        )
                    }
                }
                dayGroups.forEach { group ->
                    item(key = "day-${group.date}") {
                        DayHeader(
                            text = WidgetDates.header(group.date),
                            relative = group.relative?.let(days::day),
                            isOverdue = false,
                            modifier = Modifier.animateItem(),
                            // #331: как в виджете — у «Сегодня» прогресс, у остальных дней число задач.
                            count = group.items.size,
                            progress = if (group.relative == DayLabel.Today && group.items.isNotEmpty()) group.items.count { it.isDone } to group.items.size else null,
                        )
                    }
                    timelineItems(
                        items = group.items,
                        keyPrefix = "day-${group.date}",
                        days = days,
                        nowMinute = nowMinute.takeIf { group.relative == DayLabel.Today },
                        isOverdue = { it.id in group.overdueIds },
                        showList = true,
                        onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
                        onOpen = { onOpenTask(it.taskId) },
                        selection = selection.takeIf { it.active }?.ids,
                        onSelect = { selection.toggle(it.id) },
                        today = today,
                    )
                }
            }
            }
        }
    }
}
