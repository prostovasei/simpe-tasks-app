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
 * Размер картинки виджета (0.41.1): весь список уходит рабочему столу одним пакетом, у Android на это лимит ~1 МБ.
 * На 100+ задачах PM пакет не доставлялся, и виджет обновлялся только после возврата из приложения.
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

    /** 105 задач на 30 дней вперёд и 5 просроченных — как у PM после импорта из TickTick, без ограничений виджета. */
    private fun bigModel(): WidgetModel {
        val days = (-5..29).map { today.plusDays(it.toLong()) }
        val items = (0 until 110).map { item(it, days[it % days.size]) }
        val blocks = items.groupBy { it.due!!.day.let { d -> if (d is DayLabel.Date) d.date else today } }.toSortedMap()
            .map { (date, list) -> WidgetDayBlock(WidgetDay.On(date), list) }
        return WidgetModel(WidgetFilter.All, null, today, blocks)
    }

    private fun bytes(model: WidgetModel): Int = runBlocking {
        GlanceRemoteViews().compose(context, DpSize(400.dp, 800.dp), appWidgetOptions = Bundle()) {
            WidgetContent(model, 1, scrollable = true)
        }.remoteViews.parcelBytes()
    }

    @Test
    fun cappedWidgetFitsWithMargin() {
        val all = bytes(bigModel())
        val capped = bigModel().let { m ->
            // Как после ограничений 0.41.1: 7 дней, 10 просроченных, не больше 60 плашек.
            val until = today.plusDays(WidgetModels.DAYS - 1L)
            val past = m.days.filter { (it.day as WidgetDay.On).date < today }.flatMap { it.items }.takeLast(WidgetModels.OVERDUE_MAX)
            m.copy(days = m.days.mapNotNull { block ->
                val date = (block.day as WidgetDay.On).date
                when {
                    date < today -> block.copy(items = block.items.filter { it in past }).takeIf { it.items.isNotEmpty() }
                    date <= until -> block
                    else -> null
                }
            })
        }
        val small = bytes(capped)
        println("WIDGET SIZE: без ограничений ${all / 1024} КБ, с ограничениями ${small / 1024} КБ")
        assertTrue("С ограничениями пакет должен быть меньше трети лимита (~330 КБ): ${small / 1024} КБ", small < LIMIT / 3)
        assertTrue("Ограничения уменьшают пакет", small < all)
    }

    /** Худший случай: все [WidgetModels.ITEMS_MAX] плашек заняты (много задач и повторов в ближайшие 7 дней). */
    @Test
    fun fullWidgetFitsWithMargin() {
        val items = (0 until WidgetModels.ITEMS_MAX).map { item(it, today.plusDays((it % WidgetModels.DAYS).toLong())) }
        val blocks = items.groupBy { it.due!!.day.let { d -> if (d is DayLabel.Date) d.date else today } }.toSortedMap()
            .map { (date, list) -> WidgetDayBlock(WidgetDay.On(date), list) }
        val full = bytes(WidgetModel(WidgetFilter.All, null, today, blocks))
        println("WIDGET SIZE: заполненный виджет ${full / 1024} КБ")
        assertTrue("Полный виджет — меньше трети лимита: ${full / 1024} КБ", full < LIMIT / 3)
    }

    private companion object {
        const val LIMIT = 1024 * 1024
    }
}
