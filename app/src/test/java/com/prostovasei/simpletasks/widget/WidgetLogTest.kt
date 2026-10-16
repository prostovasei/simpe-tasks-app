package com.prostovasei.simpletasks.widget

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Журнал виджета: события по порядку, не больше 300 строк, ошибки с причиной, очистка. */
@RunWith(RobolectricTestRunner::class)
class WidgetLogTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun keepsLastEventsInOrder() {
        WidgetLog.clear(context)
        (1..305).forEach { WidgetLog.add(context, "событие $it") }
        val lines = WidgetLog.read(context).trim().lines()
        assertEquals(300, lines.size)
        assertTrue(lines.first().endsWith("событие 6"))
        assertTrue(lines.last().endsWith("событие 305"))
    }

    @Test
    fun errorsAndClear() {
        WidgetLog.clear(context)
        WidgetLog.error(context, "отрисовка", IllegalStateException("сломалось"))
        assertTrue(WidgetLog.read(context).contains("ОШИБКА отрисовка: java.lang.IllegalStateException: сломалось"))
        WidgetLog.clear(context)
        assertEquals("", WidgetLog.read(context))
    }
}
