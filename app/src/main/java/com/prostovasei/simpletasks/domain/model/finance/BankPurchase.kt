package com.prostovasei.simpletasks.domain.model.finance

import java.time.Instant
import java.time.LocalDate

/** Настройки распознавания (#233): включено ли и на какой счёт записывать покупки Яндекс Банка. */
data class BankSettings(val enabled: Boolean = false, val accountId: String? = null)

/**
 * Распознанная покупка без ответа — строка «Не разобрано» (#233, §4.4). Хранятся только сумма, счёт и время — без текста уведомления.
 * [date] — день покупки (время уведомления). [duplicate] — похожий расход уже записан (§4.5); сам ничего не пропускает.
 */
data class PendingPurchase(
    val id: String,
    val amount: Long,
    val accountId: String,
    val accountName: String,
    val postedAt: Instant,
    val date: LocalDate,
    val duplicate: DuplicateHint? = null,
    /** Значок счёта для плашки (#245). */
    val accountEmoji: String = "",
)

/** Расход, похожий на покупку: тот же счёт, сумма и день. [categoryName] = null — «Без категории». */
data class DuplicateHint(val categoryName: String?, val amount: Long, val date: LocalDate)
