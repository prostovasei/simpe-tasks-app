package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.testing.account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Экран финансов, этап 1 (#221): первый запуск, счета, окно счёта, бары. Макеты — design/approved/217. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class FinanceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<FinanceIntent>()
    private val s = ' '
    private val three = listOf(
        account("a1", "Карта", 4_230_000),
        account("a2", "Наличные", 615_000, AccountType.CASH),
        account("a3", "Накопления", 8_000_000, AccountType.SAVINGS),
    )

    /** Экран в каркасе: бар модулей сверху, нижняя панель финансов снизу — как в приложении. */
    private fun show(state: FinanceState, dark: Boolean = false, extra: @Composable () -> Unit = {}) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize()) {
                    ModuleBar(AppModule.FINANCE, onSelect = {})
                    Box(Modifier.weight(1f)) { FinanceScreen(state) { intents += it } }
                    FinanceBottomBar(state.tab, onSelect = { intents += FinanceIntent.SelectTab(it) }, onAdd = { intents += FinanceIntent.Add })
                }
                extra()
            }
        }
    }

    @Test
    fun firstRunOffersToCreateAccount() {
        show(FinanceState(accounts = emptyList()))

        composeRule.onNodeWithText("Начнём со счёта").assertExists()
        composeRule.onNodeWithTag(TAG_FINANCE_CREATE).performClick()
        composeRule.onNodeWithTag(TAG_FINANCE_ADD).performClick()
        assertEquals(listOf(FinanceIntent.NewAccount, FinanceIntent.Add), intents)
        composeRule.onNodeWithTag("$TAG_MODULE:FINANCE").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_FINANCE_TAB:ANALYTICS").assertIsSelected()
        composeRule.onRoot().captureRoboImage("screenshots/finance_empty_light.png")
    }

    @Test
    fun nothingIsShownWhileAccountsLoad() {
        show(FinanceState(accounts = null))
        composeRule.onNodeWithTag(TAG_FINANCE_EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_ACCOUNTS).assertDoesNotExist()
    }

    @Test
    fun accountsShowBalancesTotalAndOpen() {
        show(FinanceState(accounts = three))

        composeRule.onNodeWithTag(TAG_ACCOUNTS_TOTAL).assertTextEquals(" · 128${s}450${s}₽")
        composeRule.onNodeWithText("42${s}300${s}₽").assertExists()
        composeRule.onNodeWithText("Накопления").assertExists()
        composeRule.onNodeWithTag("$TAG_ACCOUNT:a2").performClick()
        composeRule.onNodeWithTag(TAG_ACCOUNT_ADD).performClick()
        composeRule.onNodeWithTag("$TAG_FINANCE_TAB:OPERATIONS").performClick()
        assertEquals(listOf(FinanceIntent.OpenAccount("a2"), FinanceIntent.NewAccount, FinanceIntent.SelectTab(FinanceTab.OPERATIONS)), intents)
        composeRule.onRoot().captureRoboImage("screenshots/finance_accounts_light.png")
    }

    @Test
    fun manyAccountsScrollSideways() {
        val many = three + listOf(account("a4", "Т-Банк", 1_248_000), account("a5", "Вклад", 25_000_000, AccountType.SAVINGS), account("a6", "Конверт", 1_500_000, AccountType.CASH))
        show(FinanceState(accounts = many), dark = true)

        composeRule.onNodeWithTag(TAG_ACCOUNTS_TOTAL).assertTextEquals(" · 405${s}930${s}₽")
        composeRule.onNodeWithTag("$TAG_ACCOUNT:a6").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_accounts_many_dark.png")
    }

    @Test
    fun operationsTabSaysTheyComeLater() {
        show(FinanceState(accounts = three, tab = FinanceTab.OPERATIONS))
        composeRule.onNodeWithText("Операций пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_ACCOUNTS).assertDoesNotExist()
    }

    @Test
    fun newAccountWindowCollectsDraft() {
        var saved: AccountDraft? = null
        composeRule.setContent { AppTheme { AccountDialog(null, onSave = { saved = it }, onDismiss = {}) } }

        // Название не введено — берётся название типа; сумма с копейками.
        composeRule.onNodeWithTag(TAG_ACCOUNT_TYPE).performClick()
        composeRule.onNodeWithTag("$TAG_ACCOUNT_TYPE:CASH").performClick()
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).performTextInput("6150,5")
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).assertTextEquals("6150,5${s}₽")
        composeRule.waitForIdle()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_account_new_light.png")
        composeRule.onNodeWithTag(TAG_ACCOUNT_SAVE).performClick()
        assertEquals(AccountDraft("Наличные", AccountType.CASH, null, 615_050), saved)

        composeRule.onNodeWithTag(TAG_ACCOUNT_NAME).performTextReplacement("  Заначка ")
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).performTextReplacement("1 000 руб")
        composeRule.onNodeWithTag(TAG_ACCOUNT_SAVE).performClick()
        assertEquals(AccountDraft("Заначка", AccountType.CASH, null, 100_000), saved)
    }

    @Test
    fun editWindowShowsAccountAndCloses() {
        var saved: AccountDraft? = null
        var closed = false
        val account = account("a1", "Карта", 4_230_050, emoji = "🪙")
        composeRule.setContent { AppTheme { AccountDialog(account, onSave = { saved = it }, onDismiss = { closed = true }) } }

        composeRule.onNodeWithText("Счёт").assertExists()
        composeRule.onNodeWithTag(TAG_ACCOUNT_NAME).assertTextEquals("Карта")
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).assertTextEquals("42300,50${s}₽")
        // 0.58.1: сумма видна целиком — поле во всю ширину окна, а не по «естественному» размеру (на телефоне PM было пусто).
        val field = composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).fetchSemanticsNode().boundsInRoot
        val window = composeRule.onNodeWithTag(TAG_ACCOUNT_WINDOW).fetchSemanticsNode().boundsInRoot
        assertTrue("Поле суммы уже окна: $field", field.width > window.width / 2)
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_account_edit_light.png")
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_account_edit_light.png")
        composeRule.onNodeWithText("Это деньги на счёте сегодня. Доходом не считается.").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_ACCOUNT_SAVE).performClick()
        assertEquals(AccountDraft("Карта", AccountType.CARD, "🪙", 4_230_050), saved)

        composeRule.onNodeWithTag("$TAG_ACCOUNT_WINDOW:$TAG_WINDOW_CLOSE").performClick()
        assertEquals(true, closed)
    }
}
