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
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.widget.theme.WidgetColors

/**
 * Виджет «Задачи» (SPEC.md §5, Итерация 06). Шапка: список (нажатие — выбор «Все» / «Входящие» / список),
 * число открытых задач, «+». Ниже — задачи с прокруткой; галочка выполняет задачу сразу, без запуска
 * приложения; нажатие на задачу открывает её в приложении.
 */
class TasksWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = context.widgetEntryPoint()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val filter = WidgetFilter.decode(currentState(FILTER_KEY))
            val model by remember(filter) { entry.observeModel(filter) }.collectAsState(initial = null)
            WidgetContent(model ?: WidgetModel(filter, null, emptyList()), appWidgetId)
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

/** [scrollable] = false — строки без прокрутки: только для снимков в тестах (там прокручиваемый список не рисуется). */
@Composable
fun WidgetContent(model: WidgetModel, appWidgetId: Int, scrollable: Boolean = true) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(RadiusTokens.XL.dp)
            .padding(SpacingTokens.SM.dp),
    ) {
        Header(model, appWidgetId)
        if (model.isEmpty) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp))
            }
        } else {
            val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
            val rows = model.sections.flatMap { section -> listOfNotNull(section.kind?.let { WidgetRow.Title(it) }) + section.items.map { WidgetRow.Task(it, section.kind) } }
            @Composable
            fun Row(row: WidgetRow) = when (row) {
                is WidgetRow.Title -> SectionTitle(row.kind)
                is WidgetRow.Task -> TaskRow(row.item, row.section, model.filter, days)
            }
            if (scrollable) {
                LazyColumn(GlanceModifier.fillMaxSize()) { items(rows, itemId = { it.id }) { Row(it) } }
            } else {
                Column(GlanceModifier.fillMaxSize()) { rows.forEach { Row(it) } }
            }
        }
    }
}

private sealed interface WidgetRow {
    val id: Long

    data class Title(val kind: WidgetSectionKind) : WidgetRow {
        override val id: Long get() = -1L - kind.ordinal
    }

    data class Task(val item: TaskItem, val section: WidgetSectionKind?) : WidgetRow {
        override val id: Long get() = item.id.hashCode().toLong() and 0xFFFFFFFFL
    }
}

