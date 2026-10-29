package com.prostovasei.simpletasks.ui.feature.bulk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ActionRibbon
import com.prostovasei.simpletasks.designsystem.components.RibbonAction
import com.prostovasei.simpletasks.designsystem.components.SelectionTopBar
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.feature.task.CalendarDialog
import com.prostovasei.simpletasks.ui.feature.task.PriorityDialog
import com.prostovasei.simpletasks.ui.feature.task.ProjectDialog
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog

/** Выбранные задачи экрана (итерация 40). Пусто — режим выбора выключен. */
@Stable
class SelectionState(initial: Set<String> = emptySet()) {
    var ids by mutableStateOf(initial)
        private set

    val active: Boolean get() = ids.isNotEmpty()

    fun toggle(id: String) {
        ids = if (id in ids) ids - id else ids + id
    }

    fun selectAll(all: Collection<String>) {
        ids = all.toSet()
    }

    fun clear() {
        ids = emptySet()
    }

    /** Задача ушла с экрана (выполнили, удалили, настал новый день) — из выбора тоже. */
    fun retain(present: Set<String>) {
        if (!present.containsAll(ids)) ids = ids intersect present
    }
}

@Composable
fun rememberSelection(): SelectionState =
    rememberSaveable(saver = listSaver({ it.ids.toList() }, { SelectionState(it.toSet()) })) { SelectionState() }

/** Включён ли режим выбора на открытом экране: приложение прячет нижнюю панель — её место занимает лента действий. */
val LocalSelectionMode = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** Массовые действия приложения; null — их нет (экран открыт вне приложения, например в тесте). */
val LocalBulkActions = staticCompositionLocalOf<BulkActionsHost?> { null }

/** Задача, которую можно выбрать: [isDone] — выполнена (для неё «Вернуть», а не «Выполнить»). */
data class Selectable(val id: String, val isDone: Boolean)

/** Где выбор: обычные задачи или корзина (там своя лента: «Восстановить», «Удалить навсегда»). */
enum class SelectionScope { Tasks, Trash }

private enum class BulkDialog { None, Day, List, Priority, DeleteForever }

/** Отступ снизу у списка в режиме выбора: последняя задача прокручивается выше ленты действий. */
@Composable
fun selectionBottomInset(selection: SelectionState): Dp = if (selection.active) AppTheme.sizes.navBar + AppTheme.spacing.lg else AppTheme.spacing.xs

/**
 * Режим выбора поверх экрана (итерация 40, бэклог №37): сверху «✕ Выбрано: N» и «Выбрать все», снизу лента действий.
 * Действие выполняется для подходящих задач (выполнить — для активных, вернуть — для выполненных), режим закрывается,
 * подсказку «… · Отмена» показывает приложение. «Назад» тоже закрывает режим.
 */
