package com.prostovasei.simpletasks.ui.feature.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.dueLines
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog
import com.prostovasei.simpletasks.ui.feature.task.priorityColor
import com.prostovasei.simpletasks.ui.feature.task.rememberSheetMotion
import com.prostovasei.simpletasks.widget.WidgetDates

@Composable
fun TrashTaskRoute(onClose: () -> Unit, viewModel: TrashTaskViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = rememberSheetMotion(onClose)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == TrashTaskEffect.Close) motion.close() }
    }
    TrashTaskScreen(state, viewModel::onIntent, motion.close, progress = motion.progress)
}

/**
 * Задача в корзине (итерация 39, бэклог №35): та же шторка, но править нельзя — только читать. Сверху плашка «В корзине · удалена…»,
 * в «⋮» два пункта: «Восстановить» и «Удалить навсегда» (после подтверждения). Нажатие на затемнение и «Назад» закрывают.
 */
/**
 * Чем окно только для чтения отличается (итерация 49): задача в корзине или «Не буду делать» — значок и текст плашки, пункты «⋮»
 * (первый — [TrashTaskIntent.Restore], второй — [TrashTaskIntent.DeleteForever]) и нужно ли подтверждать второй.
 */
enum class ReadonlyKind(val icon: Int, val banner: Int, val first: Int, val second: Int, val confirmSecond: Boolean) {
    Trash(R.drawable.ic_tool_delete, R.string.trash_banner, R.string.trash_restore, R.string.trash_delete_forever, confirmSecond = true),
    WontDo(R.drawable.ic_wont_do, R.string.wontdo_banner, R.string.wontdo_reopen, R.string.action_delete, confirmSecond = false),
}

