package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

/** Записи веса (#257). */
@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries ORDER BY date")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY date")
    suspend fun getAll(): List<WeightEntryEntity>

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun getByDate(date: Long): WeightEntryEntity?

    /** Занятая дата (уникальный индекс) заменяется. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteByDate(date: Long): Int

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntryEntity>)
}
