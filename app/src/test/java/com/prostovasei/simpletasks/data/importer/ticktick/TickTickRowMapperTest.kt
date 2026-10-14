package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.data.local.DueDateCodec
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TickTickRowMapperTest {

    private val moscow = ZoneId.of("Europe/Moscow")
    private val rows = TickTickCsvParser.parse(javaClass.classLoader!!.getResourceAsStream("ticktick/sample_backup.csv")!!)
        .associateBy { it["taskId"] }

    private fun map(id: String) = TickTickRowMapper.map(rows.getValue(id), moscow)

    @Test
    fun allDayDateIsTakenInTaskTimeZone() {
        // 2026-10-12T17:00Z — это полночь 13 октября в Красноярске (UTC+7).
        val task = map("t-inbox")
        assertTrue(task.isAllDay)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 13)), DueDateCodec.decode(task.dueDate, task.isAllDay))
        assertEquals(task.dueDate, task.startDate)
        assertEquals("PT0S", task.reminders)
    }

    @Test
    fun timedRangeKeepsStartAndDue() {
        val task = map("t-call")
        assertFalse(task.isAllDay)
        assertEquals(Instant.parse("2026-10-13T03:00:00Z").toEpochMilli(), task.startDate)
        assertEquals(Instant.parse("2026-10-13T04:00:00Z").toEpochMilli(), task.dueDate)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=TU", task.rrule)
        assertEquals("-PT10080M\nPT0S", task.reminders)
    }

    @Test
    fun checklistLinesBecomeItemsAndOtherLinesDescription() {
        val task = map("t-report")
        assertEquals("Квартальный", task.content)
        assertEquals(
            listOf(ImportedChecklistItem("Собрать данные", isDone = true), ImportedChecklistItem("Отправить", isDone = false)),
            task.checklist,
        )
        assertEquals(TaskStatus.COMPLETED, task.status)
        assertEquals(Instant.parse("2026-10-01T10:00:00Z").toEpochMilli(), task.completedAt)
    }

    @Test
    fun regularTaskKeepsMarkdownAsTextAndNormalizesLineBreaks() {
        val task = map("t-repair")
        assertEquals("Купить \"молоко\", хлеб\nи - [ ] не чек-лист", task.content)
        assertTrue(task.checklist.isEmpty())
    }

    @Test
    fun statusesMapToOurModel() {
        assertEquals(TaskStatus.ACTIVE, map("t-inbox").status)
        assertEquals(TaskStatus.ABANDONED, map("t-abandoned").status)
        assertNull("У активной задачи нет даты выполнения", map("t-inbox").completedAt)
    }

    @Test
    fun subtaskReferencesParent() {
        assertEquals("t-repair", map("t-bulbs").parentExternalId)
        assertEquals("Личное", map("t-bulbs").folderName)
        assertNull(map("t-repair").parentExternalId)
    }

    @Test
    fun missingAllDayFlagAndTimeZoneUseDefaults() {
        val task = map("t-nodate")
        assertTrue(task.isAllDay)
        assertNull(task.dueDate)
        assertNull(task.reminders)
    }

    @Test
    fun unknownTimeZoneFallsBackToDeviceZone() {
        val row = TickTickRow(
            rows.getValue("t-inbox").values + mapOf("Timezone" to "Mars/Olympus", "Due Date" to "2026-10-12T21:00:00+0000"),
        )
        val task = TickTickRowMapper.map(row, moscow)
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 10, 13)), DueDateCodec.decode(task.dueDate, true))
    }

    @Test
    fun createdTimeIsPreserved() {
        assertEquals(Instant.parse("2026-01-10T05:00:00Z").toEpochMilli(), map("t-inbox").createdAt)
    }

    @Test
    fun wholeSourceRowIsStoredForExport() {
        val source = Json.parseToJsonElement(map("t-report").sourceData).jsonObject
        assertEquals(25, source.size)
        assertEquals("kanban", source.getValue("View Mode").jsonPrimitive.content)
        assertEquals("Готово", source.getValue("Column Name").jsonPrimitive.content)
        assertEquals("CHECKLIST", source.getValue("Kind").jsonPrimitive.content)
    }

    @Test
    fun brokenDateFailsRow() {
        assertThrows(Exception::class.java) { map("t-broken") }
    }
}
