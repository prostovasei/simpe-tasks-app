package com.prostovasei.simpletasks.data.news

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** #300: какие новости новые — первый запуск, новые после обновления ленты, нажатие на новость и «Прочитать все». */
@RunWith(RobolectricTestRunner::class)
class NewsReadTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.now().epochSecond

    private fun story(n: Int, ago: Long = 600L * n) = SportsStory("https://www.sports.ru/football/$n.html", "Новость $n", "Футбол", now - ago)

    private fun save(vararg stories: SportsStory): SportsEdition {
        val edition = SportsEdition(now, stories.toList())
        context.getSharedPreferences("sports_news", Context.MODE_PRIVATE).edit()
            .putString("edition", Json.encodeToString(SportsEdition.serializer(), edition)).commit()
        return edition
    }

    @Test
    fun firstLaunchMarksWholeFeedAsRead() {
        val edition = save(story(1), story(2))
        assertTrue("После обновления приложения лента не становится полужирной", NewsRead.unread(context, edition).isEmpty())
        assertTrue(NewsRead.unread(context, edition).isEmpty())
    }

    @Test
    fun newStoriesAreUnreadUntilOpenedOrReadAll() {
        NewsRead.unread(context, save(story(3), story(4)))
        val edition = save(story(1), story(2), story(3), story(4))
        assertEquals(setOf("sports.ru/football/1.html", "sports.ru/football/2.html"), NewsRead.unread(context, edition))

        val before = NewsRead.revision.value
        NewsRead.markRead(context, story(1).url)
        assertEquals(setOf("sports.ru/football/2.html"), NewsRead.unread(context, edition))
        assertTrue("Лента перерисовывается", NewsRead.revision.value > before)

        NewsRead.markAllRead(context)
        assertTrue(NewsRead.unread(context, edition).isEmpty())
    }

    @Test
    fun unknownLinkChangesNothing() {
        NewsRead.unread(context, save(story(2)))
        val edition = save(story(1), story(2))
        NewsRead.markRead(context, "https://www.sports.ru/football/unknown.html")
        assertEquals(setOf("sports.ru/football/1.html"), NewsRead.unread(context, edition))
    }
}
