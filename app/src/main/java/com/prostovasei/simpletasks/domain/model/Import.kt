package com.prostovasei.simpletasks.domain.model

/** Итог импорта резервной копии. */
data class ImportReport(
    val tasksAdded: Int,
    val tasksSkipped: Int,
    val listsCreated: Int,
    val foldersCreated: Int,
    val checklistItems: Int,
    val subtasksLinked: Int,
    val rowsFailed: Int,
)

data class ImportProgress(val processed: Int, val total: Int)

/** Сводка по базе для экрана «Настройки». */
data class DataStats(val totalTasks: Int, val activeTasks: Int, val lists: Int)

/** Файл не является резервной копией TickTick. */
class InvalidImportFileException(message: String) : Exception(message)
