package com.prostovasei.simpletasks.domain.model

import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Следующая дата повтора. 14.10.2026 — среда. */
class RecurrenceTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val utc = ZoneId.of("UTC")
    private fun day(m: Int, d: Int) = TaskDue.AllDay(LocalDate.of(2026, m, d))
    private fun at(zone: ZoneId, m: Int, d: Int, h: Int, min: Int) =
        TaskDue.AtTime(LocalDateTime.of(2026, m, d, h, min).atZone(zone).toInstant())

    private fun next(rule: String, anchor: TaskDue, zone: ZoneId = utc) = Recurrence.next(rule, anchor, zone, today)

    @Test
    fun simpleRulesFromTickTick() {
        assertEquals(day(10, 15), next("FREQ=WEEKLY;WKST=MO;INTERVAL=1;BYDAY=TU,TH", day(10, 13))?.anchor)
        assertEquals(day(11, 14), next("FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", day(10, 14))?.anchor)
        assertEquals(TaskDue.AllDay(LocalDate.of(2027, 6, 5)), next("FREQ=YEARLY;INTERVAL=1;BYMONTH=6;BYMONTHDAY=5", day(6, 5))?.anchor)
        assertEquals(day(11, 14), next("FREQ=MONTHLY;INTERVAL=1", day(10, 14))?.anchor)
        assertEquals(day(10, 27), next("FREQ=WEEKLY;WKST=MO;INTERVAL=2;BYDAY=TU", day(10, 13))?.anchor)
        assertEquals(day(10, 15), next("FREQ=DAILY", day(10, 14))?.anchor)
        assertEquals(day(11, 27), next("FREQ=MONTHLY;BYDAY=-1FR", day(10, 30))?.anchor)
    }

    @Test
    fun ruleIsKeptAsIs() {
        val rule = "FREQ=WEEKLY;WKST=MO;INTERVAL=1;BYDAY=TU,TH"
        assertEquals(rule, next(rule, day(10, 13))?.rrule)
    }

    @Test
    fun missedRepeatsAreSkipped() {
        assertEquals("Ежедневная, просроченная на 2 недели → сегодня", day(10, 14), next("FREQ=DAILY", day(9, 30))?.anchor)
        assertEquals("Еженедельная по вторникам → ближайший вторник", day(10, 20), next("FREQ=WEEKLY;BYDAY=TU", day(9, 1))?.anchor)
    }

    @Test
    fun timeOfDayKeptAcrossDaylightSaving() {
        val berlin = ZoneId.of("Europe/Berlin") // 25.10.2026 — переход на зимнее время
        assertEquals(at(berlin, 10, 25, 9, 30), next("FREQ=DAILY", at(berlin, 10, 24, 9, 30), berlin)?.anchor)
    }

    @Test
    fun countGoesDownAndEnds() {
        val third = next("FREQ=WEEKLY;INTERVAL=1;COUNT=3;BYDAY=TH", day(10, 15))
        assertEquals(day(10, 22), third?.anchor)
        assertEquals("FREQ=WEEKLY;INTERVAL=1;COUNT=2;BYDAY=TH", third?.rrule)
        val last = next(third!!.rrule, third.anchor)
        assertEquals("FREQ=WEEKLY;INTERVAL=1;COUNT=1;BYDAY=TH", last?.rrule)
        assertNull("Третий раз — последний", next(last!!.rrule, last.anchor))
    }

    @Test
    fun countEndsWhenAllRemainingAreMissed() {
        assertNull(next("FREQ=WEEKLY;COUNT=3;BYDAY=TU", day(9, 22)))
    }

    @Test
    fun untilEndsSeries() {
        assertEquals(day(10, 15), next("FREQ=DAILY;UNTIL=20261020", day(10, 14))?.anchor)
        assertNull(next("FREQ=WEEKLY;UNTIL=20261020", day(10, 14)))
    }

    @Test
    fun brokenRuleGivesNull() {
        assertNull(next("что-то странное", day(10, 14)))
    }

    @Test
    fun intervalMovesWhole() {
        val call = task("t", start = at(utc, 10, 14, 10, 0), due = at(utc, 10, 14, 11, 0)).copy(rrule = "FREQ=DAILY", reminders = listOf("PT0S"))
        val moved = call.nextOccurrence(utc, today)!!
        assertEquals(at(utc, 10, 15, 10, 0), moved.start)
        assertEquals(at(utc, 10, 15, 11, 0), moved.due)
        assertEquals(listOf("PT0S"), moved.reminders)

        val trip = task("t", start = day(10, 16), due = day(10, 18)).copy(rrule = "FREQ=WEEKLY;BYDAY=FR")
        val next = trip.nextOccurrence(utc, today)!!
        assertEquals(day(10, 23), next.start)
        assertEquals(day(10, 25), next.due)
    }

    @Test
    fun noRuleOrDueMeansNoNext() {
        assertNull(task("t", due = day(10, 14)).nextOccurrence(utc, today))
        assertNull(task("t").copy(rrule = "FREQ=DAILY").nextOccurrence(utc, today))
    }

    @Test
    fun upcomingRepeatsFor30Days() {
        val until = today.plusDays(30)
        val weekly = Recurrence.upcoming("FREQ=WEEKLY;BYDAY=WE", day(10, 14), utc, after = today, until = until)
        assertEquals("Еженедельная — 4 повтора", listOf(day(10, 21), day(10, 28), day(11, 4), day(11, 11)), weekly)
        assertEquals(30, Recurrence.upcoming("FREQ=DAILY", day(10, 14), utc, today, until).size)
        assertEquals("14 ноября — через 31 день, за окном", emptyList<TaskDue>(), Recurrence.upcoming("FREQ=MONTHLY;BYMONTHDAY=14", day(10, 14), utc, today, until))
        assertEquals(listOf(day(11, 13)), Recurrence.upcoming("FREQ=MONTHLY;BYMONTHDAY=13", day(10, 13), utc, today, until))
    }

    @Test
    fun upcomingRespectsCountAndSkipsPast() {
        assertEquals(
            "COUNT=3: текущий + ещё 2",
            listOf(day(10, 15), day(10, 16)),
            Recurrence.upcoming("FREQ=DAILY;COUNT=3", day(10, 14), utc, today, today.plusDays(30)),
        )
        assertEquals(
            "Просроченная ежедневная: повторы с завтрашнего дня",
            day(10, 15),
            Recurrence.upcoming("FREQ=DAILY", day(10, 1), utc, today, today.plusDays(30)).first(),
        )
    }

    @Test
    fun taskUpcomingRepeatsKeepInterval() {
        val call = task("t", start = at(utc, 10, 14, 10, 0), due = at(utc, 10, 14, 11, 0)).copy(rrule = "FREQ=WEEKLY;BYDAY=WE")
        val repeats = call.upcomingRepeats(utc, today, today.plusDays(30))
        assertEquals(4, repeats.size)
        assertEquals(at(utc, 10, 21, 10, 0), repeats.first().start)
        assertEquals(at(utc, 10, 21, 11, 0), repeats.first().due)
        assertEquals(call.id, repeats.first().id)
    }

    /** 14.10.2026 — среда, 16.10 — пятница, 17.10 — суббота. */
    @Test
    fun repeatStartsFromFirstMatchingDay() {
        assertEquals("Пятница + «каждую субботу» → суббота", day(10, 17), Recurrence.alignStart("FREQ=WEEKLY;BYDAY=SA", day(10, 16), utc))
        assertEquals("Суббота уже подходит", day(10, 17), Recurrence.alignStart("FREQ=WEEKLY;BYDAY=SA", day(10, 17), utc))
        assertEquals("Вт и пт, выбрана среда → пятница", day(10, 16), Recurrence.alignStart("FREQ=WEEKLY;BYDAY=TU,FR", day(10, 14), utc))
        assertEquals("14-го числа, выбрано 15-е → 14 ноября", day(11, 14), Recurrence.alignStart("FREQ=MONTHLY;BYMONTHDAY=14", day(10, 15), utc))
        assertEquals("Каждый день — с выбранной даты", day(10, 16), Recurrence.alignStart("FREQ=DAILY", day(10, 16), utc))
        assertEquals("Каждые 2 недели без дней — с выбранной даты", day(10, 16), Recurrence.alignStart("FREQ=WEEKLY;INTERVAL=2", day(10, 16), utc))
        assertEquals("«N раз» не тратится", day(10, 17), Recurrence.alignStart("FREQ=WEEKLY;COUNT=1;BYDAY=SA", day(10, 16), utc))
        assertEquals("Время сохраняется", at(utc, 10, 17, 9, 30), Recurrence.alignStart("FREQ=WEEKLY;BYDAY=SA", at(utc, 10, 16, 9, 30), utc))
    }

    @Test
    fun taskAlignedKeepsIntervalAndRule() {
        val call = task("t", start = at(utc, 10, 16, 10, 0), due = at(utc, 10, 16, 11, 0)).copy(rrule = "FREQ=WEEKLY;BYDAY=SA")
        val aligned = call.alignedToRepeat(utc)
        assertEquals(at(utc, 10, 17, 10, 0), aligned.start)
        assertEquals(at(utc, 10, 17, 11, 0), aligned.due)
        assertEquals("FREQ=WEEKLY;BYDAY=SA", aligned.rrule)
    }

    /** 0.51.1 (разбор PM, «Вода» 10-го числа): правило с одним днём меняется под новую дату, с несколькими — нет. */
    @Test
    fun ruleWithOneDayFollowsNewDate() {
        val nov4 = LocalDate.of(2026, 11, 4) // среда
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=4", Recurrence.retargeted("FREQ=MONTHLY;BYMONTHDAY=10", nov4))
        assertEquals("FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=4", Recurrence.retargeted("FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=10", nov4))
        assertEquals("FREQ=WEEKLY;BYDAY=WE", Recurrence.retargeted("FREQ=WEEKLY;BYDAY=SA", nov4))
        assertEquals("FREQ=YEARLY;BYMONTH=11;BYMONTHDAY=4", Recurrence.retargeted("FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=8", nov4))
        assertEquals("Несколько дней — без изменений", "FREQ=WEEKLY;BYDAY=TU,TH", Recurrence.retargeted("FREQ=WEEKLY;BYDAY=TU,TH", nov4))
        assertEquals("Без дня — без изменений", "FREQ=MONTHLY", Recurrence.retargeted("FREQ=MONTHLY", nov4))
        assertEquals("«Вторая пятница» — без изменений", "FREQ=MONTHLY;BYDAY=2FR", Recurrence.retargeted("FREQ=MONTHLY;BYDAY=2FR", nov4))
        assertEquals("COUNT сохраняется", "FREQ=MONTHLY;BYMONTHDAY=4;COUNT=3", Recurrence.retargeted("FREQ=MONTHLY;BYMONTHDAY=10;COUNT=3", nov4))
    }

    @Test
    fun seriesMovesToChosenDateInsteadOfSnappingBack() {
        val water = task("w", due = TaskDue.AllDay(LocalDate.of(2026, 11, 10))).copy(rrule = "FREQ=MONTHLY;BYMONTHDAY=10")
        val moved = water.seriesMovedTo(TaskDue.AllDay(LocalDate.of(2026, 11, 4)), null, ZoneId.of("UTC"))
        assertEquals(TaskDue.AllDay(LocalDate.of(2026, 11, 4)), moved.due)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=4", moved.rrule)
        val twice = task("t", due = TaskDue.AllDay(LocalDate.of(2026, 11, 10))).copy(rrule = "FREQ=WEEKLY;BYDAY=TU,TH")
        assertEquals("Вт и чт: ср 4 → чт 5", TaskDue.AllDay(LocalDate.of(2026, 11, 5)), twice.seriesMovedTo(TaskDue.AllDay(LocalDate.of(2026, 11, 4)), null, ZoneId.of("UTC")).due)
    }

    // ---------------------------------------------------------------- #192: отменённые по одной даты

    private fun weekly(skipped: Set<LocalDate> = emptySet(), rule: String = "FREQ=WEEKLY") = Task(
        id = "t", projectId = "p", title = "Стирка", content = "", status = TaskStatus.ACTIVE, priority = Priority.NONE,
        due = day(10, 14), start = null, completedAt = null, sortOrder = 0, rrule = rule, reminders = emptyList(), parentId = null,
        createdAt = java.time.Instant.EPOCH, updatedAt = java.time.Instant.EPOCH, skipped = skipped,
    )

    @Test
    fun upcomingRepeatsHideSkippedDates() {
        val repeats = weekly(setOf(LocalDate.of(2026, 10, 28))).upcomingRepeats(utc, today, LocalDate.of(2026, 11, 11)).map { it.due }
        assertEquals(listOf(day(10, 21), day(11, 4), day(11, 11)), repeats)
    }

    @Test
    fun nextOccurrenceSkipsCancelledDatesAndCountsThem() {
        val next = weekly(setOf(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 28), LocalDate.of(2026, 12, 2))).nextOccurrence(utc, today)!!
        assertEquals(day(11, 4), next.due)
        assertEquals("Остаются только будущие отменённые даты", setOf(LocalDate.of(2026, 12, 2)), next.skipped)
        // «3 раза»: 14 окт (текущий), 21 окт отменён, 28 окт — последний; отменённый раз тоже расходует остаток.
        val counted = weekly(setOf(LocalDate.of(2026, 10, 21)), rule = "FREQ=WEEKLY;COUNT=3").nextOccurrence(utc, today)!!
        assertEquals(day(10, 28), counted.due)
        assertNull(counted.nextOccurrence(utc, today))
    }

    @Test
    fun repeatsBeforeIgnoresSkippedDates() {
        val before = weekly(setOf(LocalDate.of(2026, 10, 21))).repeatsBefore(day(11, 4), utc, today).map { it.due }
        assertEquals(listOf(day(10, 14), day(10, 28)), before)
    }
}
