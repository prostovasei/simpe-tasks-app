package com.prostovasei.simpletasks.ui.feature.finance

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.testing.FakeFinanceRepository
import com.prostovasei.simpletasks.testing.account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Раздел «Финансы», этап 1 (#221): счета, окно счёта, «+» нижней панели и вкладки. */
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val finance = FakeFinanceRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun plusWithoutAccountsOpensNewAccountAndSavesIt() = runTest(dispatcher) {
        val vm = FinanceViewModel(finance)
        assertNull(vm.state.value.accounts)
        // Пока счета читаются, «+» ничего не делает.
        vm.onIntent(FinanceIntent.Add)
        assertEquals(AccountWindow.None, vm.state.value.window)
        runCurrent()
        assertEquals(emptyList<Any>(), vm.state.value.accounts)

        vm.onIntent(FinanceIntent.Add)
        assertEquals(AccountWindow.Create, vm.state.value.window)
        vm.onIntent(FinanceIntent.SaveAccount(AccountDraft("Карта", AccountType.CARD, null, 4_230_000)))
        runCurrent()

        assertEquals(AccountWindow.None, vm.state.value.window)
        assertEquals("create:Карта:CARD:null:4230000", finance.calls.single())
        assertEquals(listOf("Карта"), vm.state.value.accounts!!.map { it.name })
        assertEquals(4_230_000L, vm.state.value.total)
    }

    @Test
    fun plusWithAccountsSaysOperationsComeLater() = runTest(dispatcher) {
        finance.accounts.value = listOf(account("a1", "Карта", 100))
        val vm = FinanceViewModel(finance)
        runCurrent()

        vm.effects.test {
            vm.onIntent(FinanceIntent.Add)
            assertEquals(FinanceEffect.OperationsSoon, awaitItem())
        }
        assertEquals(AccountWindow.None, vm.state.value.window)
    }

    @Test
    fun accountOpensForEditAndTotalSumsBalances() = runTest(dispatcher) {
        finance.accounts.value = listOf(account("a1", "Карта", 4_230_000), account("a2", "Наличные", 615_000, AccountType.CASH))
        val vm = FinanceViewModel(finance)
        runCurrent()
        assertEquals(4_845_000L, vm.state.value.total)

        vm.onIntent(FinanceIntent.OpenAccount("a2"))
        assertEquals("Наличные", (vm.state.value.window as AccountWindow.Edit).account.name)
        vm.onIntent(FinanceIntent.SaveAccount(AccountDraft("Кошелёк", AccountType.CASH, "👜", 700_000)))
        runCurrent()

        assertEquals("update:a2:Кошелёк:CASH:👜:700000", finance.calls.single())
        assertEquals(4_930_000L, vm.state.value.total)

        vm.onIntent(FinanceIntent.OpenAccount("missing"))
        assertEquals(AccountWindow.None, vm.state.value.window)
        vm.onIntent(FinanceIntent.NewAccount)
        vm.onIntent(FinanceIntent.CloseWindow)
        assertEquals(AccountWindow.None, vm.state.value.window)
    }

    @Test
    fun tabIsRemembered() = runTest(dispatcher) {
        val vm = FinanceViewModel(finance)
        assertEquals(FinanceTab.ANALYTICS, vm.state.value.tab)
        vm.onIntent(FinanceIntent.SelectTab(FinanceTab.OPERATIONS))
        assertEquals(FinanceTab.OPERATIONS, vm.state.value.tab)
    }
}
