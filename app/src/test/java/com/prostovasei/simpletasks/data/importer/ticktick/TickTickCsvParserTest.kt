package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.domain.model.InvalidImportFileException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TickTickCsvParserTest {

    private fun sample() = checkNotNull(javaClass.classLoader).getResourceAsStream("ticktick/sample_backup.csv")!!

    @Test
    fun skipsServiceLinesAndReadsRowsByColumnName() {
        val rows = TickTickCsvParser.parse(sample())

        assertEquals(9, rows.size)
        val first = rows.first()
        assertEquals("Inbox", first["List Name"])
        assertEquals("Весь день во входящих", first["Title"])
        assertEquals("t-inbox", first["taskId"])
        assertEquals(25, first.values.size)
    }

    @Test
    fun keepsLineBreaksAndQuotesInsideFields() {
        val repair = TickTickCsvParser.parse(sample()).single { it["taskId"] == "t-repair" }
        assertEquals("Купить \"молоко\", хлеб\rи - [ ] не чек-лист", repair["Content"])
    }

    @Test
    fun rejectsFileWithoutTickTickHeader() {
        val error = assertThrows(InvalidImportFileException::class.java) {
            TickTickCsvParser.parse("name,date\nТест,2026-01-01\n".byteInputStream())
        }
        assertTrue(error.message!!.isNotBlank())
    }

    @Test
    fun rejectsHeaderWithoutRequiredColumns() {
        assertThrows(InvalidImportFileException::class.java) {
            TickTickCsvParser.parse("\"Folder Name\",\"Title\"\n\"\",\"x\"\n".byteInputStream())
        }
    }

    @Test
    fun rejectsEmptyFile() {
        assertThrows(InvalidImportFileException::class.java) { TickTickCsvParser.parse("".byteInputStream()) }
    }
}
