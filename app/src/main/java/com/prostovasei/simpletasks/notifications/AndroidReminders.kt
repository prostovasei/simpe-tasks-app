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
import androidx.core.content.edit
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.movedTo
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.DueFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * «Точное время напоминаний» в настройках (0.29.2, вариант PM «В»; по умолчанию включено). Включено — напоминание
 * ставится как будильник (`setAlarmClock`): система его не откладывает, но показывает значок будильника в строке
 * состояния (звонка нет). Выключено — обычный точный будильник, который Samsung откладывает на 20–30 секунд.
 */
object ExactPrefs {
    private const val PREFS = "reminders"
    private const val KEY = "exact_alarm_clock"

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    /** Сохраняет выбор и сразу переставляет ближайший будильник по-новому. */
    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY, value) }
        context.sendBroadcast(Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE))
    }
}

/** Будильник через AlarmManager: точный, срабатывает и в режиме сна; как будильник — если включено [ExactPrefs]. */
@Singleton
class AndroidReminderAlarm @Inject constructor(@param:ApplicationContext private val context: Context) : ReminderAlarm {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun setAt(at: Instant?) {
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (at == null) {
            alarms.cancel(intent)
            return
        }
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact && ExactPrefs.enabled(context)) {
            // Нажатие на «Следующий будильник» в шторке открывает приложение.
            val show = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), intent)
        } else if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        }
    }
}

/**
 * Уведомление (итерация 27, решение PM): название, срок и кнопка «Выполнить»; нажатие открывает задачу.
 * Если включена полоска поверх приложений и её можно показать — вместо уведомления полоска (T-69).
 */
@Singleton
class AndroidReminderNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val time: TimeProvider,
    private val projects: ProjectRepository,
    private val popup: ReminderPopup,
) : ReminderNotifications {
    private val manager = NotificationManagerCompat.from(context)

    override suspend fun show(task: Task) {
        if (popup.canShow()) {
            popup.show(popupStrip(task))
            return
        }
        if (!canPost()) return
        ensureChannel()
        val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
        val text = DueFormatter.format(task.due, task.start, time.today(), time.zone())?.let(days::format)
        val id = notificationId(task.id)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    id,
                    // Задача открывается поверх того, что на экране, без перехода в приложение (решение PM).
                    Intent(context, TaskPopupActivity::class.java)
                        .putExtra(MainActivity.EXTRA_TASK_ID, task.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .addAction(0, context.getString(R.string.notification_complete), action(task.id, ReminderReceiver.ACTION_COMPLETE, id))
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        manager.notify(id, notification)
    }

    override fun cancel(taskId: String) {
        manager.cancel(notificationId(taskId))
        popup.dismiss(taskId)
    }

    /** Подпись полоски (решение PM): «Сегодня, 18:00 · Дом». */
    /** Полоска: подпись «Сегодня, 18:00 · Дом» и «Завтра, 18:00» — её показывает анимация переноса на завтра. */
    private suspend fun popupStrip(task: Task): PopupStrip {
        val days = DayNames(context.getString(R.string.day_today), context.getString(R.string.day_tomorrow), context.getString(R.string.day_yesterday))
        val today = time.today()
        val zone = time.zone()
        val due = DueFormatter.format(task.due, task.start, today, zone)?.let(days::format)
        val list = projects.observeProject(task.projectId).first()?.let { if (it.isInbox) context.getString(R.string.inbox_list_name) else it.name }
        val later = DueFormatter.format(task.due.movedTo(today.plusDays(1), zone), null, today, zone)?.let(days::format).orEmpty()
        return PopupStrip(task.id, task.title, listOfNotNull(due, list).joinToString(SEPARATOR), later)
    }

    private fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /**
     * Канал «Напоминания» с вибрацией (решение PM: два коротких импульса). Настройки канала Android
     * менять не даёт, поэтому канал версии 0.6.0 без вибрации удаляется и создаётся новый.
     */
    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = VIBRATION
        }
        context.getSystemService(NotificationManager::class.java).apply {
            deleteNotificationChannel(OLD_CHANNEL_ID)
            createNotificationChannel(channel)
        }
    }

    private fun action(taskId: String, action: String, notificationId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        // У каждой задачи и кнопки свой код — иначе Android подменит одно действие другим.
        notificationId * 2 + if (action == ReminderReceiver.ACTION_COMPLETE) 0 else 1,
        Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "reminders_vibrate"
        private const val SEPARATOR = " · "
        private const val OLD_CHANNEL_ID = "reminders"

        /** Пауза, импульс, пауза, импульс (мс). */
        val VIBRATION = longArrayOf(0, 250, 150, 250)

        fun notificationId(taskId: String): Int = taskId.hashCode()
    }
}

@Singleton
class PrefsReminderStore @Inject constructor(@param:ApplicationContext context: Context) : ReminderStore {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    override var lastCheckedAt: Instant?
        get() = prefs.getLong(KEY_LAST_CHECKED, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        set(value) = prefs.edit { if (value == null) remove(KEY_LAST_CHECKED) else putLong(KEY_LAST_CHECKED, value.toEpochMilli()) }

    /** Разрешение на уведомления уже спрашивали — второй раз при запуске не спрашиваем. */
    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_ASKED, false)
        set(value) = prefs.edit { putBoolean(KEY_PERMISSION_ASKED, value) }

    private companion object {
        const val KEY_LAST_CHECKED = "last_checked_at"
        const val KEY_PERMISSION_ASKED = "permission_asked"
    }
}
