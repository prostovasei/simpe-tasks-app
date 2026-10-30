package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.UpdateActivity
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.ReminderIconMasks
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.widget.theme.WidgetCheckColors
import com.prostovasei.simpletasks.widget.theme.WidgetColors
import java.time.LocalDate
import kotlin.math.roundToInt

/*
 * Виджет «Задачи» (SPEC.md §5; редизайн — итерации 08–09, 14, 20). Лента по дням: подпись дня, под ней — шкала дня:
 * время слева, линия с кружками приоритета (нажатие — «выполнено»), белые плашки с квадратиком списка справа. Внизу — вкладки списков, кнопка приложения,
 * сегодняшняя дата и «+». Рисует `WidgetRenderer` сразу при событии, без фонового сеанса Glance (0.9.2).
 */

/**
 * [scrollable] = false — блоки без прокрутки, [liveClock] = false — неподвижное время отметки «сейчас»:
 * только для снимков в тестах (там прокручиваемый список и системные часы не рисуются).
 */
@Composable
fun WidgetContent(model: WidgetModel, appWidgetId: Int, scrollable: Boolean = true, liveClock: Boolean = true) {
    val context = LocalContext.current
    // Итерация 25: список идёт до самого низа, нижняя панель лежит поверх — сверху прозрачная, книзу цвета фона.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(RadiusTokens.XL.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Список доходит до самого правого края виджета: полоска прокрутки лежит у края, а не на плашках;
        // правое поле плашек — у них самих (DayBlock). Слева поле минимальное: время прижато к краю (итерация 20).
        Box(GlanceModifier.fillMaxSize().padding(start = SizeTokens.WIDGET_LIST_EDGE.dp, top = SizeTokens.WIDGET_EDGE.dp)) {
            if (model.isEmpty) {
                Box(GlanceModifier.fillMaxSize().padding(bottom = SizeTokens.WIDGET_FADE.dp), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.widget_empty), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp))
                }
            } else {
                val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
                val entries = model.entries()
                @Composable
                fun Entry(entry: WidgetEntry) = when (entry) {
                    is WidgetEntry.Overdue -> OverdueTitle(entry, isFirst = entry == entries.first())
                    is WidgetEntry.Day -> DayBlock(entry.block, isFirst = entry == entries.first(), model, days, liveClock)
                }
                // Место под панелью в конце списка: последние задачи можно докрутить выше неё.
                val tail = @Composable { Spacer(GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_FADE.dp)) }
                if (scrollable) {
                    LazyColumn(GlanceModifier.fillMaxSize()) {
                        items(entries, itemId = { it.id }) { Entry(it) }
                        item(itemId = TAIL_ID) { tail() }
                    }
                } else {
                    Column(GlanceModifier.fillMaxSize()) { entries.forEach { Entry(it) } }
                }
            }
        }
        Box(GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_FADE.dp).background(ImageProvider(R.drawable.widget_bar_fade))) {}
        BottomBar(model, appWidgetId)
    }
}

private const val TAIL_ID = Long.MIN_VALUE

private val WidgetEntry.id: Long
    get() = when (this) {
        is WidgetEntry.Overdue -> OVERDUE_ID
        is WidgetEntry.Day -> block.day.id
    }

private const val OVERDUE_ID = -3L

private val WidgetDay.id: Long
    get() = when (this) {
        WidgetDay.NoDate -> -2L
        is WidgetDay.On -> date.toEpochDay()
    }

/**
 * Блок дня — шкала (итерация 20): подпись дня, под ней строки «время | линия с кружком | плашка». Линия сплошная
 * от первой строки дня до последней, щели между плашками — внутри строк.
 */
