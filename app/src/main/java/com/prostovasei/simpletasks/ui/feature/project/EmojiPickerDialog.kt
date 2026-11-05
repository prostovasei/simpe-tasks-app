package com.prostovasei.simpletasks.ui.feature.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog

/**
 * Выбор эмодзи списка (итерация 47, макет `VariantH2`, решение PM): сетка популярных эмодзи по группам и поле, куда можно вставить
 * любой эмодзи с клавиатуры. Меняется только черновик; список получает эмодзи по «Готово».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPickerDialog(selected: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    var draft by rememberSaveable { mutableStateOf(selected) }
    var custom by rememberSaveable { mutableStateOf("") }
    val glyph = with(LocalDensity.current) { sizes.listGlyphLarge.toSp() }
    SheetDialog(stringResource(R.string.project_emoji_title), TAG_EMOJI_DIALOG, onDismiss, onDone = { onConfirm(draft) }) {
        ListEmoji.POPULAR.forEach { (group, emojis) ->
            Text(
                group,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs, bottom = spacing.xxs),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xxs), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                emojis.forEach { emoji ->
                    val isSelected = emoji == draft
                    Box(
                        Modifier
                            .size(sizes.iconTile)
                            .background(if (isSelected) colors.selected else Color.Transparent, AppTheme.shapes.medium)
                            .border(if (isSelected) sizes.checkboxStroke else sizes.hairline, if (isSelected) colors.textPrimary else Color.Transparent, AppTheme.shapes.medium)
                            .clickable { draft = emoji }
                            .semantics {
                                contentDescription = emoji
                                this.selected = isSelected
                            }
                            .testTag("$TAG_EMOJI_OPTION:$emoji"),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = glyph) }
                }
            }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = { text ->
                custom = text
                // Любой эмодзи с клавиатуры: первый эмодзи из поля сразу становится выбором.
                ListEmoji.leading(text)?.let { draft = it }
            },
            placeholder = { Text(stringResource(R.string.project_emoji_custom), color = colors.inactive) },
            trailingIcon = { Text(draft, fontSize = glyph, fontWeight = FontWeight.Normal) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.divider,
                cursorColor = colors.accent,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm).testTag(TAG_EMOJI_FIELD),
        )
    }
}

const val TAG_EMOJI_DIALOG = "emoji_dialog"
const val TAG_EMOJI_OPTION = "emoji_option"
const val TAG_EMOJI_FIELD = "emoji_field"
