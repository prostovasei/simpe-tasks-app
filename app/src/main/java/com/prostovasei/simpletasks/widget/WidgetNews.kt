package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.ui.feature.news.sportsTime
import com.prostovasei.simpletasks.ui.feature.news.sportsDay
import java.time.LocalDate

internal fun newsWidgetViews(context: Context, id: Int, bar: RemoteViews, size: DpSize): RemoteViews {
    val compact = size.width.value < 240
    val adapter = Intent(context, WidgetNewsService::class.java)
        .setData("simpletasks://widget/$id/news/$compact".toUri())
        .putExtra("compact", compact)
    val open = Intent(context, WidgetNewsLinkActivity::class.java)
        .setData("simpletasks://widget/$id/open-news".toUri())
    val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return RemoteViews(context.packageName, R.layout.widget_news_root).apply {
        setViewVisibility(R.id.widget_news_header, if (compact) View.GONE else View.VISIBLE)
        setTextViewText(R.id.widget_news_updated, SportsNews.cached(context)?.let {
            context.getString(R.string.news_updated, sportsTime(it.updated))
        }.orEmpty())
        setRemoteAdapter(R.id.widget_news_list, adapter)
        setPendingIntentTemplate(R.id.widget_news_list, PendingIntent.getActivity(context, id, open, mutable))
        removeAllViews(R.id.widget_news_bar)
        addView(R.id.widget_news_bar, bar)
    }
}

class WidgetNewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        NewsRows(applicationContext, intent.getBooleanExtra("compact", false))
}

private class NewsRows(private val context: Context, private val compact: Boolean) :
    RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<RemoteViews>()

    override fun onCreate() = Unit
    override fun onDestroy() { rows = emptyList() }
    override fun getCount() = rows.size
    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    override fun onDataSetChanged() {
        val edition = SportsNews.cached(context)
        val stories = edition?.recent().orEmpty()
        val status = SportsNews.state.value.status
        rows = buildList {
            when {
                status == SportsNews.Status.LOADING -> add(row(context.getString(R.string.news_loading)))
                status == SportsNews.Status.FAILED -> add(row(context.getString(R.string.news_failed)))
                edition == null -> add(row(context.getString(R.string.news_first)))
            }
            if (edition != null && stories.isEmpty() && status == SportsNews.Status.IDLE) {
                add(row(context.getString(R.string.news_empty)))
            }
            stories.forEachIndexed { index, it ->
                val date = sportsDay(it.published)
                if (!compact && (index == 0 || sportsDay(stories[index - 1].published) != date)) {
                    add(row(context.getString(if (date == LocalDate.now()) R.string.day_today else R.string.day_yesterday)))
                }
                val meta = listOf(sportsTime(it.published), it.sport).filter(String::isNotBlank).joinToString(" · ")
                add(row(it.title, meta, it.url))
            }
        }
    }

    private fun row(title: String, meta: String = "", url: String? = null) =
        RemoteViews(context.packageName, R.layout.widget_news_row).apply {
            setTextViewText(R.id.news_row_title, title)
            setTextViewText(R.id.news_row_meta, meta)
            setViewVisibility(R.id.news_row_meta, if (meta.isBlank()) View.GONE else View.VISIBLE)
            setInt(R.id.news_row_title, "setMaxLines", if (compact) 2 else Int.MAX_VALUE)
            if (url != null) setOnClickFillInIntent(R.id.news_row, Intent().putExtra("news_url", url))
        }
}
