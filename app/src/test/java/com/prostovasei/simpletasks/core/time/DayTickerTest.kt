package com.prostovasei.simpletasks.core.time

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class DayTickerTest {

    /** Время, идущее вместе с виртуальными часами теста. */
    private class VirtualTime(private val scope: TestScope, private val start: Instant, private val zoneId: ZoneId) : TimeProvider {
        override fun now(): Instant = start.plusMillis(scope.testScheduler.currentTime)
        override fun zone(): ZoneId = zoneId
    }

    @Test
    fun switchesDayAtLocalMidnight() = runTest {
        // 23:00 13 октября в Москве.
        val time = VirtualTime(this, Instant.parse("2026-10-13T20:00:00Z"), ZoneId.of("Europe/Moscow"))

        SystemDayTicker(time).today().test {
            assertEquals(LocalDate.of(2026, 10, 13), awaitItem())
            testScheduler.advanceTimeBy(1.hours.inWholeMilliseconds + 1)
            assertEquals(LocalDate.of(2026, 10, 14), awaitItem())
            testScheduler.advanceTimeBy(24.hours.inWholeMilliseconds)
            assertEquals(LocalDate.of(2026, 10, 15), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
