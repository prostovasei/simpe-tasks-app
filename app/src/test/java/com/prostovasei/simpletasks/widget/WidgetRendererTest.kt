package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.widget.theme.LocalWidgetColors
import com.prostovasei.simpletasks.widget.theme.TaskWidgetColors
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Отрисовка без сеанса Glance (0.9.2): виджет на «рабочем столе» Robolectric получает картинку сразу,
 * вкладка меняет выбор и сразу перерисовывает виджет.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetRendererTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    private fun View.hasDescription(text: String): Boolean {
        val found = ArrayList<View>()
        findViewsWithText(found, text, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return found.isNotEmpty()
    }

    @Test
    fun renderPutsWidgetOnHomeScreenRightAway() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetRenderer.render(context, id, "тест")
        val view = manager.getViewFor(id)
        listOf("Задачи · Все, выбрано", "Финансы", "Открыть приложение", "Новая задача").forEach {
            assertTrue(it, view.hasDescription(it))
        }
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: all"))
    }

    @Test
    fun tabSwitchesListAndRedraws() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetRenderer.render(context, id, "тест")
        WidgetActionReceiver.handle(context, WidgetActions.switch(context, id, WidgetFilter.Inbox))
        assertEquals(WidgetFilter.Inbox, WidgetSelection.get(context, id))
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: inbox"))
    }

    @Test
    fun financeModuleIsIndependentAndReturningKeepsTaskView() = runBlocking {
        val one = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        val two = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetSelection.set(context, one, WidgetFilter.Inbox)
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, true))
        assertTrue(WidgetSelection.finance(context, one))
        assertTrue(!WidgetSelection.finance(context, two))
        assertTrue(manager.getViewFor(one).hasDescription("Финансы, выбрано"))
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, WidgetModule.HEALTH))
        assertEquals(WidgetModule.HEALTH, WidgetSelection.module(context, one))
        assertEquals(WidgetModule.TASKS, WidgetSelection.module(context, two))
        assertTrue(manager.getViewFor(one).hasDescription("Здоровье, выбрано"))
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, false))
        assertEquals(WidgetFilter.Inbox, WidgetSelection.get(context, one))
        assertTrue(manager.getViewFor(one).hasDescription("Задачи · Входящие, выбрано"))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun healthChartFillsTallWidgetWithOneEntry() = runBlocking {
        checkHealthLayout(320, 600, listOf(820), "widget_health_tall_light")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun compactHealthWidgetFitsChartAndThreeTabs() = runBlocking {
        checkHealthLayout(180, 110, listOf(831, 826, 820), "widget_health_compact_light")
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun checkHealthLayout(width: Int, height: Int, weights: List<Int>, screenshot: String) {
        val today = LocalDate.of(2026, 11, 21)
        val health = context.widgetEntryPoint().health()
        val size = DpSize(width.dp, height.dp)
        val model = WidgetModel(WidgetFilter.Week, null, today, emptyList())
        val bar = GlanceRemoteViews().compose(context, size) {
            CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) {
                BottomBar(model, 1, imageButtons = true, module = WidgetModule.HEALTH)
            }
        }.remoteViews
        val root = healthWidgetViews(context, today, bar, size).apply(context, FrameLayout(context))
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_health_chart).visibility)
        weights.forEachIndexed { index, weight ->
            health.save(WeightEntry(today.minusDays(4L * (weights.lastIndex - index + 1)), weight))
        }
        context.widgetEntryPoint().healthSettings().update { it.copy(goalTenths = 800, heightCm = 180) }
        // На рабочем столе Android применяет обновление поверх уже показанного пустого состояния.
        healthWidgetViews(context, today, bar, size).reapply(context, root)
        val density = context.resources.displayMetrics.density
        root.measure(
            View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val chart = root.findViewById<ImageView>(R.id.widget_health_chart)
        val dates = root.findViewById<View>(R.id.widget_health_dates)
        val panel = root.findViewById<View>(R.id.widget_health_bar)
        assertEquals(View.VISIBLE, chart.visibility)
        if (height == 110) {
            assertEquals(View.GONE, dates.visibility)
            assertEquals(panel.top, chart.bottom)
        } else {
            assertEquals(View.VISIBLE, dates.visibility)
            assertEquals(chart.bottom, dates.top)
            assertEquals(panel.top, dates.bottom)
        }
        val plot = RectF(0f, 0f, chart.drawable.intrinsicWidth.toFloat(), chart.drawable.intrinsicHeight.toFloat())
        chart.imageMatrix.mapRect(plot)
        assertTrue("График заполняет высоту: ${plot.height()} из ${chart.height}", abs(plot.height() - chart.height) <= 3 * density)
        assertEquals(ImageView.ScaleType.FIT_CENTER, chart.scaleType)

        val tabs = listOf("Задачи · Неделя", "Финансы", "Здоровье, выбрано")
        val centers = tabs.map { label ->
            val matches = ArrayList<View>()
            panel.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            val icon = matches.single { it.contentDescription == label }
            assertTrue(icon.width > 0)
            generateSequence(icon) { it.parent as? View }.sumOf { it.left } + icon.width / 2
        }
        val gaps = centers.zipWithNext { left, right -> right - left }
        assertTrue(gaps.all { it > 0 })
        assertTrue(gaps.max() - gaps.min() <= 2)
        assertEquals(width == 320, panel.hasDescription("Обновление"))
        if (width == 320) assertTrue(abs(centers[1] - root.width / 2) <= 2)
        val goalLabel = root.findViewById<android.widget.TextView>(R.id.widget_health_goal)
        assertEquals("Цель 80,0 кг · осталось 2,0 кг", goalLabel.text.toString())
        val details = root.findViewById<android.widget.TextView>(R.id.widget_health_details)
        assertEquals(if (height == 110) View.GONE else View.VISIBLE, details.visibility)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        bitmap.captureRoboImage("screenshots/$screenshot.png")
    }

    @Test
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    fun wideWidgetKeepsTabsAndDateInTheSamePlaceAcrossModules() = runBlocking {
        val today = LocalDate.of(2026, 11, 21)
        val model = WidgetModel(WidgetFilter.Week, null, today, emptyList())
        val density = context.resources.displayMetrics.density
        val width = (320 * density).toInt()
        val height = (40 * density).toInt()
        val description = context.getString(R.string.widget_today_date, WidgetDates.bar(today, withWeekday = true))
        for (module in WidgetModule.entries) {
            val bar = GlanceRemoteViews().compose(context, DpSize(320.dp, 110.dp)) {
                CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) {
                    BottomBar(model, 1, imageButtons = true, module = module)
                }
            }.remoteViews.apply(context, FrameLayout(context))
            bar.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            bar.layout(0, 0, width, height)
            fun find(label: String): View {
                val nodes = ArrayList<View>()
                bar.findViewsWithText(nodes, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
                return nodes.single { it.contentDescription == label }
            }
            fun center(view: View): Int =
                generateSequence(view) { it.parent as? View }.sumOf { it.left } + view.width / 2
            val finance = find(if (module == WidgetModule.FINANCE) "Финансы, выбрано" else "Финансы")
            val date = find(if (module == WidgetModule.TASKS) description else WidgetDates.bar(today, withWeekday = true))
            val update = find("Обновление")
            assertTrue(abs(center(finance) - width / 2) <= 1)
            assertTrue(abs(center(date) - 245 * density) <= 1)
            assertTrue(abs(center(update) - 292 * density) <= 1)
            val dateAction = generateSequence(date) { it.parent as? View }.take(3).any { it.hasOnClickListeners() }
            assertEquals(module == WidgetModule.TASKS, dateAction)
        }
    }

    @Test
    fun eachTapHasItsOwnAddress() {
        assertEquals("Разные вкладки — разные нажатия", false, WidgetActions.switch(context, 1, WidgetFilter.All).filterEquals(WidgetActions.switch(context, 1, WidgetFilter.Inbox)))
        assertEquals(false, WidgetActions.complete(context, "a").filterEquals(WidgetActions.complete(context, "b")))
    }

    @Test
    fun deletedWidgetForgetsSelection() {
        WidgetSelection.set(context, 42, WidgetFilter.Inbox)
        WidgetSelection.setFinance(context, 42, true)
        TasksWidgetReceiver().onDeleted(context, intArrayOf(42))
        assertTrue(!WidgetSelection.finance(context, 42))
        assertEquals(null, WidgetSelection.get(context, 42))
    }
}
