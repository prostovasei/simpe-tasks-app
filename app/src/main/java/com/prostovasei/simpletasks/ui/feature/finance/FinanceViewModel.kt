package com.prostovasei.simpletasks.ui.feature.finance

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.repository.FinanceRepository
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Раздел «Финансы» (этап 1 — #221, этап 2 — #225). Живёт вместе с окном приложения, а не с экраном: нижняя панель финансов
 * рисуется общим каркасом, а выбранная вкладка и черновик операции сохраняются при переходе в «Задачи» и обратно.
 */
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val finance: FinanceRepository,
    private val time: TimeProvider,
) : MviViewModel<FinanceState, FinanceIntent, FinanceEffect>(FinanceState(today = time.today())) {

    init {
        viewModelScope.launch { finance.observeAccounts().collect { accounts -> setState { copy(accounts = accounts) } } }
        viewModelScope.launch { finance.observeOperations().collect { operations -> setState { copy(operations = operations) } } }
        viewModelScope.launch { finance.observeCategories().collect { categories -> setState { copy(categories = categories) } } }
        viewModelScope.launch { finance.observeFormHints().collect { hints -> setState { copy(hints = hints) } } }
    }

    override fun onIntent(intent: FinanceIntent) {
        when (intent) {
            is FinanceIntent.SelectTab -> setState { copy(tab = intent.tab) }
            FinanceIntent.Add -> add()
            FinanceIntent.NewAccount -> setState { copy(window = AccountWindow.Create) }
            is FinanceIntent.OpenAccount -> currentState.accounts?.firstOrNull { it.id == intent.id }?.let { setState { copy(window = AccountWindow.Edit(it)) } }
            FinanceIntent.CloseWindow -> setState { copy(window = AccountWindow.None) }
            is FinanceIntent.SaveAccount -> saveAccount(intent)
            is FinanceIntent.OpenOperation -> currentState.operations?.firstOrNull { it.id == intent.id }?.let { setState { copy(form = it.toForm(), today = time.today()) } }
            is FinanceIntent.SetKind -> setKind(intent.kind)
            is FinanceIntent.EditForm -> setState { copy(form = form?.let(intent.change)) }
            FinanceIntent.CloseForm -> closeForm()
            FinanceIntent.SaveForm -> saveForm()
            FinanceIntent.TrashOperation -> trash()
            is FinanceIntent.CreateCategory -> createCategory(intent)
            is FinanceIntent.RenameCategory -> viewModelScope.launch { finance.renameCategory(intent.id, intent.name) }
            is FinanceIntent.ArchiveCategory -> viewModelScope.launch { finance.setCategoryArchived(intent.id, intent.archived) }
        }
    }

    private fun add() {
        val accounts = currentState.accounts ?: return
        if (accounts.isEmpty()) {
            setState { copy(window = AccountWindow.Create) }
            return
        }
        val today = time.today()
        // Черновик возвращается как был; иначе новый расход с последнего использованного счёта на сегодня.
        val draft = currentState.draft?.takeIf { draft -> accounts.any { it.id == draft.accountId } }
        val accountId = currentState.hints.lastAccountId?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.first().id
        setState { copy(form = draft ?: OperationForm(OperationKind.EXPENSE, accountId, today), today = today) }
    }

    private fun setKind(kind: OperationKind) {
        val form = currentState.form ?: return
        if (form.editingId != null || form.kind == kind) return
        val other = currentState.accounts.orEmpty().firstOrNull { it.id != form.accountId }?.id
        setState { copy(form = form.copy(kind = kind, categoryId = null, place = "", toAccountId = if (kind == OperationKind.TRANSFER) other else null)) }
    }

    /** Закрытие окна: незавершённый ввод новой операции остаётся черновиком, правка просто закрывается. */
    private fun closeForm() {
        val form = currentState.form ?: return
        setState { copy(form = null, draft = if (form.editingId == null) form.takeIf { it.hasInput } else draft) }
    }

    private fun saveForm() {
        val form = currentState.form ?: return
        val amount = form.kopecks ?: return
        if (!form.canSave) return
        val draft = OperationDraft(form.kind, amount, form.accountId, form.toAccountId, form.categoryId, form.place, form.comment, form.date)
        // Окно остаётся для следующей новой операции: сумма, место и комментарий очищаются; тип, счета, категория и дата — прежние.
        setState { copy(form = form.copy(editingId = null, amount = "", place = "", comment = ""), draft = null) }
        viewModelScope.launch {
            if (form.editingId == null) finance.createOperation(draft) else finance.updateOperation(form.editingId, draft)
            sendEffect(FinanceEffect.OperationSaved)
        }
    }

    private fun trash() {
        val id = currentState.form?.editingId ?: return
        setState { copy(form = null) }
        viewModelScope.launch {
            finance.trashOperation(id)
            sendEffect(FinanceEffect.OperationTrashed)
        }
    }

    private fun createCategory(intent: FinanceIntent.CreateCategory) {
        if (intent.name.isBlank() || intent.kind == OperationKind.TRANSFER) return
        viewModelScope.launch {
            val id = finance.createCategory(intent.name, intent.kind)
            if (intent.select) setState { copy(form = form?.takeIf { it.kind == intent.kind }?.copy(categoryId = id) ?: form) }
        }
    }

    private fun saveAccount(intent: FinanceIntent.SaveAccount) {
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

    private fun Operation.toForm() = OperationForm(
        kind = kind,
        accountId = accountId,
        date = date,
        editingId = id,
        amount = com.prostovasei.simpletasks.domain.model.finance.Money.input(amount),
        toAccountId = toAccountId,
        categoryId = categoryId,
        place = place.orEmpty(),
        comment = comment.orEmpty(),
    )
}
