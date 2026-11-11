package com.prostovasei.simpletasks.domain.model.finance

import java.time.Instant

/** Тип счёта (финансы, этап 1, #221): наличные, карта, накопления. [code] хранится в БД, [defaultEmoji] — когда свой эмодзи не выбран. */
enum class AccountType(val code: String, val defaultEmoji: String) {
    CASH("cash", "👛"),
    CARD("card", "💳"),
    SAVINGS("savings", "🏦"),
    ;

    companion object {
        fun fromCode(code: String): AccountType = entries.firstOrNull { it.code == code } ?: CARD
    }
}

/**
 * Счёт: собственные деньги в рублях. Суммы — в копейках целым числом. [initialBalance] — остаток на момент создания счёта
 * (доходом не считается); [balance] — остаток сейчас: начальный плюс операции (операции появятся на этапе 2, пока равен начальному).
 */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val emoji: String?,
    val initialBalance: Long,
    val balance: Long,
    val createdAt: Instant,
) {
    val emojiShown: String get() = emoji ?: type.defaultEmoji
}

/** Поля окна счёта: создание и правка. [balance] — остаток сейчас в копейках. */
data class AccountDraft(val name: String, val type: AccountType, val emoji: String?, val balance: Long)
