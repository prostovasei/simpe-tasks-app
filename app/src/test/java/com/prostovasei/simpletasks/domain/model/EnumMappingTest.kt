package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EnumMappingTest {

    @Test
    fun priorityMapsDatabaseValues() {
        // Итерация 47: уровней два — средний TickTick (3) читается как высокий, низкий (1) — как обычный.
        assertEquals(Priority.NONE, Priority.fromValue(0))
        assertEquals(Priority.NONE, Priority.fromValue(1))
        assertEquals(Priority.HIGH, Priority.fromValue(3))
        assertEquals(Priority.HIGH, Priority.fromValue(5))
        assertEquals(listOf(0, 5), Priority.entries.map { it.value })
    }

    @Test
    fun unknownPriorityFallsBackToNone() {
        assertEquals(Priority.NONE, Priority.fromValue(2))
        assertEquals(Priority.HIGH, Priority.fromValue(9))
        assertEquals(Priority.NONE, Priority.fromValue(-1))
    }

    @Test
    fun statusMapsTickTickValues() {
        assertEquals(TaskStatus.ACTIVE, TaskStatus.fromValue(0))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue(1))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue(2))
        assertEquals(TaskStatus.ABANDONED, TaskStatus.fromValue(-1))
    }
}
