package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.data.repository.RoomImportRepository
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Проверка на настоящей выгрузке PM. Запускается только локально:
 * ./gradlew testDebugUnitTest -PticktickCsv=/путь/к/файлу.csv --tests '*RealTickTickFileTest*'
 */
class RealTickTickFileTest : DatabaseTest() {

    @Test
    fun importsRealBackup() = runTest(timeout = kotlin.time.Duration.parse("2m")) {
        val path = System.getProperty("ticktick.csv")
        assumeTrue("Файл не задан (-PticktickCsv)", !path.isNullOrBlank())
        time.now = Instant.parse("2026-10-13T08:00:00Z")
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        val repository = RoomImportRepository(db, time, TickTickImporter(db, time, com.prostovasei.simpletasks.core.id.UuidIdGenerator()))

        val started = System.nanoTime()
        val first = repository.importTickTick(File(path).inputStream())
        val firstMs = (System.nanoTime() - started) / 1_000_000
        val second = repository.importTickTick(File(path).inputStream())
        val stats = repository.observeStats().first()

        val rows = db.openHelper.readableDatabase
        fun count(sql: String) = rows.query(sql).use { it.moveToFirst(); it.getInt(0) }
        println("REAL first=$first ms=$firstMs")
        println("REAL second=$second")
        println("REAL stats=$stats")
        println("REAL abandoned=${count("SELECT COUNT(*) FROM tasks WHERE status = ${TaskStatus.ABANDONED.value}")}")
        println("REAL allDay=${count("SELECT COUNT(*) FROM tasks WHERE is_all_day = 1 AND due_date IS NOT NULL")}")
        println("REAL ranges=${count("SELECT COUNT(*) FROM tasks WHERE start_date IS NOT NULL AND start_date != due_date")}")
        println("REAL withReminders=${count("SELECT COUNT(*) FROM tasks WHERE reminders IS NOT NULL")}")
        println("REAL withParent=${count("SELECT COUNT(*) FROM tasks WHERE parent_id IS NOT NULL")}")
        println("REAL checklistDone=${count("SELECT COUNT(*) FROM checklist_items WHERE is_done = 1")}")
        println("REAL allDayEpochDaySum=${count("SELECT SUM(due_date / 86400000) FROM tasks WHERE is_all_day = 1 AND due_date IS NOT NULL")}")
        println("REAL inbox=${count("SELECT COUNT(*) FROM tasks t JOIN projects p ON p.id = t.project_id WHERE p.is_inbox = 1")}")
        assertEquals(0, second.tasksAdded)
    }
}
