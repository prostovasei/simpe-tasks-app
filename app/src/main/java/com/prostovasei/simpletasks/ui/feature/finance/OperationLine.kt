package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Operation
import com.prostovasei.simpletasks.domain.model.finance.OperationKind

const val TAG_OPERATION_COMMENT_MARK = "operation_comment"

/** Значок слева (#245): эмодзи категории, у перевода «⇄», у корректировки «⚖», без категории — серый кружок «?». */
@Composable
fun OperationMark(operation: Operation) {
    when (operation.kind) {
        OperationKind.TRANSFER -> SymbolMark("⇄")
        OperationKind.CORRECTION -> SymbolMark("⚖")
        else -> if (operation.categoryName == null) UnknownMark() else CategoryMark(operation.categoryEmoji, operation.categoryName)
    }
}

@Composable
private fun SymbolMark(symbol: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AppTheme.sizes.financeMark)) {
        Text(symbol, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textSecondary)
    }
}

/** Серый кружок «?» — категория ещё не выбрана (например, расход из уведомления банка). */
@Composable
fun UnknownMark() {
    val colors = AppTheme.colors
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AppTheme.sizes.financeMark)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AppTheme.sizes.financeUnknownMark).clip(CircleShape).background(colors.selected)) {
            Text("?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        }
    }
}

/** Счёт со значком: «💳 Карта». */
fun accountLabel(emoji: String, name: String): String = "$emoji $name".trim()

/** Название плашки: категория (или «Без категории»), у перевода — «💳 Карта → 👛 Наличные», у корректировки — счёт со значком. */
@Composable
fun Operation.lineTitle(): String = when (kind) {
    OperationKind.TRANSFER -> stringResource(R.string.operation_title_transfer, accountLabel(accountEmoji, accountName), accountLabel(toAccountEmoji.orEmpty(), toAccountName.orEmpty()))
    OperationKind.CORRECTION -> accountLabel(accountEmoji, accountName)
    else -> categoryName ?: stringResource(R.string.category_none)
}

/** Серая строка: у перевода и корректировки — их подпись, у остальных — «📍 место · 💳 счёт» (нет места — только счёт). */
@Composable
fun Operation.lineDetails(): String = when (kind) {
    OperationKind.TRANSFER -> stringResource(R.string.operation_transfer)
    OperationKind.CORRECTION -> stringResource(R.string.operation_correction_caption)
    else -> {
        val account = accountLabel(accountEmoji, accountName)
        place?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.operation_details_place, it, account) } ?: account
    }
}

/**
 * Плашка операции (#245, макеты `design/approved/245`): значок, название с серой строкой деталей, «💬» при комментарии, сумма.
 * [details] — серая строка (в корзине к ней добавлены дни); [trailing] — кнопки справа (корзина).
 */
@Composable
fun OperationLine(operation: Operation, details: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    // Перевод и корректировка — серым: в расходы и доходы они не входят.
    val neutral = operation.kind == OperationKind.TRANSFER || operation.kind == OperationKind.CORRECTION
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.operationRow).padding(horizontal = spacing.md, vertical = spacing.xs),
    ) {
        OperationMark(operation)
        Column(Modifier.weight(1f).padding(start = spacing.xs, end = spacing.xs)) {
            Text(
                operation.lineTitle(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (neutral) colors.textSecondary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(details, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!operation.comment.isNullOrBlank()) {
            Text(stringResource(R.string.operation_comment_mark), style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("$TAG_OPERATION_COMMENT_MARK:${operation.id}"))
        }
        Text(
            operation.amountText(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (neutral) FontWeight.Medium else FontWeight.Bold,
            color = when {
                neutral -> colors.textSecondary
                operation.kind == OperationKind.INCOME -> colors.income
                else -> colors.textPrimary
            },
            maxLines = 1,
            softWrap = false,
        )
        trailing()
    }
}
