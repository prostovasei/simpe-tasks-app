package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.local.db.InboxSeed
import com.prostovasei.simpletasks.data.repository.RoomBackupRepository
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.InvalidImportFileException
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TickTickImporterTest : DatabaseTest() {

    private lateinit var importer: RoomBackupRepository

    @Before
    fun createImporter() {
        time.zone = ZoneId.of("Asia/Krasnoyarsk")
        importer = RoomBackupRepository(db, time, TickTickImporter(db, time, UuidIdGenerator()), TickTickExporter(db, time))
    }

    private fun sample() = javaClass.classLoader!!.getResourceAsStream("ticktick/sample_backup.csv")!!

    private suspend fun taskByExternalId(externalId: String) =
        db.taskDao().getById(db.taskDao().getIdByExternalId(externalId)!!)!!

    @Test
    fun importsListsFoldersTasksAndChecklists() = runTest {
        val report = importer.importTickTick(sample())

        assertEquals(8, report.tasksAdded)
        assertEquals(0, report.tasksSkipped)
        assertEquals(1, report.rowsFailed)
        assertEquals(3, report.listsCreated)
        assertEquals(1, report.foldersCreated)
        assertEquals(2, report.checklistItems)
        assertEquals(1, report.subtasksLinked)

        val tree = projects.observeProjectTree().first()
        assertEquals(2, tree.inbox!!.openTaskCount)
        assertEquals(listOf("Личное"), tree.groups.map { it.group.name })
        assertEquals(listOf("Дом"), tree.groups.single().projects.map { it.project.name })
        assertEquals(setOf("Работа", "Заметки"), tree.ungrouped.map { it.project.name }.toSet())
    }

    @Test
    fun importedTasksAppearInScreens() = runTest {
        importer.importTickTick(sample())

        val sections = tasks.observeAllScreen(LocalDate.of(2026, 10, 13)).first()
        assertEquals(listOf("Весь день во входящих"), sections.todayAllDay.map { it.title })
        assertEquals(listOf("Созвон"), sections.todayTimed.map { it.title })
        // Отменённая (-1) и выполненная задачи в секции не попадают.
        assertTrue(sections.overdue.isEmpty())

        val call = sections.todayTimed.single()
        assertEquals(listOf("-PT10080M", "PT0S"), call.reminders)
        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=TU", call.rrule)
        assertTrue(call.start != null && call.start != call.due)
    }

    @Test
    fun keepsStatusesDatesAndChecklistState() = runTest {
        importer.importTickTick(sample())

        assertEquals(TaskStatus.ABANDONED.value, taskByExternalId("t-abandoned").status)
        val report = taskByExternalId("t-report")
        assertEquals(TaskStatus.COMPLETED.value, report.status)
        val details = tasks.observeTaskDetails(report.id).first()!!
        assertEquals(listOf("Собрать данные" to true, "Отправить" to false), details.checklist.map { it.title to it.isDone })
        assertEquals(java.time.Instant.parse("2026-01-10T05:00:00Z"), details.task.createdAt)
    }

    @Test
    fun linksSubtaskEvenWhenItComesBeforeParent() = runTest {
        importer.importTickTick(sample())

        val parent = taskByExternalId("t-repair")
        val child = taskByExternalId("t-bulbs")
        assertEquals(parent.id, child.parentId)
        assertNull(parent.parentId)
    }

    @Test
    fun deletingParentDeletesSubtasks() = runTest {
        importer.importTickTick(sample())
        val parent = taskByExternalId("t-repair")

        tasks.deleteTask(parent.id)

        assertTrue(taskByExternalId("t-bulbs").isDeleted)
    }

    @Test
    fun ordersTasksInsideListByTickTickOrder() = runTest {
        importer.importTickTick(sample())

        val home = projects.observeProjectTree().first().groups.single().projects.single().project.id
        assertEquals(listOf("Ремонт", "Купить лампочки"), tasks.observeProjectTasks(home).first().map { it.title })
    }

    @Test
    fun reimportSkipsExistingTasks() = runTest {
        importer.importTickTick(sample())
        val second = importer.importTickTick(sample())

        assertEquals(0, second.tasksAdded)
        assertEquals(8, second.tasksSkipped)
        assertEquals(0, second.listsCreated)
        assertEquals(0, second.foldersCreated)
        assertEquals(DataStats(totalTasks = 8, activeTasks = 6, lists = 4), importer.observeStats().first())
    }

    @Test
    fun reportsProgressUpToTotal() = runTest {
        val progress = mutableListOf<ImportProgress>()
        importer.importTickTick(sample()) { progress += it }

        assertEquals((1..8).toList(), progress.map { it.processed })
        assertTrue(progress.all { it.total == 8 })
    }

    @Test
    fun notTickTickFileIsRejectedWithoutChanges() = runTest {
        val result = runCatching { importer.importTickTick("a,b\n1,2\n".byteInputStream()) }

        assertTrue(result.exceptionOrNull() is InvalidImportFileException)
        assertEquals(DataStats(totalTasks = 0, activeTasks = 0, lists = 1), importer.observeStats().first())
    }

    @Test
    fun clearAllDataRemovesEverythingAndRecreatesInbox() = runTest {
        importer.importTickTick(sample())

        importer.clearAllData()

        assertEquals(DataStats(totalTasks = 0, activeTasks = 0, lists = 1), importer.observeStats().first())
        assertEquals(InboxSeed.INBOX_ID, projects.getInbox().id)
        // После очистки тот же файл импортируется заново целиком.
        assertEquals(8, importer.importTickTick(sample()).tasksAdded)
    }

    @Test
    fun largeBackupImportsQuickly() = runTest {
        val csv = buildString {
            append("\"Folder Name\",\"List Name\",\"Title\",\"Kind\",\"Content\",\"Due Date\",\"Is All Day\",\"Status\",\"Order\",\"Timezone\",\"taskId\"\n")
            repeat(3500) { i ->
                val kind = if (i % 10 == 0) "CHECKLIST" else "TEXT"
                val content = if (kind == "CHECKLIST") "▪Один\r▫Два" else "Описание $i"
                append("\"\",\"Список ${i % 12}\",\"Задача $i\",\"$kind\",\"$content\",\"2026-10-12T17:00:00+0000\",\"true\",\"${if (i % 20 == 0) 0 else 2}\",\"$i\",\"Asia/Krasnoyarsk\",\"id-$i\"\n")
            }
        }

        val started = System.nanoTime()
        val report = importer.importTickTick(csv.byteInputStream())
        val seconds = (System.nanoTime() - started) / 1e9

        assertEquals(3500, report.tasksAdded)
        assertEquals(700, report.checklistItems)
        assertTrue("Импорт 3500 задач занял ${"%.1f".format(seconds)} с", seconds < 30)
    }
}
