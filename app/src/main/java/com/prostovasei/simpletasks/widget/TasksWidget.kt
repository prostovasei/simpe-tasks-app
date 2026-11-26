package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.designsystem.tokens.NavBarTokens
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.compose.ui.unit.Dp
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.ReminderIconMasks
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.canvasNodeIcon
import com.prostovasei.simpletasks.widget.theme.WidgetCheckColors
import androidx.compose.runtime.CompositionLocalProvider
import com.prostovasei.simpletasks.widget.theme.LocalWidgetColors
import com.prostovasei.simpletasks.widget.theme.TaskWidgetColors
import com.prostovasei.simpletasks.widget.theme.WidgetColors
import java.time.LocalDate
import kotlin.math.roundToInt

/*
 * Виджет «Задачи» (SPEC.md §5; редизайн — итерации 08–09, 14, 20). Лента по дням: подпись дня, под ней — шкала дня:
 * время слева, линия с кружками приоритета (нажатие — «выполнено»), белые плашки с квадратиком списка справа. Внизу — вкладки списков, кнопка приложения,
 * сегодняшняя дата и «+». Рисует `WidgetRenderer` сразу при событии, без фонового сеанса Glance (0.9.2).
 */

/**
 * [scrollable] = true (Android 12+) — только верхний слой: пустая лента и нижняя панель; фон и ленту рисует
 * [widgetRootViews] (итерация 42). [scrollable] = false — весь виджет с лентой без прокрутки (Android 10–11 и снимки),
 * [liveClock] = false — неподвижное время отметки «сейчас» (только для снимков в тестах).
 */
@Composable
fun WidgetContent(model: WidgetModel, appWidgetId: Int, scrollable: Boolean = true, liveClock: Boolean = true) {
    CompositionLocalProvider(LocalWidgetColors provides (if (model.filter == WidgetFilter.Inbox) WidgetColors else TaskWidgetColors)) {

        val context = LocalContext.current
        // Список идёт до самого низа, кнопки нижней панели лежат прямо на нём, без подложки и перехода к фону (итерация 43).
        val frame = GlanceModifier.fillMaxSize()
        Box(
            modifier = if (scrollable) frame else frame.background(LocalWidgetColors.current.background).cornerRadius(RadiusTokens.XL.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Правое поле плашек — у строк; слева поле минимальное: время прижато к краю (итерация 20).
            Box(GlanceModifier.fillMaxSize().padding(start = SizeTokens.WIDGET_LIST_EDGE.dp, top = SizeTokens.WIDGET_EDGE.dp)) {
                if (model.isEmpty) {
                    Box(GlanceModifier.fillMaxSize().padding(bottom = SizeTokens.WIDGET_TAIL.dp), contentAlignment = Alignment.Center) {
                        Text(context.getString(R.string.widget_empty), style = TextStyle(color = LocalWidgetColors.current.textSecondary, fontSize = 14.sp))
                    }
                } else if (!scrollable) {
                    val lines = model.lines(dayNames(context), withTail = false)
                    Column(GlanceModifier.fillMaxSize()) { ManyRows(lines) { WidgetLineContent(it, model, liveClock) } }
                }
            }
            CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) {
                BottomBar(model, appWidgetId, imageButtons = scrollable)
            }
        }
    }
}

fun dayNames(context: Context) =
    DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))

/** Одна строка ленты без прокрутки (Android 10–11 и снимки): подпись «Просрочено», подпись дня, задача, «сейчас», хвост. */
@Composable
internal fun WidgetLineContent(line: WidgetLine, model: WidgetModel, liveClock: Boolean) {
    when (line) {
        is WidgetLine.Overdue -> OverdueTitle(line.entry, line.isFirst)
        // Правое поле плашек; за ним, у самого края виджета, — полоска прокрутки.
        is WidgetLine.Title -> Box(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_LIST_EDGE.dp)) {
            DayTitle(line.day, model.today, line.isFirst, line.progress, line.count)
        }
        is WidgetLine.Task -> Box(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_LIST_EDGE.dp)) {
            TimelineRow(line.row, line.line, line.overdue, model.filter)
        }
        is WidgetLine.Now -> Box(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_LIST_EDGE.dp)) {
            NowRow(line.line, line.nowMinute, liveClock)
        }
        // Место под панелью в конце списка: последние задачи можно докрутить выше неё.
        WidgetLine.Tail -> Spacer(GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_TAIL.dp))
    }
}

