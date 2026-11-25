package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.habits.HabitMath
import com.prostovasei.simpletasks.domain.repository.HabitRepository
import com.prostovasei.simpletasks.domain.repository.HabitSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Напоминание о привычках (#340): будильник по настройкам модуля. Отдельный интерфейс — чтобы тесты не трогали Android. */
interface HabitReminder {
    fun schedule()
}

/**
 * Ежедневное напоминание (#340, ТЗ §8): в выбранное время (по умолчанию 21:00, выключено) — «Привычки: осталось N из M»,
 * только если на сегодня остались неотмеченные привычки. Нажатие открывает раздел «Привычки».
 */
@Singleton
class AndroidHabitReminder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: HabitSettingsStore,
    private val habits: HabitRepository,
    private val time: TimeProvider,
) : HabitReminder {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule() {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_HABITS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val settings = store.settings.value
        if (!settings.reminderOn) {
            alarms.cancel(intent)
            return
        }
        val at = DaySummary.nextAt(time.now(), time.zone(), settings.reminderMinute).toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    /** Время напоминания: показать уведомление, если на сегодня что-то не отмечено, и поставить следующее. */
    suspend fun fire() {
        if (store.settings.value.reminderOn) {
            val today = time.today()
            val due = habits.observeHabits().first().filter { !it.archived && HabitMath.dueOn(it, today) }
            val left = due.count { !it.isDone(today) }
            if (left > 0) show(left, due.size)
        }
        schedule()
    }

    private fun show(left: Int, total: Int) {
        if (!canPost()) return
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_HABITS, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_module_habits)
            .setContentTitle(context.getString(R.string.module_habits))
            .setContentText(context.getString(R.string.habits_reminder_text, left, total))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.habits_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "habits"
        const val NOTIFICATION_ID = Int.MAX_VALUE - 11
        private const val REQUEST_CODE = 11
    }
}
