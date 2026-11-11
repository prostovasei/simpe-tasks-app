package com.prostovasei.simpletasks.ui.feature.finance

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Раздел «Финансы» (этап 1, #221). Живёт вместе с окном приложения, а не с экраном: нижняя панель финансов рисуется общим каркасом,
 * а выбранная вкладка сохраняется при переходе в «Задачи» и обратно (решение PM 10.11.2026).
 */
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val finance: FinanceRepository,
) : MviViewModel<FinanceState, FinanceIntent, FinanceEffect>(FinanceState()) {

    init {
        viewModelScope.launch { finance.observeAccounts().collect { accounts -> setState { copy(accounts = accounts) } } }
    }

    override fun onIntent(intent: FinanceIntent) {
        when (intent) {
            is FinanceIntent.SelectTab -> setState { copy(tab = intent.tab) }
            FinanceIntent.Add -> when (currentState.accounts?.isEmpty()) {
                null -> Unit
                true -> setState { copy(window = AccountWindow.Create) }
                false -> sendEffect(FinanceEffect.OperationsSoon)
            }
            FinanceIntent.NewAccount -> setState { copy(window = AccountWindow.Create) }
            is FinanceIntent.OpenAccount -> currentState.accounts?.firstOrNull { it.id == intent.id }?.let { setState { copy(window = AccountWindow.Edit(it)) } }
            FinanceIntent.CloseWindow -> setState { copy(window = AccountWindow.None) }
            is FinanceIntent.SaveAccount -> save(intent)
        }
    }

    private fun save(intent: FinanceIntent.SaveAccount) {
        val window = currentState.window
        setState { copy(window = AccountWindow.None) }
        viewModelScope.launch {
            when (window) {
                AccountWindow.Create -> finance.createAccount(intent.draft)
                is AccountWindow.Edit -> finance.updateAccount(window.account.id, intent.draft)
                AccountWindow.None -> Unit
            }
        }
    }
}
