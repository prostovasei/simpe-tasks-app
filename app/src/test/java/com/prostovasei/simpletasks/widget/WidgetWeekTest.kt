package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
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
import org.junit.Assert.assertNotNull
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
 * Вид «Неделя» в виджете (итерация 45; #313 — «Всё листается»): четыре строки одинаковой высоты, у каждого дня свой список задач
 * с пустым местом, открывающим день, лента недель календаря, нажатия, перекраска и выбор недели в виджете.
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

    /** #313: корень недели статический — четыре строки по две ячейки, у дня свой список, в последней ячейке — лента календаря. */
    private fun root(model: WidgetModel = rich, reported: DpSize = size): View = renderWidget(context, model, reported).applyLikeLauncher(context)

    private fun View.rows(): List<View> = findViewById<ViewGroup>(R.id.week_rows).let { box -> (0 until box.childCount).map { box.getChildAt(it) } }

    /** Строки списка — как их показывает рабочий стол (адаптер набора строк). */
    private fun View.items(list: Int): List<View> = findViewById<ListView>(list).let { view -> (0 until view.adapter.count).map { view.adapter.getView(it, null, view) } }

    private fun View.day(index: Int): List<View> = items(WeekIds.DAYS[index].list)

    /** Добавка нажатия строки списка (код и задача/день из адреса) — её рабочий стол дописывает к шаблону. */
    private val fillInTag = android.content.res.Resources.getSystem().getIdentifier("fillInIntent", "id", "android")

    private fun View.fillIn(): Pair<String, String?>? {
        // Android хранит у строки списка ответ на нажатие (RemoteResponse), внутри — добавку к шаблону.
        val response = getTag(fillInTag) ?: return null
        val intent = response.javaClass.getDeclaredField("mFillIntent").apply { isAccessible = true }.get(response) as? android.content.Intent
        return intent?.let { WidgetClicks.parse(it.data) }
    }

    /** Нажатие на элемент внутри строки списка: какое окно откроется (код и день из адреса). */
    private fun View.clickCode(): Pair<String, String?>? {
        assertTrue(hasOnClickListeners())
        performClick()
        return org.robolectric.Shadows.shadowOf(this@WidgetWeekTest.context).nextStartedActivity?.let { WidgetClicks.parse(it.data) }
    }

    /** Строку списка рабочий стол оборачивает — добавка нажатия на корне строки из разметки ([id]). */
    private fun View.fillIn(id: Int): Pair<String, String?>? = findViewById<View>(id)?.fillIn()

    private fun px(dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()

    private fun View.layoutAs(widthDp: Float, heightDp: Float): View = apply {
        measure(View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(px(heightDp), View.MeasureSpec.EXACTLY))
        layout(0, 0, px(widthDp), px(heightDp))
    }

    @Test
    fun filterWeekRoundTrips() {
        assertEquals("week", WidgetFilter.Week.encode())
        assertEquals(WidgetFilter.Week, WidgetFilter.decode("week"))
        assertEquals(WidgetFilter.All, WidgetFilter.decode(null))
    }

    /**
     * Проверка PM (0.46.0, Samsung): рабочий стол сообщил высоту больше настоящей, и четвёртая строка уезжала под нижнюю панель.
     * Строки делят настоящую высоту поровну: при сообщённых 700 dp и настоящих 640 dp все четыре одинаковые и над панелью.
     * #313: список дня — под строкой даты до низа ячейки.
     */
    @Test
    fun fourEqualRowsFitTheRealHeightEvenWhenTheLauncherReportsMore() {
        val root = root(rich, DpSize(360.dp, 700.dp)).layoutAs(360f, 640f)
        val rows = root.rows()
        assertEquals(4, rows.size)
        val heights = rows.map { it.height }
        assertTrue("высоты одинаковые: $heights", heights.max() - heights.min() <= 1)
        val barTop = px(640f) - px(SizeTokens.WIDGET_BAR.toFloat())
        val box = root.findViewById<View>(R.id.week_rows)
        assertTrue("последняя строка над панелью: ${box.top + rows.last().bottom} ≤ $barTop", box.top + rows.last().bottom <= barTop + 1)
        WeekIds.DAYS.forEach { ids ->
            val cell = root.findViewById<View>(ids.root)
            val head = root.findViewById<View>(ids.head)
            val list = root.findViewById<View>(ids.list)
            assertEquals("список сразу под датой", head.bottom, list.top)
            assertEquals("список до низа ячейки", cell.height - cell.paddingBottom, list.bottom)
        }
        val calendar = root.findViewById<View>(WeekIds.CALENDAR)
        assertTrue("лента календаря в последней ячейке", calendar.height > 0)
    }

    /** #313: все задачи дня в его списке (без «+N»), под ними — пустое место, которое открывает день. */
    @Test
    fun everyTaskIsInTheDayListAndTheEmptyPlaceOpensTheDay() {
        val root = root()
        val friday = root.day(4)
        assertEquals(9, friday.size)
        assertEquals((1..8).map { "Задача $it" }, friday.take(8).map { it.findViewById<TextView>(R.id.wi_title).text.toString() })
        val filler = friday.last()
        assertNotNull(filler.findViewById<View>(R.id.wi_filler))
        assertEquals(WidgetClicks.DAY to "2026-10-30", filler.fillIn(R.id.wi_filler))
        assertEquals(listOf("Купить продукты", "Тренировка"), root.day(0).dropLast(1).map { it.findViewById<TextView>(R.id.wi_title).text.toString() })
        assertEquals("пустой день — только место, которое открывает день", 1, root.day(1).size)
        assertTrue(root.descendants().filterIsInstance<TextView>().none { it.text.toString().startsWith("+") })
    }

    @Test
    fun emptyPlaceFillsTheCellButNeverDisappears() {
        val row = WidgetWeekRows.rowHeightDp(size)
        val fit = row - SizeTokens.WIDGET_WEEK_CELL_TOP - SizeTokens.WIDGET_WEEK_HEADER - SizeTokens.WIDGET_WEEK_FILL_SLACK
        assertEquals(fit.toInt(), WidgetWeekRows.fillerDp(row, 0))
        assertEquals((fit - 2 * SizeTokens.WIDGET_WEEK_ITEM).toInt(), WidgetWeekRows.fillerDp(row, 2))
        assertEquals(SizeTokens.WIDGET_WEEK_ITEM, WidgetWeekRows.fillerDp(row, 20))
    }

    @Test
    fun allWeekClicksOnlyOpenTheDayIncludingEmptyCellAndFutureRepeat() {
        val calls = mutableListOf<Triple<Int, String, String?>>()
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_root_week)
        val template = WidgetClicks.pending(context, 1, WidgetClicks.NONE, null)
        WidgetWeekRows.fill(views, context, rich.week!!, size, WidgetWeekRows.Click { view, code, id -> calls += Triple(view, code, id) }, template)
        val heads = WeekIds.DAYS.flatMap { listOf(it.root, it.head) }
        assertEquals(heads.toSet(), calls.map { it.first }.toSet())
        assertTrue(calls.all { it.second == WidgetClicks.DAY })
        val root = root()
        assertTrue(root.findViewById<View>(WeekIds.DAYS[0].root).hasOnClickListeners())
        // Задачи (и будущий повтор субботы) открывают свой день — ни выполнения, ни задачи.
        (0..6).forEach { day ->
            root.day(day).forEach { row ->
                val expected = WidgetClicks.DAY to monday.plusDays(day.toLong()).toString()
                assertEquals(expected, row.fillIn(R.id.wi_row) ?: row.fillIn(R.id.wi_filler))
            }
        }
    }

    /** #338: даты в капсуле больше нет (выбор PM). */
    @Test
    fun barHasNoDate() {
        listOf(400, 320).forEach { width ->
            val root = renderWidget(context, rich, DpSize(width.dp, 700.dp)).applyLikeLauncher(context)
            val found = ArrayList<View>()
            root.findViewsWithText(found, context.getString(R.string.widget_today_date, "пт, 30 окт"), View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(0, found.size)
        }
        assertEquals(Triple("7", "СБ", "ноя"), WidgetDates.tile(LocalDate.of(2026, 11, 7)))
    }

    /** #313: лента календаря — с недели за две до текущей, на полгода вперёд; неделя — в месяце своего воскресенья. */
    @Test
    fun calendarStripStartsTwoWeeksBackAndNamesMonths() {
        val lines = WidgetWeekRows.calendarLines(today, monday)
        assertEquals(WidgetWeekRows.CalLine.Month(YearMonth.of(2026, 10)), lines[0])
        assertEquals(WidgetWeekRows.CalLine.Week(LocalDate.of(2026, 10, 12)), lines[1])
        assertEquals(WidgetWeekRows.CalLine.Week(LocalDate.of(2026, 10, 19)), lines[2])
        // Неделя 26 октября — 1 ноября относится к ноябрю.
        assertEquals(WidgetWeekRows.CalLine.Month(YearMonth.of(2026, 11)), lines[3])
        assertEquals(WidgetWeekRows.CalLine.Week(monday), lines[4])
        val weeks = lines.filterIsInstance<WidgetWeekRows.CalLine.Week>()
        assertEquals("до конца апреля 2027", LocalDate.of(2027, 4, 26), weeks.last().start)
        assertEquals(weeks.zipWithNext().all { (a, b) -> b.start == a.start.plusWeeks(1) }, true)
        assertEquals(lines.filterIsInstance<WidgetWeekRows.CalLine.Month>().map { it.month }.distinct().size, lines.count { it is WidgetWeekRows.CalLine.Month })
        assertEquals("ноябрь", WidgetWeekRows.monthTitle(YearMonth.of(2026, 11), today))
        assertEquals("январь 2027", WidgetWeekRows.monthTitle(YearMonth.of(2027, 1), today))
        // Выбранная раньше неделя остаётся в ленте.
        val early = LocalDate.of(2026, 9, 7)
        assertEquals(WidgetWeekRows.CalLine.Week(early), WidgetWeekRows.calendarLines(today, early)[1])
    }

    private fun TextView.isBold() = (text as? Spanned)?.getSpans(0, text.length, StyleSpan::class.java)
        ?.any { it.style == Typeface.BOLD } == true

    /** Выбранная неделя — с полосой, её числа открывают свой день; остальные недели выбираются нажатием; сегодня — жирное. */
    @Test
    fun calendarSelectedWeekOpensDaysOthersPickTheWeek() {
        val rows = root().items(WeekIds.CALENDAR)
        val lines = WidgetWeekRows.calendarLines(today, monday)
        assertEquals(lines.size, rows.size)
        assertEquals("октябрь", rows[0].findViewById<TextView>(R.id.wcm_title).text.toString())
        val selected = rows[4]
        assertNotNull(selected.findViewById<View>(R.id.wcw_row).background)
        assertEquals(listOf("26", "27", "28", "29", "30", "31", "1"), WeekIds.CAL_DAYS.map { selected.findViewById<TextView>(it).text.toString() })
        // Числа внутри строки: Android ставит им обработчик, который дописывает добавку к шаблону списка — нажимаем в настоящем списке.
        val shown = root().layoutAs(360f, 700f).findViewById<ListView>(WeekIds.CALENDAR)
        val row = (0 until shown.childCount).map { shown.getChildAt(it) }.single { it.findViewById<View>(R.id.wcw_row)?.background != null }
        assertEquals((0L..6L).map { WidgetClicks.DAY to monday.plusDays(it).toString() }, WeekIds.CAL_DAYS.map { row.findViewById<View>(it).clickCode() })
        val pick = (0 until shown.childCount).map { shown.getChildAt(it) }.first { it.findViewById<View>(R.id.wcw_row)?.let { r -> r.background == null } == true }
        assertEquals(WidgetClicks.PICK_WEEK to "2026-10-12", pick.findViewById<View>(WeekIds.CAL_DAYS[3]).clickCode())
        assertEquals(listOf("30"), WeekIds.CAL_DAYS.map { selected.findViewById<TextView>(it) }.filter { it.isBold() }.map { it.text.toString() })
        // Числа октября в ноябрьской неделе — серые, 1 ноября — основного цвета.
        val oct = selected.findViewById<TextView>(WeekIds.CAL_DAYS[0]).currentTextColor
        val nov = selected.findViewById<TextView>(WeekIds.CAL_DAYS[6]).currentTextColor
        assertTrue(oct != nov)
        val other = rows[1]
        assertNull(other.findViewById<View>(R.id.wcw_row).background)
        assertEquals(WidgetClicks.PICK_WEEK to "2026-10-12", other.fillIn(R.id.wcw_row))
        assertNull("название месяца не нажимается", rows[0].fillIn(R.id.wcm_title))
    }

    @Test
    @Config(fontScale = 1.3f)
    fun calendarDatesFitAtLargerFont() {
        for (width in listOf(320f, 392f)) {
            val root = root(rich, DpSize(width.dp, 640.dp)).layoutAs(width, 640f)
            val list = root.findViewById<ListView>(WeekIds.CALENDAR)
            val row = list.adapter.getView(4, null, list)
            row.measure(View.MeasureSpec.makeMeasureSpec(list.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            row.layout(0, 0, row.measuredWidth, row.measuredHeight)
            val dates = WeekIds.CAL_DAYS.map { row.findViewById<TextView>(it) }
            assertTrue("Даты помещаются при ширине $width dp", dates.all { date ->
                date.lineCount == 1 && date.layout.getLineEnd(0) == date.text.length && date.layout.height <= date.height
            })
        }
    }

    /**
     * Проверка PM (0.46.0): рабочий стол при обновлении применяет виджет поверх показанного (reapply) — строки недели не должны
     * добавляться к прежним (было: неделя трижды), нижняя панель — тоже одна. #313: списки — те же, не пересоздаются.
     */
    @Test
    fun reapplyingTheWidgetKeepsFourRowsTheSameListsAndOneBar() {
        val root = root()
        val lists = WeekIds.DAYS.map { root.findViewById<View>(it.list) }
        repeat(2) { renderWidget(context, rich, size).reapply(context, root) }
        assertEquals(4, root.rows().size)
        assertEquals(lists, WeekIds.DAYS.map { root.findViewById<View>(it.list) })
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

    /** Много задач на неделе — пакет виджета остаётся небольшим (предел передачи рабочему столу ~1 МБ). */
    @Test
    fun busyWeekKeepsTheParcelSmall() {
        val busy = week { day -> (1..15).map { item("b$day-$it", "Задача с длинным названием $it", "1$it:00") } }
        val kb = renderWidget(context, busy, size).parcelBytes() / 1024
        assertTrue("пакет $kb КБ", kb < 400)
    }

    /** Проверка PM (0.46.0): сегодня — без кружка, день недели и число жирные; у остальных дней жирного нет. */
    @Test
    fun todayIsBoldWithoutACircle() {
        val root = root()
        val friday = WeekIds.DAYS[4] // пятница 30 октября — сегодня
        val number = root.findViewById<TextView>(friday.number)
        assertTrue(root.findViewById<TextView>(friday.weekday).isBold())
        assertTrue(number.isBold())
        assertEquals("30 Сегодня", number.text.toString())
        assertNull("без кружка", number.background)
        val saturday = WeekIds.DAYS[5]
        assertFalse(root.findViewById<TextView>(saturday.weekday).isBold())
        assertFalse(root.findViewById<TextView>(saturday.number).isBold())
        assertEquals("31", root.findViewById<TextView>(saturday.number).text.toString())
    }

    @Test
    fun todayLabelMovesAfterReapply() {
        val root = root()
        assertEquals("30 Сегодня", root.findViewById<TextView>(WeekIds.DAYS[4].number).text.toString())
        val next = rich.copy(week = rich.week!!.copy(today = today.plusDays(1)))
        renderWidget(context, next, size).reapply(context, root)
        assertEquals("30", root.findViewById<TextView>(WeekIds.DAYS[4].number).text.toString())
        assertEquals("31 Сегодня", root.findViewById<TextView>(WeekIds.DAYS[5].number).text.toString())
    }

    /**
     * Итерация 48 (решение PM, вариант В): эмодзи списка у каждой задачи — и у «Входящих»; эмодзи в названии остаётся
     * (дубль не убирается); день недели капсом. #263: эмодзи → название → начало справа.
     */
    @Test
    fun emojiTitleAndTimeInOrderWithUppercaseWeekday() {
        val inbox = TaskItem("i1", "📥Разобрать", Priority.NONE, DueText(DayLabel.Today, "09:00"), "Входящие", "📥", isInboxList = true)
        val root = root(week { day -> if (day == 0) listOf(item("w1", "Синк", "11:00"), inbox) else emptyList() })
        val monday = root.day(0)
        assertEquals("💼", monday[0].findViewById<TextView>(R.id.wi_emoji).text.toString())
        assertEquals("📥", monday[1].findViewById<TextView>(R.id.wi_emoji).text.toString())
        assertEquals("📥Разобрать", monday[1].findViewById<TextView>(R.id.wi_title).text.toString())
        val line = monday[0].findViewById<ViewGroup>(R.id.wi_plaque)
        val order = (0 until line.childCount).map { line.getChildAt(it).id }
        assertEquals(listOf(R.id.wi_emoji, R.id.wi_title, R.id.wi_time), order)
        assertEquals("ПН", root.findViewById<TextView>(WeekIds.DAYS[0].weekday).text.toString())
        assertEquals("ВТ", root.findViewById<TextView>(WeekIds.DAYS[1].weekday).text.toString())
    }

    /** У каждого виджета свои намерения нажатий: иначе выбор недели одного виджета менял бы другой. */
    @Test
    fun clicksOfTwoWidgetsAreDifferentIntents() {
        val one = WidgetClicks.pending(context, 1, WidgetClicks.PICK_WEEK, "2026-10-26")
        val two = WidgetClicks.pending(context, 2, WidgetClicks.PICK_WEEK, "2026-10-26")
        assertFalse(one == two)
        assertEquals(one, WidgetClicks.pending(context, 1, WidgetClicks.PICK_WEEK, "2026-10-26"))
    }

    /** #336: вместо вкладок — «Разделы» (окно выбора) и «Настройки», оба нажимаются. */
    @Test
    fun sectionsAndSettingsAreClickable() {
        val root = renderWidget(context, rich, size).applyLikeLauncher(context)
        listOf("Разделы", "Настройки").forEach { label ->
            val found = ArrayList<View>()
            root.findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            assertEquals(label, 1, found.size)
            assertTrue(generateSequence(found.single()) { it.parent as? View }.take(3).any { it.hasOnClickListeners() })
        }
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