@Composable
private fun Header(model: WidgetModel, appWidgetId: Int) {
    val context = LocalContext.current
    val title = model.title ?: context.getString(if (model.filter == WidgetFilter.Inbox) R.string.inbox_list_name else R.string.widget_all)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = SpacingTokens.XS.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(pickerIntent(context, appWidgetId))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = TextStyle(color = WidgetColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Image(
                ImageProvider(R.drawable.ic_widget_expand),
                contentDescription = context.getString(R.string.widget_pick_list),
                colorFilter = ColorFilter.tint(WidgetColors.textSecondary),
                modifier = GlanceModifier.size(SizeTokens.ICON.dp),
            )
            Spacer(GlanceModifier.width(SpacingTokens.XS.dp))
            Text("${model.openCount}", style = TextStyle(color = WidgetColors.textSecondary, fontSize = 14.sp))
        }
        Box(
            modifier = GlanceModifier
                .size(SizeTokens.SWATCH.dp)
                .background(WidgetColors.accent)
                .cornerRadius(RadiusTokens.XL.dp)
                .clickable(actionStartActivity(quickAddIntent(context, model.filter))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_widget_add),
                contentDescription = context.getString(R.string.add_task),
                colorFilter = ColorFilter.tint(WidgetColors.onAccent),
                modifier = GlanceModifier.size(SizeTokens.ICON.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(kind: WidgetSectionKind) {
    val context = LocalContext.current
    val text = context.getString(
        when (kind) {
            WidgetSectionKind.OVERDUE -> R.string.all_overdue
            WidgetSectionKind.TODAY -> R.string.all_today
            WidgetSectionKind.UPCOMING -> R.string.all_upcoming
        },
    ).uppercase()
    Text(
        text,
        style = TextStyle(
            color = if (kind == WidgetSectionKind.OVERDUE) WidgetColors.overdue else WidgetColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = GlanceModifier.padding(top = SpacingTokens.XS.dp, bottom = SpacingTokens.XXS.dp),
    )
}

@Composable
private fun TaskRow(item: TaskItem, section: WidgetSectionKind?, filter: WidgetFilter, days: DayNames) {
    val context = LocalContext.current
    val secondary = if (item.isGhost) WidgetColors.divider else WidgetColors.textSecondary
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.XXS.dp)
            .clickable(actionStartActivity(openTaskIntent(context, item.taskId))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isGhost) {
            // У будущего повтора галочки нет: выполнить можно только ближайший раз.
            Spacer(GlanceModifier.width(SizeTokens.WIDGET_CHECKBOX.dp))
        } else {
            CheckBox(
                checked = false,
                onCheckedChange = actionRunCallback<CompleteTaskAction>(actionParametersOf(CompleteTaskAction.TASK_ID to item.id)),
                colors = CheckboxDefaults.colors(checkedColor = WidgetColors.accent, uncheckedColor = WidgetColors.textSecondary),
            )
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(
                item.title,
                style = TextStyle(color = if (item.isGhost) WidgetColors.textSecondary else WidgetColors.textPrimary, fontSize = 15.sp),
                maxLines = 2,
            )
            val meta = widgetMeta(item, days, context.getString(R.string.inbox_list_name), showList = filter !is WidgetFilter.ProjectList)
            if (meta.isNotEmpty()) {
                Text(
                    meta + if (item.isRecurring) " ↻" else "",
                    style = TextStyle(color = if (section == WidgetSectionKind.OVERDUE) WidgetColors.overdue else secondary, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        PriorityLabel(item.priority)
    }
}

@Composable
private fun PriorityLabel(priority: Priority) {
    val (label, colors) = when (priority) {
        Priority.HIGH -> "P1" to WidgetColors.priorityHigh
        Priority.MEDIUM -> "P2" to WidgetColors.priorityMedium
        Priority.LOW -> "P3" to WidgetColors.priorityLow
        Priority.NONE -> return
    }
    Text(
        label,
        style = TextStyle(color = colors.content, fontSize = 11.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.None),
        modifier = GlanceModifier
            .background(colors.container)
            .cornerRadius(RadiusTokens.SM.dp)
            .padding(horizontal = SpacingTokens.XS.dp, vertical = SpacingTokens.XXS.dp),
    )
}

/** «Сегодня, 10:00 · Работа»: день, время и список (в виджете одного списка — без него). */
fun widgetMeta(item: TaskItem, days: DayNames, inboxName: String, showList: Boolean): String {
    val due = item.due?.let { days.format(it) }
    val list = if (!showList) null else if (item.isInboxList) inboxName else item.listName
    return listOfNotNull(due, list).joinToString(" · ")
}

/** Открыть задачу в приложении. У каждой задачи свой адрес — иначе Android подменит одно нажатие другим. */
fun openTaskIntent(context: Context, taskId: String): Intent = Intent(context, MainActivity::class.java)
    .setData("simpletasks://task/$taskId".toUri())
    .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/** «+»: создание задачи — на сегодня («Все»), во «Входящие» или в выбранный список. */
fun quickAddIntent(context: Context, filter: WidgetFilter): Intent = Intent(context, MainActivity::class.java)
    .setData("simpletasks://add/${filter.encode()}".toUri())
    .putExtra(MainActivity.EXTRA_QUICK_ADD, true)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, filter == WidgetFilter.All)
    .putExtra(MainActivity.EXTRA_QUICK_ADD_PROJECT, (filter as? WidgetFilter.ProjectList)?.projectId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun pickerIntent(context: Context, appWidgetId: Int): Intent = Intent(context, WidgetConfigActivity::class.java)
    .setData("simpletasks://widget/$appWidgetId".toUri())
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
