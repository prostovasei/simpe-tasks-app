package com.prostovasei.simpletasks.ui.feature.task

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import com.prostovasei.simpletasks.designsystem.components.ConfirmDeleteDialog
import com.prostovasei.simpletasks.designsystem.components.NameDialog
import com.prostovasei.simpletasks.designsystem.components.PriorityBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ChecklistItem
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.ui.common.DayNames
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskDetailRoute(onClose: () -> Unit, onOpenTask: (String) -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = rememberSheetMotion(onClose)
    val snackbar = remember { SnackbarHostState() }
    val rescheduled = stringResource(R.string.task_rescheduled)
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskDetailEffect.Close -> motion.close()
                is TaskDetailEffect.Rescheduled -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(rescheduled.format(days.format(effect.next)))
                }
            }
        }
    }
    TaskDetailScreen(state, viewModel::onIntent, motion.close, onOpenTask, progress = motion.progress, reveal = motion.reveal) {
        // Подсказка «Следующий раз» — сверху, над шторкой: внизу её закрыла бы панель кнопок.
        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

/**
 * Созданная задача — та же шторка с сеткой недели, что и новая (итерация 19, решения PM): все правки сохраняются сразу,
 * галочка «Готово» и затемнение просто закрывают. Галочка у названия — выполнить; корзина на панели — удалить
 * с подтверждением. Чек-лист: отметить, переименовать (нажатие на пункт), «⋮» — выше, ниже, удалить. Подзадачи из TickTick.
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
        actionCount = 2,
        actions = {
            ToolButton(R.string.sheet_delete, active = false, tag = TAG_DETAIL_DELETE, icon = R.drawable.ic_tool_delete, tint = colors.danger) {
                onIntent(TaskDetailIntent.RequestDelete)
            }
            SheetConfirmButton(R.string.sheet_done, enabled = true, tag = TAG_DETAIL_CLOSE, onClick = onClose)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoneCircle(done, Modifier.testTag(TAG_DETAIL_DONE)) { onIntent(TaskDetailIntent.ToggleCompleted) }
            CompactField(
                value = task.title,
                onValueChange = { onIntent(TaskDetailIntent.Title(it)) },
                hint = stringResource(R.string.detail_title_hint),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                ),
                modifier = Modifier.weight(1f).testTag(TAG_DETAIL_TITLE),
            )
            PriorityBadge(task.priority)
        }
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
        Text(
            text = stringResource(R.string.detail_created, CREATED.format(task.createdAt.atZone(state.zone))),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(vertical = spacing.xs),
        )
    }

    if (state.showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.detail_delete_title),
            text = stringResource(R.string.detail_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onIntent(TaskDetailIntent.ConfirmDelete) },
            onDismiss = { onIntent(TaskDetailIntent.DismissDelete) },
        )
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
 * Видимый кружок небольшой, зона нажатия шире него (поля вокруг входят в нажатие).
 */
@Composable
private fun DoneCircle(checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
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
            .then(if (checked) Modifier.background(colors.accent) else Modifier.border(sizes.checkboxStroke, colors.textSecondary, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = colors.onAccent, modifier = Modifier.size(sizes.doneCheck))
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

private val CREATED: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))

const val TAG_DETAIL_TITLE = "detail_title"
const val TAG_DETAIL_CONTENT = "detail_content"
const val TAG_DETAIL_DONE = "detail_done"
const val TAG_DETAIL_DELETE = "detail_delete"
const val TAG_DETAIL_CLOSE = "detail_close"
const val TAG_DETAIL_ADD_ITEM = "detail_add_item"
