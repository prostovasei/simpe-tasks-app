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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.ListSquare
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.ReminderIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/**
 * Открытие и закрытие шторки: [progress] 0 → 1 — окно на месте (выехало снизу или выросло из плашки виджета),
 * [reveal] 0 → 1 — содержимое проявилось; [close] прячет окно и вызывает закрытие экрана.
 */
@Stable
class SheetMotion(val progress: () -> Float, val reveal: () -> Float, val close: () -> Unit)

/**
 * Откуда окно «вырастает» (0.29, Р1 — выбор PM в прототипе): плашка задачи или «+» в виджете, в координатах экрана
 * (`Intent.sourceBounds` от рабочего стола). null — окно выезжает снизу, как раньше.
 */
val LocalSheetOrigin = compositionLocalOf<android.graphics.Rect?> { null }

/**
 * Анимация шторки, «Назад» и своё затемнение вместо системного (окно диалога навигации).
 * Из виджета (0.29, Р1): плашка растягивается в окно, при закрытии сжимается обратно. Содержимое — С5:
 * проявляется из лёгкого размытия, когда окно на месте.
 */
@Composable
fun rememberSheetMotion(onClosed: () -> Unit): SheetMotion {
    val progress = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val morph = LocalSheetOrigin.current != null
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val currentOnClosed by rememberUpdatedState(onClosed)
    var closing by remember { mutableStateOf(false) }
    val motion = remember {
        SheetMotion(
            progress = { progress.value },
            reveal = { reveal.value },
            close = {
                if (!closing) {
                    closing = true
                    keyboard?.hide()
                    scope.launch {
                        if (morph) {
                            reveal.animateTo(0f, tween(HIDE_MS))
                            progress.animateTo(0f, tween(MORPH_CLOSE_MS, easing = MORPH_EASING))
                        } else {
                            progress.animateTo(0f, tween(CLOSE_MS, easing = FastOutLinearInEasing))
                        }
                        currentOnClosed()
                    }
                }
            },
        )
    }
    LaunchedEffect(Unit) {
        if (morph) {
            // Окно видно, только когда «плашка» доросла, — содержимое проявляется следом.
            progress.animateTo(1f, tween(MORPH_MS, easing = MORPH_EASING))
            reveal.animateTo(1f, tween(REVEAL_MS))
        } else {
            // Выезд снизу: содержимое проявляется, пока окно ещё доезжает (0.29.1 — быстрее, решение PM).
            launch { reveal.animateTo(1f, tween(REVEAL_MS, delayMillis = OPEN_MS - REVEAL_MS)) }
            progress.animateTo(1f, tween(OPEN_MS, easing = SHEET_EASING))
        }
    }
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
    reveal: () -> Float = { 1f },
    overlay: @Composable BoxScope.() -> Unit = {},
    actionCount: Int = 1,
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

    // Р1: откуда растёт окно — плашка виджета в координатах этого экрана; куда — само окно.
    val origin = LocalSheetOrigin.current
    val view = LocalView.current
    var screen by remember { mutableStateOf<Rect?>(null) }
    var card by remember { mutableStateOf<Rect?>(null) }
    val start = remember(origin, screen) {
        val at = screen ?: return@remember null
        origin?.let {
            val location = IntArray(2).also(view::getLocationOnScreen)
            Rect(Offset((it.left - location[0]).toFloat(), (it.top - location[1]).toFloat()), Size(it.width().toFloat(), it.height().toFloat()))
                .translate(-at.topLeft)
        }
    }
    val blur = with(LocalDensity.current) { SizeTokens.SHEET_REVEAL_BLUR.dp.toPx() }

    Box(modifier.fillMaxSize().onGloballyPositioned { screen = it.boundsInRoot() }) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .semantics { contentDescription = closeLabel }
                .testTag(TAG_QUICK_SCRIM),
        )
        if (start != null) {
            // Пока окно растёт или сжимается — вместо него белая «плашка», которая меняет место, размер и скругление.
            val surface = colors.surface
            val radius = with(LocalDensity.current) { RadiusTokens.SM.dp.toPx() to RadiusTokens.NAV.dp.toPx() }
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val p = progress()
                    val end = card?.translate(-(screen?.topLeft ?: Offset.Zero)) ?: return@drawBehind
                    if (p <= 0f || p >= 1f) return@drawBehind
                    val now = lerp(start, end, p)
                    drawRoundRect(surface, now.topLeft, now.size, CornerRadius(radius.first + (radius.second - radius.first) * p))
                },
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            val density = LocalDensity.current
            val opened = timelineDay?.let { LocalDate.ofEpochDay(it) }
            // Окно задачи — карточка сразу над клавиатурой (0.23.3, макет PM «А»): место не меняется, открыта
            // клавиатура или нет. Таймлайн — по-прежнему шторка снизу почти на весь экран.
            val keyboard = with(density) { rememberKeyboardHeight((maxHeight * KEYBOARD_FALLBACK).roundToPx()).toDp() }
            val ime = with(density) { WindowInsets.ime.getBottom(this).toDp() }
            val navBar = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val lift = if (opened != null) Dp.Hairline else maxOf(keyboard, ime, navBar) + spacing.xs
            val headerMax = (maxHeight - lift) * HEADER_FRACTION
            val shape = if (opened != null) AppTheme.shapes.sheetTop else AppTheme.shapes.nav
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = lift)
                    .then(if (opened != null) Modifier.fillMaxWidth().fillMaxHeight(TIMELINE_FRACTION) else Modifier.fillMaxWidth().padding(horizontal = spacing.xs))
                    .onGloballyPositioned { card = it.boundsInRoot() }
                    .graphicsLayer {
                        // Из виджета окно появляется, только когда «плашка» доросла до него.
                        if (start != null) alpha = if (progress() >= 1f) 1f else 0f else translationY = (1f - progress()) * (size.height + lift.toPx())
                    }
                    .shadow(sizes.navShadow, shape)
                    .background(colors.surface, shape)
                    // Нажатия по пустому месту шторки не должны доходить до затемнения.
                    .pointerInput(Unit) { detectTapGestures { } }
                    // С5: содержимое проявляется из размытия.
                    .graphicsLayer {
                        val r = reveal()
                        alpha = r
                        val radius = (1f - r) * blur
                        renderEffect = if (radius > MIN_BLUR) BlurEffect(radius, radius, TileMode.Decal) else null
                    }
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
                        onClear = { onGrid(GridAction.ClearDate) },
                        modifier = Modifier.padding(bottom = spacing.xs),
                    )
                    SheetToolbar(fields, onEdit, checklistActive, onChecklist, actionCount, actions, Modifier.padding(bottom = spacing.xxs))
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

