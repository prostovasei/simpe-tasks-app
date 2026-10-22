package com.prostovasei.simpletasks.ui.feature.task

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.DialogWindowProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.ReminderIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/** Открытие и закрытие шторки: [progress] 0 → 1 при открытии; [close] увозит её вниз и вызывает закрытие экрана. */
@Stable
class SheetMotion(val progress: () -> Float, val close: () -> Unit)

/** Анимация шторки, «Назад» и своё затемнение вместо системного (окно диалога навигации). */
@Composable
fun rememberSheetMotion(onClosed: () -> Unit): SheetMotion {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val currentOnClosed by rememberUpdatedState(onClosed)
    var closing by remember { mutableStateOf(false) }
    val motion = remember {
        SheetMotion(
            progress = { progress.value },
            close = {
                if (!closing) {
                    closing = true
                    keyboard?.hide()
                    scope.launch {
                        progress.animateTo(0f, tween(CLOSE_MS, easing = FastOutLinearInEasing))
                        currentOnClosed()
                    }
                }
            },
        )
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(OPEN_MS, easing = SHEET_EASING)) }
    BackHandler(onBack = motion.close)
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
    return motion
}

/**
 * Шторка задачи снизу (итерации 16–19, с итерации 21 — макет PM N2) — и для новой, и для созданной задачи.
 * Сверху [header] (название, описание, чек-лист…), потом плитки дней (итерация 22: значок календаря и 2 недели)
 * и панель кнопок: список, повтор, напоминание, приоритет, чек-лист и справа [actions].
 * Нажатие на день открывает таймлайн на 3 дня (нажатый — в центре) почти на весь экран: там выбирается время;
 * значок календаря — месяц, выбранный в нём день тоже открывает таймлайн.
 * Нажатие на затемнение или «Назад» — [onClose]; «Назад» в таймлайне возвращает к задаче.
 */
@Composable
fun TaskSheet(
    fields: SheetFields,
    onGrid: (GridAction) -> Unit,
    onEdit: (FieldEdit) -> Unit,
    onClose: () -> Unit,
    checklistActive: Boolean,
    onChecklist: () -> Unit,
    modifier: Modifier = Modifier,
    progress: () -> Float = { 1f },
    overlay: @Composable BoxScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit,
    header: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val closeLabel = stringResource(R.string.quick_add_close)
    // Открытый таймлайн — день, по которому нажали (номер дня), null — шторка с плитками дней.
    var timelineDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    val hideKeyboard: () -> Unit = {
        focus.clearFocus()
        keyboard?.hide()
    }
    val closeTimeline = {
        timelineDay = null
        // Занятость снова нужна для строки недели — с сегодняшнего дня.
        onGrid(GridAction.FirstDay(fields.today))
    }
    BackHandler(enabled = timelineDay != null, onBack = closeTimeline)

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .semantics { contentDescription = closeLabel }
                .testTag(TAG_QUICK_SCRIM),
        )
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.ime))) {
            val headerMax = maxHeight * HEADER_FRACTION
            val opened = timelineDay?.let { LocalDate.ofEpochDay(it) }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(if (opened != null) Modifier.fillMaxHeight(TIMELINE_FRACTION) else Modifier)
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
                if (opened != null) {
                    Timeline(fields, opened, onGrid, hideKeyboard, closeTimeline, Modifier.weight(1f).windowInsetsPadding(WindowInsets.navigationBars))
                } else {
                    Column(
                        Modifier
                            .heightIn(max = headerMax)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.md),
                        content = header,
                    )
                    DayTiles(
                        today = fields.today,
                        selectedDate = fields.selectedDate,
                        time = DayStripMath.timeLines(fields),
                        onDay = { date ->
                            hideKeyboard()
                            timelineDay = date.toEpochDay()
                        },
                        onCalendar = {
                            hideKeyboard()
                            calendarOpen = true
                        },
                        modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs),
                    )
                    SheetToolbar(fields, onEdit, checklistActive, onChecklist, actions, Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                }
            }
        }
        overlay()
    }
    if (calendarOpen) {
        DateDialog(
            initial = fields.selectedDate ?: fields.today,
            onPick = { date ->
                calendarOpen = false
                timelineDay = date.toEpochDay()
            },
            onDismiss = { calendarOpen = false },
        )
    }
}

/** Календарь на месяц (значок слева от плиток): выбранный день сразу открывает таймлайн. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss() },
                modifier = Modifier.testTag(TAG_DATE_DIALOG_OK),
            ) { Text(stringResource(R.string.sheet_done)) }
        },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

/** Строка «когда» зелёным (серым — «Без даты»). */
@Composable
private fun WhenLabel(fields: SheetFields, modifier: Modifier) {
    val colors = AppTheme.colors
    Text(
        whenText(fields),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (fields.due == null) colors.textSecondary else colors.accent,
        modifier = modifier.testTag(TAG_QUICK_WHEN),
    )
}

