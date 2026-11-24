package com.prostovasei.simpletasks.ui.feature.finance

import com.prostovasei.simpletasks.domain.model.finance.Budget
import com.prostovasei.simpletasks.domain.model.finance.BudgetRules
import com.prostovasei.simpletasks.domain.model.finance.MonthBudget
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.BankSettings
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import com.prostovasei.simpletasks.domain.model.finance.PendingPurchase
import com.prostovasei.simpletasks.domain.model.finance.Place
import java.time.LocalDate
import java.time.YearMonth

/** Вкладки нижней панели финансов (#217): аналитика со счетами сверху и операции. */
enum class FinanceTab { ANALYTICS, OPERATIONS }

/** Открытое окно счёта: новое или правка существующего. */
sealed interface AccountWindow {
    data object None : AccountWindow
    data object Create : AccountWindow
    data class Edit(val account: Account) : AccountWindow
}

/** Сверка остатка счёта (этап 4, #231): выбор способа и два окна ввода. «Найти операцию» окна не имеет — открывает список. */
sealed interface Reconcile {
    val account: Account

    data class Choose(override val account: Account) : Reconcile
    data class Initial(override val account: Account) : Reconcile
    data class Real(override val account: Account) : Reconcile
}

/** Окно резервной копии: действия или подтверждение замены данных содержимым выбранного файла. */
sealed interface BackupWindow {
    data object Actions : BackupWindow
    data class Confirm(val json: String, val inFile: FinanceCounts) : BackupWindow
}

/**
 * Поля окна операции (этап 2, #225). [editingId] = null — новая операция. [amount] — текст поля суммы.
 * Обязательны сумма и счёт; у перевода — ещё второй счёт, отличный от первого.
 */
data class OperationForm(
    val kind: OperationKind,
    val accountId: String,
    val date: LocalDate,
    val editingId: String? = null,
    val amount: String = "",
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val place: String = "",
    val comment: String = "",
    /** «Изменить» у покупки из уведомления банка (#233): после «Сохранить» покупка считается разобранной. */
    val purchaseId: String? = null,
) {
    val kopecks: Long? get() = Money.parse(amount)

    val canSave: Boolean
        get() = (kopecks ?: 0) > 0 && (kind != OperationKind.TRANSFER || (toAccountId != null && toAccountId != accountId))

    /** Есть что сохранять черновиком: введена сумма, место или комментарий. */
    val hasInput: Boolean get() = amount.isNotEmpty() || place.isNotBlank() || comment.isNotBlank()
}

/**
 * Окно «Бюджет» (#327) месяца [month]. [editing] — открыта правка бюджета категории. [prepare] — подготовка следующего месяца
 * из уведомления: суммы и подсказки «в среднем», кнопки «Всё по средним» и «Подтвердить».
 */
data class BudgetWindow(val month: YearMonth, val editing: String? = null, val prepare: Boolean = false)

