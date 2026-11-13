package com.prostovasei.simpletasks.domain.repository

import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
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

    /** Правит название, тип и эмодзи; остаток окно счёта не меняет ([AccountDraft.balance] не используется) — для него сверка. */
    suspend fun updateAccount(id: String, draft: AccountDraft)

    /** Сверка, способ 2: новый начальный остаток; операции не меняются, остаток сейчас пересчитывается. */
    suspend fun setInitialBalance(id: String, initialBalance: Long)

    /**
     * Сверка, способ 3: реальный остаток сейчас. Создаёт корректировку на разницу в день [date] — она видна в операциях,
     * но не входит в доходы и расходы. Разницы нет — ничего не создаётся. Возвращает сумму корректировки.
     */
    suspend fun correctBalance(id: String, realBalance: Long, date: java.time.LocalDate): Long

    /** Операции не из корзины: новые дни первыми, внутри дня — последние введённые первыми. */
    fun observeOperations(): Flow<List<Operation>>

    /** Все категории, включая архивные, в порядке создания. */
    fun observeCategories(): Flow<List<Category>>

    fun observeFormHints(): Flow<FormHints>

    /** @throws IllegalArgumentException сумма не больше нуля, перевод на тот же счёт или без второго счёта. */
    suspend fun createOperation(draft: OperationDraft): String

    suspend fun updateOperation(id: String, draft: OperationDraft)

    /** В корзину: операция перестаёт влиять на остатки, списки и аналитику, но не стирается. */
    suspend fun trashOperation(id: String)

    /** Корзина: недавно удалённые первыми. */
    fun observeTrash(): Flow<List<Operation>>

    /** Из корзины обратно: операция (перевод — целиком) снова учитывается. */
    suspend fun restoreOperation(id: String)

    /** Окончательное удаление одной операции из корзины. */
    suspend fun deleteOperationForever(id: String)

    /** Окончательное удаление всей корзины. Возвращает число удалённых операций. */
    suspend fun emptyTrash(): Int

    fun observeCounts(): Flow<FinanceCounts>

    /** Резервная копия финансов текстом JSON: счета с начальными остатками, категории, операции (с корзиной). */
    suspend fun exportBackup(): String

    /** Что в файле копии. @throws InvalidFinanceBackupException файл не подходит. */
    fun inspectBackup(json: String): FinanceCounts

    /**
     * Полностью заменяет финансовые данные содержимым копии; задачи не затрагиваются.
     * @throws InvalidFinanceBackupException файл не подходит — данные остаются как были.
     */
    suspend fun restoreBackup(json: String)

    suspend fun createCategory(name: String, kind: OperationKind, emoji: String? = null): String

    /** Правка названия и эмодзи категории (#237); [emoji] = null — значка нет. */
    suspend fun updateCategory(id: String, name: String, emoji: String?)

    suspend fun setCategoryArchived(id: String, archived: Boolean)
}
