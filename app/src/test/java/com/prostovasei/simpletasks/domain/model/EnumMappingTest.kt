package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EnumMappingTest {

    @Test
    fun priorityMapsDatabaseValues() {
        assertEquals(Priority.NONE, Priority.fromValue(0))
        assertEquals(Priority.LOW, Priority.fromValue(1))
        assertEquals(Priority.MEDIUM, Priority.fromValue(3))
        assertEquals(Priority.HIGH, Priority.fromValue(5))
    }

    @Test
    fun unknownPriorityFallsBackToNone() {
        assertEquals(Priority.NONE, Priority.fromValue(2))
        assertEquals(Priority.NONE, Priority.fromValue(-1))
    }

    @Test
    fun statusTreatsTickTickCompletedAndArchivedAsCompleted() {
        assertEquals(TaskStatus.ACTIVE, TaskStatus.fromValue(0))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue(1))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue(2))
    }
}
