package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.AccountWithBalance
import com.prostovasei.simpletasks.data.local.entity.FinanceAccountEntity
import com.prostovasei.simpletasks.data.local.entity.FinanceCategoryEntity
import com.prostovasei.simpletasks.data.local.entity.FinanceOperationEntity
import com.prostovasei.simpletasks.data.local.entity.OperationRow
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class RoomFinanceRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : FinanceRepository {
    private val accounts = db.financeAccountDao()
    private val operations = db.financeOperationDao()

    private fun now() = time.now().toEpochMilli()

    override fun observeAccounts(): Flow<List<Account>> =
        accounts.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun createAccount(draft: AccountDraft): String {
        require(draft.name.isNotBlank()) { "Название счёта не может быть пустым" }
        val id = ids.newId()
        val now = now()
        db.withTransaction {
            accounts.insert(
                FinanceAccountEntity(
                    id = id,
                    name = draft.name.trim(),
                    type = draft.type.code,
                    emoji = draft.emoji,
                    initialBalance = draft.balance,
                    sortOrder = accounts.nextSortOrder(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return id
    }

    override suspend fun updateAccount(id: String, draft: AccountDraft) {
        require(draft.name.isNotBlank()) { "Название счёта не может быть пустым" }
        db.withTransaction {
            val current = accounts.getWithBalance(id) ?: return@withTransaction
            // Остаток сейчас = начальный + операции. Указали новый остаток — сдвигаем начальный, операции не трогаем.
            val operationsSum = current.balance - current.account.initialBalance
            accounts.update(id, draft.name.trim(), draft.type.code, draft.emoji, draft.balance - operationsSum, now())
        }
    }

    override fun observeOperations(): Flow<List<Operation>> =
        operations.observeActive().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeCategories(): Flow<List<Category>> =
        operations.observeCategories().map { list -> list.map { Category(it.id, it.name, OperationKind.fromCode(it.kind), it.isArchived) } }.distinctUntilChanged()

    override fun observeFormHints(): Flow<FormHints> =
        combine(operations.observeLastAccount(), operations.observeRecentCategories(), operations.observePlaces()) { last, recent, places ->
            FormHints(lastAccountId = last.firstOrNull(), recentCategoryIds = recent, places = places)
        }.distinctUntilChanged()

    override suspend fun createOperation(draft: OperationDraft): String {
        val checked = draft.checked()
        val id = ids.newId()
        val now = now()
        operations.insert(checked.toEntity(id, createdAt = now, updatedAt = now))
        return id
    }

    override suspend fun updateOperation(id: String, draft: OperationDraft) {
        val checked = draft.checked()
        db.withTransaction {
            val current = operations.getById(id) ?: return@withTransaction
            operations.update(checked.toEntity(id, createdAt = current.createdAt, updatedAt = now()).copy(isDeleted = current.isDeleted, deletedAt = current.deletedAt))
        }
    }

    override suspend fun trashOperation(id: String) {
        operations.trash(id, now())
    }

    override suspend fun createCategory(name: String, kind: OperationKind): String {
        require(name.isNotBlank()) { "Название категории не может быть пустым" }
        require(kind != OperationKind.TRANSFER) { "У переводов нет категорий" }
        val id = ids.newId()
        val now = now()
        db.withTransaction {
            operations.insertCategory(FinanceCategoryEntity(id, name.trim(), kind.code, sortOrder = operations.nextCategoryOrder(), createdAt = now, updatedAt = now))
        }
        return id
    }

    override suspend fun renameCategory(id: String, name: String) {
        require(name.isNotBlank()) { "Название категории не может быть пустым" }
        operations.renameCategory(id, name.trim(), now())
    }

    override suspend fun setCategoryArchived(id: String, archived: Boolean) {
        operations.setCategoryArchived(id, archived, now())
    }

    /** Проверяет и приводит поля: у перевода нет категории и места, у расхода и дохода — второго счёта; пустые строки — null. */
    private fun OperationDraft.checked(): OperationDraft {
        require(amount > 0) { "Сумма операции должна быть больше нуля" }
        return if (kind == OperationKind.TRANSFER) {
            require(toAccountId != null && toAccountId != accountId) { "Перевод — между двумя разными счетами" }
            copy(categoryId = null, place = null, comment = comment.clean())
        } else {
            copy(toAccountId = null, place = place.clean(), comment = comment.clean())
        }
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun OperationDraft.toEntity(id: String, createdAt: Long, updatedAt: Long) = FinanceOperationEntity(
        id = id,
        kind = kind.code,
        amount = amount,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        place = place,
        comment = comment,
        date = date.toEpochDay(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun OperationRow.toDomain() = Operation(
        id = operation.id,
        kind = OperationKind.fromCode(operation.kind),
        amount = operation.amount,
        accountId = operation.accountId,
        accountName = accountName,
        toAccountId = operation.toAccountId,
        toAccountName = toAccountName,
        categoryId = operation.categoryId,
        categoryName = categoryName,
        place = operation.place,
        comment = operation.comment,
        date = LocalDate.ofEpochDay(operation.date),
    )

    private fun AccountWithBalance.toDomain() = Account(
        id = account.id,
        name = account.name,
        type = AccountType.fromCode(account.type),
        emoji = account.emoji,
        initialBalance = account.initialBalance,
        balance = balance,
        createdAt = Instant.ofEpochMilli(account.createdAt),
    )
}
