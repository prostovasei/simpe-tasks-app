package com.prostovasei.simpletasks.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
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

/**
 * Подпись блока «Просрочено · N» со стрелкой «свернуть» и значком «перенести на сегодня» рядом (итерация 37).
 * [onMoveToday] = null — переносить нечего, значка нет.
 */
@Composable
fun OverdueHeader(
    text: String,
    collapsed: Boolean,
    foldDescription: String,
    moveDescription: String,
    onToggle: () -> Unit,
    onMoveToday: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = spacing.sm, top = spacing.xs).testTag(TAG_OVERDUE_HEADER),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClickLabel = foldDescription, onClick = onToggle).padding(vertical = spacing.xs).testTag(TAG_OVERDUE_TOGGLE),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.overdue)
            Icon(
                painterResource(if (collapsed) R.drawable.ic_fold_closed else R.drawable.ic_fold_open),
                contentDescription = foldDescription,
                tint = colors.overdue,
                modifier = Modifier.padding(start = spacing.xxs).size(AppTheme.sizes.icon),
            )
        }
        if (onMoveToday != null) {
            Icon(
                painterResource(R.drawable.ic_move_today),
                contentDescription = moveDescription,
                tint = colors.overdue,
                modifier = Modifier
                    .clickable(onClickLabel = moveDescription, onClick = onMoveToday)
                    .padding(spacing.xs)
                    .size(AppTheme.sizes.icon)
                    .testTag(TAG_OVERDUE_MOVE),
            )
        }
    }
}

const val TAG_OVERDUE_HEADER = "overdue_header"
const val TAG_OVERDUE_TOGGLE = "overdue_toggle"
const val TAG_OVERDUE_MOVE = "overdue_move"

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

/**
 * Пустой экран со списком задач (итерация 15, вариант D2 из прототипа PM): бледные силуэты плашек,
 * первая — пунктирная подсказка «Нажмите «+» внизу», в правом нижнем углу — стрелка к «+».
 * [arrowLift] — отступ стрелки снизу: на экране списка «+» плавает над контентом.
 */
@Composable
fun EmptyTasks(title: String, hint: String, modifier: Modifier = Modifier, arrowLift: Dp = AppTheme.spacing.xxs) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Box(modifier.fillMaxSize().testTag(TAG_EMPTY_TASKS)) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(sizes.cardGap),
        ) {
            DashedHint(title, hint)
            GHOST_WIDTHS.forEach { GhostRow(it) }
        }
        EmptyArrow(Modifier.align(Alignment.BottomEnd).padding(end = spacing.lg, bottom = arrowLift).size(sizes.emptyArrow))
    }
}

@Composable
private fun DashedHint(title: String, hint: String) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.xxs)
            .drawBehind {
                val stroke = sizes.checkboxStroke.toPx()
                val dash = sizes.emptyDash.toPx()
                drawRoundRect(
                    color = colors.inactive,
                    cornerRadius = CornerRadius(spacing.sm.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
            .padding(horizontal = spacing.md, vertical = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

/** Бледный силуэт плашки: рамка галочки и серая полоска вместо названия. */
@Composable
private fun GhostRow(width: Float) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizes.ghostRow)
            .background(colors.surface, AppTheme.shapes.medium)
            .padding(horizontal = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(Modifier.size(sizes.checkbox).border(sizes.checkboxStroke, colors.divider, AppTheme.shapes.small))
        Box(Modifier.fillMaxWidth(width).height(sizes.ghostBar).background(colors.divider, CircleShape))
    }
}

/** Изогнутая стрелка вниз-вправо — к кнопке «+». */
@Composable
private fun EmptyArrow(modifier: Modifier) {
    val color = AppTheme.colors.accent
    val stroke = AppTheme.sizes.checkboxStroke
    Canvas(modifier) {
        val u = size.width / ARROW_GRID
        val style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val curve = Path().apply {
            moveTo(6 * u, 6 * u)
            cubicTo(26 * u, 10 * u, 40 * u, 24 * u, 44 * u, 46 * u)
        }
        val head = Path().apply {
            moveTo(36 * u, 40 * u)
            lineTo(44 * u, 48 * u)
            lineTo(50 * u, 38 * u)
        }
        drawPath(curve, color, style = style)
        drawPath(head, color, style = style)
    }
}

/** Длина полосок «названия» в силуэтах плашек (доля ширины). */
private val GHOST_WIDTHS = listOf(0.7f, 0.5f, 0.62f)

/** Сетка рисунка стрелки: координаты пути заданы в квадрате 60×60. */
private const val ARROW_GRID = 60f

const val TAG_EMPTY_TASKS = "empty_tasks"

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

/** Выбор значка списка (итерация 14): плитки цвета списка с белым значком или «без значка». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerDialog(
    title: String,
    icons: List<Pair<String, Int>>,
    selected: String?,
    color: Color,
    noIconLabel: String,
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
                    icons.forEach { (key, icon) ->
                        val isSelected = key == selected
                        Box(
                            modifier = Modifier
                                .size(AppTheme.sizes.iconTile)
                                .background(color, AppTheme.shapes.medium)
                                .border(
                                    if (isSelected) AppTheme.sizes.checkboxStroke else AppTheme.sizes.hairline,
                                    if (isSelected) colors.textPrimary else colors.divider,
                                    AppTheme.shapes.medium,
                                )
                                .clickable { onSelect(key) }
                                .testTag("icon:$key"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(icon), contentDescription = key, tint = colors.onListSquare)
                        }
                    }
                }
                TextButton(onClick = { onSelect(null) }) { Text(noIconLabel, color = colors.textPrimary) }
            }
        },
        confirmButton = {},
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
