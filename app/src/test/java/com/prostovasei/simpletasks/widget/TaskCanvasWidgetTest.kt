package com.prostovasei.simpletasks.widget

import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import android.text.Spanned
import android.text.style.StrikethroughSpan
import org.junit.Assert.assertTrue
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.feature.week.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/** Проверяет реальный RemoteViews/reapply: тема Android не должна вернуть карточки или белый текст. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp-night-xhdpi")
class TaskCanvasWidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val today = LocalDate.of(2026, 11, 15)
    private fun item(priority: Priority = Priority.NONE) = TaskItem(
        "a", "Проверить счета", priority, DueText(DayLabel.Today, "10:00"), "Дом", "🏠", isInboxList = false,
    )
    private fun model(filter: WidgetFilter) = WidgetModel(filter, null, today,
        listOf(WidgetDayBlock(WidgetDay.On(today), listOf(item()))))

    @Test
    fun upcomingAndInboxKeepDistinctLayoutsAndReapplyKeepsFlatStyle() {
        val all = model(WidgetFilter.All)
        val line = all.lines(dayNames(context), withTail = false).filterIsInstance<WidgetLine.Task>().single()
        val flat = WidgetRows.views(context, line, all)
        var root = flat.apply(context, FrameLayout(context))
        assertNull(root.findViewById<View>(R.id.row_plaque).background)
        assertEquals(ColorTokens.textPrimary.light.toInt(), root.findViewById<TextView>(R.id.row_title).currentTextColor)
        root = WidgetRows.views(context, line, model(WidgetFilter.Inbox)).apply(context, FrameLayout(context))
        assertNotNull(root.findViewById<View>(R.id.row_plaque).background)
        assertEquals(ColorTokens.textPrimary.dark.toInt(), root.findViewById<TextView>(R.id.row_title).currentTextColor)
        root = flat.apply(context, FrameLayout(context))
        flat.reapply(context, root)
        assertNull(root.findViewById<View>(R.id.row_plaque).background)
        assertEquals(ColorTokens.textPrimary.light.toInt(), root.findViewById<TextView>(R.id.row_title).currentTextColor)
    }

    @Test
    fun enlargedFontKeepsMeasuredTimeColumnAndOneDpGaps() {
        val config = android.content.res.Configuration(context.resources.configuration).apply { fontScale = 1.5f }
        val scaled = context.createConfigurationContext(config)
        val start = today.minusDays(6)
        val views = WidgetWeekRows.views(scaled,
            WidgetWeek(start, YearMonth.from(today), (0L..6).map { WeekDay(start.plusDays(it), if (it == 0L) listOf(item()) else emptyList()) }, today),
            androidx.compose.ui.unit.DpSize.Unspecified, WidgetWeekRows.Click { _, _, _ -> })[0]
        val root = views.apply(scaled, FrameLayout(scaled))
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 800, 360)
        val slot = WeekIds.LEFT.slots.first()
        val time = root.findViewById<TextView>(slot.time)
        val node = root.findViewById<View>(slot.check)
        val title = root.findViewById<View>(slot.title)
        val gap = scaled.resources.getDimensionPixelSize(R.dimen.widget_week_content_gap)
        assertTrue("время не обрезано", time.width >= kotlin.math.ceil(time.paint.measureText("10:00")).toInt())
        assertEquals(gap, node.left - time.right)
        assertEquals(gap, title.left - node.right)
    }

    @Test
    fun weekPriorityChangesKeepFlatRowsAndLightText() {
        val start = today.minusDays(6)
        fun views(priority: Priority, done: Boolean = false, timed: Boolean = true) = WidgetWeekRows.views(context,
            WidgetWeek(start, YearMonth.from(today), (0L..6).map { WeekDay(start.plusDays(it), if (it == 0L) listOf(item(priority).copy(isDone = done, due = DueText(DayLabel.Today, if (timed) "10:00" else null))) else emptyList()) }, today),
            androidx.compose.ui.unit.DpSize.Unspecified, WidgetWeekRows.Click { _, _, _ -> })[0]
        val high = views(Priority.HIGH)
        val root = high.apply(context, FrameLayout(context))
        val slot = WeekIds.LEFT.slots.first()
        assertNull(root.findViewById<View>(slot.plaque).background)
        assertEquals(TaskCanvasTokens.priority.light.toInt(), root.findViewById<TextView>(slot.title).currentTextColor)
        assertEquals(TaskCanvasTokens.priority.light.toInt(), root.findViewById<TextView>(slot.time).currentTextColor)
        views(Priority.HIGH, done = true).reapply(context, root)
        val title = root.findViewById<TextView>(slot.title)
        assertEquals("белая галочка и при тёмной системе", ColorTokens.onListSquare.light.toInt(), requireNotNull(root.findViewById<android.widget.ImageView>(slot.tick).imageTintList).defaultColor)
        assertEquals(TaskCanvasTokens.priorityDone.light.toInt(), title.currentTextColor)
        assertEquals(TaskCanvasTokens.priorityDone.light.toInt(), root.findViewById<TextView>(slot.time).currentTextColor)
        assertTrue(title.text !is Spanned || (title.text as Spanned).getSpans(0, title.length(), StrikethroughSpan::class.java).isEmpty())
        val emoji = root.findViewById<TextView>(slot.emoji)
        assertTrue(kotlin.math.abs((255 * TaskCanvasTokens.DONE_EMOJI_ALPHA).toInt() - android.graphics.Color.alpha(emoji.currentTextColor)) <= 1)
        views(Priority.NONE, timed = false).reapply(context, root)
        assertEquals(ColorTokens.textPrimary.light.toInt(), title.currentTextColor)
        assertEquals(255, android.graphics.Color.alpha(emoji.currentTextColor))
        assertEquals("", root.findViewById<TextView>(slot.time).text.toString())
        assertEquals(View.VISIBLE, root.findViewById<View>(slot.time).visibility)
        assertTrue(title.text !is Spanned || (title.text as Spanned).getSpans(0, title.length(), StrikethroughSpan::class.java).isEmpty())
        assertNull(root.findViewById<View>(slot.plaque).background)
    }
}
