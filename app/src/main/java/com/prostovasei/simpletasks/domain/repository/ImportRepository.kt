package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.ImportReport
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface ImportRepository {
    /**
     * Импортирует резервную копию TickTick. Уже импортированные задачи пропускаются.
     * @throws com.prostovasei.simpletasks.domain.model.InvalidImportFileException если это не файл TickTick
     */
    suspend fun importTickTick(input: InputStream, onProgress: (ImportProgress) -> Unit = {}): ImportReport

    /** Физически удаляет все данные и заново создаёт «Входящие». */
    suspend fun clearAllData()

    fun observeStats(): Flow<DataStats>
}
