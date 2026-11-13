package com.prostovasei.simpletasks.ui.feature.lists

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ConfirmDeleteDialog
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.domain.model.ProjectWithCount
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH

@Composable
fun ListsRoute(
    onOpenProject: (String) -> Unit,
    onOpenInbox: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenCompleted: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenWontDo: () -> Unit = {},
    onOpenSomeday: () -> Unit = {},
    onOpenNoList: () -> Unit = {},
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ListsEffect.OpenProject -> onOpenProject(effect.projectId)
            }
        }
    }
    ListsScreen(state, viewModel::onIntent, onOpenProject, onOpenCompleted, onOpenTrash, onOpenWontDo, onOpenSomeday, onOpenNoList, onOpenInbox, onOpenSearch)
}

/** Строка карточки «Моих списков»: папка или список (в папке — с отступом). */
private sealed interface MineRow {
    val key: String

    data class Folder(val group: GroupWithProjects, val collapsed: Boolean) : MineRow {
        override val key get() = "g-${group.group.id}"
    }

    data class Project(val item: ProjectWithCount, val indented: Boolean) : MineRow {
        override val key get() = "p-${item.project.id}"
    }
}

/**
 * Экран «Списки» (итерация 56, макет iteration56.py, решения PM 09.11.2026): без заголовка, три карточки — системные
 * (📥 💭 📌, их нельзя удалить или изменить), мои списки и папки, архив (✓ ⊗ 🗑). Названия полужирные, число задач — если не 0.
 * Внизу, над нижней панелью, закреплена строка значков: поиск, новая папка, новый список, настройки.
 */
@Composable
fun ListsScreen(
    state: ListsState,
    onIntent: (ListsIntent) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenCompleted: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenWontDo: () -> Unit = {},
    onOpenSomeday: () -> Unit = {},
    onOpenNoList: () -> Unit = {},
    onOpenInbox: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val tree = state.tree
    val mine = buildList {
        tree?.groups?.forEach { group ->
            val collapsed = group.group.id in state.collapsedFolders
            add(MineRow.Folder(group, collapsed))
            if (!collapsed) group.projects.forEach { add(MineRow.Project(it, indented = true)) }
        }
        tree?.ungrouped?.forEach { add(MineRow.Project(it, indented = false)) }
    }
    Scaffold(containerColor = colors.background, contentWindowInsets = WindowInsets(0)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag(TAG_LISTS_SCROLL),
                contentPadding = PaddingValues(top = spacing.md, bottom = sizes.listsTool + spacing.md + spacing.sm),
            ) {
                // Системные: «Входящие» (неразобранное), «Когда-нибудь» (со списком, без даты), «Без списка» (с датой, без списка).
                item(key = "inbox") {
                    CardRow(CardPos.Top, R.string.inbox_title, state.inboxCount, TAG_LISTS_INBOX, onOpenInbox, lead = { ListBadge(ListEmoji.INBOX, large = true) })
                }
                item(key = "someday") {
                    CardRow(CardPos.Middle, R.string.someday_title, state.somedayCount, TAG_SOMEDAY, onOpenSomeday, lead = { ListBadge(ListEmoji.SOMEDAY, large = true) })
                }
                item(key = "no-list") {
                    CardRow(CardPos.Bottom, R.string.no_list_title, state.noListCount, TAG_NO_LIST, onOpenNoList, lead = { ListBadge(ListEmoji.NO_LIST, large = true) })
                }
                item(key = "gap-1") { Spacer(Modifier.height(spacing.sm)) }
                if (tree != null && mine.isEmpty()) {
                    item(key = "empty") { EmptyState(stringResource(R.string.lists_empty)) }
                } else {
                    itemsIndexed(mine, key = { _, row -> row.key }) { index, row ->
                        val pos = cardPos(index, mine.size)
                        when (row) {
                            is MineRow.Folder -> FolderRow(row.group, row.collapsed, pos, onIntent)
                            is MineRow.Project -> ProjectRow(row.item, row.indented, pos) { onOpenProject(row.item.project.id) }
                        }
                    }
                    item(key = "gap-2") { Spacer(Modifier.height(spacing.sm)) }
                }
                // Архив: «Выполненные», «Не буду делать», «Корзина».
                item(key = "completed") {
                    CardRow(CardPos.Top, R.string.completed_title, 0, TAG_COMPLETED, onOpenCompleted, lead = { RowIcon(Icons.Default.Check) })
                }
                item(key = "wont-do") {
                    CardRow(CardPos.Middle, R.string.wontdo_title, state.wontDoCount, TAG_WONT_DO, onOpenWontDo, lead = { RowIcon(R.drawable.ic_wont_do) })
                }
                item(key = "trash") {
                    CardRow(CardPos.Bottom, R.string.trash_title, state.trashCount, TAG_TRASH, onOpenTrash, countTag = TAG_TRASH_COUNT, lead = { RowIcon(R.drawable.ic_tool_delete) })
                }
            }
            ListsToolbar(
                onSearch = onOpenSearch,
                onNewFolder = { onIntent(ListsIntent.OpenDialog(ListsDialog.CreateFolder)) },
                onNewList = { onIntent(ListsIntent.OpenDialog(ListsDialog.CreateList)) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    ListsDialogs(state, onIntent)
}

/** Место строки в карточке: скругляются только верх первой и низ последней строки; линия — под всеми, кроме последней. */
private enum class CardPos { Top, Middle, Bottom, Single }

private fun cardPos(index: Int, count: Int) = when {
    count == 1 -> CardPos.Single
    index == 0 -> CardPos.Top
    index == count - 1 -> CardPos.Bottom
    else -> CardPos.Middle
}

@Composable
private fun cardShape(pos: CardPos): Shape {
    val r = AppTheme.shapes.large
    return when (pos) {
        CardPos.Single -> r
        CardPos.Top -> r.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
        CardPos.Bottom -> r.copy(topStart = ZeroCornerSize, topEnd = ZeroCornerSize)
        CardPos.Middle -> RectangleShape
    }
}

/** Строка карточки: значок или эмодзи, название SemiBold, число задач (при нуле нет), [trailing] — стрелка и «⋮» папки. */
@Composable
private fun CardRow(
    pos: CardPos,
    title: String,
    count: Int,
    tag: String,
    onClick: () -> Unit,
    countTag: String = "$tag:count",
    indented: Boolean = false,
    lead: @Composable () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val start = if (indented) spacing.md + spacing.lg else spacing.md
    Box(
        Modifier
            .padding(horizontal = spacing.sm)
            .fillMaxWidth()
            .clip(cardShape(pos))
            .background(colors.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sizes.listRow)
                .clickable(onClick = onClick)
                .padding(start = start, end = spacing.md)
                .testTag(tag),
        ) {
            Box(Modifier.size(sizes.listSquareLarge), contentAlignment = Alignment.Center) { lead() }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = spacing.sm),
            )
            if (count > 0) {
                Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, modifier = Modifier.testTag(countTag))
            }
            trailing()
        }
        if (pos == CardPos.Top || pos == CardPos.Middle) {
            HorizontalDivider(
                color = colors.divider,
                thickness = sizes.hairline,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = start + sizes.listSquareLarge + spacing.sm),
            )
        }
    }
}

