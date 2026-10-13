package com.prostovasei.simpletasks.ui.feature.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ColorDot
import com.prostovasei.simpletasks.designsystem.components.ConfirmDeleteDialog
import com.prostovasei.simpletasks.designsystem.components.EmptyState
import com.prostovasei.simpletasks.designsystem.components.NameDialog
import com.prostovasei.simpletasks.designsystem.components.ScreenTitle
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.GroupWithProjects
import com.prostovasei.simpletasks.domain.model.ProjectWithCount

@Composable
fun ListsRoute(onOpenProject: (String) -> Unit, viewModel: ListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ListsEffect.OpenProject -> onOpenProject(effect.projectId)
            }
        }
    }
    ListsScreen(state, viewModel::onIntent, onOpenProject)
}

@Composable
fun ListsScreen(state: ListsState, onIntent: (ListsIntent) -> Unit, onOpenProject: (String) -> Unit) {
    val colors = AppTheme.colors
    Scaffold(containerColor = colors.background, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ScreenTitle(stringResource(R.string.lists_title), Modifier.weight(1f))
                TextButton(onClick = { onIntent(ListsIntent.OpenDialog(ListsDialog.CreateFolder)) }, modifier = Modifier.testTag(TAG_NEW_FOLDER)) {
                    Text(stringResource(R.string.lists_new_folder), color = colors.accent)
                }
                TextButton(onClick = { onIntent(ListsIntent.OpenDialog(ListsDialog.CreateList)) }, modifier = Modifier.testTag(TAG_NEW_LIST)) {
                    Text(stringResource(R.string.lists_new_list), color = colors.accent)
                }
            }
            val tree = state.tree ?: return@Column
            if (tree.groups.isEmpty() && tree.ungrouped.isEmpty()) {
                EmptyState(stringResource(R.string.lists_empty))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    tree.groups.forEach { group ->
                        val collapsed = group.group.id in state.collapsedFolders
                        item(key = "g-${group.group.id}") { FolderRow(group, collapsed, onIntent) }
                        if (!collapsed) {
                            items(group.projects, key = { "p-${it.project.id}" }) { project ->
                                ProjectRow(project, indented = true, onClick = { onOpenProject(project.project.id) })
                            }
                        }
                    }
                    items(tree.ungrouped, key = { "p-${it.project.id}" }) { project ->
                        ProjectRow(project, indented = false, onClick = { onOpenProject(project.project.id) })
                    }
                }
            }
        }
    }
    ListsDialogs(state, onIntent)
}

@Composable
private fun FolderRow(group: GroupWithProjects, collapsed: Boolean, onIntent: (ListsIntent) -> Unit) {
    val colors = AppTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIntent(ListsIntent.ToggleFolder(group.group.id)) }
            .padding(start = AppTheme.spacing.xs)
            .testTag("folder:${group.group.id}"),
    ) {
        Icon(
            imageVector = if (collapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(if (collapsed) R.string.lists_expand else R.string.lists_collapse),
            tint = colors.textSecondary,
        )
        Text(
            text = group.group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).padding(start = AppTheme.spacing.xs),
        )
        Text(
            text = group.projects.sumOf { it.openTaskCount }.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.lists_folder_menu), tint = colors.textSecondary)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename), color = colors.textPrimary) },
                onClick = {
                    menu = false
                    onIntent(ListsIntent.OpenDialog(ListsDialog.RenameFolder(group.group.id, group.group.name)))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = colors.danger) },
                onClick = {
                    menu = false
                    onIntent(ListsIntent.OpenDialog(ListsDialog.DeleteFolder(group.group.id, group.group.name)))
                },
            )
        }
    }
}

@Composable
private fun ProjectRow(item: ProjectWithCount, indented: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indented) spacing.xl else spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.sm)
            .testTag("project:${item.project.id}"),
    ) {
        val dot = parseListColor(item.project.color)
        if (dot != null) ColorDot(dot) else Spacer(Modifier.size(AppTheme.sizes.listDot))
        Text(
            text = item.project.name,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = spacing.sm),
        )
        if (item.openTaskCount > 0) {
            Text(text = item.openTaskCount.toString(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListsDialogs(state: ListsState, onIntent: (ListsIntent) -> Unit) {
    val dismiss = { onIntent(ListsIntent.DismissDialog) }
    val cancel = stringResource(R.string.action_cancel)
    when (val dialog = state.dialog) {
        ListsDialog.None -> Unit
        ListsDialog.CreateList -> {
            var folderId by rememberSaveable { mutableStateOf<String?>(null) }
            NameDialog(
                title = stringResource(R.string.lists_create_list_title),
                initialName = "",
                confirmLabel = stringResource(R.string.action_create),
                cancelLabel = cancel,
                onConfirm = { onIntent(ListsIntent.CreateList(it, folderId)) },
                onDismiss = dismiss,
            ) {
                val groups = state.tree?.groups.orEmpty()
                if (groups.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                        FolderChip(stringResource(R.string.lists_folder_none), folderId == null) { folderId = null }
                        groups.forEach { g -> FolderChip(g.group.name, folderId == g.group.id) { folderId = g.group.id } }
                    }
                }
            }
        }
        ListsDialog.CreateFolder -> NameDialog(
            title = stringResource(R.string.lists_create_folder_title),
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            cancelLabel = cancel,
            onConfirm = { onIntent(ListsIntent.CreateFolder(it)) },
            onDismiss = dismiss,
        )
        is ListsDialog.RenameFolder -> NameDialog(
            title = stringResource(R.string.lists_rename_folder_title),
            initialName = dialog.name,
            confirmLabel = stringResource(R.string.action_save),
            cancelLabel = cancel,
            onConfirm = { onIntent(ListsIntent.RenameFolder(it)) },
            onDismiss = dismiss,
        )
        is ListsDialog.DeleteFolder -> ConfirmDeleteDialog(
            title = stringResource(R.string.lists_delete_folder_title, dialog.name),
            text = stringResource(R.string.lists_delete_folder_text),
            confirmLabel = stringResource(R.string.action_delete),
            cancelLabel = cancel,
            onConfirm = { onIntent(ListsIntent.ConfirmDeleteFolder) },
            onDismiss = dismiss,
        )
    }
}

@Composable
private fun FolderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
        ),
    )
}

const val TAG_NEW_LIST = "lists_new_list"
const val TAG_NEW_FOLDER = "lists_new_folder"