/** [accounts] и [operations] = null — ещё читаются из базы (экран пустой, без мигания пустых состояний). */
data class FinanceState(
    val accounts: List<Account>? = null,
    val tab: FinanceTab = FinanceTab.ANALYTICS,
    val window: AccountWindow = AccountWindow.None,
    val operations: List<Operation>? = null,
    val categories: List<Category> = emptyList(),
    /** Справочник мест (#239), включая архивные. */
    val places: List<Place> = emptyList(),
    val hints: FormHints = FormHints(),
    /** Открытое окно операции. */
    val form: OperationForm? = null,
    /** Незавершённый ввод новой операции: возвращается при следующем открытии окна (живёт, пока приложение не закрыто). */
    val draft: OperationForm? = null,
    // Не LocalDate.EPOCH (только с Android 14) и не LocalDate.MIN (от него нельзя отнять месяц для аналитики).
    val today: LocalDate = LocalDate.ofEpochDay(0),
    /** Этап 4: меню шестерёнки, корзина, сверка, просмотр корректировки, резервная копия. */
    val trash: List<Operation> = emptyList(),
    val counts: FinanceCounts = FinanceCounts(),
    val reconcile: Reconcile? = null,
    val correction: Operation? = null,
    val backup: BackupWindow? = null,
    val lastBackupAt: Long? = null,
    /** Месяц аналитики (этап 3); сохраняется при смене вкладки и модуля, после полного закрытия — снова текущий. */
    val month: YearMonth = YearMonth.from(today),
    /** Уведомления банка (#233): настройки, системный доступ и «Не разобрано». */
    val bank: BankSettings = BankSettings(),
    val bankAccess: Boolean = false,
    val pending: List<PendingPurchase> = emptyList(),
    /** Бюджеты категорий всех месяцев (#327) и открытое окно «Бюджет». */
    val budgets: List<Budget> = emptyList(),
    val budgetWindow: BudgetWindow? = null,
) {
    /** Бюджеты, действующие в месяце (#327): свои или перенесённые из последнего заведённого. */
    fun budgetsOf(month: YearMonth): Map<String, Long> = BudgetRules.effective(budgets, month, YearMonth.from(today))

    /** Бюджет месяца для окна, полосы лимита и подсказки в окне операции. */
    fun monthBudget(month: YearMonth): MonthBudget =
        MonthBudget.of(budgetsOf(month), categories, operations.orEmpty(), month, today, BudgetRules.suggestions(operations.orEmpty(), today))

    /** Счёт для Яндекс Банка; после восстановления копии выбранного счёта может уже не быть. */
    val bankAccount: Account? get() = accounts?.firstOrNull { it.id == bank.accountId }

    /** Распознавание работает: включено, доступ есть, счёт выбран. */
    val bankActive: Boolean get() = bank.enabled && bankAccess && bankAccount != null

    /** Вперёд дальше текущего месяца листать нельзя. */
    val canGoNextMonth: Boolean get() = month < YearMonth.from(today)

    val total: Long get() = accounts.orEmpty().sumOf { it.balance }

    /**
     * Кнопки категорий окна: до [RECENT_CATEGORIES] действующих категорий нужного типа — сначала недавние, потом остальные;
     * выбранная (даже архивная или не из первых) всегда есть в списке.
     */
    fun categoryChips(form: OperationForm): List<Category> {
        val active = categories.filter { it.kind == form.kind && !it.archived }
        val recent = hints.recentCategoryIds.mapNotNull { id -> active.firstOrNull { it.id == id } }
        val chips = (recent + active).distinct().take(RECENT_CATEGORIES)
        val selected = categories.firstOrNull { it.id == form.categoryId } ?: return chips
        return if (selected in chips) chips else (listOf(selected) + chips).take(RECENT_CATEGORIES)
    }

    companion object {
        const val RECENT_CATEGORIES = 5
    }
}

sealed interface FinanceIntent {
    /** Окно «Бюджет» (#327): нажатие на полосу лимита или доли категорий; [prepare] — следующий месяц из уведомления. */
    data class OpenBudget(val month: YearMonth, val prepare: Boolean = false) : FinanceIntent
    data object CloseBudget : FinanceIntent

    /** Правка бюджета категории: открыть, закрыть без изменений, сохранить ([amount] = 0 — снять бюджет). */
    data class EditBudget(val categoryId: String?) : FinanceIntent
    data class SaveBudget(val categoryId: String, val amount: Long) : FinanceIntent

    /** «Заполнить по средним» / «Всё по средним» / подставить одну подсказку: категория → сумма. */
    data class SetBudgets(val amounts: Map<String, Long>) : FinanceIntent

    /** «Подтвердить» следующий месяц: месяц заводится копией текущего с правками, окно закрывается. */
    data object ConfirmBudget : FinanceIntent

    data class SelectTab(val tab: FinanceTab) : FinanceIntent

    /** Стрелки месяца в аналитике: -1 — предыдущий, 1 — следующий (не дальше текущего). */
    data class ShiftMonth(val by: Int) : FinanceIntent

