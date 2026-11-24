package com.prostovasei.simpletasks.domain.model.finance

import java.time.LocalDate
import java.time.YearMonth

/** Бюджет категории на месяц (#327). [amount] — копейки; 0 — бюджет снят, но месяц уже заведён (прошлые на него не переносятся). */
data class Budget(val month: YearMonth, val categoryId: String, val amount: Long)

/** Состояние строки расходов по темпу (#327, §3 ТЗ). */
enum class BudgetPace {
    /** Потрачено не больше плана к сегодня. */
    OK,

    /** Тратится быстрее плана к сегодня, но бюджет ещё не выбран. */
    AHEAD,

    /** Потрачено ровно столько, сколько запланировано: разовые платежи не горят жёлтым. */
    DONE,

    /** Перерасход. */
    OVER,
}

/** Подсказка «в среднем»: [months] — за сколько полных месяцев; 0 — по прогнозу текущего месяца. */
data class BudgetSuggestion(val amount: Long, val months: Int)

/** Строка окна «Бюджет»: категория ([categoryId] = null — «Без категории»), её план и факт месяца. */
data class BudgetLine(
    val categoryId: String?,
    val name: String,
    val emoji: String?,
    val kind: OperationKind,
    val budget: Long?,
    val fact: Long,
    val suggestion: BudgetSuggestion? = null,
)

/** Общие правила бюджета: одни для окна, окна операции, уведомлений и виджета. */
object BudgetRules {
    /** Порог уведомления о расходах категории. */
    const val ALERT_PERCENT = 80

    /** Подсказка «в среднем» округляется вверх до 100 ₽. */
    const val ROUND = 10_000L

    /** Сколько полных месяцев берётся для подсказки. */
    const val AVERAGE_MONTHS = 3L

    /**
     * Бюджеты, действующие в [month]: заведённые строки месяца. Незаведённый текущий или будущий месяц берёт бюджеты последнего
     * заведённого месяца раньше него — так 1-го числа бюджеты переносятся без подтверждения. Прошлые месяцы не наследуют.
     * Нулевые строки (снятые бюджеты) в ответ не входят.
     */
    fun effective(all: List<Budget>, month: YearMonth, current: YearMonth): Map<String, Long> {
        val own = all.filter { it.month == month }
        val rows = if (own.isNotEmpty() || month < current) {
            own
        } else {
            val previous = all.filter { it.month < month }.maxOfOrNull { it.month }
            all.filter { it.month == previous }
        }
        return rows.filter { it.amount > 0 }.associate { it.categoryId to it.amount }
    }

    /** Месяц заведён: у него есть свои строки (подтверждён или правился). */
    fun isSet(all: List<Budget>, month: YearMonth): Boolean = all.any { it.month == month }

    /** План к сегодня: бюджет × номер дня / дней в месяце. Только для текущего месяца, иначе null. */
    fun planToDate(budget: Long, month: YearMonth, today: LocalDate): Long? =
        if (YearMonth.from(today) != month) null else budget * today.dayOfMonth / month.lengthOfMonth()

    /** Доля плана к сегодня (0..1) для отметки «сегодня»; null — не текущий месяц. */
    fun todayShare(month: YearMonth, today: LocalDate): Float? =
        if (YearMonth.from(today) != month) null else today.dayOfMonth.toFloat() / month.lengthOfMonth()

    fun pace(budget: Long, fact: Long, month: YearMonth, today: LocalDate): BudgetPace = when {
        fact > budget -> BudgetPace.OVER
        fact == budget -> BudgetPace.DONE
        fact > (planToDate(budget, month, today) ?: budget) -> BudgetPace.AHEAD
        else -> BudgetPace.OK
    }

    /** Процент бюджета, целый, вниз: 16 800 из 20 000 — 84. */
    fun percent(budget: Long, fact: Long): Int = if (budget <= 0) 0 else (fact * 100 / budget).toInt()

