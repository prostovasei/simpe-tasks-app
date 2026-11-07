package com.prostovasei.simpletasks.ui.feature.task

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.compositeOver
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
import com.prostovasei.simpletasks.designsystem.components.ListBadge
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.designsystem.components.SmartDateChip
import com.prostovasei.simpletasks.designsystem.components.SmartDateHighlight
import com.prostovasei.simpletasks.designsystem.components.smartDateSpan
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueFormatter
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.ui.common.ReminderIcon
import java.time.LocalDate
import kotlin.math.floor
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
 * Сверху [header] (название, описание, чек-лист…), потом два поля — дата и время (итерация 34) — и панель кнопок:
 * список, приоритет, чек-лист, повтор, уведомления и справа [actions].
 * Нажатие на дату или время открывает окно даты и времени на весь экран: сетка на 3 дня, блок «2 недели» и календарь на 2 месяца.
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
    /** Сразу открыть выбор списка (итерация 52: «+» в «Когда-нибудь»). */
    openListOnStart: Boolean = false,
    /** Сразу окно даты и времени (итерация 53: «Отложить» → «Выбрать дату и время…»). */
    openDateOnStart: Boolean = false,
    /** Растёт — открыть окно даты (итерация 54: «Отложить» → «Выбрать дату и время…» в окне задачи). */
    dateRequests: Int = 0,
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
    var timelineOpen by rememberSaveable { mutableStateOf(openDateOnStart) }
    var tool by remember { mutableStateOf(if (openListOnStart) ToolDialog.List else ToolDialog.None) }
    val hideKeyboard: () -> Unit = {
        focus.clearFocus()
        keyboard?.hide()
    }
    // Итерация 31 (ТЗ PM): перед окном клавиатура прячется — окно по центру экрана, ничем не закрыто.
    val openTool = { which: ToolDialog ->
        hideKeyboard()
        tool = which
    }
    // Срок и начало на момент открытия окна даты: «←» и «Назад» возвращают их (решение PM — отмена изменений).
    var opened by remember { mutableStateOf<Schedule?>(if (openDateOnStart) Schedule(fields.due, fields.start) else null) }
    val closeTimeline = {
        timelineOpen = false
        // Занятость снова нужна для строки недели — с сегодняшнего дня.
        onGrid(GridAction.FirstDay(fields.today))
    }
    val cancelTimeline = {
        opened?.let { onGrid(GridAction.Restore(it)) }
        closeTimeline()
    }
    // «Готово»: выбор закончен — у повторяющейся задачи дальше окно «Изменить повторяющуюся задачу» (итерация 50).
    val applyTimeline = {
        onGrid(GridAction.Done)
        closeTimeline()
    }
    BackHandler(enabled = timelineOpen, onBack = cancelTimeline)
    LaunchedEffect(dateRequests) {
        if (dateRequests > 0 && !timelineOpen) {
            hideKeyboard()
            opened = Schedule(fields.due, fields.start)
            timelineOpen = true
        }
    }

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
            // Окно задачи — карточка сразу над клавиатурой (0.23.3, макет PM «А»): место не меняется, открыта
            // клавиатура или нет. Окно даты — шторка снизу на всю высоту, от строки статуса до низа.
            val keyboard = with(density) { rememberKeyboardHeight((maxHeight * KEYBOARD_FALLBACK).roundToPx()).toDp() }
            val ime = with(density) { WindowInsets.ime.getBottom(this).toDp() }
            val navBar = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val lift = if (timelineOpen) Dp.Hairline else maxOf(keyboard, ime, navBar) + spacing.xs
            val headerMax = (maxHeight - lift) * HEADER_FRACTION
            val shape = if (timelineOpen) AppTheme.shapes.sheetTop else AppTheme.shapes.nav
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = lift)
                    .then(if (timelineOpen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().padding(horizontal = spacing.xs))
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
                if (timelineOpen) {
                    Timeline(fields, onGrid, hideKeyboard, applyTimeline, cancelTimeline, Modifier.weight(1f).windowInsetsPadding(WindowInsets.navigationBars))
                } else {
                    Column(
                        Modifier
                            .heightIn(max = headerMax)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.md),
                        content = header,
                    )
                    DateRow(
                        fields = fields,
                        onOpenDate = {
                            hideKeyboard()
                            opened = Schedule(fields.due, fields.start)
                            timelineOpen = true
                        },
                        onClearDate = {
                            onGrid(GridAction.ClearDate)
                            onGrid(GridAction.Done)
                        },
                        onClearTime = {
                            onGrid(GridAction.ClearTime)
                            onGrid(GridAction.Done)
                        },
                        modifier = Modifier.padding(start = spacing.md, end = spacing.md),
                    )
                    SheetToolbar(fields, onEdit, checklistActive, onChecklist, actionCount, actions, openTool, Modifier.padding(bottom = spacing.xxs))
                }
            }
        }
        overlay()
    }
    ToolDialogs(tool, fields, onEdit) { tool = ToolDialog.None }
}

