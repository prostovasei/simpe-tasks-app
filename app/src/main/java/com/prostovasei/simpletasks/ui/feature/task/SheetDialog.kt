package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

/**
 * Единое окно шторки задачи (итерация 31, ТЗ PM): по центру экрана над затемнением — заголовок, содержимое
 * (длинное прокручивается) и внизу всегда «Отмена» и «Готово». «Назад» и нажатие мимо окна — как «Отмена».
 * Выбор внутри окна меняет только черновик; задача меняется по [onDone]. Без [onDone] — одна кнопка [dismissLabel]
 * (подсказка «Сначала выберите день»).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetDialog(
    title: String,
    tag: String,
    onDismiss: () -> Unit,
    onDone: (() -> Unit)?,
    subtitle: String? = null,
    dismissLabel: Int = R.string.action_cancel,
    doneLabel: Int = R.string.sheet_done,
    /** Необратимое действие (итерация 39): кнопка красная, а не зелёная. */
    danger: Boolean = false,
    edge: Dp = AppTheme.spacing.lg,
    // Кнопка «Отмена» по умолчанию — то же, что «Назад» и нажатие мимо; окно обновления разводит их (загрузку не обрывать случайно).
    onCancel: () -> Unit = onDismiss,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.large
    // Узкие поля: «Отмена» и «Готово» помещаются и на маленьком экране с крупным шрифтом.
    val buttonPadding = PaddingValues(horizontal = spacing.xs, vertical = spacing.xxs)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = edge, vertical = spacing.lg)
                .widthIn(max = sizes.dialogMaxWidth)
                .fillMaxWidth()
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                .testTag(tag),
        ) {
            Column(Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.xs)) {
                icon?.let { Box(Modifier.padding(bottom = spacing.sm)) { it() } }
                Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium) }
            }
            // Содержимое занимает остаток высоты и прокручивается; кнопки ниже всегда видны.
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = spacing.xxs),
                content = content,
            )
            HorizontalDivider(color = colors.divider)
            // Две кнопки в ряд; если слово не помещается (крупный шрифт на маленьком экране) — вторая уходит на строку ниже,
            // а не рвёт слово посередине.
            FlowRow(
                Modifier.fillMaxWidth().padding(spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = shape,
                    border = BorderStroke(sizes.hairline, colors.divider),
                    contentPadding = buttonPadding,
                    modifier = Modifier.weight(1f).heightIn(min = sizes.toolButton).testTag("$tag:$TAG_DIALOG_CANCEL"),
                ) {
                    Text(stringResource(dismissLabel), color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
                if (onDone != null) {
                    Button(
                        onClick = onDone,
                        shape = shape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (danger) colors.danger else colors.accent, contentColor = colors.onAccent),
                        contentPadding = buttonPadding,
                        modifier = Modifier.weight(1f).heightIn(min = sizes.toolButton).testTag("$tag:$TAG_DIALOG_DONE"),
                    ) {
                        Text(stringResource(doneLabel), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Вид кнопки в [ChoiceDialog]: красная, чёрная, с рамкой или просто текст. */
enum class ChoiceStyle { Danger, Primary, Outline, Plain }

/** Кнопка окна выбора: подпись, вид, тег для тестов, значок слева и действие. */
class Choice(val label: String, val style: ChoiceStyle, val tag: String, @DrawableRes val icon: Int? = null, val onClick: () -> Unit)

/**
 * Окно выбора (итерация 49, макет iteration49.py): по центру — заголовок, пояснение и кнопки столбиком на всю ширину
 * («Удалить серию» / «Не буду делать» / «Отмена»). Нажатие на кнопку сразу выполняет её; «Назад» и нажатие мимо — как «Отмена».
 */
@Composable
fun ChoiceDialog(title: String, text: String, tag: String, choices: List<Choice>, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.large
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = spacing.lg, vertical = spacing.lg)
                .widthIn(max = sizes.dialogMaxWidth)
                .fillMaxWidth()
                .clip(AppTheme.shapes.nav)
                .background(colors.surface)
                .testTag(tag),
        ) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.xs),
            ) {
                Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = spacing.xs))
            }
            Column(Modifier.fillMaxWidth().padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                choices.forEach { choice ->
                    val modifier = Modifier.fillMaxWidth().heightIn(min = sizes.toolButton).testTag("$tag:${choice.tag}")
                    val label: @Composable () -> Unit = {
                        choice.icon?.let {
                            Icon(painterResource(it), null, modifier = Modifier.padding(end = spacing.xs).size(sizes.metaIcon))
                        }
                        Text(choice.label, fontWeight = FontWeight.Bold)
                    }
                    when (choice.style) {
                        ChoiceStyle.Danger, ChoiceStyle.Primary -> Button(
                            onClick = choice.onClick,
                            shape = shape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (choice.style == ChoiceStyle.Danger) colors.danger else colors.accent,
                                contentColor = colors.onAccent,
                            ),
                            modifier = modifier,
                        ) { label() }
                        ChoiceStyle.Outline -> OutlinedButton(
                            onClick = choice.onClick,
                            shape = shape,
                            border = BorderStroke(sizes.hairline, colors.divider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            modifier = modifier,
                        ) { label() }
                        ChoiceStyle.Plain -> TextButton(
                            onClick = choice.onClick,
                            shape = shape,
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary),
                            modifier = modifier,
                        ) { label() }
                    }
                }
            }
        }
    }
}

/**
 * Вариант в окне: название, подпись под ним и [hint] справа (когда сработает напоминание).
 * Один из многих ([multi] = false) — выбранный подсвечен и с галочкой справа; несколько сразу — квадрат-галочка слева.
 */
@Composable
fun DialogOption(
    title: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    hint: String? = null,
    multi: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val select = if (multi) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.dialogRow)
            .then(if (selected && !multi) Modifier.background(colors.selected) else Modifier)
            .then(select)
            .padding(horizontal = spacing.lg, vertical = spacing.xxs)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (multi) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.inactive, checkmarkColor = colors.onAccent),
            )
        }
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
            subtitle?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
        }
        hint?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
        if (!multi && selected) Icon(Icons.Default.Check, null, tint = colors.accent)
    }
}

/** Подпись блока вариантов: «Основные», «Созданные вручную». */
@Composable
fun DialogSection(text: String) {
    val spacing = AppTheme.spacing
    Text(
        text.uppercase(),
        color = AppTheme.colors.textSecondary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.xxs),
    )
}

/** Тонкий разделитель между группами вариантов. */
@Composable
fun DialogDivider() {
    HorizontalDivider(color = AppTheme.colors.divider, modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xxs))
}

/** Текст-подсказка внутри окна (например, «Сначала выберите день»). */
@Composable
fun DialogText(text: String, tag: String? = null) {
    Text(
        text,
        color = AppTheme.colors.textSecondary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xs).then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

const val TAG_DIALOG_DONE = "done"
const val TAG_DIALOG_CANCEL = "cancel"
