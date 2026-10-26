package com.prostovasei.simpletasks.notifications

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant

/** «Точное время напоминаний» (0.29.2): включено — как будильник (система не откладывает), выключено — обычный точный. */
@RunWith(RobolectricTestRunner::class)
class AndroidReminderAlarmTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))

    @Test
    fun setsAlarmClockAndCancels() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val at = Instant.parse("2026-10-26T15:00:00Z")
        AndroidReminderAlarm(context).setAt(at)

        val alarm = alarms.peekNextScheduledAlarm()
        assertNotNull(alarm)
        assertEquals(at.toEpochMilli(), alarm!!.alarmClockInfo!!.triggerTime)

        AndroidReminderAlarm(context).setAt(null)
        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun exactOffSetsPlainExactAlarm() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        context.getSharedPreferences("reminders", android.content.Context.MODE_PRIVATE).edit().putBoolean("exact_alarm_clock", false).commit()
        val at = Instant.parse("2026-10-26T15:00:00Z")
        AndroidReminderAlarm(context).setAt(at)

        val alarm = alarms.peekNextScheduledAlarm()
        assertEquals(at.toEpochMilli(), alarm!!.triggerAtTime)
        assertNull(alarm.alarmClockInfo)
    }
}
