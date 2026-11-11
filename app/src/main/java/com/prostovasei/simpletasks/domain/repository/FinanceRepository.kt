package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import kotlinx.coroutines.flow.Flow

/** Финансы (#217). Этап 1 (#221): счета. Удаления и архива счетов нет — решение PM. */
interface FinanceRepository {
    /** Все счета в порядке создания. */
    fun observeAccounts(): Flow<List<Account>>

    /** Создаёт счёт; пустое название заменяется названием типа на стороне экрана. Возвращает id. */
    suspend fun createAccount(draft: AccountDraft): String

    /** Правит название, тип, эмодзи и остаток. Пока операций нет, остаток — это начальный остаток. */
    suspend fun updateAccount(id: String, draft: AccountDraft)
}
