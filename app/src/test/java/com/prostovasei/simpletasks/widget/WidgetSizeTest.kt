package com.prostovasei.simpletasks.widget

import android.os.Bundle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.ReminderMark
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Размер пакета виджета (0.41.1, итерация 42): вся лента уходит рабочему столу одним пакетом. На 100+ задачах PM пакет
 * строк Glance (~760 КБ) Nova Launcher не применял; строки на обычных элементах Android должны быть в разы легче.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetSizeTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val today = LocalDate.of(2026, 10, 30)

    private fun item(i: Int, date: LocalDate) = TaskItem(
        "t$i", "Задача номер $i с длинным названием", if (i % 4 == 0) Priority.HIGH else Priority.NONE,
        DueText(if (date == today) DayLabel.Today else DayLabel.Date(date, showYear = false), if (i % 2 == 0) "%02d:30".format(8 + i % 12) else null),
        "Другое", "#6B5CE7", isInboxList = false, isRecurring = i % 3 == 0, listIcon = "work",
        reminder = if (i % 2 == 0) ReminderMark.AT_DUE else if (i % 5 == 0) ReminderMark.EARLY else null, isDone = i % 7 == 0,
    )

    /** [count] задач на 14 дней (как показывает «Все») и 5 просроченных дней — как у PM после импорта из TickTick. */
    private fun model(count: Int): WidgetModel {
        val days = (-5 until WidgetModels.DAYS).map { today.plusDays(it.toLong()) }
        val blocks = (0 until count).map { item(it, days[it % days.size]) }
            .groupBy { it.due!!.day.let { d -> if (d is DayLabel.Date) d.date else today } }.toSortedMap()
            .map { (date, list) -> WidgetDayBlock(WidgetDay.On(date), list) }
        return WidgetModel(WidgetFilter.All, null, today, blocks, nowMinute = 9 * 60)
    }

    private fun bytes(model: WidgetModel): Int = runBlocking {
        GlanceRemoteViews().compose(context, DpSize(400.dp, 800.dp), appWidgetOptions = Bundle()) {
            WidgetContent(model, 1, scrollable = true)
        }.remoteViews.parcelBytes()
    }

    @Test
    fun hundredTasksAreLight() {
        val small = bytes(model(110))
        val huge = bytes(model(300))
        println("WIDGET SIZE: 110 задач — ${small / KB} КБ, 300 задач — ${huge / KB} КБ")
        // Строки Glance на 110 задачах весили ~760 КБ, обычные элементы — ~120 КБ.
        assertTrue("110 задач: ${small / KB} КБ", small < 150 * KB)
        // Больше [MAX_LINES] строк не уходит: пакет не растёт без предела.
        assertTrue("300 задач: ${huge / KB} КБ", huge < 180 * KB)
    }

    @Test
    fun linesAreCapped() {
        val lines = model(300).lines(dayNames(context))
        assertTrue("строк в модели ${lines.size}", lines.size > MAX_LINES)
    }

    private companion object {
        const val KB = 1024
    }
}
