package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity
import com.prostovasei.simpletasks.domain.model.health.HealthBackupInfo
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Записи веса в Room (#257). Одна запись на дату; удаление — настоящее (в «Здоровье» корзины нет, возврат — «Отменить» в приложении). */
class RoomHealthRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : HealthRepository {
    private val weights = db.weightEntryDao()

    private fun now() = time.now().toEpochMilli()

    override fun observeEntries(): Flow<List<WeightEntry>> = weights.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(entry: WeightEntry, replacing: LocalDate?) {
        val tenths = Weight.clamp(entry.tenths)
        val comment = entry.comment?.trim()?.takeIf { it.isNotEmpty() }
        db.withTransaction {
            val day = entry.date.toEpochDay()
            val now = now()
            // Правка с переносом на другую дату: старая запись уходит, её id и дата создания переходят к новой.
            val moved = replacing?.takeIf { it != entry.date }?.let { weights.getByDate(it.toEpochDay()) }
            val existing = weights.getByDate(day)
            if (moved != null) weights.deleteByDate(moved.date)
            val base = moved ?: existing
            weights.upsert(WeightEntryEntity(base?.id ?: ids.newId(), day, tenths, comment, base?.createdAt ?: now, now))
        }
    }

    override suspend fun delete(date: LocalDate) {
        weights.deleteByDate(date.toEpochDay())
    }

    override suspend fun hasEntry(date: LocalDate): Boolean = weights.getByDate(date.toEpochDay()) != null

    override suspend fun exportBackup(settings: HealthSettings): String = db.withTransaction {
        HealthBackupCodec.encode(HealthBackup(weights.getAll(), settings), exportedAt = now())
    }

    override fun inspectBackup(json: String): HealthBackupInfo {
        val backup = HealthBackupCodec.decode(json)
        val days = backup.entries.map { it.date }
        return HealthBackupInfo(
            entries = backup.entries.size,
            from = days.minOrNull()?.let(LocalDate::ofEpochDay),
            to = days.maxOrNull()?.let(LocalDate::ofEpochDay),
            settings = backup.settings,
        )
    }

    override suspend fun restoreBackup(json: String): HealthSettings {
        val backup = HealthBackupCodec.decode(json)
        db.withTransaction {
            weights.deleteAll()
            weights.insertAll(backup.entries)
        }
        return backup.settings
    }

    private fun WeightEntryEntity.toDomain() = WeightEntry(LocalDate.ofEpochDay(date), tenths, comment)
}
