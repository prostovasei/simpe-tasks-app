package com.prostovasei.simpletasks.notifications

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant

/**
 * «Точное время напоминаний» (0.29.2): включено — как будильник (система не откладывает), выключено — обычный точный.
 *
 * Простое `Application` вместо `TasksApp` (T-82, причина найдена 07.11.2026): `TasksApp.onCreate` запускает `ReminderManager.start()`
 * в фоне, и тот на пустой базе ставит `setAt(null)` — снимает будильник теста. Если фон успевал между «поставили» и «проверили»,
 * тест падал (чаще на медленной автосборке). Этому тесту нужен только `AndroidReminderAlarm`, без запуска приложения.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AndroidReminderAlarmTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val prefs = context.getSharedPreferences("reminders", android.content.Context.MODE_PRIVATE)

    /** Тест не зависит от порядка и от соседей: настройка «как будильник» по умолчанию включена, чужих будильников нет, право на точные есть. */
    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AndroidReminderAlarm(context).setAt(null)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
        ShadowAlarmManager.reset()
    }

    @Test
    fun setsAlarmClockAndCancels() {
        val at = Instant.parse("2099-10-26T15:00:00Z")
        AndroidReminderAlarm(context).setAt(at)

        val alarm = alarms.reminderAlarm()
        assertNotNull("Будильник напоминаний не поставлен; все: ${alarms.scheduledAlarms}", alarm)
        assertNotNull("Включено «как будильник» — нужен AlarmClockInfo", alarm!!.alarmClockInfo)
        assertEquals(at.toEpochMilli(), alarm.alarmClockInfo!!.triggerTime)

        AndroidReminderAlarm(context).setAt(null)
        assertNull("Будильник не снят; все: ${alarms.scheduledAlarms}", alarms.reminderAlarm())
    }

    @Test
    fun exactOffSetsPlainExactAlarm() {
        prefs.edit().putBoolean("exact_alarm_clock", false).commit()
        val at = Instant.parse("2099-10-26T15:00:00Z")
        AndroidReminderAlarm(context).setAt(at)

        val alarm = alarms.reminderAlarm()
        assertEquals(at.toEpochMilli(), alarm!!.triggerAtTime)
        assertNull(alarm.alarmClockInfo)
    }
}
