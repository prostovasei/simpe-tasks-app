package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickCsvParser
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickImporter
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.db.InboxSeed
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.ImportReport
import com.prostovasei.simpletasks.domain.repository.ImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class RoomImportRepository @Inject internal constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val importer: TickTickImporter,
) : ImportRepository {

    override suspend fun importTickTick(input: InputStream, onProgress: (ImportProgress) -> Unit): ImportReport {
        val rows = withContext(Dispatchers.IO) { input.use { TickTickCsvParser.parse(it) } }
        return importer.import(rows, onProgress)
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        InboxSeed(time).seed(db.openHelper.writableDatabase)
    }

    override fun observeStats(): Flow<DataStats> =
        db.taskDao().observeStats()
            .map { DataStats(totalTasks = it.totalTasks, activeTasks = it.activeTasks, lists = it.lists) }
            .distinctUntilChanged()
}
