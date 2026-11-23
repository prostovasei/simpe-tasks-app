package com.prostovasei.simpletasks.ui.feature.week

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListEffects
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.rememberNowMinute
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.widget.WidgetDates
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Шторка дня поверх рабочего стола (итерация 51): данные — те же, что у полного списка дня в приложении ([DayViewModel]).
 * [origin] — ячейка дня в виджете (`Intent.sourceBounds`), из неё окно растёт и в неё сжимается (#314).
 */
@Composable
fun DaySheetRoute(
    onClose: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAdd: (LocalDate) -> Unit,
    origin: android.graphics.Rect? = null,
    viewModel: DayViewModel = hiltViewModel(),
) {
    val state by remember(viewModel) { viewModel.day(viewModel.startDate) }.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TaskListEffects(viewModel, snackbar)
    DaySheet(
        state, viewModel::onIntent, onClose, onOpenTask, onAdd, snackbar,
        nowMinute = rememberNowMinute(),
        origin = origin,
        dayOf = { date -> remember(date) { viewModel.day(date) }.collectAsStateWithLifecycle().value },
    )
}

/**
 * Шторка дня (итерация 51, макет iteration51.py, доска «Шторка дня»): нажатие на дату дня в «Неделе» виджета — приложение не
 * открывается. Сверху компактная дата и вытянутый «+» без относительных подписей и счётчика (#265), ниже лента дня (кружок отмечает,
 * название открывает задачу). Шторка не выше [SHEET_MAX_FRACTION] экрана, дальше лента прокручивается. «Назад»,
 * смахивание вниз за верх шторки и нажатие на затемнение закрывают.
 *
 * #314 (идеи PM 1, 2, 4, 8, 11, 12): окно растёт из ячейки виджета и сжимается в неё при закрытии; задачи появляются каскадом;
 * дни листаются свайпом (заголовок движется медленнее); потянуть за верх — резинка, сильнее — окно на весь экран;
 * выполнена последняя задача дня — искры и подскок окна. [state] — день, с которого открыли; [dayOf] — любой другой день.
 */
@Composable
fun DaySheet(
    state: DayState,
    onIntent: (TaskListIntent) -> Unit,
    onClose: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAdd: (LocalDate) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    nowMinute: Int? = null,
    origin: android.graphics.Rect? = null,
    dayOf: @Composable (LocalDate) -> DayState = { date -> if (date == state.date) state else DayState(date = date, today = state.today, isLoading = false) },
) {
    val start = state.date ?: return
    TaskCanvasTheme {
        val colors = AppTheme.colors
        val spacing = AppTheme.spacing
        val sizes = AppTheme.sizes
        val density = LocalDensity.current
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
        val pager = rememberPagerState(initialPage = PAGE_CENTER) { PAGE_COUNT }
        val dateAt: (Int) -> LocalDate = { start.plusDays((it - PAGE_CENTER).toLong()) }

        // Возврат из окна задачи: окно уже открыто — не растёт и не каскадит заново.
        var opened by rememberSaveable { mutableStateOf(false) }
        var expanded by rememberSaveable { mutableStateOf(false) }
        val quietPage = remember { if (opened) pager.currentPage else NO_PAGE }
        val scrim = remember { Animatable(if (opened) 1f else 0f) }
        val grow = remember { Animatable(if (opened) 1f else 0f) }
        val reveal = remember { Animatable(if (opened) 1f else 0f) }
        val fade = remember { Animatable(1f) }
        val bounce = remember { Animatable(0f) }
        val stretch = remember { Animatable(0f) }
        val expand = remember { Animatable(if (expanded) 1f else 0f) }
        var morph by remember { mutableStateOf(origin != null) }
        var shrinking by remember { mutableStateOf(false) }
        var closing by remember { mutableStateOf(false) }
        var sheetAt by remember { mutableStateOf(Offset.Zero) }
        var lastTap by remember { mutableStateOf<Offset?>(null) }
        val celebration = remember { Celebration() }
        // Ячейка виджета — в координатах окна.
        val cell = remember(origin) {
            origin?.let {
                val at = IntArray(2).also(view::getLocationOnScreen)
                Rect(Offset((it.left - at[0]).toFloat(), (it.top - at[1]).toFloat()), androidx.compose.ui.geometry.Size(it.width().toFloat(), it.height().toFloat()))
            }
        }

        LaunchedEffect(Unit) {
            if (opened) return@LaunchedEffect
            launch { scrim.animateTo(1f, tween(DAY_SCRIM_MS)) }
            if (cell != null) {
                launch {
                    motionDelay(DAY_REVEAL_DELAY_MS.toLong())
                    reveal.animateTo(1f, tween(DAY_REVEAL_MS))
                }
                grow.animateTo(1f, tween(DAY_GROW_MS, easing = DAY_GROW_EASING))
            } else {
                launch { reveal.animateTo(1f, tween(DAY_SLIDE_MS)) }
                grow.animateTo(1f, tween(DAY_SLIDE_MS, easing = DAY_SLIDE_EASING))
            }
            opened = true
        }

        val currentOnClose by rememberUpdatedState(onClose)
        val close: () -> Unit = remember {
            {
                if (!closing) {
                    closing = true
                    scope.launch {
                        // В ячейку — только тот день, из которого выросло окно, и не развёрнутое на весь экран.
                        val intoCell = cell != null && dateAt(pager.currentPage) == start && expand.value == 0f
                        morph = intoCell
                        if (intoCell) {
                            shrinking = true
                            launch { reveal.animateTo(0f, tween(DAY_HIDE_MS)) }
                            launch {
                                motionDelay(DAY_SCRIM_OUT_DELAY_MS.toLong())
                                scrim.animateTo(0f, tween(DAY_SCRIM_OUT_MS))
                            }
                            grow.animateTo(0f, tween(DAY_SHRINK_MS, easing = DAY_SHRINK_EASING))
                            fade.animateTo(0f, tween(DAY_SHRINK_FADE_MS))
                        } else {
                            launch { scrim.animateTo(0f, tween(DAY_SLIDE_CLOSE_MS)) }
                            grow.animateTo(0f, tween(DAY_SLIDE_CLOSE_MS))
                        }
                        currentOnClose()
                    }
                }
            }
        }
        BackHandler(onBack = close)

        val closeDistance = with(density) { sizes.toolButton.toPx() }
        val expandDistance = with(density) { SizeTokens.DAY_EXPAND_DRAG.dp.toPx() }
        val stretchLimit = with(density) { SizeTokens.DAY_STRETCH.dp.toPx() }
        val bouncePx = with(density) { SizeTokens.DAY_BOUNCE.dp.toPx() }
        val radiusFrom = with(density) { RadiusTokens.MD.dp.toPx() }
        val radiusTo = with(density) { RadiusTokens.NAV.dp.toPx() }
        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                // Где нажали в последний раз — оттуда летят искры (кружок последней задачи); нажатие не перехватывается.
                .pointerInput(Unit) {
                    awaitEachGesture { lastTap = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position }
                },
        ) {
            val baseMax = maxHeight * SHEET_MAX_FRACTION
            val fullMax = maxHeight - topInset - bottomInset - spacing.xs * 2
            val e = expand.value
            val stretchDp = with(density) { stretch.value.toDp() }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrim.value }
                    .background(colors.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
                    .testTag(TAG_DAY_SCRIM),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = spacing.xs, end = spacing.xs, bottom = spacing.xs)
                    .heightIn(min = lerp(Dp.Hairline, fullMax, e), max = lerp(baseMax, fullMax, e) + stretchDp)
                    .fillMaxWidth()
                    .onGloballyPositioned { sheetAt = it.positionInRoot() }
                    .graphicsLayer {
                        val p = grow.value
                        val c = cell
                        val lift = bounce.value * bouncePx
                        if (morph && c != null && size.width > 0f && size.height > 0f) {
                            // 1 и 11: плашка ячейки растягивается в окно (и обратно), скругление 8 → 22 dp.
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = (c.left - sheetAt.x) * (1f - p)
                            translationY = (c.top - sheetAt.y) * (1f - p) - lift
                            scaleX = c.width / size.width + (1f - c.width / size.width) * p
                            scaleY = c.height / size.height + (1f - c.height / size.height) * p
                            alpha = fade.value * (if (shrinking) DAY_SHRINK_ALPHA + (1f - DAY_SHRINK_ALPHA) * p else 1f)
                            shape = RoundedCornerShape(radiusFrom + (radiusTo - radiusFrom) * p)
                        } else {
                            translationY = (1f - p) * (size.height + spacing.xs.toPx() + bottomInset.toPx()) - lift
                            shape = RoundedCornerShape(radiusTo)
                        }
                        clip = true
                    }
                    .background(colors.surface)
                    // Нажатия по шторке не доходят до затемнения (без clickable: он склеил бы строки в один элемент для чтения с экрана).
                    .pointerInput(Unit) { detectTapGestures { } }
                    .testTag(TAG_DAY_SHEET),
            ) {
                Column(Modifier.weight(1f, fill = false).graphicsLayer { alpha = reveal.value }) {
                    DayHeader(
                        pager = pager,
                        dateAt = dateAt,
                        today = state.today,
                        onAdd = onAdd,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                var dragged = 0f
                                val settle: () -> Unit = {
                                    scope.launch { stretch.animateTo(0f, spring(dampingRatio = STRETCH_DAMPING, stiffness = Spring.StiffnessMediumLow)) }
                                }
                                detectVerticalDragGestures(
                                    onDragStart = { dragged = 0f },
                                    onDragCancel = settle,
                                    onDragEnd = {
                                        when {
                                            // Вниз: развёрнутое — обратно, обычное — закрыть.
                                            dragged > closeDistance -> if (expanded) {
                                                expanded = false
                                                scope.launch { expand.animateTo(0f, tween(DAY_GROW_MS, easing = DAY_GROW_EASING)) }
                                            } else close()
                                            // 12: потянули вверх сильнее — на весь экран.
                                            -dragged > expandDistance && !expanded -> {
                                                expanded = true
                                                scope.launch { expand.animateTo(1f, tween(DAY_GROW_MS, easing = DAY_GROW_EASING)) }
                                            }
                                        }
                                        settle()
                                    },
                                ) { _, delta ->
                                    dragged += delta
                                    if (!expanded) scope.launch { stretch.snapTo(rubber(-dragged, stretchLimit)) }
                                }
                            }
                            // 8: свайп по заголовку тоже листает дни.
                            .draggable(
                                rememberDraggableState { pager.dispatchRawDelta(-it) },
                                Orientation.Horizontal,
                                onDragStopped = { velocity -> pager.animateScrollToPage(settlePage(pager, velocity)) },
                            ),
                    )
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth().animateContentSize().testTag(TAG_DAY_SHEET_PAGER),
                        verticalAlignment = Alignment.Top,
                        key = { it },
                    ) { page ->
                        val date = dateAt(page)
                        val day = dayOf(date)
                        val today = day.today
                        DayPage(
                            day = day,
                            days = days,
                            nowMinute = nowMinute.takeIf { date == today },
                            cascadeStart = if (page == PAGE_CENTER && quietPage == NO_PAGE) (if (cell != null) DAY_REVEAL_DELAY_MS else 0) else 0,
                            cascade = page != quietPage,
                            onToggle = { item ->
                                // 4: закрыта последняя задача дня — праздник.
                                if (closesDay(day.items, item)) lastTap?.let(celebration::fire)
                                onIntent(TaskListIntent.ToggleComplete(item.id, item.isRecurring, item.showsDone))
                            },
                            onOpen = { onOpenTask(it.taskId) },
                        )
                    }
                }
                Spacer(Modifier.height(stretchDp))
            }
            Sparks(celebration, bounce)
            UndoSnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = spacing.sm))
        }
    }
}

