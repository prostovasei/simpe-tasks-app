package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitBackupInfo
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Привычки и отметки (#340). */
interface HabitRepository {
    /** Все привычки (и архивные) по порядку, с отметками. */
    fun observeHabits(): Flow<List<Habit>>

    /** Новая привычка (в конец списка) или правка: id сохраняется, отметки не меняются. Возвращает id. */
    suspend fun save(id: String?, name: String, emoji: String?, schedule: HabitSchedule): String

    /** Отметить или снять «сделано» за [date]. Возвращает новое состояние. */
    suspend fun toggle(id: String, date: LocalDate): Boolean

    suspend fun setArchived(id: String, archived: Boolean)

    /** Сдвинуть привычку на одну позицию среди активных: [up] — выше. */
    suspend fun move(id: String, up: Boolean)

    /** Удалить навсегда вместе с отметками. */
    suspend fun delete(id: String)

    suspend fun exportBackup(settings: HabitSettings): String

    /** @throws com.prostovasei.simpletasks.domain.model.habits.InvalidHabitBackupException файл не подходит. */
    fun inspectBackup(json: String): HabitBackupInfo

    /** Заменяет привычки и отметки данными копии и возвращает настройки копии. Негодный файл данные не меняет. */
    suspend fun restoreBackup(json: String): HabitSettings
}

/** Настройки «Привычек» и время последней копии (#340). */
interface HabitSettingsStore {
    val settings: StateFlow<HabitSettings>
    var lastBackupAt: Long?
    fun update(transform: (HabitSettings) -> HabitSettings)
}
