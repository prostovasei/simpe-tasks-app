package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Покупки из уведомлений Яндекс Банка (#233): настройки, «Не разобрано» и ответы «Записать / Изменить / Пропустить».
 * Наше уведомление о покупке убирается, как только на покупку ответили — откуда угодно.
 */
interface BankPurchases {
    fun observeSettings(): Flow<BankSettings>

    /** Есть ли у приложения системный доступ к уведомлениям. */
    fun hasAccess(): Boolean

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setAccount(accountId: String)

    /** Покупки без ответа: новые первыми. */
    fun observePending(): Flow<List<PendingPurchase>>

    /**
     * Новое уведомление о покупке на [amount] копеек в [at]. [key] — ключ уведомления банка: одно и то же уведомление
     * (обновлённое или показанное повторно) второй покупки не создаёт (§4.6). Возвращает новую покупку или null —
     * распознавание выключено, счёт не выбран или уведомление уже было. Новая покупка показывается нашим уведомлением.
     */
    suspend fun onPurchase(key: String, amount: Long, at: Instant): PendingPurchase?

    /** «Записать»: расход «Без категории» без места на счёт покупки в день покупки. */
    suspend fun record(id: String)

    /** «Изменить» → «Сохранить»: операция из окна, покупка считается разобранной. */
    suspend fun recordAs(id: String, draft: OperationDraft)

    /** «Пропустить»: ничего не записывается, покупка уходит из «Не разобрано». */
    suspend fun skip(id: String)
}
