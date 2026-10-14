package com.prostovasei.simpletasks

import android.app.Application
import com.prostovasei.simpletasks.notifications.ReminderManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject lateinit var reminders: ReminderManager

    override fun onCreate() {
        super.onCreate()
        reminders.start()
    }
}
