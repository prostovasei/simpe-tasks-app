package com.prostovasei.simpletasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prostovasei.simpletasks.core.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Будильник напоминаний и утренней сводки, кнопки уведомления и системные события (перезагрузка, смена времени
 * или часового пояса, обновление приложения).
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reminders: ReminderManager

    @Inject lateinit var summary: DaySummary

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    // Вызов super.onReceive (внедрение зависимостей) добавляет плагин Hilt.
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val pending = goAsync()
        appScope.launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> taskId?.let { reminders.completeFromNotification(it) }
                    ACTION_TOMORROW -> taskId?.let { reminders.moveToTomorrowFromNotification(it) }
                    ACTION_SUMMARY -> summary.fire()
                    else -> {
                        reminders.check()
                        summary.schedule()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.prostovasei.simpletasks.REMINDER"
        const val ACTION_COMPLETE = "com.prostovasei.simpletasks.REMINDER_COMPLETE"
        const val ACTION_TOMORROW = "com.prostovasei.simpletasks.REMINDER_TOMORROW"
        const val ACTION_SUMMARY = "com.prostovasei.simpletasks.DAY_SUMMARY"
        const val EXTRA_TASK_ID = "task_id"
    }
}
