package com.prostovasei.simpletasks.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.PriorityColors
import com.prostovasei.simpletasks.domain.model.Priority

/**
 * Строка задачи: чекбокс, название, подпись (срок · список), плашка приоритета.
 * [isCompleting] — задача только что отмечена: зачёркнута и полупрозрачна (alpha 0.4, SPEC.md §4.1).
 * [isRecurring] — значок повтора после подписи. [isGhost] — будущий повтор: бледный, без галочки.
 */
@Composable
fun TaskRow(
    title: String,
    priority: Priority,
    meta: String?,
    metaColor: Color?,
    listColor: Color?,
    isCompleting: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: (() -> Unit)? = null,
    isRecurring: Boolean = false,
    isGhost: Boolean = false,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val alpha by animateFloatAsState(
        when {
            isCompleting -> COMPLETING_ALPHA
            isGhost -> GHOST_ALPHA
            else -> 1f
        },
        label = "taskAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = spacing.xs, vertical = spacing.xxs)
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(end = spacing.sm, top = spacing.xxs, bottom = spacing.xxs)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // У будущего повтора галочки нет: выполнить можно только ближайший раз.
        if (isGhost) Box(Modifier.minimumInteractiveComponentSize()) else Checkbox(
            checked = isCompleting,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.textSecondary,
                checkmarkColor = colors.onAccent,
            ),
            modifier = Modifier.testTag("$testTag:check"),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                textDecoration = if (isCompleting) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!meta.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    listColor?.let { ColorDot(it) }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = metaColor ?: colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isRecurring) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = metaColor ?: colors.textSecondary,
                            modifier = Modifier.size(AppTheme.sizes.metaIcon).testTag("$testTag:repeat"),
                        )
                    }
                }
            }
        }
        PriorityBadge(priority)
    }
}

/** Плашка приоритета P1–P3 (SPEC.md §3). Для «без приоритета» не рисуется — меньше визуального шума. */
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val palette = AppTheme.colors.priority
    val (label, colors) = when (priority) {
        Priority.HIGH -> "P1" to palette.high
        Priority.MEDIUM -> "P2" to palette.medium
        Priority.LOW -> "P3" to palette.low
        Priority.NONE -> return
    }
    BadgeText(label, colors, modifier)
}

@Composable
private fun BadgeText(label: String, colors: PriorityColors, modifier: Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colors.content,
        modifier = modifier
            .background(colors.container, AppTheme.shapes.small)
            .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs),
    )
}

/** Цветная точка списка. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AppTheme.sizes.listDot)
            .background(color, CircleShape),
    )
}

private const val COMPLETING_ALPHA = 0.4f
private const val GHOST_ALPHA = 0.5f
