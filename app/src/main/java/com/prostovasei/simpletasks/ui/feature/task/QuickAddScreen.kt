package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme

@Composable
fun QuickAddRoute(onClose: () -> Unit, viewModel: QuickAddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == QuickAddEffect.Close) onClose() }
    }
    QuickAddScreen(state, viewModel::onIntent, onClose)
}

/** Полноэкранное создание задачи: курсор сразу в названии, клавиатура поднята, свойства — кнопками над клавиатурой. */
@Composable
fun QuickAddScreen(state: QuickAddState, onIntent: (QuickAddIntent) -> Unit, onClose: () -> Unit, autoFocus: Boolean = true) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val titleFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            titleFocus.requestFocus()
            keyboard?.show()
        }
    }

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xxs, vertical = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, stringResource(R.string.quick_add_close), tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onIntent(QuickAddIntent.Save) },
                enabled = state.canSave,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                modifier = Modifier.padding(end = spacing.xs).testTag(TAG_QUICK_SAVE),
            ) { Text(stringResource(R.string.quick_add_save)) }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            FormTextField(
                value = state.title,
                onValueChange = { onIntent(QuickAddIntent.Title(it)) },
                hint = stringResource(R.string.quick_add_title_hint),
                textStyle = MaterialTheme.typography.titleLarge,
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(QuickAddIntent.Save) },
                modifier = Modifier.focusRequester(titleFocus).testTag(TAG_QUICK_TITLE),
            )
            FormTextField(
                value = state.content,
                onValueChange = { onIntent(QuickAddIntent.Content(it)) },
                hint = stringResource(R.string.quick_add_content_hint),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = false,
                modifier = Modifier.testTag(TAG_QUICK_CONTENT),
            )
            state.checklist?.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FormTextField(
                        value = item,
                        onValueChange = { onIntent(QuickAddIntent.ChecklistItem(index, it)) },
                        hint = stringResource(R.string.quick_add_checklist_hint),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        imeAction = ImeAction.Next,
                        onImeAction = { onIntent(QuickAddIntent.AddChecklist) },
                        modifier = Modifier.weight(1f).testTag("$TAG_QUICK_CHECKLIST:$index"),
                    )
                    IconButton(onClick = { onIntent(QuickAddIntent.RemoveChecklistItem(index)) }) {
                        Icon(Icons.Default.Close, null, tint = colors.textSecondary)
                    }
                }
            }
        }
        TaskFieldChips(
            due = state.due,
            projectId = state.projectId,
            priority = state.priority,
            rrule = state.rrule,
            reminders = state.reminders,
            projects = state.projects,
            today = state.today,
            zone = state.zone,
            onEdit = { onIntent(QuickAddIntent.Edit(it)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs),
        ) {
            AppChip(stringResource(R.string.chip_checklist), selected = state.checklist != null, tag = TAG_CHIP_CHECKLIST) {
                onIntent(QuickAddIntent.AddChecklist)
            }
        }
    }
}

/** Поле без рамки на фоне экрана. */
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
            focusedContainerColor = colors.background,
            unfocusedContainerColor = colors.background,
            disabledContainerColor = colors.background,
            focusedIndicatorColor = colors.background,
            unfocusedIndicatorColor = colors.background,
            cursorColor = colors.accent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

const val TAG_QUICK_TITLE = "quick_title"
const val TAG_QUICK_CONTENT = "quick_content"
const val TAG_QUICK_SAVE = "quick_save"
const val TAG_QUICK_CHECKLIST = "quick_checklist"
const val TAG_CHIP_CHECKLIST = "chip_checklist"
