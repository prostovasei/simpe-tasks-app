package com.prostovasei.simpletasks.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.ui.feature.all.OverdueFold
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Виджет на рабочем столе — обычный AppWidgetProvider (с 0.9.2 без GlanceAppWidgetReceiver и его сеансов).
 * Система просит обновить или изменила размер — рисуем сразу через [WidgetRenderer].
 */
class TasksWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetLog.add(context, "система: обновить ${appWidgetIds.joinToString()}")
        inBackground {
            appWidgetIds.forEach { WidgetRenderer.render(context.applicationContext, it, "система") }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetLog.add(context, "система: размер виджета $appWidgetId")
        inBackground { WidgetRenderer.render(context.applicationContext, appWidgetId, "размер") }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetLog.add(context, "система: виджет удалён ${appWidgetIds.joinToString()}")
        appWidgetIds.forEach { WidgetSelection.remove(context, it) }
    }

    private fun inBackground(block: suspend () -> Unit) {
        val pending = goAsync()
        scope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/**
 * Будильник на ближайшее изменение виджета: полночь или, с итерации 20, начало/конец сегодняшней задачи
 * ([next] — для шкалы «Сегодня»: отметка «сейчас» и бледные прошедшие задачи). Не точный: минута-другая
 * не важна, батарею бережём. Будильник один на все виджеты — новый заменяет прежний.
 */
fun scheduleRefresh(context: Context, time: TimeProvider, next: Instant?) {
    val midnight = time.today().plusDays(1).atStartOfDay(time.zone()).toInstant().plusSeconds(MIDNIGHT_DELAY_SECONDS)
    val at = next?.takeIf { it.isAfter(time.now()) && it.isBefore(midnight) } ?: midnight
    val intent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetActionReceiver::class.java).setAction(WidgetActions.MIDNIGHT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, at.toEpochMilli(), intent)
}

private const val MIDNIGHT_DELAY_SECONDS = 30L

/**
 * Обновляет виджеты при любых изменениях задач и списков (в приложении, из уведомления, из виджета)
 * и при смене дня; будильник на следующее обновление ставит сама отрисовка.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val finance: com.prostovasei.simpletasks.domain.repository.FinanceRepository,
    private val health: com.prostovasei.simpletasks.domain.repository.HealthRepository,
    private val dayTicker: DayTicker,
    private val overdueFold: OverdueFold,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            health.observeEntries().debounce(DEBOUNCE_MS).collect {
                WidgetRenderer.widgetIds(context).filter { WidgetSelection.module(context, it) == WidgetModule.HEALTH }.forEach {
                    WidgetRenderer.render(context, it, "изменился вес")
                }
            }
        }
        appScope.launch {
            combine(finance.observeAccounts(), finance.observeOperations()) { _, _ -> }
                .debounce(DEBOUNCE_MS).collect { refreshFinance() }
        }
        val prefs = com.prostovasei.simpletasks.ui.feature.finance.financeLimitPreferences(context)
        prefs.registerOnSharedPreferenceChangeListener(limitListener)

        appScope.launch {
            combine(tasks.observeDataChanges(), dayTicker.today(), overdueFold.collapsed) { _, _, _ -> }.debounce(DEBOUNCE_MS).collect {
                if (WidgetRenderer.widgetIds(context).isEmpty()) return@collect
                WidgetRenderer.renderAll(context, "изменились задачи")
            }
        }
    }

    private val limitListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        appScope.launch { refreshFinance() }
    }

    private suspend fun refreshFinance() {
        WidgetRenderer.widgetIds(context).filter { WidgetSelection.finance(context, it) }.forEach {
            WidgetRenderer.render(context, it, "изменились финансы")
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
