package com.prostovasei.simpletasks.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.PriorityColors
import com.prostovasei.simpletasks.domain.model.Priority

/**
 * Плашка задачи в одну строку (итерация 13, макет PM): квадратная галочка (рамка — цвет приоритета),
 * цветной квадратик списка, название, ↻ у повторяющихся; справа [trailing] — время столбиком
 * («16:00» или «16:00 · 17:00»), у списков — день и время.
 * [isCompleting] — задача только что отмечена: зачёркнута и полупрозрачна (SPEC.md §4.1).
 * [isGhost] — будущий повтор: бледный, без галочки. [isOverdue] — рамка галочки и время красные.
 */
@Composable
fun TaskRow(
    title: String,
    priority: Priority,
    listColor: Color?,
    isCompleting: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: (() -> Unit)? = null,
    isRecurring: Boolean = false,
    isGhost: Boolean = false,
    isOverdue: Boolean = false,
    trailing: List<String> = emptyList(),
    @DrawableRes listIcon: Int? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    // Без плавного перехода (решение PM): отмеченная задача сразу бледная.
    val alpha = when {
        isCompleting -> COMPLETING_ALPHA
        isGhost -> GHOST_ALPHA
        else -> 1f
    }
    val ring = when {
        isOverdue -> colors.overdue
        priority == Priority.HIGH -> colors.checkRings.high
        priority == Priority.MEDIUM -> colors.checkRings.medium
        priority == Priority.LOW -> colors.checkRings.low
        else -> colors.inactive
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = spacing.xxs)
            .clip(AppTheme.shapes.medium)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = spacing.xxs, end = spacing.sm, top = sizes.cardGap, bottom = sizes.cardGap)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(sizes.checkboxTouch)
                // У будущего повтора галочки нет: выполнить можно только ближайший раз.
                .then(if (isGhost) Modifier else Modifier.clickable(onClick = onToggle).testTag("$testTag:check")),
            contentAlignment = Alignment.Center,
        ) {
            if (!isGhost) SquareCheck(checked = isCompleting, ring = ring)
        }
        listColor?.let {
            ListSquare(it, listIcon)
            Spacer(Modifier.width(spacing.xs))
        }
        // Название занимает всё свободное место, ↻ — сразу за текстом, время прижато вправо.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleting) colors.textSecondary else colors.textPrimary,
                textDecoration = if (isCompleting) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isRecurring) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.padding(start = spacing.xxs).size(sizes.metaIcon).testTag("$testTag:repeat"),
                )
            }
        }
        if (trailing.isNotEmpty()) Spacer(Modifier.width(spacing.xs))
        TrailingTime(trailing, color = if (isOverdue) colors.overdue else colors.textPrimary)
    }
}

/** Квадратик цвета списка со значком [icon] (итерация 14); [large] — на экране «Списки» и в заголовке списка. */
@Composable
fun ListSquare(color: Color, @DrawableRes icon: Int?, modifier: Modifier = Modifier, large: Boolean = false) {
    val sizes = AppTheme.sizes
    Box(
        modifier = modifier
            .size(if (large) sizes.listSquareLarge else sizes.listSquare)
            .background(color, if (large) AppTheme.shapes.medium else AppTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = AppTheme.colors.onListSquare,
                modifier = Modifier.size(if (large) sizes.listGlyphLarge else sizes.listGlyph),
            )
        }
    }
}

/** Квадратная галочка (макет PM): рамка цвета [ring], отмеченная — залита акцентом. */
@Composable
private fun SquareCheck(checked: Boolean, ring: Color) {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.small
    Box(
        modifier = Modifier
            .size(AppTheme.sizes.checkbox)
            .clip(shape)
            .background(if (checked) colors.accent else colors.surface)
            .border(AppTheme.sizes.checkboxStroke, if (checked) colors.accent else ring, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(AppTheme.sizes.metaIcon))
    }
}

/** Время справа: одна строка или две через точку («16:00 · 17:00» столбиком, плотно — как в макете PM). */
@Composable
private fun TrailingTime(lines: List<String>, color: Color) {
    if (lines.isEmpty()) return
    if (lines.size == 1) {
        Text(lines[0], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        return
    }
    val base = MaterialTheme.typography.labelMedium
    val tight = base.copy(
        fontWeight = FontWeight.Bold,
        lineHeight = base.fontSize,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEachIndexed { index, line ->
            if (index > 0) Text("·", style = tight, color = AppTheme.colors.textSecondary)
            Text(line, style = tight, color = color, maxLines = 1)
        }
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
