package com.prostovasei.simpletasks.ui.feature.task

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.ListIcons
import kotlinx.coroutines.launch

@Composable
fun QuickAddRoute(onClose: () -> Unit, viewModel: QuickAddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var closing by remember { mutableStateOf(false) }
    // Закрытие — обратная анимация: шторка уезжает вниз, потом экран убирается из навигации.
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
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(OPEN_MS, easing = SHEET_EASING)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == QuickAddEffect.Close) close() }
    }
    BackHandler(onBack = close)
    // Своё затемнение плавно появляется вместе со шторкой — системное затемнение окна выключено.
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
    QuickAddScreen(state, viewModel::onIntent, close, progress = { progress.value })
}

/**
 * Новая задача — шторка снизу на 75% экрана (итерация 16, макет PM): название и описание, строка «когда»,
 * сетка недели и панель кнопок: список, повтор, напоминание, приоритет, чек-лист и «Сохранить».
 * Нажатие на затемнение или «Назад» — закрыть.
 */
@Composable
fun QuickAddScreen(
    state: QuickAddState,
    onIntent: (QuickAddIntent) -> Unit,
    onClose: () -> Unit,
    autoFocus: Boolean = true,
    progress: () -> Float = { 1f },
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
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.ime))) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(SHEET_FRACTION)
                    .graphicsLayer { translationY = (1f - progress()) * size.height }
                    .shadow(sizes.navShadow, AppTheme.shapes.sheetTop)
                    .background(colors.surface, AppTheme.shapes.sheetTop)
                    // Нажатия по пустому месту шторки не должны доходить до затемнения.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(TAG_QUICK_CARD),
            ) {
                Box(
                    Modifier
                        .padding(top = spacing.xs, bottom = spacing.xxs)
                        .align(Alignment.CenterHorizontally)
                        .size(sizes.sheetHandleWidth, sizes.sheetHandleHeight)
                        .background(colors.divider, AppTheme.shapes.small),
                )
                Column(
                    Modifier
                        .heightIn(max = sizes.pickerMaxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.md),
                ) {
                    CompactField(
                        value = state.title,
                        onValueChange = { onIntent(QuickAddIntent.Title(it)) },
                        hint = stringResource(R.string.quick_add_title_hint),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        imeAction = ImeAction.Done,
                        onImeAction = { onIntent(QuickAddIntent.Save) },
                        modifier = Modifier.focusRequester(titleFocus).testTag(TAG_QUICK_TITLE),
                    )
                    CompactField(
                        value = state.content,
                        onValueChange = { onIntent(QuickAddIntent.Content(it)) },
                        hint = stringResource(R.string.quick_add_content_hint),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = colors.textSecondary),
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
                                modifier = Modifier.weight(1f).testTag("$TAG_QUICK_CHECKLIST:$index"),
                            )
                            IconButton(onClick = { onIntent(QuickAddIntent.RemoveChecklistItem(index)) }, modifier = Modifier.size(sizes.toolButton)) {
                                Icon(Icons.Default.Close, null, tint = colors.textSecondary, modifier = Modifier.size(sizes.metaIcon))
                            }
                        }
                    }
                }
                Text(
                    whenText(state),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.due == null) colors.textSecondary else colors.accent,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs).testTag(TAG_QUICK_WHEN),
                )
                WeekGrid(
                    today = state.today,
                    nowMinute = state.nowMinute,
                    selectedDate = state.selectedDate,
                    selectedMinute = state.selectedMinute,
                    selectedDuration = state.durationMinutes,
                    busy = state.busy,
                    onDayTap = { onIntent(QuickAddIntent.DayTap(it)) },
                    onSlotTap = { date, minute -> onIntent(QuickAddIntent.SlotTap(date, minute)) },
                    onMove = { date, minute -> onIntent(QuickAddIntent.MoveSlot(date, minute)) },
                    onResize = { onIntent(QuickAddIntent.Resize(it)) },
                    onClearTime = { onIntent(QuickAddIntent.ClearTime) },
                    onFirstDayChange = { onIntent(QuickAddIntent.GridDays(it)) },
                    onInteract = {
                        focus.clearFocus()
                        keyboard?.hide()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                QuickToolbar(state, onIntent, Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

/** Строка «когда»: «Без даты», «Сегодня, весь день», «Завтра, 16:00» или «Завтра, 16:00–17:30». */
@Composable
private fun whenText(state: QuickAddState): String {
    val end = state.intervalEnd?.let { TaskDue.AtTime(it) }
    val text = DueFormatter.format(end ?: state.due, state.due.takeIf { end != null }, state.today, state.zone)
        ?: return stringResource(R.string.quick_when_none)
    val time = text.time ?: return stringResource(R.string.quick_when_all_day, dayLabelText(text.day))
    return stringResource(R.string.quick_when_time, dayLabelText(text.day), time)
}

/** Нижняя панель шторки: квадратные кнопки свойств слева и зелёная «Сохранить» справа. */
@Composable
private fun QuickToolbar(state: QuickAddState, onIntent: (QuickAddIntent) -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val onEdit: (FieldEdit) -> Unit = { onIntent(QuickAddIntent.Edit(it)) }
    var menu by remember { mutableStateOf(ToolMenu.None) }
    var repeatDialog by remember { mutableStateOf(false) }
    val close = { menu = ToolMenu.None }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val project = state.projects.firstOrNull { it.id == state.projectId }
        Box {
            ToolButton(
                R.string.quick_tool_list,
                active = project?.isInbox == false,
                tag = TAG_TOOL_LIST,
                content = {
                    ListSquare(
                        color = parseListColor(project?.color) ?: colors.inactive,
                        icon = ListIcons.drawable(if (project?.isInbox != false) ListIcons.INBOX else project.icon),
                    )
                },
            ) { menu = ToolMenu.List }
            DropdownMenu(expanded = menu == ToolMenu.List, onDismissRequest = close, containerColor = colors.surface) {
                ProjectMenuItems(state.projectId, state.projects, onEdit, close)
            }
        }
        ToolButton(R.string.quick_tool_repeat, active = state.rrule != null, tag = TAG_TOOL_REPEAT, icon = R.drawable.ic_tool_repeat) {
            repeatDialog = true
        }
        Box {
            ToolButton(
                R.string.quick_tool_reminder,
                active = state.due != null && state.reminders.isNotEmpty(),
                tag = TAG_TOOL_REMINDER,
                icon = R.drawable.ic_tool_reminder,
                dimmed = state.due == null,
            ) { menu = ToolMenu.Reminder }
            DropdownMenu(expanded = menu == ToolMenu.Reminder, onDismissRequest = close, containerColor = colors.surface) {
                val due = state.due
                if (due == null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quick_reminder_need_day), color = colors.textSecondary) },
                        onClick = close,
                        modifier = Modifier.testTag(TAG_REMINDER_NEED_DAY),
                    )
                } else {
                    ReminderMenuItems(due, state.reminders, onEdit, close)
                }
            }
        }
        Box {
            ToolButton(R.string.quick_tool_priority, active = state.priority != Priority.NONE, tag = TAG_TOOL_PRIORITY, icon = R.drawable.ic_tool_priority) {
                menu = ToolMenu.Priority
            }
            DropdownMenu(expanded = menu == ToolMenu.Priority, onDismissRequest = close, containerColor = colors.surface) {
                PriorityMenuItems(state.priority, onEdit, close)
            }
        }
        ToolButton(R.string.quick_tool_checklist, active = state.checklist != null, tag = TAG_TOOL_CHECKLIST, icon = R.drawable.ic_tool_checklist) {
            onIntent(QuickAddIntent.AddChecklist)
        }
        Spacer(Modifier.weight(1f))
        val saveLabel = stringResource(R.string.quick_add_save)
        Box(
            Modifier
                .size(AppTheme.sizes.toolButton)
                .clip(AppTheme.shapes.medium)
                .background(if (state.canSave) colors.accent else colors.inactive)
                .clickable(enabled = state.canSave, role = Role.Button) { onIntent(QuickAddIntent.Save) }
                .semantics { contentDescription = saveLabel }
                .testTag(TAG_QUICK_SAVE),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, null, tint = colors.onAccent)
        }
    }
    if (repeatDialog) {
        RepeatDialog(
            rrule = state.rrule,
            due = state.selectedDate,
            onConfirm = { onEdit(FieldEdit.Repeat(it)); repeatDialog = false },
            onDismiss = { repeatDialog = false },
        )
    }
}

