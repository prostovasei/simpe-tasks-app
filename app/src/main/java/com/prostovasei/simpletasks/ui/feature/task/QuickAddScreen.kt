package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

@Composable
fun QuickAddRoute(onClose: () -> Unit, viewModel: QuickAddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = rememberSheetMotion(onClose)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == QuickAddEffect.Close) motion.close() }
    }
    QuickAddScreen(state, viewModel::onIntent, motion.close, progress = motion.progress, reveal = motion.reveal)
}

/**
 * Новая задача — шторка с сеткой недели (итерации 16–17, макет PM): курсор сразу в названии,
 * справа на панели — зелёная «Сохранить». Нажатие на затемнение или «Назад» — закрыть без сохранения.
 */
@Composable
fun QuickAddScreen(
    state: QuickAddState,
    onIntent: (QuickAddIntent) -> Unit,
    onClose: () -> Unit,
    autoFocus: Boolean = true,
    progress: () -> Float = { 1f },
    reveal: () -> Float = { 1f },
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val titleFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            titleFocus.requestFocus()
            keyboard?.show()
        }
    }
    // Новый пункт чек-листа сразу получает курсор — и после «Далее», и после кнопки «Чек-лист» на панели.
    val checklistCount = state.checklist?.size ?: 0
    val itemFocus = remember(checklistCount) { List(checklistCount) { FocusRequester() } }
    var shownCount by remember { mutableIntStateOf(checklistCount) }
    LaunchedEffect(checklistCount) {
        if (checklistCount > shownCount) {
            itemFocus.lastOrNull()?.requestFocus()
            keyboard?.show()
        }
        shownCount = checklistCount
    }
    // Название из черновика выделено: любая буква заменит его, «Удалить» сотрёт (итерация 38).
    var title by remember {
        mutableStateOf(TextFieldValue(state.title, if (state.draftRestored) TextRange(0, state.title.length) else TextRange(state.title.length)))
    }
    TaskSheet(
        fields = state.fields,
        onGrid = { onIntent(QuickAddIntent.Grid(it)) },
        onEdit = { onIntent(QuickAddIntent.Edit(it)) },
        onClose = {
            onIntent(QuickAddIntent.Dismiss)
            onClose()
        },
        checklistActive = state.checklist != null,
        onChecklist = { onIntent(QuickAddIntent.AddChecklist) },
        progress = progress,
        reveal = reveal,
        actions = { SheetConfirmButton(R.string.quick_add_save, enabled = state.canSave, tag = TAG_QUICK_SAVE) { onIntent(QuickAddIntent.Save) } },
    ) {
        CompactField(
            value = title,
            onValueChange = {
                title = it
                onIntent(QuickAddIntent.Title(it.text))
            },
            hint = stringResource(R.string.quick_add_title_hint),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            imeAction = ImeAction.Done,
            // «ОК» на клавиатуре только убирает её (итерация 24): сохраняет задачу лишь зелёная галочка.
            onImeAction = {
                focus.clearFocus()
                keyboard?.hide()
            },
            modifier = Modifier.focusRequester(titleFocus).testTag(TAG_QUICK_TITLE),
        )
        CompactField(
            value = state.content,
            onValueChange = { onIntent(QuickAddIntent.Content(it)) },
            hint = stringResource(R.string.quick_add_content_hint),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = colors.textSecondary),
            singleLine = false,
            modifier = Modifier.padding(top = spacing.xxs).testTag(TAG_QUICK_CONTENT),
        )
        state.checklist?.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(sizes.listDot).border(sizes.checkboxStroke, colors.divider, AppTheme.shapes.small))
                Spacer(Modifier.size(spacing.xs))
                CompactField(
                    value = item,
                    onValueChange = { onIntent(QuickAddIntent.ChecklistItem(index, it)) },
                    hint = stringResource(R.string.quick_add_checklist_hint),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    imeAction = ImeAction.Next,
                    onImeAction = { onIntent(QuickAddIntent.AddChecklist) },
                    modifier = Modifier.weight(1f).focusRequester(itemFocus[index]).testTag("$TAG_QUICK_CHECKLIST:$index"),
                )
                IconButton(onClick = { onIntent(QuickAddIntent.RemoveChecklistItem(index)) }, modifier = Modifier.size(sizes.toolButton)) {
                    Icon(Icons.Default.Close, null, tint = colors.textSecondary, modifier = Modifier.size(sizes.metaIcon))
                }
            }
        }
    }
}

/** Поле без рамки на фоне [container] (по умолчанию — фон экрана). */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    container: Color = AppTheme.colors.background,
) {
    val colors = AppTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, style = textStyle, color = colors.textSecondary) },
        textStyle = textStyle.copy(color = colors.textPrimary),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            focusedIndicatorColor = container,
            unfocusedIndicatorColor = container,
            cursorColor = colors.accent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

const val TAG_QUICK_TITLE = "quick_title"
const val TAG_QUICK_CONTENT = "quick_content"
const val TAG_QUICK_SAVE = "quick_save"
const val TAG_QUICK_CHECKLIST = "quick_checklist"
