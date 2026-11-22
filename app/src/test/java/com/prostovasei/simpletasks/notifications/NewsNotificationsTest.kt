package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsStory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

/** #296: расписание фоновой проверки и вид уведомления «N новых новостей». */
@RunWith(RobolectricTestRunner::class)
class NewsNotificationsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setExecutor(SynchronousExecutor()).build(),
        )
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun work() = WorkManager.getInstance(app).getWorkInfosForUniqueWork("news_check").get()

    @Test
    fun schedulesEveryTwoHoursWithNetworkAndCancelsWhenOff() {
        NewsAlertPrefs.set(app, NewsAlertSettings())
        NewsChecks.schedule(app)
        val info = work().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.HOURS.toMillis(2), info.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)

        NewsAlertPrefs.set(app, NewsAlertSettings(enabled = false))
        assertTrue(work().all { it.state == WorkInfo.State.CANCELLED })

        NewsAlertPrefs.set(app, NewsAlertSettings())
        assertTrue(work().any { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun notificationShowsCountHeadlinesAndOpensNews() {
        val stories = (1..5).map { SportsStory("https://www.sports.ru/basketball/$it/", "Заголовок $it", "Баскетбол", 1_800_000_000L - it) }
        NewsNotifier.show(app, stories)

        val posted = shadowOf(manager).allNotifications.single()
        val shadow = shadowOf(posted)
        assertEquals("5 новых новостей", shadow.contentTitle)
        assertEquals("Заголовок 1", shadow.contentText)
        assertEquals(NewsNotifier.CHANNEL_ID, posted.channelId)
        assertEquals(R.drawable.ic_notification_news, posted.smallIcon.resId)
        val lines = posted.extras.getCharSequenceArray("android.textLines")!!.map(CharSequence::toString)
        assertEquals(listOf("Заголовок 1", "Заголовок 2", "Заголовок 3"), lines)
        assertEquals("и ещё 2", posted.extras.getCharSequence("android.summaryText").toString())
        val open = shadowOf(posted.contentIntent).savedIntent
        assertTrue(open.getBooleanExtra(MainActivity.EXTRA_OPEN_NEWS, false))
        assertTrue(NewsNotifier.isShown(app))

        NewsNotifier.show(app, stories.take(1))
        assertEquals("Одно уведомление с постоянным id", 1, shadowOf(manager).allNotifications.size)
        assertEquals("1 новая новость", shadowOf(shadowOf(manager).allNotifications.single()).contentTitle)
        NewsNotifier.show(app, stories.take(3))
        assertEquals("3 новые новости", shadowOf(shadowOf(manager).allNotifications.single()).contentTitle)

        NewsNotifier.cancel(app)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun eachModuleHasItsOwnIcon() {
        val icons = listOf(R.drawable.ic_notification_tasks, R.drawable.ic_notification_finance, R.drawable.ic_notification_health, R.drawable.ic_notification_news)
        assertEquals(4, icons.toSet().size)
        icons.forEach { assertTrue(app.resources.getResourceName(it).startsWith("com.prostovasei.simpletasks:drawable/ic_notification_")) }
    }
}
