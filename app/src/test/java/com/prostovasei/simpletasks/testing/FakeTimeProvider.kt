package com.prostovasei.simpletasks.testing

import com.prostovasei.simpletasks.core.time.TimeProvider
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeTimeProvider(
    var now: Instant = Instant.parse("2026-10-13T12:00:00Z"),
    var zone: ZoneId = ZoneId.of("Europe/Moscow"),
) : TimeProvider {
    override fun now(): Instant = now
    override fun zone(): ZoneId = zone

    fun advance(duration: Duration = Duration.ofMinutes(1)) {
        now = now.plus(duration)
    }
}
