package com.prostovasei.simpletasks.ui.feature.week

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
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
import java.time.LocalDate

/** Шторка дня поверх рабочего стола (итерация 51): данные — те же, что у полного списка дня в приложении ([DayViewModel]). */
@Composable
fun DaySheetRoute(onClose: () -> Unit, onOpenTask: (String) -> Unit, onAdd: (LocalDate) -> Unit, viewModel: DayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    DaySheet(state, viewModel::onIntent, onClose, onOpenTask, onAdd, snackbar, nowMinute = rememberNowMinute())
}

/**
 * Шторка дня (итерация 51, макет iteration51.py, доска «Шторка дня»): нажатие на дату дня в «Неделе» виджета — приложение не
 * открывается. Сверху дата, «Сегодня» / «Завтра» и «выполнено из всех», ниже лента дня как в приложении (кружок отмечает, плашка
 * открывает задачу), снизу «Задача на …». Шторка не выше [SHEET_MAX_FRACTION] экрана, дальше лента прокручивается. «Назад»,
 * смахивание вниз за верх шторки и нажатие на затемнение закрывают.
 */
@Composable
fun DaySheet(
    state: DayState,
    onIntent: (TaskListIntent) -> Unit,
    onClose: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAdd: (LocalDate) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    nowMinute: Int? = null,
) {
    TaskCanvasTheme {
        val colors = AppTheme.colors
        val spacing = AppTheme.spacing
        val sizes = AppTheme.sizes
        val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
        val date = state.date
        val today = state.today
        val closeDistance = with(LocalDensity.current) { sizes.toolButton.toPx() }
        BackHandler(onBack = onClose)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                    .testTag(TAG_DAY_SCRIM),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = spacing.xs, end = spacing.xs, bottom = spacing.xs)
                    .heightIn(max = maxHeight * SHEET_MAX_FRACTION)
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.nav)
                    .background(colors.surface)
                    // Нажатия по шторке не доходят до затемнения (без clickable: он склеил бы строки в один элемент для чтения с экрана).
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(TAG_DAY_SHEET),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var dragged = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragged = 0f },
                                onDragEnd = { if (dragged > closeDistance) onClose() },
                            ) { _, delta -> dragged += delta }
                        },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        if (date != null && today != null) {
                            val relative = when (val label = DueFormatter.dayLabel(date, today)) {
                                is DayLabel.Date -> null
                                else -> days.day(label)
                            }
                            Text(
                                WidgetDates.dayHeader(date) + relative?.let { " $it" }.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (date < today) colors.overdue else colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).testTag(TAG_DAY_SHEET_TITLE),
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }

                        if (state.items.isNotEmpty()) {
                            Text(
                                stringResource(R.string.day_sheet_progress, state.items.count { it.showsDone }, state.items.size),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.testTag(TAG_DAY_SHEET_COUNT),
                            )
                        }
                    }
                }
                if (!state.isLoading && state.items.isEmpty()) {
                    Text(
                        stringResource(R.string.day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth().background(colors.background).padding(spacing.lg),
                    )
                } else if (date != null && today != null) {
                    LazyColumn(
                        Modifier.weight(1f, fill = false).fillMaxWidth().background(colors.background).testTag(TAG_DAY_SHEET_LIST),
                        contentPadding = PaddingValues(vertical = spacing.xs),
                    ) {
                        timelineItems(
                            items = state.items,
                            keyPrefix = "day-sheet-$date",
                            days = days,
                            nowMinute = nowMinute.takeIf { date == today },
                            showNowMarker = false,
                            isOverdue = { date < today && !it.showsDone },
                            showList = true,
                            onToggle = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) },
                            onOpen = { onOpenTask(it.taskId) },
                        )
                    }
                }
                if (date != null) {
                    Row(Modifier.fillMaxWidth().padding(spacing.sm), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { onAdd(date) },
                            shape = AppTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                            modifier = Modifier.heightIn(min = sizes.navBar - spacing.xs).testTag(TAG_DAY_SHEET_ADD),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = spacing.xxs))
                            Text(stringResource(R.string.day_sheet_add, WidgetDates.withWeekday(date)), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            UndoSnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = spacing.sm))
        }
    }
}

/** Шторка не выше этой доли экрана; дальше лента прокручивается внутри (макет: ~80%). */
private const val SHEET_MAX_FRACTION = 0.8f

const val TAG_DAY_SHEET = "day_sheet"
const val TAG_DAY_SCRIM = "day_sheet_scrim"
const val TAG_DAY_SHEET_TITLE = "day_sheet_title"
const val TAG_DAY_SHEET_COUNT = "day_sheet_count"
const val TAG_DAY_SHEET_LIST = "day_sheet_list"
const val TAG_DAY_SHEET_ADD = "day_sheet_add"
