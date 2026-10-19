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
import org.junit.Assert.assertEquals
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

    private class Preview(private val model: WidgetModel, private val liveClock: Boolean = false) : GlanceAppWidget() {
        override suspend fun provideGlance(context: android.content.Context, id: GlanceId) =
            provideContent { WidgetContent(model, 1, scrollable = false, liveClock = liveClock) }
    }

    private fun item(
        id: String,
        title: String,
        due: DueText?,
        list: String? = "Работа",
        ghostOf: String? = null,
        priority: Priority = Priority.NONE,
        reminders: Boolean = false,
    ) = TaskItem(
        id, title, priority, due, list, LIST_COLORS[list], isInboxList = list == null, isRecurring = ghostOf != null || id == "gym", ghostOf = ghostOf,
        listIcon = LIST_ICONS[list ?: "Inbox"], hasReminders = reminders,
    )

    // Цвета и значки списков в тестовых данных (итерация 14).
    private val LIST_COLORS = mapOf("Работа" to "#6B5CE7", "Дом" to "#2F9E57", "Здоровье" to "#E0457B")
    private val LIST_ICONS = mapOf("Inbox" to "inbox", "Работа" to "work", "Дом" to "home", "Здоровье" to "health")

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
                    item("food", "Купить продукты", DueText(DayLabel.Today, null), list = "Дом", priority = Priority.LOW),
                    item("gym", "Зарядка", DueText(DayLabel.Today, "08:00"), list = null),
                    item("call", "Созвон с командой", DueText(DayLabel.Today, "10:00–11:00"), priority = Priority.MEDIUM, reminders = true),
                    item("doc", "Позвонить врачу", DueText(DayLabel.Today, "12:30"), list = "Здоровье", priority = Priority.HIGH),
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
        tabs = listOf(
            WidgetTab(WidgetFilter.All),
            WidgetTab(WidgetFilter.Inbox),
            WidgetTab(WidgetFilter.ProjectList("work"), "Работа", "#3D8BFD"),
            WidgetTab(WidgetFilter.ProjectList("home"), "Дом", "#46A758"),
        ),
        // Сейчас 09:40: «Зарядка» прошла, отметка «сейчас» — перед «Созвоном».
        nowMinute = 9 * 60 + 40,
    )

    @Test
    fun widgetLight() = capture("screenshots/widget_light.png")

    /** Основа миниатюры в меню виджетов (`res/drawable-nodpi/widget_preview.png`, см. docs/ARCHITECTURE.md §4.11). */
    @Test
    fun widgetPreview() = capture("screenshots/widget_preview.png", heightDp = 210, shown = model.copy(days = model.days.drop(1).take(1)))

    @Test
    @Config(qualifiers = "+night")
    fun widgetDark() = capture("screenshots/widget_dark.png")

    /** Настоящая картинка виджета: Glance молча отбрасывает детей Column сверх 10 — все 25 задач дня должны остаться. */
    @Test
    fun dayWithManyTasksKeepsAll() {
        val many = (1..25).map { item("t$it", "Задача $it", DueText(DayLabel.Today, null)) }
        val big = WidgetModel(WidgetFilter.All, null, today, listOf(WidgetDayBlock(WidgetDay.On(today), many)))
        val views = runBlocking { Preview(big).compose(context, size = DpSize(320.dp, 2000.dp)) }
        val root = views.apply(context, FrameLayout(context))
        val found = ArrayList<View>()
        root.findViewsWithText(found, "Выполнить", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        assertEquals(25, found.size)
    }

    /** Время отметки «сейчас» на телефоне — системные часы: идут сами, без перерисовки виджета. */
    @Test
    fun nowMarkerShowsSystemClock() {
        val views = runBlocking { Preview(model, liveClock = true).compose(context, size = DpSize(320.dp, 420.dp)) }
        val root = views.apply(context, FrameLayout(context))
        assertEquals(1, root.descendants().filterIsInstance<android.widget.TextClock>().count())
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is android.view.ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }

    /** Настоящая картинка: в нижней строке все кнопки на месте (Glance отбрасывает детей Row сверх 10). */
    @Test
    fun bottomBarKeepsAllButtons() {
        val views = runBlocking { Preview(model).compose(context, size = DpSize(320.dp, 420.dp)) }
        val root = views.apply(context, FrameLayout(context))
        listOf("Все", "Входящие", "Работа", "Дом", "Другие списки", "Открыть приложение", "Новая задача").forEach { label ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, 1, found.size)
        }
    }

    private fun capture(path: String, heightDp: Int = 420, shown: WidgetModel = model) {
        val views = runBlocking { Preview(shown).compose(context, size = DpSize(320.dp, heightDp.dp)) }
        val parent = FrameLayout(context)
        val view = views.apply(context, parent)
        val density = context.resources.displayMetrics.density
        val width = (320 * density).toInt()
        val height = (heightDp * density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        bitmap.captureRoboImage(path)
    }
}
