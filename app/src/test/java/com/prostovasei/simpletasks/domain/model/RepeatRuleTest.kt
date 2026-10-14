package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class RepeatRuleTest {

    @Test
    fun parsesTickTickRules() {
        assertEquals(RepeatRule(RepeatFrequency.MONTHLY, monthDay = 14), RepeatRule.parse("FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14"))
        assertEquals(RepeatRule(RepeatFrequency.YEARLY, month = 6, monthDay = 5), RepeatRule.parse("FREQ=YEARLY;INTERVAL=1;BYMONTH=6;BYMONTHDAY=5"))
        assertEquals(RepeatRule(RepeatFrequency.WEEKLY, interval = 2, weekDays = setOf(TUESDAY)), RepeatRule.parse("FREQ=WEEKLY;WKST=MO;INTERVAL=2;BYDAY=TU"))
        assertEquals(RepeatRule(RepeatFrequency.WEEKLY, weekDays = setOf(TUESDAY, THURSDAY)), RepeatRule.parse("FREQ=WEEKLY;INTERVAL=1;WKST=MO;BYDAY=TU,TH"))
        assertEquals(RepeatRule(RepeatFrequency.WEEKLY, weekDays = setOf(SATURDAY, TUESDAY)), RepeatRule.parse("FREQ=WEEKLY;BYDAY=SA,TU"))
        assertEquals(RepeatRule(RepeatFrequency.WEEKLY, weekDays = setOf(THURSDAY), count = 3), RepeatRule.parse("FREQ=WEEKLY;INTERVAL=1;COUNT=3;BYDAY=TH"))
        assertEquals(RepeatRule(RepeatFrequency.YEARLY), RepeatRule.parse("RRULE:FREQ=YEARLY"))
        assertEquals(RepeatRule(RepeatFrequency.WEEKLY, interval = 3, weekDays = setOf(WEDNESDAY)), RepeatRule.parse("FREQ=WEEKLY;WKST=MO;INTERVAL=3;BYDAY=WE"))
        assertEquals(LocalDate.of(2026, 12, 1), RepeatRule.parse("FREQ=DAILY;UNTIL=20261201T000000Z")!!.until)
    }

    @Test
    fun keepsUnknownPartsAsIs() {
        val lastFriday = RepeatRule.parse("FREQ=MONTHLY;BYDAY=-1FR")!!
        assertEquals(mapOf("BYDAY" to "-1FR"), lastFriday.unsupported)
        assertTrue(lastFriday.weekDays.isEmpty())
        assertFalse(lastFriday.isSimple(LocalDate.of(2026, 10, 30)))

        val lastDay = RepeatRule.parse("FREQ=MONTHLY;BYMONTHDAY=-1")!!
        assertEquals(mapOf("BYMONTHDAY" to "-1"), lastDay.unsupported)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", lastDay.toRRule())
    }

    @Test
    fun invalidTextIsNotARule() {
        assertNull(RepeatRule.parse(null))
        assertNull(RepeatRule.parse(""))
        assertNull(RepeatRule.parse("каждый день"))
        assertNull(RepeatRule.parse("FREQ=HOURLY"))
    }

    @Test
    fun simpleRulesCanBeEditedByConstructor() {
        val oct14 = LocalDate.of(2026, 10, 14)
        assertTrue(RepeatRule.parse("FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=14")!!.isSimple(oct14))
        assertFalse("Число не совпадает со сроком", RepeatRule.parse("FREQ=MONTHLY;BYMONTHDAY=20")!!.isSimple(oct14))
        assertFalse("Ограничение по числу раз", RepeatRule.parse("FREQ=WEEKLY;COUNT=3;BYDAY=TH")!!.isSimple(oct14))
        assertTrue(RepeatRule.parse("FREQ=WEEKLY;WKST=MO;INTERVAL=2;BYDAY=TU")!!.isSimple(oct14))
    }

    @Test
    fun buildsRRuleForConstructor() {
        assertEquals("FREQ=DAILY", RepeatRule(RepeatFrequency.DAILY).toRRule())
        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,FR,SU", RepeatRule(RepeatFrequency.WEEKLY, 2, setOf(SUNDAY, FRIDAY, TUESDAY)).toRRule())
        assertEquals("FREQ=YEARLY;INTERVAL=3", RepeatRule(RepeatFrequency.YEARLY, 3).toRRule())
    }

    @Test
    fun roundTripOfBuiltRules() {
        listOf(
            RepeatRule(RepeatFrequency.DAILY, interval = 5),
            RepeatRule(RepeatFrequency.WEEKLY, weekDays = setOf(TUESDAY, THURSDAY)),
            RepeatRule(RepeatFrequency.MONTHLY, interval = 2),
            RepeatRule(RepeatFrequency.WEEKLY, count = 4, until = LocalDate.of(2027, 1, 31)),
        ).forEach { assertEquals(it, RepeatRule.parse(it.toRRule())) }
    }
}
