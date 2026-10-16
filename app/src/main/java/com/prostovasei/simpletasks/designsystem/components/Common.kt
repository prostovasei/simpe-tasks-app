package com.prostovasei.simpletasks.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/** Заголовок экрана. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    )
}

/** Заголовок секции списка; [color] — например, красный для «Просрочено». */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.textSecondary) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.padding(start = AppTheme.spacing.md, end = AppTheme.spacing.md, top = AppTheme.spacing.md, bottom = AppTheme.spacing.xxs),
    )
}

/** Подзаголовок блока внутри секции («Весь день», «По времени»). */
/**
 * Подпись дня в ленте (итерация 13, макет PM): «ПТ, 16 окт» и белая метка [relative] («Сегодня»).
 * [isOverdue] — прошедший день с невыполненными задачами: подпись красная.
 */
@Composable
fun DayHeader(text: String, relative: String?, isOverdue: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val color = if (isOverdue) colors.overdue else colors.textPrimary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier.padding(start = spacing.sm, top = spacing.sm, bottom = spacing.xxs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        if (relative != null) {
            Text(
                relative,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier
                    .background(colors.surface, AppTheme.shapes.small)
                    .padding(horizontal = spacing.xs, vertical = AppTheme.sizes.hairline),
            )
        }
    }
}

@Composable
fun BlockHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textSecondary,
        modifier = modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xxs),
    )
}

/** Сообщение на пустом экране. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(AppTheme.spacing.xl), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
    }
}

/** Диалог ввода названия (создание и переименование списка или папки). */
@Composable
fun NameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    extraContent: @Composable () -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag(TAG_NAME_FIELD),
                )
                extraContent()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

/** Подтверждение удаления. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

/** Выбор цвета списка из готовых вариантов ([presets] — HEX-строки) или «без цвета». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    presets: List<Pair<String, Color>>,
    selected: String?,
    noColorLabel: String,
    cancelLabel: String,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    presets.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(AppTheme.sizes.colorSwatch)
                                .background(color, CircleShape)
                                .border(AppTheme.sizes.hairline, colors.divider, CircleShape)
                                .clickable { onSelect(hex) }
                                .testTag("color:$hex"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hex.equals(selected, ignoreCase = true)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.onAccent)
                            }
                        }
                    }
                }
                TextButton(onClick = { onSelect(null) }) { Text(noColorLabel) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

const val TAG_NAME_FIELD = "name_field"