/**
 * Таймлайн на 3 дня (итерация 21, макет PM N2): сверху «к задаче», выбранное время и «Готово»; сетка с нажатым днём
 * в центре; снизу «Раньше» / «Позже» — на 3 дня и «Без времени» — только день.
 */
@Composable
private fun Timeline(
    fields: SheetFields,
    day: LocalDate,
    onGrid: (GridAction) -> Unit,
    onInteract: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val scope = rememberCoroutineScope()
    val scroll = remember(day) { GridScroll(WeekGridMath.timelineScroll(fields.today, day)) }
    val page = { days: Int ->
        scope.launch {
            val limit = WeekGridMath.MAX_DAYS_AWAY.toFloat()
            animate(scroll.days, (scroll.days + days).coerceIn(-limit, limit)) { value, _ -> scroll.days = value }
        }
        Unit
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = spacing.xs, vertical = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag(TAG_TIMELINE_BACK)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.timeline_back), tint = colors.textPrimary)
            }
            WhenLabel(fields, Modifier.weight(1f))
            SheetConfirmButton(R.string.sheet_done, enabled = true, tag = TAG_TIMELINE_DONE, onClick = onBack)
        }
        WeekGrid(
            scroll = scroll,
            today = fields.today,
            nowMinute = fields.nowMinute,
            selectedDate = fields.selectedDate,
            selectedMinute = fields.selectedMinute,
            selectedDuration = fields.durationMinutes,
            busy = fields.busy,
            onDayTap = { onGrid(GridAction.DayTap(it)) },
            onSlotTap = { date, minute -> onGrid(GridAction.SlotTap(date, minute)) },
            onMove = { date, minute -> onGrid(GridAction.MoveSlot(date, minute)) },
            onResize = { onGrid(GridAction.Resize(it)) },
            onClearTime = { onGrid(GridAction.ClearTime) },
            onFirstDayChange = { onGrid(GridAction.FirstDay(it)) },
            onInteract = onInteract,
            visibleDays = WeekGridMath.TIMELINE_DAYS,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, R.string.timeline_earlier, TAG_TIMELINE_EARLIER) { page(-WeekGridMath.TIMELINE_DAYS) }
            Spacer(Modifier.weight(1f))
            TimelineButton(R.string.quick_when_none, TAG_TIMELINE_NO_DATE) {
                onGrid(GridAction.ClearDate)
                onBack()
            }
            TimelineButton(R.string.strip_no_time, TAG_TIMELINE_NO_TIME) {
                // Только день — тот, по которому открыт таймлайн; если он уже выбран — просто убрать время.
                when {
                    fields.selectedDate != day -> onGrid(GridAction.DayTap(day))
                    fields.selectedMinute != null -> onGrid(GridAction.ClearTime)
                }
                onBack()
            }
            Spacer(Modifier.weight(1f))
            ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, R.string.timeline_later, TAG_TIMELINE_LATER) { page(WeekGridMath.TIMELINE_DAYS) }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, label: Int, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val description = stringResource(label)
    Box(
        Modifier
            .size(AppTheme.sizes.toolButton)
            .clip(AppTheme.shapes.medium)
            .background(colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.textPrimary)
    }
}

@Composable
private fun TimelineButton(label: Int, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        modifier = Modifier
            .clip(AppTheme.shapes.medium)
            .background(colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
            .testTag(tag),
    )
}

/** Строка «когда»: «Без даты», «Сегодня, весь день», «Завтра, 16:00» или «Завтра, 16:00–17:30». */
@Composable
private fun whenText(fields: SheetFields): String {
    val text = DueFormatter.format(fields.due, fields.start, fields.today, fields.zone) ?: return stringResource(R.string.quick_when_none)
    val time = text.time ?: return stringResource(R.string.quick_when_all_day, dayLabelText(text.day))
    return stringResource(R.string.quick_when_time, dayLabelText(text.day), time)
}

