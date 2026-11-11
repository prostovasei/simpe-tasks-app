package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import kotlinx.coroutines.flow.Flow

/** Финансы (#217). Этап 1 (#221): счета. Этап 2 (#225): операции и категории. Удаления и архива счетов нет — решение PM. */
interface FinanceRepository {
    /** Все счета в порядке создания; остаток — начальный плюс операции не из корзины. */
    fun observeAccounts(): Flow<List<Account>>

    /** Создаёт счёт; пустое название заменяется названием типа на стороне экрана. Возвращает id. */
    suspend fun createAccount(draft: AccountDraft): String

    /** Правит название, тип, эмодзи и остаток: начальный остаток сдвигается так, чтобы остаток сейчас стал равен указанному. */
    suspend fun updateAccount(id: String, draft: AccountDraft)

    /** Операции не из корзины: новые дни первыми, внутри дня — последние введённые первыми. */
    fun observeOperations(): Flow<List<Operation>>

    /** Все категории, включая архивные, в порядке создания. */
    fun observeCategories(): Flow<List<Category>>

    fun observeFormHints(): Flow<FormHints>

    /** @throws IllegalArgumentException сумма не больше нуля, перевод на тот же счёт или без второго счёта. */
    suspend fun createOperation(draft: OperationDraft): String

    suspend fun updateOperation(id: String, draft: OperationDraft)

    /** В корзину: операция перестаёт влиять на остатки и списки, но не стирается (экран корзины — этап 4). */
    suspend fun trashOperation(id: String)

    suspend fun createCategory(name: String, kind: OperationKind): String

    suspend fun renameCategory(id: String, name: String)

    suspend fun setCategoryArchived(id: String, archived: Boolean)
}
