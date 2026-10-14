package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.repository.RoomBackupRepository
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Выгрузка в формат TickTick: импорт → выгрузка → импорт не теряет данных. */
class TickTickExporterTest : DatabaseTest() {

    private lateinit var backup: RoomBackupRepository

    @Before
    fun createRepository() {
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        time.now = Instant.parse("2026-10-13T08:00:00Z")
        backup = RoomBackupRepository(db, time, TickTickImporter(db, time, UuidIdGenerator()), TickTickExporter(db, time))
    }

    private fun sample() = javaClass.classLoader!!.getResourceAsStream("ticktick/sample_backup.csv")!!

    private suspend fun export(): ByteArray = ByteArrayOutputStream().also { backup.exportTickTick(it) }.toByteArray()

    private suspend fun exportedRows() = TickTickCsvParser.parse(export().inputStream()).associateBy { it["Title"] }

    @Test
    fun fileLooksLikeTickTickBackup() = runTest {
        backup.importTickTick(sample())
        val text = export().toString(Charsets.UTF_8)

        assertTrue(text.startsWith("﻿\"Date: 2026-10-13+0000\"\n\"Version: 7.2\"\n\"Status: \n0 Normal"))
        val header = text.lineSequence().first { it.startsWith("\"Folder Name\"") }
        assertEquals(TickTickFormat.COLUMNS.joinToString(",") { "\"$it\"" }, header)
    }

    @Test
    fun importExportImportKeepsEveryTask() = runTest {
        backup.importTickTick(sample())
        val exported = export()

        assertSameTasks(TickTickCsvParser.parse(sample()), TickTickCsvParser.parse(exported.inputStream()), time.zone)

        val statsBefore = backup.observeStats().first()
        backup.clearAllData()
        val report = backup.importTickTick(exported.inputStream())
        assertEquals(8, report.tasksAdded)
        assertEquals(1, report.subtasksLinked)
        assertEquals(statsBefore, backup.observeStats().first())
    }

    @Test
    fun exportingIntoSameDatabaseAddsNothing() = runTest {
        backup.importTickTick(sample())

        val report = backup.importTickTick(export().inputStream())

        assertEquals(0, report.tasksAdded)
        assertEquals(8, report.tasksSkipped)
    }

    @Test
    fun keepsTickTickOnlyFields() = runTest {
        backup.importTickTick(sample())
        val report = exportedRows().getValue("Отчёт")

        assertEquals("kanban", report["View Mode"])
        assertEquals("Готово", report["Column Name"])
        assertEquals("t-report", report["taskId"])
        assertEquals("NOTE", exportedRows().getValue("Идея")["Kind"])
    }

    @Test
    fun tasksCreatedInAppGetTickTickIdsAndFormat() = runTest {
        val home = projects.createProject("Дом", groupId = projects.createGroup("Личное"))
        val parent = tasks.createTask(
            NewTask(
                projectId = home,
                title = "Переезд",
                content = "Первая строка\nВторая строка",
                priority = Priority.MEDIUM,
                due = TaskDue.AtTime(Instant.parse("2026-10-14T02:30:00Z")),
                checklist = listOf("Коробки", "Грузчики"),
            ),
        )
        val child = tasks.createTask(NewTask(projectId = home, title = "Заказать машину", due = TaskDue.AllDay(LocalDate.of(2026, 10, 20))))
        tasks.updateTask(tasks.observeTaskDetails(child).first()!!.task.copy(parentId = parent))

        val rows = exportedRows()
        val move = rows.getValue("Переезд")
        val car = rows.getValue("Заказать машину")

        assertTrue(Regex("[0-9a-f]{24}").matches(move["taskId"]))
        assertEquals(move["taskId"], car["parentId"])
        assertEquals("Личное", move["Folder Name"])
        assertEquals("CHECKLIST", move["Kind"])
        assertEquals("Y", move["Is Check list"])
        assertEquals("Первая строка\rВторая строка\r▫Коробки\r▫Грузчики", move["Content"])
        assertEquals("2026-10-14T02:30:00+0000", move["Due Date"])
        assertEquals("false", move["Is All Day"])
        assertEquals("3", move["Priority"])
        assertEquals("Asia/Krasnoyarsk", move["Timezone"])
        // «Весь день» 20 октября — полночь по Красноярску (UTC+7).
        assertEquals("2026-10-19T17:00:00+0000", car["Due Date"])
        assertEquals("true", car["Is All Day"])
    }

    @Test
    fun exportsCurrentStateNotImportedCopy() = runTest {
        backup.importTickTick(sample())
        val repairId = db.taskDao().getIdByExternalId("t-repair")!!
        val repair = tasks.observeTaskDetails(repairId).first()!!.task
        tasks.updateTask(repair.copy(title = "Ремонт кухни"))
        tasks.setCompleted(repairId, completed = true)
        tasks.deleteTask(db.taskDao().getIdByExternalId("t-note")!!)

        val rows = exportedRows()

        assertFalse("Старое название", rows.containsKey("Ремонт"))
        assertEquals("2", rows.getValue("Ремонт кухни")["Status"])
        assertEquals("2026-10-13T08:00:00+0000", rows.getValue("Ремонт кухни")["Completed Time"])
        assertFalse("Удалённая задача не выгружается", rows.containsKey("Идея"))
        assertEquals("-1", rows.getValue("Отменённая")["Status"])
    }

    @Test
    fun orderFollowsPositionInList() = runTest {
        backup.importTickTick(sample())
        val rows = exportedRows()

        assertTrue(rows.getValue("Ремонт")["Order"].toLong() < rows.getValue("Купить лампочки")["Order"].toLong())
    }
}
