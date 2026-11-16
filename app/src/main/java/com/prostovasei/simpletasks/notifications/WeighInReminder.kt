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
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.repository.HealthRepository
import com.prostovasei.simpletasks.domain.repository.HealthSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Напоминание взвеситься (#257): ставит будильник по настройкам «Здоровья». Отдельный интерфейс — чтобы тесты не трогали Android. */
interface WeighInReminder {
    /** Ставит будильник на ближайшее время напоминания или снимает его, если напоминание выключено. */
    fun schedule()
}

/**
 * Ежедневное напоминание взвеситься (#257, решение PM 16.11.2026): в выбранное время (по умолчанию 8:00) — уведомление «Пора взвеситься»,
 * только если на сегодня записи веса ещё нет. Нажатие открывает окно ввода веса. По умолчанию выключено.
 */
@Singleton
class AndroidWeighInReminder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: HealthSettingsStore,
    private val health: HealthRepository,
    private val time: TimeProvider,
) : WeighInReminder {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun schedule() {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_WEIGH),
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

    /** Время напоминания: показать уведомление, если сегодня веса нет, и поставить следующее на завтра. */
    suspend fun fire() {
        if (store.settings.value.reminderOn && !health.hasEntry(time.today())) show()
        schedule()
    }

    private suspend fun show() {
        if (!canPost()) return
        val last = health.observeEntries().first().lastOrNull()
        val text = if (last == null) {
            context.getString(R.string.weigh_in_text_first)
        } else {
            context.getString(R.string.weigh_in_text, Weight.format(last.tenths))
        }
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_HEALTH_WEIGH, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.weigh_in_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.weigh_in_action), open)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.weigh_in_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "weigh_in"
        const val NOTIFICATION_ID = Int.MAX_VALUE - 7
        private const val REQUEST_CODE = 2
    }
}
