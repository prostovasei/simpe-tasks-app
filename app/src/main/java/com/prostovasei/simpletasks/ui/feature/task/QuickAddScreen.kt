package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
fun QuickAddRoute(onClose: () -> Unit, origin: Offset? = null, viewModel: QuickAddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var closing by remember { mutableStateOf(false) }
    // Закрытие — обратная анимация: карточка уходит в «+», потом экран убирается из навигации.
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            keyboard?.hide()
            scope.launch {
                progress.animateTo(0f, tween(CLOSE_MS, easing = FastOutLinearInEasing))
                onClose()
            }
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(OPEN_MS, easing = FastOutSlowInEasing)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == QuickAddEffect.Close) close() }
    }
    BackHandler(onBack = close)
    // Своё затемнение плавно появляется вместе с карточкой — системное затемнение окна выключено.
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
    QuickAddScreen(state, viewModel::onIntent, close, progress = { progress.value }, origin = origin)
}

/**
 * Новая задача — маленькая карточка над нижней панелью (итерация 15, вариант B1): вырастает из «+»
 * ([origin] — центр кнопки в координатах окна; null — из правого нижнего угла), курсор сразу в названии.
 * Нажатие на затемнение или «Назад» — закрыть.
 */
@Composable
fun QuickAddScreen(
    state: QuickAddState,
    onIntent: (QuickAddIntent) -> Unit,
    onClose: () -> Unit,
    autoFocus: Boolean = true,
    progress: () -> Float = { 1f },
    origin: Offset? = null,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val titleFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            titleFocus.requestFocus()
            keyboard?.show()
        }
    }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val closeLabel = stringResource(R.string.quick_add_close)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .semantics { contentDescription = closeLabel }
                .testTag(TAG_QUICK_SCRIM),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(spacing.sm)
                .onGloballyPositioned { cardBounds = it.boundsInWindow() }
                .graphicsLayer {
                    val p = progress()
                    val scale = START_SCALE + (1f - START_SCALE) * p
                    scaleX = scale
                    scaleY = scale
                    alpha = (p * 2f).coerceAtMost(1f)
                    transformOrigin = growOrigin(origin, cardBounds)
                }
                .shadow(sizes.navShadow, AppTheme.shapes.nav)
                .background(colors.surface, AppTheme.shapes.nav)
                .heightIn(max = sizes.pickerMaxHeight)
                .padding(vertical = spacing.xs)
                .testTag(TAG_QUICK_CARD),
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = spacing.xxs),
            ) {
                FormTextField(
                    value = state.title,
                    onValueChange = { onIntent(QuickAddIntent.Title(it)) },
                    hint = stringResource(R.string.quick_add_title_hint),
                    textStyle = MaterialTheme.typography.titleMedium,
                    imeAction = ImeAction.Done,
                    onImeAction = { onIntent(QuickAddIntent.Save) },
                    container = colors.surface,
                    modifier = Modifier.focusRequester(titleFocus).testTag(TAG_QUICK_TITLE),
                )
                FormTextField(
                    value = state.content,
                    onValueChange = { onIntent(QuickAddIntent.Content(it)) },
                    hint = stringResource(R.string.quick_add_content_hint),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = false,
                    container = colors.surface,
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
                            container = colors.surface,
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xxs),
            ) {
                AppChip(stringResource(R.string.chip_checklist), selected = state.checklist != null, tag = TAG_CHIP_CHECKLIST) {
                    onIntent(QuickAddIntent.AddChecklist)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.sm)) {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onIntent(QuickAddIntent.Save) },
                    enabled = state.canSave,
                    shape = AppTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                    modifier = Modifier.testTag(TAG_QUICK_SAVE),
                ) { Text(stringResource(R.string.quick_add_save)) }
            }
        }
    }
}

/** Точка, из которой растёт карточка: центр «+» относительно карточки (может быть за её краем). */
private fun growOrigin(origin: Offset?, card: Rect): TransformOrigin {
    if (origin == null || card.width <= 0f || card.height <= 0f) return TransformOrigin(DEFAULT_ORIGIN_X, 1f)
    return TransformOrigin((origin.x - card.left) / card.width, (origin.y - card.top) / card.height)
}

private const val OPEN_MS = 340
private const val CLOSE_MS = 220
private const val START_SCALE = 0.08f
private const val DEFAULT_ORIGIN_X = 0.9f

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
const val TAG_QUICK_CARD = "quick_card"
const val TAG_QUICK_SCRIM = "quick_scrim"
const val TAG_QUICK_CONTENT = "quick_content"
const val TAG_QUICK_SAVE = "quick_save"
const val TAG_QUICK_CHECKLIST = "quick_checklist"
const val TAG_CHIP_CHECKLIST = "chip_checklist"
