package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.health.HealthBackupInfo
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Записи веса (#257). Одна запись на дату: сохранение на занятую дату заменяет прежнюю. */
interface HealthRepository {
    /** Все записи по возрастанию даты. */
    fun observeEntries(): Flow<List<WeightEntry>>

    /**
     * Сохраняет запись. [replacing] — прежняя дата записи, которую правят: если дату сменили, старая запись исчезает.
     * Запись на дату [WeightEntry.date], если она уже есть, заменяется.
     */
    suspend fun save(entry: WeightEntry, replacing: LocalDate? = null)

    suspend fun delete(date: LocalDate)

    suspend fun hasEntry(date: LocalDate): Boolean

    /** Копия в JSON: записи и [settings]. */
    suspend fun exportBackup(settings: HealthSettings): String

    /** Что в файле копии. @throws com.prostovasei.simpletasks.domain.model.health.InvalidHealthBackupException файл не подходит. */
    fun inspectBackup(json: String): HealthBackupInfo

    /** Заменяет записи данными копии целиком и возвращает настройки копии. Негодный файл данные не меняет. */
    suspend fun restoreBackup(json: String): HealthSettings
}

/** Хранилище настроек «Здоровья» и времени последней копии (#257). */
interface HealthSettingsStore {
    val settings: StateFlow<HealthSettings>
    var lastBackupAt: Long?
    fun update(transform: (HealthSettings) -> HealthSettings)
}
