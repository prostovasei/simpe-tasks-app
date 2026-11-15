package com.prostovasei.simpletasks.domain.model.finance

import java.time.LocalDate

/**
 * Тип операции (финансы, этап 2, #225). [code] хранится в БД. [CORRECTION] — корректировка остатка при сверке (этап 4, #231):
 * единственный тип со знаковой суммой; в доходы и расходы не входит, создаётся только сверкой.
 */
enum class OperationKind(val code: String) {
    EXPENSE("expense"),
    INCOME("income"),
    TRANSFER("transfer"),
    CORRECTION("correction"),
    ;

    companion object {
        fun fromCode(code: String): OperationKind = entries.firstOrNull { it.code == code } ?: EXPENSE

        /** Типы, которые человек выбирает в окне операции. */
        val MANUAL = listOf(EXPENSE, INCOME, TRANSFER)
    }
}

/** Категория расходов или доходов ([kind] — EXPENSE или INCOME). В архиве остаётся в старых операциях, для новых не предлагается. [emoji] — значок категории (#237), необязательный. */
data class Category(val id: String, val name: String, val kind: OperationKind, val archived: Boolean, val emoji: String? = null, val uses: Int = 0)

/** Место из справочника мест (#239). [uses] — сколько операций с этим местом (вместе с корзиной): для предупреждения об удалении. */
data class Place(val id: String, val name: String, val archived: Boolean, val uses: Int = 0)

/**
 * Операция: расход, доход или перевод между своими счетами. [amount] — положительная сумма в копейках (у корректировки — со знаком).
 * [deletedOn] — день, когда операцию отправили в корзину (только в списке корзины).
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
    val deletedOn: LocalDate? = null,
    /** Значки для плашки (#245): эмодзи категории (null — не выбран), счёта и счёта назначения (у счёта без своего — значок его типа). */
    val categoryEmoji: String? = null,
    val accountEmoji: String = "",
    val toAccountEmoji: String? = null,
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

/** Сколько финансовых данных: в приложении сейчас или в файле резервной копии. Операции — вместе с корзиной. */
data class FinanceCounts(val accounts: Int = 0, val categories: Int = 0, val operations: Int = 0)

/** Файл не является резервной копией финансов Simple Tasks или повреждён. */
class InvalidFinanceBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