@Composable
private fun CardRow(
    pos: CardPos,
    @StringRes title: Int,
    count: Int,
    tag: String,
    onClick: () -> Unit,
    countTag: String = "$tag:count",
    lead: @Composable () -> Unit,
) = CardRow(pos, stringResource(title), count, tag, onClick, countTag, lead = lead)

@Composable
private fun RowIcon(@DrawableRes icon: Int) = Icon(painterResource(icon), contentDescription = null, tint = AppTheme.colors.textSecondary)

@Composable
private fun RowIcon(icon: ImageVector) = Icon(icon, contentDescription = null, tint = AppTheme.colors.textSecondary)

/** Папка: эмодзи (итерация 56), название, число задач её списков, стрелка «свернуть» и «⋮» (Изменить / Удалить). */
@Composable
private fun FolderRow(group: GroupWithProjects, collapsed: Boolean, pos: CardPos, onIntent: (ListsIntent) -> Unit) {
    val colors = AppTheme.colors
    var menu by remember { mutableStateOf(false) }
    val toggle = stringResource(if (collapsed) R.string.lists_expand else R.string.lists_collapse)
    CardRow(
        pos = pos,
        title = group.group.name,
        count = group.projects.sumOf { it.openTaskCount },
        tag = "folder:${group.group.id}",
        onClick = { onIntent(ListsIntent.ToggleFolder(group.group.id)) },
        lead = { ListBadge(group.group.emojiShown, large = true) },
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = toggle,
            tint = colors.textSecondary,
            modifier = Modifier.padding(start = AppTheme.spacing.xs).rotate(if (collapsed) COLLAPSED_ANGLE else 0f),
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("folder_menu:${group.group.id}")) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.lists_folder_menu), tint = colors.textSecondary)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit), color = colors.textPrimary) },
                    onClick = {
                        menu = false
                        onIntent(ListsIntent.OpenDialog(ListsDialog.EditFolder(group.group.id, group.group.name, group.group.emoji)))
                    },
                    modifier = Modifier.testTag(TAG_FOLDER_EDIT),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = colors.danger) },
                    onClick = {
                        menu = false
                        onIntent(ListsIntent.OpenDialog(ListsDialog.DeleteFolder(group.group.id, group.group.name)))
                    },
                    modifier = Modifier.testTag(TAG_FOLDER_DELETE),
                )
            }
        }
    }
}

