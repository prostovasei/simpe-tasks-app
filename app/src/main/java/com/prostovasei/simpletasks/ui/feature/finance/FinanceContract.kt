package com.prostovasei.simpletasks.ui.feature.finance

import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.Category
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
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
) {
    val kopecks: Long? get() = Money.parse(amount)

    val canSave: Boolean
        get() = (kopecks ?: 0) > 0 && (kind != OperationKind.TRANSFER || (toAccountId != null && toAccountId != accountId))

    /** Есть что сохранять черновиком: введена сумма, место или комментарий. */
    val hasInput: Boolean get() = amount.isNotEmpty() || place.isNotBlank() || comment.isNotBlank()
}

/** [accounts] и [operations] = null — ещё читаются из базы (экран пустой, без мигания пустых состояний). */
data class FinanceState(
    val accounts: List<Account>? = null,
    val tab: FinanceTab = FinanceTab.ANALYTICS,
    val window: AccountWindow = AccountWindow.None,
    val operations: List<Operation>? = null,
    val categories: List<Category> = emptyList(),
    val hints: FormHints = FormHints(),
    /** Открытое окно операции. */
    val form: OperationForm? = null,
    /** Незавершённый ввод новой операции: возвращается при следующем открытии окна (живёт, пока приложение не закрыто). */
    val draft: OperationForm? = null,
    val today: LocalDate = LocalDate.EPOCH,
    /** Месяц аналитики (этап 3); сохраняется при смене вкладки и модуля, после полного закрытия — снова текущий. */
    val month: YearMonth = YearMonth.from(today),
) {
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
    data class CreateCategory(val name: String, val kind: OperationKind, val select: Boolean = false) : FinanceIntent
    data class RenameCategory(val id: String, val name: String) : FinanceIntent
    data class ArchiveCategory(val id: String, val archived: Boolean) : FinanceIntent
}

sealed interface FinanceEffect {
    data object OperationSaved : FinanceEffect
    data object OperationTrashed : FinanceEffect
}
