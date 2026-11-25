package com.prostovasei.simpletasks.data.repository

import android.content.Context
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.repository.HabitSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Настройки «Привычек» в SharedPreferences (#340): напоминание и время последней копии. */
@Singleton
class PrefsHabitSettingsStore @Inject constructor(@ApplicationContext context: Context) : HabitSettingsStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<HabitSettings> = state.asStateFlow()

    override var lastBackupAt: Long?
        get() = prefs.getLong(KEY_BACKUP, 0).takeIf { it > 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY_BACKUP) else putLong(KEY_BACKUP, value) }

    @Synchronized
    override fun update(transform: (HabitSettings) -> HabitSettings) {
        val next = transform(state.value)
        prefs.edit {
            putBoolean(KEY_REMINDER_ON, next.reminderOn)
            putInt(KEY_REMINDER_MINUTE, next.reminderMinute)
        }
        state.value = next
    }

    private fun read() = HabitSettings(
        reminderOn = prefs.getBoolean(KEY_REMINDER_ON, false),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, HabitSettings.DEFAULT_REMINDER_MINUTE),
    )

    private companion object {
        const val PREFS = "habits"
        const val KEY_REMINDER_ON = "reminder_on"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_BACKUP = "last_backup_at"
    }
}