@Composable
private fun DayBlock(block: WidgetDayBlock, isFirst: Boolean, model: WidgetModel, days: DayNames, liveClock: Boolean) {
    // Прошедший день (итерация 27): красные подпись, время и кружки, раскладка — как у остальных дней.
    val overdue = (block.day as? WidgetDay.On)?.date?.let { it < model.today } == true
    val nowMinute = model.nowMinute.takeIf { block.day == WidgetDay.On(model.today) }
    val rows = WidgetTimeline.rows(block.items, overdue = false, nowMinute = nowMinute, days = days)
    // Правое поле плашек; за ним, у самого края виджета, — полоска прокрутки.
    val progress = DayProgress.of(block.items).takeIf { block.day == WidgetDay.On(model.today) }
    Column(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_LIST_EDGE.dp)) {
        DayTitle(block.day, model.today, isFirst, progress)
        // Одно место в колонке дня занимает подпись.
        ManyRows(rows.withIndex().toList(), limit = MAX_CHILDREN - 1) { (index, row) ->
            val line = Line(top = index > 0, bottom = index < rows.lastIndex)
            when (row) {
                is WidgetRow.Task -> TimelineRow(row, line, overdue, model.filter)
                WidgetRow.Now -> NowRow(line, nowMinute, liveClock)
            }
        }
    }
}

/**
 * «Просрочено · N» (итерация 37): красная подпись над прошедшими днями; нажатие на подпись со стрелкой сворачивает их,
 * значок рядом — переносит все просроченные на сегодня (окно с «Отменой» поверх рабочего стола).
 */
@Composable
private fun OverdueTitle(entry: WidgetEntry.Overdue, isFirst: Boolean) {
    val context = LocalContext.current
    val style = TextStyle(color = WidgetColors.overdue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                colorFilter = ColorFilter.tint(WidgetColors.overdue),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_FOLD_ICON.dp),
            )
        }
        if (entry.count > 0) {
            Spacer(GlanceModifier.width(SpacingTokens.SM.dp))
            Image(
                ImageProvider(R.drawable.ic_move_today),
                contentDescription = context.getString(R.string.overdue_move_today),
                colorFilter = ColorFilter.tint(WidgetColors.overdue),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_FOLD_ICON.dp).clickable(actionStartActivity(moveOverdueIntent(context))),
            )
        }
    }
}

/** Куда от кружка идёт линия шкалы: вверх (есть строка выше) и вниз (есть строка ниже). */
private data class Line(val top: Boolean, val bottom: Boolean)

/**
 * Строки без потерь: в Column виджета помещается не больше [MAX_CHILDREN] элементов, остальные Glance молча
 * отбрасывает. В колонке дня одно место занимает подпись дня, поэтому строкам — [MAX_CHILDREN] − 1;
 * длинный список раскладывается во вложенные колонки (в них — по 10 и меньше).
 */
@Composable
private fun <T> ManyRows(items: List<T>, limit: Int = MAX_CHILDREN, row: @Composable (T) -> Unit) {
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
 * Прошедший день — красным.
 */
@Composable
private fun DayTitle(day: WidgetDay, today: LocalDate, isFirst: Boolean, progress: DayProgress? = null) {
    val title = widgetDayTitle(LocalContext.current, day, today)
    val color = if (title.isPast) WidgetColors.overdue else WidgetColors.textPrimary
    // Итерация 25 (вариант PM «А»): дата группы крупнее — 14 sp, метка «Сегодня» — 12 sp.
    val style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(
            start = SizeTokens.WIDGET_EDGE.dp,
            top = if (isFirst) SizeTokens.WIDGET_DAY_GAP.dp else SizeTokens.WIDGET_EDGE.dp,
            bottom = SizeTokens.WIDGET_DAY_GAP.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.text, style = style, maxLines = 1)
        if (title.relative != null) {
            Spacer(GlanceModifier.width(SpacingTokens.XXS.dp))
            Box(
                GlanceModifier
                    .background(WidgetColors.surface)
                    .cornerRadius(RadiusTokens.SM.dp)
                    .padding(horizontal = SpacingTokens.XXS.dp, vertical = SizeTokens.HAIRLINE.dp),
            ) {
                Text(title.relative, style = style.copy(fontSize = 12.sp), maxLines = 1)
            }
        }
        if (progress != null) DayProgressBar(progress)
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
        color = WidgetColors.accent,
        backgroundColor = WidgetColors.divider,
    )
    Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
    Text(
        context.getString(R.string.widget_progress, progress.done, progress.total),
        style = TextStyle(color = WidgetColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
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
 * список, колокольчик, повтор (итерация 27). Выполненная — зачёркнутая и бледнее, кружок залит с белой галочкой.
 */
@Composable
private fun TimelineRow(row: WidgetRow.Task, line: Line, overdue: Boolean, filter: WidgetFilter) {
    val context = LocalContext.current
    val item = row.item
    val done = item.showsDone
    val faded = row.past || item.isGhost
    val textColor = when {
        done -> WidgetColors.textDone
        faded -> WidgetColors.textSecondary
        else -> WidgetColors.textPrimary
    }
    val height = if (row.isPeriod) SizeTokens.WIDGET_PERIOD_ROW else SizeTokens.WIDGET_ROW
    Row(GlanceModifier.fillMaxWidth().height(height.dp), verticalAlignment = Alignment.CenterVertically) {
        TimeColumn(row, overdue, textColor)
        Rail(row, line)
        Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(vertical = SizeTokens.WIDGET_CARD_INSET.dp)) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.surface)
                    .cornerRadius(RadiusTokens.MD.dp)
                    .padding(horizontal = SpacingTokens.SM.dp)
                    .clickable(actionStartActivity(openTaskIntent(context, item.taskId))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.title,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (item.isRecurring) MetaIcon(R.drawable.ic_tool_repeat, done)
                item.reminder?.let { ReminderIcon(it, done) }
                if (filter !is WidgetFilter.ProjectList) {
                    Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
                    ListSquare(item, done)
                }
            }
        }
    }
}

