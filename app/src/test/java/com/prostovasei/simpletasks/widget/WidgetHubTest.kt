package com.prostovasei.simpletasks.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.hub.HubNews
import com.prostovasei.simpletasks.widget.theme.LocalWidgetColors
import com.prostovasei.simpletasks.widget.theme.TaskWidgetColors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** #329, этап 2: виджет «Главная» — полоса лимита и счета, задачи «просрочено · сегодня · завтра», новости 2 × 2 со стрелками. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetHubTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val today = LocalDate.of(2026, 11, 25)

    private fun item(id: String, title: String, day: DayLabel, time: String?) =
        TaskItem(id, title, Priority.NONE, DueText(day, time), "Работа", "💼", isInboxList = false)

    private val model = WidgetModel(
        WidgetFilter.All, null, today,
        listOf(
            WidgetDayBlock(WidgetDay.On(today.minusDays(1)), listOf(item("late", "Оплатить интернет", DayLabel.Yesterday, "10:00"))),
            WidgetDayBlock(WidgetDay.On(today), listOf(item("a", "Созвон с командой", DayLabel.Today, "10:00"), item("b", "Купить продукты", DayLabel.Today, null))),
            WidgetDayBlock(WidgetDay.On(today.plusDays(1)), listOf(item("c", "Стоматолог", DayLabel.Tomorrow, "09:30"))),
            WidgetDayBlock(WidgetDay.On(today.plusDays(3)), listOf(item("far", "Через три дня", DayLabel.Date(today.plusDays(3), false), null))),
        ),
    )

    private val news = (1..6).map { HubNews("https://example.com/$it", "09:0$it · Футбол", "Новость номер $it о матче", unread = it <= 2) }

    @Test
    fun hubModelKeepsOverdueTodayAndTomorrowOnly() {
        val days = model.forHub().days.map { (it.day as WidgetDay.On).date }
        assertEquals(listOf(today.minusDays(1), today, today.plusDays(1)), days)
    }

    @Test
    fun newsPagesWrapAround() {
        assertEquals(0, hubPage(3, 3))
        assertEquals(2, hubPage(-1, 3))
        assertEquals(0, hubPage(5, 0))
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun widgetShowsFinanceTasksAndNewsPage() {
        val month = YearMonth.from(today)
        val ops = listOf(Operation("o", OperationKind.EXPENSE, 5_400_000, "a", "Карта", null, null, "c", "Продукты", null, null, today))
        val rows = FinanceRows(context, 400, 760)
        val accounts = listOf(Account("a", "Карта", AccountType.entries.first(), "💳", 0, 4_200_000, Instant.EPOCH),
            Account("b", "Наличные", AccountType.entries.first(), "👛", 0, 600_000, Instant.EPOCH))
        val finance = listOf(rows.limitViews(MonthSpending.of(ops, month, today), 8_000_000, month, today, daily = true), rows.accountRow(accounts, 4, large = true))
        val size = DpSize(400.dp, 760.dp)
        val bar = runBlocking {
            GlanceRemoteViews().compose(context, size, appWidgetOptions = Bundle()) {
                CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) { BottomBar(model, 1, true, WidgetModule.HUB) }
            }.remoteViews
        }
        // #340: плашки привычек на сегодня после финансов; сделанная — с «✓», не сегодняшняя — не показывается.
        val habits = listOf(
            com.prostovasei.simpletasks.domain.model.habits.Habit("h1", "Зарядка", "🏃", com.prostovasei.simpletasks.domain.model.habits.HabitSchedule.Daily, checks = setOf(today)),
            com.prostovasei.simpletasks.domain.model.habits.Habit("h2", "Читать", "📚", com.prostovasei.simpletasks.domain.model.habits.HabitSchedule.Daily),
            com.prostovasei.simpletasks.domain.model.habits.Habit("h3", "Растяжка", "🧘", com.prostovasei.simpletasks.domain.model.habits.HabitSchedule.Weekdays(0b0000001)),
        )
        val root = hubViews(context, 1, model.forHub(), bar, finance, news, habits, today).applyLikeLauncher(context)
        val chips = root.descendants().filter { it.id == R.id.widget_hub_habit }.map { (it as TextView).text.toString() }.toList()
        assertEquals(listOf("🏃 Зарядка ✓", "📚 Читать"), chips)
        assertTrue(root.descendants().filter { it.id == R.id.widget_hub_habit }.all { it.hasOnClickListeners() })
        val list = root.descendants().filterIsInstance<ListView>().single()
        val texts = (0 until list.adapter.count).flatMap { list.adapter.getView(it, null, list).texts() }
        assertTrue(texts.toString(), "Оплатить интернет" in texts && "Стоматолог" in texts && "Через три дня" !in texts)
        val cards = root.descendants().filter { it.id == R.id.hub_card_title }.map { (it as TextView).text.toString() }.toList()
        assertEquals(news.take(4).map { it.title }, cards)
        // #346: точек и стрелок нет — невидимые края листают; заголовков разделов нет.
        listOf(R.id.widget_hub_prev, R.id.widget_hub_next).forEach { assertTrue(root.findViewById<View>(it).hasOnClickListeners()) }
        listOf(R.id.widget_hub_title_finance, R.id.widget_hub_title_news, R.id.widget_hub_title_tasks, R.id.widget_hub_title_habits)
            .forEach { assertEquals(View.GONE, root.findViewById<View>(it).visibility) }

        val density = context.resources.displayMetrics.density
        val width = (400 * density).toInt()
        val height = (760 * density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        bitmap.captureRoboImage("screenshots/widget_hub_light.png")
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }

    private fun View.texts() = descendants().filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.map { it.text.toString() }.toList()
}
