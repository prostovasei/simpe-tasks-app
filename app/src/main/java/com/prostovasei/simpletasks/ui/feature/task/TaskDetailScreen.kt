package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.NameDialog
import com.prostovasei.simpletasks.designsystem.components.PriorityBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.widget.WidgetDates
import com.prostovasei.simpletasks.ui.common.DayNames
import kotlinx.coroutines.launch

@Composable
fun TaskDetailRoute(onClose: () -> Unit, onOpenTask: (String) -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = rememberSheetMotion(onClose)
    val snackbar = remember { SnackbarHostState() }
    val rescheduled = stringResource(R.string.task_rescheduled)
    val futureHint = stringResource(R.string.future_done_hint)
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskDetailEffect.Close -> motion.close()
                is TaskDetailEffect.Rescheduled -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(rescheduled.format(days.format(effect.next)))
                }
                is TaskDetailEffect.FutureDone -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(futureHint.format(effect.nearest))
                }
            }
        }
    }
    TaskDetailScreen(state, viewModel::onIntent, motion.close, onOpenTask, progress = motion.progress, reveal = motion.reveal) {
        // Подсказка «Следующий раз» — сверху, над шторкой: внизу её закрыла бы панель кнопок.
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

/**
 * Созданная задача — та же шторка с сеткой недели, что и новая (итерация 19, решения PM): все правки сохраняются сразу,
 * галочка «Готово» и затемнение просто закрывают. Круг у названия (цвет приоритета) — выполнить; «⋮» справа от названия —
 * «Удалить» (сразу, с «Отменой» под шторкой, итерация 36) и дата создания (итерация 34). Чек-лист: отметить, переименовать (нажатие на пункт), «⋮» — выше, ниже, удалить. Подзадачи из TickTick.
 */
@Composable
fun TaskDetailScreen(
    state: TaskDetailState,
    onIntent: (TaskDetailIntent) -> Unit,
    onClose: () -> Unit,
    onOpenTask: (String) -> Unit,
    progress: () -> Float = { 1f },
    reveal: () -> Float = { 1f },
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val task = state.task
    val fields = state.fields
    if (task == null || fields == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val done = task.status != TaskStatus.ACTIVE
    val addFocus = remember { FocusRequester() }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var focusAdd by remember { mutableStateOf(false) }
    LaunchedEffect(focusAdd) {
        if (focusAdd) {
            addFocus.requestFocus()
            focusAdd = false
        }
    }

    TaskSheet(
        fields = fields,
        onGrid = { onIntent(TaskDetailIntent.Grid(it)) },
        onEdit = { onIntent(TaskDetailIntent.Edit(it)) },
        onClose = onClose,
        checklistActive = state.checklist.isNotEmpty(),
        onChecklist = {
            showAdd = true
            focusAdd = true
        },
        progress = progress,
        reveal = reveal,
        overlay = overlay,
        actions = {
            SheetConfirmButton(R.string.sheet_done, enabled = true, tag = TAG_DETAIL_CLOSE, onClick = onClose)
        },
    ) {
        // Будущий повтор (итерация 49): плашка сверху — какой это раз и когда ближайший.
        if (state.occurrence != null) FutureBanner(task)
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoneCircle(done, priorityColor(task.priority), Modifier.testTag(TAG_DETAIL_DONE)) { onIntent(TaskDetailIntent.ToggleCompleted) }
            CompactField(
                value = task.title,
                onValueChange = { onIntent(TaskDetailIntent.Title(it)) },
                hint = stringResource(R.string.detail_title_hint),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                ),
                modifier = Modifier.weight(1f).testTag(TAG_DETAIL_TITLE),
                visualTransformation = smartHighlight(state.smart),
            )
            TaskMenu(
                createdText(task.createdAt.atZone(state.zone).toLocalDate()),
                onWontDo = if (task.status == TaskStatus.ACTIVE) { { onIntent(TaskDetailIntent.WontDo) } } else null,
            ) { onIntent(TaskDetailIntent.RequestDelete) }
        }
        SmartDateLine(state.smart, state.today, state.zone) { onIntent(TaskDetailIntent.DismissSmartDate) }
        CompactField(
            value = task.content,
            onValueChange = { onIntent(TaskDetailIntent.Content(it)) },
            hint = stringResource(R.string.quick_add_content_hint),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = colors.textSecondary),
            singleLine = false,
            modifier = Modifier.padding(top = spacing.xxs).testTag(TAG_DETAIL_CONTENT),
        )
        state.checklist.forEachIndexed { index, item ->
            ChecklistRow(item, isFirst = index == 0, isLast = index == state.checklist.lastIndex, onIntent)
        }
        if (showAdd || state.checklist.isNotEmpty()) {
            var newItem by rememberSaveable { mutableStateOf("") }
            CompactField(
                value = newItem,
                onValueChange = { newItem = it },
                hint = stringResource(R.string.detail_checklist_add),
                textStyle = MaterialTheme.typography.bodyMedium,
                imeAction = ImeAction.Done,
                onImeAction = {
                    onIntent(TaskDetailIntent.AddItem(newItem))
                    newItem = ""
                },
                modifier = Modifier.padding(start = AppTheme.sizes.checkboxTouch).focusRequester(addFocus).testTag(TAG_DETAIL_ADD_ITEM),
            )
        }
        if (state.subtasks.isNotEmpty()) SubtasksSection(state.subtasks, onIntent, onOpenTask)
    }

    when (val dialog = state.dialog) {
        DetailDialog.DeleteSeries -> ChoiceDialog(
            title = stringResource(R.string.delete_series_title),
            text = stringResource(R.string.delete_series_text, task.title),
            tag = TAG_DELETE_SERIES_DIALOG,
            choices = listOf(
                Choice(stringResource(R.string.delete_series_confirm), ChoiceStyle.Danger, TAG_CHOICE_DELETE) { onIntent(TaskDetailIntent.ConfirmDeleteSeries) },
                Choice(stringResource(R.string.action_wont_do), ChoiceStyle.Outline, TAG_CHOICE_WONT_DO, R.drawable.ic_wont_do) { onIntent(TaskDetailIntent.WontDo) },
                Choice(stringResource(R.string.action_cancel), ChoiceStyle.Plain, TAG_CHOICE_CANCEL) { onIntent(TaskDetailIntent.DismissDialog) },
            ),
            onDismiss = { onIntent(TaskDetailIntent.DismissDialog) },
        )
        is DetailDialog.Unclosed -> {
            val target = state.occurrence?.let(::dayLabel).orEmpty()
            ChoiceDialog(
                title = stringResource(R.string.unclosed_title),
                text = stringResource(R.string.unclosed_text, target, task.title, dialog.dates.joinToString(", ", transform = ::dayLabel)),
                tag = TAG_UNCLOSED_DIALOG,
                choices = listOf(
                    Choice(stringResource(R.string.unclosed_skip), ChoiceStyle.Primary, TAG_CHOICE_SKIP) { onIntent(TaskDetailIntent.CloseEarlier(EarlierRepeats.SKIP)) },
                    Choice(stringResource(R.string.unclosed_complete), ChoiceStyle.Outline, TAG_CHOICE_COMPLETE) { onIntent(TaskDetailIntent.CloseEarlier(EarlierRepeats.COMPLETE)) },
                    Choice(stringResource(R.string.action_cancel), ChoiceStyle.Plain, TAG_CHOICE_CANCEL) { onIntent(TaskDetailIntent.DismissDialog) },
                ),
                onDismiss = { onIntent(TaskDetailIntent.DismissDialog) },
            )
        }
        is DetailDialog.ChangeRepeat -> {
            val future = dialog.nearest != null
            ChoiceDialog(
                title = stringResource(R.string.change_repeat_title),
                text = stringResource(R.string.change_repeat_text, task.title, dialog.from, dialog.to),
                tag = TAG_CHANGE_REPEAT_DIALOG,
                choices = listOf(
                    Choice(
                        stringResource(R.string.change_repeat_current),
                        if (future) ChoiceStyle.Outline else ChoiceStyle.Primary,
                        TAG_CHOICE_CURRENT,
                        enabled = !future,
                        hint = dialog.nearest?.let { stringResource(R.string.change_repeat_hint, it) },
                    ) { onIntent(TaskDetailIntent.ChangeRepeat(all = false)) },
                    Choice(
                        stringResource(R.string.change_repeat_all),
                        if (future) ChoiceStyle.Primary else ChoiceStyle.Outline,
                        TAG_CHOICE_ALL,
                    ) { onIntent(TaskDetailIntent.ChangeRepeat(all = true)) },
                    Choice(stringResource(R.string.action_cancel), ChoiceStyle.Plain, TAG_CHOICE_CANCEL) { onIntent(TaskDetailIntent.DismissDialog) },
                ),
                onDismiss = { onIntent(TaskDetailIntent.DismissDialog) },
            )
        }
        null -> Unit
    }

    state.renamingItem?.let { item ->
        NameDialog(
            title = stringResource(R.string.detail_item_rename),
            initialName = item.title,
            confirmLabel = stringResource(R.string.action_save),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onIntent(TaskDetailIntent.RenameItem(it)) },
            onDismiss = { onIntent(TaskDetailIntent.DismissRename) },
        )
    }
}

