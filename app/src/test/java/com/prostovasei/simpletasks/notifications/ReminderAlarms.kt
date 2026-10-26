package com.prostovasei.simpletasks.notifications

import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** Будильник напоминаний среди всех (рядом может стоять будильник утренней сводки, итерация 30). */
internal fun ShadowAlarmManager.reminderAlarm(): ShadowAlarmManager.ScheduledAlarm? =
    scheduledAlarms.filter { shadowOf(it.operation).savedIntent.action == ReminderReceiver.ACTION_FIRE }.minByOrNull { it.triggerAtMs }
