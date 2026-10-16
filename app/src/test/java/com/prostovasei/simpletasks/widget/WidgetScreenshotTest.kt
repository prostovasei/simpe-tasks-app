package com.prostovasei.simpletasks.widget

import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Снимок виджета: Glance → RemoteViews → обычный View (как на рабочем столе). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetScreenshotTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private class Preview(private val model: WidgetModel) : GlanceAppWidget() {
        override suspend fun provideGlance(context: android.content.Context, id: GlanceId) = provideContent { WidgetContent(model, 1, scrollable = false) }
    }

    private fun item(id: String, title: String, due: DueText?, list: String? = "Работа", ghostOf: String? = null, priority: Priority = Priority.NONE) =
        TaskItem(id, title, priority, due, list, null, isInboxList = list == null, isRecurring = ghostOf != null || id == "gym", ghostOf = ghostOf)

    private val today = LocalDate.of(2026, 10, 16)

    private val model = WidgetModel(
        WidgetFilter.All,
        null,
        today,
        listOf(
            WidgetDayBlock(WidgetDay.Overdue, listOf(item("late", "Отправить отчёт", DueText(DayLabel.Yesterday, null), priority = Priority.HIGH))),
            WidgetDayBlock(
                WidgetDay.On(today),
                listOf(
                    item("gym", "Зарядка", DueText(DayLabel.Today, "08:00"), list = null),
                    item("call", "Созвон с командой", DueText(DayLabel.Today, "10:00–11:00"), priority = Priority.MEDIUM),
                    item("food", "Купить продукты", DueText(DayLabel.Today, null), list = "Дом", priority = Priority.LOW),
                ),
            ),
            WidgetDayBlock(
                WidgetDay.On(today.plusDays(1)),
                listOf(
                    item("gym@0", "Зарядка", DueText(DayLabel.Tomorrow, "08:00"), list = null, ghostOf = "gym"),
                    item("net", "Оплатить интернет", DueText(DayLabel.Tomorrow, null)),
                ),
            ),
            WidgetDayBlock(
                WidgetDay.On(today.plusDays(3)),
                listOf(item("doc", "Записаться к врачу", DueText(DayLabel.Date(today.plusDays(3), false), "09:30"), list = "Здоровье")),
            ),
        ),
    )

    @Test
    fun widgetLight() = capture("screenshots/widget_light.png")

    @Test
    @Config(qualifiers = "+night")
    fun widgetDark() = capture("screenshots/widget_dark.png")

    private fun capture(path: String) {
        val views = runBlocking { Preview(model).compose(context, size = DpSize(320.dp, 420.dp)) }
        val parent = FrameLayout(context)
        val view = views.apply(context, parent)
        val density = context.resources.displayMetrics.density
        val width = (320 * density).toInt()
        val height = (420 * density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        bitmap.captureRoboImage(path)
    }
}
