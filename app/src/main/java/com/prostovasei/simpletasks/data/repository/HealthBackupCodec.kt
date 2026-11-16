package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.InvalidHealthBackupException
import com.prostovasei.simpletasks.domain.model.health.Weight
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Содержимое резервной копии «Здоровья» (#257): записи веса и настройки. */
internal data class HealthBackup(val entries: List<WeightEntryEntity>, val settings: HealthSettings)

/**
 * Файл резервной копии «Здоровья» — JSON: пометка [FORMAT], версия [VERSION], время, настройки и записи как в таблице.
 * Разбор проверяет пометку, версию, диапазоны веса и уникальность дат.
 */
internal object HealthBackupCodec {
    const val FORMAT = "simple-tasks-health"
    const val VERSION = 1

    fun encode(backup: HealthBackup, exportedAt: Long): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("exported_at", exportedAt)
        .put(
            "settings",
            JSONObject()
                .putOpt("goal_tenths", backup.settings.goalTenths)
                .putOpt("height_cm", backup.settings.heightCm)
                .put("reminder_on", backup.settings.reminderOn)
                .put("reminder_minute", backup.settings.reminderMinute),
        )
        .put(
            "weights",
            JSONArray(
                backup.entries.map { e ->
                    JSONObject().put("id", e.id).put("date", e.date).put("tenths", e.tenths).putOpt("comment", e.comment)
                        .put("created_at", e.createdAt).put("updated_at", e.updatedAt)
                },
            ),
        )
        .toString(2)

    fun decode(json: String): HealthBackup {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw InvalidHealthBackupException("Это не резервная копия «Здоровья» Simple Tasks")
            if (root.optInt("version", -1) != VERSION) throw InvalidHealthBackupException("Неизвестная версия резервной копии")
            val s = root.getJSONObject("settings")
            val settings = HealthSettings(
                goalTenths = if (s.isNull("goal_tenths")) null else s.getInt("goal_tenths"),
                heightCm = if (s.isNull("height_cm")) null else s.getInt("height_cm"),
                reminderOn = s.getBoolean("reminder_on"),
                reminderMinute = s.getInt("reminder_minute"),
            )
            val array = root.getJSONArray("weights")
            val entries = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                WeightEntryEntity(
                    id = o.getString("id"),
                    date = o.getLong("date"),
                    tenths = o.getInt("tenths"),
                    comment = if (o.isNull("comment")) null else o.getString("comment"),
                    createdAt = o.getLong("created_at"),
                    updatedAt = o.getLong("updated_at"),
                )
            }
            val outOfRange = entries.any { it.tenths !in Weight.MIN_TENTHS..Weight.MAX_TENTHS } ||
                (settings.goalTenths != null && settings.goalTenths !in Weight.MIN_TENTHS..Weight.MAX_TENTHS) ||
                (settings.heightCm != null && settings.heightCm !in Weight.MIN_HEIGHT..Weight.MAX_HEIGHT) ||
                settings.reminderMinute !in 0 until MINUTES_IN_DAY
            val duplicates = entries.map { it.date }.toSet().size != entries.size || entries.map { it.id }.toSet().size != entries.size
            if (outOfRange || duplicates) throw InvalidHealthBackupException("Резервная копия повреждена")
            return HealthBackup(entries, settings)
        } catch (e: JSONException) {
            throw InvalidHealthBackupException("Файл не читается как резервная копия")
        }
    }

    private const val MINUTES_IN_DAY = 24 * 60
}
