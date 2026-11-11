package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Сумма в строке списка: расход «− 850 ₽», доход «+ 65 000 ₽», перевод — без знака. */
fun Operation.amountText(): String = when (kind) {
    OperationKind.EXPENSE -> "− ${Money.format(amount)}"
    OperationKind.INCOME -> "+ ${Money.format(amount)}"
    OperationKind.TRANSFER -> Money.format(amount)
}

/**
 * Вкладка «Операции» (этап 2, #225; экран 1.1): компактный список по датам — категория и сумма; перевод («Карта → Наличные»)
 * серым с подписью, в расходы и доходы он не входит. Нажатие открывает правку. Поиска и фильтров нет (решение PM).
 */
@Composable
fun OperationsList(operations: List<Operation>, today: LocalDate, onOpen: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    if (operations.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl).testTag(TAG_OPERATIONS_EMPTY)) {
            Text(stringResource(R.string.operations_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(stringResource(R.string.operations_empty_text), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = spacing.xs))
        }
        return
    }
    val days = operations.groupBy { it.date }.toList()
    LazyColumn(Modifier.fillMaxSize().testTag(TAG_OPERATIONS), contentPadding = PaddingValues(start = spacing.md, end = spacing.md, bottom = spacing.lg)) {
        items(days, key = { it.first.toEpochDay() }) { (date, list) ->
            Text(
                dayTitle(date, today),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = spacing.xxs, top = spacing.sm, bottom = spacing.xs),
            )
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.large).background(colors.surface)) {
                list.forEachIndexed { i, operation ->
                    OperationRow(operation) { onOpen(operation.id) }
                    if (i != list.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline, modifier = Modifier.padding(start = spacing.md))
                }
            }
        }
    }
}

@Composable
private fun OperationRow(operation: Operation, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val transfer = operation.kind == OperationKind.TRANSFER
    val title = if (transfer) "${operation.accountName} → ${operation.toAccountName.orEmpty()}" else operation.categoryName ?: stringResource(R.string.category_none)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizes.operationRow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .testTag("$TAG_OPERATION:${operation.id}"),
    ) {
        Column(Modifier.weight(1f).padding(end = spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (transfer) colors.textSecondary else colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (transfer) Text(stringResource(R.string.operation_transfer), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Text(
            operation.amountText(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (transfer) FontWeight.Medium else FontWeight.Bold,
            color = if (transfer) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

const val TAG_OPERATIONS = "finance_operations"
const val TAG_OPERATIONS_EMPTY = "finance_operations_empty"
const val TAG_OPERATION = "operation"
