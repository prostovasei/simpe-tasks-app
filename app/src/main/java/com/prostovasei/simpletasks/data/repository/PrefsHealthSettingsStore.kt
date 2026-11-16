package com.prostovasei.simpletasks.data.repository

import android.content.Context
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.repository.HealthSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Настройки «Здоровья» в SharedPreferences (#257): цель, рост, напоминание, время последней копии. */
@Singleton
class PrefsHealthSettingsStore @Inject constructor(@ApplicationContext context: Context) : HealthSettingsStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<HealthSettings> = state.asStateFlow()

    override var lastBackupAt: Long?
        get() = prefs.getLong(KEY_BACKUP, 0).takeIf { it > 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY_BACKUP) else putLong(KEY_BACKUP, value) }

    @Synchronized
    override fun update(transform: (HealthSettings) -> HealthSettings) {
        val next = transform(state.value)
        prefs.edit {
            if (next.goalTenths == null) remove(KEY_GOAL) else putInt(KEY_GOAL, next.goalTenths)
            if (next.heightCm == null) remove(KEY_HEIGHT) else putInt(KEY_HEIGHT, next.heightCm)
            putBoolean(KEY_REMINDER_ON, next.reminderOn)
            putInt(KEY_REMINDER_MINUTE, next.reminderMinute)
        }
        state.value = next
    }

    private fun read() = HealthSettings(
        goalTenths = if (prefs.contains(KEY_GOAL)) prefs.getInt(KEY_GOAL, 0) else null,
        heightCm = if (prefs.contains(KEY_HEIGHT)) prefs.getInt(KEY_HEIGHT, 0) else null,
        reminderOn = prefs.getBoolean(KEY_REMINDER_ON, false),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, HealthSettings.DEFAULT_REMINDER_MINUTE),
    )

    private companion object {
        const val PREFS = "health"
        const val KEY_GOAL = "goal_tenths"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_REMINDER_ON = "reminder_on"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_BACKUP = "last_backup_at"
    }
}
