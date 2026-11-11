package com.prostovasei.simpletasks.domain.model.finance

import java.time.LocalDate
import java.time.YearMonth

/** Расходы одной категории за период. [categoryId] = null — «Без категории». [previous] — за прошлый период, [share] — доля в процентах. */
data class CategorySpend(val categoryId: String?, val name: String?, val amount: Long, val previous: Long, val share: Int)

/**
 * Аналитика расходов за месяц (финансы, этап 3, #229; решения PM — requirements.md §8). Период — календарный месяц [month];
 * текущий незавершённый месяц считается по сегодняшний день ([partial]) и сравнивается с теми же днями прошлого месяца
 * (если в нём дней меньше — до его последнего дня), завершённые месяцы — целиком. [lastDay] и [previousLastDay] — последние дни периодов.
 * В расходы входят только операции-расходы: переводы и доходы не считаются. Категории без расходов за период в список не попадают.
 */
data class MonthAnalytics(
    val month: YearMonth,
    val partial: Boolean,
    val lastDay: Int,
    val previousLastDay: Int,
    val total: Long,
    val previousTotal: Long,
    val categories: List<CategorySpend>,
) {
    val previousMonth: YearMonth get() = month.minusMonths(1)

    companion object {
        fun of(operations: List<Operation>, month: YearMonth, today: LocalDate): MonthAnalytics {
            val partial = month == YearMonth.from(today)
            val lastDay = if (partial) today.dayOfMonth else month.lengthOfMonth()
            val previous = month.minusMonths(1)
            val previousLastDay = if (partial) minOf(lastDay, previous.lengthOfMonth()) else previous.lengthOfMonth()
            val expenses = operations.filter { it.kind == OperationKind.EXPENSE }
            fun within(ym: YearMonth, last: Int) = expenses.filter { YearMonth.from(it.date) == ym && it.date.dayOfMonth <= last }
            val now = within(month, lastDay)
            val before = within(previous, previousLastDay).groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.amount } }
            val total = now.sumOf { it.amount }
            val categories = now.groupBy { it.categoryId }.map { (id, list) ->
                val amount = list.sumOf { it.amount }
                CategorySpend(id, list.first().categoryName, amount, before[id] ?: 0, share = if (total == 0L) 0 else Math.round(amount * 100.0 / total).toInt())
            }.sortedWith(compareByDescending<CategorySpend> { it.amount }.thenBy { it.name.orEmpty() })
            return MonthAnalytics(month, partial, lastDay, previousLastDay, total, before.values.sum(), categories)
        }

        /** Изменение в процентах с одним знаком: «+9,2%», «−9,1%»; прошлое значение 0 — null (процент не показывается). */
        fun percent(current: Long, previous: Long): String? {
            if (previous == 0L) return null
            val tenths = Math.round((current - previous) * 1000.0 / previous)
            val sign = if (tenths < 0) "−" else "+"
            val abs = kotlin.math.abs(tenths)
            return "$sign${abs / 10},${abs % 10}%"
        }
    }
}