/**
 * Отметка «выполнено» у названия (0.31.2, решение PM): круглая и вплотную к левому краю — как описание под ней.
 * Видимый кружок небольшой, зона нажатия шире него (поля вокруг входят в нажатие). Цвет — приоритета задачи (итерация 34),
 * без приоритета — серый контур и зелёная заливка у выполненной.
 */
@Composable
private fun DoneCircle(checked: Boolean, priority: androidx.compose.ui.graphics.Color?, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val spacing = AppTheme.spacing
    val description = stringResource(R.string.notification_complete)
    Box(
        modifier
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description }
            .padding(end = spacing.xs, top = spacing.xs, bottom = spacing.xs)
            .size(sizes.doneCircle)
            .clip(CircleShape)
            .then(if (checked) Modifier.background(priority ?: colors.accent) else Modifier.border(sizes.checkboxStroke, priority ?: colors.textSecondary, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = colors.onAccent, modifier = Modifier.size(sizes.doneCheck))
    }
}

/** Плашка будущего повтора (итерация 49): «Будущий повтор · ближайший — вс, 08 ноя». */
@Composable
private fun FutureBanner(series: Task) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val nearest = (series.start ?: series.due)?.let(::dayLabel).orEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = spacing.xs)
            .clip(AppTheme.shapes.large)
            .background(colors.selected)
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
            .testTag(TAG_DETAIL_FUTURE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_tool_repeat), null, tint = colors.textSecondary, modifier = Modifier.size(AppTheme.sizes.metaIcon))
        Text(
            stringResource(R.string.future_banner, nearest),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = spacing.xs),
        )
    }
}

