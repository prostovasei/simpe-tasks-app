package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фильтр недели по спискам (#243). */
class WeekFilterTest {
    @Test
    fun emptyFilterAllowsEverythingInAnyMode() {
        assertFalse(WeekFilter().isActive)
        assertTrue(WeekFilter().allows("work", isInbox = false))
        assertTrue(WeekFilter(WeekFilterMode.HIDE).allows("work", isInbox = false))
        assertTrue(WeekFilter(WeekFilterMode.HIDE).allows("inbox", isInbox = true))
    }

    @Test
    fun showModeKeepsOnlyMarked() {
        val filter = WeekFilter(WeekFilterMode.SHOW, setOf("work"))
        assertTrue(filter.allows("work", isInbox = false))
        assertFalse(filter.allows("home", isInbox = false))
        assertFalse(filter.allows("inbox", isInbox = true))
    }

    @Test
    fun hideModeDropsMarked() {
        val filter = WeekFilter(WeekFilterMode.HIDE, setOf("work"))
        assertFalse(filter.allows("work", isInbox = false))
        assertTrue(filter.allows("home", isInbox = false))
        assertTrue(filter.allows("inbox", isInbox = true))
    }

    @Test
    fun noListItemCoversDatedTasksWithoutList() {
        assertTrue(WeekFilter(WeekFilterMode.SHOW, setOf(WeekFilter.NO_LIST)).allows("inbox-id", isInbox = true))
        assertFalse(WeekFilter(WeekFilterMode.HIDE, setOf(WeekFilter.NO_LIST)).allows("inbox-id", isInbox = true))
    }

    @Test
    fun storedAsText() {
        val filter = WeekFilter(WeekFilterMode.HIDE, setOf("b", "a", WeekFilter.NO_LIST))
        assertEquals(filter, WeekFilter.decode(filter.encode()))
        assertEquals(WeekFilter(), WeekFilter.decode(null))
        assertEquals(WeekFilter(), WeekFilter.decode("мусор"))
    }
}
