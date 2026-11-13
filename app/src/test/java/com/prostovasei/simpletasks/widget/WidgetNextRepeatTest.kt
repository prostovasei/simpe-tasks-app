package com.prostovasei.simpletasks.widget

import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/** Подсказка в виджете при отметке повторяющейся задачи (#235): «Следующий повтор: …» в формате срока приложения. */
@RunWith(RobolectricTestRunner::class)
class WidgetNextRepeatTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    /** 13.10.2026, 15:00 по Москве. */
    private val time = FakeTimeProvider()

    private fun message(due: TaskDue?) = WidgetActionReceiver.nextRepeatMessage(context, task("t", due = due), time)

    @Test
    fun tomorrowAllDay() {
        assertEquals("Следующий повтор: Завтра", message(TaskDue.AllDay(LocalDate.parse("2026-10-14"))))
    }

    @Test
    fun todayAndLaterDatesWithTime() {
        // 18:00 по Москве.
        assertEquals("Следующий повтор: Завтра, 18:00", message(TaskDue.AtTime(Instant.parse("2026-10-14T15:00:00Z"))))
        assertEquals("Следующий повтор: 16 октября, 09:30", message(TaskDue.AtTime(Instant.parse("2026-10-16T06:30:00Z"))))
    }

    @Test
    fun otherYearShowsYear() {
        assertEquals("Следующий повтор: 5 января 2027", message(TaskDue.AllDay(LocalDate.parse("2027-01-05"))))
    }

    @Test
    fun noDueMeansNoHint() {
        assertNull(message(null))
    }
}
