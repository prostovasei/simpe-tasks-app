package com.prostovasei.simpletasks.data.repository

import androidx.room.withTransaction
import com.prostovasei.simpletasks.core.id.IdGenerator
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.local.entity.BankPurchaseEntity
import com.prostovasei.simpletasks.data.local.entity.PendingPurchaseRow
import com.prostovasei.simpletasks.domain.bank.BankPurchaseNotifier
import com.prostovasei.simpletasks.domain.bank.BankSettingsStore
import com.prostovasei.simpletasks.domain.bank.NotificationAccess
import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.DuplicateHint
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import com.prostovasei.simpletasks.domain.repository.BankPurchases
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Покупки из уведомлений Яндекс Банка (#233): «Не разобрано» в Room, настройки — в [BankSettingsStore]. */
@Singleton
class RoomBankPurchases @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val finance: FinanceRepository,
    private val store: BankSettingsStore,
    private val notifier: BankPurchaseNotifier,
    private val access: NotificationAccess,
) : BankPurchases {
    private val purchases = db.bankPurchaseDao()

    private fun now() = time.now().toEpochMilli()

    override fun observeSettings(): Flow<BankSettings> = store.settings

    override fun hasAccess(): Boolean = access.granted()

    override suspend fun setEnabled(enabled: Boolean) = store.update { copy(enabled = enabled) }

    override suspend fun setAccount(accountId: String) = store.update { copy(accountId = accountId) }

    override fun observePending(): Flow<List<PendingPurchase>> =
        purchases.observePending().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun onPurchase(key: String, amount: Long, at: Instant): PendingPurchase? {
        val settings = store.settings.value
        val accountId = settings.accountId?.takeIf { settings.enabled && amount > 0 } ?: return null
        val date = at.atZone(time.zone()).toLocalDate().toEpochDay()
        val id = ids.newId()
        val row = db.withTransaction {
            // Счёт мог пропасть после восстановления копии — тогда записывать некуда.
            if (db.financeAccountDao().getWithBalance(accountId) == null) return@withTransaction null
            purchases.pruneAnswered(now() - KEEP_ANSWERED.toMillis())
            val entity = BankPurchaseEntity(
                id = id,
                notificationKey = key,
                amount = amount,
                accountId = accountId,
                postedAt = at.toEpochMilli(),
                date = date,
                duplicateId = purchases.findDuplicate(accountId, amount, date),
                updatedAt = now(),
            )
            if (purchases.insert(entity) == -1L) null else purchases.getRow(id)
        } ?: return null
        return row.toDomain().also(notifier::show)
    }

    override suspend fun record(id: String) {
        answer(id) { purchase -> OperationDraft(OperationKind.EXPENSE, purchase.amount, purchase.accountId, date = LocalDate.ofEpochDay(purchase.date)) }
    }

    override suspend fun recordAs(id: String, draft: OperationDraft) {
        answer(id) { draft }
    }

    override suspend fun skip(id: String) {
        purchases.answer(id, BankPurchaseEntity.SKIPPED, null, now())
        notifier.cancel(id)
    }

    /** Запись операции и отметка покупки — одной транзакцией: повторное нажатие второй операции не создаст. */
    private suspend fun answer(id: String, draft: (BankPurchaseEntity) -> OperationDraft) {
        db.withTransaction {
            val purchase = purchases.getPending(id) ?: return@withTransaction
            val operationId = finance.createOperation(draft(purchase))
            purchases.answer(id, BankPurchaseEntity.RECORDED, operationId, now())
        }
        notifier.cancel(id)
    }

    private fun PendingPurchaseRow.toDomain() = PendingPurchase(
        id = purchase.id,
        amount = purchase.amount,
        accountId = purchase.accountId,
        accountName = accountName,
        postedAt = Instant.ofEpochMilli(purchase.postedAt),
        date = LocalDate.ofEpochDay(purchase.date),
        duplicate = if (duplicateId != null && duplicateAmount != null && duplicateDate != null) {
            DuplicateHint(duplicateCategory, duplicateAmount, LocalDate.ofEpochDay(duplicateDate))
        } else {
            null
        },
    )

    private companion object {
        /** Сколько помнить разобранные покупки, чтобы повтор того же уведомления не создал новую. */
        val KEEP_ANSWERED: Duration = Duration.ofDays(7)
    }
}