/**
 * «Просрочено · N» (итерация 37): красная подпись над прошедшими днями; нажатие на подпись со стрелкой сворачивает их,
 * значок рядом — переносит все просроченные на сегодня (окно с «Отменой» поверх рабочего стола).
 */
@Composable
private fun OverdueTitle(entry: WidgetEntry.Overdue, isFirst: Boolean) {
    val context = LocalContext.current
    val style = TextStyle(color = LocalWidgetColors.current.overdue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val text = if (entry.count > 0) context.getString(R.string.overdue_count, entry.count) else context.getString(R.string.overdue_title)
    val fold = context.getString(if (entry.collapsed) R.string.overdue_fold_closed else R.string.overdue_fold_open)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(
            start = SizeTokens.WIDGET_EDGE.dp,
            top = if (isFirst) SizeTokens.WIDGET_DAY_GAP.dp else SizeTokens.WIDGET_EDGE.dp,
            bottom = SizeTokens.WIDGET_DAY_GAP.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(GlanceModifier.clickable(actionSendBroadcast(WidgetActions.toggleOverdue(context))), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = style, maxLines = 1)
            Spacer(GlanceModifier.width(SpacingTokens.XXS.dp))
            Image(
                ImageProvider(if (entry.collapsed) R.drawable.ic_fold_closed else R.drawable.ic_fold_open),
                contentDescription = fold,
                colorFilter = ColorFilter.tint(LocalWidgetColors.current.overdue),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_FOLD_ICON.dp),
            )
        }
        if (entry.count > 0) {
            Spacer(GlanceModifier.width(SpacingTokens.SM.dp))
            Image(
                ImageProvider(R.drawable.ic_move_today),
                contentDescription = context.getString(R.string.overdue_move_today),
                colorFilter = ColorFilter.tint(LocalWidgetColors.current.overdue),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_FOLD_ICON.dp).then(GlanceModifier.clickable(actionStartActivity(moveOverdueIntent(context)))),
            )
        }
    }
}

/** Куда от кружка идёт линия шкалы: вверх (есть строка выше) и вниз (есть строка ниже). */
data class Line(val top: Boolean, val bottom: Boolean)

/**
 * Строки без потерь: в Column виджета помещается не больше [MAX_CHILDREN] элементов, остальные Glance молча
 * отбрасывает. В колонке дня одно место занимает подпись дня, поэтому строкам — [MAX_CHILDREN] − 1;
 * длинный список раскладывается во вложенные колонки (в них — по 10 и меньше).
 */
@Composable
internal fun <T> ManyRows(items: List<T>, limit: Int = MAX_CHILDREN, row: @Composable (T) -> Unit) {
    val chunks = rowChunks(items, limit)
    if (chunks == null) {
        items.forEach { row(it) }
    } else {
        chunks.forEach { chunk -> Column(GlanceModifier.fillMaxWidth()) { ManyRows(chunk, MAX_CHILDREN, row) } }
    }
}

/** null — [items] помещаются в одну колонку ([limit] детей); иначе — не больше [limit] частей поровну. */
internal fun <T> rowChunks(items: List<T>, limit: Int): List<List<T>>? =
    if (items.size <= limit) null else items.chunked((items.size + limit - 1) / limit)

/** Предел детей у Row/Column в Glance. */
internal const val MAX_CHILDREN = 10

/**
 * «ПТ, 16 окт» + белая метка «Сегодня» / «Завтра» / «Вчера» (итерации 14, 27 — как в приложении), «Без даты».
 * Прошедший день — красным. Справа — число задач дня (итерация 43); у «Сегодня» вместо него прогресс.
 */
