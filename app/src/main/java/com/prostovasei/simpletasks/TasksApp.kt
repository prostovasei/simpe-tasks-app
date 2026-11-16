package com.prostovasei.simpletasks

import android.app.Application
import com.prostovasei.simpletasks.notifications.DaySummary
import com.prostovasei.simpletasks.notifications.ReminderManager
import com.prostovasei.simpletasks.notifications.WeighInReminder
import com.prostovasei.simpletasks.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject lateinit var reminders: ReminderManager

    @Inject lateinit var widgets: WidgetUpdater

    @Inject lateinit var summary: DaySummary

    @Inject lateinit var weighIn: WeighInReminder

    override fun onCreate() {
        super.onCreate()
        reminders.start()
        widgets.start()
        summary.schedule()
        weighIn.schedule()
    }
}
