package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.domain.bank.BankPurchaseNotifier
import com.prostovasei.simpletasks.domain.bank.BankSettingsStore
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.DuplicateHint
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Покупки из уведомлений банка (#233): «Не разобрано», ответы, повторы, дубли, связь с резервной копией. */
class BankPurchasesTest : DatabaseTest() {
    private val ids = UuidIdGenerator()
    private val finance by lazy { RoomFinanceRepository(db, time, ids) }
    private val store = object : BankSettingsStore {
        override val settings = MutableStateFlow(BankSettings())
        override fun update(change: BankSettings.() -> BankSettings) {
            settings.value = settings.value.change()
        }
    }
    private val shown = mutableListOf<PendingPurchase>()
    private val cancelled = mutableListOf<String>()
    private val notifier = object : BankPurchaseNotifier {
        override fun show(purchase: PendingPurchase) {
            shown += purchase
        }
        override fun cancel(purchaseId: String) {
            cancelled += purchaseId
        }
    }
    private val bank by lazy { RoomBankPurchases(db, time, ids, finance, store, notifier) { true } }

    /** 13.10.2026, 15:00 по Москве. */
    private val at = Instant.parse("2026-10-13T12:00:00Z")
    private val day = LocalDate.parse("2026-10-13")

    private suspend fun card(): String {
        val id = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 1_000_000))
        bank.setAccount(id)
        bank.setEnabled(true)
        return id
    }

    @Test
    fun nothingIsCreatedWhileDisabledOrWithoutAccount() = runTest {
        assertNull(bank.onPurchase("k1", 68_000, at))
        val id = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 0))
        bank.setAccount(id)
        assertNull(bank.onPurchase("k1", 68_000, at))
        bank.setEnabled(true)
        bank.setAccount("missing")
        assertNull(bank.onPurchase("k1", 68_000, at))
        assertTrue(bank.observePending().first().isEmpty())
        assertTrue(shown.isEmpty())
    }

    @Test
    fun newPurchaseIsPendingAndShownOnce() = runTest {
        val account = card()
        val purchase = bank.onPurchase("k1", 68_000, at)
        assertNotNull(purchase)
        assertEquals(PendingPurchase(purchase!!.id, 68_000, account, "Карта", at, day), purchase)
        assertEquals(listOf(purchase), shown)
        // То же уведомление ещё раз (обновлено или показано повторно) — новой покупки нет (§4.6).
        assertNull(bank.onPurchase("k1", 68_000, at))
        assertEquals(listOf(purchase), bank.observePending().first())
    }

    @Test
    fun recordCreatesUncategorizedExpenseOnPurchaseDay() = runTest {
        val account = card()
        val purchase = bank.onPurchase("k1", 68_000, at.minusSeconds(86_400))!!
        bank.record(purchase.id)
        bank.record(purchase.id)

        val operation = finance.observeOperations().first().single()
        assertEquals(OperationKind.EXPENSE, operation.kind)
        assertEquals(68_000L, operation.amount)
        assertEquals(account, operation.accountId)
        assertNull(operation.categoryId)
        assertNull(operation.place)
        assertEquals(day.minusDays(1), operation.date)
        assertTrue(bank.observePending().first().isEmpty())
        assertEquals(listOf(purchase.id, purchase.id), cancelled)
        assertEquals(1_000_000L - 68_000L, finance.observeAccounts().first().single().balance)
        // Ответили — повтор того же уведомления покупку не возвращает.
        assertNull(bank.onPurchase("k1", 68_000, at))
    }

    @Test
    fun recordAsSavesEditedOperation() = runTest {
        val account = card()
        val category = finance.createCategory("Кафе", OperationKind.EXPENSE)
        val purchase = bank.onPurchase("k1", 68_000, at)!!
        bank.recordAs(purchase.id, OperationDraft(OperationKind.EXPENSE, 70_000, account, categoryId = category, place = "Кофейня", date = day))

        val operation = finance.observeOperations().first().single()
        assertEquals(70_000L, operation.amount)
        assertEquals("Кафе", operation.categoryName)
        assertTrue(bank.observePending().first().isEmpty())
    }

    @Test
    fun skipWritesNothing() = runTest {
        card()
        val purchase = bank.onPurchase("k1", 68_000, at)!!
        bank.skip(purchase.id)
        assertTrue(finance.observeOperations().first().isEmpty())
        assertTrue(bank.observePending().first().isEmpty())
        assertEquals(listOf(purchase.id), cancelled)
    }

    @Test
    fun similarExpenseIsMarkedButNotSkipped() = runTest {
        val account = card()
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE)
        finance.createOperation(OperationDraft(OperationKind.EXPENSE, 124_000, account, categoryId = food, date = day))

        val first = bank.onPurchase("k1", 124_000, at)!!
        assertEquals(DuplicateHint("Продукты", 124_000, day), first.duplicate)
        // Другой счёт, сумма или день — не дубль.
        assertNull(bank.onPurchase("k2", 124_100, at)!!.duplicate)
        assertNull(bank.onPurchase("k3", 124_000, at.plusSeconds(86_400))!!.duplicate)
        assertEquals(3, bank.observePending().first().size)
    }

    @Test
    fun expenseLinkedToAnotherPurchaseIsNotDuplicate() = runTest {
        card()
        val first = bank.onPurchase("k1", 50_000, at)!!
        bank.record(first.id)
        // Тот же день и сумма, но этот расход уже связан с первой покупкой — две одинаковые покупки подряд не дубли.
        assertNull(bank.onPurchase("k2", 50_000, at.plusSeconds(60))!!.duplicate)
    }

    @Test
    fun trashedDuplicateLosesMark() = runTest {
        val account = card()
        val manual = finance.createOperation(OperationDraft(OperationKind.EXPENSE, 30_000, account, date = day))
        val purchase = bank.onPurchase("k1", 30_000, at)!!
        assertEquals(DuplicateHint(null, 30_000, day), purchase.duplicate)
        finance.trashOperation(manual)
        assertNull(bank.observePending().first().single().duplicate)
    }

    @Test
    fun restoringBackupClearsUnsorted() = runTest {
        card()
        bank.onPurchase("k1", 68_000, at)
        val backup = finance.exportBackup()
        finance.restoreBackup(backup)
        assertTrue(bank.observePending().first().isEmpty())
    }
}
