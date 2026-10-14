package com.prostovasei.simpletasks.data.importer.ticktick

import org.junit.Assert.assertEquals
import java.time.ZoneId

/**
 * Сравнивает две выгрузки TickTick по смыслу: каждая задача после разбора по правилам импорта
 * должна совпасть во всех полях, кроме служебных (исходная строка и числовое значение Order).
 */
internal fun assertSameTasks(expected: List<TickTickRow>, actual: List<TickTickRow>, zone: ZoneId) {
    fun normalize(rows: List<TickTickRow>) = rows
        .mapNotNull { runCatching { TickTickRowMapper.map(it, zone) }.getOrNull() }
        .associate { it.externalId to it.copy(sourceData = "", order = null) }

    val before = normalize(expected)
    val after = normalize(actual)
    assertEquals("Набор задач", before.keys, after.keys)
    before.forEach { (id, task) -> assertEquals("Задача $id", task, after[id]) }
}