@Composable
fun TrashTaskScreen(
    state: TrashTaskState,
    onIntent: (TrashTaskIntent) -> Unit,
    onClose: () -> Unit,
    progress: () -> Float = { 1f },
    kind: ReadonlyKind = ReadonlyKind.Trash,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val trashed = state.trashed
    val item = state.item
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(progress())
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .testTag(TAG_TRASH_TASK_SCRIM),
        )
        if (trashed == null || item == null) return@Box
        val task = trashed.details.task
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = (1f - progress()) * size.height }
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xs, vertical = spacing.sm)
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                // Нажатие на саму карточку не должно закрывать окно, но и склеивать её текст в один узел для чтения с экрана.
                .pointerInput(Unit) { detectTapGestures { } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.md)
                .testTag(TAG_TRASH_TASK_CARD),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.selected).padding(horizontal = spacing.sm + spacing.xxs, vertical = spacing.xs + spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(kind.icon), null, tint = colors.textSecondary, modifier = Modifier.size(sizes.metaIcon))
                Spacer(Modifier.width(spacing.xs))
                Text(
                    stringResource(kind.banner, state.deletedOn?.let { if (kind == ReadonlyKind.WontDo) WidgetDates.withWeekday(it) else WidgetDates.short(it) }.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.testTag(TAG_TRASH_TASK_BANNER),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (kind == ReadonlyKind.WontDo) {
                    Icon(painterResource(R.drawable.ic_wont_do), null, tint = colors.inactive, modifier = Modifier.size(sizes.doneCircle))
                } else {
                    Box(Modifier.size(sizes.doneCircle).clip(CircleShape).border(sizes.checkboxStroke, priorityColor(task.priority) ?: colors.textSecondary, CircleShape))
                }
                Spacer(Modifier.width(spacing.xs))
                Text(
                    task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f).testTag(TAG_TRASH_TASK_TITLE),
                )
                TrashTaskMenu(
                    kind,
                    onRestore = { onIntent(TrashTaskIntent.Restore) },
                    onDeleteForever = { if (kind.confirmSecond) confirmDelete = true else onIntent(TrashTaskIntent.DeleteForever) },
                )
            }
            if (task.content.isNotBlank()) {
                Text(task.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.xxs))
            }
            val due = dueLines(item).joinToString(" · ")
            if (due.isNotEmpty()) {
                Row(Modifier.padding(top = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_widget_all), null, tint = colors.textSecondary, modifier = Modifier.size(sizes.metaIcon))
                    Spacer(Modifier.width(spacing.xs))
                    Text(due, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
            Row(Modifier.padding(top = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                ListBadge(item.listEmoji)
                // Без списка: с датой — «Без списка», без даты — «Входящие» (итерация 52.1).
                val listName = if (item.isInboxList) stringResource(if (item.due != null) R.string.no_list_title else R.string.inbox_list_name) else item.listName
                listName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(start = spacing.xs)) }
                if (item.isRecurring) Icon(painterResource(R.drawable.ic_tool_repeat), null, tint = colors.textSecondary, modifier = Modifier.padding(start = spacing.sm).size(sizes.metaIcon))
                if (item.reminder != null) Icon(painterResource(R.drawable.ic_tool_reminder), null, tint = colors.textSecondary, modifier = Modifier.padding(start = spacing.sm).size(sizes.metaIcon))
            }
            if (trashed.details.checklist.isNotEmpty()) {
                Text(stringResource(R.string.trash_checklist).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.md))
                trashed.details.checklist.forEach { entry ->
                    Text(
                        (if (entry.isDone) "✓ " else "○ ") + entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.isDone) colors.textSecondary else colors.textPrimary,
                        modifier = Modifier.padding(top = spacing.xxs),
                    )
                }
            }
            if (trashed.subtasks.isNotEmpty()) {
                Text(stringResource(R.string.trash_subtasks).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.md))
                trashed.subtasks.forEach { sub ->
                    Text(sub.title, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(top = spacing.xxs))
                }
            }
        }
    }
    if (confirmDelete) {
        SheetDialog(
            title = stringResource(R.string.trash_delete_one_title),
            tag = TAG_TRASH_DELETE_DIALOG,
            onDismiss = { confirmDelete = false },
            onDone = {
                confirmDelete = false
                onIntent(TrashTaskIntent.DeleteForever)
            },
            doneLabel = R.string.trash_delete_confirm,
            danger = true,
            subtitle = stringResource(R.string.trash_delete_one_text, trashed?.details?.task?.title.orEmpty()),
        ) {}
    }
}

/** «⋮» справа от названия: «Восстановить» и «Удалить навсегда» (красным); у «Не буду делать» — «Вернуть в работу» и «Удалить». */
@Composable
private fun TrashTaskMenu(kind: ReadonlyKind, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val colors = AppTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(AppTheme.sizes.checkboxTouch).testTag(TAG_TRASH_TASK_MENU)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.detail_menu), tint = colors.textSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surface) {
            DropdownMenuItem(
                text = { Text(stringResource(kind.first), fontWeight = FontWeight.SemiBold) },
                onClick = {
                    open = false
                    onRestore()
                },
                modifier = Modifier.testTag(TAG_TRASH_TASK_RESTORE),
            )
            DropdownMenuItem(
                text = { Text(stringResource(kind.second), color = colors.danger, fontWeight = FontWeight.SemiBold) },
                onClick = {
                    open = false
                    onDeleteForever()
                },
                modifier = Modifier.testTag(TAG_TRASH_TASK_DELETE),
            )
        }
    }
}

const val TAG_TRASH_TASK_SCRIM = "trash_task_scrim"
const val TAG_TRASH_TASK_CARD = "trash_task_card"
const val TAG_TRASH_TASK_BANNER = "trash_task_banner"
const val TAG_TRASH_TASK_TITLE = "trash_task_title"
const val TAG_TRASH_TASK_MENU = "trash_task_menu"
const val TAG_TRASH_TASK_RESTORE = "trash_task_restore"
const val TAG_TRASH_TASK_DELETE = "trash_task_delete"
const val TAG_TRASH_DELETE_DIALOG = "trash_delete_dialog"