@Composable
private fun ProjectRow(item: ProjectWithCount, indented: Boolean, pos: CardPos, onClick: () -> Unit) = CardRow(
    pos = pos,
    title = item.project.name,
    count = item.openTaskCount,
    tag = "project:${item.project.id}",
    onClick = onClick,
    indented = indented,
    lead = { ListBadge(item.project.emojiShown, large = true) },
)

/** Строка значков над нижней панелью (итерация 56): поиск, новая папка, новый список — всегда на месте. Настройки — из меню кнопки модуля (#241). */
@Composable
private fun ListsToolbar(onSearch: () -> Unit, onNewFolder: () -> Unit, onNewList: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background, colors.background)))
            .padding(start = spacing.sm, end = spacing.sm, top = spacing.md, bottom = spacing.xs)
            .testTag(TAG_LISTS_TOOLBAR),
    ) {
        ToolButton(R.drawable.ic_nav_search, R.string.search_hint, TAG_LISTS_SEARCH, onSearch)
        ToolButton(R.drawable.ic_folder_plus, R.string.lists_new_folder_action, TAG_NEW_FOLDER, onNewFolder)
        ToolButton(R.drawable.ic_list_plus, R.string.lists_new_list_action, TAG_NEW_LIST, onNewList)
    }
}

@Composable
private fun RowScope.ToolButton(@DrawableRes icon: Int, @StringRes label: Int, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val description = stringResource(label)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(AppTheme.sizes.listsTool)
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) { Icon(painterResource(icon), contentDescription = null, tint = colors.textPrimary) }
}

@Composable
private fun ListsDialogs(state: ListsState, onIntent: (ListsIntent) -> Unit) {
    val dismiss = { onIntent(ListsIntent.DismissDialog) }
    when (val dialog = state.dialog) {
        ListsDialog.None -> Unit
        ListsDialog.CreateList -> ListEditDialog(
            title = stringResource(R.string.lists_create_list_title),
            doneLabel = R.string.action_create,
            initialName = "",
            initialEmoji = null,
            folders = state.tree?.groups?.map { it.group }.orEmpty(),
            onConfirm = { name, emoji, folderId -> onIntent(ListsIntent.CreateList(name, folderId, emoji)) },
            onDismiss = dismiss,
        )
        ListsDialog.CreateFolder -> ListEditDialog(
            title = stringResource(R.string.lists_create_folder_title),
            doneLabel = R.string.action_create,
            initialName = "",
            initialEmoji = null,
            folders = null,
            onConfirm = { name, emoji, _ -> onIntent(ListsIntent.CreateFolder(name, emoji)) },
            onDismiss = dismiss,
        )
        is ListsDialog.EditFolder -> ListEditDialog(
            title = stringResource(R.string.lists_edit_folder_title),
            doneLabel = R.string.action_save,
            initialName = dialog.name,
            initialEmoji = dialog.emoji,
            folders = null,
            onConfirm = { name, emoji, _ -> onIntent(ListsIntent.UpdateFolder(name, emoji)) },
            onDismiss = dismiss,
        )
        is ListsDialog.DeleteFolder -> ConfirmDeleteDialog(
            title = stringResource(R.string.lists_delete_folder_title, dialog.name),
            text = stringResource(R.string.lists_delete_folder_text),
            confirmLabel = stringResource(R.string.action_delete),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onIntent(ListsIntent.ConfirmDeleteFolder) },
            onDismiss = dismiss,
        )
    }
}

/** Свёрнутая папка — стрелка смотрит вправо. */
private const val COLLAPSED_ANGLE = -90f

const val TAG_LISTS_SEARCH = "lists_search"
const val TAG_LISTS_INBOX = "lists_inbox"
const val TAG_LISTS_SCROLL = "lists_scroll"
const val TAG_LISTS_TOOLBAR = "lists_toolbar"
const val TAG_COMPLETED = "lists_completed"
const val TAG_WONT_DO = "lists_wont_do"
const val TAG_SOMEDAY = "lists_someday"
const val TAG_NO_LIST = "lists_no_list"
const val TAG_TRASH_COUNT = "lists_trash_count"
const val TAG_FOLDER_EDIT = "folder_edit"
const val TAG_FOLDER_DELETE = "folder_delete"
const val TAG_NEW_LIST = "lists_new_list"
const val TAG_NEW_FOLDER = "lists_new_folder"