/** Колонка времени: по правому краю, одним размером; конец периода — серым под началом. */
@Composable
private fun TimeColumn(row: WidgetRow.Task, overdue: Boolean, textColor: ColorProvider) {
    val style = TextStyle(
        color = if (overdue) WidgetColors.overdue else textColor,
        fontSize = if (overdue) 11.sp else 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
    )
    Column(
        modifier = GlanceModifier.width(SizeTokens.WIDGET_TIME.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.start?.let { Text(it, style = style, maxLines = 1) }
        row.end?.let {
            if (row.isPeriod) Spacer(GlanceModifier.height(SizeTokens.HAIRLINE.dp))
            Text(it, style = style.copy(color = if (overdue) WidgetColors.overdue else WidgetColors.textSecondary), maxLines = 1)
        }
    }
}

/**
 * Колонка шкалы: линия сверху и снизу, между ними кружок приоритета (у периода — капсула во всю высоту плашки).
 * У повторяющейся тоже кружок — значок повтора только в плашке (итерация 27). Выполненная — кружок или капсула залиты
 * с белой галочкой. Нажатие на колонку отмечает задачу или возвращает выполненную; у будущего повтора — ничего.
 */
@Composable
private fun Rail(row: WidgetRow.Task, line: Line) {
    val context = LocalContext.current
    val item = row.item
    val done = item.showsDone
    // Итерация 27.1 (вариант PM «А»): кружок всегда цвета приоритета, просрочка — красными подписью дня и временем.
    val color = when {
        (row.past || item.isGhost) && !done -> WidgetColors.inactive
        else -> widgetCheckColors(item.priority).ring
    }
    val action = when {
        item.isGhost -> GlanceModifier
        done -> GlanceModifier.clickable(actionSendBroadcast(WidgetActions.uncomplete(context, item.id)))
        else -> GlanceModifier.clickable(actionSendBroadcast(WidgetActions.complete(context, item.id)))
    }
    val label = if (item.isGhost) null else context.getString(if (done) R.string.task_uncomplete else R.string.widget_complete)
    Column(
        modifier = GlanceModifier.width(SizeTokens.WIDGET_RAIL.dp).fillMaxHeight().then(action),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (row.isPeriod) {
            LineSegment(line.top, GlanceModifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp))
            Box(GlanceModifier.width(SizeTokens.WIDGET_NODE.dp).defaultWeight(), contentAlignment = Alignment.Center) {
                Image(
                    ImageProvider(if (done) R.drawable.widget_period_fill else R.drawable.widget_period_ring),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(color),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxSize(),
                )
                if (done) NodeCheck()
            }
            LineSegment(line.bottom, GlanceModifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp))
        } else {
            LineSegment(line.top, GlanceModifier.defaultWeight())
            Box(GlanceModifier.size(SizeTokens.WIDGET_NODE.dp), contentAlignment = Alignment.Center) {
                Image(
                    ImageProvider(if (done) R.drawable.ic_widget_node_fill else R.drawable.ic_widget_node_ring),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(color),
                    modifier = GlanceModifier.size(SizeTokens.WIDGET_NODE.dp),
                )
                if (done) NodeCheck()
            }
            LineSegment(line.bottom, GlanceModifier.defaultWeight())
        }
    }
}

