package com.prostovasei.simpletasks.ui.feature.finance

import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft

/** Вкладки нижней панели финансов (#217): аналитика со счетами сверху и операции. */
enum class FinanceTab { ANALYTICS, OPERATIONS }

/** Открытое окно счёта: новое или правка существующего. */
sealed interface AccountWindow {
    data object None : AccountWindow
    data object Create : AccountWindow
    data class Edit(val account: Account) : AccountWindow
}

/** [accounts] = null — счета ещё читаются из базы (экран пустой, без мигания «Начнём со счёта»). */
data class FinanceState(
    val accounts: List<Account>? = null,
    val tab: FinanceTab = FinanceTab.ANALYTICS,
    val window: AccountWindow = AccountWindow.None,
) {
    val total: Long get() = accounts.orEmpty().sumOf { it.balance }
}

sealed interface FinanceIntent {
    data class SelectTab(val tab: FinanceTab) : FinanceIntent

    /** Чёрный «+» нижней панели: без счетов — создание счёта, со счетами — операция (этап 2). */
    data object Add : FinanceIntent
    data object NewAccount : FinanceIntent
    data class OpenAccount(val id: String) : FinanceIntent
    data object CloseWindow : FinanceIntent
    data class SaveAccount(val draft: AccountDraft) : FinanceIntent
}

sealed interface FinanceEffect {
    /** Этап 1: операций ещё нет — короткая подсказка. */
    data object OperationsSoon : FinanceEffect
}
