package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppColors
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.finance.BudgetLine
import com.prostovasei.simpletasks.domain.model.finance.BudgetPace
import com.prostovasei.simpletasks.domain.model.finance.BudgetRules
import com.prostovasei.simpletasks.domain.model.finance.BudgetSuggestion
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthBudget
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * #327 — бюджет на месяц по категориям (макеты design/approved/327, решения PM 24.11.2026).
 * Полоса лимита вверху «Финансов» — вариант Г; окно «Бюджет»; правка бюджета категории; подсказка в окне операции.
 */

/** Цвет заполнения по темпу: тёмный — в плане и «бюджет выбран», жёлтый — быстрее плана, красный — перерасход. */
internal fun AppColors.pace(pace: BudgetPace): Color = when (pace) {
    BudgetPace.OK, BudgetPace.DONE -> textPrimary
    BudgetPace.AHEAD -> warning
    BudgetPace.OVER -> danger
}

/** Подпись под полоской расходов: «осталось N ₽», «бюджет выбран» или «перерасход N ₽». */
internal fun paceText(pace: BudgetPace, budget: Long, fact: Long, amount: (Long) -> String = Money::format): String = when (pace) {
    BudgetPace.OVER -> "перерасход ${amount(fact - budget)}"
    BudgetPace.DONE -> "бюджет выбран"
    else -> "осталось ${amount(budget - fact)}"
}

/** Лимит месяца: сумма бюджетов расходов; без бюджетов — прежний лимит, заданный вручную до #327. */
internal fun monthLimit(context: android.content.Context, budget: MonthBudget): Long? =
    budget.planOut.takeIf { it > 0 } ?: financeLimitPreferences(context).getLong(budget.month.toString(), 0L).takeIf { it > 0 }

/** Полоска с отметкой «сегодня»: заполнение [fill] (0..1), отметка в доле [today] (null — без отметки). */
@Composable
internal fun BudgetProgress(fill: Float, color: Color, today: Float?, modifier: Modifier = Modifier, height: Int = SizeTokens.BUDGET_BAR, content: @Composable BoxScope.() -> Unit = {}) {
    val colors = AppTheme.colors
    BoxWithConstraints(modifier.fillMaxWidth().height((height + 2 * SizeTokens.BUDGET_MARK_OVER).dp)) {
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().height(height.dp).clip(CircleShape).background(colors.accentSoft),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill.coerceIn(0f, 1f)).clip(CircleShape).background(color))
            content()
        }
        if (today != null) {
            Box(
                Modifier
                    .offset(x = maxWidth * today - (SizeTokens.BUDGET_MARK / 2).dp)
                    .width(SizeTokens.BUDGET_MARK.dp)
                    .fillMaxHeight()
                    .border(SizeTokens.HAIRLINE.dp, colors.surface)
                    .background(colors.textSecondary)
                    .testTag(TAG_BUDGET_TODAY),
            )
        }
    }
}

/**
 * Полоса лимита вверху «Финансов» (вариант Г, решение PM 24.11.2026): внутри потрачено, справа лимит; под ней «осталось»
 * (или «перерасход») и прогноз. Нажатие открывает окно «Бюджет». Без лимита — серая полоса и «Задать бюджет».
 */
