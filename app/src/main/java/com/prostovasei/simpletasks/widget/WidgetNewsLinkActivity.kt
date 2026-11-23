package com.prostovasei.simpletasks.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.prostovasei.simpletasks.data.news.NewsRead
import com.prostovasei.simpletasks.data.news.SportsNews

class WidgetNewsLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_NEWS_URL)
        // #300: «Прочитать все» в виджете или нажатие на новость — отметка «прочитано», виджет перерисовывает список.
        if (intent.getBooleanExtra(EXTRA_READ_ALL, false)) {
            NewsRead.markAllRead(this)
            NewsWidgets.refreshList(this)
            finish()
            return
        }
        url?.let {
            NewsRead.markRead(this, it)
            NewsWidgets.refreshList(this)
        }
        try {
            if (url != null && SportsNews.cached(this)?.stories?.any { it.url == url } == true) {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        } catch (_: ActivityNotFoundException) {
            // На устройстве нет приложения, которое открывает HTTPS.
        } finally {
            finish()
        }
    }
}
