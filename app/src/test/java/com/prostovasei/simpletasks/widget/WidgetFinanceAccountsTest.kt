package com.prostovasei.simpletasks.widget

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** #317: строка счетов виджета «Финансы» не размножается, когда лента доприменяет её к уже показанной строке. */
@RunWith(RobolectricTestRunner::class)
class WidgetFinanceAccountsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun account(id: String, emoji: String, rubles: Long) =
        Account(id, "Счёт $id", AccountType.entries.first(), emoji, 0, rubles * 100, Instant.EPOCH)

    @Test
    fun reappliedRowKeepsOneSetOfAccounts() {
        val accounts = listOf(account("a", "💳", 32_000), account("b", "📉", 12_000), account("c", "💰", 5_000), account("d", "💳", 1_000))
        val row = FinanceRows(context, 392, 600).accountRow(accounts, 4)
        val view = row.apply(context, FrameLayout(context))
        // Лента виджета переиспользует строку: те же команды применяются к уже показанной — как при каждом обновлении.
        repeat(3) { row.reapply(context, view) }
        val strip = view.findViewById<ViewGroup>(R.id.widget_finance_accounts)
        assertEquals("Счетов столько же, сколько есть", 4, strip.childCount)
        val first = (strip.getChildAt(0) as TextView).text.toString()
        assertTrue("С суммой, а не только эмодзи: $first", first.startsWith("💳") && first.contains("32"))
    }

    /** #327: полоса лимита виджета (вариант Г) — картинка полосы и «осталось» жёлтым, когда тратится быстрее плана. */
    @Test
    fun limitBarShowsRemainingByPace() {
        val today = java.time.LocalDate.of(2026, 11, 24)
        val month = java.time.YearMonth.from(today)
        val ops = listOf(
            com.prostovasei.simpletasks.domain.model.finance.Operation(
                "o", com.prostovasei.simpletasks.domain.model.finance.OperationKind.EXPENSE, 1_680_000, "a", "Карта", null, null, "c", "Продукты", null, null, today,
            ),
        )
        val model = com.prostovasei.simpletasks.domain.model.finance.MonthSpending.of(ops, month, today)
        val view = FinanceRows(context, 392, 600).limitViews(model, 2_000_000, month, today).apply(context, FrameLayout(context))
        val status = view.findViewById<TextView>(R.id.widget_finance_limit_status)
        assertEquals("осталось 3k ₽", status.text.toString())
        assertEquals(com.prostovasei.simpletasks.designsystem.tokens.ColorTokens.warning.light.toInt(), status.currentTextColor)
        val bar = view.findViewById<android.widget.ImageView>(R.id.widget_finance_limit_bar)
        assertTrue("Полоса нарисована", (bar.drawable as android.graphics.drawable.BitmapDrawable).bitmap.width > 0)
    }
}
