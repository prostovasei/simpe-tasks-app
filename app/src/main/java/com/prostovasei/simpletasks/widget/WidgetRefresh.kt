package com.prostovasei.simpletasks.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first

/**
 * Обновление виджетов — только штатным путём Glance (доработка 0.9.1). В 0.8.1–0.9.0 картинка дополнительно
 * строилась вторым сеансом Glance для того же виджета; два сеанса мешали друг другу — мелькал старый список,
 * виджет пропадал (docs/ARCHITECTURE.md §4.12). Теперь рисует один сеанс, а здесь его только будят.
 */
object WidgetRefresh {
    /** Все виджеты на рабочем столе: изменились задачи, полночь, галочка. */
    suspend fun all(context: Context, reason: String) {
        WidgetLog.add(context, "обновить все: $reason")
        TasksWidget().updateAll(context)
    }

    /** Один виджет: сменился выбранный список. Сеанс перечитает выбор и перерисует виджет. */
    suspend fun one(context: Context, glanceId: GlanceId, reason: String) {
        WidgetLog.add(context, "обновить виджет: $reason")
        TasksWidget().update(context, glanceId)
    }

    /** Данные виджета по сохранённому выбору списка — для первой отрисовки сеанса. */
    suspend fun loadModel(context: Context, glanceId: GlanceId): WidgetModel {
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        return context.widgetEntryPoint().observeModel(WidgetFilter.decode(prefs[TasksWidget.FILTER_KEY])).first()
    }
}
