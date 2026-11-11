package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickExporter
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickImporter
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** Счета финансов (этап 1, #221): создание, порядок, правка; очистка данных задач счета не трогает. */
class FinanceRepositoryTest : DatabaseTest() {
    private val finance by lazy { RoomFinanceRepository(db, time, UuidIdGenerator()) }

    @Test
    fun accountsAreCreatedInOrderWithBalance() = runTest {
        assertTrue(finance.observeAccounts().first().isEmpty())
        finance.createAccount(AccountDraft(" Карта ", AccountType.CARD, null, 4_230_000))
        time.advance(Duration.ofMinutes(1))
        finance.createAccount(AccountDraft("Заначка", AccountType.CASH, "🧦", 615_050))

        val accounts = finance.observeAccounts().first()
        assertEquals(listOf("Карта", "Заначка"), accounts.map { it.name })
        assertEquals(listOf(AccountType.CARD, AccountType.CASH), accounts.map { it.type })
        assertEquals(listOf("💳", "🧦"), accounts.map { it.emojiShown })
        assertNull(accounts[0].emoji)
        assertEquals(listOf(4_230_000L, 615_050L), accounts.map { it.balance })
        assertEquals(accounts.map { it.balance }, accounts.map { it.initialBalance })
    }

    @Test
    fun accountIsEdited() = runTest {
        val id = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 100))
        finance.updateAccount(id, AccountDraft("Вклад", AccountType.SAVINGS, "💰", 8_000_000))

        val account = finance.observeAccounts().first().single()
        assertEquals(id, account.id)
        assertEquals("Вклад", account.name)
        assertEquals(AccountType.SAVINGS, account.type)
        assertEquals("💰", account.emoji)
        assertEquals(8_000_000L, account.balance)
    }

    @Test
    fun blankNameIsRejected() = runTest {
        assertTrue(runCatching { finance.createAccount(AccountDraft("  ", AccountType.CARD, null, 0)) }.exceptionOrNull() is IllegalArgumentException)
    }

    /** «Очистить все данные» в настройках задач удаляет задачи и списки, но не финансы. */
    @Test
    fun clearingTaskDataKeepsAccounts() = runTest {
        finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 4_230_000))
        tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача"))
        val backup = RoomBackupRepository(db, time, TickTickImporter(db, time, UuidIdGenerator()), TickTickExporter(db, time))

        backup.clearAllData()

        assertEquals(0, backup.observeStats().first().totalTasks)
        assertTrue(projects.getInbox().isInbox)
        assertEquals(listOf("Карта"), finance.observeAccounts().first().map { it.name })
    }

    private suspend fun twoAccounts(): Pair<String, String> {
        val card = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 1_000_000))
        time.advance()
        val cash = finance.createAccount(AccountDraft("Наличные", AccountType.CASH, null, 200_000))
        return card to cash
    }

    private fun expense(account: String, amount: Long, date: String = "2026-10-13", category: String? = null, place: String? = null) =
        OperationDraft(OperationKind.EXPENSE, amount, account, categoryId = category, place = place, date = LocalDate.parse(date))

    /** Остаток = начальный + доходы − расходы − переводы со счёта + переводы на счёт; корзина не считается. */
    @Test
    fun balancesFollowOperations() = runTest {
        val (card, cash) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        finance.createOperation(OperationDraft(OperationKind.INCOME, 6_500_000, card, date = LocalDate.parse("2026-10-12")))
        val transfer = finance.createOperation(OperationDraft(OperationKind.TRANSFER, 500_000, card, toAccountId = cash, date = LocalDate.parse("2026-10-13")))

        fun balances() = kotlinx.coroutines.runBlocking { finance.observeAccounts().first().map { it.balance } }
        assertEquals(listOf(1_000_000L - 85_000 + 6_500_000 - 500_000, 200_000L + 500_000), balances())
        assertEquals(listOf(1_000_000L, 200_000L), finance.observeAccounts().first().map { it.initialBalance })

        // Правка перевода: сумма и направление — обе стороны пересчитываются согласованно.
        finance.updateOperation(transfer, OperationDraft(OperationKind.TRANSFER, 100_000, cash, toAccountId = card, date = LocalDate.parse("2026-10-13")))
        assertEquals(listOf(1_000_000L - 85_000 + 6_500_000 + 100_000, 200_000L - 100_000), balances())

        finance.trashOperation(transfer)
        assertEquals(listOf(1_000_000L - 85_000 + 6_500_000, 200_000L), balances())
        assertEquals(2, finance.observeOperations().first().size)
    }

    /** Список: новые дни первыми, внутри дня — последние введённые первыми; названия счёта и категории подставлены. */
    @Test
    fun operationsAreListedNewestFirstWithNames() = runTest {
        val (card, cash) = twoAccounts()
        val food = finance.createCategory(" Продукты ", OperationKind.EXPENSE)
        finance.createOperation(expense(card, 85_000, "2026-10-12", food, " Пятёрочка "))
        time.advance()
        finance.createOperation(expense(card, 62_000, "2026-10-13"))
        time.advance()
        finance.createOperation(OperationDraft(OperationKind.TRANSFER, 500_000, card, toAccountId = cash, categoryId = food, place = "x", comment = " в кошелёк ", date = LocalDate.parse("2026-10-13")))

        val list = finance.observeOperations().first()
        assertEquals(listOf(500_000L, 62_000L, 85_000L), list.map { it.amount })
        val transfer = list[0]
        assertEquals("Карта" to "Наличные", transfer.accountName to transfer.toAccountName)
        assertNull(transfer.categoryId)
        assertNull(transfer.place)
        assertEquals("в кошелёк", transfer.comment)
        assertNull(list[1].categoryName)
        assertEquals("Продукты" to "Пятёрочка", list[2].categoryName to list[2].place)
        assertEquals(LocalDate.parse("2026-10-12"), list[2].date)
    }

    @Test
    fun invalidOperationsAreRejected() = runTest {
        val (card, _) = twoAccounts()
        fun fails(draft: OperationDraft) = kotlinx.coroutines.runBlocking { runCatching { finance.createOperation(draft) }.exceptionOrNull() is IllegalArgumentException }
        assertTrue(fails(expense(card, 0)))
        assertTrue(fails(OperationDraft(OperationKind.TRANSFER, 100, card, toAccountId = card, date = LocalDate.parse("2026-10-13"))))
        assertTrue(fails(OperationDraft(OperationKind.TRANSFER, 100, card, date = LocalDate.parse("2026-10-13"))))
        assertTrue(finance.observeOperations().first().isEmpty())
    }

    /** Подсказки окна: счёт последней операции, категории и места по давности использования; корзина не учитывается. */
    @Test
    fun formHintsFollowRecentOperations() = runTest {
        val (card, cash) = twoAccounts()
        assertEquals(FormHints(), finance.observeFormHints().first())
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE)
        val cafe = finance.createCategory("Кафе", OperationKind.EXPENSE)
        finance.createOperation(expense(card, 100, category = food, place = "Пятёрочка"))
        time.advance()
        finance.createOperation(expense(card, 100, category = cafe, place = "Кофейня"))
        time.advance()
        finance.createOperation(expense(cash, 100, category = food, place = "Пятёрочка"))
        time.advance()
        val last = finance.createOperation(expense(card, 100, place = "Озон"))

        assertEquals(FormHints(card, listOf(food, cafe), listOf("Озон", "Пятёрочка", "Кофейня")), finance.observeFormHints().first())
        finance.trashOperation(last)
        assertEquals(FormHints(cash, listOf(food, cafe), listOf("Пятёрочка", "Кофейня")), finance.observeFormHints().first())
    }

    /** Категории: отдельно расходы и доходы, архив вместо удаления — в старых операциях название остаётся. */
    @Test
    fun categoriesAreRenamedAndArchived() = runTest {
        val (card, _) = twoAccounts()
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE)
        time.advance()
        val salary = finance.createCategory("Зарплата", OperationKind.INCOME)
        finance.createOperation(expense(card, 100, category = food))
        finance.renameCategory(food, " Еда ")
        finance.setCategoryArchived(food, true)

        assertEquals(listOf(Category(food, "Еда", OperationKind.EXPENSE, true), Category(salary, "Зарплата", OperationKind.INCOME, false)), finance.observeCategories().first())
        assertEquals("Еда", finance.observeOperations().first().single().categoryName)
        finance.setCategoryArchived(food, false)
        assertFalse(finance.observeCategories().first().first().archived)
        assertTrue(runCatching { finance.createCategory(" ", OperationKind.EXPENSE) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { finance.createCategory("Перевод", OperationKind.TRANSFER) }.exceptionOrNull() is IllegalArgumentException)
    }

    /** Правка остатка в окне счёта при имеющихся операциях: сдвигается начальный остаток, операции не меняются. */
    @Test
    fun editingBalanceShiftsInitialBalance() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        finance.updateAccount(card, AccountDraft("Карта", AccountType.CARD, null, 950_000))

        val account = finance.observeAccounts().first().first()
        assertEquals(950_000L, account.balance)
        assertEquals(1_035_000L, account.initialBalance)
    }
}