/** Нижняя панель шторки: квадратные кнопки свойств слева, [actions] справа. */
@Composable
private fun SheetToolbar(
    fields: SheetFields,
    onEdit: (FieldEdit) -> Unit,
    checklistActive: Boolean,
    onChecklist: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    var menu by remember { mutableStateOf(ToolMenu.None) }
    var repeatDialog by remember { mutableStateOf(false) }
    var customReminder by remember { mutableStateOf(false) }
    val close = { menu = ToolMenu.None }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val project = fields.projects.firstOrNull { it.id == fields.projectId }
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
                ProjectMenuItems(fields.projectId, fields.projects, onEdit, close)
            }
        }
        ToolButton(R.string.quick_tool_repeat, active = fields.rrule != null, tag = TAG_TOOL_REPEAT, icon = R.drawable.ic_tool_repeat) {
            repeatDialog = true
        }
        Box {
            val badge = Reminders.badge(fields.due, fields.reminders)
            ToolButton(
                R.string.quick_tool_reminder,
                active = badge != null,
                tag = TAG_TOOL_REMINDER,
                icon = if (badge == null) R.drawable.ic_tool_reminder else null,
                dimmed = fields.due == null,
                content = {
                    if (badge != null) {
                        ReminderIcon(badge, sizes.toolIcon, tint = colors.onAccent, badgeColor = colors.onAccent, glyphColor = colors.accent)
                    }
                },
            ) { menu = ToolMenu.Reminder }
            DropdownMenu(expanded = menu == ToolMenu.Reminder, onDismissRequest = close, containerColor = colors.surface) {
                val due = fields.due
                if (due == null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quick_reminder_need_day), color = colors.textSecondary) },
                        onClick = close,
                        modifier = Modifier.testTag(TAG_REMINDER_NEED_DAY),
                    )
                } else {
                    ReminderMenuItems(due, fields.reminders, onEdit, close, onCustom = { customReminder = true })
                }
            }
        }
        Box {
            ToolButton(R.string.quick_tool_priority, active = fields.priority != Priority.NONE, tag = TAG_TOOL_PRIORITY, icon = R.drawable.ic_tool_priority) {
                menu = ToolMenu.Priority
            }
            DropdownMenu(expanded = menu == ToolMenu.Priority, onDismissRequest = close, containerColor = colors.surface) {
                PriorityMenuItems(fields.priority, onEdit, close)
            }
        }
        ToolButton(R.string.quick_tool_checklist, active = checklistActive, tag = TAG_TOOL_CHECKLIST, icon = R.drawable.ic_tool_checklist, onClick = onChecklist)
        Spacer(Modifier.weight(1f))
        actions()
    }
    val due = fields.due
    if (customReminder && due != null) {
        CustomReminderDialog(
            due = due,
            start = fields.start,
            zone = fields.zone,
            onConfirm = { trigger ->
                customReminder = false
                if (fields.reminders.none { Reminders.sameMoment(it, trigger) }) onEdit(FieldEdit.SetReminders(fields.reminders + trigger))
            },
            onDismiss = { customReminder = false },
        )
    }
    if (repeatDialog) {
        RepeatDialog(
            rrule = fields.rrule,
            due = fields.selectedDate,
            onConfirm = { onEdit(FieldEdit.Repeat(it)); repeatDialog = false },
            onDismiss = { repeatDialog = false },
        )
    }
}

private enum class ToolMenu { None, List, Reminder, Priority }

/** Квадратная кнопка панели (макет PM): выбранная — залита акцентом; [dimmed] — бледная, пока нет даты. */
@Composable
fun ToolButton(
    label: Int,
    active: Boolean,
    tag: String,
    @DrawableRes icon: Int? = null,
    dimmed: Boolean = false,
    tint: Color? = null,
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
            Icon(painterResource(icon), null, tint = if (active) colors.onAccent else tint ?: colors.textSecondary, modifier = Modifier.size(sizes.toolIcon))
        } else {
            content()
        }
    }
}

/** Зелёная квадратная кнопка с галочкой справа на панели: «Сохранить» у новой задачи, «Готово» у созданной. */
@Composable
fun SheetConfirmButton(label: Int, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val description = stringResource(label)
    Box(
        Modifier
            .size(AppTheme.sizes.toolButton)
            .clip(AppTheme.shapes.medium)
            .background(if (enabled) colors.accent else colors.inactive)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Check, null, tint = colors.onAccent)
    }
}

/** Поле без рамки и отступов (шторка задачи): подсказка того же стиля, бледная. */
@Composable
fun CompactField(
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
/** Таймлайн занимает почти весь экран; верхняя часть шторки (поля, чек-лист) — не больше этой доли экрана. */
private const val TIMELINE_FRACTION = 0.97f
private const val HEADER_FRACTION = 0.45f
private const val DIMMED_ALPHA = 0.45f

const val TAG_QUICK_CARD = "quick_card"
const val TAG_QUICK_SCRIM = "quick_scrim"
const val TAG_QUICK_WHEN = "quick_when"
const val TAG_TOOL_LIST = "tool_list"
const val TAG_TOOL_REPEAT = "tool_repeat"
const val TAG_TOOL_REMINDER = "tool_reminder"
const val TAG_TOOL_PRIORITY = "tool_priority"
const val TAG_TOOL_CHECKLIST = "tool_checklist"
const val TAG_REMINDER_NEED_DAY = "reminder_need_day"
const val TAG_TIMELINE_BACK = "timeline_back"
const val TAG_TIMELINE_DONE = "timeline_done"
const val TAG_TIMELINE_EARLIER = "timeline_earlier"
const val TAG_TIMELINE_LATER = "timeline_later"
const val TAG_TIMELINE_NO_TIME = "timeline_no_time"
const val TAG_TIMELINE_NO_DATE = "timeline_no_date"
const val TAG_DATE_DIALOG_OK = "date_dialog_ok"
