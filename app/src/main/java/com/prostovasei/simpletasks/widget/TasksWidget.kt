package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.widget.theme.WidgetCheckColors
import com.prostovasei.simpletasks.widget.theme.WidgetColors
import java.time.LocalDate

/**
 * Виджет «Задачи» (SPEC.md §5; редизайн — итерация 08). Лента по дням: подпись дня над светлой плашкой,
 * в плашке задачи с круглой галочкой цвета приоритета. Внизу — выбор списка («Все» / «Входящие» / список),
 * сегодняшняя дата и «+». Галочка выполняет задачу сразу; нажатие на задачу открывает её поверх рабочего стола.
 */
class TasksWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = context.widgetEntryPoint()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val filter = WidgetFilter.decode(currentState(FILTER_KEY))
            val model by remember(filter) { entry.observeModel(filter) }.collectAsState(initial = null)
            WidgetContent(model ?: WidgetModel(filter, null, entry.time().today(), emptyList()), appWidgetId)
        }
    }

    companion object {
        val FILTER_KEY = stringPreferencesKey("filter")
    }
}

/** Галочка в виджете: задача выполняется (повторяющаяся — переезжает), все виджеты обновляются. */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID] ?: return
        context.widgetEntryPoint().tasks().complete(taskId)
        TasksWidget().updateAll(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
    }
}

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

/** Блок дня: подпись над плашкой, почти вплотную; задачи внутри — без своих плашек (решение PM). */
@Composable
private fun DayBlock(block: WidgetDayBlock, isFirst: Boolean, model: WidgetModel, days: DayNames) {
    Column(GlanceModifier.fillMaxWidth()) {
        DayTitle(block.day, model.today, isFirst)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetColors.dayBlock)
                .cornerRadius(RadiusTokens.LG.dp)
                .padding(horizontal = SpacingTokens.XXS.dp, vertical = SizeTokens.WIDGET_DAY_GAP.dp),
        ) {
            block.items.forEach { TaskRow(it, overdue = block.day == WidgetDay.Overdue, model.filter, days) }
        }
    }
}

/** «ПРОСРОЧЕНО», «СЕГОДНЯ · 16 окт», «ПОНЕДЕЛЬНИК · 19 окт», «БЕЗ ДАТЫ». */
@Composable
private fun DayTitle(day: WidgetDay, today: LocalDate, isFirst: Boolean) {
    val (label, date) = widgetDayTitle(LocalContext.current, day, today)
    val color = if (day == WidgetDay.Overdue) WidgetColors.overdue else WidgetColors.textSecondary
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(
            start = SizeTokens.WIDGET_EDGE.dp,
            top = if (isFirst) SizeTokens.WIDGET_DAY_GAP.dp else SizeTokens.WIDGET_EDGE.dp,
            bottom = SizeTokens.WIDGET_DAY_GAP.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        if (date != null) Text(" · $date", style = TextStyle(color = color, fontSize = 11.sp), maxLines = 1)
    }
}

/** Подпись дня и короткая дата к ней (у «Просрочено» и «Без даты» даты нет). */
fun widgetDayTitle(context: Context, day: WidgetDay, today: LocalDate): Pair<String, String?> = when (day) {
    WidgetDay.Overdue -> context.getString(R.string.all_overdue) to null
    WidgetDay.NoDate -> context.getString(R.string.date_none) to null
    is WidgetDay.On -> when (day.date) {
        today -> context.getString(R.string.day_today)
        today.plusDays(1) -> context.getString(R.string.day_tomorrow)
        else -> WidgetDates.weekday(day.date)
    } to WidgetDates.short(day.date)
}

