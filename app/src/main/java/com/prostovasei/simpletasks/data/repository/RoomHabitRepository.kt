package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.HabitCheckEntity
import com.prostovasei.simpletasks.data.local.entity.HabitEntity
import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitBackupInfo
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Привычки в Room (#340). Удаление — настоящее; от случайного удаления — архив и подтверждение. */
class RoomHabitRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : HabitRepository {
    private val dao = db.habitDao()

    private fun now() = time.now().toEpochMilli()

    override fun observeHabits(): Flow<List<Habit>> = combine(dao.observeHabits(), dao.observeChecks()) { habits, checks ->
        val byHabit = checks.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.date) })
        habits.map { it.toDomain(byHabit[it.id].orEmpty().toSet()) }
    }

    override suspend fun save(id: String?, name: String, emoji: String?, schedule: HabitSchedule): String = db.withTransaction {
        val now = now()
        val existing = id?.let { dao.getHabit(it) }
        val (kind, mask, perWeek) = HabitScheduleCodec.encode(schedule)
        val habit = HabitEntity(
            id = existing?.id ?: ids.newId(),
            name = name.trim().take(Habit.MAX_NAME),
            emoji = emoji?.trim()?.takeIf { it.isNotEmpty() },
            schedule = kind,
            weekdays = mask,
            perWeek = perWeek,
            sortOrder = existing?.sortOrder ?: (dao.maxSortOrder() + 1),
            archived = existing?.archived ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        dao.upsertHabit(habit)
        habit.id
    }

    override suspend fun toggle(id: String, date: LocalDate): Boolean = db.withTransaction {
        val day = date.toEpochDay()
        if (dao.hasCheck(id, day) > 0) {
            dao.deleteCheck(id, day)
            false
        } else {
            dao.insertCheck(HabitCheckEntity(id, day, now()))
            true
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        db.withTransaction {
            val habit = dao.getHabit(id) ?: return@withTransaction
            // Из архива привычка возвращается в конец списка.
            val order = if (!archived && habit.archived) dao.maxSortOrder() + 1 else habit.sortOrder
            dao.upsertHabit(habit.copy(archived = archived, sortOrder = order, updatedAt = now()))
        }
    }

    override suspend fun move(id: String, up: Boolean) {
        db.withTransaction {
            val active = dao.getHabits().filter { !it.archived }
            val index = active.indexOfFirst { it.id == id }
            val target = if (up) index - 1 else index + 1
            if (index < 0 || target !in active.indices) return@withTransaction
            val reordered = active.toMutableList().apply { add(target, removeAt(index)) }
            val now = now()
            dao.upsertHabits(reordered.mapIndexed { i, h -> h.copy(sortOrder = i, updatedAt = if (h.sortOrder != i) now else h.updatedAt) })
        }
    }

    override suspend fun delete(id: String) {
        dao.deleteHabit(id)
    }

    override suspend fun exportBackup(settings: HabitSettings): String = db.withTransaction {
        HabitBackupCodec.encode(HabitBackup(dao.getHabits(), dao.getChecks(), settings), exportedAt = now())
    }

    override fun inspectBackup(json: String): HabitBackupInfo {
        val backup = HabitBackupCodec.decode(json)
        return HabitBackupInfo(backup.habits.size, backup.checks.size, backup.settings)
    }

    override suspend fun restoreBackup(json: String): HabitSettings {
        val backup = HabitBackupCodec.decode(json)
        db.withTransaction {
            dao.deleteAllHabits()
            dao.upsertHabits(backup.habits)
            dao.insertChecks(backup.checks)
        }
        return backup.settings
    }

    private fun HabitEntity.toDomain(checks: Set<LocalDate>) =
        Habit(id, name, emoji, HabitScheduleCodec.decode(schedule, weekdays, perWeek), sortOrder, archived, checks)
}

/** Расписание в колонках таблицы: `DAILY` / `WEEKDAYS` + маска / `PER_WEEK` + число. */
internal object HabitScheduleCodec {
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val PER_WEEK = "PER_WEEK"

    fun encode(schedule: HabitSchedule): Triple<String, Int, Int> = when (schedule) {
        HabitSchedule.Daily -> Triple(DAILY, HabitSchedule.ALL_DAYS, 0)
        is HabitSchedule.Weekdays -> Triple(WEEKDAYS, schedule.mask.coerceIn(1, HabitSchedule.ALL_DAYS), 0)
        is HabitSchedule.PerWeek -> Triple(PER_WEEK, HabitSchedule.ALL_DAYS, schedule.times.coerceIn(1, HabitSchedule.MAX_PER_WEEK))
    }

    fun decode(kind: String, mask: Int, perWeek: Int): HabitSchedule = when (kind) {
        WEEKDAYS -> if (mask and HabitSchedule.ALL_DAYS == 0) HabitSchedule.Daily else HabitSchedule.Weekdays(mask and HabitSchedule.ALL_DAYS)
        PER_WEEK -> HabitSchedule.PerWeek(perWeek.coerceIn(1, HabitSchedule.MAX_PER_WEEK))
        else -> HabitSchedule.Daily
    }

    fun valid(kind: String, mask: Int, perWeek: Int): Boolean = when (kind) {
        DAILY -> true
        WEEKDAYS -> mask in 1..HabitSchedule.ALL_DAYS
        PER_WEEK -> perWeek in 1..HabitSchedule.MAX_PER_WEEK
        else -> false
    }
}
