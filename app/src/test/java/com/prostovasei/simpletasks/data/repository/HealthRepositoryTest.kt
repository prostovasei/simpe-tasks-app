package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.InvalidHealthBackupException
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** Записи веса (#257): одна на дату, перенос даты, удаление, резервная копия. */
class HealthRepositoryTest : DatabaseTest() {
    private val health by lazy { RoomHealthRepository(db, time, UuidIdGenerator()) }
    private val d1 = LocalDate.parse("2026-11-14")
    private val d2 = LocalDate.parse("2026-11-15")
    private val d3 = LocalDate.parse("2026-11-16")

    @Test
    fun entriesAreReturnedByDateAndClamped() = runTest {
        health.save(WeightEntry(d3, 824, "  после тренировки "))
        health.save(WeightEntry(d1, 830))
        health.save(WeightEntry(d2, 99_999))

        val all = health.observeEntries().first()
        assertEquals(listOf(d1, d2, d3), all.map { it.date })
        assertEquals(2500, all[1].tenths)
        assertEquals("после тренировки", all[2].comment)
        assertEquals(null, all[0].comment)
    }

    @Test
    fun secondEntryForTheSameDateReplacesTheFirst() = runTest {
        health.save(WeightEntry(d1, 830, "утро"))
        time.advance(Duration.ofMinutes(5))
        health.save(WeightEntry(d1, 826))

        val all = health.observeEntries().first()
        assertEquals(1, all.size)
        assertEquals(826, all.single().tenths)
        assertEquals(null, all.single().comment)
        // Дата создания остаётся прежней, меняется только обновление.
        val row = db.weightEntryDao().getByDate(d1.toEpochDay())!!
        assertTrue(row.updatedAt > row.createdAt)
    }

    @Test
    fun editingWithAnotherDateMovesTheEntry() = runTest {
        health.save(WeightEntry(d1, 830))
        val id = db.weightEntryDao().getByDate(d1.toEpochDay())!!.id

        health.save(WeightEntry(d2, 828), replacing = d1)

        assertEquals(listOf(d2), health.observeEntries().first().map { it.date })
        assertEquals(id, db.weightEntryDao().getByDate(d2.toEpochDay())!!.id)
    }

    @Test
    fun movingOntoAnOccupiedDateReplacesThatEntry() = runTest {
        health.save(WeightEntry(d1, 830))
        health.save(WeightEntry(d2, 820))

        health.save(WeightEntry(d2, 828), replacing = d1)

        val all = health.observeEntries().first()
        assertEquals(listOf(d2), all.map { it.date })
        assertEquals(828, all.single().tenths)
    }

    @Test
    fun deleteRemovesTheEntry() = runTest {
        health.save(WeightEntry(d1, 830))
        assertTrue(health.hasEntry(d1))

        health.delete(d1)

        assertFalse(health.hasEntry(d1))
        assertTrue(health.observeEntries().first().isEmpty())
        // Отмена удаления возвращает запись.
        health.save(WeightEntry(d1, 830))
        assertTrue(health.hasEntry(d1))
    }

    @Test
    fun backupRoundTripRestoresEntriesAndSettings() = runTest {
        health.save(WeightEntry(d1, 830, "утро"))
        health.save(WeightEntry(d3, 824))
        val settings = HealthSettings(goalTenths = 780, heightCm = 180, reminderOn = true, reminderMinute = 450)
        val json = health.exportBackup(settings)

        val info = health.inspectBackup(json)
        assertEquals(2, info.entries)
        assertEquals(d1, info.from)
        assertEquals(d3, info.to)
        assertEquals(settings, info.settings)

        health.delete(d1)
        health.save(WeightEntry(d2, 700))
        val restored = health.restoreBackup(json)

        assertEquals(settings, restored)
        val all = health.observeEntries().first()
        assertEquals(listOf(d1 to 830, d3 to 824), all.map { it.date to it.tenths })
        assertEquals("утро", all.first().comment)
    }

    @Test
    fun backupWithoutGoalAndHeightIsRestoredToo() = runTest {
        health.save(WeightEntry(d1, 830))
        val json = health.exportBackup(HealthSettings())
        assertEquals(HealthSettings(), health.inspectBackup(json).settings)
    }

    @Test
    fun invalidBackupDoesNotChangeData() = runTest {
        health.save(WeightEntry(d1, 830))
        val good = health.exportBackup(HealthSettings())

        listOf(
            "не json",
            "{}",
            good.replace("simple-tasks-health", "simple-tasks-finance"),
            good.replace("\"version\": 1", "\"version\": 9"),
            // Вес вне допустимого диапазона.
            good.replace("\"tenths\": 830", "\"tenths\": 5"),
        ).forEach { bad ->
            assertThrows(InvalidHealthBackupException::class.java) { health.inspectBackup(bad) }
            assertThrows(InvalidHealthBackupException::class.java) { runBlockingRestore(bad) }
        }
        assertEquals(listOf(830), health.observeEntries().first().map { it.tenths })
    }

    @Test
    fun backupWithDuplicateDatesIsRejected() {
        val day = d1.toEpochDay()
        val entry = { id: String, tenths: Int -> com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity(id, day, tenths, null, 1, 1) }
        val json = HealthBackupCodec.encode(HealthBackup(listOf(entry("a", 830), entry("b", 828)), HealthSettings()), exportedAt = 0)
        assertThrows(InvalidHealthBackupException::class.java) { health.inspectBackup(json) }
    }

    private fun runBlockingRestore(json: String) = kotlinx.coroutines.runBlocking { health.restoreBackup(json) }
}
