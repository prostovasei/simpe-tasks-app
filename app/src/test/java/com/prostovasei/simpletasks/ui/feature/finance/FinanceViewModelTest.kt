package com.prostovasei.simpletasks.ui.feature.finance

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.testing.FakeFinanceRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.account
import com.prostovasei.simpletasks.testing.category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** Раздел «Финансы»: счета (этап 1, #221), окно операции, черновик и категории (этап 2, #225). */
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val finance = FakeFinanceRepository()

    /** 13.10.2026, Москва. */
    private val time = FakeTimeProvider()
    private val today = LocalDate.parse("2026-10-13")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val files = mutableMapOf<String, String>()
    private val documents = object : com.prostovasei.simpletasks.core.files.DocumentOpener {
        override fun openInput(uri: String) = (files[uri] ?: throw java.io.FileNotFoundException(uri)).byteInputStream()
        override fun openOutput(uri: String) = object : java.io.ByteArrayOutputStream() {
            override fun close() {
                if (uri == "readonly") throw java.io.IOException("нельзя записать")
                files[uri] = toString(Charsets.UTF_8)
            }
        }
    }
    private val store = object : FinanceBackupStore {
        override var lastBackupAt: Long? = null
    }

    private val bank = com.prostovasei.simpletasks.testing.FakeBankPurchases()

    private fun vm() = FinanceViewModel(finance, time, documents, store, dispatcher, bank)

    private fun twoAccounts() {
        finance.accounts.value = listOf(account("a1", "Карта", 4_230_000), account("a2", "Наличные", 615_000, AccountType.CASH))
    }

    private fun FinanceViewModel.edit(change: OperationForm.() -> OperationForm) = onIntent(FinanceIntent.EditForm(change))

    @Test
    fun plusWithoutAccountsOpensNewAccountAndSavesIt() = runTest(dispatcher) {
        val vm = vm()
        assertNull(vm.state.value.accounts)
        // Пока счета читаются, «+» ничего не делает.
        vm.onIntent(FinanceIntent.Add)
        assertEquals(AccountWindow.None, vm.state.value.window)
        runCurrent()
        assertEquals(emptyList<Any>(), vm.state.value.accounts)

        vm.onIntent(FinanceIntent.Add)
        assertEquals(AccountWindow.Create, vm.state.value.window)
        assertNull(vm.state.value.form)
        vm.onIntent(FinanceIntent.SaveAccount(AccountDraft("Карта", AccountType.CARD, null, 4_230_000)))
        runCurrent()

        assertEquals(AccountWindow.None, vm.state.value.window)
        assertEquals("create:Карта:CARD:null:4230000", finance.calls.single())
        assertEquals(4_230_000L, vm.state.value.total)
    }

    @Test
    fun accountOpensForEditAndTotalSumsBalances() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        assertEquals(4_845_000L, vm.state.value.total)

        vm.onIntent(FinanceIntent.OpenAccount("a2"))
        assertEquals("Наличные", (vm.state.value.window as AccountWindow.Edit).account.name)
        vm.onIntent(FinanceIntent.SaveAccount(AccountDraft("Кошелёк", AccountType.CASH, "👜", 700_000)))
        runCurrent()
        assertEquals("update:a2:Кошелёк:CASH:👜:700000", finance.calls.single())

        vm.onIntent(FinanceIntent.OpenAccount("missing"))
        assertEquals(AccountWindow.None, vm.state.value.window)
    }

    /** «+» — сразу расход с последнего использованного счёта на сегодня; сохраняется только с суммой. */
    @Test
    fun plusOpensExpenseOnLastAccountAndSaves() = runTest(dispatcher) {
        twoAccounts()
        finance.hints.value = FormHints(lastAccountId = "a2")
        val vm = vm()
        runCurrent()

        vm.onIntent(FinanceIntent.Add)
        val form = vm.state.value.form!!
        assertEquals(OperationForm(OperationKind.EXPENSE, "a2", today), form)
        assertFalse(form.canSave)
        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()
        assertTrue(finance.calls.isEmpty())

        vm.edit { copy(amount = "850", categoryId = "c1", place = " Пятёрочка ", comment = "хлеб") }
        vm.effects.test {
            vm.onIntent(FinanceIntent.SaveForm)
            runCurrent()
            assertEquals(FinanceEffect.OperationSaved, awaitItem())
        }
        assertEquals("operation:EXPENSE:85000:a2:null:c1: Пятёрочка :хлеб:2026-10-13", finance.calls.single())
    }

    /** После сохранения окно остаётся: сумма, место и комментарий очищены; счёт, категория и выбранная дата — прежние. */
    @Test
    fun windowStaysAfterSaveKeepingAccountCategoryAndDate() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        val yesterday = today.minusDays(1)
        vm.edit { copy(amount = "620", accountId = "a2", categoryId = "c2", place = "Кофейня", comment = "латте", date = yesterday) }
        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()

        assertEquals(OperationForm(OperationKind.EXPENSE, "a2", yesterday, categoryId = "c2"), vm.state.value.form)

        // Закрыли без ввода — черновика нет; новое открытие снова на сегодня.
        vm.onIntent(FinanceIntent.CloseForm)
        assertNull(vm.state.value.draft)
        time.advance(Duration.ofDays(1))
        vm.onIntent(FinanceIntent.Add)
        assertEquals(today.plusDays(1), vm.state.value.form!!.date)
        assertEquals(today.plusDays(1), vm.state.value.today)
    }

    /** Незавершённый ввод при закрытии — черновик: возвращается при следующем открытии, после сохранения исчезает. */
    @Test
    fun unfinishedInputIsKeptAsDraft() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        vm.onIntent(FinanceIntent.SetKind(OperationKind.INCOME))
        vm.edit { copy(amount = "65000", comment = "аванс") }
        vm.onIntent(FinanceIntent.CloseForm)
        assertNull(vm.state.value.form)

        vm.onIntent(FinanceIntent.Add)
        val restored = vm.state.value.form!!
        assertEquals(OperationKind.INCOME, restored.kind)
        assertEquals("65000", restored.amount)
        assertEquals("аванс", restored.comment)

        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()
        vm.onIntent(FinanceIntent.CloseForm)
        assertNull(vm.state.value.draft)
    }

    /** Смена типа: сумма, дата и комментарий остаются; категория и место сбрасываются; у перевода — второй счёт. */
    @Test
    fun kindSwitchKeepsAmountDateCommentAndResetsCategoryAndPlace() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        val date = today.minusDays(3)
        vm.edit { copy(amount = "5000", categoryId = "c1", place = "Озон", comment = "на отпуск", date = date) }

        vm.onIntent(FinanceIntent.SetKind(OperationKind.TRANSFER))
        val transfer = vm.state.value.form!!
        assertEquals(OperationForm(OperationKind.TRANSFER, "a1", date, amount = "5000", toAccountId = "a2", comment = "на отпуск"), transfer)
        assertTrue(transfer.canSave)
        vm.edit { copy(toAccountId = "a1") }
        assertFalse(vm.state.value.form!!.canSave)
        vm.edit { copy(toAccountId = "a2") }
        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()
        assertEquals("operation:TRANSFER:500000:a1:a2:null::на отпуск:$date", finance.calls.single())

        vm.onIntent(FinanceIntent.SetKind(OperationKind.INCOME))
        assertNull(vm.state.value.form!!.toAccountId)
    }

    /** Правка: то же окно с полями операции, тип не меняется; после сохранения окно очищается для новой; корзина закрывает окно. */
    @Test
    fun operationIsEditedAndTrashed() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        vm.edit { copy(amount = "850,5", place = "Пятёрочка") }
        vm.onIntent(FinanceIntent.SaveForm)
        vm.onIntent(FinanceIntent.CloseForm)
        runCurrent()
        finance.calls.clear()

        vm.onIntent(FinanceIntent.OpenOperation("o1"))
        val form = vm.state.value.form!!
        assertEquals("o1", form.editingId)
        assertEquals("850,50", form.amount)
        assertEquals("Пятёрочка", form.place)
        vm.onIntent(FinanceIntent.SetKind(OperationKind.INCOME))
        assertEquals(OperationKind.EXPENSE, vm.state.value.form!!.kind)

        vm.edit { copy(amount = "900") }
        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()
        assertEquals("updateOperation:o1:EXPENSE:90000:a1:null:2026-10-13", finance.calls.single())
        assertNull(vm.state.value.form!!.editingId)
        assertEquals("", vm.state.value.form!!.amount)
        vm.onIntent(FinanceIntent.CloseForm)

        vm.onIntent(FinanceIntent.OpenOperation("o1"))
        vm.effects.test {
            vm.onIntent(FinanceIntent.TrashOperation)
            runCurrent()
            // Раньше в очереди — «Сохранено» от двух сохранений выше.
            assertEquals(listOf(FinanceEffect.OperationSaved, FinanceEffect.OperationSaved, FinanceEffect.OperationTrashed), listOf(awaitItem(), awaitItem(), awaitItem()))
        }
        assertNull(vm.state.value.form)
        assertEquals("trash:o1", finance.calls.last())
        assertTrue(vm.state.value.operations!!.isEmpty())
    }

    /** Кнопки категорий: до пяти действующих нужного типа, недавние первыми; выбранная архивная тоже видна. */
    @Test
    fun categoryChipsShowRecentFirstUpToFive() = runTest(dispatcher) {
        twoAccounts()
        finance.categories.value = (1..7).map { category("c$it", "Категория $it") } + category("i1", "Зарплата", OperationKind.INCOME) + category("old", "Такси", archived = true)
        finance.hints.value = FormHints(recentCategoryIds = listOf("c6", "i1", "old", "c3"))
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        val state = vm.state.value

        assertEquals(listOf("c6", "c3", "c1", "c2", "c4"), state.categoryChips(state.form!!).map { it.id })
        assertEquals(listOf("old", "c6", "c3", "c1", "c2"), state.categoryChips(state.form!!.copy(categoryId = "old")).map { it.id })
        assertEquals(listOf("i1"), state.categoryChips(state.form!!.copy(kind = OperationKind.INCOME)).map { it.id })
    }

    @Test
    fun categoryCreatedFromFormIsSelected() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        vm.onIntent(FinanceIntent.CreateCategory("Продукты", OperationKind.EXPENSE, select = true))
        runCurrent()
        assertEquals("c1", vm.state.value.form!!.categoryId)

        vm.onIntent(FinanceIntent.CreateCategory("  ", OperationKind.EXPENSE))
        vm.onIntent(FinanceIntent.CreateCategory("Перевод", OperationKind.TRANSFER))
        vm.onIntent(FinanceIntent.UpdateCategory("c1", "Еда", "🍔"))
        vm.onIntent(FinanceIntent.ArchiveCategory("c1", true))
        runCurrent()
        assertEquals(listOf("category:Продукты:EXPENSE", "update:c1:Еда:🍔", "archive:c1:true"), finance.calls)
        assertTrue(vm.state.value.categories.single().archived)
        assertEquals("🍔", vm.state.value.categories.single().emoji)
    }

    /** Месяц аналитики: назад — сколько угодно, вперёд — не дальше текущего. */
    @Test
    fun analyticsMonthShiftsButNotIntoFuture() = runTest(dispatcher) {
        val vm = vm()
        val current = java.time.YearMonth.of(2026, 10)
        assertEquals(current, vm.state.value.month)
        assertFalse(vm.state.value.canGoNextMonth)

        vm.onIntent(FinanceIntent.ShiftMonth(1))
        assertEquals(current, vm.state.value.month)
        vm.onIntent(FinanceIntent.ShiftMonth(-1))
        vm.onIntent(FinanceIntent.ShiftMonth(-1))
        assertEquals(java.time.YearMonth.of(2026, 8), vm.state.value.month)
        assertTrue(vm.state.value.canGoNextMonth)
        vm.onIntent(FinanceIntent.ShiftMonth(1))
        assertEquals(java.time.YearMonth.of(2026, 9), vm.state.value.month)
    }

    /** Корзина: операция уходит из списка, возвращается, удаляется навсегда; очистка — вся корзина. */
    @Test
    fun trashRestoresAndDeletesForever() = runTest(dispatcher) {
        twoAccounts()
        finance.operations.value = listOf(com.prostovasei.simpletasks.testing.operation("o1", 85_000, "2026-10-13"), com.prostovasei.simpletasks.testing.operation("o2", 62_000, "2026-10-12"))
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.OpenOperation("o1"))
        vm.onIntent(FinanceIntent.TrashOperation)
        vm.onIntent(FinanceIntent.OpenOperation("o2"))
        vm.onIntent(FinanceIntent.TrashOperation)
        runCurrent()
        assertEquals(listOf("o2", "o1"), vm.state.value.trash.map { it.id })

        vm.onIntent(FinanceIntent.RestoreOperation("o1"))
        runCurrent()
        assertEquals(listOf("o1"), vm.state.value.operations!!.map { it.id })
        vm.onIntent(FinanceIntent.DeleteForever("o2"))
        runCurrent()
        assertTrue(vm.state.value.trash.isEmpty())
        vm.onIntent(FinanceIntent.EmptyTrash)
        runCurrent()
        assertEquals(listOf("trash:o1", "trash:o2", "restore:o1", "deleteForever:o2", "emptyTrash"), finance.calls)
    }

    /** Сверка из окна счёта: три способа — список операций, начальный остаток, реальный остаток (корректировка сегодняшним днём). */
    @Test
    fun reconcileOffersThreeWays() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.OpenReconcile)
        assertNull(vm.state.value.reconcile)

        vm.onIntent(FinanceIntent.OpenAccount("a1"))
        vm.onIntent(FinanceIntent.OpenReconcile)
        val card = vm.state.value.accounts!!.first()
        assertEquals(AccountWindow.None, vm.state.value.window)
        assertEquals(Reconcile.Choose(card), vm.state.value.reconcile)

        vm.onIntent(FinanceIntent.FindOperation)
        assertNull(vm.state.value.reconcile)
        assertEquals(FinanceTab.OPERATIONS, vm.state.value.tab)

        vm.onIntent(FinanceIntent.PickReconcile(Reconcile.Initial(card)))
        vm.onIntent(FinanceIntent.SaveInitialBalance(3_015_000))
        vm.onIntent(FinanceIntent.PickReconcile(Reconcile.Real(card)))
        vm.onIntent(FinanceIntent.SaveRealBalance(4_245_000))
        runCurrent()
        assertNull(vm.state.value.reconcile)
        assertEquals(listOf("initial:a1:3015000", "correct:a1:4245000:2026-10-13"), finance.calls)
    }

    /** Корректировку из списка не правят: окно просмотра, из него — только корзина. */
    @Test
    fun correctionOpensReadOnlyAndCanBeTrashed() = runTest(dispatcher) {
        twoAccounts()
        finance.operations.value = listOf(com.prostovasei.simpletasks.testing.operation("k1", 15_000, "2026-10-13", OperationKind.CORRECTION))
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.OpenOperation("k1"))
        assertNull(vm.state.value.form)
        assertEquals("k1", vm.state.value.correction!!.id)
        vm.onIntent(FinanceIntent.TrashOperation)
        runCurrent()
        assertNull(vm.state.value.correction)
        assertEquals("trash:k1", finance.calls.single())
    }

    /** Копия: сохранение в выбранный файл запоминает время; ошибка записи — подсказка, время не меняется. */
    @Test
    fun backupIsSavedToFile() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.OpenMenu)
        vm.onIntent(FinanceIntent.OpenBackup)
        assertFalse(vm.state.value.menu)
        assertEquals(BackupWindow.Actions, vm.state.value.backup)

        vm.effects.test {
            vm.onIntent(FinanceIntent.ExportBackup("readonly"))
            runCurrent()
            assertEquals(FinanceEffect.BackupFailed, awaitItem())
            assertNull(vm.state.value.lastBackupAt)

            vm.onIntent(FinanceIntent.ExportBackup("file"))
            runCurrent()
            assertEquals(FinanceEffect.BackupSaved, awaitItem())
        }
        assertEquals("{backup}", files["file"])
        assertEquals(time.now.toEpochMilli(), vm.state.value.lastBackupAt)
        assertEquals(time.now.toEpochMilli(), store.lastBackupAt)
    }

    /** Восстановление: годный файл — подтверждение с содержимым копии, затем замена; негодный — подсказка, данные не трогаются. */
    @Test
    fun backupIsRestoredOnlyAfterConfirmation() = runTest(dispatcher) {
        twoAccounts()
        files["good"] = "{copy}"
        files["bad"] = "мусор"
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        vm.edit { copy(amount = "850") }
        vm.onIntent(FinanceIntent.CloseForm)

        vm.effects.test {
            vm.onIntent(FinanceIntent.PickBackup("missing"))
            runCurrent()
            assertEquals(FinanceEffect.BackupInvalid, awaitItem())
            finance.backupCounts = null
            vm.onIntent(FinanceIntent.PickBackup("bad"))
            runCurrent()
            assertEquals(FinanceEffect.BackupInvalid, awaitItem())
            assertNull(vm.state.value.backup)

            finance.backupCounts = com.prostovasei.simpletasks.domain.model.finance.FinanceCounts(3, 13, 214)
            vm.onIntent(FinanceIntent.PickBackup("good"))
            runCurrent()
            assertEquals(BackupWindow.Confirm("{copy}", com.prostovasei.simpletasks.domain.model.finance.FinanceCounts(3, 13, 214)), vm.state.value.backup)
            assertTrue(finance.calls.none { it.startsWith("restoreBackup") })

            vm.onIntent(FinanceIntent.ConfirmRestore)
            runCurrent()
            assertEquals(FinanceEffect.BackupRestored, awaitItem())
        }
        assertEquals("restoreBackup:{copy}", finance.calls.last())
        assertNull(vm.state.value.backup)
        // Черновик ссылался на прежние счета.
        assertNull(vm.state.value.draft)
    }

    @Test
    fun tabIsRemembered() = runTest(dispatcher) {
        val vm = vm()
        assertEquals(FinanceTab.ANALYTICS, vm.state.value.tab)
        vm.onIntent(FinanceIntent.SelectTab(FinanceTab.OPERATIONS))
        assertEquals(FinanceTab.OPERATIONS, vm.state.value.tab)
    }

    private fun purchase(id: String = "p1", account: String = "a1") = com.prostovasei.simpletasks.domain.model.finance.PendingPurchase(
        id, 68_000, account, "Карта", java.time.Instant.parse("2026-10-12T09:30:00Z"), today.minusDays(1),
    )

    /** #233: «Изменить» — окно нового расхода с суммой, счётом и датой покупки; «Сохранить» разбирает покупку. */
    @Test
    fun editPurchaseOpensPrefilledExpenseAndRecordsIt() = runTest(dispatcher) {
        twoAccounts()
        bank.pending.value = listOf(purchase(account = "a2"))
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.EditPurchase("p1"))
        val form = vm.state.value.form!!
        assertEquals(OperationKind.EXPENSE, form.kind)
        assertEquals("680", form.amount)
        assertEquals("a2", form.accountId)
        assertEquals(today.minusDays(1), form.date)
        assertNull(form.categoryId)
        assertEquals(FinanceTab.OPERATIONS, vm.state.value.tab)

        vm.onIntent(FinanceIntent.SaveForm)
        runCurrent()
        assertEquals(listOf("recordAs:p1"), bank.calls)
        assertEquals(68_000L, bank.recorded.single().amount)
        assertNull(vm.state.value.form!!.purchaseId)
        assertTrue(finance.calls.none { it.startsWith("operation:") })
    }

    /** Закрыл окно покупки без сохранения — покупка остаётся в «Не разобрано», черновиком не становится. */
    @Test
    fun closingPurchaseWindowKeepsItPending() = runTest(dispatcher) {
        twoAccounts()
        bank.pending.value = listOf(purchase())
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.EditPurchase("p1"))
        vm.onIntent(FinanceIntent.CloseForm)
        assertNull(vm.state.value.form)
        assertNull(vm.state.value.draft)
        assertTrue(bank.calls.isEmpty())
        assertEquals(1, vm.state.value.pending.size)
    }

    /** Из уведомления при холодном запуске покупка открывается, когда список прочитан. */
    @Test
    fun editPurchaseWaitsForPendingList() = runTest(dispatcher) {
        twoAccounts()
        bank.pending.value = listOf(purchase())
        val vm = vm()
        vm.onIntent(FinanceIntent.EditPurchase("p1"))
        runCurrent()
        assertEquals("p1", vm.state.value.form?.purchaseId)
    }

    @Test
    fun recordAndSkipAnswerPurchase() = runTest(dispatcher) {
        bank.pending.value = listOf(purchase("p1"), purchase("p2"))
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.RecordPurchase("p1"))
        vm.onIntent(FinanceIntent.SkipPurchase("p2"))
        runCurrent()
        assertEquals(listOf("record:p1", "skip:p2"), bank.calls)
        assertTrue(vm.state.value.pending.isEmpty())
    }

    /** Выключатель включается, только когда есть доступ и счёт (макет 1.2); выключается всегда. */
    @Test
    fun bankSwitchNeedsAccessAndAccount() = runTest(dispatcher) {
        twoAccounts()
        val vm = vm()
        runCurrent()
        vm.onIntent(FinanceIntent.SetBankEnabled(true))
        runCurrent()
        assertFalse(bank.settings.value.enabled)

        vm.onIntent(FinanceIntent.SetBankAccount("a1"))
        runCurrent()
        vm.onIntent(FinanceIntent.SetBankEnabled(true))
        runCurrent()
        assertFalse(bank.settings.value.enabled)

        bank.access = true
        vm.onIntent(FinanceIntent.SetBankEnabled(true))
        runCurrent()
        assertTrue(vm.state.value.bankActive)

        bank.access = false
        vm.onIntent(FinanceIntent.RefreshBankAccess)
        assertFalse(vm.state.value.bankActive)
        assertTrue(vm.state.value.bank.enabled)
        vm.onIntent(FinanceIntent.SetBankEnabled(false))
        runCurrent()
        assertFalse(bank.settings.value.enabled)
    }
}
