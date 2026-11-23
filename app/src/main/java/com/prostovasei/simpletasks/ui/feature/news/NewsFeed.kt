package com.prostovasei.simpletasks.ui.feature.news

import android.content.Context
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.NewsAlerts
import com.prostovasei.simpletasks.data.news.NewsRead
import com.prostovasei.simpletasks.data.news.SportsNews
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal sealed interface NewsFeedRow {
    data class Day(val title: String, val updatedAt: String? = null) : NewsFeedRow
    data class Message(val text: String) : NewsFeedRow
    data class Article(val title: String, val meta: String, val url: String, val unread: Boolean = false) : NewsFeedRow

    /** #300: строка «Новые · N — Прочитать все» над лентой; только если есть непрочитанные. */
    data class Unread(val count: Int) : NewsFeedRow
}

internal fun newsFeedRows(context: Context, status: SportsNews.Status): List<NewsFeedRow> {
    val now = Instant.now()
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()
    val time = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val edition = SportsNews.cached(context)
    val stories = edition?.recent(now).orEmpty()
    val unread = NewsRead.unread(context, edition)
    val unreadCount = stories.count { NewsAlerts.key(it) in unread }
    return buildList {
        if (unreadCount > 0) add(NewsFeedRow.Unread(unreadCount))
        add(NewsFeedRow.Day(context.getString(R.string.day_today),
            edition?.let { time.format(Instant.ofEpochSecond(it.updated)) }))
        val message = when {
            status == SportsNews.Status.LOADING -> R.string.news_loading
            status == SportsNews.Status.FAILED -> R.string.news_failed
            edition == null -> R.string.news_first
            stories.isEmpty() -> R.string.news_empty
            else -> null
        }
        if (message != null) add(NewsFeedRow.Message(context.getString(message)))

        var previousDate = today
        stories.forEach { story ->
            val published = Instant.ofEpochSecond(story.published).atZone(zone)
            val date = published.toLocalDate()
            if (date != previousDate) {
                val label = if (date == today.minusDays(1)) context.getString(R.string.day_yesterday)
                else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
                add(NewsFeedRow.Day(label))
                previousDate = date
            }
            val meta = listOf(time.format(published), story.sport).filter(String::isNotBlank).joinToString(" · ")
            add(NewsFeedRow.Article(story.title, meta, story.url, unread = NewsAlerts.key(story) in unread))
        }
    }
}
