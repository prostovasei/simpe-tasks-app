package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.data.local.entity.HabitCheckEntity
import com.prostovasei.simpletasks.data.local.entity.HabitEntity
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.model.habits.InvalidHabitBackupException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Содержимое резервной копии «Привычек» (#340). */
internal data class HabitBackup(val habits: List<HabitEntity>, val checks: List<HabitCheckEntity>, val settings: HabitSettings)

/** Файл копии «Привычек» — JSON с пометкой [FORMAT] и версией [VERSION], как у «Здоровья». Разбор проверяет расписания и ссылки отметок. */
internal object HabitBackupCodec {
    const val FORMAT = "simple-tasks-habits"
    const val VERSION = 1
    private const val MINUTES_IN_DAY = 24 * 60

    fun encode(backup: HabitBackup, exportedAt: Long): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("exported_at", exportedAt)
        .put("settings", JSONObject().put("reminder_on", backup.settings.reminderOn).put("reminder_minute", backup.settings.reminderMinute))
        .put(
            "habits",
            JSONArray(
                backup.habits.map { h ->
                    JSONObject().put("id", h.id).put("name", h.name).putOpt("emoji", h.emoji).put("schedule", h.schedule)
                        .put("weekdays", h.weekdays).put("per_week", h.perWeek).put("sort_order", h.sortOrder).put("archived", h.archived)
                        .put("created_at", h.createdAt).put("updated_at", h.updatedAt)
                },
            ),
        )
        .put("checks", JSONArray(backup.checks.map { c -> JSONObject().put("habit_id", c.habitId).put("date", c.date).put("created_at", c.createdAt) }))
        .toString(2)

    fun decode(json: String): HabitBackup {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw InvalidHabitBackupException("Это не резервная копия «Привычек» Simple Tasks")
            if (root.optInt("version", -1) != VERSION) throw InvalidHabitBackupException("Неизвестная версия резервной копии")
            val s = root.getJSONObject("settings")
            val settings = HabitSettings(s.getBoolean("reminder_on"), s.getInt("reminder_minute"))
            val habitsJson = root.getJSONArray("habits")
            val habits = (0 until habitsJson.length()).map { i ->
                val o = habitsJson.getJSONObject(i)
                HabitEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    emoji = if (o.isNull("emoji")) null else o.getString("emoji"),
                    schedule = o.getString("schedule"),
                    weekdays = o.getInt("weekdays"),
                    perWeek = o.getInt("per_week"),
                    sortOrder = o.getInt("sort_order"),
                    archived = o.getBoolean("archived"),
                    createdAt = o.getLong("created_at"),
                    updatedAt = o.getLong("updated_at"),
                )
            }
            val checksJson = root.getJSONArray("checks")
            val checks = (0 until checksJson.length()).map { i ->
                val o = checksJson.getJSONObject(i)
                HabitCheckEntity(o.getString("habit_id"), o.getLong("date"), o.getLong("created_at"))
            }
            val ids = habits.map { it.id }.toSet()
            val broken = ids.size != habits.size ||
                habits.any { it.name.isBlank() || !HabitScheduleCodec.valid(it.schedule, it.weekdays, it.perWeek) } ||
                checks.any { it.habitId !in ids } ||
                checks.map { it.habitId to it.date }.toSet().size != checks.size ||
                settings.reminderMinute !in 0 until MINUTES_IN_DAY
            if (broken) throw InvalidHabitBackupException("Резервная копия повреждена")
            return HabitBackup(habits, checks, settings)
        } catch (e: JSONException) {
            throw InvalidHabitBackupException("Файл не читается как резервная копия")
        }
    }
}