@Composable
private fun TaskRow(item: TaskItem, overdue: Boolean, filter: WidgetFilter, days: DayNames) {
    val context = LocalContext.current
    val secondary = if (item.isGhost) WidgetColors.divider else WidgetColors.textSecondary
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = SizeTokens.WIDGET_ROW_GAP.dp)
            .clickable(actionStartActivity(openTaskIntent(context, item.taskId))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isGhost) {
            // У будущего повтора галочки нет: выполнить можно только ближайший раз.
            Spacer(GlanceModifier.width(SizeTokens.WIDGET_CHECKBOX.dp))
        } else {
            PriorityCheck(item)
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(
                item.title,
                style = TextStyle(color = if (item.isGhost) WidgetColors.textSecondary else WidgetColors.textPrimary, fontSize = 14.sp),
                maxLines = 2,
            )
            // День уже в подписи блока; у просроченных он нужен — «Вчера», «12 октября».
            val meta = widgetMeta(item, days, context.getString(R.string.inbox_list_name), showList = filter !is WidgetFilter.ProjectList, showDay = overdue)
            if (meta.isNotEmpty() || item.isRecurring) {
                Text(
                    listOf(meta, if (item.isRecurring) "↻" else "").filter { it.isNotEmpty() }.joinToString(" "),
                    style = TextStyle(color = if (overdue) WidgetColors.overdue else secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Круглая галочка: цвет — приоритет (P1 красный, P2 жёлтый, P3 синий, без приоритета серый). Нажатие выполняет задачу. */
@Composable
private fun PriorityCheck(item: TaskItem) {
    val context = LocalContext.current
    val colors = widgetCheckColors(item.priority)
    Box(
        modifier = GlanceModifier
            .size(SizeTokens.WIDGET_CHECKBOX.dp)
            .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(CompleteTaskAction.TASK_ID to item.id))),
        contentAlignment = Alignment.Center,
    ) {
        if (item.priority != Priority.NONE) {
            Image(
                ImageProvider(R.drawable.ic_widget_check_fill),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.fill),
                modifier = GlanceModifier.size(SizeTokens.CHECKBOX.dp),
            )
        }
        Image(
            ImageProvider(R.drawable.ic_widget_check_ring),
            contentDescription = context.getString(R.string.widget_complete),
            colorFilter = ColorFilter.tint(colors.ring),
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

/** Нижняя строка цвета фона, без линии: слева выбор списка, справа сегодняшняя дата и «+» (решение PM). */
@Composable
private fun BottomBar(model: WidgetModel, appWidgetId: Int) {
    val context = LocalContext.current
    val title = model.title ?: context.getString(if (model.filter == WidgetFilter.Inbox) R.string.inbox_list_name else R.string.widget_all)
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(SizeTokens.WIDGET_BAR.dp).padding(horizontal = SizeTokens.WIDGET_EDGE.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier
                .background(WidgetColors.surface)
                .cornerRadius(RadiusTokens.XL.dp)
                .padding(start = SpacingTokens.SM.dp, end = SpacingTokens.XS.dp, top = SpacingTokens.XXS.dp, bottom = SpacingTokens.XXS.dp)
                .clickable(actionStartActivity(pickerIntent(context, appWidgetId))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = TextStyle(color = WidgetColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Image(
                ImageProvider(R.drawable.ic_widget_expand),
                contentDescription = context.getString(R.string.widget_pick_list),
                colorFilter = ColorFilter.tint(WidgetColors.textSecondary),
                modifier = GlanceModifier.size(SizeTokens.CHECKBOX.dp),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(WidgetDates.short(model.today), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp), maxLines = 1)
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

/**
 * «10:00 · Работа»: время и список (в виджете одного списка — без него). День ([showDay]) — только у просроченных:
 * у остальных он в подписи блока.
 */
fun widgetMeta(item: TaskItem, days: DayNames, inboxName: String, showList: Boolean, showDay: Boolean): String {
    val due = item.due?.let { if (showDay) days.format(it) else it.time }
    val list = if (!showList) null else if (item.isInboxList) inboxName else item.listName
    return listOfNotNull(due, list).joinToString(" · ")
}

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
