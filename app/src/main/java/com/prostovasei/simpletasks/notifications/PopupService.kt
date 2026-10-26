package com.prostovasei.simpletasks.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R

/**
 * Не даёт Samsung «заморозить» приложение, пока полоска напоминания на экране (0.30.0, находка PM).
 * Без него полоска добавлялась, приложение сразу засыпало, и анимация появления не доигрывала — полоска оставалась
 * невидимой за краем экрана, пока не коснёшься её места. Сервис переднего плана Samsung не замораживает; цена —
 * тихое свёрнутое уведомление «Напоминание на экране» в шторке, пока висит полоска.
 */
class PopupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PopupTrace.add(this, "сервис «не засыпать» запущен")
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.popup_channel), NotificationManager.IMPORTANCE_MIN))
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.popup_service_title))
            .setContentText(intent?.getStringExtra(EXTRA_TITLE))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) }
            .onSuccess { PopupTrace.add(this, "сервис «не засыпать» на переднем плане, важность ${PopupTrace.importance()}") }
            .onFailure {
                PopupTrace.add(this, "сервис «не засыпать» НЕ вышел на передний план: $it")
                stopSelf()
            }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "popup_on_screen"
        private const val NOTIFICATION_ID = Int.MAX_VALUE - 5
        private const val EXTRA_TITLE = "title"

        /** Полоска на экране — приложение не засыпает. Запуск разрешён: окно поверх приложений уже видно. */
        fun start(context: Context, title: String) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, PopupService::class.java).putExtra(EXTRA_TITLE, title))
            }.onFailure { PopupTrace.add(context, "сервис «не засыпать» НЕ запущен: $it") }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PopupService::class.java))
        }
    }
}
