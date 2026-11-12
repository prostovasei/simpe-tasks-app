package com.prostovasei.simpletasks.ui.feature.finance

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Когда последний раз сохраняли резервную копию финансов (этап 4, #231) — для строки «Последняя копия» в окне копии. */
interface FinanceBackupStore {
    var lastBackupAt: Long?
}

@Singleton
class PrefsFinanceBackupStore @Inject constructor(@ApplicationContext context: Context) : FinanceBackupStore {
    private val prefs = context.getSharedPreferences("finance", Context.MODE_PRIVATE)

    override var lastBackupAt: Long?
        get() = prefs.getLong(KEY, 0).takeIf { it > 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY) else putLong(KEY, value) }

    private companion object {
        const val KEY = "last_backup_at"
    }
}
