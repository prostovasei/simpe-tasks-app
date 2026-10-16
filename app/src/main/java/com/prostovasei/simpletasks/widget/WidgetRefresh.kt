package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first

/**
 * Мгновенное обновление виджетов (доработка 0.8.1). Обычный путь Glance перерисовывает виджет фоновой задачей,
 * которую Android может отложить на несколько секунд, — переключение списка и галочка «зависали».
 * Здесь картинка строится сразу и отдаётся рабочему столу напрямую; затем Glance догоняет тем же содержимым.
 */
object WidgetRefresh {
    /** Все виджеты на рабочем столе. */
    suspend fun all(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(TasksWidget::class.java).forEach { one(context, it) }
    }

    suspend fun one(context: Context, glanceId: GlanceId) {
        try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            val model = loadModel(context, glanceId)
            val manager = AppWidgetManager.getInstance(context)
            val views = Snapshot(model, appWidgetId).compose(context, glanceId, options = manager.getAppWidgetOptions(appWidgetId))
            manager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            // Не получилось сразу — виджет всё равно обновится обычным путём ниже, чуть позже.
            Log.w(TAG, "Instant widget refresh failed", e)
        }
        TasksWidget().update(context, glanceId)
    }

    /** Данные виджета по сохранённому выбору списка — те же, что покажет сам виджет. */
    suspend fun loadModel(context: Context, glanceId: GlanceId): WidgetModel {
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        return context.widgetEntryPoint().observeModel(WidgetFilter.decode(prefs[TasksWidget.FILTER_KEY])).first()
    }

    /** Готовый снимок виджета для готовых данных. */
    private class Snapshot(private val model: WidgetModel, private val appWidgetId: Int) : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { WidgetContent(model, appWidgetId) }
    }

    private const val TAG = "WidgetRefresh"
}
