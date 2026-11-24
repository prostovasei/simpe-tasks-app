package com.prostovasei.simpletasks.domain.model.finance

import com.prostovasei.simpletasks.notifications.BudgetAlerts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Правила бюджета #327: темп, перенос месяцев, подсказка «в среднем», окно месяца, время уведомления. */
class BudgetTest {
    private val nov = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 24)

    private fun op(id: String, kind: OperationKind, amount: Long, category: String?, date: LocalDate) =
        Operation(id, kind, amount, "a", "Карта", null, null, category, category, null, null, date)

    @Test
    fun paceFollowsPlanToToday() {
        // 24 ноября: план к сегодня — 80% бюджета.
        assertEquals(1_600_000L, BudgetRules.planToDate(2_000_000, nov, today))
        assertEquals(BudgetPace.OK, BudgetRules.pace(2_000_000, 1_500_000, nov, today))
        assertEquals(BudgetPace.AHEAD, BudgetRules.pace(2_000_000, 1_680_000, nov, today))
        assertEquals(BudgetPace.DONE, BudgetRules.pace(2_500_000, 2_500_000, nov, today))
        assertEquals(BudgetPace.OVER, BudgetRules.pace(600_000, 680_000, nov, today))
        // Прошлый месяц: темпа нет — только перерасход.
        assertEquals(BudgetPace.OK, BudgetRules.pace(2_000_000, 1_900_000, YearMonth.of(2026, 10), today))
        assertNull(BudgetRules.todayShare(YearMonth.of(2026, 10), today))
        assertEquals(84, BudgetRules.percent(2_000_000, 1_680_000))
    }

    @Test
    fun budgetsCarryToCurrentAndNextMonthButNotToPast() {
        val oct = YearMonth.of(2026, 10)
        val all = listOf(Budget(oct, "food", 2_000_000), Budget(oct, "cafe", 600_000), Budget(oct, "taxi", 0))
        assertEquals(mapOf("food" to 2_000_000L, "cafe" to 600_000L), BudgetRules.effective(all, nov, nov))
        assertEquals(mapOf("food" to 2_000_000L, "cafe" to 600_000L), BudgetRules.effective(all, nov.plusMonths(1), nov))
        assertEquals(emptyMap<String, Long>(), BudgetRules.effective(all, YearMonth.of(2026, 9), nov))
        // Заведённый месяц живёт своими строками: снятый бюджет не возвращается из прошлого.
        val set = all + Budget(nov, "food", 0)
        assertEquals(emptyMap<String, Long>(), BudgetRules.effective(set, nov, nov))
    }

    @Test
    fun suggestionUsesFullMonthsThenForecast() {
        val ops = listOf(
            op("1", OperationKind.EXPENSE, 1_000_000, "food", LocalDate.of(2026, 8, 10)), // месяц первой операции не 1-го — не считается
            op("2", OperationKind.EXPENSE, 2_000_000, "food", LocalDate.of(2026, 9, 3)),
            op("3", OperationKind.EXPENSE, 2_100_000, "food", LocalDate.of(2026, 10, 5)),
            op("4", OperationKind.EXPENSE, 500_000, "food", LocalDate.of(2026, 11, 5)),
        )
        // Полных месяцев два (сентябрь, октябрь): (20 000 + 21 000) / 2 = 20 500 ₽.
        assertEquals(BudgetSuggestion(2_050_000, 2), BudgetRules.suggestions(ops, today)["food"])
        // Учёт начат 10 ноября: полных месяцев нет — прогноз ноября 5 000 × 30 / 24 = 6 250 → вверх до 6 300 ₽.
        val fresh = listOf(op("5", OperationKind.EXPENSE, 500_000, "food", LocalDate.of(2026, 11, 10)))
        assertEquals(BudgetSuggestion(630_000, 0), BudgetRules.suggestions(fresh, today)["food"])
    }

    @Test
    fun monthBudgetSplitsPlannedAndFreeCategories() {
        val categories = listOf(
            Category("food", "Продукты", OperationKind.EXPENSE, false, "🛒"),
            Category("rent", "Аренда", OperationKind.EXPENSE, false, "🏠"),
            Category("gift", "Подарки", OperationKind.EXPENSE, false, "🎁"),
            Category("old", "Старое", OperationKind.EXPENSE, true),
            Category("pay", "Зарплата", OperationKind.INCOME, false, "💼"),
        )
        val ops = listOf(
            op("1", OperationKind.EXPENSE, 1_680_000, "food", LocalDate.of(2026, 11, 20)),
            op("2", OperationKind.EXPENSE, 2_500_000, "rent", LocalDate.of(2026, 11, 1)),
            op("3", OperationKind.EXPENSE, 240_000, "gift", LocalDate.of(2026, 11, 3)),
            op("4", OperationKind.EXPENSE, 100_000, null, LocalDate.of(2026, 11, 4)),
            op("5", OperationKind.EXPENSE, 900_000, "food", LocalDate.of(2026, 11, 28)), // после сегодня — не считается
            op("6", OperationKind.INCOME, 6_500_000, "pay", LocalDate.of(2026, 11, 10)),
        )
        val budget = MonthBudget.of(mapOf("food" to 2_000_000L, "rent" to 2_500_000L, "pay" to 8_000_000L), categories, ops, nov, today)
        assertEquals(listOf("rent", "food"), budget.expenses.map { it.categoryId })
        assertEquals(listOf("gift", null), budget.freeExpenses.map { it.categoryId })
        assertEquals(4_500_000L, budget.planOut)
        assertEquals(4_520_000L, budget.spent)
        assertEquals(8_000_000L, budget.planIn)
        assertEquals(6_500_000L, budget.received)
    }

    @Test
    fun nextMonthReminderIsSevenDaysBeforeTheFirstAt19() {
        assertEquals(LocalDateTime.of(2026, 11, 24, 19, 0), BudgetAlerts.nextMonthReminder(LocalDateTime.of(2026, 11, 24, 10, 0)))
        assertEquals(LocalDateTime.of(2026, 12, 25, 19, 0), BudgetAlerts.nextMonthReminder(LocalDateTime.of(2026, 11, 24, 19, 0)))
        assertEquals(LocalDateTime.of(2027, 2, 22, 19, 0), BudgetAlerts.nextMonthReminder(LocalDateTime.of(2027, 2, 1, 0, 0)))
    }
}
