package com.prostovasei.simpletasks.ui.feature.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #348: доли полосы хаба и признаки превышения плана. */
class HubBarScaleTest {
    private val k = 100_000L

    @Test fun normalMonthHasNoTick() {
        val s = HubBarScale.of(42 * k, 90 * k, 48 * k)
        assertEquals(42f / 90, s.fact, 0.001f)
        assertEquals(48f / 90, s.forecast, 0.001f)
        assertNull(s.plan)
        assertFalse(s.factOver || s.forecastOver)
    }

    @Test fun forecastOverScalesToForecast() {
        val s = HubBarScale.of(62 * k, 90 * k, 104 * k)
        assertEquals(1f, s.forecast, 0.001f)
        assertEquals(90f / 104, s.plan!!, 0.001f)
        assertTrue(s.forecastOver); assertFalse(s.factOver)
    }

    @Test fun factOverIsRed() {
        val s = HubBarScale.of(97 * k, 90 * k, 112 * k)
        assertTrue(s.factOver)
        assertEquals(90f / 112, s.plan!!, 0.001f)
    }

    @Test fun noBudgetScalesToForecast() {
        val s = HubBarScale.of(42 * k, null, 48 * k)
        assertNull(s.plan)
        assertEquals(1f, s.forecast, 0.001f)
        assertEquals("42", financeNumber(42 * k))
    }
}
