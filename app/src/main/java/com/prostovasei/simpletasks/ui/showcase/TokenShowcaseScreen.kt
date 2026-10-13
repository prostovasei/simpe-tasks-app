package com.prostovasei.simpletasks.ui.showcase

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.PriorityColors

/**
 * Временный экран Итерации 01: показывает все токены, чтобы проверить палитру на устройстве.
 * Заменяется экраном «Все» в Итерации 04.
 */
@Composable
fun TokenShowcaseScreen(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.showcase_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.showcase_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )

        SectionTitle(R.string.showcase_section_colors)
        Swatch(colors.background, R.string.token_background)
        Swatch(colors.surface, R.string.token_surface)
        Swatch(colors.textPrimary, R.string.token_text_primary)
        Swatch(colors.textSecondary, R.string.token_text_secondary)
        Swatch(colors.accent, R.string.token_accent)
        Swatch(colors.overdue, R.string.token_overdue)
        Swatch(colors.divider, R.string.token_divider)

        SectionTitle(R.string.showcase_section_priorities)
        PriorityBadge(colors.priority.high, R.string.priority_high)
        PriorityBadge(colors.priority.medium, R.string.priority_medium)
        PriorityBadge(colors.priority.low, R.string.priority_low)
        PriorityBadge(colors.priority.none, R.string.priority_none)

        SectionTitle(R.string.showcase_section_preview)
        SampleSectionHeader(R.string.sample_section_overdue, colors.overdue)
        SampleTaskCard(
            title = R.string.sample_task_overdue,
            meta = R.string.sample_task_overdue_meta,
            metaColor = colors.overdue,
            priority = colors.priority.high,
            priorityLabel = R.string.priority_high,
        )
        SampleSectionHeader(R.string.sample_section_today, colors.textSecondary)
        SampleTaskCard(
            title = R.string.sample_task_today,
            meta = R.string.sample_task_today_meta,
            metaColor = colors.textSecondary,
            priority = colors.priority.medium,
            priorityLabel = R.string.priority_medium,
        )
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.textPrimary,
        modifier = Modifier.padding(top = AppTheme.spacing.md),
    )
}

@Composable
private fun Swatch(color: Color, @StringRes label: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizes.swatch)
                .background(color, AppTheme.shapes.medium)
                .border(AppTheme.sizes.hairline, AppTheme.colors.divider, AppTheme.shapes.medium),
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun PriorityBadge(colors: PriorityColors, @StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = colors.content,
        modifier = Modifier
            .background(colors.container, AppTheme.shapes.small)
            .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs),
    )
}

@Composable
private fun SampleSectionHeader(@StringRes text: Int, color: Color) {
    Text(
        text = stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = AppTheme.spacing.xs),
    )
}

@Composable
private fun SampleTaskCard(
    @StringRes title: Int,
    @StringRes meta: Int,
    metaColor: Color,
    priority: PriorityColors,
    @StringRes priorityLabel: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface, AppTheme.shapes.large)
            .padding(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizes.checkbox)
                .border(AppTheme.sizes.checkboxStroke, AppTheme.colors.textSecondary, AppTheme.shapes.small),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(meta),
                style = MaterialTheme.typography.bodySmall,
                color = metaColor,
            )
        }
        PriorityBadge(priority, priorityLabel)
    }
}

@Preview(name = "Светлая", showBackground = true)
@Composable
private fun TokenShowcaseLightPreview() {
    AppTheme(darkTheme = false) { TokenShowcaseScreen() }
}

@Preview(name = "Тёмная", showBackground = true)
@Composable
private fun TokenShowcaseDarkPreview() {
    AppTheme(darkTheme = true) { TokenShowcaseScreen() }
}
