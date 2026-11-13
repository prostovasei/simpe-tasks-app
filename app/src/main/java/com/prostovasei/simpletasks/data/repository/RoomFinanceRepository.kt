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
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
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
        accounts.update(id, draft.name.trim(), draft.type.code, draft.emoji, now())
    }

    override suspend fun setInitialBalance(id: String, initialBalance: Long) {
        accounts.setInitialBalance(id, initialBalance, now())
    }

    override suspend fun correctBalance(id: String, realBalance: Long, date: LocalDate): Long = db.withTransaction {
        val current = accounts.getWithBalance(id) ?: return@withTransaction 0L
        val difference = realBalance - current.balance
        if (difference != 0L) {
            val now = now()
            operations.insert(
                FinanceOperationEntity(ids.newId(), OperationKind.CORRECTION.code, difference, accountId = id, date = date.toEpochDay(), createdAt = now, updatedAt = now),
            )
        }
        difference
    }

    override fun observeTrash(): Flow<List<Operation>> =
        operations.observeTrash().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun restoreOperation(id: String) {
        operations.restore(id, now())
    }

    override suspend fun deleteOperationForever(id: String) {
        operations.deleteTrashed(id)
    }

    override suspend fun emptyTrash(): Int = operations.emptyTrash()

    override fun observeCounts(): Flow<FinanceCounts> =
        combine(accounts.observeCount(), operations.observeCategoryCount(), operations.observeCount()) { a, c, o -> FinanceCounts(a, c, o) }.distinctUntilChanged()

    override suspend fun exportBackup(): String = db.withTransaction {
        FinanceBackupCodec.encode(FinanceBackup(accounts.getAll(), operations.getAllCategories(), operations.getAll()), exportedAt = now())
    }

    override fun inspectBackup(json: String): FinanceCounts =
        FinanceBackupCodec.decode(json).let { FinanceCounts(it.accounts.size, it.categories.size, it.operations.size) }

    override suspend fun restoreBackup(json: String) {
        // Сначала полный разбор и проверка: негодный файл не должен стереть данные.
        val backup = FinanceBackupCodec.decode(json)
        db.withTransaction {
            // «Не разобрано» в копию не входит (решение PM, #233) и ссылается на прежние счета — очищается.
            db.bankPurchaseDao().deleteAll()
            operations.deleteAll()
            operations.deleteAllCategories()
            accounts.deleteAll()
            accounts.insertAll(backup.accounts)
            operations.insertAllCategories(backup.categories)
            operations.insertAll(backup.operations)
        }
    }

    override fun observeOperations(): Flow<List<Operation>> =
        operations.observeActive().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeCategories(): Flow<List<Category>> =
        operations.observeCategories().map { list -> list.map { Category(it.id, it.name, OperationKind.fromCode(it.kind), it.isArchived, it.emoji) } }.distinctUntilChanged()

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
            // Корректировку сверки не правят — только корзина.

            val current = operations.getById(id) ?: return@withTransaction
            operations.update(checked.toEntity(id, createdAt = current.createdAt, updatedAt = now()).copy(isDeleted = current.isDeleted, deletedAt = current.deletedAt))
        }
    }

    override suspend fun trashOperation(id: String) {
        operations.trash(id, now())
    }

    override suspend fun createCategory(name: String, kind: OperationKind, emoji: String?): String {
        require(name.isNotBlank()) { "Название категории не может быть пустым" }
        require(kind != OperationKind.TRANSFER) { "У переводов нет категорий" }
        val id = ids.newId()
        val now = now()
        db.withTransaction {
            operations.insertCategory(FinanceCategoryEntity(id, name.trim(), kind.code, sortOrder = operations.nextCategoryOrder(), createdAt = now, updatedAt = now, emoji = emoji?.trim()?.takeIf { it.isNotEmpty() }))
        }
        return id
    }

    override suspend fun updateCategory(id: String, name: String, emoji: String?) {
        require(name.isNotBlank()) { "Название категории не может быть пустым" }
        operations.updateCategory(id, name.trim(), emoji?.trim()?.takeIf { it.isNotEmpty() }, now())
    }

    override suspend fun setCategoryArchived(id: String, archived: Boolean) {
        operations.setCategoryArchived(id, archived, now())
    }

    /** Проверяет и приводит поля: у перевода нет категории и места, у расхода и дохода — второго счёта; пустые строки — null. */
    private fun OperationDraft.checked(): OperationDraft {
        require(kind != OperationKind.CORRECTION) { "Корректировку создаёт только сверка" }
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
        deletedOn = operation.deletedAt?.let { Instant.ofEpochMilli(it).atZone(time.zone()).toLocalDate() },
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
