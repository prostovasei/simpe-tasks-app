package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.ui.feature.news.NewsFeedRow
import com.prostovasei.simpletasks.ui.feature.news.newsFeedRows

internal fun newsWidgetViews(context: Context, id: Int, bar: RemoteViews): RemoteViews {
    val adapter = Intent(context, WidgetNewsService::class.java)
        .setData("simpletasks://widget/$id/news".toUri())
    val open = Intent(context, WidgetNewsLinkActivity::class.java)
        .setData("simpletasks://widget/$id/open-news".toUri())
    val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return RemoteViews(context.packageName, R.layout.widget_news_root).apply {
        setRemoteAdapter(R.id.widget_news_list, adapter)
        setPendingIntentTemplate(R.id.widget_news_list, PendingIntent.getActivity(context, id, open, mutable))
        removeAllViews(R.id.widget_news_bar)
        addView(R.id.widget_news_bar, bar)
    }
}

/** #300: после отметки «прочитано» в приложении или виджете список виджетов «Новости» перечитывается. */
internal object NewsWidgets {
    fun refreshList(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = WidgetRenderer.widgetIds(context).filter { WidgetSelection.module(context, it) == WidgetModule.NEWS }.toIntArray()
        if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_news_list)
    }
}

internal const val EXTRA_NEWS_URL = "news_url"
internal const val EXTRA_READ_ALL = "news_read_all"

class WidgetNewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        NewsRows(applicationContext)
}

private class NewsRows(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<RemoteViews>()

    override fun onCreate() = Unit
    override fun onDestroy() { rows = emptyList() }
    override fun getCount() = rows.size
    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 4
    override fun getItemId(position: Int) = position.toLong()
    override fun hasStableIds() = false

    override fun onDataSetChanged() {
        rows = newsFeedRows(context, SportsNews.state.value.status).map {
            when (it) {
                is NewsFeedRow.Day -> day(it)
                is NewsFeedRow.Message -> row(it.text)
                is NewsFeedRow.Article -> row(it.title, it.meta, it.url, it.unread)
                is NewsFeedRow.Unread -> unread(it.count)
            }
        }
    }

    private fun day(day: NewsFeedRow.Day) =
        RemoteViews(context.packageName, R.layout.widget_news_day).apply {
            setTextViewText(R.id.news_row_title, day.title)
            setTextViewText(R.id.widget_news_updated, day.updatedAt.orEmpty())
            setViewVisibility(R.id.widget_news_updated, if (day.updatedAt == null) View.GONE else View.VISIBLE)
            day.updatedAt?.let { setContentDescription(R.id.widget_news_updated, context.getString(R.string.news_updated, it)) }
        }

    /** #300: «Новые · N» и «Прочитать все» — нажатие уходит в [WidgetNewsLinkActivity] с [EXTRA_READ_ALL]. */
    private fun unread(count: Int) =
        RemoteViews(context.packageName, R.layout.widget_news_unread).apply {
            setTextViewText(R.id.news_unread_count, context.getString(R.string.news_unread_count, count))
            setOnClickFillInIntent(R.id.news_read_all, Intent().putExtra(EXTRA_READ_ALL, true))
        }

    private fun row(title: String, meta: String = "", url: String? = null, unread: Boolean = false) =
        RemoteViews(context.packageName, if (unread) R.layout.widget_news_row_unread else R.layout.widget_news_row).apply {
            setTextViewText(R.id.news_row_title, title)
            setTextViewText(R.id.news_row_meta, meta)
            setViewVisibility(R.id.news_row_meta, if (meta.isBlank()) View.GONE else View.VISIBLE)
            if (url != null) setOnClickFillInIntent(R.id.news_row, Intent().putExtra(EXTRA_NEWS_URL, url))
        }
}
