package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

internal fun financeLimitPreferences(context: android.content.Context) =
    context.getSharedPreferences("finance_monthly_limits", android.content.Context.MODE_PRIVATE)

internal fun financeMonthLabel(month: YearMonth) =
    "${month.format(monthFormat).take(3)} ${(month.year % 100).toString().padStart(2, '0')}"

@Composable
fun AnalyticsBlock(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val spacing = AppTheme.spacing
    val model = remember(state.operations, state.month, state.today) {
        MonthSpending.of(state.operations.orEmpty(), state.month, state.today)
    }
    val context = LocalContext.current
    val budget = remember(state.budgets, state.categories, state.operations, state.month, state.today) { state.monthBudget(state.month) }
    val limit = monthLimit(context, budget)
    val openBudget = { onIntent(FinanceIntent.OpenBudget(state.month)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag(TAG_ANALYTICS)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${compactAmount(model.average, 1)}/день", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.analytics_prev_month), TAG_MONTH_PREV, true) { onIntent(FinanceIntent.ShiftMonth(-1)) }
            Text(financeMonthLabel(state.month), Modifier.testTag(TAG_MONTH_TITLE), style = MaterialTheme.typography.labelLarge)
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.analytics_next_month), TAG_MONTH_NEXT, state.canGoNextMonth) { onIntent(FinanceIntent.ShiftMonth(1)) }
        }
        LimitBar(model.total, limit, model.forecast, state.month, state.today, openBudget)
        SpendingChart(model, state.month == YearMonth.from(state.today))
        if (model.total == 0L) {
            Text(stringResource(R.string.analytics_empty), Modifier.padding(vertical = spacing.sm).testTag(TAG_ANALYTICS_EMPTY))
        } else {
            SpendingStrip("Категории", model.categories, model.total, openBudget)
            SpendingStrip("Места", model.places, model.total)
        }
    }
}

@Composable
private fun SpendingStrip(title: String, rows: List<MonthSpending.Rank>, total: Long, onClick: (() -> Unit)? = null) {
    val density = LocalDensity.current
    val ink = AppTheme.colors.textPrimary.toArgb()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = AppTheme.spacing.md)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick).testTag(TAG_ANALYTICS_CATEGORY) else Modifier)) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val bitmap = remember(rows, total, width, density, ink) {
            sharesBitmap(rows, total, width, density.density, density.fontScale, ink).asImageBitmap()
        }
        Image(bitmap, sharesDescription(title, rows, total),
            Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height))
    }
}

internal fun compactAmount(amount: Long, precision: Int = 0): String {
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

internal fun axisAmount(amount: Long): String =
    String.format(Locale.ROOT, "%.3f", amount / 100_000.0).trimEnd('0').trimEnd('.') + "k"

@Composable
private fun MonthArrow(icon: ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.sizes.checkboxTouch)
            .clip(AppTheme.shapes.large)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
    ) { Icon(icon, contentDescription = null, tint = colors.textSecondary) }
}

const val TAG_ANALYTICS = "finance_analytics"
const val TAG_MONTH_PREV = "analytics_month_prev"
const val TAG_MONTH_NEXT = "analytics_month_next"
const val TAG_MONTH_TITLE = "analytics_month"
const val TAG_ANALYTICS_TOTAL = "analytics_total"
const val TAG_ANALYTICS_DELTA = "analytics_delta"
const val TAG_ANALYTICS_COMPARE = "analytics_compare"
const val TAG_ANALYTICS_EMPTY = "analytics_empty"
const val TAG_ANALYTICS_CATEGORY = "analytics_category"