@Composable
fun SelectionFrame(
    selection: SelectionState,
    selectable: List<Selectable>,
    modifier: Modifier = Modifier,
    scope: SelectionScope = SelectionScope.Tasks,
    content: @Composable BoxScope.() -> Unit,
) {
    val bulk = LocalBulkActions.current
    val mode = LocalSelectionMode.current
    val present = selectable.map { it.id }.toSet()
    LaunchedEffect(present) { selection.retain(present) }
    val active = selection.active
    LaunchedEffect(active) { mode?.value = active }
    DisposableEffect(mode) { onDispose { mode?.value = false } }
    BackHandler(enabled = active) { selection.clear() }
    var dialog by remember { mutableStateOf(BulkDialog.None) }
    val chosen = selectable.filter { it.id in selection.ids }
    val all = chosen.map { it.id }
    val open = chosen.filterNot { it.isDone }.map { it.id }
    val done = chosen.filter { it.isDone }.map { it.id }
    val act: (BulkActions.() -> Unit) -> Unit = { action ->
        bulk?.actions?.action()
        selection.clear()
    }
    Box(modifier.fillMaxSize()) {
        content()
        if (active) {
            SelectionTopBar(all.size, onClose = selection::clear, onSelectAll = { selection.selectAll(present) }, modifier = Modifier.align(Alignment.TopCenter))
            val actions = when (scope) {
                SelectionScope.Tasks -> listOf(
                    RibbonAction(R.drawable.ic_node_check, stringResource(R.string.bulk_complete), TAG_BULK_COMPLETE, enabled = open.isNotEmpty()) { act { complete(open) } },
                    RibbonAction(R.drawable.ic_popup_tomorrow, stringResource(R.string.bulk_tomorrow), TAG_BULK_TOMORROW, enabled = open.isNotEmpty()) { act { toTomorrow(open) } },
                    RibbonAction(R.drawable.ic_widget_all, stringResource(R.string.bulk_other_day), TAG_BULK_DAY, enabled = open.isNotEmpty()) { dialog = BulkDialog.Day },
                    RibbonAction(R.drawable.ic_list_inbox, stringResource(R.string.bulk_move), TAG_BULK_MOVE) { dialog = BulkDialog.List },
                    RibbonAction(R.drawable.ic_tool_priority, stringResource(R.string.bulk_priority), TAG_BULK_PRIORITY) { dialog = BulkDialog.Priority },
                    RibbonAction(R.drawable.ic_snack_restore, stringResource(R.string.bulk_reopen), TAG_BULK_REOPEN, enabled = done.isNotEmpty()) { act { reopen(done) } },
                    RibbonAction(R.drawable.ic_tool_delete, stringResource(R.string.bulk_delete), TAG_BULK_DELETE, danger = true) { act { delete(all) } },
                )
                SelectionScope.Trash -> listOf(
                    RibbonAction(R.drawable.ic_snack_restore, stringResource(R.string.bulk_restore), TAG_BULK_RESTORE) { act { restoreFromTrash(all) } },
                    RibbonAction(R.drawable.ic_tool_delete, stringResource(R.string.bulk_delete_forever), TAG_BULK_DELETE_FOREVER, danger = true) { dialog = BulkDialog.DeleteForever },
                )
            }
            ActionRibbon(actions, Modifier.align(Alignment.BottomCenter))
        }
    }
    val close = { dialog = BulkDialog.None }
    when (dialog) {
        BulkDialog.None -> Unit
        BulkDialog.Day -> CalendarDialog(today = bulk?.today ?: java.time.LocalDate.now(), selected = null, onPick = { date ->
            close()
            act { toDay(open, date) }
        }, onDismiss = close)
        BulkDialog.List -> {
            val options = bulk?.projectOptions?.collectAsStateWithLifecycle()?.value.orEmpty()
            ProjectDialog(projectId = "", projects = options, onConfirm = { projectId ->
                close()
                val option = options.firstOrNull { it.id == projectId } ?: return@ProjectDialog
                act { moveTo(all, projectId, option.name.takeUnless { option.isInbox }) }
            }, onDismiss = close)
        }
        BulkDialog.Priority -> PriorityDialog(Priority.NONE, onConfirm = { priority ->
            close()
            act { setPriority(all, priority) }
        }, onDismiss = close)
        BulkDialog.DeleteForever -> SheetDialog(
            title = stringResource(R.string.bulk_delete_forever_title),
            tag = TAG_BULK_DELETE_FOREVER_DIALOG,
            onDismiss = close,
            onDone = {
                close()
                act { deleteForever(all) }
            },
            doneLabel = R.string.trash_delete_confirm,
            danger = true,
            subtitle = stringResource(R.string.bulk_delete_forever_text, all.size),
        ) {}
    }
}

const val TAG_BULK_COMPLETE = "bulk_complete"
const val TAG_BULK_TOMORROW = "bulk_tomorrow"
const val TAG_BULK_DAY = "bulk_day"
const val TAG_BULK_MOVE = "bulk_move"
const val TAG_BULK_PRIORITY = "bulk_priority"
const val TAG_BULK_REOPEN = "bulk_reopen"
const val TAG_BULK_DELETE = "bulk_delete"
const val TAG_BULK_RESTORE = "bulk_restore"
const val TAG_BULK_DELETE_FOREVER = "bulk_delete_forever"
const val TAG_BULK_DELETE_FOREVER_DIALOG = "bulk_delete_forever_dialog"
