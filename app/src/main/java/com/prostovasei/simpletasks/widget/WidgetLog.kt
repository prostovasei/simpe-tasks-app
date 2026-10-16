package com.prostovasei.simpletasks.widget

import android.content.Context
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Журнал виджета (доработка 0.9.1): последние события — нажатия, перерисовки, ошибки — в файле приложения.
 * Логов с телефона PM у нас нет; если виджет ведёт себя странно, PM присылает журнал из «Настроек»
 * (долгое нажатие на строку версии).
 */
object WidgetLog {
    private const val FILE_NAME = "widget-log.txt"
    private const val MAX_LINES = 300
    private const val TAG = "WidgetLog"
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss.SSS")
    private val lock = Any()

    fun add(context: Context, event: String) {
        Log.i(TAG, event)
        synchronized(lock) {
            runCatching {
                val file = file(context)
                val lines = (if (file.exists()) file.readLines() else emptyList()) + "${TIME.format(LocalDateTime.now())}  $event"
                file.writeText(lines.takeLast(MAX_LINES).joinToString("\n", postfix = "\n"))
            }
        }
    }

    fun error(context: Context, event: String, error: Throwable) {
        Log.e(TAG, event, error)
        add(context, "ОШИБКА $event: $error | " + error.stackTrace.take(STACK_LINES).joinToString(" < "))
    }

    fun read(context: Context): String = synchronized(lock) { runCatching { file(context).readText() }.getOrDefault("") }

    fun clear(context: Context) {
        synchronized(lock) { file(context).delete() }
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    private const val STACK_LINES = 6
}
