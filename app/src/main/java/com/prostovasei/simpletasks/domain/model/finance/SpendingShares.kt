package com.prostovasei.simpletasks.domain.model.finance

import java.math.BigInteger
import kotlin.math.abs

internal fun sharePercentages(rows: List<MonthSpending.Rank>, total: Long): List<Int> {
    if (total <= 0) return emptyList()
    val denominator = BigInteger.valueOf(total)
    val parts = rows.map { (BigInteger.valueOf(it.amount) * BigInteger.valueOf(100)).divideAndRemainder(denominator) }
    val percentages = parts.map { it[0].toInt() }.toMutableList()
    parts.indices.sortedByDescending { parts[it][1] }.take(100 - percentages.sum()).forEach { percentages[it]++ }
    return percentages
}

internal fun financeThousands(amount: Long): String {
    val thousands = abs(amount / 100_000) + if (abs(amount % 100_000) >= 50_000) 1 else 0
    return (if (amount < 0 && thousands != 0L) "−" else "") + "${thousands}k"
}
