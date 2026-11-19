package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("ru"))

@Composable
fun AnalyticsBlock(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val spacing = AppTheme.spacing
    val model = remember(state.operations, state.month, state.today) {
        MonthSpending.of(state.operations.orEmpty(), state.month, state.today)
    }
    val prefs = LocalContext.current.getSharedPreferences("finance_monthly_limits", 0)
    val monthKey = state.month.toString()
    var limit by remember(monthKey) { mutableStateOf(prefs.getLong(monthKey, 0L).takeIf { it > 0 }) }
    var editing by rememberSaveable(monthKey) { mutableStateOf(false) }
    if (editing) {
        var input by rememberSaveable(monthKey) { mutableStateOf(limit?.let(Money::input).orEmpty()) }
        val value = Money.parse(input)
        FinanceWindow("Лимит на месяц", "finance_limit_dialog", { editing = false }, actions = {
            Button(enabled = value != null && (input.isBlank() || value > 0), onClick = {
                if (value != null) {
                    limit = value.takeIf { it > 0 }
                    val editor = prefs.edit()
                    if (value == 0L) editor.remove(monthKey) else editor.putLong(monthKey, value)
                    editor.apply()
                    editing = false
                }
            }) { Text("Сохранить") }
        }) {
            OutlinedTextField(input, { input = Money.filterInput(it) },
                label = { Text("Лимит, ₽") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag(TAG_ANALYTICS)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${compactAmount(model.average, 1)}/день", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.analytics_prev_month), TAG_MONTH_PREV, true) { onIntent(FinanceIntent.ShiftMonth(-1)) }
            Text("${state.month.format(monthFormat).take(3)} ${(state.month.year % 100).toString().padStart(2, '0')}", Modifier.testTag(TAG_MONTH_TITLE), style = MaterialTheme.typography.labelLarge)
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.analytics_next_month), TAG_MONTH_NEXT, state.canGoNextMonth) { onIntent(FinanceIntent.ShiftMonth(1)) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Amount(R.drawable.ic_finance_receipt, "Потрачено", compactAmount(model.total), Modifier.weight(1f), amountTag = Modifier.testTag(TAG_ANALYTICS_TOTAL))
            Amount(R.drawable.ic_finance_limit, "Остаток до лимита", limit?.let { compactAmount((it - model.total).coerceAtLeast(0)) } ?: "—",
                Modifier.weight(1f).clickable(role = Role.Button) { editing = true }.padding(vertical = spacing.xs), Arrangement.Center)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                model.forecast?.let { Amount(R.drawable.ic_finance_forecast, "Прогноз", compactAmount(it)) }
            }
        }
        SpendingChart(model, state.month == YearMonth.from(state.today))
        if (model.total == 0L) {
            Text(stringResource(R.string.analytics_empty), Modifier.padding(vertical = spacing.sm).testTag(TAG_ANALYTICS_EMPTY))
        } else {
            Row(Modifier.fillMaxWidth().padding(top = spacing.xs), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SpendingTable("Категории", model.categories, Modifier.weight(1f))
                SpendingTable("Места", model.places, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Amount(icon: Int, label: String, amount: String, modifier: Modifier = Modifier, arrangement: Arrangement.Horizontal = Arrangement.Start, amountTag: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = arrangement) {
        Icon(painterResource(icon), label, Modifier.size(AppTheme.sizes.moduleIcon).padding(end = AppTheme.spacing.xxs), tint = AppTheme.colors.textSecondary)
        Text(amount, amountTag, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SpendingTable(title: String, rows: List<MonthSpending.Rank>, modifier: Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text("₽", color = AppTheme.colors.textSecondary)
        }
        rows.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                Text(listOfNotNull(row.icon, row.name).joinToString(" "), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(Money.amount(row.amount), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun compactAmount(amount: Long, precision: Int = 0): String {
    if (amount < 100_000L) return Money.format(amount)
    val scaled = if (precision == 0) "${(amount / 100_000.0).roundToLong()}" else
        String.format(Locale.forLanguageTag("ru"), "%.1f", amount / 100_000.0).removeSuffix(",0")
    return "${scaled}k ₽"
}

@Composable
private fun SpendingChart(model: MonthSpending, current: Boolean) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val labelStyle = MaterialTheme.typography.labelSmall
    val elapsed = model.cumulative.size
    val fraction = (elapsed - 1f) / (model.days - 1)
    val day = elapsed.toString().padStart(2, '0')
    val guide = colors.divider
    val ink = colors.textPrimary
    val muted = colors.textSecondary
    Column(Modifier.padding(top = spacing.sm)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(AppTheme.sizes.financeChartHeight)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Накопленные расходы за месяц" }) {
                fun point(index: Int, amount: Long) = Offset(
                    index.toFloat() / (model.days - 1) * size.width,
                    size.height * (1f - (amount.toDouble() / model.ceiling).toFloat()),
                )
                for (line in 0..2) {
                    val y = line * size.height / 2
                    drawLine(guide, Offset(0f, y), Offset(size.width, y))
                }
                val end = point(elapsed - 1, model.total)
                if (current) drawLine(guide, Offset(end.x, 0f), Offset(end.x, size.height), pathEffect = dash)
                model.cumulative.zipWithNext().forEachIndexed { i, (a, b) ->
                    drawLine(ink, point(i, a), point(i + 1, b), strokeWidth = 2.5f)
                }
                drawCircle(ink, radius = 3f, center = end)
                model.forecast?.let { drawLine(muted, end, point(model.days - 1, it), strokeWidth = 2f, pathEffect = dash) }
            }
            Text(axisAmount(model.ceiling), Modifier.offset(y = spacing.xxs), style = labelStyle, color = muted)
            Text(axisAmount(model.ceiling / 2), Modifier.offset(y = maxHeight / 2 + spacing.xxs), style = labelStyle, color = muted)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = spacing.xxs)) {
            Text("01", Modifier.align(Alignment.TopStart), style = labelStyle, color = if (current && elapsed == 1) ink else muted)
            Text(model.days.toString(), Modifier.align(Alignment.TopEnd), style = labelStyle, color = if (current && elapsed == model.days) ink else muted)
            if (current && elapsed in 2 until model.days) {
                Text(day, Modifier.fillMaxWidth().padding(start = maxWidth * (fraction * 2 - 1).coerceAtLeast(0f),
                    end = maxWidth * (1 - fraction * 2).coerceAtLeast(0f),
                    top = if (elapsed == 2 || elapsed == model.days - 1) spacing.md else spacing.xxs),
                    textAlign = TextAlign.Center, style = labelStyle)
            }
        }
    }
}

private fun axisAmount(amount: Long): String =
    String.format(Locale.ROOT, "%.3f", amount / 100_000.0).trimEnd('0').trimEnd('.') + "k"

@Composable
private fun MonthArrow(icon: ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.sizes.checkboxTouch)
            .clip(AppTheme.shapes.large)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
    ) { Icon(icon, contentDescription = null, tint = colors.textPrimary) }
}

private const val DISABLED_ALPHA = 0.35f

const val TAG_ANALYTICS = "finance_analytics"
const val TAG_MONTH_PREV = "analytics_month_prev"
const val TAG_MONTH_NEXT = "analytics_month_next"
const val TAG_MONTH_TITLE = "analytics_month"
const val TAG_ANALYTICS_TOTAL = "analytics_total"
const val TAG_ANALYTICS_DELTA = "analytics_delta"
const val TAG_ANALYTICS_COMPARE = "analytics_compare"
const val TAG_ANALYTICS_EMPTY = "analytics_empty"
const val TAG_ANALYTICS_CATEGORY = "analytics_category"
