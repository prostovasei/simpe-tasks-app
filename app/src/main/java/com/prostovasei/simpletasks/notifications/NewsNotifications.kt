package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.NewsAlertState
import com.prostovasei.simpletasks.data.news.NewsAlerts
import com.prostovasei.simpletasks.data.news.SportsEdition
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.data.news.SportsStory
import com.prostovasei.simpletasks.widget.WidgetLog
import com.prostovasei.simpletasks.widget.WidgetRenderer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Настройки уведомлений о новостях (#296): включены и тишина 23:00–08:00 по умолчанию. */
data class NewsAlertSettings(
    val enabled: Boolean = true,
    val quietOn: Boolean = true,
    val quietFrom: Int = DEFAULT_QUIET_FROM,
    val quietTo: Int = DEFAULT_QUIET_TO,
) {
    companion object {
        const val DEFAULT_QUIET_FROM = 23 * 60
        const val DEFAULT_QUIET_TO = 8 * 60
    }
}

/** Хранение настроек и состояния проверок (SharedPreferences, без Room). */
object NewsAlertPrefs {
    private const val PREFS = "news_alerts"
    private const val KEY_ON = "enabled"
    private const val KEY_QUIET_ON = "quiet_on"
    private const val KEY_FROM = "quiet_from"
    private const val KEY_TO = "quiet_to"
    private const val KEY_STATE = "state"
    private val json = Json { ignoreUnknownKeys = true }

    fun settings(context: Context): NewsAlertSettings = prefs(context).run {
        NewsAlertSettings(
            enabled = getBoolean(KEY_ON, true),
            quietOn = getBoolean(KEY_QUIET_ON, true),
            quietFrom = getInt(KEY_FROM, NewsAlertSettings.DEFAULT_QUIET_FROM),
            quietTo = getInt(KEY_TO, NewsAlertSettings.DEFAULT_QUIET_TO),
        )
    }

    /** Сохраняет и переставляет фоновую проверку. Повторное включение начинает с чистого листа: первая проверка ничего не присылает. */
    fun set(context: Context, settings: NewsAlertSettings) {
        val wasOn = settings(context).enabled
        prefs(context).edit {
            putBoolean(KEY_ON, settings.enabled)
            putBoolean(KEY_QUIET_ON, settings.quietOn)
            putInt(KEY_FROM, settings.quietFrom)
            putInt(KEY_TO, settings.quietTo)
            if (settings.enabled && !wasOn) remove(KEY_STATE)
        }
        NewsChecks.schedule(context)
    }

    internal fun state(context: Context): NewsAlertState =
        prefs(context).getString(KEY_STATE, null)?.let { runCatching { json.decodeFromString<NewsAlertState>(it) }.getOrNull() } ?: NewsAlertState()

    internal fun saveState(context: Context, state: NewsAlertState) {
        prefs(context).edit { putString(KEY_STATE, json.encodeToString(NewsAlertState.serializer(), state)) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Фоновая проверка «Главных новостей» примерно раз в 2 часа при наличии сети (#296). */
object NewsChecks {
    private const val WORK = "news_check"
    private const val INTERVAL_HOURS = 2L

    fun schedule(context: Context) {
        // В тестах WorkManager может быть не инициализирован — расписание тогда не нужно.
        val work = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (!NewsAlertPrefs.settings(context).enabled) {
            work.cancelUniqueWork(WORK)
            NewsNotifier.cancel(context)
            return
        }
        val request = PeriodicWorkRequestBuilder<NewsCheckWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Ручное обновление ленты (приложение или виджет): показанное пользователь уже видел. */
    internal fun markSeen(context: Context, cancelNotification: Boolean) {
        if (SportsNews.state.value.status == SportsNews.Status.FAILED) return
        val edition = SportsNews.cached(context) ?: return
        NewsAlertPrefs.saveState(context, NewsAlerts.seen(NewsAlertPrefs.state(context), edition, Instant.now().epochSecond))
        if (cancelNotification) NewsNotifier.cancel(context)
    }

    /** Одна проверка: загрузить ленту, обновить виджеты и решить, показывать ли уведомление. */
    internal suspend fun run(context: Context) {
        val settings = NewsAlertPrefs.settings(context)
        if (!settings.enabled) return
        var failed = false
        SportsNews.refresh(context, reportFailure = { failed = true; WidgetLog.add(context, it) }) {
            WidgetRenderer.renderNewsWidgets(context)
        }
        val edition: SportsEdition = SportsNews.cached(context) ?: return
        if (failed) return
        val now = Instant.now()
        val local = now.atZone(ZoneId.systemDefault())
        val quiet = settings.quietOn && NewsAlerts.inQuiet(local.hour * 60 + local.minute, settings.quietFrom, settings.quietTo)
        val decision = NewsAlerts.check(NewsAlertPrefs.state(context), edition, now.epochSecond, quiet, NewsNotifier.isShown(context))
        NewsAlertPrefs.saveState(context, decision.state)
        decision.notify?.let { NewsNotifier.show(context, it) }
    }
}

class NewsCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NewsChecks.run(applicationContext)
        return Result.success()
    }
}

/** Уведомление «N новых новостей» (#296): одно, с постоянным id; нажатие открывает вкладку «Новости». */
internal object NewsNotifier {
    const val CHANNEL_ID = "news"
    private const val NOTIFICATION_ID = Int.MAX_VALUE - 9
    private const val REQUEST_CODE = 9
    private const val LINES = 3

    fun isShown(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == NOTIFICATION_ID }
    }.getOrDefault(false)

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun show(context: Context, stories: List<SportsStory>) {
        if (stories.isEmpty() || !canPost(context)) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.news_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val form = when (NewsAlerts.titleForm(stories.size)) {
            NewsAlerts.Form.ONE -> R.string.news_alert_one
            NewsAlerts.Form.FEW -> R.string.news_alert_few
            NewsAlerts.Form.MANY -> R.string.news_alert_many
        }
        val title = context.getString(form, stories.size)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        stories.take(LINES).forEach { style.addLine(it.title) }
        if (stories.size > LINES) style.setSummaryText(context.getString(R.string.news_alert_more, stories.size - LINES))
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_NEWS, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_news)
            .setContentTitle(title)
            .setContentText(stories.first().title)
            .setStyle(style)
            .setNumber(stories.size)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
}
