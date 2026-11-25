package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.model.habits.InvalidHabitBackupException
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Привычки (#340): создание, правка, отметки, порядок, архив, удаление, резервная копия. */
class HabitRepositoryTest : DatabaseTest() {
    private val habits by lazy { RoomHabitRepository(db, time, UuidIdGenerator()) }
    private val day = LocalDate.parse("2026-11-25")

    @Test
    fun saveToggleAndEditKeepChecks() = runTest {
        val id = habits.save(null, "  Зарядка ", "🏃", HabitSchedule.Daily)
        assertTrue(habits.toggle(id, day))
        assertTrue(habits.toggle(id, day.minusDays(1)))
        assertFalse(habits.toggle(id, day))
        habits.save(id, "Утренняя зарядка", null, HabitSchedule.PerWeek(3))

        val h = habits.observeHabits().first().single()
        assertEquals("Утренняя зарядка", h.name)
        assertEquals(null, h.emoji)
        assertEquals(HabitSchedule.PerWeek(3), h.schedule)
        assertEquals(setOf(day.minusDays(1)), h.checks)
    }

    @Test
    fun orderArchiveAndDelete() = runTest {
        val a = habits.save(null, "А", null, HabitSchedule.Daily)
        val b = habits.save(null, "Б", null, HabitSchedule.Weekdays(0b0010101))
        val c = habits.save(null, "В", null, HabitSchedule.Daily)
        habits.move(c, up = true)
        assertEquals(listOf("А", "В", "Б"), habits.observeHabits().first().map { it.name })
        habits.setArchived(a, true)
        habits.move(b, up = true)
        assertEquals(listOf("Б", "В"), habits.observeHabits().first().filter { !it.archived }.map { it.name })
        habits.setArchived(a, false)
        assertEquals("А", habits.observeHabits().first().last().name)
        habits.toggle(b, day)
        habits.delete(b)
        assertEquals(listOf("В", "А"), habits.observeHabits().first().map { it.name })
        assertEquals(emptyList<Any>(), db.habitDao().getChecks())
    }

    @Test
    fun backupRoundTripAndInvalidFileKeepsData() = runTest {
        val id = habits.save(null, "Читать", "📚", HabitSchedule.Weekdays(0b1010101))
        habits.toggle(id, day)
        habits.setArchived(habits.save(null, "Старая", null, HabitSchedule.Daily), true)
        val json = habits.exportBackup(HabitSettings(reminderOn = true, reminderMinute = 20 * 60))

        val info = habits.inspectBackup(json)
        assertEquals(2, info.habits)
        assertEquals(1, info.checks)
        habits.delete(id)
        assertThrows(InvalidHabitBackupException::class.java) { habits.inspectBackup("{\"format\":\"simple-tasks-health\"}") }
        assertEquals(1, habits.observeHabits().first().size)

        val settings = habits.restoreBackup(json)
        assertEquals(HabitSettings(true, 20 * 60), settings)
        val restored = habits.observeHabits().first()
        assertEquals(listOf("Читать", "Старая"), restored.map { it.name })
        assertEquals(setOf(day), restored.first().checks)
        assertTrue(restored.last().archived)
    }
}
