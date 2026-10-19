package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
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
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.ListIcons
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.widget.theme.WidgetCheckColors
import com.prostovasei.simpletasks.widget.theme.WidgetColors
import java.time.LocalDate

/*
 * Виджет «Задачи» (SPEC.md §5; редизайн — итерации 08–09, 14). Лента по дням: подпись дня, под ней — белые
 * плашки задач с квадратной галочкой цвета приоритета и квадратиком списка. Внизу — вкладки списков, кнопка приложения,
 * сегодняшняя дата и «+». Рисует `WidgetRenderer` сразу при событии, без фонового сеанса Glance (0.9.2).
 */

/** [scrollable] = false — блоки без прокрутки: только для снимков в тестах (там прокручиваемый список не рисуется). */
@Composable
fun WidgetContent(model: WidgetModel, appWidgetId: Int, scrollable: Boolean = true) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(RadiusTokens.XL.dp),
    ) {
        Box(GlanceModifier.fillMaxWidth().defaultWeight().padding(start = SizeTokens.WIDGET_EDGE.dp, end = SizeTokens.WIDGET_EDGE.dp, top = SizeTokens.WIDGET_EDGE.dp)) {
            if (model.isEmpty) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.widget_empty), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp))
                }
            } else {
                val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
                val blocks = model.days.filter { it.items.isNotEmpty() }
                @Composable
                fun Block(block: WidgetDayBlock) = DayBlock(block, isFirst = block == blocks.first(), model, days)
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

/** Блок дня (итерация 14, как в приложении): подпись дня, под ней — отдельные белые плашки задач через щель 2 dp. */
@Composable
private fun DayBlock(block: WidgetDayBlock, isFirst: Boolean, model: WidgetModel, days: DayNames) {
    // Справа — свободная полоска для линии прокрутки (она рисуется системой по краю списка).
    Column(GlanceModifier.fillMaxWidth().padding(end = SizeTokens.WIDGET_SCROLL_GUTTER.dp)) {
        DayTitle(block.day, model.today, isFirst)
        // Одно место в колонке дня занимает подпись.
        ManyRows(block.items.withIndex().toList(), limit = MAX_CHILDREN - 1) { (index, item) ->
            Column(GlanceModifier.fillMaxWidth()) {
                if (index > 0) Spacer(GlanceModifier.height(SizeTokens.CARD_GAP.dp))
                TaskRow(item, overdue = block.day == WidgetDay.Overdue, model.filter, days)
            }
        }
    }
}

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
 * Плашка задачи (итерация 14, как в приложении): квадратная галочка (рамка — приоритет), квадратик списка со
 * значком, название, ↻; справа время (интервал — столбиком), у просроченных — день.
 */
@Composable
private fun TaskRow(item: TaskItem, overdue: Boolean, filter: WidgetFilter, days: DayNames) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.surface)
            .cornerRadius(RadiusTokens.MD.dp)
            .padding(end = SpacingTokens.SM.dp, top = SizeTokens.WIDGET_ROW_GAP.dp, bottom = SizeTokens.WIDGET_ROW_GAP.dp)
            .clickable(actionStartActivity(openTaskIntent(context, item.taskId))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isGhost) {
            // У будущего повтора галочки нет: выполнить можно только ближайший раз.
            Spacer(GlanceModifier.width(SizeTokens.WIDGET_CHECKBOX.dp))
        } else {
            PriorityCheck(item, overdue)
        }
        if (filter !is WidgetFilter.ProjectList) {
            ListSquare(item)
            Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
        }
        val titleColor = if (item.isGhost) WidgetColors.textSecondary else WidgetColors.textPrimary
        Text(
            item.title + if (item.isRecurring) " ↻" else "",
            style = TextStyle(color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        val lines = widgetTrailing(item, days, showDay = overdue)
        if (lines.isNotEmpty()) {
            Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
            val timeStyle = TextStyle(color = if (overdue) WidgetColors.overdue else titleColor, fontSize = if (lines.size == 1) 13.sp else 11.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                lines.forEachIndexed { index, line ->
                    if (index > 0) Text("·", style = timeStyle.copy(color = WidgetColors.textSecondary, fontSize = 9.sp), maxLines = 1)
                    Text(line, style = timeStyle, maxLines = 1)
                }
            }
        }
    }
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

/** Справа на плашке: время («10:00» или «10:00», «11:00» столбиком); у просроченных — день и время начала. */
fun widgetTrailing(item: TaskItem, days: DayNames, showDay: Boolean): List<String> {
    val due = item.due ?: return emptyList()
    val times = due.time?.split("–").orEmpty()
    if (!showDay) return times
    val day = when (val label = due.day) {
        is DayLabel.Date -> WidgetDates.short(label.date)
        else -> days.day(label)
    }
    return listOfNotNull(day, times.firstOrNull())
}

/** Квадратная галочка: рамка — приоритет (P1 красный, P2 жёлтый, P3 синий, без приоритета серый), у просроченной — красная. */
@Composable
private fun PriorityCheck(item: TaskItem, overdue: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(SizeTokens.WIDGET_CHECKBOX.dp)
            .clickable(actionSendBroadcast(WidgetActions.complete(context, item.id))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(R.drawable.ic_widget_check_ring),
            contentDescription = context.getString(R.string.widget_complete),
            colorFilter = ColorFilter.tint(if (overdue) WidgetColors.overdue else widgetCheckColors(item.priority).ring),
            modifier = GlanceModifier.size(SizeTokens.CHECKBOX.dp),
        )
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
