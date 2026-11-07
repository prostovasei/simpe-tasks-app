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
        id, title, Priority.HIGH, DueText(day, time), "Работа", "💼", isInboxList = false,
        isRecurring = ghostOf != null, ghostOf = ghostOf, reminder = ReminderMark.EARLY, isDone = done,
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

    /** Итерация 51: дата в нижней строке ведёт к подписи «Сегодня» (просроченный вчерашний день остаётся выше); без дней — к началу. */
    @Test
    fun todayLineIsTheTodayTitle() {
        val lines = model.lines(dayNames(context))
        val index = model.todayLine(lines)
        assertEquals(WidgetDay.On(today), (lines[index] as WidgetLine.Title).day)
        assertTrue("Вчерашний день выше", index > 0)
        val future = model.copy(days = model.days.drop(2))
        assertEquals("Сегодня пусто — первый следующий день", 0, future.todayLine(future.lines(dayNames(context))))
        assertEquals(0, model.todayLine(emptyList()))
        // Нажатие на дату — своё действие виджету, со своим адресом.
        val intent = WidgetActions.today(context, 7)
        assertEquals(WidgetActions.TODAY, intent.action)
        assertEquals(7, intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1))
    }

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
        // У будущего повтора кружок ничего не отмечает (пустое нажатие) и без подписи «Выполнить».
        val labels = rows.map { it.findViewById<View>(R.id.row_node).contentDescription?.toString() }
        assertEquals(listOf("Выполнить", "Снять отметку «выполнено»", "Выполнить", null), labels)
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

    /**
     * Android перекрашивает уже показанные строки (прокрутка, обновление): строка «простой» задачи на месте задачи
     * с повтором, напоминанием и меткой «Сегодня» не должна унаследовать ничего чужого (0.42.2, баг на телефоне PM).
     */
    @Test
    fun recycledRowsKeepNothingFromPreviousTask() {
        fun task(id: String, rich: Boolean, day: LocalDate) = TaskItem(
            id, "Задача $id", if (rich) Priority.HIGH else Priority.NONE, DueText(DayLabel.Date(day, false), if (rich) "10:00" else null),
            "Работа", "💼", isInboxList = false, isRecurring = rich, reminder = if (rich) ReminderMark.EARLY else null,
        )
        val rich = model.copy(days = listOf(WidgetDayBlock(WidgetDay.On(today), listOf(task("a", true, today), task("b", true, today)))), nowMinute = null)
        val later = today.plusDays(3)
        val plain = model.copy(days = listOf(WidgetDayBlock(WidgetDay.On(later), listOf(task("c", false, later), task("d", false, later)))), nowMinute = null)
        val shown = list(rich).rows()
        val next = list(plain)
        val rows = shown.mapIndexed { i, old -> next.adapter.getView(i, old, next).also { assertTrue("строка $i перекрашена", it === old) } }

        val title = rows.first { it.findViewById<View>(R.id.title_root) != null }
        listOf(R.id.title_chip, R.id.title_progress, R.id.title_progress_text).forEach {
            assertEquals(title.resources.getResourceEntryName(it), View.GONE, title.findViewById<View>(it).visibility)
        }
        rows.filter { it.findViewById<View>(R.id.row_plaque) != null }.forEach { row ->
            listOf(R.id.row_start, R.id.row_bell).forEach {
                assertEquals(row.resources.getResourceEntryName(it), View.GONE, row.findViewById<View>(it).visibility)
            }
        }
    }

    /** Баг на телефоне PM: «вернуть» выполненную — кружок снова пустой, без галочки, с подписью «Выполнить». */
    @Test
    fun uncompletedTaskLosesCheck() {
        fun day(done: Boolean) = model.copy(days = listOf(WidgetDayBlock(WidgetDay.On(today), listOf(item("x", "Xgg", DayLabel.Today, null, done = done)))))
        val doneRow = list(day(true)).rows().first { it.findViewById<View>(R.id.row_plaque) != null }
        assertEquals(View.VISIBLE, doneRow.findViewById<View>(R.id.row_node_check).visibility)
        val next = list(day(false))
        val position = (0 until next.adapter.count).first { next.adapter.getView(it, null, next).findViewById<View>(R.id.row_plaque) != null }
        val row = next.adapter.getView(position, doneRow, next)
        assertEquals(View.GONE, row.findViewById<View>(R.id.row_node_check).visibility)
        assertEquals("Выполнить", row.findViewById<View>(R.id.row_node).contentDescription)
        assertFalse(row.findViewById<TextView>(R.id.row_title).text is android.text.Spanned)
    }

    /** Число задач дня справа у подписи (итерация 43); у «Сегодня» вместо него прогресс. */
    @Test
    fun dayTitlesShowTaskCount() {
        val rows = list().rows()
        fun count(position: Int) = rows[position].findViewById<TextView>(R.id.title_count)
        val titles = rows.indices.filter { rows[it].findViewById<View>(R.id.title_root) != null }
        // Вчера: 1, сегодня: 2 (с прогрессом, без числа), завтра: 1.
        assertEquals(listOf("1", "2", "1"), titles.map { count(it).text.toString() })
        assertEquals(listOf(View.VISIBLE, View.GONE, View.VISIBLE), titles.map { count(it).visibility })
        assertEquals(View.VISIBLE, rows[titles[1]].findViewById<View>(R.id.title_progress_text).visibility)
    }

    /** Нижняя панель Glance лежит поверх ленты и тоже доходит до рабочего стола. */
    @Test
    fun bottomBarOverList() {
        val root = renderWidget(context, model).applyLikeLauncher(context)
        // «Открыть приложение» — пустое место и капсула с галочкой (итерация 43): два нажимаемых элемента.
        mapOf("Все" to 1, "Входящие" to 1, "Открыть приложение" to 2, "Новая задача" to 1).forEach { (label, count) ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, count, found.size)
            // Подпись бывает у значка, нажатие — у кнопки вокруг него.
            found.forEach { view ->
                val clickable = generateSequence(view) { it.parent as? View }.take(3).any { it.hasOnClickListeners() }
                assertTrue("$label нажимается", clickable)
            }
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
