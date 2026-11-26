package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.SortBy
import com.prostovasei.simpletasks.domain.model.TaskSort

/** Варианты сортировки экрана (#357): в «Предстоящем» — без «Вручную». */
fun sortOptions(upcoming: Boolean): List<SortBy> =
    if (upcoming) listOf(SortBy.TIME, SortBy.PRIORITY, SortBy.CREATED) else listOf(SortBy.DUE, SortBy.PRIORITY, SortBy.MANUAL, SortBy.CREATED)

/** Новая сортировка после нажатия на вариант: выбранный — порядок разворачивается, другой — со стандартным направлением. */
fun TaskSort.pick(by: SortBy): TaskSort = if (this.by == by) flipped() else TaskSort(by)

/** Подпись направления («сначала ранние»). */
fun sortHint(by: SortBy, descending: Boolean): Int = when (by) {
    SortBy.DUE -> if (descending) R.string.sort_hint_due_desc else R.string.sort_hint_due_asc
    SortBy.TIME -> if (descending) R.string.sort_hint_time_desc else R.string.sort_hint_time_asc
    SortBy.PRIORITY -> if (descending) R.string.sort_hint_priority_desc else R.string.sort_hint_priority_asc
    SortBy.CREATED -> if (descending) R.string.sort_hint_created_desc else R.string.sort_hint_created_asc
    SortBy.MANUAL -> R.string.sort_hint_manual
}

private fun sortIcon(by: SortBy): Int = when (by) {
    SortBy.DUE -> R.drawable.ic_sort_due
    SortBy.TIME -> R.drawable.ic_time
    SortBy.PRIORITY -> R.drawable.ic_tool_priority
    SortBy.MANUAL -> R.drawable.ic_sort_manual
    SortBy.CREATED -> R.drawable.ic_sort_created
}

/**
 * Шторка «Сортировка» (#357, выбор PM): варианты со значками, выбранный — фиолетовым с ярлыком направления «↑ сначала ранние»;
 * повторное нажатие разворачивает порядок. Внизу «Сбросить на стандарт» и «Готово». Изменения применяются сразу.
 */
@Composable
fun SortSheet(subtitle: String, sort: TaskSort, default: TaskSort, upcoming: Boolean, onChange: (TaskSort) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss, indication = null, interactionSource = null), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.windowInsetsPadding(WindowInsets.navigationBars).fillMaxWidth().padding(spacing.sm).clip(AppTheme.shapes.extraLarge)
                    .background(colors.surface).clickable(enabled = false) {}.padding(vertical = spacing.sm).testTag(TAG_SORT_SHEET),
            ) {
                Text(stringResource(R.string.sort_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = colors.textPrimary, modifier = Modifier.padding(horizontal = spacing.md))
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = spacing.md).padding(bottom = spacing.xs))
                sortOptions(upcoming).forEach { by ->
                    val on = sort.by == by
                    val tint = if (on) colors.priority.high.content else colors.textPrimary
                    Row(
                        Modifier.fillMaxWidth().height(SizeTokens.SORT_ROW.dp).clickable(role = Role.RadioButton) { onChange(sort.pick(by)) }
                            .semantics { selected = on }.padding(horizontal = spacing.md).testTag("$TAG_SORT_OPTION:${by.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(sortIcon(by)), null, tint = if (on) tint else colors.textSecondary, modifier = Modifier.size(SizeTokens.SORT_ICON.dp))
                        Spacer(Modifier.width(spacing.sm))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(sortLabel(by)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tint)
                                if (default.by == by) {
                                    Text(stringResource(R.string.sort_standard), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary,
                                        modifier = Modifier.padding(start = spacing.xs))
                                }
                            }
                            if (!on || by == SortBy.MANUAL) {
                                Text(stringResource(sortHint(by, if (on) sort.descending else TaskSort.defaultDescending(by))),
                                    style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }
                        if (on && by != SortBy.MANUAL) {
                            Text(
                                "${if (sort.descending) "↓" else "↑"} ${stringResource(sortHint(by, sort.descending))}",
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.priority.high.content,
                                modifier = Modifier.clip(CircleShape).background(colors.priority.high.container).clickable(role = Role.Button) { onChange(sort.flipped()) }
                                    .padding(horizontal = spacing.sm, vertical = spacing.xxs).testTag(TAG_SORT_FLIP),
                            )
                        }
                    }
                }
                if (sort.by != SortBy.MANUAL) {
                    Text(stringResource(R.string.sort_flip_note), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = spacing.md).padding(top = spacing.xxs))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = spacing.sm).padding(top = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onChange(default) }, enabled = sort != default, modifier = Modifier.testTag(TAG_SORT_RESET)) {
                        Text(stringResource(R.string.sort_reset), color = if (sort != default) colors.accent else colors.textSecondary)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.surface)) {
                        Text(stringResource(R.string.sort_ok))
                    }
                }
            }
        }
    }
}

const val TAG_SORT_SHEET = "sort_sheet"
const val TAG_SORT_OPTION = "sort_option"
const val TAG_SORT_FLIP = "sort_flip"
const val TAG_SORT_RESET = "sort_reset"
