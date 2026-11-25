package com.prostovasei.simpletasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.prostovasei.simpletasks.data.local.entity.HabitCheckEntity
import com.prostovasei.simpletasks.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

/** Привычки и отметки (#340). */
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sort_order, created_at")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_checks ORDER BY date")
    fun observeChecks(): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habits ORDER BY sort_order, created_at")
    suspend fun getHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_checks ORDER BY date")
    suspend fun getChecks(): List<HabitCheckEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: String): HabitEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM habits")
    suspend fun maxSortOrder(): Int

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    @Upsert
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheckEntity)

    @Query("DELETE FROM habit_checks WHERE habit_id = :habitId AND date = :date")
    suspend fun deleteCheck(habitId: String, date: Long): Int

    @Query("SELECT COUNT(*) FROM habit_checks WHERE habit_id = :habitId AND date = :date")
    suspend fun hasCheck(habitId: String, date: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(checks: List<HabitCheckEntity>)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
