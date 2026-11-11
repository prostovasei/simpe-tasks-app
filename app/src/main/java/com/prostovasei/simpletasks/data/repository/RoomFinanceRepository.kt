package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.FinanceAccountEntity
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomFinanceRepository @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : FinanceRepository {
    private val accounts = db.financeAccountDao()

    override fun observeAccounts(): Flow<List<Account>> =
        accounts.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun createAccount(draft: AccountDraft): String {
        require(draft.name.isNotBlank()) { "Название счёта не может быть пустым" }
        val id = ids.newId()
        val now = time.now().toEpochMilli()
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
        // Этап 1: операций ещё нет, остаток сейчас и есть начальный остаток.
        accounts.update(id, draft.name.trim(), draft.type.code, draft.emoji, draft.balance, time.now().toEpochMilli())
    }

    private fun FinanceAccountEntity.toDomain() = Account(
        id = id,
        name = name,
        type = AccountType.fromCode(type),
        emoji = emoji,
        initialBalance = initialBalance,
        balance = initialBalance,
        createdAt = Instant.ofEpochMilli(createdAt),
    )
}
