package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.DayHeader
import com.prostovasei.simpletasks.designsystem.components.EmptyTasks
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.TaskSort
import com.prostovasei.simpletasks.ui.feature.bulk.SelectionState
import com.prostovasei.simpletasks.ui.feature.bulk.selectionBottomInset
import com.prostovasei.simpletasks.widget.WidgetDates
import java.time.LocalDate

/**
 * Список задач экрана списка и «Входящих» с сортировкой (#357, выбор PM): при «по сроку» — подписи дней («Просрочено», дни, «Без даты»),
 * иначе — один список; выполненные — свёрнутой строкой «Выполнено · N ›» внизу. При ручной сортировке строки перетаскиваются.
 * Задачи [tasks] уже упорядочены.
 */
@Composable
fun SortedTaskList(
    tasks: List<TaskItem>,
    sort: TaskSort,
    today: LocalDate?,
    isLoading: Boolean,
    emptyText: String,
    onIntent: (TaskListIntent) -> Unit,
    showList: Boolean,
    modifier: Modifier = Modifier,
    onOpenTask: (String) -> Unit = {},
    emptyArrowLift: Dp = AppTheme.spacing.xxs,
    selection: SelectionState? = null,
    askManual: Boolean = false,
) {
    if (!isLoading && tasks.isEmpty()) {
        EmptyTasks(emptyText, stringResource(R.string.empty_hint), modifier, emptyArrowLift)
        return
    }
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    val sorted = remember(tasks, sort, today) { sortedTasks(tasks, sort, today) }
    val listState = rememberLazyListState()
    val reorder = rememberReorder(listState)
    val flat = sorted.groups.firstOrNull { it.kind == TaskGroup.Kind.FLAT }
    reorder.manual = sort.by == SortBy.MANUAL
    reorder.keyPrefix = TaskGroup(TaskGroup.Kind.FLAT, null, emptyList()).key
    reorder.ids = flat?.items?.map { it.id }.orEmpty()
    reorder.onAsk = { onIntent(TaskListIntent.AskManualSort) }
    reorder.onDrop = { onIntent(TaskListIntent.Reorder(it)) }
    var doneOpen by rememberSaveable { mutableStateOf(false) }
    val day = today ?: LocalDate.now()
    val withHeaders = sorted.groups.any { it.kind == TaskGroup.Kind.OVERDUE || it.kind == TaskGroup.Kind.DAY }
    val toggle: (TaskItem) -> Unit = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) }
    val open: (TaskItem) -> Unit = { onOpenTask(it.taskId) }
    val selected = selection?.takeIf { it.active }?.ids
    val onSelect = selection?.let { s -> { item: TaskItem -> s.toggle(item.id) } }
    val overdueTitle = stringResource(R.string.overdue_title)
    val noDateTitle = stringResource(R.string.sort_no_date)
    LazyColumn(
        modifier.fillMaxSize().testTag(TAG_SORTED_LIST),
        state = listState,
        contentPadding = PaddingValues(top = AppTheme.spacing.xs, bottom = selection?.let { selectionBottomInset(it) } ?: AppTheme.spacing.xs),
    ) {
        sorted.groups.forEach { group ->
            val header: String? = when (group.kind) {
                TaskGroup.Kind.OVERDUE -> overdueTitle
                TaskGroup.Kind.DAY -> group.date?.let { WidgetDates.header(it) }
                TaskGroup.Kind.NO_DATE -> noDateTitle.takeIf { withHeaders }
                TaskGroup.Kind.FLAT -> null
            }
            if (header != null) {
                item(key = "h-${group.key}") {
                    val relative = when (group.date) {
                        day -> days.day(DayLabel.Today)
                        day.plusDays(1) -> days.day(DayLabel.Tomorrow)
                        else -> null
                    }
                    DayHeader(
                        header, relative.takeIf { group.kind == TaskGroup.Kind.DAY }, isOverdue = group.kind == TaskGroup.Kind.OVERDUE,
                        modifier = Modifier.animateItem().testTag("$TAG_GROUP_HEADER:${group.key}"), count = group.items.size,
                    )
                }
            }
            timelineItems(
                items = if (group.kind == TaskGroup.Kind.FLAT) reorder.arrange(group.items) { it.id } else group.items,
                keyPrefix = group.key,
                days = days,
                nowMinute = null,
                isOverdue = { group.kind == TaskGroup.Kind.OVERDUE },
                showList = showList,
                onToggle = toggle,
                onOpen = open,
                dayLabels = group.kind != TaskGroup.Kind.DAY,
                selection = selected,
                onSelect = onSelect,
                today = day,
                reorder = reorder,
            )
        }
        if (sorted.done.isNotEmpty()) {
            item(key = "h-done") { DoneHeader(sorted.done.size, doneOpen, Modifier.animateItem()) { doneOpen = !doneOpen } }
            if (doneOpen) {
                timelineItems(
                    items = sorted.done, keyPrefix = "done", days = days, nowMinute = null, isOverdue = { false }, showList = showList,
                    onToggle = toggle, onOpen = open, dayLabels = true, selection = selected, onSelect = onSelect, today = day,
                )
            }
        }
    }
    if (askManual) {
        ManualSortDialog(
            current = stringResource(sortLabel(sort.by)).lowercase(),
            onSwitch = { onIntent(TaskListIntent.SwitchToManual(it)) },
            onDismiss = { onIntent(TaskListIntent.DismissManualSort) },
        )
    }
}

