package com.prostovasei.simpletasks.widget

import android.content.Context
import com.prostovasei.simpletasks.BuildConfig
import com.prostovasei.simpletasks.core.update.AppUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * #354: красная точка на кнопке обновления в панели «Главной». Номер последней найденной сборки хранится в настройках;
 * проверка в сети — не чаще раза в [CHECK_EVERY_MS], при перерисовке «Главной», с ограничением по времени.
 */
object UpdateBadge {
    private const val PREFS = "update_badge"
    private const val KEY_BUILD = "build"
    private const val KEY_CHECKED = "checked_at"
    private const val CHECK_EVERY_MS = 6 * 60 * 60 * 1000L
    private const val TIMEOUT_MS = 8_000L

    fun available(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BUILD, 0) > BuildConfig.VERSION_CODE

    suspend fun refreshIfStale(context: Context, updater: AppUpdater, now: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (now - prefs.getLong(KEY_CHECKED, 0) < CHECK_EVERY_MS) return
        val build = try {
            withTimeoutOrNull(TIMEOUT_MS) { updater.check()?.build ?: 0 }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return
        prefs.edit().putInt(KEY_BUILD, build).putLong(KEY_CHECKED, now).apply()
    }
}
