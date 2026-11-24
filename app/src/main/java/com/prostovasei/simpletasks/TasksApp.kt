package com.prostovasei.simpletasks

import android.app.Application
import com.prostovasei.simpletasks.notifications.DaySummary
import com.prostovasei.simpletasks.notifications.NewsChecks
import com.prostovasei.simpletasks.notifications.ReminderManager
import com.prostovasei.simpletasks.notifications.WeighInReminder
import com.prostovasei.simpletasks.widget.WidgetUpdater
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.update.UpdateFiles
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject lateinit var reminders: ReminderManager

    @Inject lateinit var widgets: WidgetUpdater

    @Inject lateinit var summary: DaySummary

    @Inject lateinit var weighIn: WeighInReminder

    @Inject lateinit var budget: com.prostovasei.simpletasks.notifications.BudgetAlerts

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        reminders.start()
        widgets.start()
        summary.schedule()
        weighIn.schedule()
        budget.start()
        NewsChecks.schedule(this)
        // #312: скачанные раньше обновления (~19 МБ каждое) больше не нужны — установщик их уже скопировал.
        appScope.launch(Dispatchers.IO) { UpdateFiles.clear(cacheDir) }
    }
}
