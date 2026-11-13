package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostovasei.simpletasks.data.local.entity.FinancePlaceEntity
import com.prostovasei.simpletasks.data.local.entity.PlaceRow
import kotlinx.coroutines.flow.Flow

/** Справочник мест (#239). */
@Dao
interface FinancePlaceDao {
    @Query("SELECT p.*, (SELECT COUNT(*) FROM finance_operations o WHERE o.place = p.name) AS uses FROM finance_places p")
    fun observeAll(): Flow<List<PlaceRow>>

    /** Действующие места для выбора в окне операции: недавно использованные первыми, остальные — по названию. */
    @Query(
        "SELECT p.name FROM finance_places p LEFT JOIN finance_operations o ON o.place = p.name AND o.is_deleted = 0" +
            " WHERE p.is_archived = 0 GROUP BY p.id ORDER BY MAX(o.created_at) DESC, p.name",
    )
    fun observeHints(): Flow<List<String>>

    @Query("SELECT * FROM finance_places WHERE id = :id")
    suspend fun getById(id: String): FinancePlaceEntity?

    @Query("SELECT * FROM finance_places WHERE name = :name")
    suspend fun getByName(name: String): FinancePlaceEntity?

    /** -1 — место с таким названием уже есть. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(place: FinancePlaceEntity): Long

    @Query("UPDATE finance_places SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long): Int

    @Query("UPDATE finance_places SET is_archived = :archived, updated_at = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long): Int

    @Query("DELETE FROM finance_places WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM finance_places ORDER BY created_at")
    suspend fun getAll(): List<FinancePlaceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(places: List<FinancePlaceEntity>)

    @Query("DELETE FROM finance_places")
    suspend fun deleteAll()
}
