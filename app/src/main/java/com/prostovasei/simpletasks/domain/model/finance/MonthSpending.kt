package com.prostovasei.simpletasks.domain.model.finance

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

internal data class MonthSpending(
    val days: Int,
    val cumulative: List<Long>,
    val forecast: Long?,
    val ceiling: Long,
    val categories: List<Rank>,
    val places: List<Rank>,
) {
    val total: Long get() = cumulative.last()
    val average: Long get() = (total.toDouble() / cumulative.size).roundToLong()

    data class Rank(val name: String, val amount: Long, val icon: String? = null)

    companion object {
        fun of(operations: List<Operation>, month: YearMonth, today: LocalDate): MonthSpending {
            val days = month.lengthOfMonth()
            val elapsed = if (YearMonth.from(today) == month) today.dayOfMonth else days
            val expenses = operations.filter {
                it.kind == OperationKind.EXPENSE && YearMonth.from(it.date) == month && it.date.dayOfMonth <= elapsed
            }
            val byDay = expenses.groupBy { it.date.dayOfMonth }
            var total = 0L
            val cumulative = (1..elapsed).map { day ->
                total += byDay[day].orEmpty().sumOf { it.amount }
                total
            }
            val forecast = if (elapsed < days && total > 0) (total.toDouble() / elapsed * days).roundToLong() else null
            val scaleMax = forecast ?: total
            var magnitude = 100_000L
            while (magnitude <= scaleMax / 10 && magnitude < Long.MAX_VALUE / 10) magnitude *= 10
            val ceiling = listOf(10, 15, 20, 25, 30, 40, 50, 60, 80, 100)
                .map { factor -> (magnitude / 10).let { if (it > Long.MAX_VALUE / factor) Long.MAX_VALUE else it * factor } }
                .first { it >= scaleMax }
            val categories = expenses.groupBy { it.categoryId }.values.map { entries ->
                val first = entries.first()
                Rank(first.categoryName ?: "Без категории", entries.sumOf { it.amount }, first.categoryEmoji)
            }
            val byPlace = expenses.groupBy { it.place.orEmpty().trim() }
            val places = byPlace.filterKeys { it.isNotEmpty() }.map { (name, entries) -> Rank(name, entries.sumOf { it.amount }) }
            return MonthSpending(days, cumulative, forecast, ceiling, rank(categories),
                rank(places, byPlace[""].orEmpty().sumOf { it.amount }))
        }

        private fun rank(rows: List<Rank>, unassigned: Long = 0): List<Rank> {
            val ordered = rows.sortedWith(compareByDescending<Rank> { it.amount }.thenBy { it.name })
            val others = ordered.drop(4).sumOf { it.amount } + unassigned
            return (ordered.take(4) + if (others > 0) listOf(Rank("Остальное", others)) else emptyList())
                .sortedWith(compareByDescending<Rank> { it.amount }.thenBy { it.name })
        }
    }
}
