package com.prostovasei.simpletasks.ui.feature.finance

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.IoDispatcher
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.core.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationDraft
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.repository.BankPurchases
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
    private val documents: DocumentOpener,
    private val backupStore: FinanceBackupStore,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val purchases: BankPurchases,
) : MviViewModel<FinanceState, FinanceIntent, FinanceEffect>(
    FinanceState(today = time.today(), lastBackupAt = backupStore.lastBackupAt, bankAccess = purchases.hasAccess()),
) {
    /** «Изменить» из уведомления при холодном запуске: покупка откроется, когда список прочитается из базы. */
    private var waitingPurchase: String? = null
    private val pendingReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    init {
        viewModelScope.launch { finance.observeAccounts().collect { accounts -> setState { copy(accounts = accounts) } } }
        viewModelScope.launch { finance.observeOperations().collect { operations -> setState { copy(operations = operations) } } }
        viewModelScope.launch { finance.observeCategories().collect { categories -> setState { copy(categories = categories) } } }
        viewModelScope.launch { finance.observePlaces().collect { places -> setState { copy(places = places) } } }
        viewModelScope.launch { finance.observeFormHints().collect { hints -> setState { copy(hints = hints) } } }
        viewModelScope.launch { finance.observeTrash().collect { trash -> setState { copy(trash = trash) } } }
        viewModelScope.launch { finance.observeCounts().collect { counts -> setState { copy(counts = counts) } } }
        viewModelScope.launch { purchases.observeSettings().collect { settings -> setState { copy(bank = settings) } } }
        viewModelScope.launch {
            purchases.observePending().collect { pending ->
                setState { copy(pending = pending) }
                pendingReady.complete(Unit)
                waitingPurchase?.let { id ->
                    waitingPurchase = null
                    editPurchase(id)
                }
            }
        }
    }

    override fun onIntent(intent: FinanceIntent) {
        when (intent) {
            is FinanceIntent.SelectTab -> setState { copy(tab = intent.tab) }
            is FinanceIntent.ShiftMonth -> shiftMonth(intent.by)
            FinanceIntent.Add -> add()
            FinanceIntent.NewAccount -> setState { copy(window = AccountWindow.Create) }
            is FinanceIntent.OpenAccount -> currentState.accounts?.firstOrNull { it.id == intent.id }?.let { setState { copy(window = AccountWindow.Edit(it)) } }
            FinanceIntent.CloseWindow -> setState { copy(window = AccountWindow.None) }
            is FinanceIntent.SaveAccount -> saveAccount(intent)
            is FinanceIntent.OpenOperation -> currentState.operations?.firstOrNull { it.id == intent.id }?.let { operation ->
                // Корректировку сверки не правят: её можно только посмотреть и отправить в корзину.
                if (operation.kind == OperationKind.CORRECTION) setState { copy(correction = operation) } else setState { copy(form = operation.toForm(), today = time.today()) }
            }
            is FinanceIntent.DeleteCategory -> viewModelScope.launch { finance.deleteCategoryForever(intent.id) }
            is FinanceIntent.CreatePlace -> if (intent.name.isNotBlank()) viewModelScope.launch { finance.createPlace(intent.name) }
            is FinanceIntent.RenamePlace -> if (intent.name.isNotBlank()) viewModelScope.launch { finance.renamePlace(intent.id, intent.name) }
            is FinanceIntent.ArchivePlace -> viewModelScope.launch { finance.setPlaceArchived(intent.id, intent.archived) }
            is FinanceIntent.DeletePlace -> viewModelScope.launch { finance.deletePlaceForever(intent.id) }
            is FinanceIntent.RestoreOperation -> viewModelScope.launch {
                finance.restoreOperation(intent.id)
                sendEffect(FinanceEffect.OperationRestored)
            }
            is FinanceIntent.DeleteForever -> viewModelScope.launch { finance.deleteOperationForever(intent.id) }
            FinanceIntent.EmptyTrash -> viewModelScope.launch { finance.emptyTrash() }
            FinanceIntent.OpenReconcile -> (currentState.window as? AccountWindow.Edit)?.let { setState { copy(window = AccountWindow.None, reconcile = Reconcile.Choose(it.account)) } }
            is FinanceIntent.PickReconcile -> setState { copy(reconcile = intent.step) }
            FinanceIntent.FindOperation -> setState { copy(reconcile = null, tab = FinanceTab.OPERATIONS) }
            is FinanceIntent.SaveInitialBalance -> reconcile { finance.setInitialBalance(it, intent.initialBalance) }
            is FinanceIntent.SaveRealBalance -> reconcile { finance.correctBalance(it, intent.realBalance, time.today()) }
            FinanceIntent.CloseReconcile -> setState { copy(reconcile = null) }
            FinanceIntent.CloseCorrection -> setState { copy(correction = null) }
            FinanceIntent.OpenBackup -> setState { copy(backup = BackupWindow.Actions) }
            FinanceIntent.CloseBackup -> setState { copy(backup = null) }
            is FinanceIntent.ExportBackup -> exportBackup(intent.uri)
            is FinanceIntent.PickBackup -> pickBackup(intent.uri)
            FinanceIntent.ConfirmRestore -> restoreBackup()
            FinanceIntent.RefreshBankAccess -> setState { copy(bankAccess = purchases.hasAccess()) }
            is FinanceIntent.SetBankEnabled -> setBankEnabled(intent.enabled)
            is FinanceIntent.SetBankAccount -> viewModelScope.launch { purchases.setAccount(intent.accountId) }
            is FinanceIntent.RecordPurchase -> viewModelScope.launch {
                purchases.record(intent.id)
                sendEffect(FinanceEffect.PurchaseRecorded)
            }
            is FinanceIntent.EditPurchase -> editPurchase(intent.id)
            is FinanceIntent.SkipPurchase -> viewModelScope.launch { purchases.skip(intent.id) }
            is FinanceIntent.SetKind -> setKind(intent.kind)
            is FinanceIntent.EditForm -> setState { copy(form = form?.let(intent.change)) }
            FinanceIntent.CloseForm -> closeForm()
            FinanceIntent.SaveForm -> saveForm()
            FinanceIntent.TrashOperation -> trash()
            is FinanceIntent.CreateCategory -> createCategory(intent)
            is FinanceIntent.UpdateCategory -> viewModelScope.launch { finance.updateCategory(intent.id, intent.name, intent.emoji) }
            is FinanceIntent.ArchiveCategory -> viewModelScope.launch { finance.setCategoryArchived(intent.id, intent.archived) }
        }
    }

    private fun shiftMonth(by: Int) {
        val today = time.today()
        val target = currentState.month.plusMonths(by.toLong())
        if (target <= java.time.YearMonth.from(today)) setState { copy(month = target, today = today) }
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

    /** Включить можно, только когда доступ выдан и счёт выбран (макет 1.2); выключить — всегда. */
    private fun setBankEnabled(enabled: Boolean) {
        val access = purchases.hasAccess()
        setState { copy(bankAccess = access) }
        if (enabled && (!access || currentState.bankAccount == null)) return
        viewModelScope.launch { purchases.setEnabled(enabled) }
    }

    /** «Изменить»: окно нового расхода — сумма, счёт и дата покупки подставлены, категории нет (§4.3, макет 2.4). */
    suspend fun openPendingPurchase(id: String): Boolean {
        pendingReady.await()
        if (currentState.pending.none { it.id == id }) return false
        editPurchase(id)
        return true
    }

    private fun editPurchase(id: String) {
        val purchase = currentState.pending.firstOrNull { it.id == id }
        if (purchase == null) {
            if (!pendingReady.isCompleted) waitingPurchase = id
            return
        }
        val accounts = currentState.accounts.orEmpty()
        val accountId = purchase.accountId.takeIf { id -> accounts.isEmpty() || accounts.any { it.id == id } } ?: accounts.first().id
        val form = OperationForm(OperationKind.EXPENSE, accountId, purchase.date, amount = Money.input(purchase.amount), purchaseId = id)
        setState { copy(form = form, tab = FinanceTab.OPERATIONS, today = time.today()) }
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
        // Окно покупки из банка черновиком не становится: покупка остаётся в «Не разобрано».
        setState { copy(form = null, draft = if (form.editingId == null && form.purchaseId == null) form.takeIf { it.hasInput } else draft) }
    }

    private fun saveForm() {
        val form = currentState.form ?: return
        val amount = form.kopecks ?: return
        if (!form.canSave) return
        val draft = OperationDraft(form.kind, amount, form.accountId, form.toAccountId, form.categoryId, form.place, form.comment, form.date)
        // Окно остаётся для следующей новой операции: сумма, место и комментарий очищаются; тип, счета, категория и дата — прежние.
        setState { copy(form = form.copy(editingId = null, purchaseId = null, amount = "", place = "", comment = ""), draft = if (form.purchaseId == null) null else this.draft) }
        viewModelScope.launch {
            when {
                form.purchaseId != null -> purchases.recordAs(form.purchaseId, draft)
                form.editingId == null -> finance.createOperation(draft)
                else -> finance.updateOperation(form.editingId, draft)
            }
            sendEffect(FinanceEffect.OperationSaved)
        }
    }

    private fun trash() {
        val id = currentState.form?.editingId ?: currentState.correction?.id ?: return
        setState { copy(form = null, correction = null) }
        viewModelScope.launch {
            finance.trashOperation(id)
            sendEffect(FinanceEffect.OperationTrashed)
        }
    }

    private fun reconcile(action: suspend (accountId: String) -> Unit) {
        val accountId = currentState.reconcile?.account?.id ?: return
        setState { copy(reconcile = null) }
        viewModelScope.launch { action(accountId) }
    }

    private fun exportBackup(uri: String) {
        viewModelScope.launch {
            val saved = runCatching {
                val json = finance.exportBackup()
                withContext(io) { documents.openOutput(uri).use { it.write(json.toByteArray()) } }
            }.isSuccess
            if (saved) {
                val now = time.now().toEpochMilli()
                backupStore.lastBackupAt = now
                setState { copy(lastBackupAt = now) }
            }
            sendEffect(if (saved) FinanceEffect.BackupSaved else FinanceEffect.BackupFailed)
        }
    }

    /** Файл выбран: читаем и проверяем; годный — окно подтверждения с содержимым копии, негодный — подсказка, данные не трогаем. */
    private fun pickBackup(uri: String) {
        viewModelScope.launch {
            val preview = runCatching {
                val json = withContext(io) { documents.openInput(uri).use { it.readBytes().decodeToString() } }
                BackupWindow.Confirm(json, finance.inspectBackup(json))
            }.getOrNull()
            if (preview == null) sendEffect(FinanceEffect.BackupInvalid) else setState { copy(backup = preview) }
        }
    }

    private fun restoreBackup() {
        val confirm = currentState.backup as? BackupWindow.Confirm ?: return
        // Черновик и открытые окна ссылаются на прежние счета и категории — после замены данных они недействительны.
        setState { copy(backup = null, form = null, draft = null, window = AccountWindow.None, reconcile = null, correction = null) }
        viewModelScope.launch {
            val restored = runCatching { finance.restoreBackup(confirm.json) }.isSuccess
            sendEffect(if (restored) FinanceEffect.BackupRestored else FinanceEffect.BackupInvalid)
        }
    }

    private fun createCategory(intent: FinanceIntent.CreateCategory) {
        if (intent.name.isBlank() || intent.kind == OperationKind.TRANSFER) return
        viewModelScope.launch {
            val id = finance.createCategory(intent.name, intent.kind, intent.emoji)
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
                AccountWindow.None -> return@launch
            }
            sendEffect(FinanceEffect.AccountSaved)
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
