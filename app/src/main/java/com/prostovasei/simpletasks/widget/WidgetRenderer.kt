package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/**
 * Отрисовка виджета сразу при событии (доработка 0.9.2, docs/ARCHITECTURE.md §4.13). Раньше виджет рисовал
 * фоновый сеанс Glance: через 5 с без дела он засыпал, а Android будил его через общую очередь задач —
 * на телефоне PM до 10–20 с. Теперь картинка собирается здесь же (`GlanceRemoteViews`, без сеансов) и сразу
 * отдаётся рабочему столу. Это единственный путь отрисовки; обновления идут по очереди (`mutex`).
 * `GlanceRemoteViews` помечен в Glance как экспериментальный — при обновлении Glance проверять этот файл.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
object WidgetRenderer {
    private val mutex = Mutex()

    /** Свой сборщик на каждый виджет: одинаковая раскладка элементов от обновления к обновлению. */
    private val composers = HashMap<Int, GlanceRemoteViews>()

    /** Когда шкала «Сегодня» каждого виджета изменится сама (начало или конец задачи); null — до полуночи ничего. */
    private val nextChanges = HashMap<Int, Instant?>()

    fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TasksWidgetReceiver::class.java))

    suspend fun renderAll(context: Context, reason: String) {
        widgetIds(context).forEach { render(context, it, reason) }
    }

    suspend fun render(context: Context, appWidgetId: Int, reason: String) = mutex.withLock {
        val started = SystemClock.elapsedRealtime()
        try {
            val filter = selection(context, appWidgetId)
            val model = context.widgetEntryPoint().observeModel(filter).first()
            // Список удалили — виджет вернулся к «Все», запоминаем.
            if (model.filter != filter) WidgetSelection.set(context, appWidgetId, model.filter)
            val manager = AppWidgetManager.getInstance(context)
            val options = manager.getAppWidgetOptions(appWidgetId)
            val views = composers.getOrPut(appWidgetId) { GlanceRemoteViews() }
                .compose(context, sizeOf(options), appWidgetOptions = options) {
                    // Прокручиваемый список без сеанса Glance работает только на Android 12+.
                    WidgetContent(model, appWidgetId, scrollable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                }
                .remoteViews
            manager.updateAppWidget(appWidgetId, views)
            val time = context.widgetEntryPoint().time()
            nextChanges[appWidgetId] = nextChange(model, time.zone())
            scheduleRefresh(context, time, nextChanges.filterKeys { it in widgetIds(context) }.values.filterNotNull().minOrNull())
            val tasks = model.days.sumOf { it.items.size }
            val kb = views.parcelBytes() / BYTES_IN_KB
            WidgetLog.add(context, "отрисовка $appWidgetId: ${model.filter.encode()}, задач $tasks, ${SystemClock.elapsedRealtime() - started} мс, пакет $kb КБ, Android ${android.os.Build.VERSION.RELEASE} ($reason)")
        } catch (e: Exception) {
            WidgetLog.error(context, "отрисовка виджета $appWidgetId ($reason)", e)
        }
    }

    private fun nextChange(model: WidgetModel, zone: ZoneId): Instant? {
        val now = model.nowMinute ?: return null
        val today = model.days.find { it.day == WidgetDay.On(model.today) } ?: return null
        val minute = WidgetTimeline.nextChange(today.items, now) ?: return null
        return model.today.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant()
    }

    /** Выбор списка; до 0.9.2 он хранился в состоянии Glance — переносим один раз. */
    private suspend fun selection(context: Context, appWidgetId: Int): WidgetFilter =
        WidgetSelection.get(context, appWidgetId) ?: legacyFilter(context, appWidgetId).also { WidgetSelection.set(context, appWidgetId, it) }

    private suspend fun legacyFilter(context: Context, appWidgetId: Int): WidgetFilter = try {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        WidgetFilter.decode(prefs[LEGACY_FILTER_KEY])
    } catch (e: Exception) {
        WidgetFilter.All
    }

    /** Размер виджета из системы (портрет: ширина — минимальная, высота — максимальная). */
    private fun sizeOf(options: android.os.Bundle): DpSize {
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else DpSize.Unspecified
    }

    private val LEGACY_FILTER_KEY = stringPreferencesKey("filter")
    private const val BYTES_IN_KB = 1024
}

/** Размер картинки виджета при передаче рабочему столу (0.41.1): у Android лимит на пакет ~1 МБ на всё приложение. */
fun android.widget.RemoteViews.parcelBytes(): Int {
    val parcel = android.os.Parcel.obtain()
    return try {
        writeToParcel(parcel, 0)
        parcel.dataSize()
    } finally {
        parcel.recycle()
    }
}

/** Выбранный в виджете список — свой у каждого виджета на рабочем столе. */
object WidgetSelection {
    private const val PREFS = "widget_selection"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context, appWidgetId: Int): WidgetFilter? = prefs(context).getString(key(appWidgetId), null)?.let(WidgetFilter::decode)

    fun set(context: Context, appWidgetId: Int, filter: WidgetFilter) {
        prefs(context).edit().putString(key(appWidgetId), filter.encode()).commit()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(key(appWidgetId)).commit()
    }

    private fun key(appWidgetId: Int) = "filter_$appWidgetId"
}
