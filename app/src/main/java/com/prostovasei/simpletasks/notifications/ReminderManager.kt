package com.prostovasei.simpletasks.notifications

import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Будильник на ближайшее напоминание. */
interface ReminderAlarm {
    /** null — отменить будильник. */
    fun setAt(at: Instant?)
}

/** Показ уведомлений. */
interface ReminderNotifications {
    suspend fun show(task: Task)
    fun cancel(taskId: String)
}

/** Когда напоминания проверялись в последний раз. */
interface ReminderStore {
    var lastCheckedAt: Instant?
}

/**
 * Напоминания (Итерация 05b). Один будильник AlarmManager стоит на ближайшее напоминание; когда он
 * срабатывает, [check] показывает всё, что наступило с прошлой проверки, и ставит следующий.
 * - Задачи изменились → будильник переставляется ([start]).
 * - Телефон перезагрузили, сменили время или пояс → [check] (пропущенное за последние сутки покажется).
 */
@Singleton
class ReminderManager @Inject constructor(
    private val tasks: TaskRepository,
    private val time: TimeProvider,
    private val alarm: ReminderAlarm,
    private val notifications: ReminderNotifications,
    private val store: ReminderStore,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    private val mutex = Mutex()

    /** Запуск приложения: проверка пропущенного и слежение за изменениями задач. */
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            check()
            tasks.observeWithReminders().debounce(DEBOUNCE_MS).collect { reschedule(it) }
        }
    }

    suspend fun check() = mutex.withLock {
        val now = time.now()
        val from = maxOf(store.lastCheckedAt ?: now, now.minus(MAX_MISSED))
        val active = tasks.observeWithReminders().first()
        ReminderPlanner.due(active, time.zone(), after = from, until = now).forEach { (task, _) -> notifications.show(task) }
        store.lastCheckedAt = now
        alarm.setAt(ReminderPlanner.next(active, time.zone(), after = now))
    }

    private suspend fun reschedule(active: List<Task>) = mutex.withLock {
        alarm.setAt(ReminderPlanner.next(active, time.zone(), after = time.now()))
    }

    /** «Выполнить» из уведомления. */
    suspend fun completeFromNotification(taskId: String) {
        notifications.cancel(taskId)
        tasks.complete(taskId)
    }

    /** «На завтра» из уведомления. */
    suspend fun moveToTomorrowFromNotification(taskId: String) {
        notifications.cancel(taskId)
        tasks.moveToTomorrow(taskId)
    }

    private companion object {
        const val DEBOUNCE_MS = 300L

        /** Телефон был выключен дольше — старые напоминания уже не нужны. */
        val MAX_MISSED: Duration = Duration.ofDays(1)
    }
}
