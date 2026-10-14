package com.prostovasei.simpletasks.notifications

import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.Task
import java.time.Instant
import java.time.ZoneId

/** Какие напоминания сработали и когда следующее. Без Android — проверяется обычными тестами. */
object ReminderPlanner {

    /**
     * Задачи, у которых напоминание пришлось на промежуток ([after], [until]]. Напоминание, время которого
     * прошло до последней правки задачи, не показывается: новая задача на сегодня, созданная в 15:00,
     * не напомнит «в 10:00» задним числом.
     */
    fun due(tasks: List<Task>, zone: ZoneId, after: Instant, until: Instant): List<Pair<Task, Instant>> =
        tasks.mapNotNull { task ->
            Reminders.instants(task, zone)
                .filter { it > after && it > task.updatedAt && it <= until }
                .maxOrNull()
                ?.let { task to it }
        }

    /** Ближайшее напоминание после [after]; null — будильник не нужен. */
    fun next(tasks: List<Task>, zone: ZoneId, after: Instant): Instant? =
        tasks.asSequence().flatMap { Reminders.instants(it, zone) }.filter { it > after }.minOrNull()
}
