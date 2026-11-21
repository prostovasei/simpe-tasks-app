package com.prostovasei.simpletasks.data.news

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SportsNewsTest {
    @Test
    fun loadsAllDateGroupsAndPagesWithinRollingWindow() {
        val now = Instant.parse("2026-01-01T07:30:00Z")
        val pages = mutableListOf<String>()
        val edition = SportsNews.load(now) { url, _ ->
            pages += url
            val groups = if (pages.size == 1) {
                date("1 января", story("09:00", "/football/one.html", "Матч")) +
                    date("31 декабря", story("22:00", "/hockey/two.html", "Хоккей") +
                        story("20:00", "/football/game.html", "Викторина", game = true))
            } else {
                date("31 декабря", story("22:00", "/hockey/two.html?from=page2", "Дубль") +
                    story("10:30", "/tennis/three.html", "На границе") +
                    story("10:29", "/tennis/old.html", "Раньше границы"))
            }
            val next = "<a href='/news/top/?page=${pages.size + 1}'>Следующая</a>"
            Jsoup.parse("""
                <ul><li class="panel active-panel"><div class="news">$groups</div>$next</li></ul>
            """.trimIndent(), url)
        }

        assertEquals(
            listOf("https://www.sports.ru/news/top/", "https://www.sports.ru/news/top/?page=2"),
            pages,
        )
        assertEquals(listOf("Матч", "Хоккей", "На границе"), edition.stories.map { it.title })
        assertEquals(Instant.parse("2025-12-31T07:30:00Z").epochSecond, edition.stories.last().published)
    }

    private fun date(day: String, entries: String) = "<div class='short-news'><b>$day</b>$entries</div>"

    private fun story(time: String, path: String, title: String, game: Boolean = false): String {
        val marker = if (game) "<span class='tests-games'>Тесты и игры</span>" else ""
        return "<p><span class='time'>$time</span>$marker<a class='short-text' href='$path'>$title</a></p>"
    }
}