    /** Чёрный «+» нижней панели: без счетов — создание счёта, со счетами — окно операции (черновик, если был). */
    data object Add : FinanceIntent
    data object NewAccount : FinanceIntent
    data class OpenAccount(val id: String) : FinanceIntent
    data object CloseWindow : FinanceIntent
    data class SaveAccount(val draft: AccountDraft) : FinanceIntent

    /** Нажатие на операцию в списке — правка в том же окне. */
    data class OpenOperation(val id: String) : FinanceIntent

    /** Смена типа новой операции: сумма, дата и комментарий остаются, категория и место сбрасываются. */
    data class SetKind(val kind: OperationKind) : FinanceIntent

    /** Любое другое поле окна операции. */
    data class EditForm(val change: OperationForm.() -> OperationForm) : FinanceIntent
    data object CloseForm : FinanceIntent
    data object SaveForm : FinanceIntent

    /** Корзина в заголовке окна правки. */
    data object TrashOperation : FinanceIntent

    /** [select] — сразу выбрать новую категорию в открытом окне операции. */
    data class CreateCategory(val name: String, val kind: OperationKind, val select: Boolean = false, val emoji: String? = null) : FinanceIntent
    data class UpdateCategory(val id: String, val name: String, val emoji: String?) : FinanceIntent
    data class ArchiveCategory(val id: String, val archived: Boolean) : FinanceIntent

    /** Удаление категории навсегда (#239): операции остаются «Без категории». */
    data class DeleteCategory(val id: String) : FinanceIntent

    /** Справочник мест (#239). */
    data class CreatePlace(val name: String) : FinanceIntent
    data class RenamePlace(val id: String, val name: String) : FinanceIntent
    data class ArchivePlace(val id: String, val archived: Boolean) : FinanceIntent
    data class DeletePlace(val id: String) : FinanceIntent

    /** Корзина: вернуть операцию, удалить её навсегда, очистить всю корзину. */
    data class RestoreOperation(val id: String) : FinanceIntent
    data class DeleteForever(val id: String) : FinanceIntent
    data object EmptyTrash : FinanceIntent

    /** Сверка: «Остаток не сходится» в окне счёта, выбор способа, сохранение. */
    data object OpenReconcile : FinanceIntent
    data class PickReconcile(val step: Reconcile) : FinanceIntent
    data object FindOperation : FinanceIntent
    data class SaveInitialBalance(val initialBalance: Long) : FinanceIntent
    data class SaveRealBalance(val realBalance: Long) : FinanceIntent
    data object CloseReconcile : FinanceIntent
    data object CloseCorrection : FinanceIntent

    /** Резервная копия: окно, сохранение в выбранный файл, выбор файла для восстановления и подтверждение замены. */
    data object OpenBackup : FinanceIntent
    data object CloseBackup : FinanceIntent
    data class ExportBackup(val uri: String) : FinanceIntent
    data class PickBackup(val uri: String) : FinanceIntent
    data object ConfirmRestore : FinanceIntent

    /** Уведомления банка (#233): проверить доступ (после возврата из настроек Android), выключатель, счёт. */
    data object RefreshBankAccess : FinanceIntent
    data class SetBankEnabled(val enabled: Boolean) : FinanceIntent
    data class SetBankAccount(val accountId: String) : FinanceIntent

    /** Ответы на покупку из «Не разобрано» или нашего уведомления. «Изменить» открывает окно нового расхода с подстановкой. */
    data class RecordPurchase(val id: String) : FinanceIntent
    data class EditPurchase(val id: String) : FinanceIntent
    data class SkipPurchase(val id: String) : FinanceIntent
}

sealed interface FinanceEffect {
    data object AccountSaved : FinanceEffect
    data object OperationSaved : FinanceEffect
    data object OperationTrashed : FinanceEffect
    data object OperationRestored : FinanceEffect
    data object BackupSaved : FinanceEffect
    data object BackupFailed : FinanceEffect
    data object BackupInvalid : FinanceEffect
    data object BackupRestored : FinanceEffect
    data object PurchaseRecorded : FinanceEffect
}
