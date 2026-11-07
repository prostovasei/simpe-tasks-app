package com.prostovasei.simpletasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.prostovasei.simpletasks.ui.feature.week.monthAfterPick
import com.prostovasei.simpletasks.ui.feature.week.weekStart
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Нажатия в виджете (доработка 0.9.2): вкладка списка, галочка; будильник полуночи и шкалы дня. Обрабатываются сразу этим приёмником
 * приложения — без фонового сеанса Glance и очереди задач Android. Приёмник закрыт для других приложений.
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                handle(app, intent)
            } catch (e: Exception) {
                WidgetLog.error(app, "нажатие ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        suspend fun handle(context: Context, intent: Intent) {
            when (intent.action) {
                WidgetActions.SWITCH -> {
                    val id = intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1)
                    val filter = WidgetFilter.decode(intent.getStringExtra(WidgetActions.EXTRA_FILTER))
                    WidgetLog.add(context, "вкладка: ${filter.encode()}")
                    WidgetSelection.set(context, id, filter)
                    WidgetRenderer.render(context, id, "вкладка")
                }
                WidgetActions.COMPLETE -> {
                    val taskId = intent.getStringExtra(WidgetActions.EXTRA_TASK_ID) ?: return
                    WidgetLog.add(context, "галочка: задача выполнена")
                    context.widgetEntryPoint().tasks().complete(taskId)
                    WidgetRenderer.renderAll(context, "галочка")
                }
                WidgetActions.UNCOMPLETE -> {
                    val taskId = intent.getStringExtra(WidgetActions.EXTRA_TASK_ID) ?: return
                    WidgetLog.add(context, "галочка: отметка снята")
                    context.widgetEntryPoint().tasks().setCompleted(taskId, completed = false)
                    WidgetRenderer.renderAll(context, "галочка")
                }
                WidgetActions.TOGGLE_OVERDUE -> {
                    WidgetLog.add(context, "просрочено: свернуть/развернуть")
                    context.widgetEntryPoint().overdueFold().toggle()
                    WidgetRenderer.renderAll(context, "просрочено")
                }
                WidgetActions.PICK_WEEK -> {
                    val id = intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1)
                    val date = intent.getStringExtra(WidgetActions.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
                    val pick = WidgetSelection.weekPick(context, id)
                    // 0.46.0: стрелок месяца нет — неделя с днями соседнего месяца перелистывает календарь на него.
                    val shown = pick.month ?: YearMonth.from(LocalDate.now(context.widgetEntryPoint().time().zone()))
                    val month = monthAfterPick(shown, date.weekStart())
                    WidgetLog.add(context, "неделя: $date, месяц: $month")
                    WidgetSelection.setWeekPick(context, id, WeekPick(start = date.weekStart(), month = month))
                    WidgetRenderer.render(context, id, "неделя")
                }
                WidgetActions.TODAY -> {
                    // Итерация 51: дата в нижней строке — к сегодня: «Неделя» — текущая неделя и месяц, лента — к «Сегодня» / в начало.
                    val id = intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1)
                    val filter = WidgetSelection.get(context, id) ?: WidgetFilter.All
                    WidgetLog.add(context, "к сегодня: ${filter.encode()}")
                    if (filter == WidgetFilter.Week) WidgetSelection.setWeekPick(context, id, WeekPick())
                    WidgetRenderer.render(context, id, "сегодня", scrollToToday = filter != WidgetFilter.Week)
                }
                WidgetActions.MIDNIGHT -> WidgetRenderer.renderAll(context, "время")
            }
        }
    }
}

object WidgetActions {
    const val SWITCH = "com.prostovasei.simpletasks.widget.SWITCH"
    const val COMPLETE = "com.prostovasei.simpletasks.widget.COMPLETE"
    const val UNCOMPLETE = "com.prostovasei.simpletasks.widget.UNCOMPLETE"
    const val TOGGLE_OVERDUE = "com.prostovasei.simpletasks.widget.TOGGLE_OVERDUE"
    const val PICK_WEEK = "com.prostovasei.simpletasks.widget.PICK_WEEK"
    const val MIDNIGHT = "com.prostovasei.simpletasks.WIDGET_MIDNIGHT"
    const val TODAY = "com.prostovasei.simpletasks.widget.TODAY"
    const val EXTRA_DATE = "date"
    const val EXTRA_WIDGET_ID = "widget_id"
    const val EXTRA_FILTER = "filter"
    const val EXTRA_TASK_ID = "task_id"

    /** У каждого нажатия свой адрес — иначе Android подменит одно нажатие другим. */
    fun switch(context: Context, appWidgetId: Int, filter: WidgetFilter): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(SWITCH)
        .setData("simpletasks://widget/$appWidgetId/switch/${filter.encode()}".toUri())
        .putExtra(EXTRA_WIDGET_ID, appWidgetId)
        .putExtra(EXTRA_FILTER, filter.encode())

    fun complete(context: Context, taskId: String): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(COMPLETE)
        .setData("simpletasks://widget/complete/$taskId".toUri())
        .putExtra(EXTRA_TASK_ID, taskId)

    /** Свернуть или развернуть «Просрочено» (итерация 37). */
    fun toggleOverdue(context: Context): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(TOGGLE_OVERDUE)
        .setData("simpletasks://widget/overdue/toggle".toUri())

    /** Выбор недели в мини-календаре виджета (итерация 45). */
    fun pickWeek(context: Context, appWidgetId: Int, date: String): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(PICK_WEEK)
        .setData("simpletasks://widget/$appWidgetId/week/$date".toUri())
        .putExtra(EXTRA_WIDGET_ID, appWidgetId)
        .putExtra(EXTRA_DATE, date)

    /** Дата в нижней строке (итерация 51): вернуть виджет к сегодня. */
    fun today(context: Context, appWidgetId: Int): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(TODAY)
        .setData("simpletasks://widget/$appWidgetId/today".toUri())
        .putExtra(EXTRA_WIDGET_ID, appWidgetId)

    /** Снять отметку с выполненной (итерация 27): выполненная остаётся в виджете до полуночи. */
    fun uncomplete(context: Context, taskId: String): Intent = Intent(context, WidgetActionReceiver::class.java)
        .setAction(UNCOMPLETE)
        .setData("simpletasks://widget/uncomplete/$taskId".toUri())
        .putExtra(EXTRA_TASK_ID, taskId)
}
