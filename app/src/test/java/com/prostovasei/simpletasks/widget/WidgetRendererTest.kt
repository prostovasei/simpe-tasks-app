package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
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
        listOf("Разделы", "Настройки", "Создать задачу голосом", "Добавить").forEach {
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
        // Дождаться первой отрисовки, которая сохраняет исходный раздел нового виджета.
        WidgetRenderer.render(context, one, "тест")
        WidgetActionReceiver.handle(context, WidgetActions.switch(context, one, WidgetFilter.Inbox))
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, true))
        assertTrue(WidgetSelection.finance(context, one))
        assertTrue(!WidgetSelection.finance(context, two))
        assertTrue(manager.getViewFor(one).hasDescription("Добавить"))
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, WidgetModule.HEALTH))
        assertEquals(WidgetModule.HEALTH, WidgetSelection.module(context, one))
        assertEquals(WidgetModule.TASKS, WidgetSelection.module(context, two))
        assertTrue(manager.getViewFor(one).hasDescription("Добавить"))
        WidgetActionReceiver.handle(context, WidgetActions.module(context, one, false))
        assertEquals(WidgetFilter.Inbox, WidgetSelection.get(context, one))
        assertTrue(manager.getViewFor(one).hasDescription("Добавить"))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun healthChartFillsTallWidgetWithOneEntry() = runBlocking {
        checkHealthLayout(320, 600, listOf(820), "widget_health_tall_light")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun compactHealthWidgetFitsChartAndFourTabs() = runBlocking {
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

        // #338: капсула справа — «Разделы», «Настройки», микрофон, «Добавить» по порядку слева направо.
        val buttons = listOf("Разделы", "Настройки", "Создать задачу голосом", "Добавить")
        val centers = buttons.map { label ->
            val matches = ArrayList<View>()
            panel.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            val icon = matches.single { it.contentDescription == label }
            assertTrue(icon.width > 0)
            generateSequence(icon) { it.parent as? View }.sumOf { it.left } + icon.width / 2
        }
        assertTrue(centers.zipWithNext { left, right -> right - left }.all { it > 0 })
        assertTrue("капсула справа", centers.last() > panel.width / 2)
        val goalLabel = root.findViewById<android.widget.TextView>(R.id.widget_health_goal)
        assertEquals("Цель 80,0 кг · осталось 2,0 кг", goalLabel.text.toString())
        val details = root.findViewById<android.widget.TextView>(R.id.widget_health_details)
        assertEquals(if (height == 110) View.GONE else View.VISIBLE, details.visibility)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        bitmap.captureRoboImage("screenshots/$screenshot.png")
    }

    /** #336: в каждом разделе — тёмная капсула справа: дата (в «Задачах» — к сегодня), «Разделы», «Настройки», «+» раздела. */
    @Test
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    fun barIsCapsuleOnTheRightInEveryModule() = runBlocking {
        val today = LocalDate.of(2026, 11, 21)
        val model = WidgetModel(WidgetFilter.All, null, today, emptyList())
        val density = context.resources.displayMetrics.density
        val description = context.getString(R.string.widget_today_date, WidgetDates.bar(today, withWeekday = true))
        for (widthDp in listOf(180, 392)) for (module in WidgetModule.entries) {
            val bar = GlanceRemoteViews().compose(context, DpSize(widthDp.dp, 110.dp)) {
                CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) {
                    BottomBar(model, 1, imageButtons = true, module = module)
                }
            }.remoteViews.apply(context, FrameLayout(context))
            val width = (widthDp * density).toInt()
            bar.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((52 * density).toInt(), View.MeasureSpec.EXACTLY))
            bar.layout(0, 0, width, bar.measuredHeight)
            fun find(label: String): View {
                val nodes = ArrayList<View>()
                bar.findViewsWithText(nodes, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
                return nodes.single { it.contentDescription == label }
            }
            fun clickable(view: View) = generateSequence(view) { it.parent as? View }.take(3).any { it.hasOnClickListeners() }
            val add = "Добавить"
            // #352: в «Главной» — «Открыть приложение» и «Обновить» вместо «Настроек».
            val labels = if (module == WidgetModule.HUB) listOf("Открыть приложение", "Разделы", "Обновить приложение", "Создать задачу голосом", add)
            else listOf("Разделы", "Настройки", "Создать задачу голосом", add)
            labels.forEach { assertTrue("$module $it", clickable(find(it))) }
            val plus = find(add)
            val right = generateSequence(plus) { it.parent as? View }.sumOf { it.left } + plus.width
            assertTrue("$module: капсула у правого края", width - right <= ((SizeTokens.WIDGET_EDGE + SizeTokens.NAV_PAD + 1) * density).toInt())
            // #338: даты в капсуле нет.
            val dates = ArrayList<View>()
            bar.findViewsWithText(dates, WidgetDates.bar(today, withWeekday = true), View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(0, dates.size)
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