private enum class ToolMenu { None, List, Reminder, Priority }

/** Квадратная кнопка панели (макет PM): выбранная — залита акцентом; [dimmed] — бледная, пока нет даты. */
@Composable
private fun ToolButton(
    label: Int,
    active: Boolean,
    tag: String,
    @DrawableRes icon: Int? = null,
    dimmed: Boolean = false,
    content: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.medium
    val description = stringResource(label)
    Box(
        Modifier
            .size(sizes.toolButton)
            .clip(shape)
            .background(if (active) colors.accent else colors.surface)
            .border(sizes.checkboxStroke, if (active) colors.accent else colors.divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = active
            }
            .then(if (dimmed) Modifier.alpha(DIMMED_ALPHA) else Modifier)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = if (active) colors.onAccent else colors.textSecondary, modifier = Modifier.size(sizes.toolIcon))
        } else {
            content()
        }
    }
}

/** Поле без рамки и отступов (шторка новой задачи): подсказка того же стиля, бледная. */
@Composable
private fun CompactField(
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
    val style = if (textStyle.color == Color.Unspecified) textStyle.copy(color = colors.textPrimary) else textStyle
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xxs),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = colors.inactive))
                inner()
            }
        },
    )
}

private val SHEET_EASING = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val OPEN_MS = 340
private const val CLOSE_MS = 220
private const val SHEET_FRACTION = 0.75f
private const val DIMMED_ALPHA = 0.45f

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
const val TAG_QUICK_WHEN = "quick_when"
const val TAG_TOOL_LIST = "tool_list"
const val TAG_TOOL_REPEAT = "tool_repeat"
const val TAG_TOOL_REMINDER = "tool_reminder"
const val TAG_TOOL_PRIORITY = "tool_priority"
const val TAG_TOOL_CHECKLIST = "tool_checklist"
const val TAG_REMINDER_NEED_DAY = "reminder_need_day"