/** Заголовок: дата текущего дня и «+». 8: при листании дата сдвигается вдвое медленнее окна и сменяется плавно. */
@Composable
private fun DayHeader(pager: PagerState, dateAt: (Int) -> LocalDate, today: LocalDate?, onAdd: (LocalDate) -> Unit, modifier: Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val current = pager.currentPage
    Row(
        modifier.fillMaxWidth().padding(horizontal = spacing.sm, vertical = spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(Modifier.weight(1f).clipToBounds()) {
            for (page in current - 1..current + 1) {
                val date = dateAt(page)
                Text(
                    WidgetDates.dayHeader(date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (today != null && date < today) colors.overdue else colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val shift = page - (pager.currentPage + pager.currentPageOffsetFraction)
                            translationX = shift * size.width * TITLE_PARALLAX
                            alpha = (1f - abs(shift) * 2f).coerceIn(0f, 1f)
                        }
                        .then(if (page == current) Modifier.testTag(TAG_DAY_SHEET_TITLE) else Modifier.clearAndSetSemantics { }),
                )
            }
        }
        val date = dateAt(current)
        val addLabel = stringResource(R.string.day_sheet_add, WidgetDates.withWeekday(date))
        Box(
            Modifier.size(width = SizeTokens.WIDGET_ADD_W.dp, height = sizes.toolButton)
                .clip(AppTheme.shapes.extraLarge)
                .clickable(role = Role.Button) { onAdd(date) }
                .semantics { contentDescription = addLabel }
                .testTag(TAG_DAY_SHEET_ADD),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(SizeTokens.WIDGET_ADD_W.dp, SizeTokens.WIDGET_ADD_H.dp)
                    .clip(AppTheme.shapes.extraLarge).background(colors.accent)
                    .testTag(TAG_DAY_SHEET_ADD_VISUAL),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(SizeTokens.WIDGET_ADD_ICON.dp))
            }
        }
    }
}

