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
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.AllScreenTasks
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Утренняя сводка и озвучка плана дня (итерация 30, решения PM): в 8:00 (время меняется в настройках) —
 * уведомление «План на сегодня · 5 задач» со списком и кнопкой «Озвучить». Голос — только по кнопке,
 * сам в 8:00 не говорит. Пустой день — без уведомления.
 */

/** План дня: задачи со временем по порядку, без времени и сколько просрочено. Выполненные не считаются. */
data class DayPlan(val timed: List<TimedTask>, val allDay: List<String>, val overdue: Int) {
    data class TimedTask(val time: String, val title: String)

    val total: Int get() = timed.size + allDay.size
    val isEmpty: Boolean get() = total == 0 && overdue == 0

    companion object {
        private val TIME = DateTimeFormatter.ofPattern("HH:mm")

        fun of(all: AllScreenTasks, zone: ZoneId): DayPlan {
            fun List<Task>.open() = filterNot(Task::isCompleted)
            val timed = all.todayTimed.open()
                .mapNotNull { task -> ((task.start ?: task.due) as? TaskDue.AtTime)?.instant?.let { it to task } }
                .sortedBy { it.first }
                .map { (at, task) -> TimedTask(at.atZone(zone).format(TIME), task.title) }
            return DayPlan(timed, all.todayAllDay.open().map(Task::title), all.overdue.open().size)
        }
    }
}

/** Включена ли сводка и во сколько (минута дня). По умолчанию — каждый день в 8:00. */
object SummaryPrefs {
    private const val PREFS = "reminders"
    private const val KEY_ON = "summary_enabled"
    private const val KEY_MINUTE = "summary_minute"
    const val DEFAULT_MINUTE = 8 * 60

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ON, true)

    fun minute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, DEFAULT_MINUTE)

    /** Сохраняет и сразу переставляет будильник сводки. */
    fun set(context: Context, enabled: Boolean, minute: Int) {
        prefs(context).edit {
            putBoolean(KEY_ON, enabled)
            putInt(KEY_MINUTE, minute)
        }
        context.sendBroadcast(Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE))
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

@Singleton
class DaySummary @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val time: TimeProvider,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Ставит будильник на ближайшее время сводки (сегодня, если ещё не прошло, иначе завтра) или снимает его. */
    fun schedule() {
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SUMMARY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (!SummaryPrefs.enabled(context)) {
            alarms.cancel(intent)
            return
        }
        val at = nextAt(time.now(), time.zone(), SummaryPrefs.minute(context)).toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    /** Время сводки: показать уведомление и поставить следующую на завтра. */
    suspend fun fire() {
        show(plan())
        schedule()
    }

    suspend fun plan(): DayPlan = DayPlan.of(tasks.observeAllScreen(time.today()).first(), time.zone())

    private fun show(plan: DayPlan) {
        if (plan.isEmpty || !canPost()) return
        ensureChannel()
        val res = context.resources
        val first = plan.timed.firstOrNull()?.let { context.getString(R.string.summary_first_timed, it.title, it.time) }
            ?: plan.allDay.firstOrNull()?.let { context.getString(R.string.summary_first, it) }
        val overdue = plan.overdue.takeIf { it > 0 }?.let { res.getQuantityString(R.plurals.summary_overdue, it, it) }
        val lines = plan.timed.map { "${it.time}  ${it.title}" } + plan.allDay.map { "•  $it" } + listOfNotNull(overdue)
        val speak = PendingIntent.getForegroundService(
            context,
            REQUEST_CODE,
            Intent(context, SpeechService::class.java).setAction(SpeechService.ACTION_SPEAK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(context, REQUEST_CODE, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.summary_title, res.getQuantityString(R.plurals.summary_tasks, plan.total, plan.total)))
            .setContentText(listOfNotNull(first, overdue).joinToString(" · "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.summary_speak), speak)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Что прочитает голос — по фразе: приветствие, задачи со временем, без времени, просроченные. */
    fun speech(plan: DayPlan, hour: Int): List<String> {
        val res = context.resources
        val greeting = context.getString(
            when {
                hour < NOON -> R.string.speech_morning
                hour < EVENING -> R.string.speech_day
                else -> R.string.speech_evening
            },
        )
        val head = if (plan.total == 0) context.getString(R.string.speech_no_tasks) else context.getString(R.string.speech_total, res.getQuantityString(R.plurals.summary_tasks, plan.total, plan.total))
        return buildList {
            add("$greeting $head")
            plan.timed.forEach { add(context.getString(R.string.speech_timed, it.time, it.title)) }
            if (plan.allDay.isNotEmpty()) add(context.getString(R.string.speech_all_day, plan.allDay.joinToString(", ")))
            if (plan.overdue > 0) add(context.getString(R.string.speech_overdue, res.getQuantityString(R.plurals.summary_tasks, plan.overdue, plan.overdue)))
            add(context.getString(R.string.speech_bye))
        }
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.summary_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "day_summary"
        const val NOTIFICATION_ID = Int.MAX_VALUE - 1
        private const val REQUEST_CODE = 1
        private const val NOON = 12
        private const val EVENING = 18

        /** Ближайший момент [minute] дня после [now]: сегодня, если ещё не наступил, иначе завтра. */
        fun nextAt(now: Instant, zone: ZoneId, minute: Int): Instant {
            val local = now.atZone(zone)
            val today = local.toLocalDate().atTime(LocalTime.of(minute / 60, minute % 60)).atZone(zone)
            return (if (today.isAfter(local)) today else today.plusDays(1)).toInstant()
        }
    }
}
