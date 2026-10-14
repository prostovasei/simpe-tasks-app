package com.prostovasei.simpletasks.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Источник текущего времени и часового пояса. Подменяется в тестах. */
interface TimeProvider {
    fun now(): Instant

    /** Часовой пояс читается при каждом вызове: пользователь может сменить его в поездке. */
    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
