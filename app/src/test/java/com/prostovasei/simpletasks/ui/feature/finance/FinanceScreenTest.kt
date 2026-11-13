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
import com.prostovasei.simpletasks.testing.place
import com.prostovasei.simpletasks.ui.feature.task.TAG_DIALOG_DONE
import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_DIALOG
import com.prostovasei.simpletasks.ui.feature.project.TAG_EMOJI_FIELD
import com.prostovasei.simpletasks.domain.model.finance.FinanceCounts
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
        show(FinanceState(accounts = three, operations = emptyList(), today = today))

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
        show(FinanceState(accounts = many, operations = emptyList(), today = today), dark = true)

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

        // #237: без заголовка «Новая операция» и кнопки «×»; «назад» и «✓» — значки внизу.
        composeRule.onNodeWithText("Новая операция").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_OPERATION_WINDOW:$TAG_WINDOW_CLOSE").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_AMOUNT).assertTextEquals("850${s}₽")
        composeRule.onNodeWithTag(TAG_OPERATION_ACCOUNT).assertContentDescriptionEquals("Счёт: 💳 Карта")
        composeRule.onNodeWithTag(TAG_OPERATION_DATE).assertContentDescriptionEquals("Дата: вт. 10 ноя 2026")
        composeRule.onNodeWithText("сегодня").assertExists()
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).assertContentDescriptionEquals("Место: Место платежа")
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c2").assertIsSelected()
        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_NAME).assertTextEquals("Кафе")
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c6").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:old").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_TRASH).assertDoesNotExist()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_expense_light.png")

        // Категория: другая — выбрана, повторное нажатие снимает выбор.
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c1").performClick()
        assertEquals("c1", form.categoryId)
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c1").performClick()
        assertEquals(null, form.categoryId)

        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_NAME).assertTextEquals("Без категории")

        // Место: нажатие открывает выбор, подсказки из ранее введённых по набранному тексту; нажатие на место подставляет его.
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).performClick()
        composeRule.onNodeWithTag(TAG_PLACE_INPUT).performTextInput("пе")
        composeRule.onNodeWithTag("$TAG_PLACE_HINT:Озон").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_PLACE_HINT:Перекрёсток").performClick()
        assertEquals("Перекрёсток", form.place)
        composeRule.onNodeWithTag(TAG_PLACE_DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).assertContentDescriptionEquals("Место: Перекрёсток")
        composeRule.onNodeWithTag(TAG_OPERATION_COMMENT).performTextInput("хлеб")
        assertEquals("хлеб", form.comment)

        composeRule.onNodeWithTag(TAG_OPERATION_ACCOUNT).performClick()
        composeRule.onNodeWithTag("$TAG_OPERATION_ACCOUNT:a2").performClick()
        assertEquals("a2", form.accountId)

        intents.clear()
        composeRule.onNodeWithTag("$TAG_OPERATION_KIND:INCOME").performClick()
        composeRule.onNodeWithTag(TAG_OPERATION_SAVE).performClick()
        composeRule.onNodeWithTag(TAG_OPERATION_BACK).performClick()
        assertEquals(listOf(FinanceIntent.SetKind(OperationKind.INCOME), FinanceIntent.SaveForm, FinanceIntent.CloseForm), intents)
    }

    /** Выбор места (#237): набранное — по «✓»; «назад» закрывает, ничего не меняя; введённое место видно в поле и сохраняется как есть. */
    @Test
    fun placePickerAppliesTypedTextOrCancels() {
        var form by mutableStateOf(OperationForm(OperationKind.EXPENSE, "a1", today, amount = "850", place = "Озон"))
        composeRule.setContent {
            AppTheme {
                OperationDialog(full(form), form) { intent ->
                    intents += intent
                    if (intent is FinanceIntent.EditForm) form = intent.change(form)
                }
            }
        }
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).assertContentDescriptionEquals("Место: Озон")
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).performClick()
        composeRule.onNodeWithTag(TAG_PLACE_INPUT).performTextReplacement("Новое место ")
        composeRule.onNode(isDialog().and(androidx.compose.ui.test.hasTestTag(TAG_PLACE_DIALOG))).captureRoboImage("screenshots/finance_place_picker_light.png")
        composeRule.onNodeWithTag(TAG_PLACE_BACK).performClick()
        assertEquals("Озон", form.place)

        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).performClick()
        composeRule.onNodeWithTag(TAG_PLACE_INPUT).performTextReplacement("Новое место ")
        composeRule.onNodeWithTag(TAG_PLACE_DONE).performClick()
        assertEquals("Новое место", form.place)
        composeRule.onNodeWithTag(TAG_OPERATION_PLACE).performClick()
        composeRule.onNodeWithTag(TAG_PLACE_INPUT).performTextReplacement("")
        composeRule.onNodeWithTag(TAG_PLACE_DONE).performClick()
        assertEquals("", form.place)
    }

    /** Категории иконками (#237): эмодзи на плитке, у категории без эмодзи — первая буква; название выбранной — под рядом. */
    @Test
    fun categoryTilesShowEmojiOrFirstLetter() {
        val withEmoji = cats.map { if (it.id == "c1") it.copy(emoji = "🛒") else it }
        val form = OperationForm(OperationKind.EXPENSE, "a1", today, amount = "850", categoryId = "c1")
        composeRule.setContent { AppTheme { OperationDialog(full(form).copy(categories = withEmoji), form) {} } }
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:c1").assertContentDescriptionEquals("Продукты")
        composeRule.onNodeWithText("🛒").assertExists()
        composeRule.onNodeWithText("К").assertExists()
        composeRule.onNodeWithTag(TAG_OPERATION_CATEGORY_NAME).assertTextEquals("Продукты")
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_emoji_light.png")
    }

    /** Формат даты окна (#237): «ср. 15 окт 2026». */
    @Test
    fun shortDateFormat() {
        assertEquals("ср. 15 окт 2025", shortDate(LocalDate.parse("2025-10-15")))
        assertEquals("чт. 15 окт 2026", shortDate(LocalDate.parse("2026-10-15")))
        assertEquals("пт. 13 ноя 2026", shortDate(LocalDate.parse("2026-11-13")))
        assertEquals("вс. 3 мая 2026", shortDate(LocalDate.parse("2026-05-03")))
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

        composeRule.onNodeWithTag("$TAG_OPERATION_KIND:INCOME").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_OPERATION_DATE).assertContentDescriptionEquals("Дата: пн. 9 ноя 2026")
        composeRule.onNodeWithText("вчера").assertExists()
        // Архивная категория операции остаётся видна и выбрана.
        composeRule.onNodeWithTag("$TAG_OPERATION_CATEGORY:old").assertIsSelected()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_operation_edit_light.png")
        composeRule.onNodeWithTag(TAG_OPERATION_TRASH).performClick()
        composeRule.onNodeWithTag(TAG_OPERATION_BACK).performClick()
        assertEquals(listOf(FinanceIntent.TrashOperation, FinanceIntent.CloseForm), intents)
    }

    private val spend = listOf(
        operation("n1", 2_100_000, "2026-11-03", category = "Продукты"),
        operation("n2", 1_000_000, "2026-11-10", category = "Кафе"),
        operation("n3", 680_000, "2026-11-05", category = "Подарки"),
        operation("n4", 500_000, "2026-11-01"),
        operation("n5", 6_500_000, "2026-11-02", OperationKind.INCOME, category = "Зарплата"),
        operation("o1", 1_800_000, "2026-10-10", category = "Продукты"),
        operation("o2", 1_100_000, "2026-10-02", category = "Кафе"),
        operation("o3", 400_000, "2026-10-01"),
        operation("o4", 620_000, "2026-10-09", category = "Транспорт"),
    )

    /** Аналитика текущего месяца: итог, разница к тем же дням октября, категории с долями; «в октябре не было» вместо процента. */
    @Test
    fun analyticsShowsMonthTotalDeltaAndCategories() {
        show(FinanceState(accounts = three, operations = spend, today = today))

        composeRule.onNodeWithTag(TAG_MONTH_TITLE).assertTextEquals("Ноябрь 2026")
        composeRule.onNodeWithText("Расходы · 1–10 ноября").assertExists()
        composeRule.onNodeWithTag(TAG_ANALYTICS_TOTAL).assertTextEquals("42${s}800${s}₽")
        composeRule.onNodeWithTag(TAG_ANALYTICS_DELTA).assertTextEquals("+3${s}600${s}₽ · +9,2%")
        composeRule.onNodeWithTag(TAG_ANALYTICS_COMPARE).assertTextEquals("К 1–10 октября · было 39${s}200${s}₽")
        composeRule.onNodeWithText("+3${s}000${s}₽ · +16,7%").assertExists()
        composeRule.onNodeWithText("−1${s}000${s}₽ · −9,1%").assertExists()
        composeRule.onNodeWithText("+6${s}800${s}₽ · в октябре не было").assertExists()
        composeRule.onNodeWithText("49%").assertExists()
        composeRule.onNodeWithTag("$TAG_ANALYTICS_CATEGORY:none").assertExists()
        composeRule.onNodeWithText("Транспорт").assertDoesNotExist()
        composeRule.onNodeWithText("Зарплата").assertDoesNotExist()

        composeRule.onNodeWithTag(TAG_MONTH_NEXT).performClick()
        composeRule.onNodeWithTag(TAG_MONTH_PREV).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.ShiftMonth(-1)), intents)
        composeRule.onRoot().captureRoboImage("screenshots/finance_analytics_light.png")
    }

    /** Завершённый месяц — «за месяц» и сравнение с целым прошлым месяцем, где расходов не было. */
    @Test
    fun finishedMonthComparesWithWholePreviousMonth() {
        show(FinanceState(accounts = three, operations = spend, today = today, month = java.time.YearMonth.of(2026, 10)), dark = true)

        composeRule.onNodeWithTag(TAG_MONTH_TITLE).assertTextEquals("Октябрь 2026")
        composeRule.onNodeWithText("Расходы · за месяц").assertExists()
        composeRule.onNodeWithTag(TAG_ANALYTICS_DELTA).assertTextEquals("+39${s}200${s}₽")
        composeRule.onNodeWithTag(TAG_ANALYTICS_COMPARE).assertTextEquals("К сентябрю · расходов не было")
        composeRule.onNodeWithText("+18${s}000${s}₽ · в сентябре не было").assertExists()
        composeRule.onNodeWithTag(TAG_MONTH_NEXT).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.ShiftMonth(1)), intents)
        composeRule.onRoot().captureRoboImage("screenshots/finance_analytics_month_dark.png")
    }

    /** Месяц без расходов: итог 0 ₽ с разницей к прошлому, вместо списка — строка; без расходов вообще — без плашки разницы. */
    @Test
    fun monthWithoutExpensesShowsNote() {
        show(FinanceState(accounts = three, operations = spend.filter { it.date.monthValue == 10 }, today = today))

        composeRule.onNodeWithTag(TAG_ANALYTICS_TOTAL).assertTextEquals("0${s}₽")
        composeRule.onNodeWithTag(TAG_ANALYTICS_DELTA).assertTextEquals("−39${s}200${s}₽ · −100,0%")
        composeRule.onNodeWithText("В этом месяце расходов пока нет.").assertExists()
        composeRule.onNodeWithText("По категориям").assertDoesNotExist()
        composeRule.onRoot().captureRoboImage("screenshots/finance_analytics_empty_light.png")
    }

    @Test
    fun noOperationsAtAllHidesDelta() {
        show(FinanceState(accounts = three, operations = emptyList(), today = today))
        composeRule.onNodeWithTag(TAG_ANALYTICS_TOTAL).assertTextEquals("0${s}₽")
        composeRule.onNodeWithTag(TAG_ANALYTICS_DELTA).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_ANALYTICS_COMPARE).assertDoesNotExist()
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
        composeRule.onNodeWithTag("$TAG_ARCHIVED_WINDOW:$TAG_ARCHIVED_RESTORE").performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY:c2").performClick()
        composeRule.onNodeWithText("Категория расходов").assertExists()
        composeRule.onNodeWithTag(TAG_CATEGORY_NAME).performTextReplacement("Кофе")
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY:c3").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_ARCHIVE).performClick()
        composeRule.onNodeWithTag("$TAG_ARCHIVE_CONFIRM:confirm").performClick()

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
                FinanceIntent.UpdateCategory("c2", "Кофе", null),
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
    fun editWindowShowsBalancesAndOffersReconcile() {
        var saved: AccountDraft? = null
        var closed = false
        var reconcile = false
        val account = account("a1", "Карта", 4_230_050, emoji = "🪙").copy(initialBalance = 3_000_000)
        composeRule.setContent { AppTheme { AccountDialog(account, onSave = { saved = it }, onDismiss = { closed = true }, onReconcile = { reconcile = true }) } }

        composeRule.onNodeWithText("Счёт").assertExists()
        composeRule.onNodeWithTag(TAG_ACCOUNT_NAME).assertTextEquals("Карта")
        // Этап 4: остаток в окне счёта не вводится — показан вместе с начальным, меняется сверкой.
        composeRule.onNodeWithTag(TAG_ACCOUNT_AMOUNT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_ACCOUNT_BALANCE).assertTextEquals("42${s}300,50${s}₽")
        composeRule.onNodeWithTag(TAG_ACCOUNT_INITIAL).assertTextEquals("30${s}000${s}₽ · 01.11.2026")
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_account_edit_light.png")
        composeRule.onNodeWithTag(TAG_ACCOUNT_NAME).performTextReplacement("Яндекс")
        composeRule.onNodeWithTag(TAG_ACCOUNT_SAVE).performClick()
        assertEquals(AccountDraft("Яндекс", AccountType.CARD, "🪙", 4_230_050), saved)

        composeRule.onNodeWithTag(TAG_ACCOUNT_RECONCILE).performClick()
        composeRule.onNodeWithTag("$TAG_ACCOUNT_WINDOW:$TAG_WINDOW_CLOSE").performClick()
        assertEquals(true to true, reconcile to closed)
    }

    private val card = account("a1", "Карта", 4_230_000).copy(initialBalance = 3_000_000)

    /** Сверка: три способа; «Найти операцию» и выбор окна уходят намерениями. */
    @Test
    fun reconcileOffersThreeWays() {
        composeRule.setContent { AppTheme { ReconcileDialogs(Reconcile.Choose(card), today) { intents += it } } }

        composeRule.onNodeWithText("Остаток не сходится").assertExists()
        composeRule.onNodeWithText("Карта · по приложению 42${s}300${s}₽").assertExists()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_reconcile_light.png")
        composeRule.onNodeWithTag(TAG_RECONCILE_FIND).performClick()
        composeRule.onNodeWithTag(TAG_RECONCILE_INITIAL).performClick()
        composeRule.onNodeWithTag(TAG_RECONCILE_REAL).performClick()
        assertEquals(listOf(FinanceIntent.FindOperation, FinanceIntent.PickReconcile(Reconcile.Initial(card)), FinanceIntent.PickReconcile(Reconcile.Real(card))), intents)
    }

    /** Реальный остаток: разница считается сама; без разницы «Сохранить» не нажимается. */
    @Test
    fun realBalanceShowsCorrection() {
        composeRule.setContent { AppTheme { ReconcileDialogs(Reconcile.Real(card), today) { intents += it } } }

        composeRule.onNodeWithTag(TAG_RECONCILE_AMOUNT).assertTextEquals("42300${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_RESULT).assertTextEquals("0${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_SAVE).performClick()
        assertTrue(intents.isEmpty())

        composeRule.onNodeWithTag(TAG_RECONCILE_AMOUNT).performTextReplacement("42450")
        composeRule.onNodeWithTag(TAG_RECONCILE_WAS).assertTextEquals("42${s}300${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_RESULT).assertTextEquals("+150${s}₽")
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_reconcile_real_light.png")
        composeRule.onNodeWithTag(TAG_RECONCILE_AMOUNT).performTextReplacement("42000")
        composeRule.onNodeWithTag(TAG_RECONCILE_RESULT).assertTextEquals("−300${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_SAVE).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.SaveRealBalance(4_200_000)), intents)
    }

    /** Начальный остаток: видно, каким станет остаток сейчас (сдвиг на ту же величину). */
    @Test
    fun initialBalanceShowsResultingBalance() {
        composeRule.setContent { AppTheme { ReconcileDialogs(Reconcile.Initial(card), today) { intents += it } } }

        composeRule.onNodeWithText("Сколько было на счёте «Карта» 01.11.2026").assertExists()
        composeRule.onNodeWithTag(TAG_RECONCILE_AMOUNT).assertTextEquals("30000${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_RESULT).assertTextEquals("42${s}300${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_AMOUNT).performTextReplacement("30150")
        composeRule.onNodeWithTag(TAG_RECONCILE_WAS).assertTextEquals("30${s}000${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_RESULT).assertTextEquals("42${s}450${s}₽")
        composeRule.onNodeWithTag(TAG_RECONCILE_SAVE).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.SaveInitialBalance(3_015_000)), intents)
    }

    /** Корректировка в списке операций — серым с подписью и своим знаком; её окно — только просмотр и корзина. */
    @Test
    fun correctionIsShownInListAndReadOnlyWindow() {
        val correction = operation("k1", -30_000, "2026-11-10", OperationKind.CORRECTION, account = "Наличные")
        show(full().copy(operations = listOf(correction) + ops))
        composeRule.onNodeWithText("Корректировка остатка").assertExists()
        composeRule.onNodeWithText("−300${s}₽").assertExists()
        composeRule.onNodeWithTag("$TAG_OPERATION:k1").performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.OpenOperation("k1")), intents)
    }

    @Test
    fun correctionWindowOnlyTrashes() {
        var trashed = false
        val correction = operation("k1", 15_000, "2026-11-09", OperationKind.CORRECTION, account = "Наличные")
        composeRule.setContent { AppTheme { CorrectionDialog(correction, today, onTrash = { trashed = true }, onDismiss = {}) } }
        composeRule.onNodeWithTag("$TAG_CORRECTION_WINDOW:amount").assertTextEquals("+150${s}₽")
        composeRule.onNodeWithTag("$TAG_CORRECTION_WINDOW:date").assertTextEquals("09.11.2026 · вчера")
        composeRule.onNodeWithTag(TAG_OPERATION_TRASH).performClick()
        assertEquals(true, trashed)
    }

    private val trashed = listOf(
        operation("t1", 62_000, "2026-11-09", category = "Кафе").copy(deletedOn = today),
        operation("t2", 500_000, "2026-11-07", OperationKind.TRANSFER, to = "Наличные").copy(deletedOn = today.minusDays(1)),
        operation("t3", 135_000, "2026-11-05", category = "Продукты").copy(deletedOn = LocalDate.parse("2026-11-06")),
    )

    /** Корзина: строки с днём операции и днём удаления; восстановление сразу, удаление и очистка — после подтверждения. */
    @Test
    fun trashRestoresAtOnceAndAsksBeforeDeleting() {
        var back = false
        composeRule.setContent { AppTheme { FinanceTrashScreen(trashed, today, { intents += it }, onBack = { back = true }) } }

        composeRule.onNodeWithText("9 ноября · удалено сегодня").assertExists()
        composeRule.onNodeWithText("7 ноября · удалено вчера").assertExists()
        composeRule.onNodeWithText("5 ноября · удалено 6 ноября").assertExists()
        composeRule.onNodeWithText("Карта → Наличные").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_trash_light.png")

        composeRule.onNodeWithTag("$TAG_TRASH_RESTORE:t2").performClick()
        composeRule.onNodeWithTag("$TAG_TRASH_DELETE:t1").performClick()
        composeRule.onNodeWithText("Удалить навсегда?").assertExists()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.RestoreOperation("t2")), intents)
        composeRule.onNodeWithTag("$TAG_TRASH_CONFIRM:cancel").performClick()
        composeRule.onNodeWithTag("$TAG_TRASH_DELETE:t1").performClick()
        composeRule.onNodeWithTag("$TAG_TRASH_CONFIRM:confirm").performClick()

        composeRule.onNodeWithTag(TAG_TRASH_CLEAR).performClick()
        composeRule.onNodeWithText("Операций в корзине: 3. Они будут удалены навсегда, восстановить их будет нельзя.").assertExists()
        composeRule.onNodeWithTag("$TAG_TRASH_CONFIRM:confirm").performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        assertEquals(listOf(FinanceIntent.RestoreOperation("t2"), FinanceIntent.DeleteForever("t1"), FinanceIntent.EmptyTrash), intents)
        assertEquals(true, back)
    }

    @Test
    fun emptyTrashHasNoClearButton() {
        composeRule.setContent { AppTheme { FinanceTrashScreen(emptyList(), today, {}, onBack = {}) } }
        composeRule.onNodeWithText("Корзина пуста").assertExists()
        composeRule.onNodeWithTag(TAG_TRASH_CLEAR).assertDoesNotExist()
    }

    /** Страница настроек (#239): вместо окна — экран со стрелкой назад; пункты с подписями-счётчиками, в том числе «Места». */
    @Test
    fun settingsPageLeadsToDirectoriesTrashBackupAndBank() {
        val clicks = mutableListOf<String>()
        val places = listOf(place("p1", "Пятёрочка"), place("p2", "Озон"), place("p3", "Кофейня", archived = true))
        val state = full().copy(trash = trashed, lastBackupAt = null, places = places)
        composeRule.setContent {
            AppTheme {
                FinanceSettingsScreen(state, onBack = { clicks += "back" }, onCategories = { clicks += "categories" }, onPlaces = { clicks += "places" }, onTrash = { clicks += "trash" }, onBackup = { clicks += "backup" }, onBank = { clicks += "bank" })
            }
        }
        composeRule.onNodeWithText("Настройки финансов").assertExists()
        composeRule.onNodeWithText("Действующих: 7").assertExists()
        composeRule.onNodeWithText("Действующих: 2").assertExists()
        composeRule.onNodeWithText("Операций: 3").assertExists()
        composeRule.onNodeWithText("Копия ещё не сохранялась").assertExists()
        composeRule.onNodeWithText("Выключено").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_settings_light.png")
        listOf(TAG_MENU_CATEGORIES, TAG_MENU_PLACES, TAG_MENU_TRASH, TAG_MENU_BACKUP, TAG_MENU_BANK).forEach { composeRule.onNodeWithTag(it).performClick() }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        assertEquals(listOf("categories", "places", "trash", "backup", "bank", "back"), clicks)
    }

    /** Окна копии: действия и подтверждение замены с содержимым копии и текущими данными. */
    @Test
    fun backupWindowsSaveRestoreAndConfirm() {
        val clicks = mutableListOf<String>()
        var confirm by mutableStateOf(false)
        composeRule.setContent {
            AppTheme {
                if (!confirm) {
                    BackupDialog(lastBackupAt = null, onSave = { clicks += "save" }, onRestore = { clicks += "restore" }, onDismiss = {})
                } else {
                    RestoreConfirmDialog(FinanceCounts(3, 13, 214), FinanceCounts(3, 13, 221), onConfirm = { clicks += "replace" }, onDismiss = { clicks += "cancel" })
                }
            }
        }
        composeRule.onNodeWithTag(TAG_BACKUP_LAST).assertTextEquals("Копия ещё не сохранялась")
        composeRule.onNodeWithTag(TAG_BACKUP_SAVE).performClick()
        composeRule.onNodeWithTag(TAG_BACKUP_RESTORE).performClick()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_backup_light.png")

        confirm = true
        composeRule.onNodeWithText("счетов 3 · категорий 13 · операций 214").assertExists()
        composeRule.onNodeWithText("счетов 3 · категорий 13 · операций 221").assertExists()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/finance_backup_confirm_light.png")
        composeRule.onNodeWithTag("$TAG_RESTORE_WINDOW:cancel").performClick()
        composeRule.onNodeWithTag("$TAG_RESTORE_WINDOW:confirm").performClick()
        assertEquals(listOf("save", "restore", "cancel", "replace"), clicks)
    }

    // ---------- Уведомления банка (#233; макеты design/approved/233/screens.html) ----------

    private val purchases = listOf(
        com.prostovasei.simpletasks.domain.model.finance.PendingPurchase("p1", 68_000, "a1", "Карта", java.time.Instant.parse("2026-11-10T11:32:00Z"), today),
        com.prostovasei.simpletasks.domain.model.finance.PendingPurchase(
            "p2", 124_000, "a1", "Карта", java.time.Instant.parse("2026-11-10T09:05:00Z"), today,
            com.prostovasei.simpletasks.domain.model.finance.DuplicateHint("Продукты", 124_000, today),
        ),
        com.prostovasei.simpletasks.domain.model.finance.PendingPurchase("p3", 35_000, "a1", "Карта", java.time.Instant.parse("2026-11-09T16:47:00Z"), today.minusDays(1)),
    )

    private fun bankState(enabled: Boolean, access: Boolean, account: String?) =
        full().copy(bank = com.prostovasei.simpletasks.domain.model.finance.BankSettings(enabled, account), bankAccess = access)

    private fun showBank(state: FinanceState, onAccess: () -> Unit = {}) {
        composeRule.setContent { AppTheme { BankSettingsScreen(state, { intents += it }, onBack = {}, onGiveAccess = onAccess) } }
    }

    /** Макет 1.2: первая настройка — выключатель недоступен, пока нет доступа и счёта. */
    @Test
    fun bankSettingsFirstRun() {
        var asked = false
        showBank(bankState(enabled = false, access = false, account = null)) { asked = true }
        composeRule.onNodeWithText("Распознавать покупки").assertExists()
        composeRule.onNodeWithText("Выбрать").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_bank_off_light.png")
        composeRule.onNodeWithTag(TAG_BANK_ACCESS).performClick()
        assertTrue(asked)
        composeRule.onNodeWithTag(TAG_BANK_ACCOUNT).performClick()
        composeRule.onNodeWithTag("$TAG_BANK_ACCOUNT:a2").performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.SetBankAccount("a2")), intents)
    }

    /** Макет 1.3: включено — доступ есть, счёт выбран. */
    @Test
    fun bankSettingsOn() {
        showBank(bankState(enabled = true, access = true, account = "a1"))
        composeRule.onNodeWithTag(TAG_BANK_ACCESS_OK).assertExists()
        composeRule.onNodeWithText("💳 Карта").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_bank_on_light.png")
        composeRule.onNodeWithTag(TAG_BANK_SWITCH).performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.SetBankEnabled(false)), intents)
    }

    /** Макет 1.4: доступ отозван в Android — предупреждение, выключатель выключен. */
    @Test
    fun bankSettingsRevoked() {
        showBank(bankState(enabled = true, access = false, account = "a1"))
        composeRule.onNodeWithTag(TAG_BANK_REVOKED).assertExists()
        composeRule.onNodeWithTag(TAG_BANK_ACCESS).assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_bank_revoked_light.png")
    }

    /** Макет 2.2: «Не разобрано» над списком операций, только когда есть покупки. */
    @Test
    fun unsortedRowAboveOperations() {
        var opened = false
        composeRule.setContent {
            AppTheme {
                Column(Modifier.fillMaxSize()) {
                    ModuleBar(AppModule.FINANCE, onSelect = {}, onSettings = {})
                    Box(Modifier.weight(1f)) { FinanceScreen(full().copy(pending = purchases), onOpenUnsorted = { opened = true }) { intents += it } }
                    FinanceBottomBar(FinanceTab.OPERATIONS, onSelect = {}, onAdd = {})
                }
            }
        }
        composeRule.onNodeWithTag(TAG_UNSORTED_ROW).assertExists()
        composeRule.onNodeWithText("3").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/finance_operations_unsorted_light.png")
        composeRule.onNodeWithTag(TAG_UNSORTED_ROW).performClick()
        assertTrue(opened)
    }

    @Test
    fun noUnsortedRowWithoutPurchases() {
        show(full())
        composeRule.onNodeWithTag(TAG_UNSORTED_ROW).assertDoesNotExist()
    }

    /** Макет 2.3: покупки, пометка дубля и три действия. */
    @Test
    fun unsortedScreenAnswersPurchases() {
        val edited = mutableListOf<String>()
        composeRule.setContent { AppTheme { UnsortedScreen(purchases, today, { intents += it }, onBack = {}, onEdit = { edited += it }, zone = java.time.ZoneId.of("Europe/Moscow")) } }
        composeRule.onNodeWithText("Яндекс Банк · Карта · сегодня, 14:32").assertExists()
        composeRule.onNodeWithText("Яндекс Банк · Карта · вчера, 19:47").assertExists()
        composeRule.onNodeWithTag("$TAG_PURCHASE_DUPLICATE:p2").assertTextEquals("Похоже, уже записано: «Продукты», −1${s}240${s}₽ сегодня")
        composeRule.onRoot().captureRoboImage("screenshots/finance_unsorted_light.png")
        composeRule.onNodeWithTag("$TAG_PURCHASE_RECORD:p1").performClick()
        composeRule.onNodeWithTag("$TAG_PURCHASE_SKIP:p3").performClick()
        composeRule.onNodeWithTag("$TAG_PURCHASE_EDIT:p2").performClick()
        assertEquals(listOf<FinanceIntent>(FinanceIntent.RecordPurchase("p1"), FinanceIntent.SkipPurchase("p3")), intents)
        assertEquals(listOf("p2"), edited)
    }

    /** Эмодзи категории (#237): выбор в окне правки, «Убрать» снимает значок; в списке — эмодзи или первая буква. */
    @Test
    fun categoryEmojiIsPickedAndRemoved() {
        val withEmoji = cats.map { if (it.id == "c2") it.copy(emoji = "☕") else it }
        composeRule.setContent { AppTheme { CategoriesScreen(withEmoji, { intents += it }, onBack = {}) } }
        composeRule.onNodeWithText("☕").assertExists()
        composeRule.onNodeWithTag("$TAG_CATEGORY:c1").assertTextEquals("П", "Продукты")
        composeRule.onRoot().captureRoboImage("screenshots/finance_categories_emoji_light.png")

        // Кафе: убрать значок.
        composeRule.onNodeWithTag("$TAG_CATEGORY:c2").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_EMOJI_CLEAR).performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()
        // Продукты: выбрать эмодзи.
        composeRule.onNodeWithTag("$TAG_CATEGORY:c1").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_EMOJI_CLEAR).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_CATEGORY_EMOJI).performClick()
        composeRule.onNodeWithTag(TAG_EMOJI_FIELD).performTextReplacement("🛒")
        composeRule.onNodeWithTag("$TAG_EMOJI_DIALOG:$TAG_DIALOG_DONE").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_SAVE).performClick()

        assertEquals(listOf<FinanceIntent>(FinanceIntent.UpdateCategory("c2", "Кафе", null), FinanceIntent.UpdateCategory("c1", "Продукты", "🛒")), intents)
    }

    /** Справочник мест (#239): по алфавиту, архив внизу; правка, архив с предупреждением, удаление навсегда только из архива. */
    @Test
    fun placesDirectoryArchivesAndDeletesWithWarnings() {
        val places = listOf(place("p1", "Пятёрочка", uses = 4), place("p2", "Азбука вкуса"), place("p3", "озон"), place("p4", "Кофейня у дома", archived = true, uses = 5))
        var back = false
        composeRule.setContent { AppTheme { PlacesScreen(places, { intents += it }, onBack = { back = true }) } }
        val order = composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag("$TAG_PLACE:p2").or(androidx.compose.ui.test.hasTestTag("$TAG_PLACE:p3")).or(androidx.compose.ui.test.hasTestTag("$TAG_PLACE:p1")))
            .fetchSemanticsNodes().sortedBy { it.boundsInRoot.top }.map { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }
        assertEquals(listOf("$TAG_PLACE:p2", "$TAG_PLACE:p3", "$TAG_PLACE:p1"), order)
        composeRule.onRoot().captureRoboImage("screenshots/finance_places_light.png")

        // Правка: переименование, затем «В архив» — с предупреждением.
        composeRule.onNodeWithTag("$TAG_PLACE:p1").performClick()
        composeRule.onNodeWithTag(TAG_PLACE_NAME).performTextReplacement("Пятёрочка у дома")
        composeRule.onNodeWithTag(TAG_PLACE_SAVE).performClick()
        composeRule.onNodeWithTag("$TAG_PLACE:p3").performClick()
        composeRule.onNodeWithTag(TAG_PLACE_ARCHIVE).performClick()
        composeRule.onNodeWithText("Перенести в архив?").assertExists()
        composeRule.onNodeWithTag("$TAG_ARCHIVE_CONFIRM:confirm").performClick()

        // Архивное: окно «вернуть / удалить навсегда», удаление — с числом операций и подтверждением.
        composeRule.onNodeWithTag("$TAG_PLACE:p4").performClick()
        composeRule.onNodeWithText("В архиве. Операций: 5.").assertExists()
        composeRule.onNodeWithTag("$TAG_ARCHIVED_WINDOW:$TAG_ARCHIVED_DELETE").performClick()
        composeRule.onNodeWithText("Место «Кофейня у дома» будет удалено. Операций, у которых место станет пустым: 5. Сами операции не удаляются. Это нельзя отменить.").assertExists()
        composeRule.onNode(isDialog().and(androidx.compose.ui.test.hasTestTag(TAG_DELETE_CONFIRM))).captureRoboImage("screenshots/finance_delete_confirm_light.png")
        composeRule.onNodeWithTag("$TAG_DELETE_CONFIRM:confirm").performClick()
        composeRule.onNodeWithTag(TAG_PLACE_ADD).performClick()
        composeRule.onNodeWithTag(TAG_PLACE_NAME).performTextInput("ВкусВилл")
        composeRule.onNodeWithTag(TAG_PLACE_SAVE).performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertEquals(
            listOf(FinanceIntent.RenamePlace("p1", "Пятёрочка у дома"), FinanceIntent.ArchivePlace("p3", true), FinanceIntent.DeletePlace("p4"), FinanceIntent.CreatePlace("ВкусВилл")),
            intents,
        )
        assertTrue(back)
    }

    /** Категории (#239): архив — с предупреждением, архивная — вернуть или удалить навсегда; «Отмена» ничего не делает. */
    @Test
    fun categoryArchiveAndDeleteAskFirst() {
        val withUses = cats.map { if (it.id == "old") it.copy(uses = 14) else it }
        composeRule.setContent { AppTheme { CategoriesScreen(withUses, { intents += it }, onBack = {}) } }
        composeRule.onNodeWithTag("$TAG_CATEGORY:c2").performClick()
        composeRule.onNodeWithTag(TAG_CATEGORY_ARCHIVE).performClick()
        composeRule.onNodeWithText("Категория «Кафе» не будет предлагаться в новых операциях. В старых операциях останется. Вернуть можно из архива.").assertExists()
        composeRule.onNode(isDialog().and(androidx.compose.ui.test.hasTestTag(TAG_ARCHIVE_CONFIRM))).captureRoboImage("screenshots/finance_archive_confirm_light.png")
        composeRule.onNodeWithTag("$TAG_ARCHIVE_CONFIRM:cancel").performClick()
        assertTrue(intents.isEmpty())
        composeRule.onNodeWithTag(TAG_CATEGORY_ARCHIVE).performClick()
        composeRule.onNodeWithTag("$TAG_ARCHIVE_CONFIRM:confirm").performClick()

        composeRule.onNodeWithTag("$TAG_CATEGORY:old").performClick()
        composeRule.onNodeWithTag("$TAG_ARCHIVED_WINDOW:$TAG_ARCHIVED_RESTORE").performClick()
        composeRule.onNodeWithTag("$TAG_CATEGORY:old").performClick()
        composeRule.onNodeWithTag("$TAG_ARCHIVED_WINDOW:$TAG_ARCHIVED_DELETE").performClick()
        composeRule.onNodeWithText("Категория «Такси» будет удалена. Операций, у которых категория станет «Без категории»: 14. Сами операции не удаляются. Это нельзя отменить.").assertExists()
        composeRule.onNodeWithTag("$TAG_DELETE_CONFIRM:confirm").performClick()

        assertEquals(
            listOf(FinanceIntent.ArchiveCategory("c2", true), FinanceIntent.ArchiveCategory("old", false), FinanceIntent.DeleteCategory("old")),
            intents,
        )
    }
}
