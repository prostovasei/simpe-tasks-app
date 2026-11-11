package com.prostovasei.simpletasks.domain.model.finance

import java.time.LocalDate

/** Тип операции (финансы, этап 2, #225). [code] хранится в БД. Корректировка остатка появится на этапе 4. */
enum class OperationKind(val code: String) {
    EXPENSE("expense"),
    INCOME("income"),
    TRANSFER("transfer"),
    ;

    companion object {
        fun fromCode(code: String): OperationKind = entries.firstOrNull { it.code == code } ?: EXPENSE
    }
}

/** Категория расходов или доходов ([kind] — EXPENSE или INCOME). В архиве остаётся в старых операциях, для новых не предлагается. */
data class Category(val id: String, val name: String, val kind: OperationKind, val archived: Boolean)

/**
 * Операция: расход, доход или перевод между своими счетами. [amount] — положительная сумма в копейках.
 * У перевода есть [toAccountId], нет категории и места. [date] — день операции (без часового пояса).
 */
data class Operation(
    val id: String,
    val kind: OperationKind,
    val amount: Long,
    val accountId: String,
    val accountName: String,
    val toAccountId: String?,
    val toAccountName: String?,
    val categoryId: String?,
    val categoryName: String?,
    val place: String?,
    val comment: String?,
    val date: LocalDate,
)

/** Поля операции для создания и правки. */
data class OperationDraft(
    val kind: OperationKind,
    val amount: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val place: String? = null,
    val comment: String? = null,
    val date: LocalDate,
)

/**
 * Подсказки окна операции: счёт последней операции, недавние категории (новые первыми) и ранее введённые места.
 */
data class FormHints(
    val lastAccountId: String? = null,
    val recentCategoryIds: List<String> = emptyList(),
    val places: List<String> = emptyList(),
)
