package com.prostovasei.simpletasks.notifications.bank

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.bank.BankPurchaseNotifier
import com.prostovasei.simpletasks.domain.bank.BankSettingsStore
import com.prostovasei.simpletasks.domain.bank.NotificationAccess
import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Настройки распознавания (#233) в SharedPreferences «bank». */
@Singleton
class PrefsBankSettingsStore @Inject constructor(@param:ApplicationContext context: Context) : BankSettingsStore {
    private val prefs = context.getSharedPreferences("bank", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(BankSettings(prefs.getBoolean(KEY_ENABLED, false), prefs.getString(KEY_ACCOUNT, null)))

    override val settings: StateFlow<BankSettings> = state.asStateFlow()

    @Synchronized
    override fun update(change: BankSettings.() -> BankSettings) {
        val next = state.value.change()
        prefs.edit {
            putBoolean(KEY_ENABLED, next.enabled)
            if (next.accountId == null) remove(KEY_ACCOUNT) else putString(KEY_ACCOUNT, next.accountId)
        }
        state.value = next
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ACCOUNT = "account_id"
    }
}

/** Доступ к уведомлениям выдан именно этому приложению. */
class AndroidNotificationAccess @Inject constructor(@param:ApplicationContext private val context: Context) : NotificationAccess {
    override fun granted(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    companion object {
        /** Системный экран доступа: с Android 11 — сразу настройка нашего приложения, раньше — общий список. */
        fun settingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(context, BankNotificationListener::class.java).flattenToString())
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }

        /** Резервный вариант, если производитель телефона убрал экран настройки одного приложения. */
        fun listIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}

/**
 * Наше уведомление о покупке (#233, §4.3; макет 2.1): заголовок «−680 ₽», текст «Яндекс Банк · Карта», пометка дубля,
 * кнопки «Записать», «Изменить», «Пропустить». Отдельный канал «Покупки по карте» без звука и вибрации. Само не убирается.
 */
@Singleton
class AndroidBankNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val time: TimeProvider,
) : BankPurchaseNotifier {
    private val manager = NotificationManagerCompat.from(context)

    override fun show(purchase: PendingPurchase) {
        if (!canPost()) return
        ensureChannel()
        val id = notificationId(purchase.id)
        val text = context.getString(R.string.bank_notification_text, purchase.accountName)
        val duplicate = purchase.duplicate?.let { dup ->
            val today = time.today()
            val day = when (dup.date) {
                today -> context.getString(R.string.finance_today)
                today.minusDays(1) -> context.getString(R.string.finance_yesterday)
                else -> dup.date.format(DAY)
            }
            context.getString(R.string.bank_duplicate, dup.categoryName ?: context.getString(R.string.category_none), Money.format(dup.amount), day)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("−${Money.format(purchase.amount)}")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setWhen(purchase.postedAt.toEpochMilli())
            .setShowWhen(true)
            .setAutoCancel(false)
            .setContentIntent(editIntent(purchase.id, id))
            .addAction(0, context.getString(R.string.bank_record), action(purchase.id, BankPurchaseReceiver.ACTION_RECORD, id, 0))
            .addAction(0, context.getString(R.string.bank_edit), editIntent(purchase.id, id))
            .addAction(0, context.getString(R.string.bank_skip), action(purchase.id, BankPurchaseReceiver.ACTION_SKIP, id, 2))
        if (duplicate != null) builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$duplicate"))
        @Suppress("MissingPermission") // проверено в canPost()
        manager.notify(id, builder.build())
    }

    override fun cancel(purchaseId: String) = manager.cancel(notificationId(purchaseId))

    private fun canPost(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Канал без звука и вибрации (PM не использует вибрацию). */
    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.bank_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun action(purchaseId: String, action: String, notificationId: Int, index: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId * ACTIONS + index,
        Intent(context, BankPurchaseReceiver::class.java).setAction(action).putExtra(BankPurchaseReceiver.EXTRA_PURCHASE_ID, purchaseId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** «Изменить» и нажатие на уведомление: приложение с окном нового расхода. */
    private fun editIntent(purchaseId: String, notificationId: Int): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId * ACTIONS + 1,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_BANK_PURCHASE, purchaseId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "bank_purchases"
        private const val ACTIONS = 3
        private val DAY = java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.forLanguageTag("ru"))

        /** Свой ряд номеров, чтобы не совпасть с напоминаниями задач. */
        fun notificationId(purchaseId: String): Int = "bank:$purchaseId".hashCode()
    }
}