/**
 * Таймлайн на 3 дня (итерация 21, макет PM N2; 0.23.1 — макет «А»): сетка с нажатым днём в центре, листается свайпом;
 * снизу иконки «Без даты» и «Без времени» (только день) и зелёная галочка «Готово».
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
    val spacing = AppTheme.spacing
    val scroll = remember(day) { GridScroll(WeekGridMath.timelineScroll(fields.today, day)) }
    Column(modifier.fillMaxWidth().padding(top = spacing.xs)) {
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
            ToolButton(R.string.quick_when_none, active = false, tag = TAG_TIMELINE_NO_DATE, icon = R.drawable.ic_no_date) {
                onGrid(GridAction.ClearDate)
                onBack()
            }
            ToolButton(R.string.strip_no_time, active = false, tag = TAG_TIMELINE_NO_TIME, icon = R.drawable.ic_no_time) {
                // Только день — тот, по которому открыт таймлайн; если он уже выбран — просто убрать время.
                when {
                    fields.selectedDate != day -> onGrid(GridAction.DayTap(day))
                    fields.selectedMinute != null -> onGrid(GridAction.ClearTime)
                }
                onBack()
            }
            Spacer(Modifier.weight(1f))
            SheetConfirmButton(R.string.sheet_done, enabled = true, tag = TAG_TIMELINE_DONE, onClick = onBack)
        }
    }
}

/** Нижняя панель шторки: квадратные кнопки свойств слева, [actions] справа. */
@Composable
private fun SheetToolbar(
    fields: SheetFields,
    onEdit: (FieldEdit) -> Unit,
    checklistActive: Boolean,
    onChecklist: () -> Unit,
    actionCount: Int,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var menu by remember { mutableStateOf(ToolMenu.None) }
    var repeatDialog by remember { mutableStateOf(false) }
    var customReminder by remember { mutableStateOf(false) }
    val close = { menu = ToolMenu.None }
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs)) {
    val count = TOOLBAR_TOOLS + actionCount
    val fit = ((maxWidth - spacing.xs * (count - 1)) / count).coerceAtMost(SizeTokens.TOOL_BUTTON.dp)
    CompositionLocalProvider(LocalToolButtonSize provides fit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val project = fields.projects.firstOrNull { it.id == fields.projectId }
        val inbox = fields.projects.firstOrNull { it.isInbox }
        Box {
            ToolButton(
                R.string.quick_tool_list,
                active = project?.isInbox == false,
                tag = TAG_TOOL_LIST,
                clear = if (project?.isInbox == false && inbox != null) ClearAction(R.string.clear_list, TAG_CLEAR_LIST) { onEdit(FieldEdit.Project(inbox.id)) } else null,
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
        ToolButton(
            R.string.quick_tool_repeat,
            active = fields.rrule != null,
            tag = TAG_TOOL_REPEAT,
            icon = R.drawable.ic_tool_repeat,
            dimmed = fields.due == null,
            clear = if (fields.rrule != null) ClearAction(R.string.clear_repeat, TAG_CLEAR_REPEAT) { onEdit(FieldEdit.Repeat(null)) } else null,
        ) {
            repeatDialog = true
        }
        Box {
            val mark = Reminders.mark(fields.due, fields.reminders)
            ToolButton(
                R.string.quick_tool_reminder,
                active = mark != null,
                tag = TAG_TOOL_REMINDER,
                icon = if (mark == null) R.drawable.ic_tool_reminder else null,
                dimmed = fields.due == null,
                clear = if (mark != null) ClearAction(R.string.clear_reminders, TAG_CLEAR_REMINDERS) { onEdit(FieldEdit.SetReminders(emptyList())) } else null,
                content = {
                    if (mark != null) {
                        ReminderIcon(mark, LocalToolButtonSize.current * ICON_SHARE, tint = colors.accent, earlyColor = colors.accent)
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
            ToolButton(
                R.string.quick_tool_priority,
                active = fields.priority != Priority.NONE,
                tag = TAG_TOOL_PRIORITY,
                icon = R.drawable.ic_tool_priority,
                clear = if (fields.priority != Priority.NONE) ClearAction(R.string.clear_priority, TAG_CLEAR_PRIORITY) { onEdit(FieldEdit.SetPriority(Priority.NONE)) } else null,
            ) {
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
    }
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
    clear: ClearAction? = null,
    content: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Box {
        ToolButtonBody(label, active, tag, icon, dimmed, tint, content, onClick)
        // Крестик в правом верхнем углу выбранной кнопки (0.23.2): сбрасывает только это свойство.
        clear?.let { ClearBadge(it, Modifier.align(Alignment.TopEnd).offset(x = AppTheme.sizes.weekClear, y = -AppTheme.sizes.weekClear)) }
    }
}

/** Что сбрасывает крестик: подпись для TalkBack, тег для тестов и действие. */
data class ClearAction(val label: Int, val tag: String, val onClear: () -> Unit)

/** Круглый крестик (как на блоке времени в сетке); область нажатия — вдвое больше значка. */
@Composable
fun ClearBadge(action: ClearAction, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val description = stringResource(action.label)
    Box(
        modifier
            .size(sizes.weekClear * 2)
            .clickable(role = Role.Button, onClick = action.onClear)
            .semantics { contentDescription = description }
            .testTag(action.tag),
        contentAlignment = Alignment.Center,
    ) {
        // Итерация 26: серая «таблетка» с белым × — заметна, но не спорит с выбранным.
        Box(
            Modifier
                .size(sizes.clearDot)
                .background(colors.inactive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = colors.surface, modifier = Modifier.size(sizes.clearGlyph))
        }
    }
}

@Composable
private fun ToolButtonBody(
    label: Int,
    active: Boolean,
    tag: String,
    @DrawableRes icon: Int?,
    dimmed: Boolean,
    tint: Color?,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.large
    val description = stringResource(label)
    val jelly = rememberJelly()
    // Итерация 24: кнопки крупнее (50 dp, скругление как у плиток дней), обводка тоньше — в 1 dp (вариант PM «А»).
    Box(
        Modifier
            .size(LocalToolButtonSize.current)
            .jelly(jelly)
            .clip(shape)
            .background(if (active) colors.selected else colors.surface)
            .border(sizes.hairline, if (active) colors.selected else colors.divider, shape)
            .jellyClick(jelly, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = active
            }
            .then(if (dimmed) Modifier.alpha(DIMMED_ALPHA) else Modifier)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = if (active) colors.accent else tint ?: colors.textSecondary, modifier = Modifier.size(LocalToolButtonSize.current * ICON_SHARE))
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
    val jelly = rememberJelly()
    Box(
        Modifier
            .size(LocalToolButtonSize.current)
            .jelly(jelly)
            .clip(AppTheme.shapes.large)
            .background(if (enabled) colors.accent else colors.inactive)
            .jellyClick(jelly, enabled = enabled, onClick = onClick)
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
/** Окно из виджета — быстро (итерация 30, №73: 0,2 с рост и 0,09 с проявление; было 0,32 + 0,16). */
private val MORPH_EASING = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val MORPH_MS = 200
private const val MORPH_CLOSE_MS = 200
private const val HIDE_MS = 60
private const val REVEAL_MS = 90
private const val MIN_BLUR = 0.5f
private const val OPEN_MS = 240
private const val CLOSE_MS = 160
/** Таймлайн занимает почти весь экран; верхняя часть шторки (поля, чек-лист) — не больше этой доли экрана. */
private const val TIMELINE_FRACTION = 0.97f
private const val HEADER_FRACTION = 0.45f

/** Пока клавиатура ни разу не открывалась — её высота примерно 40% экрана. */
private const val KEYBOARD_FALLBACK = 0.4f
private const val DIMMED_ALPHA = 0.45f

const val TAG_QUICK_CARD = "quick_card"
const val TAG_QUICK_SCRIM = "quick_scrim"
const val TAG_TOOL_LIST = "tool_list"
const val TAG_TOOL_REPEAT = "tool_repeat"
const val TAG_TOOL_REMINDER = "tool_reminder"
const val TAG_TOOL_PRIORITY = "tool_priority"
const val TAG_TOOL_CHECKLIST = "tool_checklist"
const val TAG_REMINDER_NEED_DAY = "reminder_need_day"
const val TAG_TIMELINE_DONE = "timeline_done"
const val TAG_TIMELINE_NO_TIME = "timeline_no_time"
const val TAG_TIMELINE_NO_DATE = "timeline_no_date"
const val TAG_CLEAR_LIST = "clear_list"
const val TAG_CLEAR_REPEAT = "clear_repeat"
const val TAG_CLEAR_REMINDERS = "clear_reminders"
const val TAG_CLEAR_PRIORITY = "clear_priority"
const val TAG_CLEAR_DATE = "clear_date"
const val TAG_DATE_DIALOG_OK = "date_dialog_ok"


/**
 * Размер квадратных кнопок панели (итерация 24): 50 dp, а если все кнопки и действия справа не помещаются
 * в ширину (у созданной задачи их 7) — поровну меньше.
 */
val LocalToolButtonSize = compositionLocalOf { SizeTokens.TOOL_BUTTON.dp }

/** Иконка занимает половину кнопки (20 из 40, 25 из 50). */
private const val ICON_SHARE = 0.5f

/** Кнопки панели слева: список, повтор, напоминание, приоритет, чек-лист. */
private const val TOOLBAR_TOOLS = 5