    /**
     * Подсказки «в среднем» по категориям (#327, §7 ТЗ): сумма операций категории за последние до [AVERAGE_MONTHS] полных месяцев
     * учёта, делённая на их число, вверх до 100 ₽. Полный месяц учёта — завершённый месяц, начавшийся не раньше первой операции
     * (месяц первой операции — только если она 1-го числа). Полных нет — прогноз текущего месяца: траты × дней / прошедшие дни.
     */
    fun suggestions(operations: List<Operation>, today: LocalDate): Map<String, BudgetSuggestion> {
        val counted = operations.filter { (it.kind == OperationKind.EXPENSE || it.kind == OperationKind.INCOME) && it.categoryId != null }
        val first = operations.minOfOrNull { it.date } ?: return emptyMap()
        val current = YearMonth.from(today)
        val tracked = YearMonth.from(first).let { if (first.dayOfMonth == 1) it else it.plusMonths(1) }
        val from = maxOf(tracked, current.minusMonths(AVERAGE_MONTHS))
        val months = generateSequence(from) { it.plusMonths(1) }.takeWhile { it < current }.toList()
        if (months.isNotEmpty()) {
            val inRange = counted.filter { YearMonth.from(it.date) in months }
            return inRange.groupBy { it.categoryId!! }.mapValues { (_, list) ->
                BudgetSuggestion(roundUp(list.sumOf { it.amount }.toDouble() / months.size), months.size)
            }.filterValues { it.amount > 0 }
        }
        val elapsed = today.dayOfMonth
        val days = current.lengthOfMonth()
        return counted.filter { YearMonth.from(it.date) == current && !it.date.isAfter(today) }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> BudgetSuggestion(roundUp(list.sumOf { it.amount }.toDouble() * days / elapsed), 0) }
            .filterValues { it.amount > 0 }
    }

    private fun roundUp(amount: Double): Long {
        val whole = kotlin.math.ceil(amount).toLong()
        return (whole + ROUND - 1) / ROUND * ROUND
    }
}

/**
 * Бюджет месяца для окна «Бюджет» (#327): строки расходов и доходов с планом и строки без плана; суммы месяца.
 * Факт текущего месяца — по сегодня включительно, как в «Аналитике».
 */
data class MonthBudget(
    val month: YearMonth,
    val today: LocalDate,
    val expenses: List<BudgetLine>,
    val freeExpenses: List<BudgetLine>,
    val incomes: List<BudgetLine>,
    val freeIncomes: List<BudgetLine>,
    /** Все расходы и доходы месяца, включая категории без плана. */
    val spent: Long,
    val received: Long,
) {
    /** Лимит месяца = сумма бюджетов расходов (решение PM, вопрос 9). */
    val planOut: Long get() = expenses.sumOf { it.budget ?: 0 }
    val planIn: Long get() = incomes.sumOf { it.budget ?: 0 }
    val isEmpty: Boolean get() = expenses.isEmpty() && incomes.isEmpty()

    companion object {
        fun of(
            budgets: Map<String, Long>,
            categories: List<Category>,
            operations: List<Operation>,
            month: YearMonth,
            today: LocalDate,
            suggestions: Map<String, BudgetSuggestion> = emptyMap(),
        ): MonthBudget {
            val last = if (YearMonth.from(today) == month) today else month.atEndOfMonth()
            val inMonth = operations.filter { YearMonth.from(it.date) == month && !it.date.isAfter(last) }
            fun lines(kind: OperationKind): Pair<List<BudgetLine>, List<BudgetLine>> {
                val ops = inMonth.filter { it.kind == kind }
                val facts = ops.groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.amount } }
                val all = categories.filter { it.kind == kind }
                val planned = all.filter { (budgets[it.id] ?: 0) > 0 }
                    .map { BudgetLine(it.id, it.name, it.emoji, kind, budgets.getValue(it.id), facts[it.id] ?: 0, suggestions[it.id]) }
                    .sortedWith(compareByDescending<BudgetLine> { it.budget }.thenBy { it.name })
                val free = all.filter { (budgets[it.id] ?: 0) <= 0 && (!it.archived || (facts[it.id] ?: 0) > 0) }
                    .map { BudgetLine(it.id, it.name, it.emoji, kind, null, facts[it.id] ?: 0, suggestions[it.id]) }
                val none = facts[null]?.takeIf { it > 0 }?.let { listOf(BudgetLine(null, "Без категории", null, kind, null, it)) }.orEmpty()
                return planned to (free + none).sortedWith(compareByDescending<BudgetLine> { it.fact }.thenBy { it.name })
            }
            val (expenses, freeExpenses) = lines(OperationKind.EXPENSE)
            val (incomes, freeIncomes) = lines(OperationKind.INCOME)
            return MonthBudget(
                month, today, expenses, freeExpenses, incomes, freeIncomes,
                spent = inMonth.filter { it.kind == OperationKind.EXPENSE }.sumOf { it.amount },
                received = inMonth.filter { it.kind == OperationKind.INCOME }.sumOf { it.amount },
            )
        }
    }
}
