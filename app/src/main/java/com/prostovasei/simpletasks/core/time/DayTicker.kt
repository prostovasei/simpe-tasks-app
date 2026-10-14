package com.prostovasei.simpletasks.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate

/** Текущий день, который сам переключается в полночь — чтобы «Сегодня» не устаревало на открытом экране (аудит R7). */
fun interface DayTicker {
    fun today(): Flow<LocalDate>
}

class SystemDayTicker(private val time: TimeProvider) : DayTicker {
    override fun today(): Flow<LocalDate> = flow {
        while (true) {
            val now = time.now()
            val zone = time.zone()
            val today = now.atZone(zone).toLocalDate()
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
            // Пояс и дата пересчитываются после каждого ожидания: смена пояса подхватится не позже полуночи.
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(MIN_DELAY_MS))
        }
    }.distinctUntilChanged()

    private companion object {
        const val MIN_DELAY_MS = 1_000L
    }
}
