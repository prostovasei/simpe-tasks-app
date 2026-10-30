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
 * Размер картинки виджета (0.41.1 → итерация 42). С классическим списком рабочему столу уходит только рамка (нижняя строка, фон),
 * а строки он запрашивает сам — поэтому рамка не растёт с числом задач. Было: вся лента одной картинкой, ~7 КБ на плашку,
 * 763 КБ на 110 задачах, и Nova Launcher её не применял.
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
        reminder = if (i % 2 == 0) ReminderMark.AT_DUE else null,
    )

    private fun model(count: Int): WidgetModel {
        val days = (-5..29).map { today.plusDays(it.toLong()) }
        val items = (0 until count).map { item(it, days[it % days.size]) }
        val blocks = items.groupBy { it.due!!.day.let { d -> if (d is DayLabel.Date) d.date else today } }.toSortedMap()
            .map { (date, list) -> WidgetDayBlock(WidgetDay.On(date), list) }
        return WidgetModel(WidgetFilter.All, null, today, blocks)
    }

    private fun frameBytes(model: WidgetModel): Int = runBlocking {
        GlanceRemoteViews().compose(context, DpSize(400.dp, 800.dp), appWidgetOptions = Bundle()) {
            WidgetContent(model, 1, scrollable = true)
        }.remoteViews.parcelBytes()
    }

    @Test
    fun frameDoesNotGrowWithTasks() {
        val small = frameBytes(model(3))
        val big = frameBytes(model(300))
        println("WIDGET SIZE: рамка при 3 задачах ${small / 1024} КБ, при 300 — ${big / 1024} КБ")
        assertTrue("Рамка не зависит от числа задач", big == small)
        assertTrue("Рамка — маленькая: ${big / 1024} КБ", big < LIMIT / 20)
    }

    private companion object {
        const val LIMIT = 1024 * 1024
    }
}
