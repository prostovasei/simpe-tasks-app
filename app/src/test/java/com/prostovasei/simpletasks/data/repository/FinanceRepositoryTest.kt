package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickExporter
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickImporter
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
import com.prostovasei.simpletasks.domain.model.finance.InvalidFinanceBackupException
import com.prostovasei.simpletasks.domain.model.finance.MonthAnalytics
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
import java.time.YearMonth

/** Счета финансов (этап 1, #221): создание, порядок, правка; очистка данных задач счета не трогает. */
class FinanceRepositoryTest : DatabaseTest() {
    private val finance by lazy { RoomFinanceRepository(db, time, UuidIdGenerator()) }

    /** #245: операция несёт значки для плашки — эмодзи категории и счетов (у счёта без своего — значок его типа). */
    @Test
    fun operationCarriesIconsOfCategoryAndAccounts() = runTest {
        val card = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 0))
        val cash = finance.createAccount(AccountDraft("Наличные", AccountType.CASH, "💰", 0))
        val food = finance.createCategory("Еда", OperationKind.EXPENSE, "🍽")
        val day = LocalDate.parse("2026-11-14")
        finance.createOperation(OperationDraft(OperationKind.EXPENSE, 40_000, card, categoryId = food, place = "Пятёрочка", comment = "к ужину", date = day))
        finance.createOperation(OperationDraft(OperationKind.TRANSFER, 500_000, card, toAccountId = cash, date = day))
        finance.createOperation(OperationDraft(OperationKind.EXPENSE, 10_000, cash, date = day))

        val ops = finance.observeOperations().first()
        val expense = ops.single { it.categoryName == "Еда" }
        assertEquals(Triple("🍽", "💳", null), Triple(expense.categoryEmoji, expense.accountEmoji, expense.toAccountEmoji))
        val transfer = ops.single { it.kind == OperationKind.TRANSFER }
        assertEquals("💳" to "💰", transfer.accountEmoji to transfer.toAccountEmoji)
        val none = ops.single { it.kind == OperationKind.EXPENSE && it.categoryName == null }
        assertNull(none.categoryEmoji)
        assertEquals("💰", none.accountEmoji)
    }

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
        // Остаток окно счёта не меняет (этап 4): для него сверка.
        assertEquals(100L, account.balance)
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
        // #239: места — из справочника; «Озон» остался в нём, но без действующих операций — в конце.
        assertEquals(FormHints(cash, listOf(food, cafe), listOf("Пятёрочка", "Кофейня", "Озон")), finance.observeFormHints().first())
        // Архивное место не предлагается.
        finance.setPlaceArchived(finance.observePlaces().first().single { it.name == "Кофейня" }.id, true)
        assertEquals(listOf("Пятёрочка", "Озон"), finance.observeFormHints().first().places)
    }

    /** Категории: отдельно расходы и доходы, архив вместо удаления — в старых операциях название остаётся. */
    @Test
    fun categoriesAreRenamedAndArchived() = runTest {
        val (card, _) = twoAccounts()
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE)
        time.advance()
        val salary = finance.createCategory("Зарплата", OperationKind.INCOME)
        finance.createOperation(expense(card, 100, category = food))
        finance.updateCategory(food, " Еда ", "🍔")
        finance.setCategoryArchived(food, true)

        assertEquals(listOf(Category(food, "Еда", OperationKind.EXPENSE, true, "🍔", uses = 1), Category(salary, "Зарплата", OperationKind.INCOME, false)), finance.observeCategories().first())
        assertEquals("Еда", finance.observeOperations().first().single().categoryName)
        finance.setCategoryArchived(food, false)
        assertFalse(finance.observeCategories().first().first().archived)
        assertTrue(runCatching { finance.createCategory(" ", OperationKind.EXPENSE) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { finance.createCategory("Перевод", OperationKind.TRANSFER) }.exceptionOrNull() is IllegalArgumentException)
    }

    /** Правка счёта меняет название, тип и эмодзи; остаток окно не трогает. */
    @Test
    fun editingAccountKeepsBalance() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        finance.updateAccount(card, AccountDraft("Яндекс", AccountType.SAVINGS, "💰", 1))

        val account = finance.observeAccounts().first().first()
        assertEquals("Яндекс", account.name)
        assertEquals(915_000L, account.balance)
        assertEquals(1_000_000L, account.initialBalance)
    }

    /** Сверка, способ 2: новый начальный остаток — операции те же, остаток сейчас сдвинулся на ту же величину. */
    @Test
    fun initialBalanceChangeShiftsCurrentBalance() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        finance.setInitialBalance(card, 1_015_000)

        val account = finance.observeAccounts().first().first()
        assertEquals(1_015_000L to 930_000L, account.initialBalance to account.balance)
        assertEquals(1, finance.observeOperations().first().size)
    }

    /** Сверка, способ 3: корректировка на разницу — видна в операциях, двигает остаток, не считается расходом; без разницы не создаётся. */
    @Test
    fun realBalanceCreatesCorrection() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        val day = LocalDate.parse("2026-10-13")
        time.advance()

        assertEquals(15_000L, finance.correctBalance(card, 930_000, day))
        assertEquals(0L, finance.correctBalance(card, 930_000, day))
        time.advance()
        assertEquals(-30_000L, finance.correctBalance(card, 900_000, day))

        val account = finance.observeAccounts().first().first()
        assertEquals(1_000_000L to 900_000L, account.initialBalance to account.balance)
        val list = finance.observeOperations().first()
        assertEquals(listOf(OperationKind.CORRECTION, OperationKind.CORRECTION, OperationKind.EXPENSE), list.map { it.kind })
        assertEquals(listOf(-30_000L, 15_000L, 85_000L), list.map { it.amount })
        assertEquals(85_000L, MonthAnalytics.of(list, YearMonth.of(2026, 10), day).total)
        // Корректировка не подсказывает счёт и её нельзя создать или исправить вручную.
        assertEquals(card, finance.observeFormHints().first().lastAccountId)
        assertTrue(runCatching { finance.createOperation(OperationDraft(OperationKind.CORRECTION, 100, card, date = day)) }.exceptionOrNull() is IllegalArgumentException)

        finance.trashOperation(list[0].id)
        assertEquals(930_000L, finance.observeAccounts().first().first().balance)
    }

    /** Корзина: удалённые недавно — первыми, с днём удаления; восстановление возвращает в остаток; удаление навсегда и очистка. */
    @Test
    fun trashRestoresAndDeletesForever() = runTest {
        val (card, cash) = twoAccounts()
        val a = finance.createOperation(expense(card, 85_000))
        val transfer = finance.createOperation(OperationDraft(OperationKind.TRANSFER, 500_000, card, toAccountId = cash, date = LocalDate.parse("2026-10-13")))
        val c = finance.createOperation(expense(card, 10_000))
        finance.trashOperation(a)
        time.advance(Duration.ofDays(1))
        finance.trashOperation(transfer)
        finance.trashOperation(c)

        val trash = finance.observeTrash().first()
        assertEquals(setOf(transfer, c), trash.take(2).map { it.id }.toSet())
        assertEquals(a, trash[2].id)
        assertEquals(LocalDate.parse("2026-10-13"), trash[2].deletedOn)
        assertEquals(LocalDate.parse("2026-10-14"), trash[0].deletedOn)
        assertEquals(listOf(1_000_000L, 200_000L), finance.observeAccounts().first().map { it.balance })

        // Перевод восстанавливается целиком: обе стороны.
        finance.restoreOperation(transfer)
        assertEquals(listOf(500_000L, 700_000L), finance.observeAccounts().first().map { it.balance })
        assertNull(finance.observeOperations().first().single().deletedOn)

        // Удалить навсегда можно только то, что в корзине.
        finance.deleteOperationForever(transfer)
        assertEquals(1, finance.observeOperations().first().size)
        finance.deleteOperationForever(a)
        assertEquals(listOf(c), finance.observeTrash().first().map { it.id })
        assertEquals(1, finance.emptyTrash())
        assertTrue(finance.observeTrash().first().isEmpty())
        assertEquals(FinanceCounts(2, 0, 1), finance.observeCounts().first())
    }

    /** Резервная копия: файл восстанавливает счета, начальные остатки, категории (с архивом), операции и корзину; задачи не трогает. */
    @Test
    fun backupRoundTripReplacesFinanceDataOnly() = runTest {
        val (card, cash) = twoAccounts()
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE, "🛒")
        val old = finance.createCategory("Такси", OperationKind.EXPENSE)
        finance.setCategoryArchived(old, true)
        finance.createOperation(expense(card, 85_000, category = food, place = "Пятёрочка"))
        finance.createOperation(OperationDraft(OperationKind.TRANSFER, 500_000, card, toAccountId = cash, comment = "в кошелёк", date = LocalDate.parse("2026-10-12")))
        finance.correctBalance(cash, 690_000, LocalDate.parse("2026-10-13"))
        val trashed = finance.createOperation(expense(card, 10_000, category = old))
        finance.trashOperation(trashed)
        tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача"))

        val json = finance.exportBackup()
        assertEquals(FinanceCounts(2, 2, 4), finance.inspectBackup(json))
        val accountsBefore = finance.observeAccounts().first()
        val operationsBefore = finance.observeOperations().first()
        val categoriesBefore = finance.observeCategories().first()
        val trashBefore = finance.observeTrash().first()

        // После копии данные меняются — восстановление возвращает состояние копии целиком.
        finance.createAccount(AccountDraft("Лишний", AccountType.CARD, null, 5))
        finance.createOperation(expense(card, 777))
        finance.updateCategory(food, "Еда", null)
        finance.emptyTrash()
        finance.restoreBackup(json)

        assertEquals(accountsBefore, finance.observeAccounts().first())
        assertEquals(listOf(1_000_000L - 85_000 - 500_000, 690_000L), finance.observeAccounts().first().map { it.balance })
        assertEquals(operationsBefore, finance.observeOperations().first())
        assertEquals(categoriesBefore, finance.observeCategories().first())
        assertEquals(trashBefore, finance.observeTrash().first())
        assertEquals(1, RoomBackupRepository(db, time, TickTickImporter(db, time, UuidIdGenerator()), TickTickExporter(db, time)).observeStats().first().totalTasks)
    }

    /** Негодный файл — ошибка, данные остаются как были. */
    @Test
    fun invalidBackupChangesNothing() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 85_000))
        val good = finance.exportBackup()
        val bad = listOf(
            "не json",
            "{}",
            good.replace("simple-tasks-finance", "other-app"),
            good.replace("\"version\": 1", "\"version\": 99"),
            good.replace("\"account_id\": \"$card\"", "\"account_id\": \"missing\""),
            good.replace("\"initial_balance\": 1000000", "\"initial_balance\": \"много\""),
        )
        bad.forEach { json ->
            assertTrue(json.take(40), runCatching { finance.inspectBackup(json) }.exceptionOrNull() is InvalidFinanceBackupException)
            assertTrue(json.take(40), runCatching { finance.restoreBackup(json) }.exceptionOrNull() is InvalidFinanceBackupException)
        }
        assertEquals(2, finance.observeAccounts().first().size)
        assertEquals(1, finance.observeOperations().first().size)
    }

    /** Справочник мест (#239): место из операции попадает в справочник само, повтор — не дубль; создание вручную. */
    @Test
    fun placesDirectoryFillsFromOperations() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 100, place = "Пятёрочка"))
        finance.createOperation(expense(card, 200, place = "Пятёрочка"))
        val ozon = finance.createPlace(" Озон ")
        assertEquals(ozon, finance.createPlace("Озон"))
        val list = finance.observePlaces().first()
        assertEquals(setOf("Пятёрочка", "Озон"), list.map { it.name }.toSet())
        assertEquals(2, list.single { it.name == "Пятёрочка" }.uses)
        assertEquals(0, list.single { it.name == "Озон" }.uses)
    }

    /** Переименование места меняет его во всех операциях (в том числе в корзине); совпадение с другим местом — объединение. */
    @Test
    fun renamingPlaceUpdatesOperationsAndMerges() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 100, place = "Пятерочка"))
        val trashed = finance.createOperation(expense(card, 50, place = "Пятерочка"))
        finance.trashOperation(trashed)
        finance.createOperation(expense(card, 300, place = "Пятёрочка"))
        val typo = finance.observePlaces().first().single { it.name == "Пятерочка" }.id

        finance.renamePlace(typo, "Пятёрочка")

        assertEquals(listOf("Пятёрочка"), finance.observePlaces().first().map { it.name })
        assertEquals(3, finance.observePlaces().first().single().uses)
        assertEquals(listOf("Пятёрочка", "Пятёрочка"), finance.observeOperations().first().map { it.place })
        assertEquals("Пятёрочка", finance.observeTrash().first().single().place)
    }

    /** Удаление навсегда (#239): у операций места или категории становятся пустыми, сами операции и остатки не меняются. */
    @Test
    fun deletingForeverClearsOperations() = runTest {
        val (card, _) = twoAccounts()
        val food = finance.createCategory("Продукты", OperationKind.EXPENSE)
        finance.createOperation(expense(card, 100, category = food, place = "Кофейня"))
        val balance = finance.observeAccounts().first().first().balance
        val cafe = finance.observePlaces().first().single().id
        finance.setPlaceArchived(cafe, true)
        assertTrue(finance.observePlaces().first().single().archived)

        finance.deletePlaceForever(cafe)
        finance.setCategoryArchived(food, true)
        finance.deleteCategoryForever(food)

        assertTrue(finance.observePlaces().first().isEmpty())
        assertTrue(finance.observeCategories().first().isEmpty())
        val operation = finance.observeOperations().first().single()
        assertNull(operation.place)
        assertNull(operation.categoryId)
        assertEquals(balance, finance.observeAccounts().first().first().balance)
    }

    /** Резервная копия со справочником мест; старая копия без него — места берутся из операций. */
    @Test
    fun backupKeepsPlacesAndReadsOldCopies() = runTest {
        val (card, _) = twoAccounts()
        finance.createOperation(expense(card, 100, place = "Озон"))
        val archived = finance.createPlace("Кофейня")
        finance.setPlaceArchived(archived, true)
        val json = finance.exportBackup()
        val before = finance.observePlaces().first()
        finance.deletePlaceForever(archived)
        finance.restoreBackup(json)
        assertEquals(before.sortedBy { it.name }, finance.observePlaces().first().sortedBy { it.name })

        val old = org.json.JSONObject(json).apply { remove("places") }.toString()
        finance.restoreBackup(old)
        assertEquals(listOf("Озон" to false), finance.observePlaces().first().map { it.name to it.archived })
    }
}
