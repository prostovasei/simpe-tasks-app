package com.prostovasei.simpletasks.data.importer.ticktick

import com.prostovasei.simpletasks.domain.model.InvalidImportFileException
import java.io.BufferedReader
import java.io.InputStream

/** Строка резервной копии TickTick: «название колонки → значение». */
internal class TickTickRow(val values: Map<String, String>) {
    operator fun get(column: String): String = values[column].orEmpty()
}

/**
 * Разбирает резервную копию TickTick (CSV). Перед заголовком в файле идут служебные строки
 * («Date: …», «Version: …», легенда статусов) — они пропускаются.
 */
internal object TickTickCsvParser {
    private const val FIRST_COLUMN = "Folder Name"

    /** Метка порядка байтов (BOM), с которой TickTick начинает файл. */
    private val BOM = 0xFEFF.toChar()
    private val REQUIRED_COLUMNS = listOf("List Name", "Title", "taskId")

    fun parse(input: InputStream): List<TickTickRow> {
        val reader = BufferedReader(input.reader(Charsets.UTF_8))
        val csv = CsvReader(reader)
        val header = findHeader(csv)
        val missing = REQUIRED_COLUMNS - header.toSet()
        if (missing.isNotEmpty()) throw InvalidImportFileException("Нет колонок: ${missing.joinToString()}")

        val rows = mutableListOf<TickTickRow>()
        while (true) {
            val record = csv.readRecord() ?: break
            if (record.all { it.isBlank() }) continue
            rows += TickTickRow(header.indices.associate { i -> header[i] to record.getOrElse(i) { "" } })
        }
        return rows
    }

    private fun findHeader(csv: CsvReader): List<String> {
        while (true) {
            val record = csv.readRecord() ?: throw InvalidImportFileException("Заголовок TickTick не найден")
            val first = record.firstOrNull()?.trimStart(BOM)?.trim()
            if (first == FIRST_COLUMN) return listOf(FIRST_COLUMN) + record.drop(1).map { it.trim() }
        }
    }
}
