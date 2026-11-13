package com.prostovasei.simpletasks.notifications.bank

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.domain.bank.BankPurchaseParser
import com.prostovasei.simpletasks.domain.bank.BankPurchaseNotifier as Notifier
import com.prostovasei.simpletasks.domain.bank.BankSettingsStore
import com.prostovasei.simpletasks.domain.bank.NotificationAccess
import com.prostovasei.simpletasks.data.repository.RoomBankPurchases
import com.prostovasei.simpletasks.domain.repository.BankPurchases
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Чтение уведомлений (#233). Android передаёт сюда уведомления всех приложений — сразу отбрасываем всё, кроме приложений
 * Яндекса, и ничего не сохраняем: из покупки остаются только сумма и время. Работает, только когда PM выдал доступ
 * и включил распознавание.
 */
@AndroidEntryPoint
class BankNotificationListener : NotificationListenerService() {
    @Inject lateinit var purchases: BankPurchases

    @Inject lateinit var settings: BankSettingsStore

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!BankPurchaseParser.isBankApp(sbn.packageName) || !settings.settings.value.enabled) return
        // Сводка группы («Яндекс Пэй · 2 уведомления») повторяет отдельные уведомления — их и читаем, иначе покупка удвоится.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = sbn.notification.extras
        val amount = BankPurchaseParser.parse(
            sbn.packageName,
            extras.getCharSequence(Notification.EXTRA_TITLE),
            listOf(extras.getCharSequence(Notification.EXTRA_TEXT), extras.getCharSequence(Notification.EXTRA_BIG_TEXT)),
        ) ?: return
        // Ключ — пакет, номер уведомления и время его создания: обновление того же уведомления — та же покупка (§4.6).
        val key = "${sbn.key}|${sbn.notification.`when`}"
        val at = Instant.ofEpochMilli(sbn.postTime)
        appScope.launch { purchases.onPurchase(key, amount, at) }
    }
}

/** Кнопки «Записать» и «Пропустить» нашего уведомления о покупке. */
@AndroidEntryPoint
class BankPurchaseReceiver : BroadcastReceiver() {
    @Inject lateinit var purchases: BankPurchases

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_PURCHASE_ID) ?: return
        val pending = goAsync()
        appScope.launch {
            try {
                when (intent.action) {
                    ACTION_RECORD -> purchases.record(id)
                    ACTION_SKIP -> purchases.skip(id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RECORD = "com.prostovasei.simpletasks.BANK_RECORD"
        const val ACTION_SKIP = "com.prostovasei.simpletasks.BANK_SKIP"
        const val EXTRA_PURCHASE_ID = "purchase_id"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BankModule {
    @Binds
    abstract fun bindPurchases(impl: RoomBankPurchases): BankPurchases

    @Binds
    abstract fun bindSettings(impl: PrefsBankSettingsStore): BankSettingsStore

    @Binds
    abstract fun bindNotifier(impl: AndroidBankNotifier): Notifier

    @Binds
    abstract fun bindAccess(impl: AndroidNotificationAccess): NotificationAccess
}
