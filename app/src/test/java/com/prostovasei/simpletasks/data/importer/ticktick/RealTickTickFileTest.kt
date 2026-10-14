package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.data.mapper.toDomain
import com.prostovasei.simpletasks.data.repository.RoomBackupRepository
import com.prostovasei.simpletasks.domain.model.nextOccurrence
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
        val path = System.getProperty("ticktick.csv").orEmpty()
        assumeTrue("Файл не задан (-PticktickCsv)", path.isNotBlank())
        time.now = Instant.parse("2026-10-13T08:00:00Z")
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        val repository = RoomBackupRepository(db, time, TickTickImporter(db, time, com.prostovasei.simpletasks.core.id.UuidIdGenerator()), TickTickExporter(db, time))

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

    @Test
    fun realBackupSurvivesExportAndReimport() = runTest(timeout = kotlin.time.Duration.parse("2m")) {
        val path = System.getProperty("ticktick.csv").orEmpty()
        assumeTrue("Файл не задан (-PticktickCsv)", path.isNotBlank())
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        val ids = com.prostovasei.simpletasks.core.id.UuidIdGenerator()
        val repository = RoomBackupRepository(db, time, TickTickImporter(db, time, ids), TickTickExporter(db, time))

        repository.importTickTick(File(path).inputStream())
        val statsBefore = repository.observeStats().first()
        val exported = java.io.ByteArrayOutputStream()
        val count = repository.exportTickTick(exported)
        println("REAL exported=$count bytes=${exported.size()}")

        val original = TickTickCsvParser.parse(File(path).inputStream())
        val roundTrip = TickTickCsvParser.parse(exported.toByteArray().inputStream())
        assertSameTasks(original, roundTrip, time.zone)

        repository.clearAllData()
        val report = repository.importTickTick(exported.toByteArray().inputStream())
        println("REAL reimport=$report")
        assertEquals(statsBefore, repository.observeStats().first())
    }

    @Test
    fun realRepeatsAndRemindersWork() = runTest(timeout = kotlin.time.Duration.parse("2m")) {
        val path = System.getProperty("ticktick.csv").orEmpty()
        assumeTrue("Файл не задан (-PticktickCsv)", path.isNotBlank())
        time.now = Instant.parse("2026-10-14T03:00:00Z")
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        val repository = RoomBackupRepository(db, time, TickTickImporter(db, time, com.prostovasei.simpletasks.core.id.UuidIdGenerator()), TickTickExporter(db, time))
        repository.importTickTick(File(path).inputStream())
        val today = time.now().atZone(time.zone).toLocalDate()

        val ids = db.openHelper.readableDatabase.query("SELECT id FROM tasks WHERE is_deleted = 0").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        val tasks = ids.map { checkNotNull(db.taskDao().observeById(it).first()).toDomain() }

        val repeating = tasks.filter { it.status == TaskStatus.ACTIVE && it.rrule != null && it.due != null }
        val moved = repeating.mapNotNull { t -> t.nextOccurrence(time.zone, today)?.let { t to it } }
        val ended = repeating.filter { t -> moved.none { it.first.id == t.id } }
        moved.forEach { (before, after) -> println("REAL repeat ${before.rrule}: ${before.due} -> ${after.due} (${after.rrule})") }
        ended.forEach { println("REAL repeat ended ${it.rrule}: ${it.due}") }
        println("REAL repeating=${repeating.size} moved=${moved.size} ended=${ended.size}")
        assertEquals("Закончиться может только правило с COUNT/UNTIL", emptyList<String>(), ended.mapNotNull { it.rrule }.filterNot { "COUNT=" in it || "UNTIL=" in it })

        val withReminders = tasks.filter { it.reminders.isNotEmpty() && it.due != null }
        val unreadable = withReminders.flatMap { it.reminders }.filter { com.prostovasei.simpletasks.domain.model.parseReminder(it) == null }
        println("REAL reminders tasks=${withReminders.size} unreadable=$unreadable")
        assertEquals(emptyList<String>(), unreadable)
        val upcoming = tasks.filter { it.status == TaskStatus.ACTIVE }
            .flatMap { t -> com.prostovasei.simpletasks.domain.model.Reminders.instants(t, time.zone).filter { it > time.now() }.map { it to t.title.length } }
        println("REAL upcoming reminder moments=${upcoming.size} first=${upcoming.minOfOrNull { it.first }}")
    }
}