/** «вс, 08 ноя» для даты раза. */
private fun dayLabel(due: TaskDue): String = WidgetDates.withWeekday(
    when (due) {
        is TaskDue.AllDay -> due.date
        is TaskDue.AtTime -> due.instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    },
)

/**
 * «⋮» справа от названия: «Не буду делать» (итерация 49; только у незакрытой задачи), «Удалить» (красным) и под чертой —
 * когда задача создана.
 */
@Composable
private fun TaskMenu(created: String, onWontDo: (() -> Unit)?, onDelete: () -> Unit) {
    val colors = AppTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(AppTheme.sizes.checkboxTouch).testTag(TAG_DETAIL_MENU)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.detail_menu), tint = colors.textSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surface) {
            if (onWontDo != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_wont_do), color = colors.textPrimary, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_wont_do), null, tint = colors.textPrimary, modifier = Modifier.size(AppTheme.sizes.icon)) },
                    onClick = {
                        open = false
                        onWontDo()
                    },
                    modifier = Modifier.testTag(TAG_DETAIL_WONT_DO),
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = colors.danger, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_tool_delete), null, tint = colors.danger, modifier = Modifier.size(AppTheme.sizes.icon)) },
                onClick = {
                    open = false
                    onDelete()
                },
                modifier = Modifier.testTag(TAG_DETAIL_DELETE),
            )
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = AppTheme.spacing.sm))
            Text(
                stringResource(R.string.detail_created, created),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm).testTag(TAG_DETAIL_CREATED),
            )
        }
    }
}