@Composable
internal fun LimitBar(spent: Long, limit: Long?, forecast: Long?, month: YearMonth, today: java.time.LocalDate, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val pace = limit?.let { BudgetRules.pace(it, spent, month, today) }
    val fill = if (limit == null) 0f else (spent.toFloat() / limit).coerceIn(0f, 1f)
    val color = pace?.let { colors.pace(it) } ?: colors.accentSoft
    val inside = fill >= LIMIT_TEXT_INSIDE
    Column(
        Modifier.fillMaxWidth().clip(AppTheme.shapes.large).clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.xxs).testTag(TAG_LIMIT_BAR),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            BudgetProgress(fill, color, limit?.let { BudgetRules.todayShare(month, today) }, Modifier.weight(1f), SizeTokens.LIMIT_BAR) {
                Text(
                    compactAmount(spent),
                    Modifier.align(Alignment.CenterStart).padding(horizontal = spacing.sm).testTag(TAG_ANALYTICS_TOTAL),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (inside) colors.onAccent else colors.textPrimary,
                )
            }
            Text(limit?.let { compactAmount(it).removeSuffix(" ₽").removeSuffix(" ₽") } ?: "—",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val status = if (limit == null || pace == null) "Задать бюджет" else paceText(pace, limit, spent, ::compactAmount)
            Text(
                status, Modifier.weight(1f).testTag(TAG_LIMIT_STATUS), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = when (pace) {
                    null, BudgetPace.OK, BudgetPace.DONE -> colors.textSecondary
                    else -> colors.pace(pace)
                },
            )
            if (forecast != null) {
                Icon(painterResource(R.drawable.ic_finance_forecast), null, Modifier.size(AppTheme.sizes.moduleIcon).padding(end = spacing.xxs), tint = colors.textSecondary)
                Text("прогноз ${compactAmount(forecast)}", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
}

/** Окно «Бюджет» месяца с правкой категории поверх. */
@Composable
internal fun BudgetDialog(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val window = state.budgetWindow ?: return
    val budget = remember(state.budgets, state.categories, state.operations, window.month, state.today) { state.monthBudget(window.month) }
    val current = YearMonth.from(state.today)
    val editable = window.month >= current
    val title = "Бюджет · ${financeMonthLabel(window.month)}"
    FinanceWindow(title, TAG_BUDGET_WINDOW, { onIntent(FinanceIntent.CloseBudget) }, actions = {
        if (window.prepare) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                val averages = (budget.expenses + budget.freeExpenses + budget.incomes + budget.freeIncomes)
                    .mapNotNull { line -> line.categoryId?.let { id -> line.suggestion?.let { id to it.amount } } }.toMap()
                OutlinedButton(onClick = { onIntent(FinanceIntent.SetBudgets(averages)) }, enabled = averages.isNotEmpty(),
                    modifier = Modifier.weight(1f).testTag(TAG_BUDGET_ALL_AVERAGE)) { Text("Всё по средним") }
                Button(onClick = { onIntent(FinanceIntent.ConfirmBudget) }, modifier = Modifier.weight(1f).testTag(TAG_BUDGET_CONFIRM)) { Text("Подтвердить") }
            }
        }
    }) {
        if (window.prepare) {
            BudgetNote("Копия ${monthGenitive(window.month.minusMonths(1))}. Под суммой — средние траты, нажатие подставляет.")
        } else if (!budget.isEmpty) {
            BudgetNote("Доходы ${Money.amount(budget.planIn)} − расходы ${Money.amount(budget.planOut)} = отложится ${Money.format(budget.planIn - budget.planOut)}")
        }
        if (budget.isEmpty && editable && !window.prepare) {
            val averages = (budget.freeExpenses + budget.freeIncomes).mapNotNull { line -> line.categoryId?.let { id -> line.suggestion?.let { id to it.amount } } }.toMap()
            BudgetNote("Бюджетов пока нет. Задайте сумму у категории или заполните все категории по средним тратам — их можно поправить.")
            if (averages.isNotEmpty()) {
                Button(onClick = { onIntent(FinanceIntent.SetBudgets(averages)) }, modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs).testTag(TAG_BUDGET_FILL)) {
                    Text("Заполнить по средним")
                }
            }
        }
        val open: (BudgetLine) -> Unit = { line -> line.categoryId?.let { onIntent(FinanceIntent.EditBudget(it)) } }
        val suggest: (BudgetLine) -> Unit = { line -> line.categoryId?.let { id -> line.suggestion?.let { onIntent(FinanceIntent.SetBudgets(mapOf(id to it.amount))) } } }
        BudgetSection("Расходы", budget.planOut, budget.spent, budget.expenses, budget.freeExpenses, OperationKind.EXPENSE, window.prepare, editable, budget, open, suggest)
        BudgetSection("Доходы", budget.planIn, budget.received, budget.incomes, budget.freeIncomes, OperationKind.INCOME, window.prepare, editable, budget, open, suggest)
    }
    window.editing?.let { id ->
        val line = (budget.expenses + budget.freeExpenses + budget.incomes + budget.freeIncomes).firstOrNull { it.categoryId == id } ?: return@let
        BudgetEditDialog(line, window.month, budget, onIntent)
    }
}

