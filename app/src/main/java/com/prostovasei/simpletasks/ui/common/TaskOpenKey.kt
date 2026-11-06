package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.domain.model.TaskDue
import java.time.Instant
import java.time.LocalDate

/**
 * Ключ открытия задачи (итерация 49): у будущего повтора к id серии через «@» дописывается дата раза — «id@d2026-11-22» (весь день)
 * или «id@t1795000000000» (время, мс). Так один и тот же путь «открыть задачу» (приложение, «Неделя», виджет, окно поверх рабочего
 * стола) приводит в окно этого раза. У обычной задачи ключ — просто id.
 */
object TaskOpenKey {
    private const val SEPARATOR = '@'

    fun of(taskId: String, occurrence: TaskDue?): String = when (occurrence) {
        null -> taskId
        is TaskDue.AllDay -> "$taskId${SEPARATOR}d${occurrence.date}"
        is TaskDue.AtTime -> "$taskId${SEPARATOR}t${occurrence.instant.toEpochMilli()}"
    }

    /** id задачи и дата раза (null — открыта сама задача, не будущий повтор). */
    fun parse(key: String): Pair<String, TaskDue?> {
        val at = key.lastIndexOf(SEPARATOR)
        if (at < 0) return key to null
        val id = key.substring(0, at)
        val value = key.substring(at + 2)
        val occurrence = runCatching {
            when (key.getOrNull(at + 1)) {
                'd' -> TaskDue.AllDay(LocalDate.parse(value))
                't' -> TaskDue.AtTime(Instant.ofEpochMilli(value.toLong()))
                else -> null
            }
        }.getOrNull() ?: return key to null
        return id to occurrence
    }
}