@Composable
private fun SheetCheckbox(checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    Checkbox(
        checked = checked,
        onCheckedChange = { onToggle() },
        colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.textSecondary, checkmarkColor = colors.onAccent),
        modifier = modifier.size(AppTheme.sizes.checkboxTouch),
    )
}

@Composable
private fun ChecklistRow(item: ChecklistItem, isFirst: Boolean, isLast: Boolean, onIntent: (TaskDetailIntent) -> Unit) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("item:${item.id}")) {
        SheetCheckbox(item.isDone) { onIntent(TaskDetailIntent.ToggleItem(item.id)) }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isDone) colors.textSecondary else colors.textPrimary,
            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).clickable { onIntent(TaskDetailIntent.StartRenameItem(item.id)) },
        )
        ItemMenu(item, isFirst, isLast, onIntent)
    }
}

@Composable
private fun ItemMenu(item: ChecklistItem, isFirst: Boolean, isLast: Boolean, onIntent: (TaskDetailIntent) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.size(AppTheme.sizes.checkboxTouch).testTag("item_menu:${item.id}")) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.detail_item_menu), tint = AppTheme.colors.textSecondary)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            if (!isFirst) MenuText(R.string.detail_item_up) { menu = false; onIntent(TaskDetailIntent.MoveItem(item.id, up = true)) }
            if (!isLast) MenuText(R.string.detail_item_down) { menu = false; onIntent(TaskDetailIntent.MoveItem(item.id, up = false)) }
            MenuText(R.string.action_rename) { menu = false; onIntent(TaskDetailIntent.StartRenameItem(item.id)) }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = AppTheme.colors.danger) },
                onClick = { menu = false; onIntent(TaskDetailIntent.DeleteItem(item.id)) },
            )
        }
    }
}

@Composable
private fun MenuText(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label), color = AppTheme.colors.textPrimary) }, onClick = onClick)
}

@Composable
private fun SubtasksSection(subtasks: List<Task>, onIntent: (TaskDetailIntent) -> Unit, onOpenTask: (String) -> Unit) {
    val colors = AppTheme.colors
    Text(
        text = stringResource(R.string.detail_subtasks).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = AppTheme.spacing.sm),
    )
    subtasks.forEach { sub ->
        val done = sub.status != TaskStatus.ACTIVE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onOpenTask(sub.id) }.testTag("subtask:${sub.id}"),
        ) {
            SheetCheckbox(done) { onIntent(TaskDetailIntent.ToggleSubtask(sub.id)) }
            Text(
                text = sub.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) colors.textSecondary else colors.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            PriorityBadge(sub.priority)
        }
    }
}


const val TAG_DETAIL_TITLE = "detail_title"
const val TAG_DETAIL_CONTENT = "detail_content"
const val TAG_DETAIL_DONE = "detail_done"
const val TAG_DETAIL_DELETE = "detail_delete"
const val TAG_DETAIL_MENU = "detail_menu"
const val TAG_DETAIL_CREATED = "detail_created"
const val TAG_DETAIL_CLOSE = "detail_close"
const val TAG_DETAIL_ADD_ITEM = "detail_add_item"
const val TAG_DETAIL_WONT_DO = "detail_wont_do"
const val TAG_DETAIL_FUTURE = "detail_future"
const val TAG_DELETE_SERIES_DIALOG = "delete_series_dialog"
const val TAG_UNCLOSED_DIALOG = "unclosed_dialog"
const val TAG_CHANGE_REPEAT_DIALOG = "change_repeat_dialog"
const val TAG_CHOICE_CURRENT = "current"
const val TAG_CHOICE_ALL = "all"
const val TAG_CHOICE_DELETE = "delete"
const val TAG_CHOICE_WONT_DO = "wont_do"
const val TAG_CHOICE_SKIP = "skip"
const val TAG_CHOICE_COMPLETE = "complete"
const val TAG_CHOICE_CANCEL = "cancel"