@Composable
private fun BudgetNote(text: String) {
    Text(text, Modifier.padding(bottom = AppTheme.spacing.xs), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
}

@Composable
private fun BudgetSection(
    title: String,
    plan: Long,
    fact: Long,
    planned: List<BudgetLine>,
    free: List<BudgetLine>,
    kind: OperationKind,
    prepare: Boolean,
    editable: Boolean,
    budget: MonthBudget,
    onOpen: (BudgetLine) -> Unit,
    onSuggest: (BudgetLine) -> Unit,
) {
    if (planned.isEmpty() && free.isEmpty()) return
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(Modifier.fillMaxWidth().padding(top = spacing.md, bottom = spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        if (prepare && plan > 0) Text(Money.format(plan), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
    }
    if (!prepare && planned.isNotEmpty()) {
        BudgetRow(BudgetLine(null, "Всего", null, kind, plan, fact), budget, total = true, onClick = null)
        HorizontalDivider(thickness = SizeTokens.BUDGET_TOTAL_RULE.dp, color = colors.divider)
    }
    planned.forEach { line ->
        if (prepare) PlanRow(line, onOpen, onSuggest) else BudgetRow(line, budget, onClick = if (editable && line.categoryId != null) ({ onOpen(line) }) else null)
        HorizontalDivider(color = colors.divider)
    }
    if (free.isNotEmpty()) {
        Text(if (kind == OperationKind.EXPENSE) "Без бюджета" else "Без плана", Modifier.padding(top = spacing.sm),
            style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        free.forEach { line ->
            if (prepare) PlanRow(line, onOpen, onSuggest) else FreeRow(line, onClick = if (editable && line.categoryId != null) ({ onOpen(line) }) else null)
            HorizontalDivider(color = colors.divider)
        }
    }
}

/** Строка с планом: название, «факт из плана», полоска, подпись по темпу. */
@Composable
private fun BudgetRow(line: BudgetLine, budget: MonthBudget, total: Boolean = false, onClick: (() -> Unit)?) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val plan = line.budget ?: return
    val income = line.kind == OperationKind.INCOME
    val pace = BudgetRules.pace(plan, line.fact, budget.month, budget.today)
    val color = if (income) colors.income else colors.pace(pace)
    val (note, noteColor) = when {
        income && line.fact > plan -> "сверх плана ${Money.format(line.fact - plan)}" to colors.income
        income -> "ещё ${Money.format(plan - line.fact)}" to colors.textSecondary
        else -> paceText(pace, plan, line.fact) to if (pace == BudgetPace.OK || pace == BudgetPace.DONE) colors.textSecondary else colors.pace(pace)
    }
    Column(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = spacing.xs).testTag(if (total) "$TAG_BUDGET_ROW:total:${line.kind.code}" else "$TAG_BUDGET_ROW:${line.categoryId}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineName(line, total, Modifier.weight(1f))
            Text(Money.amount(line.fact), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(" из ${Money.format(plan)}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        }
        BudgetProgress(line.fact.toFloat() / plan, color, if (income) null else BudgetRules.todayShare(budget.month, budget.today))
        Text(note, Modifier.align(Alignment.End).testTag("$TAG_BUDGET_NOTE:${line.categoryId ?: "total"}"), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = noteColor)
    }
}

/** Категория без плана: справа факт месяца или «—»; нажатие задаёт бюджет. */
@Composable
private fun FreeRow(line: BudgetLine, onClick: (() -> Unit)?) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = AppTheme.spacing.sm).testTag("$TAG_BUDGET_ROW:${line.categoryId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineName(line, false, Modifier.weight(1f), muted = line.fact == 0L)
        Text(if (line.fact > 0) Money.format(line.fact) else "—", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
    }
}

/** Подготовка следующего месяца: сумма плана, под ней чип «в среднем N» — нажатие подставляет. */
@Composable
private fun PlanRow(line: BudgetLine, onOpen: (BudgetLine) -> Unit, onSuggest: (BudgetLine) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(line) }.padding(vertical = spacing.xs).testTag("$TAG_BUDGET_ROW:${line.categoryId}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineName(line, false, Modifier.weight(1f))
            Text(line.budget?.let(Money::format) ?: "—", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (line.budget == null) colors.inactive else colors.textPrimary)
        }
        line.suggestion?.let { suggestion ->
            val same = suggestion.amount == line.budget
            Text(
                if (same) "как в среднем" else "${suggestionLabel(suggestion)} ${Money.amount(suggestion.amount)}",
                Modifier.align(Alignment.End).padding(top = spacing.xxs).clip(AppTheme.shapes.medium).background(colors.accentSoft)
                    .clickable(enabled = !same, role = Role.Button) { onSuggest(line) }
                    .padding(horizontal = spacing.xs, vertical = spacing.xxs).testTag("$TAG_BUDGET_SUGGEST:${line.categoryId}"),
                style = MaterialTheme.typography.labelSmall, color = if (same) colors.inactive else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun LineName(line: BudgetLine, total: Boolean, modifier: Modifier, muted: Boolean = false) {
    val colors = AppTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (!total) Text(line.emoji ?: "", Modifier.width(AppTheme.sizes.moduleIcon + AppTheme.spacing.xs), style = MaterialTheme.typography.titleMedium)
        Text(line.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (total) FontWeight.Bold else FontWeight.SemiBold,
            color = if (muted) colors.inactive else colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Правка бюджета категории: сумма, потрачено, чип «в среднем» подставляет; пустое поле снимает бюджет. */
@Composable
private fun BudgetEditDialog(line: BudgetLine, month: YearMonth, budget: MonthBudget, onIntent: (FinanceIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val id = line.categoryId ?: return
    var input by rememberSaveable(id, month.toString()) { mutableStateOf(line.budget?.let(Money::input).orEmpty()) }
    val value = Money.parse(input)
    val title = listOfNotNull(line.emoji, line.name).joinToString(" ") + " · ${financeMonthLabel(month)}"
    FinanceWindow(title, TAG_BUDGET_EDIT, { onIntent(FinanceIntent.EditBudget(null)) }, actions = {
        Button(
            enabled = input.isBlank() || (value != null && value > 0),
            onClick = { onIntent(FinanceIntent.SaveBudget(id, if (input.isBlank()) 0 else value ?: 0)) },
            modifier = Modifier.fillMaxWidth().testTag(TAG_BUDGET_SAVE),
        ) { Text("Сохранить") }
    }) {
        OutlinedTextField(
            input, { input = Money.filterInput(it) }, Modifier.fillMaxWidth().testTag(TAG_BUDGET_FIELD),
            label = { Text(if (line.kind == OperationKind.INCOME) "План на месяц, ₽" else "Бюджет на месяц, ₽") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        val factLabel = if (line.kind == OperationKind.INCOME) "Получено" else "Потрачено"
        val left = if (YearMonth.from(budget.today) == month) " · осталось дней: ${month.lengthOfMonth() - budget.today.dayOfMonth + 1}" else ""
        Text("$factLabel ${Money.format(line.fact)}$left", Modifier.padding(top = spacing.sm), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        line.suggestion?.let { suggestion ->
            Text(
                "${suggestionLabel(suggestion).replaceFirstChar { it.uppercase() }} ${Money.format(suggestion.amount)}",
                Modifier.padding(vertical = spacing.sm).clip(AppTheme.shapes.large).background(colors.accentSoft)
                    .clickable(role = Role.Button) { input = Money.input(suggestion.amount) }
                    .padding(horizontal = spacing.md, vertical = spacing.xs).testTag(TAG_BUDGET_AVERAGE)
                    .semantics { contentDescription = "Подставить ${Money.format(suggestion.amount)}" },
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary,
            )
        }
        Text("Пустое поле — без бюджета.", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
    }
}

/** «в среднем» — по полным месяцам; «по прогнозу» — пока полных месяцев учёта нет. */
private fun suggestionLabel(suggestion: BudgetSuggestion) = if (suggestion.months == 0) "по прогнозу" else "в среднем"

private val genitive = DateTimeFormatter.ofPattern("LLLL", Locale.forLanguageTag("ru"))
private val GENITIVE = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")

/** «ноября» — для «Копия ноября». */
internal fun monthGenitive(month: YearMonth): String = GENITIVE[month.monthValue - 1]

/** «декабрь» — для «Бюджет на декабрь». */
internal fun monthNominative(month: YearMonth): String = month.format(genitive)

/**
 * Подсказка в окне расхода (#327, §5 ТЗ): «Продукты · останется 2 000 ₽ из 20 000» или «превысит бюджет на 600 ₽».
 * null — не расход, категория без бюджета. Вторая часть — превышение (красный). Прежняя сумма правки не учитывается дважды.
 */
internal fun budgetHint(state: FinanceState, form: OperationForm, categoryName: String): Pair<String, Boolean>? {
    if (form.kind != OperationKind.EXPENSE) return null
    val categoryId = form.categoryId ?: return null
    val month = YearMonth.from(form.date)
    val budget = state.budgetsOf(month)[categoryId] ?: return null
    val spent = state.operations.orEmpty().filter {
        it.kind == OperationKind.EXPENSE && it.categoryId == categoryId && YearMonth.from(it.date) == month && it.id != form.editingId
    }.sumOf { it.amount }
    val amount = form.kopecks ?: 0
    val after = spent + amount
    val plan = Money.amount(budget)
    return when {
        after > budget -> "$categoryName · превысит бюджет на ${Money.format(after - budget)}" to true
        amount == 0L -> "$categoryName · осталось ${Money.format(budget - after)} из $plan" to false
        else -> "$categoryName · останется ${Money.format(budget - after)} из $plan" to false
    }
}

/** Внутри толстой полосы потраченное пишется белым, если заполнено не меньше этой доли; иначе — тёмным по серому. */
private const val LIMIT_TEXT_INSIDE = 0.3f

const val TAG_LIMIT_BAR = "finance_limit_bar"
const val TAG_LIMIT_STATUS = "finance_limit_status"
const val TAG_BUDGET_TODAY = "budget_today"
const val TAG_BUDGET_WINDOW = "budget_window"
const val TAG_BUDGET_ROW = "budget_row"
const val TAG_BUDGET_NOTE = "budget_note"
const val TAG_BUDGET_SUGGEST = "budget_suggest"
const val TAG_BUDGET_FILL = "budget_fill"
const val TAG_BUDGET_ALL_AVERAGE = "budget_all_average"
const val TAG_BUDGET_CONFIRM = "budget_confirm"
const val TAG_BUDGET_EDIT = "budget_edit"
const val TAG_BUDGET_FIELD = "budget_field"
const val TAG_BUDGET_AVERAGE = "budget_average"
const val TAG_BUDGET_SAVE = "budget_save"
