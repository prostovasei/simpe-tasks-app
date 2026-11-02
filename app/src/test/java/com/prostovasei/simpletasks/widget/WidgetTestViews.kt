package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.runBlocking
import org.robolectric.shadows.ShadowLooper
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/** Виджет Android 12+ целиком, как его собирает [WidgetRenderer]: корень с лентой и нижняя панель Glance поверх. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
fun renderWidget(context: Context, model: WidgetModel, size: DpSize = DpSize(320.dp, 420.dp), liveClock: Boolean = false): RemoteViews =
    runBlocking {
        val overlay = GlanceRemoteViews().compose(context, size, appWidgetOptions = Bundle()) {
            WidgetContent(model, 1, scrollable = true, liveClock = liveClock)
        }.remoteViews
        widgetRootViews(context, 1, model, overlay, size, liveClock)
    }

/**
 * Применить виджет, как рабочий стол (итерация 42): после передачи в другой процесс и в фоне (applyAsync — скрытый метод
 * Android, поэтому через отражение). Обычный apply прощал вложенный в Glance список, а Nova оставляла его пустым.
 */
fun RemoteViews.applyLikeLauncher(context: Context): View {
    val parcel = android.os.Parcel.obtain()
    val received = try {
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        RemoteViews.CREATOR.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
    var result: View? = null
    var error: Throwable? = null
    val listenerClass = Class.forName("android.widget.RemoteViews\$OnViewAppliedListener")
    val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { _, method, args ->
        when (method.name) {
            "onViewApplied" -> result = args[0] as View
            "onError" -> error = args[0] as Throwable
        }
        null
    }
    val applyAsync = RemoteViews::class.java.methods.first { it.name == "applyAsync" && it.parameterTypes.size == 4 }
    applyAsync.invoke(received, context, AppWidgetHostView(context), Executor { it.run() }, listener)
    ShadowLooper.idleMainLooper()
    error?.let { throw AssertionError("виджет не применился", it) }
    return checkNotNull(result) { "виджет не применился" }
}
