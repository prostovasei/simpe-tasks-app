package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.widget.theme.WidgetCheckColors
import com.prostovasei.simpletasks.widget.theme.WidgetColors
import java.time.LocalDate

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
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(RadiusTokens.XL.dp),
    ) {
        // Список доходит до самого правого края виджета: полоска прокрутки лежит у края, а не на плашках;
        // правое поле плашек — у них самих (DayBlock). Слева поле минимальное: время прижато к краю (итерация 20).
        Box(GlanceModifier.fillMaxWidth().defaultWeight().padding(start = SizeTokens.WIDGET_LIST_EDGE.dp, top = SizeTokens.WIDGET_EDGE.dp)) {
            if (model.isEmpty) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.widget_empty), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp))
                }
            } else {
                val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
                val blocks = model.days.filter { it.items.isNotEmpty() }
                @Composable
                fun Block(block: WidgetDayBlock) = DayBlock(block, isFirst = block == blocks.first(), model, days, liveClock)
                if (scrollable) {
                    LazyColumn(GlanceModifier.fillMaxSize()) { items(blocks, itemId = { it.day.id }) { Block(it) } }
                } else {
                    Column(GlanceModifier.fillMaxSize()) { blocks.forEach { Block(it) } }
                }
            }
        }
        BottomBar(model, appWidgetId)
    }
}

private val WidgetDay.id: Long
    get() = when (this) {
        WidgetDay.Overdue -> -1L
        WidgetDay.NoDate -> -2L
        is WidgetDay.On -> date.toEpochDay()
    }

/**
 * Блок дня — шкала (итерация 20): подпись дня, под ней строки «время | линия с кружком | плашка». Линия сплошная
 * от первой строки дня до последней, щели между плашками — внутри строк.
 */
