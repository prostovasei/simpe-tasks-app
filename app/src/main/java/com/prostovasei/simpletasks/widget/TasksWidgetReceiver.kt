package com.prostovasei.simpletasks.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MIDNIGHT) return super.onReceive(context, intent)
        // Новый день: «Сегодня» и «Просрочено» в виджете пересчитываются, даже если приложение не запущено.
        val pending = goAsync()
        MainScope().launch {
            try {
                WidgetRefresh.all(context, "полночь")
                scheduleMidnight(context, context.widgetEntryPoint().time())
            } finally {
                pending.finish()
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetLog.add(context, "система: обновить ${appWidgetIds.joinToString()}")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetLog.add(context, "система: размер виджета $appWidgetId")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetLog.add(context, "система: виджет удалён ${appWidgetIds.joinToString()}")
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        const val ACTION_MIDNIGHT = "com.prostovasei.simpletasks.WIDGET_MIDNIGHT"
    }
}

/** Будильник на ближайшую полночь (не точный: минута-другая не важна, батарею бережём). */
fun scheduleMidnight(context: Context, time: TimeProvider) {
    val midnight = time.today().plusDays(1).atStartOfDay(time.zone()).toInstant().plusSeconds(MIDNIGHT_DELAY_SECONDS)
    val intent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TasksWidgetReceiver::class.java).setAction(TasksWidgetReceiver.ACTION_MIDNIGHT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, midnight.toEpochMilli(), intent)
}

private const val MIDNIGHT_DELAY_SECONDS = 30L

/**
 * Обновляет виджеты при любых изменениях задач и списков (в приложении, из уведомления, из виджета)
 * и при смене дня; ставит будильник на полночь, если виджеты есть на рабочем столе.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val dayTicker: DayTicker,
    private val time: TimeProvider,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            combine(tasks.observeDataChanges(), dayTicker.today()) { _, _ -> }.debounce(DEBOUNCE_MS).collect {
                if (GlanceAppWidgetManager(context).getGlanceIds(TasksWidget::class.java).isEmpty()) return@collect
                WidgetRefresh.all(context, "изменились задачи")
                scheduleMidnight(context, time)
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
