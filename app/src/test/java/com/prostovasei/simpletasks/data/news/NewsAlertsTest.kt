package com.prostovasei.simpletasks.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #296: что считается новым, тишина и объединение с неснятым уведомлением. */
class NewsAlertsTest {
    private val now = 1_800_000_000L
    private fun story(id: Int, ago: Long = 60L * id) = SportsStory("https://www.sports.ru/football/$id/", "Новость $id", "Футбол", now - ago)
    private fun edition(vararg ids: Int) = SportsEdition(now, ids.map { story(it) })

    @Test
    fun firstCheckOnlyRemembers() {
        val decision = NewsAlerts.check(NewsAlertState(), edition(1, 2), now, quiet = false, shownActive = false)
        assertNull(decision.notify)
        assertEquals(2, decision.state.known!!.size)
    }

    @Test
    fun notifiesOnlyAboutNewStories() {
        val first = NewsAlerts.check(NewsAlertState(), edition(1, 2), now, false, false).state
        val second = NewsAlerts.check(first, edition(1, 2, 3, 4), now, false, false)
        assertEquals(listOf("Новость 3", "Новость 4"), second.notify!!.map { it.title })
        val third = NewsAlerts.check(second.state, edition(1, 2, 3, 4), now, false, false)
        assertNull(third.notify)
    }

    @Test
    fun wwwAndBareHostAreTheSameStory() {
        val first = NewsAlerts.check(NewsAlertState(), edition(1), now, false, false).state
        val bare = SportsEdition(now, listOf(story(1).copy(url = "https://sports.ru/football/1/")))
        assertNull(NewsAlerts.check(first, bare, now, false, false).notify)
    }

    @Test
    fun quietCollectsAndSendsOnceAfterwards() {
        val start = NewsAlerts.check(NewsAlertState(), edition(1), now, false, false).state
        val night1 = NewsAlerts.check(start, edition(1, 2), now, quiet = true, shownActive = false)
        assertNull(night1.notify)
        val night2 = NewsAlerts.check(night1.state, edition(1, 2, 3), now, quiet = true, shownActive = false)
        assertNull(night2.notify)
        assertEquals(2, night2.state.pending.size)
        val morning = NewsAlerts.check(night2.state, edition(1, 2, 3), now, quiet = false, shownActive = false)
        assertEquals(listOf("Новость 2", "Новость 3"), morning.notify!!.map { it.title })
        assertTrue(morning.state.pending.isEmpty())
    }

    @Test
    fun unseenNotificationIsMergedNewestFirst() {
        val start = NewsAlerts.check(NewsAlertState(), edition(5), now, false, false).state
        val first = NewsAlerts.check(start, edition(5, 4), now, false, false)
        val merged = NewsAlerts.check(first.state, edition(5, 4, 1), now, false, shownActive = true)
        assertEquals(listOf("Новость 1", "Новость 4"), merged.notify!!.map { it.title })
        val dismissed = NewsAlerts.check(merged.state, edition(5, 4, 1, 2), now, false, shownActive = false)
        assertEquals(listOf("Новость 2"), dismissed.notify!!.map { it.title })
    }

    @Test
    fun seenClearsPendingAndForgetsOldKeys() {
        val start = NewsAlerts.check(NewsAlertState(), edition(1), now, false, false).state
        val night = NewsAlerts.check(start, edition(1, 2), now, quiet = true, shownActive = false).state
        val seen = NewsAlerts.seen(night, edition(1, 2, 3), now)
        assertTrue(seen.pending.isEmpty())
        assertNull(NewsAlerts.check(seen, edition(1, 2, 3), now, false, false).notify)
        val later = now + 49 * 3600
        val pruned = NewsAlerts.seen(seen, SportsEdition(later, emptyList()), later)
        assertTrue(pruned.known!!.isEmpty())
    }

    @Test
    fun russianPluralForms() {
        val one = NewsAlerts.Form.ONE
        val few = NewsAlerts.Form.FEW
        val many = NewsAlerts.Form.MANY
        val expected = mapOf(1 to one, 2 to few, 4 to few, 5 to many, 11 to many, 12 to many, 14 to many, 21 to one, 22 to few, 25 to many, 101 to one, 111 to many)
        expected.forEach { (count, form) -> assertEquals("$count", form, NewsAlerts.titleForm(count)) }
    }

    @Test
    fun quietPeriodAcrossMidnight() {
        assertTrue(NewsAlerts.inQuiet(23 * 60, 23 * 60, 8 * 60))
        assertTrue(NewsAlerts.inQuiet(3 * 60, 23 * 60, 8 * 60))
        assertFalse(NewsAlerts.inQuiet(8 * 60, 23 * 60, 8 * 60))
        assertFalse(NewsAlerts.inQuiet(12 * 60, 23 * 60, 8 * 60))
        assertTrue(NewsAlerts.inQuiet(13 * 60, 12 * 60, 14 * 60))
        assertFalse(NewsAlerts.inQuiet(14 * 60, 12 * 60, 14 * 60))
        assertFalse(NewsAlerts.inQuiet(10 * 60, 9 * 60, 9 * 60))
    }
}
