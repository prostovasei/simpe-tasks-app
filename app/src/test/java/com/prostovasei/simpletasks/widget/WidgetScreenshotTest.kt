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
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
        reminder: ReminderMark? = null,
        done: Boolean = false,
    ) = TaskItem(
        id, title, priority, due, list, LIST_EMOJIS[list] ?: if (due != null) "📌" else "📥", isInboxList = list == null, isRecurring = ghostOf != null || id == "gym", ghostOf = ghostOf, reminder = reminder, isDone = done,
    )

    // Эмодзи списков в тестовых данных (итерация 47; до этого — цвета и значки).
    private val LIST_EMOJIS = mapOf("Работа" to "💼", "Дом" to "🏠", "Здоровье" to "❤️")

    private val today = LocalDate.of(2026, 10, 16)

    private val model = WidgetModel(
        WidgetFilter.All,
        null,
        today,
        listOf(
            // Итерация 27: просроченные — по своим дням, выполненные остаются до полуночи (галочка, зачёркнуты, бледнее).
            WidgetDayBlock(
                WidgetDay.On(today.minusDays(1)),
                listOf(
                    item("late", "Отправить отчёт", DueText(DayLabel.Yesterday, null), priority = Priority.HIGH),
                    item("cafe", "Кафе", DueText(DayLabel.Yesterday, "18:00–21:00"), list = "Дом", reminder = ReminderMark.EARLY, done = true),
                ),
            ),
            WidgetDayBlock(
                WidgetDay.On(today),
                listOf(
                    item("food", "Купить продукты", DueText(DayLabel.Today, null), list = "Дом", priority = Priority.NONE, reminder = ReminderMark.AT_DUE),
                    item("gym", "Зарядка", DueText(DayLabel.Today, "08:00"), list = null, reminder = ReminderMark.AT_DUE, done = true),
                    item("call", "Созвон с командой", DueText(DayLabel.Today, "10:00–11:00"), priority = Priority.HIGH, reminder = ReminderMark.EARLY),
                    item("doc", "Позвонить врачу", DueText(DayLabel.Today, "12:30"), list = "Здоровье", priority = Priority.HIGH, reminder = ReminderMark.EARLY),
                ),
            ),
            WidgetDayBlock(
                WidgetDay.On(today.plusDays(1)),
                listOf(
                    item("gym@0", "Зарядка", DueText(DayLabel.Tomorrow, "08:00"), list = null, ghostOf = "gym"),
                    item("net", "Оплатить интернет", DueText(DayLabel.Tomorrow, null), reminder = ReminderMark.EARLY),
                ),
            ),
            WidgetDayBlock(
                WidgetDay.On(today.plusDays(3)),
                listOf(item("doc", "Записаться к врачу", DueText(DayLabel.Date(today.plusDays(3), false), "09:30"), list = "Здоровье")),
            ),
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

    /** Лента на обычных элементах Android (итерация 42, как на телефоне): должна совпадать с [widgetLight] 1-в-1. */
    @Test
    fun widgetListLight() = capture("screenshots/widget_list_light.png", scrollable = true)

    @Test
    @Config(qualifiers = "+night")
    fun widgetListDark() = capture("screenshots/widget_list_dark.png", scrollable = true)

    /** Вид «Неделя» в виджете (итерация 45): четыре строки одинаковой высоты, «+N», мини-календарь, нижняя панель с тремя вкладками. */
    @Test
    fun widgetWeekLight() = capture("screenshots/widget_week_light.png", heightDp = 640, shown = weekModel, scrollable = true)

    @Test
    @Config(qualifiers = "+night")
    fun widgetWeekDark() = capture("screenshots/widget_week_dark.png", heightDp = 640, shown = weekModel, scrollable = true)

    @Test
    fun widgetWeekPriorityStates() {
        val model = weekModel
        val week = requireNotNull(model.week)
        val first = week.days.first().items.first()
        val day = week.days.first().copy(items = listOf(
            first.copy(id = "p1", title = "Высокий приоритет", priority = Priority.HIGH, isDone = false),
            first.copy(id = "p2", title = "Выполненный приоритет", priority = Priority.HIGH, isDone = true),
            first.copy(id = "p3", title = "Повтор выполнен", priority = Priority.HIGH, isDone = true, isRecurring = true),
            first.copy(id = "p4", title = "Без времени", priority = Priority.HIGH, isDone = false, due = DueText(DayLabel.Today, null)),
        ))
        capture("screenshots/widget_week_priority_states.png", heightDp = 640,
            shown = model.copy(week = week.copy(days = listOf(day) + week.days.drop(1))), scrollable = true)
    }

    private val weekModel: WidgetModel
        get() {
            val monday = today.minusDays(4)
            fun day(offset: Int) = monday.plusDays(offset.toLong())
            val tasks = mapOf(
                0 to listOf(item("w1", "Купить продукты", DueText(DayLabel.Today, "10:00"), priority = Priority.NONE, done = true), item("w2", "Тренировка", DueText(DayLabel.Today, null))),
                1 to listOf(item("w3", "Оплатить интернет", DueText(DayLabel.Today, "12:00"), priority = Priority.HIGH), item("w4", "Позвонить маме", DueText(DayLabel.Today, null))),
                2 to listOf(item("w5", "Встреча", DueText(DayLabel.Today, "15:00"), priority = Priority.HIGH)),
                4 to (1..6).map { item("w1$it", "Задача $it", DueText(DayLabel.Today, if (it == 1) "17:00" else null), priority = if (it == 1) Priority.HIGH else Priority.NONE) },
                5 to listOf(item("w6", "Уборка", DueText(DayLabel.Today, "11:00")), item("w7@0", "Созвон с командой", DueText(DayLabel.Today, "10:00"), ghostOf = "w7")),
                6 to listOf(item("w8", "План на неделю", DueText(DayLabel.Today, null))),
            )
            return WidgetModel(
                WidgetFilter.Week, null, today, emptyList(),
                week = WidgetWeek(monday, java.time.YearMonth.from(today), (0..6).map { com.prostovasei.simpletasks.ui.feature.week.WeekDay(day(it), tasks[it].orEmpty()) }, today),
            )
        }

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

    /** Нижняя строка содержит «Задачи», «Финансы», «Открыть приложение» и «+». */
    @Test
    fun bottomBarKeepsAllButtons() {
        val views = runBlocking { Preview(model).compose(context, size = DpSize(320.dp, 420.dp)) }
        val root = views.apply(context, FrameLayout(context))
        // Итерация 57: галочки нет — приложение открывает свободное место строки (две свободные полосы).
        mapOf("Задачи · Все, выбрано" to 1, "Финансы" to 1, "Открыть приложение" to 2, "Новая задача" to 1).forEach { (label, count) ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, count, found.size)
        }
        // Вкладок своих списков и «Другие списки» больше нет.
        listOf("Работа", "Другие списки").forEach { label ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, 0, found.size)
        }
    }

    private fun capture(path: String, heightDp: Int = 420, shown: WidgetModel = model, scrollable: Boolean = false) {
        // Лента Android 12+ — как на телефоне: корень с лентой, нижняя панель Glance поверх, применение в фоне.
        val view = if (scrollable) {
            renderWidget(context, shown, DpSize(320.dp, heightDp.dp)).applyLikeLauncher(context)
        } else {
            runBlocking { Preview(shown).compose(context, size = DpSize(320.dp, heightDp.dp)) }.apply(context, FrameLayout(context))
        }
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
