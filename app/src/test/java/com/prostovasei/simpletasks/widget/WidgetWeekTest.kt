package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Вид «Неделя» в виджете (итерация 45): четыре строки одинаковой высоты, плашки по высоте и «+N», нажатия, мини-календарь,
 * перекраска строк другой неделей и выбор недели и месяца в виджете.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class WidgetWeekTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val size = DpSize(360.dp, 700.dp)
    private val monday = LocalDate.of(2026, 10, 26)
    private val today = LocalDate.of(2026, 10, 30)

    private fun item(id: String, title: String, time: String? = null, priority: Priority = Priority.NONE, done: Boolean = false, ghostOf: String? = null) =
        TaskItem(id, title, priority, DueText(DayLabel.Today, time), "Работа", "#6B5CE7", isInboxList = false, isDone = done, ghostOf = ghostOf)

    private fun week(start: LocalDate = monday, month: YearMonth = YearMonth.of(2026, 10), items: (Int) -> List<TaskItem>) =
        WidgetModel(
            WidgetFilter.Week, null, today, emptyList(),
            week = WidgetWeek(start, month, (0L..6L).map { WeekDay(start.plusDays(it), items(it.toInt())) }, today),
        )

    private val rich = week { day ->
        when (day) {
            0 -> listOf(item("a1", "Купить продукты", "10:00", Priority.LOW, done = true), item("a2", "Тренировка"))
            4 -> (1..6).map { item("e$it", "Задача $it", if (it == 1) "17:00" else null, Priority.HIGH) }
            5 -> listOf(item("f1", "Уборка", "11:00"), item("f2@0", "Созвон", "10:00", ghostOf = "f2"))
            else -> emptyList()
        }
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }

    private fun list(model: WidgetModel = rich): ListView =
        renderWidget(context, model, size).applyLikeLauncher(context).descendants().filterIsInstance<ListView>().single()

    private fun ListView.rows(): List<View> = (0 until adapter.count).map { adapter.getView(it, null, this) }

    private fun View.visibleTexts() = descendants().filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.map { it.text.toString() }.toList()

    @Test
    fun filterWeekRoundTrips() {
        assertEquals("week", WidgetFilter.Week.encode())
        assertEquals(WidgetFilter.Week, WidgetFilter.decode("week"))
        assertEquals(WidgetFilter.All, WidgetFilter.decode(null))
    }

    @Test
    fun fourRowsOfEqualHeightFillTheWidgetAboveTheBar() {
        val rows = list().rows()
        assertEquals(4, rows.size)
        val heights = rows.map { it.findViewById<View>(R.id.week_row).layoutParams.height }
        assertEquals("высоты одинаковые", 1, heights.distinct().size)
        val expectedPx = WidgetWeekRows.rowHeightDp(size) * context.resources.displayMetrics.density
        assertEquals(expectedPx, heights.first().toFloat(), 1f)
        // Четыре строки и нижняя панель помещаются в высоту виджета.
        val total = WidgetWeekRows.rowHeightDp(size) * 4 + SizeTokens.WIDGET_EDGE + SizeTokens.WIDGET_BAR
        assertTrue(total <= size.height.value + 1)
    }

    @Test
    fun tasksFitByHeightAndTheRestIsPlusN() {
        val capacity = WidgetWeekRows.capacity(WidgetWeekRows.rowHeightDp(size))
        assertTrue("в ячейку помещается больше одной плашки", capacity in 2 until WeekIds.SLOTS)
        val friday = list().rows()[2]
        val shown = WeekIds.LEFT.slots.count { friday.findViewById<View>(it.plaque).visibility == View.VISIBLE }
        assertEquals(capacity, shown)
        val more = friday.findViewById<TextView>(WeekIds.LEFT.more)
        assertEquals(View.VISIBLE, more.visibility)
        assertEquals("+${6 - capacity}", more.text.toString())
        // Субботние две задачи помещаются: «+N» нет.
        assertEquals(View.GONE, friday.findViewById<View>(WeekIds.RIGHT.more).visibility)
    }

    @Test
    fun dateRowAndBothZonesOfEveryTaskAreClickable() {
        val first = list().rows()[0]
        assertTrue("строка даты открывает день", first.findViewById<View>(WeekIds.LEFT.head).hasOnClickListeners())
        val slot = WeekIds.LEFT.slots[0]
        assertTrue("кружок", first.findViewById<View>(slot.check).hasOnClickListeners())
        assertTrue("плашка", first.findViewById<View>(slot.plaque).hasOnClickListeners())
        assertEquals("Снять отметку «выполнено»", first.findViewById<View>(slot.node).contentDescription.toString())
        assertEquals(View.VISIBLE, first.findViewById<View>(slot.tick).visibility)
        assertEquals("Выполнить", first.findViewById<View>(WeekIds.LEFT.slots[1].node).contentDescription.toString())
        // Будущий повтор: кружок ничего не отмечает и без подписи.
        val saturday = list().rows()[2]
        assertNull(saturday.findViewById<View>(WeekIds.RIGHT.slots[1].node).contentDescription)
    }

    @Test
    fun calendarHasSixClickableWeeksMonthAndArrows() {
        val last = list().rows()[3]
        assertTrue(last.visibleTexts().contains("октябрь 2026"))
        assertEquals(6, WeekIds.CAL_ROWS.count { last.findViewById<View>(it.row).hasOnClickListeners() })
        assertTrue(last.findViewById<View>(WeekIds.CAL_PREV).hasOnClickListeners())
        assertTrue(last.findViewById<View>(WeekIds.CAL_NEXT).hasOnClickListeners())
        // Октябрь 2026: первая строка — с 28 сентября, последняя — со 2 ноября.
        assertEquals("28", last.findViewById<TextView>(WeekIds.CAL_ROWS.first().days.first()).text.toString())
        assertEquals("8", last.findViewById<TextView>(WeekIds.CAL_ROWS.last().days.last()).text.toString())
        // Воскресенье 1 ноября — в левой ячейке последней строки.
        assertTrue(last.visibleTexts().contains("вс"))
    }

    /** Android перекрашивает уже показанные строки: строки пустой недели на месте строк богатой не наследуют ничего чужого. */
    @Test
    fun recycledRowsKeepNothingFromAnotherWeek() {
        val shown = list(rich).rows()
        val empty = week(monday.plusDays(7), YearMonth.of(2026, 11)) { emptyList() }
        val next = list(empty)
        shown.forEachIndexed { index, old ->
            val row = next.adapter.getView(index, old, next)
            assertTrue("строка $index перекрашена", row === old)
            listOf(WeekIds.LEFT, WeekIds.RIGHT).forEach { cell ->
                if (index < 3 || cell === WeekIds.LEFT) {
                    cell.slots.forEach { assertEquals(View.GONE, row.findViewById<View>(it.plaque).visibility) }
                    assertEquals(View.GONE, row.findViewById<View>(cell.more).visibility)
                }
            }
        }
        // Календарь показывает другой месяц.
        assertTrue(shown[3].visibleTexts().contains("ноябрь 2026"))
    }

    @Test
    fun weekTabIsInTheSwitcher() {
        val root = renderWidget(context, rich, size).applyLikeLauncher(context)
        val found = ArrayList<View>()
        root.findViewsWithText(found, "Неделя", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        assertEquals(1, found.size)
        assertTrue(generateSequence(found.single()) { it.parent as? View }.take(3).any { it.hasOnClickListeners() })
    }

    @Test
    fun smallWidgetKeepsTheMinimumRowHeight() {
        assertTrue(WidgetWeekRows.rowHeightDp(DpSize(360.dp, 200.dp)) >= SizeTokens.WIDGET_WEEK_ROW_MIN)
        assertEquals(SizeTokens.WIDGET_WEEK_ROW_DEFAULT.toFloat(), WidgetWeekRows.rowHeightDp(DpSize.Unspecified), 0f)
    }

    // ---- Выбор недели и месяца в виджете ----

    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    @Test
    fun weekPickAndMonthShiftAreSavedPerWidget() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        // Сначала дожидаемся первой отрисовки «системы»: она запоминает выбор по умолчанию и не должна затереть вкладку.
        WidgetRenderer.render(context, id, "тест")
        WidgetActionReceiver.handle(context, WidgetActions.switch(context, id, WidgetFilter.Week))
        assertEquals(WidgetLog.read(context), WidgetFilter.Week, WidgetSelection.get(context, id))
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: week"))

        // Нажатие на строку недели: понедельник той недели, куда попала любая дата.
        WidgetActionReceiver.handle(context, WidgetActions.pickWeek(context, id, "2026-11-04"))
        assertEquals(LocalDate.of(2026, 11, 2), WidgetSelection.weekPick(context, id).start)

        WidgetActionReceiver.handle(context, WidgetActions.shiftMonth(context, id, 1))
        val month = WidgetSelection.weekPick(context, id).month
        WidgetActionReceiver.handle(context, WidgetActions.shiftMonth(context, id, 1))
        assertEquals(month!!.plusMonths(1), WidgetSelection.weekPick(context, id).month)
        // Неделя при листании месяца не меняется.
        assertEquals(LocalDate.of(2026, 11, 2), WidgetSelection.weekPick(context, id).start)

        // Другой виджет не затронут; удаление виджета забывает выбор.
        assertEquals(WeekPick(), WidgetSelection.weekPick(context, id + 100))
        TasksWidgetReceiver().onDeleted(context, intArrayOf(id))
        assertEquals(WeekPick(), WidgetSelection.weekPick(context, id))
        assertFalse(WidgetLog.read(context).contains("ошибка"))
    }

    @Test
    fun eachWeekTapHasItsOwnAddress() {
        assertFalse(WidgetActions.pickWeek(context, 1, "2026-10-26").filterEquals(WidgetActions.pickWeek(context, 1, "2026-11-02")))
        assertFalse(WidgetActions.shiftMonth(context, 1, -1).filterEquals(WidgetActions.shiftMonth(context, 1, 1)))
        assertFalse(WidgetActions.shiftMonth(context, 1, 1).filterEquals(WidgetActions.shiftMonth(context, 2, 1)))
    }
}
