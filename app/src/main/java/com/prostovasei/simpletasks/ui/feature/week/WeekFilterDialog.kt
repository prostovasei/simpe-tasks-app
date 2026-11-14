package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ProjectTree
import com.prostovasei.simpletasks.domain.model.WeekFilter
import com.prostovasei.simpletasks.domain.model.WeekFilterMode

const val TAG_WEEK_FILTER = "week_filter"
const val TAG_WEEK_FILTER_SHOW = "week_filter_show"
const val TAG_WEEK_FILTER_HIDE = "week_filter_hide"
const val TAG_WEEK_FILTER_NONE = "week_filter_none"
const val TAG_WEEK_FILTER_LIST = "week_filter_list"
const val TAG_WEEK_FILTER_GROUP = "week_filter_group"
const val TAG_WEEK_FILTER_RESET = "week_filter_reset"
const val TAG_WEEK_FILTER_DONE = "week_filter_done"

/** Окно фильтра вида «Неделя» (#243, макет `design/approved/243`): общее для виджета и страницы приложения. */
@Composable
fun WeekFilterDialog(tree: ProjectTree?, filter: WeekFilter, onApply: (WeekFilter) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeekFilterPanel(tree, filter, onApply)
    }
}

@Composable
fun WeekFilterPanel(tree: ProjectTree?, filter: WeekFilter, onApply: (WeekFilter) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var mode by remember(filter) { mutableStateOf(filter.mode) }
    var ids by remember(filter) { mutableStateOf(filter.ids) }

    fun toggle(vararg keys: String, on: Boolean) {
        ids = if (on) ids + keys else ids - keys.toSet()
    }

    Column(
        modifier = Modifier
            .padding(spacing.lg)
            .fillMaxWidth()
            .background(colors.surface, AppTheme.shapes.extraLarge)
            .padding(vertical = spacing.md)
            .testTag(TAG_WEEK_FILTER),
    ) {
        Text(
            stringResource(R.string.week_filter_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm)
                .background(colors.background, AppTheme.shapes.medium).padding(spacing.xxs),
        ) {
            ModeButton(R.string.week_filter_show, mode == WeekFilterMode.SHOW, TAG_WEEK_FILTER_SHOW, Modifier.weight(1f)) { mode = WeekFilterMode.SHOW }
            ModeButton(R.string.week_filter_hide, mode == WeekFilterMode.HIDE, TAG_WEEK_FILTER_HIDE, Modifier.weight(1f)) { mode = WeekFilterMode.HIDE }
        }
        Column(Modifier.heightIn(max = AppTheme.sizes.pickerMaxHeight).verticalScroll(rememberScrollState())) {
            if (tree?.inbox != null) {
                val key = WeekFilter.NO_LIST
                FilterRow(null, stringResource(R.string.week_filter_no_list), state(key in ids), 0, TAG_WEEK_FILTER_NONE) { toggle(key, on = key !in ids) }
            }
            tree?.groups?.forEach { group ->
                val keys = group.projects.map { it.project.id }
                val marked = keys.count { it in ids }
                val groupState = when {
                    keys.isEmpty() || marked == 0 -> ToggleableState.Off
                    marked == keys.size -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
                FilterRow(group.group.emoji ?: DEFAULT_FOLDER_EMOJI, group.group.name, groupState, 0, "$TAG_WEEK_FILTER_GROUP:${group.group.id}") {
                    toggle(*keys.toTypedArray(), on = groupState != ToggleableState.On)
                }
                group.projects.forEach { item ->
                    val id = item.project.id
                    FilterRow(item.project.emojiShown, item.project.name, state(id in ids), 1, "$TAG_WEEK_FILTER_LIST:$id") { toggle(id, on = id !in ids) }
                }
            }
            tree?.ungrouped?.forEach { item ->
                val id = item.project.id
                FilterRow(item.project.emojiShown, item.project.name, state(id in ids), 0, "$TAG_WEEK_FILTER_LIST:$id") { toggle(id, on = id !in ids) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.week_filter_reset),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                modifier = Modifier.clickable { ids = emptySet() }.padding(spacing.sm).testTag(TAG_WEEK_FILTER_RESET),
            )
            Text(
                stringResource(R.string.week_filter_done),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onAccent,
                modifier = Modifier
                    .background(colors.accent, AppTheme.shapes.medium)
                    .clickable { onApply(WeekFilter(mode, ids)) }
                    .padding(horizontal = spacing.xl, vertical = spacing.sm)
                    .testTag(TAG_WEEK_FILTER_DONE),
            )
        }
    }
}

private fun state(on: Boolean) = if (on) ToggleableState.On else ToggleableState.Off

private const val DEFAULT_FOLDER_EMOJI = "📁"

@Composable
private fun ModeButton(label: Int, selected: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) colors.onAccent else colors.textSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .background(if (selected) colors.accent else colors.background, AppTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spacing.sm)
            .testTag(tag),
    )
}

@Composable
private fun FilterRow(emoji: String?, title: String, state: ToggleableState, level: Int, tag: String, onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = spacing.lg + spacing.lg * level, end = spacing.lg, top = spacing.xxs, bottom = spacing.xxs)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        TriStateCheckbox(state = state, onClick = null)
        emoji?.let { ListBadge(it) }
        Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textPrimary, modifier = Modifier.weight(1f))
    }
}