/** Белая галочка в залитом кружке или капсуле. */
@Composable
private fun NodeCheck() {
    Image(
        ImageProvider(R.drawable.ic_node_check),
        contentDescription = null,
        colorFilter = ColorFilter.tint(WidgetColors.onAccent),
        modifier = GlanceModifier.size(SizeTokens.NODE_CHECK.dp),
    )
}

/** Отрезок линии шкалы ([visible] = false — пустое место той же высоты: у первой и последней строки дня). */
@Composable
private fun LineSegment(visible: Boolean, modifier: GlanceModifier) {
    if (visible) Box(modifier.width(SizeTokens.HAIRLINE.dp).background(WidgetColors.divider)) {} else Spacer(modifier)
}

/**
 * Отметка «сейчас»: зелёное время слева, точка на линии шкалы и линия до правого края. Время — часы системы
 * ([liveClock]), идут сами; в снимках для тестов — неподвижное время из модели.
 */
@Composable
private fun NowRow(line: Line, nowMinute: Int?, liveClock: Boolean) {
    val context = LocalContext.current
    Row(GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_NOW_ROW.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(SizeTokens.WIDGET_TIME.dp), contentAlignment = Alignment.CenterEnd) {
            if (liveClock) {
                AndroidRemoteViews(RemoteViews(context.packageName, R.layout.widget_now_clock))
            } else if (nowMinute != null) {
                Text(
                    "%02d:%02d".format(nowMinute / MINUTES_PER_HOUR, nowMinute % MINUTES_PER_HOUR),
                    style = TextStyle(color = WidgetColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        Box(GlanceModifier.width(SizeTokens.WIDGET_RAIL.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(GlanceModifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                LineSegment(line.top, GlanceModifier.defaultWeight())
                LineSegment(line.bottom, GlanceModifier.defaultWeight())
            }
            Image(
                ImageProvider(R.drawable.ic_widget_dot),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.accent),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_NOW_DOT.dp),
            )
        }
        Box(GlanceModifier.defaultWeight().height(SizeTokens.HAIRLINE.dp).background(WidgetColors.accent)) {}
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
        colorFilter = ColorFilter.tint(if (done) WidgetColors.iconDone else WidgetColors.textSecondary),
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
        MaskImage(masks.bell, if (done) WidgetColors.iconDone else WidgetColors.textSecondary, size)
        masks.early?.let { MaskImage(it, if (done) WidgetColors.accentDone else WidgetColors.accent, size) }
    }
}

@Composable
private fun MaskImage(mask: Bitmap, color: ColorProvider, size: Int) {
    Image(ImageProvider(mask), contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = GlanceModifier.size(size.dp))
}

/** Квадратик цвета списка со значком (как в приложении); [done] — у выполненной бледнее (70%). */
@Composable
private fun ListSquare(item: TaskItem, done: Boolean) {
    val color = when {
        item.isInboxList -> if (done) WidgetColors.inboxListDone else WidgetColors.inboxList
        else -> parseListColor(item.listColor)?.let { ColorProvider(if (done) it.copy(alpha = DONE_ALPHA) else it) }
            ?: if (done) WidgetColors.inactiveDone else WidgetColors.inactive
    }
    Box(
        modifier = GlanceModifier.size(SizeTokens.WIDGET_LIST_SQUARE.dp).background(color).cornerRadius(RadiusTokens.SM.dp),
        contentAlignment = Alignment.Center,
    ) {
        ListIcons.drawable(item.listIcon)?.let { icon ->
            Image(
                ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.onListSquare),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_LIST_GLYPH.dp),
            )
        }
    }
}

fun widgetCheckColors(priority: Priority): WidgetCheckColors = when (priority) {
    Priority.HIGH -> WidgetColors.checkHigh
    Priority.MEDIUM -> WidgetColors.checkMedium
    Priority.LOW -> WidgetColors.checkLow
    Priority.NONE -> WidgetColors.checkNone
}

