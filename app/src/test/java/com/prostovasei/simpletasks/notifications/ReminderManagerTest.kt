package com.prostovasei.simpletasks.notifications

import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderManagerTest {
    private val time = FakeTimeProvider(now = Instant.parse("2026-10-14T09:00:00Z"), zone = ZoneOffset.UTC)
    private val repository = FakeTaskRepository()
    private val alarm = object : ReminderAlarm {
        var scheduled: Instant? = null
        override fun setAt(at: Instant?) { scheduled = at }
    }
    private val shown = mutableListOf<String>()
    private val cancelled = mutableListOf<String>()
    private val notifications = object : ReminderNotifications {
        override suspend fun show(task: Task) { shown += task.id }
        override fun cancel(taskId: String) { cancelled += taskId }
    }
    private val store = object : ReminderStore {
        override var lastCheckedAt: Instant? = null
    }

    private fun TestScope.manager() = ReminderManager(repository, time, alarm, notifications, store, backgroundScope)
    /** Фоновые задачи (backgroundScope) не ждут advanceUntilIdle — двигаем время явно, с запасом на debounce. */
    private fun TestScope.settle() {
        advanceTimeBy(1_000)
        runCurrent()
    }

    private fun at(text: String) = TaskDue.AtTime(Instant.parse(text))

    @Test
    fun alarmIsSetForNearestReminder() = runTest {
        repository.tasks.value = listOf(
            task("call", due = at("2026-10-14T12:00:00Z")).copy(reminders = listOf("PT0S", "-PT30M")),
            task("later", due = at("2026-10-15T12:00:00Z")).copy(reminders = listOf("PT0S")),
            task("past", due = at("2026-10-14T08:00:00Z")).copy(reminders = listOf("PT0S")),
        )
        manager().check()
        assertEquals(Instant.parse("2026-10-14T11:30:00Z"), alarm.scheduled)
        assertEquals("Первая проверка не показывает старое", emptyList<String>(), shown)
    }

    @Test
    fun alarmShowsWhatCameSinceLastCheck() = runTest {
        repository.tasks.value = listOf(
            task("call", due = at("2026-10-14T12:00:00Z")).copy(reminders = listOf("-PT30M")),
            task("trip", due = TaskDue.AllDay(LocalDate.of(2026, 10, 14))).copy(reminders = listOf("P0DT10H0M0S")),
            task("tomorrow", due = at("2026-10-15T12:00:00Z")).copy(reminders = listOf("PT0S")),
        )
        val manager = manager()
        manager.check()

        time.now = Instant.parse("2026-10-14T11:30:00Z")
        manager.check()
        assertEquals(listOf("trip", "call"), shown.sorted().reversed())
        assertEquals(Instant.parse("2026-10-15T12:00:00Z"), alarm.scheduled)

        manager.check()
        assertEquals("Повторная проверка ничего не дублирует", 2, shown.size)
    }

    @Test
    fun missedRemindersOlderThanADayAreDropped() = runTest {
        store.lastCheckedAt = Instant.parse("2026-10-10T00:00:00Z")
        repository.tasks.value = listOf(
            task("old", due = at("2026-10-11T12:00:00Z")).copy(reminders = listOf("PT0S")),
            task("fresh", due = at("2026-10-14T06:00:00Z")).copy(reminders = listOf("PT0S")),
        )
        manager().check()
        assertEquals(listOf("fresh"), shown)
    }

    @Test
    fun reminderBeforeLastEditIsNotShown() = runTest {
        store.lastCheckedAt = Instant.parse("2026-10-14T06:00:00Z")
        repository.tasks.value = listOf(
            task("new", due = TaskDue.AllDay(LocalDate.of(2026, 10, 14))).copy(
                reminders = listOf("P0DT7H0M0S"),
                updatedAt = Instant.parse("2026-10-14T08:00:00Z"),
            ),
        )
        manager().check()
        assertEquals(emptyList<String>(), shown)
    }

    @Test
    fun noRemindersNoAlarm() = runTest {
        alarm.scheduled = Instant.EPOCH
        manager().check()
        assertNull(alarm.scheduled)
    }

    @Test
    fun alarmFollowsTaskChanges() = runTest {
        val manager = manager()
        manager.start()
        settle()
        assertNull(alarm.scheduled)

        repository.tasks.value = listOf(task("call", due = at("2026-10-14T12:00:00Z")).copy(reminders = listOf("PT0S")))
        settle()
        assertEquals(Instant.parse("2026-10-14T12:00:00Z"), alarm.scheduled)

        repository.tasks.value = emptyList()
        settle()
        assertNull(alarm.scheduled)
    }

    @Test
    fun notificationButtons() = runTest {
        repository.tasks.value = listOf(task("call", due = at("2026-10-14T12:00:00Z")))
        val manager = manager()
        manager.completeFromNotification("call")
        manager.moveToTomorrowFromNotification("call")
        assertEquals(listOf("call", "call"), cancelled)
        assertEquals(listOf("call" to true), repository.completedCalls)
        assertEquals(listOf("call"), repository.movedToTomorrow)
    }

    @Test
    fun plannerIgnoresTasksWithoutDue() {
        val tasks = listOf(task("x").copy(reminders = listOf("PT0S")))
        assertNull(ReminderPlanner.next(tasks, ZoneOffset.UTC, Instant.EPOCH))
        assertEquals(emptyList<Any>(), ReminderPlanner.due(tasks, ZoneOffset.UTC, Instant.EPOCH, Instant.EPOCH.plus(Duration.ofDays(99999))))
    }
}
