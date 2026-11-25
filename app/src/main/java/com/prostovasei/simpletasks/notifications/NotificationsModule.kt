package com.prostovasei.simpletasks.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    abstract fun bindAlarm(impl: AndroidReminderAlarm): ReminderAlarm

    @Binds
    abstract fun bindNotifications(impl: AndroidReminderNotifications): ReminderNotifications

    @Binds
    abstract fun bindStore(impl: PrefsReminderStore): ReminderStore

    @Binds
    abstract fun bindWeighIn(impl: AndroidWeighInReminder): WeighInReminder

    @Binds
    abstract fun bindHabits(impl: AndroidHabitReminder): HabitReminder
}
