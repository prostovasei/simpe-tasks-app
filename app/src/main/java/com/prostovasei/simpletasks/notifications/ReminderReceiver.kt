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

    @Inject lateinit var weighIn: AndroidWeighInReminder

    @Inject lateinit var habits: AndroidHabitReminder

    @Inject lateinit var budget: BudgetAlerts

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    // Вызов super.onReceive (внедрение зависимостей) добавляет плагин Hilt.
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        PopupTrace.add(context, "будильник: ${intent.action?.substringAfterLast('.')}")
        val pending = goAsync()
        appScope.launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> taskId?.let { reminders.completeFromNotification(it) }
                    ACTION_TOMORROW -> taskId?.let { reminders.moveToTomorrowFromNotification(it) }
                    ACTION_WONT_DO -> taskId?.let { reminders.wontDoFromNotification(it) }
                    ACTION_SUMMARY -> summary.fire()
                    ACTION_WEIGH -> weighIn.fire()
                    ACTION_HABITS -> habits.fire()
                    ACTION_BUDGET_MONTH -> budget.fireMonth()
                    ACTION_BUDGET_KEEP -> intent.getStringExtra(BudgetAlerts.EXTRA_MONTH)?.let { budget.keep(java.time.YearMonth.parse(it)) }
                    else -> {
                        reminders.check()
                        summary.schedule()
                        weighIn.schedule()
                        habits.schedule()
                        budget.schedule()
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
        const val ACTION_WONT_DO = "com.prostovasei.simpletasks.REMINDER_WONT_DO"
        const val ACTION_SUMMARY = "com.prostovasei.simpletasks.DAY_SUMMARY"
        const val ACTION_WEIGH = "com.prostovasei.simpletasks.WEIGH_IN"
        const val ACTION_HABITS = "com.prostovasei.simpletasks.HABITS"

        /** #327: «Бюджет на следующий месяц» и его кнопка «Оставить». */
        const val ACTION_BUDGET_MONTH = "com.prostovasei.simpletasks.BUDGET_MONTH"
        const val ACTION_BUDGET_KEEP = "com.prostovasei.simpletasks.BUDGET_KEEP"
        const val EXTRA_TASK_ID = "task_id"
    }
}
