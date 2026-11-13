package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Money

/** Знак рубля после суммы — неразрывный пробел и «₽». */
private const val RUBLE = " ₽"

/**
 * Показывает «42300 ₽», а в поле хранит только «42300»: курсор не заходит за знак рубля. Пустое поле — бледный «0» и тот же «₽»:
 * подсказка — часть того же текста, а не отдельная надпись поверх (0.59.1, #227: отдельная «0 ₽» наезжала на «₽» поля).
 */
private class RubleSuffix(private val placeholder: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = if (text.isEmpty()) {
            buildAnnotatedString {
                withStyle(SpanStyle(color = placeholder)) { append("0") }
                append(RUBLE)
            }
        } else {
            AnnotatedString(text.text + RUBLE)
        }
        return TransformedText(
            shown,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = minOf(offset, text.length)
            },
        )
    }
}

/**
 * Крупная сумма окна финансов: поле во всю ширину, текст по центру, «₽» дописывается к числу, клавиатура цифровая.
 * Ширину поля нельзя задавать через IntrinsicSize: Compose считает её для поля ввода неверно, и сумма пропадает (0.58.1, #223).
 */
@Composable
fun AmountField(value: String, onChange: (String) -> Unit, tag: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    val colors = AppTheme.colors
    val style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = align)
    BasicTextField(
        value = value,
        onValueChange = { onChange(Money.filterInput(it)) },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = RubleSuffix(colors.inactive),
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.sm).testTag(tag),
    )
}

/** Текст в строке окна: значение справа, [placeholder] бледным, пока пусто. */
@Composable
fun FieldInput(value: String, onChange: (String) -> Unit, placeholder: String, tag: String, modifier: Modifier = Modifier, bold: Boolean = false, maxLength: Int = FIELD_MAX) {
    val colors = AppTheme.colors
    val style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = colors.textPrimary, textAlign = TextAlign.End)
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.take(maxLength)) },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth().testTag(tag),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(fontWeight = FontWeight.Normal), color = colors.inactive)
                inner()
            }
        },
    )
}

/** Переключатель из нескольких равных частей на серой подложке: выбранная — белая и жирная («Расход / Доход / Перевод»). */
@Composable
fun <T> Segments(options: List<Pair<T, String>>, selected: T, tag: String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        modifier = modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.selected).padding(spacing.xxs),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AppTheme.sizes.financeChip)
                    .clip(AppTheme.shapes.medium)
                    .background(if (active) colors.surface else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(value) })
                    .testTag("$tag:$value"),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) colors.textPrimary else colors.textSecondary, maxLines = 1)
            }
        }
    }
}

/** Серая кнопка во всю ширину: второе действие окна («В архив», «Новая категория»). */
@Composable
fun SecondaryAction(label: String, tag: String, onClick: () -> Unit, plus: Boolean = false) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.large)
            .background(colors.selected)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppTheme.spacing.sm)
            .testTag(tag),
    ) {
        if (plus) Icon(Icons.Default.Add, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.padding(end = AppTheme.spacing.xs).size(AppTheme.sizes.accountAddIcon))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}

/** Наибольшая длина текста в строке окна (место, комментарий, название). */
const val FIELD_MAX = 80
