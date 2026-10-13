package com.prostovasei.simpletasks.data.importer.ticktick

import java.io.Reader

/**
 * Читатель CSV по RFC 4180: поля в кавычках могут содержать запятые, переводы строк и "" (экранированная кавычка).
 * Строки (записи) разделяются \n или \r\n вне кавычек.
 */
internal class CsvReader(private val reader: Reader) {

    /** Возвращает следующую запись или null в конце файла. */
    fun readRecord(): List<String>? {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var sawAnything = false

        while (true) {
            val code = reader.read()
            if (code == -1) {
                if (!sawAnything) return null
                fields += field.toString()
                return fields
            }
            sawAnything = true
            val c = code.toChar()
            if (inQuotes) {
                if (c == '"') {
                    reader.mark(1)
                    val next = reader.read()
                    if (next == '"'.code) {
                        field.append('"')
                    } else {
                        inQuotes = false
                        if (next != -1) reader.reset()
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        fields += field.toString()
                        field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        fields += field.toString()
                        return fields
                    }
                    else -> field.append(c)
                }
            }
        }
    }
}