/**
 * Окно даты и времени (итерации 33–34, макет `VariantC4`): сетка на 3 дня с часом, подогнанным под высоту (10:00–24:00
 * видно сразу), под ней блок «2 недели», внизу «←» (отмена), «без даты», листание недель, «Календарь» и зелёная галочка.
 * Свайп по сетке листает дни; день в блоке выбирается сразу и переносит сетку к нему.
 */
@Composable
private fun Timeline(
    fields: SheetFields,
    onGrid: (GridAction) -> Unit,
    onInteract: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    val spacing = AppTheme.spacing
    val today = fields.today
    val scroll = remember { GridScroll(WeekGridMath.timelineScroll(today, fields.selectedDate ?: today)) }
    val scope = rememberCoroutineScope()
    val gridFirst by remember(today) { derivedStateOf { today.plusDays(floor(scroll.days).toLong()) } }
    var blockFirst by remember { mutableStateOf(WeekBlockMath.first(today, fields.selectedDate, null)) }
    // Выбранный день ушёл за две недели блока — блок показывает его неделю; стрелки двигают блок сами.
    LaunchedEffect(fields.selectedDate) { blockFirst = WeekBlockMath.first(today, fields.selectedDate, blockFirst) }
    var calendar by remember { mutableStateOf(false) }
    val pick: (LocalDate) -> Unit = { date ->
        onGrid(GridAction.PickDay(date))
        val target = WeekGridMath.timelineScroll(today, date)
        scope.launch { animate(scroll.days, target) { value, _ -> scroll.days = value } }
    }
    Column(modifier.fillMaxWidth().padding(top = spacing.xs)) {
        WeekGrid(
            scroll = scroll,
            today = today,
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
            fitHours = WeekGridMath.OPEN_HOURS,
            startMinute = WeekGridMath.openMinute(fields.selectedMinute),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        WeekBlock(today = today, selected = fields.selectedDate, gridFirst = gridFirst, first = blockFirst, onPick = pick)
        WindowBar(
            first = blockFirst,
            canGoBack = blockFirst > WeekBlockMath.monday(today),
            onCancel = onCancel,
            onNoDate = {
                onGrid(GridAction.ClearDate)
                onApply()
            },
            onFirstChange = { blockFirst = it },
            onCalendar = {
                onInteract()
                calendar = true
            },
            onDone = onApply,
        )
    }
    if (calendar) {
        CalendarDialog(
            today = today,
            selected = fields.selectedDate,
            onPick = {
                calendar = false
                pick(it)
            },
            onDismiss = { calendar = false },
        )
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
    open: (ToolDialog) -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xs)) {
    val count = TOOLBAR_TOOLS + actionCount
    // В строке ещё и распорка между кнопками и действиями — поэтому щелей на одну больше, чем кнопок (0.31.2: раньше
    // не считалась, и у созданной задачи корзина прижималась к галочке).
    val fit = ((maxWidth - spacing.xs * count) / count).coerceAtMost(SizeTokens.TOOL_BUTTON.dp)
    CompositionLocalProvider(LocalToolButtonSize provides fit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val project = fields.projects.firstOrNull { it.id == fields.projectId }
        val inbox = fields.projects.firstOrNull { it.isInbox }
        ToolButton(
            R.string.quick_tool_list,
            active = project?.isInbox == false,
            tag = TAG_TOOL_LIST,
            clear = if (project?.isInbox == false && inbox != null) ClearAction(R.string.clear_list, TAG_CLEAR_LIST) { onEdit(FieldEdit.Project(inbox.id)) } else null,
            content = {
                // Без списка: с датой — 📌, без даты — 📥 (итерация 52.1).
                ListBadge(ListEmoji.ofTask(project?.isInbox != false, fields.due != null, project?.emoji ?: ListEmoji.INBOX), large = true)
            },
        ) { open(ToolDialog.List) }
        ToolButton(
            R.string.quick_tool_priority,
            active = fields.priority != Priority.NONE,
            tag = TAG_TOOL_PRIORITY,
            icon = R.drawable.ic_tool_priority,
            activeTint = priorityColor(fields.priority),
        ) {
            // Итерация 47 (решение PM): окна приоритета нет — нажатие включает высокий, повторное выключает.
            onEdit(FieldEdit.SetPriority(if (fields.priority == Priority.HIGH) Priority.NONE else Priority.HIGH))
        }
        ToolButton(R.string.quick_tool_checklist, active = checklistActive, tag = TAG_TOOL_CHECKLIST, icon = R.drawable.ic_tool_checklist, onClick = onChecklist)
        // Повтор и уведомления доступны, если есть дата; без неё бледные, а в их окнах — подсказка «Сначала выберите день».
        ToolButton(
            R.string.quick_tool_repeat,
            active = fields.rrule != null,
            tag = TAG_TOOL_REPEAT,
            icon = R.drawable.ic_tool_repeat,
            dimmed = fields.due == null,
        ) { open(ToolDialog.Repeat) }
        val mark = Reminders.mark(fields.due, fields.reminders)
        ToolButton(
            R.string.quick_tool_reminder,
            active = mark != null,
            tag = TAG_TOOL_REMINDER,
            icon = if (mark == null) R.drawable.ic_tool_reminder else null,
            dimmed = fields.due == null,
            content = {
                if (mark != null) {
                    ReminderIcon(mark, LocalToolButtonSize.current * ICON_SHARE, tint = colors.accent, earlyColor = colors.accent)
                }
            },
        ) { open(ToolDialog.Reminder) }
        Spacer(Modifier.weight(1f))
        actions()
    }
    }
    }
}