/** Лента одного дня в листалке. 2: строки появляются каскадом, пока день только что показан ([cascade]). */
@Composable
private fun DayPage(
    day: DayState,
    days: DayNames,
    nowMinute: Int?,
    cascadeStart: Int,
    cascade: Boolean,
    onToggle: (TaskItem) -> Unit,
    onOpen: (TaskItem) -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val date = day.date ?: return
    val today = day.today
    val rows = remember { Cascade(cascadeStart).also { it.open = cascade } }
    LaunchedEffect(day.isLoading) {
        if (!day.isLoading && rows.open) {
            motionDelay((rows.startMs + CASCADE_WINDOW_MS).toLong())
            rows.open = false
        }
    }
    if (!day.isLoading && day.items.isEmpty()) {
        Text(
            stringResource(R.string.day_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth().background(colors.background).padding(spacing.lg),
        )
    } else if (today != null) {
        LazyColumn(
            Modifier.fillMaxWidth().background(colors.background).testTag(TAG_DAY_SHEET_LIST),
            contentPadding = PaddingValues(vertical = spacing.xs),
        ) {
            timelineItems(
                items = day.items,
                keyPrefix = "day-sheet-$date",
                days = days,
                nowMinute = nowMinute,
                showNowMarker = false,
                isOverdue = { date < today && !it.showsDone },
                showList = true,
                onToggle = onToggle,
                onOpen = onOpen,
                rowMotion = { index, content -> CascadeRow(rows, index, content) },
            )
        }
    }
}

/** 4: отметка [item] закрывает день — все остальные задачи дня уже выполнены. */
internal fun closesDay(items: List<TaskItem>, item: TaskItem): Boolean = !item.showsDone && items.none { it.id != item.id && !it.showsDone }

/** Куда долистать после свайпа по заголовку: быстрый взмах — на соседний день, иначе — к ближайшему. */
private fun settlePage(pager: PagerState, velocity: Float): Int {
    val position = pager.currentPage + pager.currentPageOffsetFraction
    return when {
        velocity < -FLING_VELOCITY -> ceil(position).toInt()
        velocity > FLING_VELOCITY -> floor(position).toInt()
        else -> position.roundToInt()
    }.coerceIn(0, PAGE_COUNT - 1)
}

/** Шторка не выше этой доли экрана; дальше лента прокручивается внутри (макет: ~80%). */
private const val SHEET_MAX_FRACTION = 0.8f

/** 8: дней в листалке — по 10 лет в обе стороны от открытого. */
private const val PAGE_CENTER = 3650
private const val PAGE_COUNT = PAGE_CENTER * 2 + 1
private const val NO_PAGE = -1
private const val TITLE_PARALLAX = 0.5f
private const val FLING_VELOCITY = 400f
private const val STRETCH_DAMPING = 0.5f

const val TAG_DAY_SHEET = "day_sheet"
const val TAG_DAY_SCRIM = "day_sheet_scrim"
const val TAG_DAY_SHEET_TITLE = "day_sheet_title"
const val TAG_DAY_SHEET_COUNT = "day_sheet_count"
const val TAG_DAY_SHEET_LIST = "day_sheet_list"
const val TAG_DAY_SHEET_PAGER = "day_sheet_pager"
const val TAG_DAY_SHEET_ADD = "day_sheet_add"

const val TAG_DAY_SHEET_ADD_VISUAL = "day_sheet_add_visual"