@Composable
private fun DayBlock(block: WidgetDayBlock, isFirst: Boolean, model: WidgetModel, days: DayNames, liveClock: Boolean) {
    val overdue = block.day == WidgetDay.Overdue
    val nowMinute = model.nowMinute.takeIf { block.day == WidgetDay.On(model.today) }
    val rows = WidgetTimeline.rows(block.items, overdue, nowMinute, days)
    // Правое поле плашек; за ним, у самого края виджета, — полоска прокрутки.
    Column(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_LIST_EDGE.dp)) {
        DayTitle(block.day, model.today, isFirst)
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

/** «Просрочено», «ПТ, 16 окт» + белая метка «Сегодня» / «Завтра» (итерация 14, как в приложении), «Без даты». */
@Composable
private fun DayTitle(day: WidgetDay, today: LocalDate, isFirst: Boolean) {
    val title = widgetDayTitle(LocalContext.current, day, today)
    val color = if (day == WidgetDay.Overdue) WidgetColors.overdue else WidgetColors.textPrimary
    val style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(title.relative, style = style.copy(fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}

/** Подпись дня: [text] — «ПТ, 16 ОКТ»; [relative] — «СЕГОДНЯ» / «ЗАВТРА» или null. */
data class WidgetDayTitle(val text: String, val relative: String?, val isToday: Boolean)

fun widgetDayTitle(context: Context, day: WidgetDay, today: LocalDate): WidgetDayTitle = when (day) {
    WidgetDay.Overdue -> WidgetDayTitle(context.getString(R.string.all_overdue), null, false)
    WidgetDay.NoDate -> WidgetDayTitle(context.getString(R.string.date_none), null, false)
    is WidgetDay.On -> WidgetDayTitle(
        text = WidgetDates.header(day.date),
        relative = when (day.date) {
            today -> context.getString(R.string.day_today)
            today.plusDays(1) -> context.getString(R.string.day_tomorrow)
            else -> null
        },
        isToday = day.date == today,
    )
}

/**
 * Строка шкалы: слева время (у периода — начало и конец, у задачи на весь день — пусто, у просроченной — день),
 * по центру линия с кружком приоритета (нажатие — «выполнено»), справа плашка: название, будильник, повтор, список.
 */
@Composable
private fun TimelineRow(row: WidgetRow.Task, line: Line, overdue: Boolean, filter: WidgetFilter) {
    val context = LocalContext.current
    val item = row.item
    val faded = row.past || item.isGhost
    val textColor = if (faded) WidgetColors.textSecondary else WidgetColors.textPrimary
    val height = if (row.isPeriod) SizeTokens.WIDGET_PERIOD_ROW else SizeTokens.WIDGET_ROW
    Row(GlanceModifier.fillMaxWidth().height(height.dp), verticalAlignment = Alignment.CenterVertically) {
        TimeColumn(row, overdue, textColor)
        Rail(row, line, overdue)
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
                    style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (item.hasReminders) MetaIcon(R.drawable.ic_tool_reminder)
                if (item.isRecurring) MetaIcon(R.drawable.ic_tool_repeat)
                if (filter !is WidgetFilter.ProjectList) {
                    Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
                    ListSquare(item)
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
 * Колонка шкалы: линия сверху и снизу, между ними кружок приоритета (у повторяющейся — значок повтора,
 * у периода — вытянутое кольцо). Нажатие на колонку отмечает задачу выполненной; у будущего повтора — нет.
 */
@Composable
private fun Rail(row: WidgetRow.Task, line: Line, overdue: Boolean) {
    val context = LocalContext.current
    val item = row.item
    val color = when {
        overdue -> WidgetColors.overdue
        row.past || item.isGhost -> WidgetColors.inactive
        else -> widgetCheckColors(item.priority).ring
    }
    val action = if (item.isGhost) GlanceModifier else GlanceModifier.clickable(actionSendBroadcast(WidgetActions.complete(context, item.id)))
    val label = if (item.isGhost) null else context.getString(R.string.widget_complete)
    Column(
        modifier = GlanceModifier.width(SizeTokens.WIDGET_RAIL.dp).fillMaxHeight().then(action),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (row.isPeriod) {
            LineSegment(line.top, GlanceModifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp))
            Image(
                ImageProvider(R.drawable.widget_period_ring),
                contentDescription = label,
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.width(SizeTokens.WIDGET_NODE.dp).defaultWeight(),
            )
            LineSegment(line.bottom, GlanceModifier.height(SizeTokens.WIDGET_PERIOD_STUB.dp))
        } else {
            LineSegment(line.top, GlanceModifier.defaultWeight())
            val repeat = item.isRecurring
            Image(
                ImageProvider(if (repeat) R.drawable.ic_tool_repeat else R.drawable.ic_widget_node_ring),
                contentDescription = label,
                colorFilter = ColorFilter.tint(if (repeat) WidgetColors.textSecondary else color),
                modifier = GlanceModifier.size((if (repeat) SizeTokens.WIDGET_META_ICON else SizeTokens.WIDGET_NODE).dp),
            )
            LineSegment(line.bottom, GlanceModifier.defaultWeight())
        }
    }
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

/** Значок в плашке справа от названия: будильник или повтор. */
@Composable
private fun MetaIcon(icon: Int) {
    Spacer(GlanceModifier.width(SpacingTokens.XXS.dp))
    Image(
        ImageProvider(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(WidgetColors.textSecondary),
        modifier = GlanceModifier.size(SizeTokens.WIDGET_META_ICON.dp),
    )
}

/** Квадратик цвета списка со значком (как в приложении). */
@Composable
private fun ListSquare(item: TaskItem) {
    val color = if (item.isInboxList) WidgetColors.inboxList else parseListColor(item.listColor)?.let { ColorProvider(it) } ?: WidgetColors.inactive
    Box(
        modifier = GlanceModifier.size(SizeTokens.LIST_SQUARE.dp).background(color).cornerRadius(RadiusTokens.SM.dp),
        contentAlignment = Alignment.Center,
    ) {
        ListIcons.drawable(item.listIcon)?.let { icon ->
            Image(
                ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.onListSquare),
                modifier = GlanceModifier.size(SizeTokens.LIST_GLYPH.dp),
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
 * Нижняя строка (решение PM, итерация 09): слева вкладки списков — нажатие сразу переключает виджет, «⋯» открывает
 * окошко со всеми списками; в центре — открыть приложение; справа «пт, 16 окт» и «+».
 */
@Composable
private fun BottomBar(model: WidgetModel, appWidgetId: Int) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_BAR.dp).padding(horizontal = SizeTokens.WIDGET_EDGE.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Вкладки — отдельной группой: в Row виджета не больше 10 элементов (иначе «+» пропадает).
        Row(verticalAlignment = Alignment.CenterVertically) {
            model.tabs.forEach { tab ->
                ListTab(
                    icon = when (tab.filter) {
                        WidgetFilter.All -> R.drawable.ic_widget_all
                        WidgetFilter.Inbox -> R.drawable.ic_widget_inbox
                        is WidgetFilter.ProjectList -> R.drawable.ic_widget_list
                    },
                    label = tab.name ?: context.getString(if (tab.filter == WidgetFilter.Inbox) R.string.inbox_list_name else R.string.widget_all),
                    tint = tab.color?.let { parseListColor(it) }?.let { ColorProvider(it) },
                    selected = tab.filter == model.filter,
                    action = actionSendBroadcast(WidgetActions.switch(context, appWidgetId, tab.filter)),
                )
            }
            ListTab(
                icon = R.drawable.ic_widget_more,
                label = context.getString(R.string.widget_pick_list),
                tint = null,
                selected = false,
                action = actionStartActivity(pickerIntent(context, appWidgetId)),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier
                .size(SizeTokens.WIDGET_ADD.dp)
                .background(WidgetColors.surface)
                .cornerRadius(RadiusTokens.MD.dp)
                .clickable(actionStartActivity(openAppIntent(context))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_widget_app),
                contentDescription = context.getString(R.string.widget_open_app),
                colorFilter = ColorFilter.tint(WidgetColors.accent),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_ADD_ICON.dp),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(WidgetDates.withWeekday(model.today), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 13.sp), maxLines = 1)
        Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
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

/** Вкладка: иконка (у своего списка — его цветом), у выбранной — синяя с точкой снизу. */
@Composable
private fun ListTab(icon: Int, label: String, tint: ColorProvider?, selected: Boolean, action: Action) {
    Column(
        modifier = GlanceModifier.width(SizeTokens.WIDGET_TAB.dp).height(SizeTokens.WIDGET_BAR.dp).clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            ImageProvider(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(if (selected) WidgetColors.accent else tint ?: WidgetColors.textSecondary),
            modifier = GlanceModifier.size(SizeTokens.WIDGET_TAB_ICON.dp),
        )
        Spacer(GlanceModifier.height(SizeTokens.WIDGET_TAB_DOT.dp))
        if (selected) {
            Image(
                ImageProvider(R.drawable.ic_widget_dot),
                contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.accent),
                modifier = GlanceModifier.size(SizeTokens.WIDGET_TAB_DOT.dp),
            )
        } else {
            Spacer(GlanceModifier.size(SizeTokens.WIDGET_TAB_DOT.dp))
        }
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

fun pickerIntent(context: Context, appWidgetId: Int): Intent = Intent(context, WidgetConfigActivity::class.java)
    .setData("simpletasks://widget/$appWidgetId".toUri())
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
