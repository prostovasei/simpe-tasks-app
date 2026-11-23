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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Проверяет реальный RemoteViews/reapply: тема Android не должна вернуть карточки или белый текст. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
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
    fun upcomingUsesTwoDpGapsForRegularRepeatAndPeriodWithMatchingTimeStyle() {
        val variants = listOf(
            item(), item().copy(isRecurring = true), item(Priority.HIGH),
            item(Priority.HIGH).copy(isDone = true), item().copy(isDone = true),
            item().copy(due = DueText(DayLabel.Today, "10:00–11:00")),
            item().copy(due = DueText(DayLabel.Today, "10:00–11:00"), isRecurring = true),
        )
        variants.forEach { task ->
            val all = model(WidgetFilter.All).copy(days = listOf(WidgetDayBlock(WidgetDay.On(today), listOf(task))))
            val line = all.lines(dayNames(context), withTail = false).filterIsInstance<WidgetLine.Task>().single()
            val views = WidgetRows.views(context, line, all)
            val root = views.apply(context, FrameLayout(context))
            root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.AT_MOST))
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.widget_row), root.measuredHeight)
            fun x(view: View): Int {
                var result = view.left
                var parent = view.parent
                while (parent is View) { result += parent.left; parent = parent.parent }
                return result
            }
            val time = root.findViewById<View>(R.id.row_time)
            val node = root.findViewById<View>(R.id.row_node)
            val title = root.findViewById<TextView>(R.id.row_title)
            val start = root.findViewById<TextView>(R.id.row_start)
            val gap = context.resources.getDimensionPixelSize(R.dimen.widget_week_content_gap)
            assertEquals(gap, x(node) - x(time) - time.width)
            assertEquals(gap, x(title) - x(node) - node.width)
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.widget_week_check), node.width)
            assertEquals(title.currentTextColor, start.currentTextColor)
            assertTrue(start.typeface.isBold && title.typeface.isBold)
            assertTrue(start.width >= kotlin.math.ceil(start.paint.measureText("10:00")).toInt())
            if (line.row.isPeriod) {
                assertEquals(title.currentTextColor, root.findViewById<TextView>(R.id.row_end).currentTextColor)
                assertEquals(node.width, node.height)
            }
            assertTrue(title.text !is Spanned || (title.text as Spanned).getSpans(0, title.length(), StrikethroughSpan::class.java).isEmpty())
            views.reapply(context, root)
            assertEquals(title.currentTextColor, start.currentTextColor)
        }
    }

    @Test
    fun periodAndOrdinaryReuseRoundLayoutWithoutStaleEndTime() {
        listOf(WidgetFilter.All, WidgetFilter.Inbox).forEach { filter ->
            fun views(period: Boolean) = model(filter).let { model ->
                val task = item().copy(due = DueText(DayLabel.Today, if (period) "10:00–11:00" else "10:00"))
                val current = model.copy(days = listOf(WidgetDayBlock(WidgetDay.On(today), listOf(task))))
                WidgetRows.views(context, current.lines(dayNames(context), withTail = false).filterIsInstance<WidgetLine.Task>().single(), current)
            }
            val period = views(true)
            val root = period.apply(context, FrameLayout(context))
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.row_end).visibility)
            assertEquals("11:00", root.findViewById<TextView>(R.id.row_end).text.toString())
            views(false).reapply(context, root)
            assertEquals(View.GONE, root.findViewById<View>(R.id.row_end).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.row_time_gap).visibility)
            period.reapply(context, root)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.row_end).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.row_time_gap).visibility)
        }
    }

    /** Видимые пиксели при размере 14 dp на xhdpi, а не только размеры ImageView. */
    @Test
    fun repeatAndCircleHaveMatchingVisibleBounds() {
        fun bounds(resource: Int, height: Int): android.graphics.Rect {
            val bitmap = android.graphics.Bitmap.createBitmap(28, height, android.graphics.Bitmap.Config.ARGB_8888)
            requireNotNull(context.getDrawable(resource)).mutate().apply {
                setBounds(0, 0, bitmap.width, bitmap.height)
                draw(android.graphics.Canvas(bitmap))
            }
            val bounds = android.graphics.Rect()
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) >= 128) bounds.union(x, y, x + 1, y + 1)
            }
            bitmap.recycle()
            return bounds
        }
        val circle = bounds(R.drawable.ic_week_node_ring, 28)
        val repeat = bounds(R.drawable.ic_task_repeat_round, 28)
        assertTrue("ширина: $circle / $repeat", kotlin.math.abs(circle.width() - repeat.width()) <= 1)
        assertTrue("высота: $circle / $repeat", kotlin.math.abs(circle.height() - repeat.height()) <= 1)
        assertEquals(bounds(R.drawable.ic_task_period_ring, 116), bounds(R.drawable.ic_task_period_repeat, 116))
    }

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
    fun enlargedFontKeepsMeasuredTimeColumnAndTwoDpGaps() {
        val config = android.content.res.Configuration(context.resources.configuration).apply { fontScale = 1.5f }
        val scaled = context.createConfigurationContext(config)
        // #313: строка задачи в списке дня.
        val views = WidgetWeekRows.taskRow(scaled, today, item())
        val root = views.apply(scaled, FrameLayout(scaled))
        root.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        root.layout(0, 0, 400, root.measuredHeight)
        val time = root.findViewById<TextView>(R.id.wi_time)
        val emoji = root.findViewById<View>(R.id.wi_emoji)
        val title = root.findViewById<View>(R.id.wi_title)
        val gap = scaled.resources.getDimensionPixelSize(R.dimen.widget_week_content_gap)
        assertTrue("время не обрезано", time.width >= kotlin.math.ceil(time.paint.measureText("10:00")).toInt())
        assertEquals(gap, title.left - emoji.right)
        assertEquals(gap, time.left - title.right)
    }

    @Test
    fun weekPriorityChangesKeepFlatRowsAndLightText() {
        // #313: строка задачи в списке дня; Android переиспользует строки — свойства задаются заново.
        fun views(priority: Priority, done: Boolean = false, timed: Boolean = true) =
            WidgetWeekRows.taskRow(context, today, item(priority).copy(isDone = done, due = DueText(DayLabel.Today, if (timed) "10:00" else null)))
        val high = views(Priority.HIGH)
        val root = high.apply(context, FrameLayout(context))
        assertNull(root.findViewById<View>(R.id.wi_plaque).background)
        assertEquals(TaskCanvasTokens.weekPriority.light.toInt(), root.findViewById<TextView>(R.id.wi_title).currentTextColor)
        assertEquals(TaskCanvasTokens.weekPriority.light.toInt(), root.findViewById<TextView>(R.id.wi_time).currentTextColor)
        views(Priority.HIGH, done = true).reapply(context, root)
        val title = root.findViewById<TextView>(R.id.wi_title)
        assertEquals(TaskCanvasTokens.priorityDone.light.toInt(), title.currentTextColor)
        assertEquals(TaskCanvasTokens.priorityDone.light.toInt(), root.findViewById<TextView>(R.id.wi_time).currentTextColor)
        assertTrue(title.text !is Spanned || (title.text as Spanned).getSpans(0, title.length(), StrikethroughSpan::class.java).isEmpty())
        val emoji = root.findViewById<TextView>(R.id.wi_emoji)
        assertTrue(kotlin.math.abs((255 * TaskCanvasTokens.DONE_EMOJI_ALPHA).toInt() - android.graphics.Color.alpha(emoji.currentTextColor)) <= 1)
        views(Priority.NONE, timed = false).reapply(context, root)
        assertEquals(ColorTokens.textPrimary.light.toInt(), title.currentTextColor)
        assertEquals(255, android.graphics.Color.alpha(emoji.currentTextColor))
        assertEquals("", root.findViewById<TextView>(R.id.wi_time).text.toString())
        assertEquals(View.GONE, root.findViewById<View>(R.id.wi_time).visibility)
        assertTrue(title.text !is Spanned || (title.text as Spanned).getSpans(0, title.length(), StrikethroughSpan::class.java).isEmpty())
        assertNull(root.findViewById<View>(R.id.wi_plaque).background)
    }
}
