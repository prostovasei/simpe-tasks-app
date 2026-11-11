package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickCsvParser
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickExporter
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickImporter
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.db.InboxSeed
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.ImportReport
import com.prostovasei.simpletasks.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import androidx.room.withTransaction
import javax.inject.Inject

class RoomBackupRepository @Inject internal constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val importer: TickTickImporter,
    private val exporter: TickTickExporter,
) : BackupRepository {

    override suspend fun importTickTick(input: InputStream, onProgress: (ImportProgress) -> Unit): ImportReport {
        val rows = withContext(Dispatchers.IO) { input.use { TickTickCsvParser.parse(it) } }
        return importer.import(rows, onProgress)
    }

    override suspend fun exportTickTick(output: OutputStream): Int =
        withContext(Dispatchers.IO) { output.use { exporter.export(it) } }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        // Только таблицы задач: финансы (#217) этой кнопкой не стираются — у них своя резервная копия и своё удаление.
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            TASK_TABLES.forEach { sql.execSQL("DELETE FROM $it") }
        }
        InboxSeed(time).seed(db.openHelper.writableDatabase)
    }

    override fun observeStats(): Flow<DataStats> =
        db.taskDao().observeStats()
            .map { DataStats(totalTasks = it.totalTasks, activeTasks = it.activeTasks, lists = it.lists) }
            .distinctUntilChanged()
}

/** Таблицы задач в порядке удаления: сначала зависимые (FOREIGN KEY). */
private val TASK_TABLES = listOf("checklist_items", "tasks", "projects", "project_groups")
