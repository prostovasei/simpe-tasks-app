package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.CategorySpend
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthAnalytics

/** «+3 600 ₽ · +9,2%»; прошлый период 0 ₽ — вместо процента [none] («в октябре не было») или только сумма. */
private fun deltaText(current: Long, previous: Long, none: String?): String {
    val rubles = Money.format(current - previous, sign = true)
    val percent = MonthAnalytics.percent(current, previous) ?: none
    return if (percent == null) rubles else "$rubles · $percent"
}

/**
 * Аналитика расходов за месяц (этап 3, #229; макеты design/approved/217: compact-module-bar.html, экраны 4.1 и 4.2):
 * переключатель месяца, итог с разницей к прошлому периоду и список категорий — сумма, полоска, доля и своя разница.
 * Рисуется под карточками счетов. Доходов, переводов и графиков здесь нет (решение PM).
 */
@Composable
fun AnalyticsBlock(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val analytics = remember(state.operations, state.month, state.today) { MonthAnalytics.of(state.operations.orEmpty(), state.month, state.today) }
    val nominative = stringArrayResource(R.array.months_nominative)
    val genitive = stringArrayResource(R.array.months_genitive)
    val dative = stringArrayResource(R.array.months_dative)
    val prepositional = stringArrayResource(R.array.months_prepositional)
    val previousIndex = analytics.previousMonth.monthValue - 1
    val wasNone = stringResource(R.string.analytics_was_none, prepositional[previousIndex])

    Column(Modifier.padding(horizontal = spacing.md).testTag(TAG_ANALYTICS)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = spacing.md, bottom = spacing.sm)) {
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.analytics_prev_month), TAG_MONTH_PREV, enabled = true) { onIntent(FinanceIntent.ShiftMonth(-1)) }
            Text(
                "${nominative[state.month.monthValue - 1]} ${state.month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag(TAG_MONTH_TITLE),
            )
            MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.analytics_next_month), TAG_MONTH_NEXT, enabled = state.canGoNextMonth) { onIntent(FinanceIntent.ShiftMonth(1)) }
        }

        // Итог: «Расходы · 1–10 ноября», крупная сумма, плашка разницы и с чем сравнили.
        Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.extraLarge).background(colors.surface).padding(spacing.md)) {
            val period = if (analytics.partial) {
                stringResource(R.string.analytics_expenses_days, days(analytics.lastDay, genitive[state.month.monthValue - 1]))
            } else {
                stringResource(R.string.analytics_expenses_month)
            }
            Text(period, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Text(
                Money.format(analytics.total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.padding(top = spacing.xxs).testTag(TAG_ANALYTICS_TOTAL),
            )
            if (analytics.total != 0L || analytics.previousTotal != 0L) {
                Text(
                    deltaText(analytics.total, analytics.previousTotal, none = null),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.xs).clip(AppTheme.shapes.small).background(colors.selected).padding(horizontal = spacing.xs, vertical = spacing.xxs).testTag(TAG_ANALYTICS_DELTA),
                )
                val versus = if (analytics.partial) days(analytics.previousLastDay, genitive[previousIndex]) else dative[previousIndex]
                Text(
                    if (analytics.previousTotal == 0L) stringResource(R.string.analytics_compare_none, versus) else stringResource(R.string.analytics_compare, versus, Money.format(analytics.previousTotal)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xs).testTag(TAG_ANALYTICS_COMPARE),
                )
            }
        }

        if (analytics.categories.isEmpty()) {
            Text(
                stringResource(R.string.analytics_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md).clip(AppTheme.shapes.extraLarge).background(colors.surface).padding(spacing.md).testTag(TAG_ANALYTICS_EMPTY),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = spacing.xxs, end = spacing.xxs, top = spacing.lg, bottom = spacing.sm)) {
                Text(stringResource(R.string.analytics_by_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.analytics_share), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Column(Modifier.fillMaxWidth().clip(AppTheme.shapes.extraLarge).background(colors.surface)) {
                analytics.categories.forEachIndexed { i, category ->
                    CategoryRow(category, wasNone)
                    if (i != analytics.categories.lastIndex) HorizontalDivider(color = colors.divider, thickness = AppTheme.sizes.hairline)
                }
            }
        }
    }
}

/** «1–10 ноября»; один день — «1 ноября». */
private fun days(last: Int, monthGenitive: String) = if (last == 1) "1 $monthGenitive" else "1–$last $monthGenitive"

@Composable
private fun MonthArrow(icon: ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.sizes.checkboxTouch)
            .clip(AppTheme.shapes.large)
            .background(colors.surface)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
    ) { Icon(icon, contentDescription = null, tint = colors.textPrimary) }
}

/** Категория: название и сумма, полоска доли, под ней разница с прошлым периодом и доля в процентах. */
@Composable
private fun CategoryRow(category: CategorySpend, wasNone: String) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm).testTag("$TAG_ANALYTICS_CATEGORY:${category.categoryId ?: "none"}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                category.name ?: stringResource(R.string.category_none),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = spacing.sm),
            )
            Text(Money.format(category.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, softWrap = false)
        }
        Box(Modifier.padding(top = spacing.xs).fillMaxWidth().height(AppTheme.sizes.progressHeight).clip(AppTheme.shapes.small).background(colors.selected)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(category.share / PERCENT).clip(AppTheme.shapes.small).background(colors.textSecondary))
        }
        Row(Modifier.padding(top = spacing.xs)) {
            Text(deltaText(category.amount, category.previous, wasNone), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${category.share}%", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

private const val PERCENT = 100f
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