/** «Выполнено · N ›» (#357, выбор PM): серая строка, нажатие раскрывает выполненные сегодня. */
@Composable
private fun DoneHeader(count: Int, open: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = SizeTokens.DAY_HEAD_SP.sp, fontWeight = FontWeight.Medium)
    Row(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(start = spacing.sm, end = spacing.sm, top = spacing.sm, bottom = spacing.xs)
            .testTag(TAG_DONE_HEADER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(stringResource(R.string.sort_done_block), style = style, color = colors.textSecondary)
        Text("›", style = style, color = colors.textSecondary, modifier = Modifier.rotate(if (open) DONE_OPEN_DEGREES else 0f))
        Spacer(Modifier.weight(1f))
        Text(count.toString(), style = style, color = colors.textSecondary)
    }
}

/** «Переключить на ручную сортировку?» (#357): нынешний порядок станет ручным; галочка «Больше не спрашивать». */
@Composable
fun ManualSortDialog(current: String, onSwitch: (skipQuestion: Boolean) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    var skip by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_manual_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(stringResource(R.string.sort_manual_text, current))
                Row(
                    Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm).clickable(role = Role.Checkbox) { skip = !skip }.testTag(TAG_MANUAL_SKIP),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(skip, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.accent))
                    Text(stringResource(R.string.sort_manual_skip), color = colors.textPrimary, modifier = Modifier.padding(start = AppTheme.spacing.xs))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSwitch(skip) }, modifier = Modifier.testTag(TAG_MANUAL_SWITCH)) { Text(stringResource(R.string.sort_manual_switch)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        modifier = Modifier.testTag(TAG_MANUAL_DIALOG),
    )
}

/** Название варианта сортировки. */
fun sortLabel(by: SortBy): Int = when (by) {
    SortBy.DUE -> R.string.sort_due
    SortBy.TIME -> R.string.sort_time
    SortBy.PRIORITY -> R.string.sort_priority
    SortBy.MANUAL -> R.string.sort_manual
    SortBy.CREATED -> R.string.sort_created
}

private const val DONE_OPEN_DEGREES = 90f
const val TAG_SORTED_LIST = "sorted_list"
const val TAG_GROUP_HEADER = "group_header"
const val TAG_DONE_HEADER = "done_header"
const val TAG_MANUAL_DIALOG = "manual_sort_dialog"
const val TAG_MANUAL_SKIP = "manual_sort_skip"
const val TAG_MANUAL_SWITCH = "manual_sort_switch"
