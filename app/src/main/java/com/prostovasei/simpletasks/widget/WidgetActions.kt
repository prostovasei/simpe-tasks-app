package com.prostovasei.simpletasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
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
                WidgetActions.MIDNIGHT -> WidgetRenderer.renderAll(context, "время")
            }
        }
    }
}

object WidgetActions {
    const val SWITCH = "com.prostovasei.simpletasks.widget.SWITCH"
    const val COMPLETE = "com.prostovasei.simpletasks.widget.COMPLETE"
    const val MIDNIGHT = "com.prostovasei.simpletasks.WIDGET_MIDNIGHT"
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
}
