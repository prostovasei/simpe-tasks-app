package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.performScrollToKey
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LISTS_SCROLL
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LISTS_SEARCH
import com.prostovasei.simpletasks.ui.feature.lists.TAG_LISTS_INBOX
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_SCRIM
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.designsystem.components.TAG_NAME_FIELD
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.feature.settings.TAG_APP_INFO
import com.prostovasei.simpletasks.ui.feature.settings.TAG_APP_SETTINGS
import com.prostovasei.simpletasks.ui.feature.settings.TAG_SETTINGS_APP
import com.prostovasei.simpletasks.ui.feature.settings.TAG_SETTINGS_FINANCE
import com.prostovasei.simpletasks.ui.feature.settings.TAG_SETTINGS_TASKS
import com.prostovasei.simpletasks.ui.navigation.TAG_APP_BAR
import com.prostovasei.simpletasks.ui.navigation.TAG_MODULE_SETTINGS
import com.prostovasei.simpletasks.ui.feature.lists.TAG_NEW_LIST
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_CLOSE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_DELETE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_MENU
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_TITLE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_SAVE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_TITLE
import com.prostovasei.simpletasks.ui.feature.search.TAG_SEARCH_FIELD
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_HEADER
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_MOVE
import com.prostovasei.simpletasks.designsystem.components.TAG_OVERDUE_TOGGLE
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.widget.widgetEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import com.prostovasei.simpletasks.ui.feature.lists.TAG_TRASH_COUNT
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_EMPTY
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_BANNER
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_MENU
import com.prostovasei.simpletasks.ui.feature.trash.TAG_TRASH_TASK_RESTORE
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_BAR
import com.prostovasei.simpletasks.designsystem.components.TAG_SELECTION_COUNT
import com.prostovasei.simpletasks.designsystem.components.TAG_ACTION_RIBBON
import com.prostovasei.simpletasks.ui.feature.bulk.TAG_BULK_DELETE
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Запуск настоящего приложения (Hilt, Room, навигация): вкладки открываются, список создаётся,
 * открывается и закрывается кнопкой «Назад». Ловит ошибки, которые видны только на телефоне.
 */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun walkThroughTabsAndCreateList() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        // #241: заголовка «Предстоящее» над лентой нет; названия страниц — только в нижней панели.
        composeRule.onNodeWithText("Предстоящее").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Предстоящее").assertExists()

        // Итерация 55: «Входящие» и «Поиск» — в «Списках» (настройки с #241 — из меню кнопки модуля).
        pickTask("LISTS")
        composeRule.onNodeWithTag(TAG_LISTS_INBOX).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё разобрано") }

        // «Неделя» — отдельная страница панели (#241: скрытого выбора удержанием больше нет).
        pickTask("WEEK")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("week-screen") }
        composeRule.onNodeWithTag("week-calendar").assertExists()

        // Поиск: поле ввода сразу на экране, ничего не найдено. «Списки» из поиска — назад к самим «Спискам».
        pickTask("LISTS")
        composeRule.onNodeWithTag(TAG_LISTS_SEARCH).performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("молоко")
        composeRule.onNodeWithText("Ничего не найдено").assertExists()

        // Единые настройки (#241): меню кнопки модуля → «Настройки» → «Задачи» → «Импорт из TickTick»; «Назад» — на главную страницу настроек.
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithTag("tab:LISTS").assertExists()
        composeRule.onNodeWithTag(TAG_SETTINGS_TASKS).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Импорт из TickTick") }
        composeRule.onNodeWithText("Обновление").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithTag(TAG_SETTINGS_APP).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_INFO) }
        composeRule.onNodeWithText("Обновление").assertExists()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("tab:LISTS") }

        // После настроек открыт поиск (откуда уходили): «Списки» ведут к самим «Спискам» — ждём, пока переход закончится.
        pickTask("LISTS")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_LISTS_SCROLL) && !composeRule.onAllNodesWithTagExists(TAG_SEARCH_FIELD) }
        composeRule.onNodeWithTag(TAG_LISTS_SCROLL).performScrollToNode(hasText("Списков пока нет"))
        composeRule.onNodeWithText("Списков пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_NEW_LIST).performClick()
        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextReplacement("Покупки")
        composeRule.onNodeWithText("Создать").performClick()

        // Новый список открывается сразу, нижний бар скрыт.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("В списке пусто") }
        composeRule.onNodeWithTag("tab:LISTS").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Назад").performClick()
        // «Списки» прокручиваются: новый список — под системными строками, на тестовом экране ниже края.
        composeRule.waitUntil(5_000) { runCatching { composeRule.onNodeWithTag(TAG_LISTS_SCROLL).performScrollToNode(hasText("Покупки")) }.isSuccess }
        // «Предстоящее» — своя страница панели.
        pickTask("UPCOMING")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
    }

    /** Финансы, этап 1 (#221): бар модулей, первый счёт и возврат в то же место при смене модуля. */
    @Test
    fun switchToFinanceCreateAccountAndComeBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        pickTask("LISTS")
        switchModule("FINANCE")

        // Первый запуск: счетов нет, нижняя панель — финансовая, «+» ведёт к созданию счёта.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Начнём со счёта") }
        composeRule.onNodeWithTag("tab:LISTS").assertExists()
        composeRule.onNodeWithTag("finance_add").performClick()
        composeRule.onNodeWithTag("account_amount").performTextReplacement("42300")
        composeRule.onNodeWithTag("account_save").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasContentDescription("Карта, 42\u00A0300\u00A0₽")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("42k").assertExists()

        // Этап 2 (#225): категория из настроек, расход через «+» — остаток счёта уменьшается, операция в списке.
        openFinanceSettings()
        composeRule.onNodeWithTag("finance_menu_categories").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("category_add") }
        composeRule.onNodeWithTag("category_add").performClick()
        composeRule.onNodeWithTag("category_name").performTextReplacement("Продукты")
        composeRule.onNodeWithTag("category_save").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Продукты") }
        composeRule.onNodeWithTag("app_bar").assertDoesNotExist()
        // «Назад» из категорий — в настройки финансов, затем на главную страницу настроек, затем в финансы.
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_settings") }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_add") }
        composeRule.onNodeWithTag("finance_add").performClick()
        composeRule.onNodeWithTag("operation_amount").performTextReplacement("850")
        composeRule.onNodeWithContentDescription("Продукты").performClick()
        composeRule.onNodeWithTag("operation_save").performClick()
        // Окно остаётся для следующей операции; закрываем его.
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasContentDescription("Карта, 41\u00A0450\u00A0₽")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("operation_back").performClick()

        composeRule.onNodeWithTag("finance_tab:OPERATIONS").assertDoesNotExist()
        composeRule.onNodeWithTag("finance_operations").performScrollToNode(hasText("− 850\u00A0₽") and hasClickAction())
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("− 850\u00A0₽") }
        switchModule("TASKS")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_LISTS_SCROLL) }
        composeRule.onNodeWithTag("finance_tab:OPERATIONS").assertDoesNotExist()
        switchModule("FINANCE")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("− 850\u00A0₽") }

        // Этап 4 (#231): операция в корзину из окна правки — остаток вернулся; из корзины (меню шестерёнки) — обратно.
        composeRule.onNodeWithTag("finance_operations").performScrollToNode(hasText("− 850\u00A0₽") and hasClickAction())
        composeRule.onNode(hasText("Продукты") and hasClickAction()).performClick()
        composeRule.onNodeWithTag("operation_trash").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Операций пока нет") }
        openFinanceSettings()
        composeRule.onNodeWithText("Операций: 1").assertExists()
        composeRule.onNodeWithTag("finance_menu_trash").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_trash") }
        composeRule.onNodeWithContentDescription("Восстановить").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Корзина пуста") }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_settings") }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_operations") }
        composeRule.onNodeWithTag("finance_operations").performScrollToNode(hasText("− 850\u00A0₽") and hasClickAction())
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("− 850\u00A0₽") }

        // На вложенных экранах (настройки) нижней панели нет.
        switchModule("TASKS")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_LISTS_SCROLL) }
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithTag(TAG_APP_BAR).assertExists()
    }

    /** Здоровье (#257): третий модуль — первая запись веса, вторая заменяет на ту же дату, настройки, возврат в задачи. */
    @Test
    fun switchToHealthRecordWeightAndComeBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        switchModule("HEALTH")

        // Записей нет: нижняя панель — «Здоровья» (одна страница «Вес»), «+» открывает окно ввода с 70,0.
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_empty") }
        composeRule.onNodeWithTag("tab:LISTS").assertDoesNotExist()
        composeRule.onNodeWithTag("module:HEALTH").assertExists()
        composeRule.onNodeWithTag("health_add").performClick()
        composeRule.onNodeWithTag("weight_window:confirm").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_hero") }
        composeRule.onNodeWithTag("health_weight").assertTextEquals("70,0")

        // Вторая запись на сегодня заменяет первую: в истории одна строка.
        composeRule.onNodeWithTag("health_add").performClick()
        composeRule.onNodeWithTag("weight_comment").performTextReplacement("утром")
        composeRule.onNodeWithTag("weight_window:confirm").performClick()
        // Тестовый экран крошечный: история ниже края, лента её не компонует — прокручиваем к записи.
        composeRule.waitUntil(5_000) { runCatching { composeRule.onNodeWithTag("health_list").performScrollToNode(hasText("утром")) }.isSuccess }
        composeRule.onNodeWithTag("health_goal").assertExists()

        // Настройки → «Здоровье» → напоминание включается и показывает время.
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithTag("settings_health").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_settings") }
        composeRule.onNodeWithTag("health_reminder_switch").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_reminder_time") }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_list") }

        // Возврат в задачи и снова в «Здоровье»: запись на месте (лента — с начала, на крошечном экране её верх мог уйти за край).
        switchModule("TASKS")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("tab:UPCOMING") }
        composeRule.onNodeWithTag("health_list").assertDoesNotExist()
        switchModule("HEALTH")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("health_list") }
        composeRule.onNodeWithTag("health_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("health_weight").assertTextEquals("70,0")
    }

    /** Меню кнопки модуля: выбрать модуль. */
    private fun switchModule(name: String) {
        if (name == "TASKS") taskTab().performClick()
        else composeRule.onNodeWithTag("module:$name").performClick()
    }

    private fun taskTab() = composeRule.onNode(
        androidx.compose.ui.test.hasTestTag("tab:UPCOMING") or
            androidx.compose.ui.test.hasTestTag("tab:WEEK") or androidx.compose.ui.test.hasTestTag("tab:LISTS"),
    )

    private fun pickTask(name: String) {
        taskTab().performClick()
        composeRule.onNodeWithTag("task_view:$name").performClick()
    }

    /** Единые настройки → «Финансы» (#241). */
    private fun openFinanceSettings() {
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists(TAG_APP_SETTINGS) }
        composeRule.onNodeWithTag(TAG_SETTINGS_FINANCE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTagExists("finance_settings") }
    }

    @Test
    fun createTaskOpenItAndRename() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        // «+» на «Все» → задача на сегодня.
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Купить продукты")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Купить продукты") }
        composeRule.onNodeWithText("Сегодня").assertExists()

        // Поиск находит новую задачу по части слова.
        pickTask("LISTS")
        composeRule.onNodeWithTag(TAG_LISTS_SEARCH).performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("прод")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("НАЙДЕНА 1 ЗАДАЧА") }
        pickTask("UPCOMING")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_SEARCH_FIELD).fetchSemanticsNodes().isEmpty() }

        // Тап по карточке → шторка задачи → переименование → «Готово».
        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_TITLE).performTextReplacement("Купить продукты на неделю")
        composeRule.onNodeWithTag(TAG_DETAIL_CLOSE).performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Купить продукты на неделю") }
    }

    /** Итерация 36: удаление из шторки — без вопроса; под шторкой «Задача удалена · Отмена», «Отмена» возвращает задачу. */
    @Test
    fun deleteTaskShowsUndoAndUndoBringsItBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Лишняя задача")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Лишняя задача") }

        composeRule.onNodeWithText("Лишняя задача").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Задача удалена") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }
        // Строка уходит из списка с анимацией — ждём, пока исчезнет.
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Лишняя задача") }

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Лишняя задача") }
    }

    /** Итерация 37: «Просрочено · N» — стрелка сворачивает прошедшие дни, значок переносит на сегодня с «Отменой». */
    @Test
    fun overdueBlockCollapsesAndMovesToToday() {
        val entry = ApplicationProvider.getApplicationContext<android.app.Application>().widgetEntryPoint()
        val past = entry.time().today().minusDays(3)
        val id = runBlocking { entry.tasks().createTask(NewTask(entry.projects().getInbox().id, "Просроченная", due = TaskDue.AllDay(past))) }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Просроченная") }
        composeRule.onNodeWithText("Просрочено · 1").assertExists()

        composeRule.onNodeWithTag(TAG_OVERDUE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Просроченная") }
        composeRule.onNodeWithText("Просрочено · 1").assertExists()
        composeRule.onNodeWithTag(TAG_OVERDUE_TOGGLE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Просроченная") }

        composeRule.onNodeWithTag(TAG_OVERDUE_MOVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Перенесено на сегодня: 1") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_OVERDUE_HEADER).fetchSemanticsNodes().isEmpty() }
        assertEquals(TaskDue.AllDay(entry.time().today()), runBlocking { entry.tasks().getTask(id) }?.due)

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_OVERDUE_HEADER).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TaskDue.AllDay(past), runBlocking { entry.tasks().getTask(id) }?.due)
    }

    /** Итерация 40: долгое нажатие — выбор; вместо вкладок лента; «Удалить» убирает выбранные в корзину, «Отмена» возвращает. */
    @Test
    fun bulkDeleteFromSelectionAndUndo() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        listOf("Первая", "Вторая").forEach { title ->
            composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
            composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement(title)
            composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists(title) }
        }

        composeRule.onNodeWithText("Первая").performTouchInput { longClick() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_SELECTION_BAR).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("tab:UPCOMING").assertDoesNotExist()
        composeRule.onNodeWithText("Вторая").performClick()
        composeRule.onNodeWithTag(TAG_SELECTION_COUNT).assertTextEquals("Выбрано: 2")

        composeRule.onNodeWithTag(TAG_ACTION_RIBBON).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(TAG_BULK_DELETE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Удалено: 2") }
        composeRule.onNodeWithTag(TAG_SELECTION_BAR).assertDoesNotExist()
        composeRule.onNodeWithTag("tab:UPCOMING").assertExists()
        composeRule.waitUntil(5_000) { !composeRule.onAllNodesWithTextExists("Первая") }

        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Первая") && composeRule.onAllNodesWithTextExists("Вторая") }
    }

    /** Итерация 39: удалённая задача лежит в «Корзине» (строка в «Списках» со счётчиком), открывается без правки и возвращается из «⋮». */
    @Test
    fun deletedTaskGoesToTrashAndComesBack() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("В корзину")
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) {
            !composeRule.onAllNodesWithTagExists(TAG_QUICK_SCRIM) &&
                composeRule.onAllNodesWithTextExists("В корзину")
        }
        composeRule.onNodeWithText("В корзину").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
        composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isEmpty() }

        pickTask("LISTS")
        // Итерация 57 (CI #90): «Списки» дозагружают числа и дерево списков уже после открытия — строки сдвигаются, и нажатие по старому
        // месту «Корзины» попадало мимо. Сначала ждём все данные (дерево списков и число корзины), потом прокручиваем к «Корзине».
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Списков пока нет") }
        composeRule.onNodeWithTag(TAG_LISTS_SCROLL).performScrollToKey("trash")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_TRASH_COUNT, useUnmergedTree = true).assertTextEquals("1")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_LISTS_SCROLL).performScrollToKey("trash")
        composeRule.onNodeWithTag(TAG_TRASH).performClick()
        composeRule.waitUntil(5_000) {
            !composeRule.onAllNodesWithTagExists(TAG_QUICK_SCRIM) &&
                composeRule.onAllNodesWithTextExists("В корзину")
        }

        composeRule.onNodeWithText("В корзину").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_TASK_BANNER).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_TRASH_TASK_MENU).performClick()
        composeRule.onNodeWithTag(TAG_TRASH_TASK_RESTORE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Задача восстановлена") }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_TASK_BANNER).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_TRASH_EMPTY).fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithContentDescription("Назад").performClick()
        pickTask("UPCOMING")
        composeRule.waitUntil(5_000) {
            !composeRule.onAllNodesWithTagExists(TAG_QUICK_SCRIM) &&
                composeRule.onAllNodesWithTextExists("В корзину")
        }
    }

    /** Итерация 38: закрыли карточку новой задачи, не сохранив, — следующее «+» открывается с тем же названием. */
    @Test
    fun draftOfNewTaskComesBackOnNextPlus() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).performTextReplacement("Купить билеты")
        // Нажатие на затемнение под карточкой закрывает её без сохранения (как «Назад»).
        composeRule.onNodeWithTag(TAG_QUICK_SCRIM).performTouchInput { click(Offset(centerX, bottom - 5f)) }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }

        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Купить билеты")))

        // Сохранили — черновика больше нет.
        composeRule.onNodeWithTag(TAG_QUICK_SAVE).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithTag(TAG_ADD_TASK).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAG_QUICK_TITLE).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    /** Итерация 38: запрос запоминается по «поиску» на клавиатуре; при пустом поле он в «Недавних», нажатие ищет. */
    @Test
    fun searchRemembersQueriesAndRepeatsThem() {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Всё сделано") }
        pickTask("LISTS")
        composeRule.onNodeWithTag(TAG_LISTS_SEARCH).performClick()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("молоко")
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performImeAction()
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).performTextReplacement("")

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("НЕДАВНИЕ") }
        composeRule.onNodeWithTag("search_recent:молоко").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Ничего не найдено") }
        composeRule.onNodeWithTag(TAG_SEARCH_FIELD).assertTextEquals("молоко")
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasText(text) or androidx.compose.ui.test.hasContentDescription(text))
        .fetchSemanticsNodes().isNotEmpty()

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
