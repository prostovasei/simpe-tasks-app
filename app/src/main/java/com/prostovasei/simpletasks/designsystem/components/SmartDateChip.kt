package com.prostovasei.simpletasks.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens

/**
 * Чип срока из названия (итерация 41, бэклог №29): акцентная капсула «Завтра, 10:00» под полем названия;
 * крестик — не разбирать дату в названии этой задачи.
 */
@Composable
fun SmartDateChip(text: String, offLabel: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        modifier
            .height(SizeTokens.SMART_CHIP.dp)
            .clip(CircleShape)
            .background(colors.accentSoft)
            .padding(start = spacing.sm, end = spacing.xxs)
            .testTag(TAG_SMART_CHIP),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Icon(painterResource(R.drawable.ic_widget_all), null, tint = colors.accent, modifier = Modifier.size(SizeTokens.SMART_CHIP_ICON.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.accent, maxLines = 1)
        Box(
            Modifier
                .size(SizeTokens.SMART_CHIP_CLOSE.dp)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .clickable(onClick = onDismiss)
                .semantics { contentDescription = offLabel }
                .testTag(TAG_SMART_CHIP_OFF),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, null, tint = colors.accent, modifier = Modifier.size(SizeTokens.SMART_CHIP_CLOSE_ICON.dp))
        }
    }
}

/** Подсветка фраз срока в поле названия: цвет акцента на светлом акцентном фоне; текст и курсор не меняются. */
class SmartDateHighlight(private val ranges: List<IntRange>, private val style: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = buildAnnotatedString {
            append(text)
            ranges.filter { it.first >= 0 && it.last < text.length }.forEach { addStyle(style, it.first, it.last + 1) }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean = other is SmartDateHighlight && other.ranges == ranges && other.style == style

    override fun hashCode(): Int = ranges.hashCode() * HASH + style.hashCode()

    private companion object {
        const val HASH = 31
    }
}

/** Стиль подсветки фразы срока. */
@Composable
fun smartDateSpan(): SpanStyle = SpanStyle(color = AppTheme.colors.accent, background = AppTheme.colors.accentSoft)

const val TAG_SMART_CHIP = "smart_chip"
const val TAG_SMART_CHIP_OFF = "smart_chip_off"
