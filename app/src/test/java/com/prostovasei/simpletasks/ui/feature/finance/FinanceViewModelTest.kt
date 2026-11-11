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

    private fun twoAccounts() {
        finance.accounts.value = listOf(account("a1", "Карта", 4_230_000), account("a2", "Наличные", 615_000, AccountType.CASH))
    }

    private fun FinanceViewModel.edit(change: OperationForm.() -> OperationForm) = onIntent(FinanceIntent.EditForm(change))

    @Test
    fun plusWithoutAccountsOpensNewAccountAndSavesIt() = runTest(dispatcher) {
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
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
        val vm = FinanceViewModel(finance, time)
        runCurrent()
        vm.onIntent(FinanceIntent.Add)
        vm.onIntent(FinanceIntent.CreateCategory("Продукты", OperationKind.EXPENSE, select = true))
        runCurrent()
        assertEquals("c1", vm.state.value.form!!.categoryId)

        vm.onIntent(FinanceIntent.CreateCategory("  ", OperationKind.EXPENSE))
        vm.onIntent(FinanceIntent.CreateCategory("Перевод", OperationKind.TRANSFER))
        vm.onIntent(FinanceIntent.RenameCategory("c1", "Еда"))
        vm.onIntent(FinanceIntent.ArchiveCategory("c1", true))
        runCurrent()
        assertEquals(listOf("category:Продукты:EXPENSE", "rename:c1:Еда", "archive:c1:true"), finance.calls)
        assertTrue(vm.state.value.categories.single().archived)
    }

    /** Месяц аналитики: назад — сколько угодно, вперёд — не дальше текущего. */
    @Test
    fun analyticsMonthShiftsButNotIntoFuture() = runTest(dispatcher) {
        val vm = FinanceViewModel(finance, time)
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

    @Test
    fun tabIsRemembered() = runTest(dispatcher) {
        val vm = FinanceViewModel(finance, time)
        assertEquals(FinanceTab.ANALYTICS, vm.state.value.tab)
        vm.onIntent(FinanceIntent.SelectTab(FinanceTab.OPERATIONS))
        assertEquals(FinanceTab.OPERATIONS, vm.state.value.tab)
    }
}
