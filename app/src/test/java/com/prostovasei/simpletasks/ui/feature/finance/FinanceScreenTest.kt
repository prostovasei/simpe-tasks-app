package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.testing.account
import com.prostovasei.simpletasks.testing.category
import com.prostovasei.simpletasks.testing.operation
import com.prostovasei.simpletasks.domain.model.finance.FormHints
import com.prostovasei.simpletasks.domain.model.finance.OperationKind
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import java.time.LocalDate
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
    private fun show(state: FinanceState, dark: Boolean = false, onSettings: (() -> Unit)? = null, extra: @Composable () -> Unit = {}) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize()) {
                    ModuleBar(AppModule.FINANCE, onSelect = {}, onSettings = onSettings)
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

    private val today = LocalDate.parse("2026-11-10")
    private val food = category("c1", "Продукты")
    private val cats = listOf(food, category("c2", "Кафе"), category("c3", "Транспорт"), category("c4", "Дом"), category("c5", "Здоровье"), category("c6", "Подарки"), category("i1", "Зарплата", OperationKind.INCOME), category("old", "Такси", archived = true))
    private val ops = listOf(
        operation("o1", 85_000, "2026-11-10", category = "Продукты"),
        operation("o2", 6_500_000, "2026-11-10", OperationKind.INCOME, category = "Зарплата"),
        operation("o3", 500_000, "2026-11-10", OperationKind.TRANSFER, to = "Наличные"),
        operation("o4", 62_000, "2026-11-09", category = "Кафе"),
        operation("o5", 120_000, "2026-11-09"),
        operation("o6", 240_000, "2025-12-30", category = "Дом"),
    )

    private fun full(form: OperationForm? = null, tab: FinanceTab = FinanceTab.OPERATIONS) =
        FinanceState(accounts = three, tab = tab, operations = ops, categories = cats, hints = FormHints("a1", listOf("c2"), listOf("Пятёрочка", "Перекрёсток", "Озон")), form = form, today = today)

    /** Список операций: дни, знаки сумм, перевод с подписью, «Без категории»; нажатие открывает правку. */
    @Test
    fun operationsAreGroupedByDayWithSigns() {
        show(full(), onSettings = { intents += FinanceIntent.CloseWindow })

        composeRule.onNodeWithText("10 ноября · сегодня").assertExists()
        composeRule.onNodeWithText("9 ноября · вчера").assertExists()
        composeRule.onNodeWithText("30 декабря 2025").assertExists()
        composeRule.onNodeWithText("− 850${s}₽").assertExists()
        composeRule.onNodeWithText("+ 65${s}000${s}₽").assertExists()
        composeRule.onNodeWithText("Карта → Наличные").assertExists()
        composeRule.onNodeWithText("5${s}000${s}₽").assertExists()
        composeRule.onNodeWithText("Без категории").assertExists()
        composeRule.onNodeWithTag("$TAG_OPERATION:o4").performClick()
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        assertEquals(listOf(FinanceIntent.OpenOperation("o4"), FinanceIntent.CloseWindow), intents)
        composeRule.onRoot().captureRoboImage("screenshots/finance_operations_light.png")
    }

    @Test
    fun emptyOperationsInviteToAdd() {
        show(FinanceState(accounts = three, tab = FinanceTab.OPERATIONS, operations = emptyList(), today = today))
        composeRule.onNodeWithText("Операций пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_ACCOUNTS).assertDoesNotExist()
    }

    /** 0.59.1 (#227): панели задач — бар модулей без плашки (активна галочка) и нижняя панель без плашек, «Списки» — линии без точек. */
    @Test
    fun taskBarsWithoutChips() {
        composeRule.setContent {
            AppTheme {
                Column(Modifier.fillMaxSize()) {
                    ModuleBar(AppModule.TASKS, onSelect = {})
                    Box(Modifier.weight(1f).fillMaxWidth().background(AppTheme.colors.background))
                    com.prostovasei.simpletasks.ui.navigation.BottomBar(selected = com.prostovasei.simpletasks.ui.navigation.Tab.LISTS, onSelect = {}, inboxCount = 3)
                }
            }
        }
        composeRule.onNodeWithTag("$TAG_MODULE:TASKS").assertIsSelected()
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).assertDoesNotExist()
        composeRule.onRoot().captureRoboImage("screenshots/task_bars_light.png")
    }

    /** 0.59.1 (#227): новое пустое окно расхода — снимок пустой суммы. */
    @Test
    fun emptyExpenseWindowShowsOneRubleSign() {
        val form = OperationForm(OperationKind.EXPENSE, "a1", today)
        composeRule.setContent { AppTheme { OperationDialog(full(form).copy(categories = emptyList()), form) {} } }
        composeRule.onNodeWithTag(TAG_OPERATION_AMOUNT).assertTextEquals("0${s}₽")
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_empty_light.png")
    }

    /** Окно расхода: тип, сумма, счёт, пять категорий (недавняя первой) и «Все категории», место, комментарий, дата. */
    @Test
    fun expenseWindowShowsFieldsAndSendsChanges() {
        var form by mutableStateOf(OperationForm(OperationKind.EXPENSE, "a1", today, amount = "850", categoryId = "c2"))
        composeRule.setContent {
            AppTheme {
                OperationDialog(full(form), form) { intent ->
                    intents += intent
                    if (intent is FinanceIntent.EditForm) form = intent.change(form)
                }
            }
        }

        composeRule.onNodeWithText("Новая операция").assertExists()
        composeRule.onNodeWithTag(TAG_OPERATION_AMOUNT).assertTextEquals("850${s}₽")
        composeRule.onNodeWithTag(TAG_OPERATION_ACCOUNT).assertContentDescriptionEquals("Счёт: 💳 Карта")
        composeRule.onNodeWithTag(TAG_OPERATION_DATE).assertContentDescriptionEquals("Дата: 10.11.2026 · сегодня")
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c2").assertIsSelected()
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c6").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:old").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_TRASH).assertDoesNotExist()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_expense_light.png")

        // Категория: другая — выбрана, повторное нажатие снимает выбор.
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c1").performClick()
        assertEquals("c1", form.categoryId)
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c1").performClick()
        assertEquals(null, form.categoryId)

        // Место: подсказки из ранее введённых по набранному тексту.
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).performTextInput("пе")
        composeRule.onNodeWithTag("$TAG_OPERATION_PLACE_HINTS:Озон").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_OPERATION_PLACE_HINTS:Перекрёсток").performClick()
        assertEquals("Перекрёсток", form.place)
        composeRule.onNodeWithTag(TAG_OPERATION_COMMENT).performTextInput("хлеб")
        assertEquals("хлеб", form.comment)

        composeRule.onNodeWithTag(TAG_OPERATION_ACCOUNT).performClick()
        composeRule.onNodeWithTag("$TAG_OPERATION_ACCOUNT:a2").performClick()
        assertEquals("a2", form.accountId)

        intents.clear()
        composeRule.onNodeWithTag("$TAG_OPERATION_KIND:INCOME").performClick()
        composeRule.onNodeWithTag(TAG_OPERATION_SAVE).performClick()
        composeRule.onNodeWithTag("$TAG_OPERATION_WINDOW:$TAG_WINDOW_CLOSE").performClick()
        assertEquals(listOf(FinanceIntent.SetKind(OperationKind.INCOME), FinanceIntent.SaveForm, FinanceIntent.CloseForm), intents)
    }

    /** «Все категории»: полный список без архивных, «Без категории» и создание новой прямо из операции. */
    @Test
    fun allCategoriesPickerSelectsAndCreates() {
        var form by mutableStateOf(OperationForm(OperationKind.EXPENSE, "a1", today, amount = "850"))
        composeRule.setContent {
            AppTheme {
                OperationDialog(full(form), form) { intent ->
                    intents += intent
                    if (intent is FinanceIntent.EditForm) form = intent.change(form)
                }
            }
        }

        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_ALL).performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY_PICKER:old").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_CATEGORY_PICKER:i1").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_CATEGORY_PICKER:none").assertExists()
        composeRule.onNodeWithTag("$TAG_CATEGORY_PICKER:c6").performClick()
        assertEquals("c6", form.categoryId)
        // Шестая категория не из первых пяти — показана первой кнопкой.
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c6").assertIsSelected()

        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_ALL).performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_PICKER_NEW).performClick()
        composeRule.onNodeWithText("Новая категория расходов").assertExists()
        composeRule.onNodeWithTag(TAG_CATEGORY_NAME).performTextInput("Одежда")
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        assertEquals(FinanceIntent.CreateCategory("Одежда", OperationKind.EXPENSE, select = true), intents.last())
    }

    /** Перевод: «Со счёта» и «На счёт» вместо категории и места. Без категорий — «Создать категорию». Нет суммы — «Сохранить» не нажимается. */
    @Test
    fun transferAndFirstOperationWindows() {
        var form by mutableStateOf(OperationForm(OperationKind.TRANSFER, "a1", today, amount = "5000", toAccountId = "a2"))
        var state by mutableStateOf(full(form))
        composeRule.setContent { AppTheme { OperationDialog(state.copy(form = form), form) { intent -> intents += intent } } }

        composeRule.onNodeWithTag(TAG_OPERATION_ACCOUNT).assertContentDescriptionEquals("Со счёта: 💳 Карта")
        composeRule.onNodeWithTag(TAG_OPERATION_TO).assertContentDescriptionEquals("На счёт: 👛 Наличные")
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORIES).assertDoesNotExist()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_transfer_light.png")
        composeRule.onNodeWithTag(TAG_OPERATION_TO).performClick()
        composeRule.onNodeWithTag("$TAG_OPERATION_TO:a1").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_OPERATION_TO:a3").assertExists()

        state = state.copy(categories = emptyList())
        form = OperationForm(OperationKind.EXPENSE, "a1", today)
        composeRule.onNodeWithText("Категорий пока нет. Операцию можно сохранить и без категории.").assertExists()
        intents.clear()
        composeRule.onNodeWithTag(TAG_OPERATION_SAVE).performClick()
        assertEquals(emptyList<FinanceIntent>(), intents)
        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_CREATE).performClick()
        composeRule.onNodeWithText("Новая категория расходов").assertExists()
    }

    /** Правка: заголовок — тип операции, переключателя типа нет, корзина в заголовке, дата «вчера». */
    @Test
    fun editWindowHasTrashAndNoKindSwitch() {
        val form = OperationForm(OperationKind.EXPENSE, "a1", today.minusDays(1), editingId = "o4", amount = "620", categoryId = "old", place = "Кофейня")
        composeRule.setContent { AppTheme { OperationDialog(full(form), form) { intents += it } } }

        composeRule.onNodeWithText("Расход").assertExists()
        composeRule.onNodeWithTag("$TAG_OPERATION_KIND:INCOME").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_DATE).assertContentDescriptionEquals("Дата: 09.11.2026 · вчера")
        // Архивная категория операции остаётся видна и выбрана.
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:old").assertIsSelected()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_edit_light.png")
        composeRule.onNodeWithTag(TAG_OPERATION_TRASH).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.TrashOperation), intents)
    }

    /** Экран «Категории»: два списка, архив внизу, правка названия и «В архив», возврат из архива. */
    @Test
    fun categoriesScreenManagesBothLists() {
        var back = false
        composeRule.setContent { AppTheme { CategoriesScreen(cats, { intents += it }, onBack = { back = true }) } }

        composeRule.onNodeWithText("Продукты").assertExists()
        composeRule.onNodeWithText("Зарплата").assertDoesNotExist()
        composeRule.onNodeWithText("Архив").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_categories_light.png")

        composeRule.onNodeWithTag("$TAG_CATEGORY:old").performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY:c2").performClick()
        composeRule.onNodeWithText("Категория расходов").assertExists()
        composeRule.onNodeWithTag(TAG_CATEGORY_NAME).performTextReplacement("Кофе")
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY:c3").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_ARCHIVE).performClick()

        composeRule.onNodeWithTag("$TAG_CATEGORY_KIND:INCOME").performClick()
        composeRule.onNodeWithText("Зарплата").assertExists()
        composeRule.onNodeWithText("Архив").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_CATEGORY_ADD).performClick()
        composeRule.onNodeWithText("Новая категория доходов").assertExists()
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_NAME).performTextInput(" Подарки ")
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertEquals(
            listOf(
                FinanceIntent.ArchiveCategory("old", archived = false),
                FinanceIntent.RenameCategory("c2", "Кофе"),
                FinanceIntent.ArchiveCategory("c3", archived = true),
                FinanceIntent.CreateCategory("Подарки", OperationKind.INCOME),
            ),
            intents,
        )
        assertEquals(true, back)
    }

    @Test
    fun newAccountWindowCollectsDraft() {
        var saved: AccountDraft? = null
        composeRule.setContent { AppTheme { AccountDialog(null, onSave = { saved = it }, onDismiss = {}) } }

        // 0.59.1 (#227): пустая сумма — бледный «0» и один «₽» в том же тексте, без отдельной надписи поверх.
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).assertTextEquals("0${s}₽")
        composeRule.onAllNodesWithText("0${s}₽", substring = true).assertCountEquals(1)
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
