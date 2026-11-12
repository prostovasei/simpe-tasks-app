package com.prostovasei.simpletasks.domain.bank

import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import kotlinx.coroutines.flow.StateFlow

/** Где хранятся настройки распознавания (#233). */
interface BankSettingsStore {
    val settings: StateFlow<BankSettings>

    fun update(change: BankSettings.() -> BankSettings)
}

/** Наше уведомление о покупке (#233, §4.3): «−680 ₽ · Яндекс Банк · Карта» и три кнопки. */
interface BankPurchaseNotifier {
    fun show(purchase: PendingPurchase)

    fun cancel(purchaseId: String)
}

/** Системный доступ к уведомлениям (Android: «Доступ к уведомлениям»). */
fun interface NotificationAccess {
    fun granted(): Boolean
}
