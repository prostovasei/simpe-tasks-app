package com.prostovasei.simpletasks.data.importer.ticktick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class CsvReaderTest {

    private fun records(text: String): List<List<String>> {
        val reader = CsvReader(BufferedReader(StringReader(text)))
        return generateSequence { reader.readRecord() }.toList()
    }

    @Test
    fun readsQuotedFieldsWithCommasQuotesAndLineBreaks() {
        val result = records("\"a,b\",\"say \"\"hi\"\"\",\"line1\rline2\nline3\"\nplain,,end\n")
        assertEquals(
            listOf(
                listOf("a,b", "say \"hi\"", "line1\rline2\nline3"),
                listOf("plain", "", "end"),
            ),
            result,
        )
    }

    @Test
    fun supportsWindowsLineEndingsAndMissingFinalNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), records("a,b\r\nc,d"))
    }

    @Test
    fun emptyInputHasNoRecords() {
        assertNull(CsvReader(BufferedReader(StringReader(""))).readRecord())
    }

    @Test
    fun quoteAtEndOfFileIsHandled() {
        assertEquals(listOf(listOf("x", "y")), records("\"x\",\"y\""))
    }
}
