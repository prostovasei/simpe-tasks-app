package com.prostovasei.simpletasks.widget

import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Лента на обычных элементах Android (итерация 42): все строки доходят до списка — после передачи рабочему столу и применения
 * в фоне, как у Nova; у задач — текст и обе зоны нажатия.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetListTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val today = LocalDate.of(2026, 10, 16)

    private fun item(id: String, title: String, day: DayLabel, time: String?, done: Boolean = false, ghostOf: String? = null) = TaskItem(
        id, title, Priority.HIGH, DueText(day, time), "Работа", "#6B5CE7", isInboxList = false,
        isRecurring = ghostOf != null, ghostOf = ghostOf, listIcon = "work", reminder = ReminderMark.EARLY, isDone = done,
    )

    private val model = WidgetModel(
        WidgetFilter.All,
        null,
        today,
        listOf(
            WidgetDayBlock(WidgetDay.On(today.minusDays(1)), listOf(item("late", "Отчёт", DayLabel.Yesterday, null))),
            WidgetDayBlock(
                WidgetDay.On(today),
                listOf(
                    item("gym", "Зарядка", DayLabel.Today, "08:00", done = true),
                    item("call", "Созвон", DayLabel.Today, "10:00–11:00"),
                ),
            ),
            WidgetDayBlock(WidgetDay.On(today.plusDays(1)), listOf(item("gym@1", "Зарядка завтра", DayLabel.Tomorrow, "08:00", ghostOf = "gym"))),
        ),
        nowMinute = 9 * 60,
    )

    private fun list(shown: WidgetModel = model): ListView {
        val root = renderWidget(context, shown).applyLikeLauncher(context)
        return root.descendants().filterIsInstance<ListView>().single()
    }

    private fun ListView.rows(): List<View> = (0 until adapter.count).map { adapter.getView(it, null, this) }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) for (i in 0 until childCount) yieldAll(getChildAt(i).descendants())
    }

    private fun View.texts() = descendants().filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.map { it.text.toString() }.toList()

    @Test
    fun everyLineArrives() {
        val list = list()
        assertEquals(model.lines(dayNames(context)).size, list.adapter.count)
        val texts = list.rows().flatMap { it.texts() }
        listOf("Отчёт", "Зарядка", "Созвон", "Зарядка завтра", "10:00", "11:00", "1 из 2", "09:00").forEach {
            assertTrue("нет «$it» в $texts", it in texts)
        }
    }

    @Test
    fun tasksHaveBothClickZones() {
        val rows = list().rows().filter { it.findViewById<View>(R.id.row_plaque) != null }
        assertEquals(4, rows.size)
        rows.forEach { row ->
            assertTrue("плашка открывает задачу", row.findViewById<View>(R.id.row_plaque).hasOnClickListeners())
        }
        // У будущего повтора кружок не нажимается (как раньше), у остальных — отмечает задачу.
        val checks = rows.map { it.findViewById<View>(R.id.row_check).hasOnClickListeners() }
        assertEquals(listOf(true, true, true, false), checks)
    }

    @Test
    fun overdueTitleHasFoldAndMove() {
        val row = list().rows().first { it.findViewById<View>(R.id.overdue_toggle) != null }
        assertTrue(row.findViewById<View>(R.id.overdue_toggle).hasOnClickListeners())
        assertTrue(row.findViewById<View>(R.id.overdue_move).hasOnClickListeners())
    }

    @Test
    fun doneTaskIsStruckThrough() {
        val title = list().rows().map { it.findViewById<TextView?>(R.id.row_title) }.first { it?.text.toString() == "Зарядка" }!!
        assertTrue(title.paint.isStrikeThruText || title.text is android.text.Spanned)
        val open = list().rows().map { it.findViewById<TextView?>(R.id.row_title) }.first { it?.text.toString() == "Созвон" }!!
        assertFalse(open.text is android.text.Spanned)
    }

    /** Нижняя панель Glance лежит поверх ленты и тоже доходит до рабочего стола. */
    @Test
    fun bottomBarOverList() {
        val root = renderWidget(context, model).applyLikeLauncher(context)
        listOf("Все", "Входящие", "Открыть приложение", "Новая задача").forEach { label ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, 1, found.size)
            // Подпись бывает у значка, нажатие — у кнопки вокруг него.
            val clickable = generateSequence(found.single()) { it.parent as? View }.take(3).any { it.hasOnClickListeners() }
            assertTrue("$label нажимается", clickable)
        }
    }

    /** Не больше [MAX_LINES] строк и хвост под нижней панелью в конце. */
    @Test
    fun longListIsCappedWithTail() {
        val many = (1..200).map { item("t$it", "Задача $it", DayLabel.Today, null) }
        val big = model.copy(days = listOf(WidgetDayBlock(WidgetDay.On(today), many)))
        val list = list(big)
        assertEquals(MAX_LINES + 1, list.adapter.count)
        assertEquals(0, list.rows().last().texts().size)
    }
}
