package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.view.View
import android.view.ViewGroup
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
        TaskItem(id, title, priority, DueText(DayLabel.Today, time), "Работа", "💼", isInboxList = false, isDone = done, ghostOf = ghostOf)

    private fun week(start: LocalDate = monday, month: YearMonth = YearMonth.of(2026, 10), items: (Int) -> List<TaskItem>) =
        WidgetModel(
            WidgetFilter.Week, null, today, emptyList(),
            week = WidgetWeek(start, month, (0L..6L).map { WeekDay(start.plusDays(it), items(it.toInt())) }, today),
        )

    private val rich = week { day ->
        when (day) {
            0 -> listOf(item("a1", "Купить продукты", "10:00", Priority.NONE, done = true), item("a2", "Тренировка"))
            4 -> (1..8).map { item("e$it", "Задача $it", if (it == 1) "17:00" else null, Priority.HIGH) }
            5 -> listOf(item("f1", "Уборка", "11:00"), item("f2@0", "Созвон", "10:00", ghostOf = "f2"))
            else -> emptyList()
        }
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }

    /** 0.46.0: строки недели — не лента, а дети `week_rows` (делят высоту виджета поровну). */
    private fun list(model: WidgetModel = rich, reported: DpSize = size): ViewGroup =
        renderWidget(context, model, reported).applyLikeLauncher(context).findViewById(R.id.week_rows)

    private fun ViewGroup.rows(): List<View> = (0 until childCount).map { getChildAt(it) }

    private fun px(dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()

    private fun View.visibleTexts() = descendants().filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.map { it.text.toString() }.toList()

    @Test
    fun filterWeekRoundTrips() {
        assertEquals("week", WidgetFilter.Week.encode())
        assertEquals(WidgetFilter.Week, WidgetFilter.decode("week"))
        assertEquals(WidgetFilter.All, WidgetFilter.decode(null))
    }

    /**
     * Проверка PM (0.46.0, Samsung): рабочий стол сообщил высоту больше настоящей, и четвёртая строка уезжала под нижнюю панель.
     * Строки делят настоящую высоту поровну: при сообщённых 700 dp и настоящих 640 dp все четыре одинаковые и над панелью.
     */
    @Test
    fun fourEqualRowsFitTheRealHeightEvenWhenTheLauncherReportsMore() {
        val root = renderWidget(context, rich, DpSize(360.dp, 700.dp)).applyLikeLauncher(context)
        val width = px(360f)
        val height = px(640f)
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        val rows = root.findViewById<ViewGroup>(R.id.week_rows).let { box -> (0 until box.childCount).map { box.getChildAt(it) } }
        assertEquals(4, rows.size)
        val heights = rows.map { it.height }
        assertTrue("высоты одинаковые: $heights", heights.max() - heights.min() <= 1)
        val barTop = height - px(SizeTokens.WIDGET_BAR.toFloat())
        val box = root.findViewById<View>(R.id.week_rows)
        assertTrue("последняя строка над панелью: ${box.top + rows.last().bottom} ≤ $barTop", box.top + rows.last().bottom <= barTop + 1)
        // Строки задач во всех ячейках одной высоты и целиком внутри ячейки.
        val friday = rows[2]
        val slots = WeekIds.LEFT.slots.map { friday.findViewById<View>(it.plaque) }.filter { it.visibility == View.VISIBLE }
        val slotHeights = slots.map { it.height }
        assertTrue("строки задач одной высоты (до пикселя округления): $slotHeights", slotHeights.max() - slotHeights.min() <= 1)
        assertTrue("строки задач внутри ячейки", slots.last().bottom <= friday.findViewById<View>(R.id.wl_root).height)
    }

    @Test
    fun tasksFitByHeightAndTheRestIsPlusN() {
        val capacity = WidgetWeekRows.capacity(WidgetWeekRows.rowHeightDp(size))
        // 0.46.0: компактные строки — на виджете высотой 700 dp в ячейку помещается 6 задач (решение PM).
        assertEquals(6, capacity)
        val friday = list().rows()[2]
        val shown = WeekIds.LEFT.slots.count { friday.findViewById<View>(it.plaque).visibility == View.VISIBLE }
        assertEquals(capacity, shown)
        val more = friday.findViewById<TextView>(WeekIds.LEFT.more)
        assertEquals(View.VISIBLE, more.visibility)
        assertEquals("+${8 - capacity}", more.text.toString())
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
    fun calendarHasSixClickableWeeksWithoutMonthRow() {
        val last = list().rows()[3]
        // 0.46.0: строки «октябрь 2026» со стрелками нет — месяц листается выбором недели.
        assertFalse(last.visibleTexts().contains("октябрь 2026"))
        assertEquals(6, WeekIds.CAL_ROWS.count { last.findViewById<View>(it.row).hasOnClickListeners() })
        // Октябрь 2026: первая строка — с 28 сентября, последняя — со 2 ноября.
        assertEquals("28", last.findViewById<TextView>(WeekIds.CAL_ROWS.first().days.first()).text.toString())
        assertEquals("8", last.findViewById<TextView>(WeekIds.CAL_ROWS.last().days.last()).text.toString())
        // Воскресенье 1 ноября — в левой ячейке последней строки.
        assertTrue(last.visibleTexts().contains("вс"))
    }

    /**
     * Проверка PM (0.46.0): рабочий стол при обновлении применяет виджет поверх показанного (reapply) — строки недели не должны
     * добавляться к прежним (было: неделя трижды), нижняя панель — тоже одна.
     */
    @Test
    fun reapplyingTheWidgetKeepsFourRowsAndOneBar() {
        val first = renderWidget(context, rich, size)
        val root = first.applyLikeLauncher(context)
        repeat(2) { renderWidget(context, rich, size).reapply(context, root) }
        assertEquals(4, root.findViewById<ViewGroup>(R.id.week_rows).childCount)
        assertEquals(1, root.findViewById<ViewGroup>(R.id.widget_overlay).childCount)
    }

    /** Лента «Все» после повторного применения: одна нижняя панель. */
    @Test
    fun reapplyingTheListKeepsOneBar() {
        val all = WidgetModel(WidgetFilter.All, null, today, emptyList())
        val root = renderWidget(context, all, size).applyLikeLauncher(context)
        repeat(2) { renderWidget(context, all, size).reapply(context, root) }
        assertEquals(1, root.findViewById<ViewGroup>(R.id.widget_overlay).childCount)
    }

    /** Решение PM — 6 задач в ячейке: на виджете во весь экран телефона (Samsung PM: ~640–680 dp) влезает 6, а не 5. */
    @Test
    fun sixTasksFitOnAFullScreenWidget() {
        listOf(640, 680, 700).forEach { h ->
            assertEquals("высота $h dp", 6, WidgetWeekRows.capacity(WidgetWeekRows.rowHeightDp(DpSize(360.dp, h.dp))))
        }
    }

    /** Проверка PM (0.46.0): сегодня — без кружка, день недели и число жирные; у остальных дней жирного нет. */
    @Test
    fun todayIsBoldWithoutACircle() {
        fun TextView.isBold() = (text as? android.text.Spanned)?.getSpans(0, text.length, android.text.style.StyleSpan::class.java)
            ?.any { it.style == android.graphics.Typeface.BOLD } == true
        val friday = list().rows()[2] // пятница 30 октября — сегодня
        val number = friday.findViewById<TextView>(WeekIds.LEFT.number)
        assertTrue(friday.findViewById<TextView>(WeekIds.LEFT.weekday).isBold())
        assertTrue(number.isBold())
        assertNull("без кружка", number.background)
        val saturday = friday.findViewById<TextView>(WeekIds.RIGHT.weekday)
        assertFalse(saturday.isBold())
        assertFalse(friday.findViewById<TextView>(WeekIds.RIGHT.number).isBold())
    }

    /** Пустые места под задачи невидимы, но держат размер (строки во всех ячейках одной высоты); лишние места убраны. */
    @Test
    fun emptySlotsKeepTheirShareAndExtraSlotsAreGone() {
        val small = DpSize(360.dp, 500.dp)
        val capacity = WidgetWeekRows.capacity(WidgetWeekRows.rowHeightDp(small))
        assertTrue("capacity $capacity", capacity in 1 until WeekIds.SLOTS)
        val monday = list(rich, small).rows()[0]
        WeekIds.LEFT.slots.forEachIndexed { index, slot ->
            val expected = when {
                index < 2 -> View.VISIBLE
                index < capacity -> View.INVISIBLE
                else -> View.GONE
            }
            assertEquals("место $index", expected, monday.findViewById<View>(slot.plaque).visibility)
        }
        // Календарь другого месяца: ноябрь 2026 начинается с недели 26 октября.
        val november = list(week(start = LocalDate.of(2026, 11, 2), month = YearMonth.of(2026, 11)) { emptyList() }).rows()[3]
        assertEquals("26", november.findViewById<TextView>(WeekIds.CAL_ROWS.first().days.first()).text.toString())
    }

    /** У каждого виджета свои намерения нажатий: иначе выбор недели одного виджета менял бы другой. */
    @Test
    fun clicksOfTwoWidgetsAreDifferentIntents() {
        val one = WidgetClicks.pending(context, 1, WidgetClicks.PICK_WEEK, "2026-10-26")
        val two = WidgetClicks.pending(context, 2, WidgetClicks.PICK_WEEK, "2026-10-26")
        assertFalse(one == two)
        assertEquals(one, WidgetClicks.pending(context, 1, WidgetClicks.PICK_WEEK, "2026-10-26"))
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
    fun weekPickAndItsMonthAreSavedPerWidget() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        // Сначала дожидаемся первой отрисовки «системы»: она запоминает выбор по умолчанию и не должна затереть вкладку.
        WidgetRenderer.render(context, id, "тест")
        WidgetActionReceiver.handle(context, WidgetActions.switch(context, id, WidgetFilter.Week))
        assertEquals(WidgetLog.read(context), WidgetFilter.Week, WidgetSelection.get(context, id))
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: week"))

        // Нажатие на строку недели: понедельник той недели, куда попала любая дата.
        WidgetActionReceiver.handle(context, WidgetActions.pickWeek(context, id, "2026-11-04"))
        assertEquals(LocalDate.of(2026, 11, 2), WidgetSelection.weekPick(context, id).start)

        // Неделя целиком из ноября — календарь на ноябре, каким бы ни был месяц сегодня.
        assertEquals(YearMonth.of(2026, 11), WidgetSelection.weekPick(context, id).month)
        // 0.46.0: стрелок нет — неделя с днями соседнего месяца перелистывает календарь на него (вперёд и назад).
        WidgetActionReceiver.handle(context, WidgetActions.pickWeek(context, id, "2026-11-30"))
        assertEquals(WeekPick(LocalDate.of(2026, 11, 30), YearMonth.of(2026, 12)), WidgetSelection.weekPick(context, id))
        WidgetActionReceiver.handle(context, WidgetActions.pickWeek(context, id, "2026-10-26"))
        assertEquals(WeekPick(LocalDate.of(2026, 10, 26), YearMonth.of(2026, 10)), WidgetSelection.weekPick(context, id))

        // Другой виджет не затронут; удаление виджета забывает выбор.
        assertEquals(WeekPick(), WidgetSelection.weekPick(context, id + 100))
        TasksWidgetReceiver().onDeleted(context, intArrayOf(id))
        assertEquals(WeekPick(), WidgetSelection.weekPick(context, id))
        assertFalse(WidgetLog.read(context).contains("ошибка"))
    }

    @Test
    fun eachWeekTapHasItsOwnAddress() {
        assertFalse(WidgetActions.pickWeek(context, 1, "2026-10-26").filterEquals(WidgetActions.pickWeek(context, 1, "2026-11-02")))
        assertFalse(WidgetActions.pickWeek(context, 1, "2026-10-26").filterEquals(WidgetActions.pickWeek(context, 2, "2026-10-26")))
    }
}