/** Окна кнопок шторки: меняют задачу только по «Готово» и только если выбор изменился. */
@Composable
private fun ToolDialogs(dialog: ToolDialog, fields: SheetFields, onEdit: (FieldEdit) -> Unit, close: () -> Unit) {
    when (dialog) {
        ToolDialog.None -> Unit
        ToolDialog.List -> ProjectDialog(
            projectId = fields.projectId,
            projects = fields.projects,
            onConfirm = { if (it != fields.projectId) onEdit(FieldEdit.Project(it)); close() },
            onDismiss = close,
            hasDate = fields.due != null,
        )
        ToolDialog.Repeat -> RepeatDialog(
            rrule = fields.rrule,
            due = fields.selectedDate,
            onConfirm = { if (it != fields.rrule) onEdit(FieldEdit.Repeat(it)); close() },
            onDismiss = close,
        )
        ToolDialog.Reminder -> RemindersDialog(
            due = fields.due,
            start = fields.start,
            zone = fields.zone,
            reminders = fields.reminders,
            onConfirm = { if (it != fields.reminders) onEdit(FieldEdit.SetReminders(it)); close() },
            onDismiss = close,
        )
    }
}

private enum class ToolDialog { None, List, Repeat, Reminder }

/**
 * Квадратная кнопка панели (макет PM): выбранная — залита акцентом; [dimmed] — пока нет даты: фон и рамка те же,
 * бледная только иконка (итерация 31, ТЗ PM — раньше бледнела вся кнопка и сливалась с фоном).
 */
@Composable
fun ToolButton(
    label: Int,
    active: Boolean,
    tag: String,
    @DrawableRes icon: Int? = null,
    dimmed: Boolean = false,
    tint: Color? = null,
    activeTint: Color? = null,
    clear: ClearAction? = null,
    content: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Box {
        ToolButtonBody(label, active, tag, icon, dimmed, tint, activeTint, content, onClick)
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
    activeTint: Color?,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val shape = AppTheme.shapes.large
    val description = stringResource(label)
    val jelly = rememberJelly()
    // Выбранная кнопка залита светлым оттенком акцента, а если у неё свой цвет ([activeTint], приоритет) — его светлым оттенком.
    val fill = when {
        !active -> colors.surface
        activeTint != null -> activeTint.copy(alpha = ACTIVE_FILL_ALPHA).compositeOver(colors.surface)
        else -> colors.selected
    }
    // Итерация 24: кнопки крупнее (50 dp, скругление как у плиток дней), обводка тоньше — в 1 dp (вариант PM «А»).
    Box(
        Modifier
            .size(LocalToolButtonSize.current)
            .jelly(jelly)
            .clip(shape)
            .background(fill)
            .border(sizes.hairline, if (active) fill else colors.divider, shape)
            .jellyClick(jelly, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = active
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            val iconTint = when {
                active -> activeTint ?: colors.accent
                dimmed -> colors.inactive.copy(alpha = DIMMED_ICON_ALPHA)
                else -> tint ?: colors.textSecondary
            }
            Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(LocalToolButtonSize.current * ICON_SHARE))
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
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    val colors = AppTheme.colors
    val style = if (textStyle.color == Color.Unspecified) textStyle.copy(color = colors.textPrimary) else textStyle
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = visualTransformation,
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

/** То же поле, но с выделением текста (итерация 38): подставленный из черновика текст выделен, любая буква заменит его. */
@Composable
fun CompactField(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    hint: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    val colors = AppTheme.colors
    val style = if (textStyle.color == Color.Unspecified) textStyle.copy(color = colors.textPrimary) else textStyle
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.accent),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xxs),
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) Text(hint, style = style.copy(color = colors.inactive))
                inner()
            }
        },
    )
}

