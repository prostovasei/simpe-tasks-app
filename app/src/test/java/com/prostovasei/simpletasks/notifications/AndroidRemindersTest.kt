package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.core.time.SystemTimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.repository.RoomProjectRepository
import com.prostovasei.simpletasks.data.repository.RoomTaskRepository
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class AndroidRemindersTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    private val popup = ReminderPopup(app, FakeTaskRepository(), FakeTimeProvider(now = Instant.parse("2026-10-14T05:00:00Z"), zone = ZoneOffset.UTC), CoroutineScope(Dispatchers.Unconfined))

    private fun notifications() = AndroidReminderNotifications(
        app,
        FakeTimeProvider(now = Instant.parse("2026-10-14T05:00:00Z"), zone = ZoneOffset.UTC),
        FakeProjectRepository(),
        popup,
    )

    @Test
    fun notificationHasTitleTimeAndCompleteButton() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifications().show(task("t1", title = "Позвонить маме", due = TaskDue.AtTime(Instant.parse("2026-10-14T18:30:00Z"))))

        val posted = shadowOf(notificationManager).allNotifications.single()
        val shadow = shadowOf(posted)
        assertEquals("Позвонить маме", shadow.contentTitle)
        assertEquals("Итерация 27: только срок, без списка", "Сегодня, 18:30", shadow.contentText)
        assertEquals("Итерация 49: «Не буду делать» посередине", listOf("Выполнено", "Не буду делать", "Отложить"), posted.actions.map { it.title.toString() })
        assertEquals(AndroidReminderNotifications.CHANNEL_ID, posted.channelId)
        assertEquals("#296: иконка задач", com.prostovasei.simpletasks.R.drawable.ic_notification_tasks, posted.smallIcon.resId)
        val channel = notificationManager.getNotificationChannel(AndroidReminderNotifications.CHANNEL_ID)
        assertTrue("Вибрация включена", channel.shouldVibrate())
        assertEquals(AndroidReminderNotifications.VIBRATION.toList(), channel.vibrationPattern.toList())

        val open = shadowOf(posted.contentIntent).savedIntent
        assertEquals("Задача открывается поверх, без приложения", com.prostovasei.simpletasks.TaskPopupActivity::class.java.name, open.component?.className)
        assertEquals("t1", open.getStringExtra(MainActivity.EXTRA_TASK_ID))
        val complete = shadowOf(posted.actions[0].actionIntent).savedIntent
        assertEquals(ReminderReceiver.ACTION_COMPLETE, complete.action)
        assertEquals("t1", complete.getStringExtra(ReminderReceiver.EXTRA_TASK_ID))
        val wontDo = shadowOf(posted.actions[1].actionIntent).savedIntent
        assertEquals(ReminderReceiver.ACTION_WONT_DO, wontDo.action)
        assertEquals("t1", wontDo.getStringExtra(ReminderReceiver.EXTRA_TASK_ID))
        // Итерация 53: «Отложить» открывает окно выбора поверх экрана (меню в самом уведомлении Android не показывает).
        val snooze = shadowOf(posted.actions[2].actionIntent).savedIntent
        assertEquals(com.prostovasei.simpletasks.TaskPopupActivity::class.java.name, snooze.component?.className)
        assertEquals("t1", snooze.getStringExtra(MainActivity.EXTRA_SNOOZE_TASK))

        notifications().cancel("t1")
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun popupReplacesNotificationWhenEnabledAndAllowed() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PopupPrefs.setEnabled(app, true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
        try {
            notifications().show(task("t1", title = "Забрать посылку", due = TaskDue.AtTime(Instant.parse("2026-10-14T18:00:00Z"))))
            // T-69: только полоска, обычного уведомления нет (0.30.1 — и без вибрации).
            assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
            assertEquals(listOf("t1"), popup.shown())
            // 0.30.0: пока полоска на экране, приложение не засыпает (сервис переднего плана).
            assertEquals(PopupService::class.java.name, shadowOf(app).nextStartedService?.component?.className)

            notifications().cancel("t1")
            assertTrue("Выполнили в другом месте — полоска уходит", popup.shown().isEmpty())
            assertEquals(PopupService::class.java.name, shadowOf(app).nextStoppedService?.component?.className)
        } finally {
            PopupPrefs.setEnabled(app, false)
            org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
        }
    }

    @Test
    fun withoutOverlayPermissionNotificationIsShown() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        PopupPrefs.setEnabled(app, true)
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
        try {
            notifications().show(task("t1", due = TaskDue.AtTime(Instant.parse("2026-10-14T18:00:00Z"))))
            assertEquals(1, shadowOf(notificationManager).allNotifications.size)
            assertTrue(popup.shown().isEmpty())
        } finally {
            PopupPrefs.setEnabled(app, false)
        }
    }

    @Test
    fun withoutPermissionNothingIsShown() = runBlocking {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifications().show(task("t1", due = TaskDue.AllDay(java.time.LocalDate.of(2026, 10, 14))))
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun alarmIsScheduledAndCancelled() {
        val alarm = AndroidReminderAlarm(app)
        val at = Instant.parse("2030-01-01T10:00:00Z")
        alarm.setAt(at)
        val scheduled = shadowOf(app.getSystemService(AlarmManager::class.java)).reminderAlarm()
        assertEquals(at.toEpochMilli(), scheduled!!.triggerAtMs)
        alarm.setAt(null)
        assertEquals(null, shadowOf(app.getSystemService(AlarmManager::class.java)).reminderAlarm())
    }

    /** Системное событие (перезагрузка) доходит до приложения: зависимости внедрены, будильник поставлен. */
    @Test
    fun bootBroadcastSchedulesNearestReminder() {
        val due = Instant.now().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        // Та же база, что у приложения: задача появляется «до перезагрузки». Приложение в это время само
        // открывает базу при запуске — при «database is locked» пробуем ещё раз.
        val time = SystemTimeProvider()
        val ids = UuidIdGenerator()
        val db = AppDatabase.build(app, time)
        retryWhileLocked {
            runBlocking {
                val inbox = RoomProjectRepository(db, time, ids).getInbox().id
                RoomTaskRepository(db, time, ids).createTask(NewTask(inbox, "Созвон", due = TaskDue.AtTime(due), reminders = listOf("-PT30M")))
            }
        }
        db.close()

        app.sendBroadcast(Intent(app, ReminderReceiver::class.java).setAction(Intent.ACTION_BOOT_COMPLETED))
        shadowOf(Looper.getMainLooper()).idle()

        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
        val deadline = System.currentTimeMillis() + 10_000
        while (alarms.reminderAlarm()?.triggerAtMs != due.minus(Duration.ofMinutes(30)).toEpochMilli() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertNotNull(alarms.reminderAlarm())
        assertEquals(due.minus(Duration.ofMinutes(30)).toEpochMilli(), alarms.reminderAlarm()!!.triggerAtMs)
    }

    private fun retryWhileLocked(block: () -> Unit) {
        repeat(50) {
            try {
                return block()
            } catch (_: android.database.sqlite.SQLiteDatabaseLockedException) {
                Thread.sleep(100)
            }
        }
        block()
    }
}