/**
 * Нижняя строка (итерация 25, вариант PM «Г»): слева белая капсула-переключатель «Все» / «Входящие» (выбранный
 * с итерации 26 — светло-зелёный с зелёной иконкой), справа «+»; всё место между ними — открыть приложение. Дата и «⋯» убраны.
 */
@Composable
private fun BottomBar(model: WidgetModel, appWidgetId: Int) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_BAR.dp).padding(horizontal = SizeTokens.WIDGET_EDGE.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.background(WidgetColors.surface).cornerRadius(RadiusTokens.LG.dp).padding(SizeTokens.WIDGET_SWITCH_PAD.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(WidgetFilter.All, WidgetFilter.Inbox).forEach { filter ->
                SwitchTab(
                    icon = if (filter == WidgetFilter.All) R.drawable.ic_widget_all else R.drawable.ic_widget_inbox,
                    label = context.getString(if (filter == WidgetFilter.Inbox) R.string.inbox_list_name else R.string.widget_all),
                    selected = filter == model.filter,
                    action = actionSendBroadcast(WidgetActions.switch(context, appWidgetId, filter)),
                )
            }
        }
        val openApp = context.getString(R.string.widget_open_app)
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .clickable(actionStartActivity(openAppIntent(context)))
                .semantics { contentDescription = openApp },
        ) {}
        // Итерация 32: кнопка обновления слева от «+» — открывает окно проверки и загрузки новой версии поверх рабочего стола.
        val update = context.getString(R.string.widget_update)
        Box(
            modifier = GlanceModifier
                .size(SizeTokens.WIDGET_UPDATE.dp)
                .background(WidgetColors.surface)
                .cornerRadius(RadiusTokens.MD.dp)
                .clickable(actionStartActivity(updateIntent(context))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_widget_update),
                contentDescription = update,
                colorFilter = ColorFilter.tint(WidgetColors.textSecondary),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_UPDATE_ICON.dp),
            )
        }
        Spacer(GlanceModifier.width(SizeTokens.WIDGET_UPDATE_GAP.dp))
        Box(
            modifier = GlanceModifier
                .size(SizeTokens.WIDGET_ADD.dp)
                .background(WidgetColors.accent)
                .cornerRadius(RadiusTokens.MD.dp)
                .clickable(actionStartActivity(quickAddIntent(context, model.filter))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_widget_add),
                contentDescription = context.getString(R.string.add_task),
                colorFilter = ColorFilter.tint(WidgetColors.onAccent),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_ADD_ICON.dp),
            )
        }
    }
}

/** Кнопка переключателя: выбранная залита зелёным с белой иконкой, другая — серая иконка на белом. */
@Composable
private fun SwitchTab(icon: Int, label: String, selected: Boolean, action: Action) {
    Box(
        modifier = GlanceModifier
            .width(SizeTokens.WIDGET_SWITCH_W.dp)
            .height(SizeTokens.WIDGET_SWITCH_H.dp)
            .background(if (selected) WidgetColors.selected else WidgetColors.surface)
            .cornerRadius(RadiusTokens.MD.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(if (selected) WidgetColors.accent else WidgetColors.textSecondary),
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

/** «+»: создание задачи поверх рабочего стола — на сегодня («Все»), во «Входящие» или в выбранный список. */
fun quickAddIntent(context: Context, filter: WidgetFilter): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://add/${filter.encode()}".toUri())
    .putExtra(MainActivity.EXTRA_QUICK_ADD, true)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, filter == WidgetFilter.All)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_PROJECT, (filter as? WidgetFilter.ProjectList)?.projectId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/** «Перенести просроченные на сегодня»: прозрачное окно поверх рабочего стола переносит и показывает «Отмена» (итерация 37). */
fun moveOverdueIntent(context: Context): Intent = Intent(context, TaskPopupActivity::class.java)
    .setData("simpletasks://overdue/move".toUri())
    .putExtra(MainActivity.EXTRA_MOVE_OVERDUE, true)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

/** Окно обновления поверх рабочего стола (кнопка в нижней строке виджета). */
fun updateIntent(context: Context): Intent = Intent(context, UpdateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun pickerIntent(context: Context, appWidgetId: Int): Intent = Intent(context, WidgetConfigActivity::class.java)
    .setData("simpletasks://widget/$appWidgetId".toUri())
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
