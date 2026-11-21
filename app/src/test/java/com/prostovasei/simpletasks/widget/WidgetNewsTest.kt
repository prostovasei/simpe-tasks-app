package com.prostovasei.simpletasks.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsEdition
import com.prostovasei.simpletasks.data.news.SportsStory
import com.prostovasei.simpletasks.widget.theme.LocalWidgetColors
import com.prostovasei.simpletasks.widget.theme.TaskWidgetColors
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetNewsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun wideShowsSavedNewsAndSeparateRefreshControls() = runBlocking {
        checkLayout(392, 280, compact = false, screenshot = "screenshots/widget_news_wide_light.png")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun compactKeepsManualRefreshAndFitsNewsRows() = runBlocking {
        checkLayout(180, 110, compact = true, screenshot = "screenshots/widget_news_compact_light.png")
    }

    @Test
    fun opensSavedArticleAndIgnoresUnknownLinks() {
        val story = saveEdition().stories.first()
        val known = Intent(context, WidgetNewsLinkActivity::class.java).putExtra("news_url", story.url)
        val activity = Robolectric.buildActivity(WidgetNewsLinkActivity::class.java, known).create().get()
        val opened = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened?.action)
        assertEquals(story.url, opened?.dataString)

        val unknown = Intent(context, WidgetNewsLinkActivity::class.java)
            .putExtra("news_url", "https://www.sports.ru/football/unknown.html")
        val rejected = Robolectric.buildActivity(WidgetNewsLinkActivity::class.java, unknown).create().get()
        assertNull(shadowOf(rejected).nextStartedActivity)
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun checkLayout(widthDp: Int, heightDp: Int, compact: Boolean, screenshot: String) {
        val edition = saveEdition()
        val size = DpSize(widthDp.dp, heightDp.dp)
        val model = WidgetModel(WidgetFilter.All, null, LocalDate.now(), emptyList())
        val bar = GlanceRemoteViews().compose(context, size) {
            CompositionLocalProvider(LocalWidgetColors provides TaskWidgetColors) {
                BottomBar(model, 1, imageButtons = true, module = WidgetModule.NEWS)
            }
        }.remoteViews
        val root = newsWidgetViews(context, 1, bar, size).apply(context, FrameLayout(context))
        val header = root.findViewById<View>(R.id.widget_news_header)
        assertEquals(if (compact) View.GONE else View.VISIBLE, header.visibility)

        val refresh = root.viewsWithDescription(context.getString(R.string.news_refresh)).single()
        assertTrue(generateSequence(refresh) { it.parent as? View }.take(3).any(View::hasOnClickListeners))
        assertEquals(if (compact) 0 else 1, root.viewsWithDescription(context.getString(R.string.update_title)).size)

        val service = Robolectric.buildService(WidgetNewsService::class.java).create().get()
        val rows = service.onGetViewFactory(Intent().putExtra("compact", compact))
        rows.onCreate()
        rows.onDataSetChanged()
        val list = root.findViewById<ListView>(R.id.widget_news_list)
        // В тесте нет процесса лаунчера. Подставляем строки, которые ему отдаёт настоящая фабрика.
        list.adapter = object : BaseAdapter() {
            override fun getCount() = rows.count
            override fun getItem(position: Int) = rows.getViewAt(position)
            override fun getItemId(position: Int) = rows.getItemId(position)
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                checkNotNull(rows.getViewAt(position)).apply(context, parent)
        }
        val titles = (0 until rows.count).map { position ->
            checkNotNull(rows.getViewAt(position)).apply(context, list)
                .findViewById<TextView>(R.id.news_row_title).text.toString()
        }
        val storyTitles = edition.stories.map(SportsStory::title)
        assertEquals(storyTitles.dropLast(1), titles.filter(storyTitles::contains))

        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        val visibleRows = (0 until list.childCount).map { list.getChildAt(it) }
        assertTrue(visibleRows.any { it.findViewById<TextView>(R.id.news_row_title).text.toString() == edition.stories.first().title })

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        bitmap.captureRoboImage(screenshot)
        rows.onDestroy()
    }

    private fun View.viewsWithDescription(label: String): List<View> {
        val found = ArrayList<View>()
        findViewsWithText(found, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return found.filter { it.contentDescription == label }
    }

    private fun saveEdition(): SportsEdition {
        val now = Instant.now().epochSecond
        val edition = SportsEdition(now, listOf(
            SportsStory("https://www.sports.ru/football/1.html", "Чемпионат России. Лидеры встретятся в воскресенье", "Футбол", now - 1_200),
            SportsStory("https://www.sports.ru/hockey/2.html", "Сборная выиграла первый матч турнира", "Хоккей", now - 3_600),
            SportsStory("https://www.sports.ru/tennis/3.html", "Определились полуфиналисты турнира", "Теннис", now - 5_400),
            SportsStory("https://www.sports.ru/football/4.html", "Вчерашняя новость за пределами окна", "Футбол", now - 86_401),
        ))
        context.getSharedPreferences("sports_news", Context.MODE_PRIVATE).edit()
            .putString("edition", Json.encodeToString(SportsEdition.serializer(), edition)).commit()
        return edition
    }
}