/** Подсветка фраз срока в названии (итерация 41); без найденной фразы — обычное поле. */
@Composable
fun smartHighlight(smart: SmartTitle): androidx.compose.ui.text.input.VisualTransformation {
    val ranges = smart.found?.ranges ?: return androidx.compose.ui.text.input.VisualTransformation.None
    val style = smartDateSpan()
    return remember(ranges, style) { SmartDateHighlight(ranges, style) }
}

/** Чип «Завтра, 10:00» под названием, пока в нём найдена фраза срока (итерация 41). */
@Composable
fun SmartDateLine(smart: SmartTitle, today: java.time.LocalDate, zone: java.time.ZoneId, onDismiss: () -> Unit) {
    val found = smart.found ?: return
    val due = DueFormatter.format(found.due(zone), null, today, zone) ?: return
    val names = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    SmartDateChip(
        text = names.format(due),
        offLabel = stringResource(R.string.smart_date_off),
        onDismiss = onDismiss,
        modifier = Modifier.padding(vertical = AppTheme.spacing.xxs),
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
/** Верхняя часть шторки (поля, чек-лист) — не больше этой доли экрана. */
private const val HEADER_FRACTION = 0.45f

/** Пока клавиатура ни разу не открывалась — её высота примерно 40% экрана. */
private const val KEYBOARD_FALLBACK = 0.4f

/** Иконка недоступной кнопки (нет даты) заметно бледнее доступных (0.31.2, решение PM). */
private const val DIMMED_ICON_ALPHA = 0.6f

/** Прозрачность цвета приоритета в заливке выбранной кнопки панели. */
private const val ACTIVE_FILL_ALPHA = 0.16f

const val TAG_QUICK_CARD = "quick_card"
const val TAG_QUICK_SCRIM = "quick_scrim"
const val TAG_TOOL_LIST = "tool_list"
const val TAG_TOOL_REPEAT = "tool_repeat"
const val TAG_TOOL_REMINDER = "tool_reminder"
const val TAG_TOOL_PRIORITY = "tool_priority"
const val TAG_TOOL_CHECKLIST = "tool_checklist"
const val TAG_REMINDER_NEED_DAY = "reminder_need_day"
const val TAG_TIMELINE_DONE = "timeline_done"
const val TAG_TIMELINE_NO_DATE = "timeline_no_date"
const val TAG_CLEAR_LIST = "clear_list"
const val TAG_CLEAR_PRIORITY = "clear_priority"
const val TAG_CLEAR_DATE = "clear_date"


/** Цвет приоритета (как у кружка в ленте) или null, если приоритета нет. */
@Composable
fun priorityColor(priority: Priority): Color? {
    val rings = AppTheme.colors.checkRings
    return when (priority) {
        Priority.HIGH -> rings.high
        Priority.NONE -> null
    }
}

/**
 * Размер квадратных кнопок панели (итерация 24): 50 dp, а если все кнопки и действия справа не помещаются
 * в ширину (у созданной задачи их 7) — поровну меньше.
 */
val LocalToolButtonSize = compositionLocalOf { SizeTokens.TOOL_BUTTON.dp }

/** Иконка занимает половину кнопки (20 из 40, 25 из 50). */
private const val ICON_SHARE = 0.5f

/** Кнопки панели слева: список, приоритет, чек-лист, повтор, уведомления. */
private const val TOOLBAR_TOOLS = 5
