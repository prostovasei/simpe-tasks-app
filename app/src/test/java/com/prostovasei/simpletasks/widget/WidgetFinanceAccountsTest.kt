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
}
