package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale.forLanguageTag("ru")
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RU)
private val DAY_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RU)

/** «10 ноября · сегодня», «9 ноября · вчера», «8 ноября», а в другом году — «30 декабря 2025». */
@Composable
fun dayTitle(date: LocalDate, today: LocalDate): String {
    val text = date.format(if (date.year == today.year) DAY else DAY_YEAR)
    return when (date) {
        today -> "$text · ${stringResource(R.string.finance_today)}"
        today.minusDays(1) -> "$text · ${stringResource(R.string.finance_yesterday)}"
        else -> text
    }
}

/** День коротко для подписи: «сегодня», «вчера», иначе «6 ноября» (в другом году — с годом). */
@Composable
fun shortDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.finance_today)
    today.minusDays(1) -> stringResource(R.string.finance_yesterday)
    else -> date.format(if (date.year == today.year) DAY else DAY_YEAR)
}

/** День без «сегодня / вчера»: «9 ноября». */
fun plainDay(date: LocalDate, today: LocalDate): String = date.format(if (date.year == today.year) DAY else DAY_YEAR)

/** Сумма в строке списка: расход «− 850 ₽», доход «+ 65 000 ₽», перевод — без знака, корректировка — со своим знаком. */
fun Operation.amountText(): String = when (kind) {
    OperationKind.EXPENSE -> "− ${Money.format(amount)}"
    OperationKind.INCOME -> "+ ${Money.format(amount)}"
    OperationKind.TRANSFER -> Money.format(amount)
    OperationKind.CORRECTION -> Money.format(amount, sign = true)
}

/** Группы операций для общего списка финансов. */
internal fun LazyListScope.operationsFeed(
    operations: List<Operation>, today: LocalDate, unsorted: Int,
    onUnsorted: () -> Unit, onOpen: (String) -> Unit,
) {
    if (unsorted > 0) item("unsorted") {
        UnsortedRow(unsorted, onUnsorted, Modifier.padding(horizontal = AppTheme.spacing.md))
    }
    if (operations.isEmpty()) item("operations-empty") {
        Text(stringResource(R.string.operations_empty_title), Modifier.padding(AppTheme.spacing.md).testTag(TAG_OPERATIONS_EMPTY))
    }
    operations.groupBy { it.date }.toSortedMap(reverseOrder()).forEach { (date, list) ->
        item("day:${date.toEpochDay()}") {
            val colors = AppTheme.colors
            val spacing = AppTheme.spacing
            Box(Modifier.padding(horizontal = spacing.md)) {
            // Итог дня (#245): расходы за день; переводы и корректировки в него не входят.
            val expenses = list.filter { it.kind == OperationKind.EXPENSE }.sumOf { it.amount }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(start = spacing.xxs, end = spacing.xxs, top = spacing.sm, bottom = spacing.xs)) {
                Text(dayTitle(date, today), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                if (expenses > 0) {
                    Text(
                        stringResource(R.string.operation_day_expenses, Money.format(expenses)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.testTag("$TAG_OPERATIONS_DAY_TOTAL:${date.toEpochDay()}"),
                    )
                }
            }
            }
        }
        items(list, key = { "operation:${it.id}" }) { operation ->
            OperationRow(operation) { onOpen(operation.id) }
        }
    }
}

/** Название строки: категория (или «Без категории»), у перевода — «Карта → Наличные», у корректировки — счёт. */
@Composable
fun Operation.title(): String = when (kind) {
    OperationKind.TRANSFER -> "$accountName → ${toAccountName.orEmpty()}"
    OperationKind.CORRECTION -> accountName
    else -> categoryName ?: stringResource(R.string.category_none)
}

@Composable
private fun OperationRow(operation: Operation, onClick: () -> Unit) {
    OperationLine(operation, operation.lineDetails(), Modifier.clickable(role = Role.Button, onClick = onClick).testTag("$TAG_OPERATION:${operation.id}"))
}

const val TAG_OPERATIONS = "finance_operations"
const val TAG_OPERATIONS_DAY_TOTAL = "finance_operations_day_total"
const val TAG_OPERATIONS_EMPTY = "finance_operations_empty"
const val TAG_OPERATION = "operation"
