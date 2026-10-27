package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Готовые варианты окна повтора (итерация 31). 14.10.2026 — среда. */
class RepeatPresetTest {
    private val wednesday = LocalDate.of(2026, 10, 14)

    @Test
    fun ownRulesAreRecognised() {
        RepeatPreset.entries.filterNot { it == RepeatPreset.CUSTOM }.forEach { preset ->
            assertEquals(preset.name, preset, RepeatPreset.of(preset.rule(), wednesday))
        }
    }

    @Test
    fun tickTickRulesForTheTaskDateAreRecognised() {
        assertEquals(RepeatPreset.WEEKLY, RepeatPreset.of("FREQ=WEEKLY;WKST=MO;INTERVAL=1;BYDAY=WE", wednesday))
        assertEquals(RepeatPreset.MONTHLY, RepeatPreset.of("FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14", wednesday))
        assertEquals(RepeatPreset.YEARLY, RepeatPreset.of("FREQ=YEARLY;INTERVAL=1;BYMONTH=10;BYMONTHDAY=14", wednesday))
        assertEquals(RepeatPreset.WORKDAYS, RepeatPreset.of("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR", wednesday))
        assertEquals(RepeatPreset.WORKDAYS, RepeatPreset.of("FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR", wednesday))
    }

    @Test
    fun everythingElseIsCustom() {
        assertEquals("каждые 2 недели", RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=WEEKLY;INTERVAL=2", wednesday))
        assertEquals("по вторникам у задачи в среду", RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=WEEKLY;BYDAY=TU", wednesday))
        assertEquals("число не задачи", RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=MONTHLY;BYMONTHDAY=3", wednesday))
        assertEquals("последняя пятница", RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=MONTHLY;BYDAY=-1FR", wednesday))
        assertEquals("10 раз", RepeatPreset.CUSTOM, RepeatPreset.of("FREQ=DAILY;COUNT=10", wednesday))
        assertEquals("не RRULE", RepeatPreset.CUSTOM, RepeatPreset.of("что-то", wednesday))
        assertEquals(RepeatPreset.NONE, RepeatPreset.of(null, wednesday))
    }

    @Test
    fun presetsRepeatFromTheTaskDate() {
        val anchor = TaskDue.AllDay(wednesday)
        fun next(preset: RepeatPreset) = Recurrence.next(preset.rule()!!, anchor, ZoneId.of("UTC"), wednesday)?.anchor
        assertEquals(TaskDue.AllDay(wednesday.plusDays(1)), next(RepeatPreset.DAILY))
        assertEquals(TaskDue.AllDay(wednesday.plusWeeks(1)), next(RepeatPreset.WEEKLY))
        assertEquals(TaskDue.AllDay(wednesday.plusMonths(1)), next(RepeatPreset.MONTHLY))
        assertEquals(TaskDue.AllDay(wednesday.plusYears(1)), next(RepeatPreset.YEARLY))
        assertEquals(TaskDue.AllDay(wednesday.plusDays(1)), next(RepeatPreset.WORKDAYS))
        // С пятницы рабочий день — понедельник.
        assertEquals(
            TaskDue.AllDay(LocalDate.of(2026, 10, 19)),
            Recurrence.next(RepeatPreset.WORKDAYS.rule()!!, TaskDue.AllDay(LocalDate.of(2026, 10, 16)), ZoneId.of("UTC"), LocalDate.of(2026, 10, 16))?.anchor,
        )
    }
}
