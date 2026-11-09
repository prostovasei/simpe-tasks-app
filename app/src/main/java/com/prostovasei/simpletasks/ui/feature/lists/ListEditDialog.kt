package com.prostovasei.simpletasks.ui.feature.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.TAG_NAME_FIELD
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.domain.model.ProjectGroup
import com.prostovasei.simpletasks.ui.feature.project.EmojiPickerDialog
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog

/**
 * Окно создания списка и папки и правки папки (итерация 56, макет iteration56.py, доска «Окно создания»): слева крупная плашка
 * эмодзи (нажатие — все эмодзи, [EmojiPickerDialog]), справа название жирным; ниже быстрый выбор эмодзи и «…», у списка — папка.
 * [folders] = null — без выбора папки. [onConfirm] получает название без пробелов по краям, эмодзи (📁 → null) и папку.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEditDialog(
    title: String,
    doneLabel: Int,
    initialName: String,
    initialEmoji: String?,
    folders: List<ProjectGroup>?,
    onConfirm: (name: String, emoji: String?, folderId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var name by rememberSaveable { mutableStateOf(initialName) }
    var emoji by rememberSaveable { mutableStateOf(initialEmoji ?: ListEmoji.DEFAULT) }
    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    SheetDialog(
        title = title,
        tag = TAG_LIST_EDIT_DIALOG,
        onDismiss = onDismiss,
        onDone = { onConfirm(name.trim(), emoji.takeIf { it != ListEmoji.DEFAULT }, folderId) },
        doneLabel = doneLabel,
        doneEnabled = name.isNotBlank(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs)) {
            EmojiButton(emoji) { picker = true }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.lists_name_hint), color = colors.inactive, fontWeight = FontWeight.Bold) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                shape = AppTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.divider,
                    cursorColor = colors.accent,
                ),
                modifier = Modifier.weight(1f).padding(start = spacing.sm).focusRequester(focus).testTag(TAG_NAME_FIELD),
            )
        }
        Label(R.string.lists_emoji_label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            modifier = Modifier.padding(horizontal = spacing.lg),
        ) {
            ListEmoji.QUICK.forEach { e -> QuickTile(e, selected = e == emoji) { emoji = e } }
            MoreTile { picker = true }
        }
        if (folders != null && folders.isNotEmpty()) {
            Label(R.string.lists_folder_label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.padding(horizontal = spacing.lg).testTag(TAG_LIST_EDIT_FOLDERS),
            ) {
                FolderChip(stringResource(R.string.lists_folder_none), folderId == null, "$TAG_LIST_EDIT_FOLDER:none") { folderId = null }
                folders.forEach { g ->
                    FolderChip("${g.emojiShown} ${g.name}", folderId == g.id, "$TAG_LIST_EDIT_FOLDER:${g.id}") { folderId = g.id }
                }
            }
        }
        Box(Modifier.padding(bottom = spacing.sm))
    }
    if (picker) {
        EmojiPickerDialog(emoji, onConfirm = { emoji = it; picker = false }, onDismiss = { picker = false })
    }
}

@Composable
private fun Label(text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(start = AppTheme.spacing.lg, end = AppTheme.spacing.lg, top = AppTheme.spacing.sm, bottom = AppTheme.spacing.xxs),
    )
}

/** Плашка выбранного эмодзи: нажатие — окно со всеми эмодзи; в углу — стрелка «есть выбор». */
@Composable
private fun EmojiButton(emoji: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val glyph = with(LocalDensity.current) { sizes.listGlyphLarge.toSp() * EMOJI_BUTTON_SCALE }
    val more = stringResource(R.string.lists_emoji_more)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.emojiButton)
            .background(colors.selected, AppTheme.shapes.large)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$more: $emoji" }
            .testTag(TAG_LIST_EDIT_EMOJI),
    ) {
        Text(emoji, fontSize = glyph)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = AppTheme.spacing.xxs, y = AppTheme.spacing.xxs)
                .size(sizes.emojiButtonMark)
                .background(colors.surface, AppTheme.shapes.large)
                .border(sizes.hairline, colors.divider, AppTheme.shapes.large),
        ) { Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.textSecondary, modifier = Modifier.size(sizes.metaIcon)) }
    }
}

@Composable
private fun QuickTile(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val glyph = with(LocalDensity.current) { sizes.listGlyphLarge.toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.emojiQuick)
            .background(if (selected) colors.selected else Color.Transparent, AppTheme.shapes.medium)
            .border(if (selected) sizes.checkboxStroke else sizes.hairline, if (selected) colors.textPrimary else Color.Transparent, AppTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = emoji
                this.selected = selected
            }
            .testTag("$TAG_LIST_EDIT_QUICK:$emoji"),
    ) { Text(emoji, fontSize = glyph) }
}

@Composable
private fun MoreTile(onClick: () -> Unit) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val more = stringResource(R.string.lists_emoji_more)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.emojiQuick)
            .border(sizes.hairline, colors.divider, AppTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics { contentDescription = more }
            .testTag(TAG_LIST_EDIT_MORE),
    ) { Text("…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = colors.textSecondary) }
}

@Composable
private fun FolderChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
        ),
        modifier = Modifier.testTag(tag),
    )
}

/** Эмодзи на плашке крупнее, чем в строке «Списков». */
private const val EMOJI_BUTTON_SCALE = 1.4f

const val TAG_LIST_EDIT_DIALOG = "list_edit_dialog"
const val TAG_LIST_EDIT_EMOJI = "list_edit_emoji"
const val TAG_LIST_EDIT_QUICK = "list_edit_quick"
const val TAG_LIST_EDIT_MORE = "list_edit_more"
const val TAG_LIST_EDIT_FOLDERS = "list_edit_folders"
const val TAG_LIST_EDIT_FOLDER = "list_edit_folder"
