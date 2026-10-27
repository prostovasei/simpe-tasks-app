package com.prostovasei.simpletasks.ui.feature.task

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** История «Созданных вручную» напоминаний (итерация 31): по 3 последних, новые сверху, без повторов. */
@RunWith(RobolectricTestRunner::class)
class ReminderHistoryTest {

    @Test
    fun newestFirstWithoutDuplicatesAndOnlyThree() {
        assertEquals(listOf("-PT45M", "-PT2H"), ReminderHistory.merge(listOf("-PT2H"), listOf("-PT45M")))
        // «-P0DT2H0M0S» — тот же момент, что «-PT2H»: поднимается наверх, не дублируется.
        assertEquals(listOf("-P0DT2H0M0S", "-PT45M"), ReminderHistory.merge(listOf("-PT45M", "-PT2H"), listOf("-P0DT2H0M0S")))
        assertEquals(listOf("-PT4H", "-PT3H", "-PT2H"), ReminderHistory.merge(listOf("-PT1H"), listOf("-PT2H", "-PT3H", "-PT4H")))
    }

    @Test
    fun timedAndAllDayAreKeptSeparatelyAcrossRestarts() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ReminderHistory.add(context, allDay = false, created = listOf("-PT45M"))
        ReminderHistory.add(context, allDay = true, created = listOf("-P1DT15H0M0S"))
        ReminderHistory.add(context, allDay = false, created = listOf("-PT2H15M"))
        assertEquals(listOf("-PT2H15M", "-PT45M"), ReminderHistory.load(context, allDay = false))
        assertEquals(listOf("-P1DT15H0M0S"), ReminderHistory.load(context, allDay = true))
    }
}