@Composable
private fun DayTitle(day: WidgetDay, today: LocalDate, isFirst: Boolean, progress: DayProgress? = null, count: Int? = null) {
    val title = widgetDayTitle(LocalContext.current, day, today)
    // #334: «вт 25 ноя · Сегодня» одной строкой 12 sp средним серым; прошедший день — красным.
    val color = if (title.isPast) LocalWidgetColors.current.overdue else LocalWidgetColors.current.textSecondary
    val style = TextStyle(color = color, fontSize = SizeTokens.DAY_HEAD_SP.sp, fontWeight = FontWeight.Medium)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(
            start = SizeTokens.WIDGET_EDGE.dp,
            top = if (isFirst) SizeTokens.WIDGET_DAY_GAP.dp else SizeTokens.WIDGET_EDGE.dp,
            bottom = SizeTokens.WIDGET_DAY_GAP.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.relative?.let { "${title.text} · $it" } ?: title.text, style = style, maxLines = 1)
        if (progress != null) {
            DayProgressBar(progress)
        } else if (count != null) {
            Spacer(GlanceModifier.defaultWeight())
            Text(count.toString(), style = TextStyle(color = LocalWidgetColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }
}

/**
 * Прогресс дня у «Сегодня» (итерация 30): тонкая зелёная линия до правого края и «3 из 7».
 * Всё выполнено — линия полностью зелёная.
 */
@Composable
private fun RowScope.DayProgressBar(progress: DayProgress) {
    val context = LocalContext.current
    Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
    LinearProgressIndicator(
        progress = progress.fraction,
        modifier = GlanceModifier.defaultWeight().height(SizeTokens.WIDGET_PROGRESS.dp).cornerRadius(RadiusTokens.SM.dp),
        color = LocalWidgetColors.current.accent,
        backgroundColor = LocalWidgetColors.current.divider,
    )
    Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
    Text(
        context.getString(R.string.widget_progress, progress.done, progress.total),
        style = TextStyle(color = LocalWidgetColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

/** Подпись дня: [text] — «ПТ, 16 ОКТ»; [relative] — «Сегодня» / «Завтра» / «Вчера» или null; [isPast] — день прошёл. */
data class WidgetDayTitle(val text: String, val relative: String?, val isToday: Boolean, val isPast: Boolean = false)

fun widgetDayTitle(context: Context, day: WidgetDay, today: LocalDate): WidgetDayTitle = when (day) {
    WidgetDay.NoDate -> WidgetDayTitle(context.getString(R.string.date_none), null, false)
    is WidgetDay.On -> WidgetDayTitle(
        text = WidgetDates.header(day.date),
        relative = when (day.date) {
            today -> context.getString(R.string.day_today)
            today.plusDays(1) -> context.getString(R.string.day_tomorrow)
            today.minusDays(1) -> context.getString(R.string.day_yesterday)
            else -> null
        },
        isToday = day.date == today,
        isPast = day.date < today,
    )
}

/**
 * Строка шкалы: слева время (у периода — начало и конец, у задачи на весь день — пусто), по центру линия с кружком
 * приоритета (нажатие — «выполнено», у выполненной — вернуть), справа плашка: название и значки справа налево —
 * список, колокольчик (итерация 27; повтор с итерации 47 — знаком вместо кружка). Выполненная — полупрозрачная (#316, без зачёркивания),
 * кружок залит с белой галочкой. Анимаций в виджете нет — вид меняется сразу.
 */
@Composable
private fun TimelineRow(row: WidgetRow.Task, line: Line, overdue: Boolean, filter: WidgetFilter) {
    val context = LocalContext.current
    val item = row.item
    val done = item.showsDone
    // Итерация 47: будущий повтор не тусклый.
    val faded = row.past
    val compact = LocalWidgetColors.current === TaskWidgetColors
    val textColor = when {
        compact && item.priority == Priority.HIGH -> if (done) LocalWidgetColors.current.canvasPriorityDone else LocalWidgetColors.current.canvasPriority
        compact && done -> LocalWidgetColors.current.canvasDone
        done -> LocalWidgetColors.current.textDone
        faded -> LocalWidgetColors.current.textSecondary
        else -> LocalWidgetColors.current.textPrimary
    }
    val height = SizeTokens.WIDGET_ROW
    Row(GlanceModifier.fillMaxWidth().height(height.dp).then(if (compact) GlanceModifier.padding(start = SizeTokens.WIDGET_EDGE.dp) else GlanceModifier), verticalAlignment = Alignment.CenterVertically) {
        TimeColumn(row, overdue, textColor)
        Rail(row, line)
        Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(vertical = SizeTokens.WIDGET_CARD_INSET.dp)) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .then(if (LocalWidgetColors.current === TaskWidgetColors) GlanceModifier else GlanceModifier.background(LocalWidgetColors.current.surface))
                    .cornerRadius(RadiusTokens.MD.dp)
                    .then(if (compact) GlanceModifier.padding(end = SpacingTokens.SM.dp) else GlanceModifier.padding(horizontal = SpacingTokens.SM.dp))
                    .then(GlanceModifier.clickable(actionStartActivity(openTaskIntent(context, item.taskId)))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.title,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                item.reminder?.let { ReminderIcon(it, done) }
                Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
                ListSquare(item, compact && done)
            }
        }
    }
}

/** Колонка времени: по правому краю, одним размером; конец периода — серым под началом. */
@Composable
private fun TimeColumn(row: WidgetRow.Task, overdue: Boolean, textColor: ColorProvider) {
    val compact = LocalWidgetColors.current === TaskWidgetColors
    val context = LocalContext.current
    val width = if (compact) (canvasTimeWidthPx(context) / context.resources.displayMetrics.density).dp else maxOf(SizeTokens.WIDGET_TIME.dp, (canvasTimeWidthPx(context) / context.resources.displayMetrics.density).dp)
    val style = TextStyle(
        color = if (overdue && !compact) LocalWidgetColors.current.overdue else textColor,
        fontSize = if (overdue && !compact) 11.sp else 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
    )
    Column(
        modifier = GlanceModifier.width(width).fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.start?.let { Text(it, style = style, maxLines = 1) }
        row.end?.let {
            if (row.isPeriod) Spacer(GlanceModifier.height(SizeTokens.HAIRLINE.dp))
            Text(it, style = style.copy(color = if (compact) textColor else if (overdue) LocalWidgetColors.current.overdue else LocalWidgetColors.current.textSecondary), maxLines = 1)
        }
    }
}

/**
 * Колонка шкалы: линия сверху и снизу, между ними кружок приоритета (в том числе у периода).
 * У повторяющейся тоже кружок — значок повтора только в плашке (итерация 27). Выполненная — кружок или капсула залиты
 * с белой галочкой. Нажатие на колонку отмечает задачу или возвращает выполненную; у будущего повтора — ничего.
 */
@Composable
private fun Rail(row: WidgetRow.Task, line: Line) {
    val compact = LocalWidgetColors.current === TaskWidgetColors
    val nodeSize = if (compact) SizeTokens.WEEKVIEW_CHECK else SizeTokens.WIDGET_NODE
    val context = LocalContext.current
    val item = row.item
    val done = item.showsDone
    // Итерация 27.1 (вариант PM «А»): кружок всегда цвета приоритета, просрочка — красными подписью дня и временем.
    val color = when {
        compact && item.priority == Priority.HIGH -> if (done) LocalWidgetColors.current.canvasPriorityDone else LocalWidgetColors.current.canvasPriority
        compact && done -> LocalWidgetColors.current.canvasDone
        row.past && !done -> LocalWidgetColors.current.inactive
        done -> if (item.priority == Priority.HIGH) LocalWidgetColors.current.checkHigh.done else LocalWidgetColors.current.checkNone.done
        else -> if (item.priority == Priority.HIGH) LocalWidgetColors.current.checkHigh.ring else LocalWidgetColors.current.checkNone.ring
    }
    // Итерация 47: у повторяющейся задачи вместо кружка — знак повтора (у периода тоже круглый).
    // Проверка PM (0.47.0): у выполненной повторяющейся знак остаётся (тусклым), галочки нет.
    val repeat = item.isRecurring
    val action = when {
        item.isGhost -> GlanceModifier
        done -> GlanceModifier.clickable(actionSendBroadcast(WidgetActions.uncomplete(context, item.id)))
        else -> GlanceModifier.clickable(actionSendBroadcast(WidgetActions.complete(context, item.id)))
    }
    val label = if (item.isGhost) null else context.getString(if (done) R.string.task_uncomplete else R.string.widget_complete)
    Column(
        modifier = GlanceModifier.width((if (compact) SizeTokens.WEEKVIEW_CHECK + 2 * SizeTokens.TIMELINE_CHECK_GAP else SizeTokens.WIDGET_RAIL).dp).fillMaxHeight()
            .then(if (compact) GlanceModifier.padding(horizontal = SizeTokens.TIMELINE_CHECK_GAP.dp) else GlanceModifier).then(action),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            LineSegment(line.top, GlanceModifier.defaultWeight())
            Box(GlanceModifier.size(nodeSize.dp), contentAlignment = Alignment.Center) {
                Image(
                    ImageProvider(if (compact) item.canvasNodeIcon(false) else if (repeat) R.drawable.ic_task_repeat_round else if (done) R.drawable.ic_widget_node_fill else R.drawable.ic_widget_node_ring),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(color),
                    modifier = GlanceModifier.size(nodeSize.dp),
                )
                if (done && !repeat) NodeCheck()
            }
            LineSegment(line.bottom, GlanceModifier.defaultWeight())
    }
}

/** Белая галочка в залитом кружке. */
@Composable
private fun NodeCheck() {
    Image(
        ImageProvider(R.drawable.ic_node_check),
        contentDescription = null,
        colorFilter = ColorFilter.tint(LocalWidgetColors.current.onAccent),
        modifier = GlanceModifier.size((if (LocalWidgetColors.current === TaskWidgetColors) SizeTokens.WEEKVIEW_CHECK_ICON else SizeTokens.NODE_CHECK).dp),
    )
}

/** Отрезок линии шкалы ([visible] = false — пустое место той же высоты: у первой и последней строки дня). */
@Composable
private fun LineSegment(visible: Boolean, modifier: GlanceModifier) {
    Spacer(modifier) // #331: без вертикальной линии
}

/**
 * Отметка «сейчас»: зелёное время слева, точка на линии шкалы и линия до правого края. Время — часы системы
 * ([liveClock]), идут сами; в снимках для тестов — неподвижное время из модели.
 */
@Composable
private fun NowRow(line: Line, nowMinute: Int?, liveClock: Boolean) {
    val context = LocalContext.current
    val compact = LocalWidgetColors.current === TaskWidgetColors
    val width = if (compact) (canvasTimeWidthPx(context) / context.resources.displayMetrics.density).dp else SizeTokens.WIDGET_TIME.dp
    Row(GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_NOW_ROW.dp).then(if (compact) GlanceModifier.padding(start = SizeTokens.WIDGET_EDGE.dp) else GlanceModifier), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(width), contentAlignment = Alignment.CenterEnd) {
            if (liveClock) {
                AndroidRemoteViews(RemoteViews(context.packageName, R.layout.widget_now_clock))
            } else if (nowMinute != null) {
                Text(
                    "%02d:%02d".format(nowMinute / MINUTES_PER_HOUR, nowMinute % MINUTES_PER_HOUR),
                    style = TextStyle(color = LocalWidgetColors.current.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        Box(GlanceModifier.width((if (compact) SizeTokens.WEEKVIEW_CHECK + 2 * SizeTokens.WEEK_CONTENT_GAP else SizeTokens.WIDGET_RAIL).dp).fillMaxHeight()
            .then(if (compact) GlanceModifier.padding(horizontal = SizeTokens.WEEK_CONTENT_GAP.dp) else GlanceModifier), contentAlignment = Alignment.Center) {
            Column(GlanceModifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                LineSegment(line.top, GlanceModifier.defaultWeight())
                LineSegment(line.bottom, GlanceModifier.defaultWeight())
            }
            Image(
                ImageProvider(R.drawable.ic_widget_dot),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalWidgetColors.current.accent),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_NOW_DOT.dp),
            )
        }
        Box(GlanceModifier.defaultWeight().height(SizeTokens.HAIRLINE.dp).background(LocalWidgetColors.current.accent)) {}
    }
}

private const val MINUTES_PER_HOUR = 60

/** Значок повтора в плашке; [done] — у выполненной бледнее. */
@Composable
private fun MetaIcon(icon: Int, done: Boolean) {
    Spacer(GlanceModifier.width(SpacingTokens.XXS.dp))
    Image(
        ImageProvider(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(if (done) LocalWidgetColors.current.iconDone else LocalWidgetColors.current.textSecondary),
        modifier = GlanceModifier.size(SizeTokens.WIDGET_META_ICON.dp),
    )
}

/** Колокольчик (итерация 27): без цифр, со стрелкой назад — напоминание заранее (0.27.1); две маски, каждая со своим цветом темы. */
@Composable
private fun ReminderIcon(mark: ReminderMark, done: Boolean) {
    val context = LocalContext.current
    val size = SizeTokens.WIDGET_REMINDER_ICON
    val masks = ReminderIconMasks.of(mark, (size * context.resources.displayMetrics.density).roundToInt())
    Spacer(GlanceModifier.width(SpacingTokens.XXS.dp))
    Box(GlanceModifier.size(size.dp)) {
        MaskImage(masks.bell, if (done) LocalWidgetColors.current.iconDone else LocalWidgetColors.current.textSecondary, size)
        masks.early?.let { MaskImage(it, if (done) LocalWidgetColors.current.accentDone else LocalWidgetColors.current.accent, size) }
    }
}

@Composable
private fun MaskImage(mask: Bitmap, color: ColorProvider, size: Int) {
    Image(ImageProvider(mask), contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = GlanceModifier.size(size.dp))
}

/** Эмодзи списка (итерация 47) вместо квадратика цвета со значком, как в приложении. */
@Composable
private fun ListSquare(item: TaskItem, dim: Boolean = false) {
    val style = TextStyle(fontSize = 15.sp)
    Text(item.listEmoji, style = if (dim) style.copy(color = LocalWidgetColors.current.canvasEmojiDone) else style, maxLines = 1)
}

fun widgetCheckColors(priority: Priority): WidgetCheckColors = when (priority) {
    Priority.HIGH -> WidgetColors.checkHigh
    Priority.NONE -> WidgetColors.checkNone
}

/**
 * Нижняя панель виджета (#336, конструктор PM): тёмная капсула справа — дата «25 ср/ноя» (в «Задачах» — к сегодня), хаб (сетка 2 × 2,
 * окно выбора раздела), настройки (ползунки, приложение на настройках) и фиолетовый «+» раздела. Вкладок в панели больше нет.
 * [imageButtons] — фоны картинками (Android 12+, скругление Glance при первом показе не применялось).
 */
@Composable
internal fun BottomBar(model: WidgetModel, appWidgetId: Int, imageButtons: Boolean, module: WidgetModule = WidgetModule.TASKS) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_BAR.dp).padding(end = SizeTokens.WIDGET_EDGE.dp),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // #338 (конструктор PM): капсула 40 — разделы (четыре точки), настройки, микрофон, светло-фиолетовый «+» без круга (меню «что добавить»).
        Row(
            modifier = (if (imageButtons) GlanceModifier.background(ImageProvider(R.drawable.widget_nav_capsule))
            else GlanceModifier.background(Color(NavBarTokens.background.light)).cornerRadius(SizeTokens.NAV_BUTTON.dp))
                .padding(SizeTokens.NAV_PAD.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // #352 (выбор PM): в «Главной» первой — значок приложения (хаб приложения), вместо настроек — «Обновить».
            val hub = module == WidgetModule.HUB
            if (hub) NavButton(R.drawable.ic_nav_app, context.getString(R.string.widget_open_app),
                actionStartActivity(Intent(context, com.prostovasei.simpletasks.MainActivity::class.java)
                    .putExtra(com.prostovasei.simpletasks.MainActivity.EXTRA_OPEN_HUB, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)))
            NavButton(R.drawable.ic_nav_grid, context.getString(R.string.nav_sections), actionStartActivity(widgetSectionsIntent(context, appWidgetId)))
            // #354 (выбор PM): «Обновить приложение» — стрелка вверх в круге, красная точка при новой версии, нажатие сразу скачивает и ставит.
            if (hub) NavButton(R.drawable.ic_nav_update, context.getString(R.string.widget_update_app),
                actionStartActivity(Intent(context, com.prostovasei.simpletasks.UpdateActivity::class.java)
                    .putExtra(com.prostovasei.simpletasks.UpdateActivity.EXTRA_AUTO, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)),
                badge = UpdateBadge.available(context))
            else NavButton(R.drawable.ic_nav_sliders, context.getString(R.string.settings_title),
                actionStartActivity(Intent(context, com.prostovasei.simpletasks.MainActivity::class.java)
                    .putExtra(com.prostovasei.simpletasks.MainActivity.EXTRA_OPEN_SETTINGS, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)))
            NavButton(R.drawable.ic_nav_mic, context.getString(R.string.widget_voice),
                actionStartActivity(voiceIntent(context, if (module == WidgetModule.TASKS) model.filter else WidgetFilter.All)))
            NavButton(R.drawable.ic_nav_plus, context.getString(R.string.nav_add), actionStartActivity(widgetAddIntent(context, appWidgetId)),
                tint = NavBarTokens.plus, iconSize = SizeTokens.NAV_PLUS_ICON)
        }
    }
}

/** Кнопка капсулы 34 dp без подложки (#338): значок 20, у «+» — 26 светло-фиолетовым. */
@Composable
private fun NavButton(icon: Int, description: String, action: Action,
                      tint: com.prostovasei.simpletasks.designsystem.tokens.ThemedColor = NavBarTokens.icon, iconSize: Int = SizeTokens.NAV_ICON, badge: Boolean = false) {
    Box(
        modifier = GlanceModifier.size(SizeTokens.NAV_BUTTON.dp).clickable(action).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Image(ImageProvider(icon), contentDescription = null, colorFilter = ColorFilter.tint(ColorProvider(Color(tint.light))), modifier = GlanceModifier.size(iconSize.dp))
        if (badge) Box(GlanceModifier.fillMaxSize().padding(SizeTokens.NAV_BADGE_INSET.dp), contentAlignment = Alignment.TopEnd) {
            Image(ImageProvider(R.drawable.widget_nav_badge), contentDescription = null, modifier = GlanceModifier.size(SizeTokens.NAV_BADGE.dp))
        }
    }
}

@Composable
private fun RowScope.WidgetOpenGap(label: String) {
    val context = LocalContext.current
    Box(GlanceModifier.defaultWeight().fillMaxHeight()
        .clickable(actionStartActivity(openAppIntent(context)))
        .semantics { contentDescription = label }) {}
}

/**
 * Фон кнопки нижней строки (0.42.2): на Android 12+ — картинка со скруглением (скругление Glance при первом показе виджета
 * не применялось); в виджете без прокрутки — цвет и скругление Glance: картинка — лишний элемент, а их у Glance не больше ~100.
 */
private fun GlanceModifier.button(image: Int, color: ColorProvider, radius: Int, asImage: Boolean): GlanceModifier =
    if (asImage) background(ImageProvider(image)) else background(color).cornerRadius(radius.dp)

/** Вкладка в белом блоке (итерация 57): только значок — выбранный вид чёрный (`accent`), остальные серые (`inactive`), без подложек. */
@Composable
private fun RowScope.SwitchTab(icon: Int, label: String, selected: Boolean, action: Action) {
    val description = if (selected) LocalContext.current.getString(R.string.widget_tab_selected, label) else label
    val color = if (selected) LocalWidgetColors.current.accent else LocalWidgetColors.current.inactive
    BarIcon(icon, description, color, action)
}

@Composable
private fun RowScope.BarIcon(icon: Int, description: String, color: ColorProvider, action: Action) {
    Box(
        modifier = GlanceModifier.defaultWeight()
            .height(SizeTokens.WIDGET_BAR.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(color),
            modifier = GlanceModifier.size(SizeTokens.WIDGET_TAB_ICON.dp),
        )
    }
}

/** Открыть приложение, как с рабочего стола: уже запущенное — вернуть на экран. */
fun openAppIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
    .setAction(Intent.ACTION_MAIN)
    .addCategory(Intent.CATEGORY_LAUNCHER)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * Открыть задачу поверх рабочего стола, без приложения (решение PM). У каждой задачи свой адрес —
 * иначе Android подменит одно нажатие другим.
 */
fun openTaskIntent(context: Context, taskId: String): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://task/$taskId".toUri())
    .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/**
 * Шторка дня поверх рабочего стола (итерация 51; в итерации 45 открывался список дня в приложении): нажатие на строку даты в ячейке
 * вида «Неделя» в виджете. [date] — «2026-10-30». У каждого дня свой адрес.
 */
fun openDayIntent(context: Context, date: String): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://day/$date".toUri())
    .putExtra(MainActivity.EXTRA_OPEN_DAY, date)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/** «+»: создание задачи поверх рабочего стола — на сегодня («Все»), во «Входящие» или в выбранный список. */
fun quickAddIntent(context: Context, filter: WidgetFilter): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://add/${filter.encode()}".toUri())
    .putExtra(MainActivity.EXTRA_QUICK_ADD, true)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, filter == WidgetFilter.All)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/** #95: микрофон — окно голосового ввода поверх рабочего стола; задача попадает туда же, куда и по «+». У вида свой адрес. */
fun voiceIntent(context: Context, filter: WidgetFilter): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://voice/${filter.encode()}".toUri())
    .putExtra(MainActivity.EXTRA_VOICE, true)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, filter == WidgetFilter.All)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/** «Перенести просроченные на сегодня»: прозрачное окно поверх рабочего стола переносит и показывает «Отмена» (итерация 37). */
fun moveOverdueIntent(context: Context): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://overdue/move".toUri())
    .putExtra(MainActivity.EXTRA_MOVE_OVERDUE, true)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

fun pickerIntent(context: Context, appWidgetId: Int): Intent = Intent(context, WidgetConfigActivity::class.java)
    .setData("simpletasks://widget/$appWidgetId".toUri())
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
