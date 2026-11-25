package com.prostovasei.simpletasks.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
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
import java.time.LocalDate

/** #340: виджет «Привычки» — «N из M сегодня», строки на сегодня с серией и отметкой, пустые состояния. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h700dp-xhdpi")
class WidgetHabitsTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val today = LocalDate.of(2026, 11, 25)
    private fun days(vararg ago: Long) = ago.map { today.minusDays(it) }.toSet()

    private val habits = listOf(
        Habit("h1", "Зарядка", "🏃", HabitSchedule.Daily, 0, checks = days(0, 1, 2, 3, 4)),
        Habit("h2", "Читать 20 минут", "📚", HabitSchedule.Daily, 1, checks = days(1, 2)),
        Habit("h3", "Бросковая тренировка", "🏀", HabitSchedule.PerWeek(3), 2, checks = days(2, 6, 7, 8)),
        Habit("h4", "Растяжка", "🧘", HabitSchedule.Weekdays(0b0000001), 3),
        Habit("h5", "Старая", null, HabitSchedule.Daily, 4, archived = true),
    )

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun widgetShowsTodayHabitsWithStreaksAndChecks() {
        val model = WidgetModel(WidgetFilter.All, null, today, emptyList())
        val size = DpSize(400.dp, 700.dp)
        val bar = runBlocking {
            GlanceRemoteViews().compose(context, size, appWidgetOptions = Bundle()) {
                CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) { BottomBar(model, 1, true, WidgetModule.HABITS) }
            }.remoteViews
        }
        val root = habitsViews(context, 1, habits, today, bar).applyLikeLauncher(context)
        assertEquals("1 из 3 сегодня", root.findViewById<TextView>(R.id.widget_habits_summary).text.toString())
        val list = root.descendants().filterIsInstance<ListView>().single()
        val rows = (0 until list.adapter.count).map { list.adapter.getView(it, null, list) }
        val names = rows.map { it.findViewById<TextView>(R.id.widget_habit_name).text.toString() }
        // Растяжка (только пн) и архивная не нужны сегодня (ср).
        assertEquals(listOf("Зарядка", "Читать 20 минут", "Бросковая тренировка"), names)
        assertEquals("🔥 5", rows[0].findViewById<TextView>(R.id.widget_habit_streak).text.toString())
        assertEquals("🔥 1 нед.", rows[2].findViewById<TextView>(R.id.widget_habit_streak).text.toString())
        assertEquals("Снять отметку «Зарядка»", rows[0].findViewById<View>(R.id.widget_habit_row).contentDescription)
        assertEquals("Отметить «Читать 20 минут»", rows[1].findViewById<View>(R.id.widget_habit_row).contentDescription)

        val density = context.resources.displayMetrics.density
        val width = (400 * density).toInt()
        val height = (700 * density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        bitmap.captureRoboImage("screenshots/widget_habits_light.png")
    }

    @Test
    fun emptyStatesExplainWhatToDo() {
        val none = habitsViews(context, 1, emptyList(), today, null).applyLikeLauncher(context)
        assertEquals("Привычек пока нет", none.findViewById<TextView>(R.id.widget_habits_empty).text.toString())
        assertEquals(View.GONE, none.findViewById<View>(R.id.widget_habits_list).visibility)
        val mondayOnly = habitsViews(context, 1, listOf(habits[3]), today, null).applyLikeLauncher(context)
        assertEquals("На сегодня привычек нет", mondayOnly.findViewById<TextView>(R.id.widget_habits_empty).text.toString())
        assertTrue(mondayOnly.findViewById<View>(R.id.widget_habits_empty).hasOnClickListeners())
    }

    @Test
    fun toggleHasItsOwnAddressPerHabit() {
        assertEquals(false, WidgetActions.toggleHabit(context, 1, "h1").filterEquals(WidgetActions.toggleHabit(context, 1, "h2")))
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }
}
