package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskDetailRoute(onClose: () -> Unit, onOpenTask: (String) -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val rescheduled = stringResource(R.string.task_rescheduled)
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskDetailEffect.Close -> onClose()
                is TaskDetailEffect.Rescheduled -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(rescheduled.format(days.format(effect.next)))
                }
            }
        }
    }
    Box {
        TaskDetailScreen(state, viewModel::onIntent, onClose, onOpenTask)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun TaskDetailScreen(
    state: TaskDetailState,
    onIntent: (TaskDetailIntent) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val task = state.task

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back), tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (task != null) TaskMenu(onIntent)
        }
        if (task == null) return@Column
        val done = task.status != TaskStatus.ACTIVE

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = spacing.xxs)) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { onIntent(TaskDetailIntent.ToggleCompleted) },
                    colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.textSecondary, checkmarkColor = colors.onAccent),
                    modifier = Modifier.testTag(TAG_DETAIL_DONE),
                )
                FormTextField(
                    value = task.title,
                    onValueChange = { onIntent(TaskDetailIntent.Title(it)) },
                    hint = stringResource(R.string.detail_title_hint),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textDecoration = if (done) TextDecoration.LineThrough else null),
                    modifier = Modifier.weight(1f).testTag(TAG_DETAIL_TITLE),
                )
                PriorityBadge(task.priority, Modifier.padding(end = spacing.sm))
            }
            TaskFieldChips(
                due = task.due,
                projectId = task.projectId,
                priority = task.priority,
                rrule = task.rrule,
                reminders = task.reminders,
                projects = state.projects,
                today = state.today,
                zone = state.zone,
                onEdit = { onIntent(TaskDetailIntent.Edit(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md),
            )
            FormTextField(
                value = task.content,
                onValueChange = { onIntent(TaskDetailIntent.Content(it)) },
                hint = stringResource(R.string.quick_add_content_hint),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                modifier = Modifier.padding(horizontal = spacing.xs).testTag(TAG_DETAIL_CONTENT),
            )
            ChecklistSection(state.checklist, onIntent)
            if (state.subtasks.isNotEmpty()) SubtasksSection(state.subtasks, onIntent, onOpenTask)
            Text(
                text = stringResource(R.string.detail_created, CREATED.format(task.createdAt.atZone(state.zone))),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            )
        }
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

@Composable
private fun TaskMenu(onIntent: (TaskDetailIntent) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.testTag(TAG_DETAIL_MENU)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.detail_menu), tint = AppTheme.colors.textPrimary)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppTheme.colors.surface) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = AppTheme.colors.danger) },
                onClick = { menu = false; onIntent(TaskDetailIntent.RequestDelete) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(start = AppTheme.spacing.md, top = AppTheme.spacing.sm),
    )
}

@Composable
private fun ChecklistSection(items: List<ChecklistItem>, onIntent: (TaskDetailIntent) -> Unit) {
    val colors = AppTheme.colors
    SectionTitle(stringResource(R.string.detail_checklist))
    items.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs).testTag("item:${item.id}")) {
            Checkbox(
                checked = item.isDone,
                onCheckedChange = { onIntent(TaskDetailIntent.ToggleItem(item.id)) },
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.textSecondary, checkmarkColor = colors.onAccent),
            )
            Text(
                text = item.title,
                color = if (item.isDone) colors.textSecondary else colors.textPrimary,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).clickable { onIntent(TaskDetailIntent.StartRenameItem(item.id)) },
            )
            ItemMenu(item, isFirst = index == 0, isLast = index == items.lastIndex, onIntent)
        }
    }
    var newItem by rememberSaveable { mutableStateOf("") }
    FormTextField(
        value = newItem,
        onValueChange = { newItem = it },
        hint = stringResource(R.string.detail_checklist_add),
        textStyle = MaterialTheme.typography.bodyMedium,
        imeAction = ImeAction.Done,
        onImeAction = {
            onIntent(TaskDetailIntent.AddItem(newItem))
            newItem = ""
        },
        modifier = Modifier.padding(start = AppTheme.spacing.xl).testTag(TAG_DETAIL_ADD_ITEM),
    )
}

@Composable
private fun ItemMenu(item: ChecklistItem, isFirst: Boolean, isLast: Boolean, onIntent: (TaskDetailIntent) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.testTag("item_menu:${item.id}")) {
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
    SectionTitle(stringResource(R.string.detail_subtasks))
    subtasks.forEach { sub ->
        val done = sub.status != TaskStatus.ACTIVE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onOpenTask(sub.id) }.padding(horizontal = AppTheme.spacing.xxs).testTag("subtask:${sub.id}"),
        ) {
            Checkbox(
                checked = done,
                onCheckedChange = { onIntent(TaskDetailIntent.ToggleSubtask(sub.id)) },
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.textSecondary, checkmarkColor = colors.onAccent),
            )
            Text(
                text = sub.title,
                color = if (done) colors.textSecondary else colors.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            PriorityBadge(sub.priority, Modifier.padding(end = AppTheme.spacing.md))
        }
    }
}

private val CREATED: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))

const val TAG_DETAIL_TITLE = "detail_title"
const val TAG_DETAIL_CONTENT = "detail_content"
const val TAG_DETAIL_DONE = "detail_done"
const val TAG_DETAIL_MENU = "detail_menu"
const val TAG_DETAIL_ADD_ITEM = "detail_add_item"
