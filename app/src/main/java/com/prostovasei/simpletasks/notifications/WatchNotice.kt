package com.prostovasei.simpletasks.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.R

/**
 * Уведомление для часов, пока на телефоне полоска (0.30.1, решение PM): часы вибрируют только от уведомлений телефона,
 * а полоска — не уведомление. Канал без звука и вибрации и без всплывающего окна: на телефоне — лишь строка в шторке,
 * часы показывают его со своей вибрацией. Полоска ушла — уведомление убирается и с часов.
 */
class WatchNotice(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun show(strip: PopupStrip) {
        if (!canPost()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(strip.title)
            .setContentText(strip.subtitle)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // проверено в canPost()
        manager.notify(id(strip.taskId), notification)
    }

    fun cancel(taskId: String) = manager.cancel(id(taskId))

    private fun id(taskId: String) = "watch:$taskId".hashCode()

    private fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.watch_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "reminders_watch"
    }
}
