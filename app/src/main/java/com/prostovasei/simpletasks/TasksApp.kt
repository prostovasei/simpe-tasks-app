package com.prostovasei.simpletasks

import android.app.Application
import com.prostovasei.simpletasks.notifications.ReminderManager
import com.prostovasei.simpletasks.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject lateinit var reminders: ReminderManager

    @Inject lateinit var widgets: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        reminders.start()
        widgets.start()
    }
}
