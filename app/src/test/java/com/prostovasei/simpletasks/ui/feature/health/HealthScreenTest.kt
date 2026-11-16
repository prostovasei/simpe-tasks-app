package com.prostovasei.simpletasks.ui.feature.health

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.domain.model.health.HealthBackupInfo
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import com.prostovasei.simpletasks.ui.navigation.AppModule
import com.prostovasei.simpletasks.ui.navigation.HealthBottomBar
import com.prostovasei.simpletasks.ui.navigation.TAG_MODULE
import com.prostovasei.simpletasks.ui.navigation.TAG_MODULE_BUTTON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Раздел «Здоровье» (#257): экраны и окна по макетам design/approved/257. Данные вымышленные. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class HealthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HealthIntent>()
    private val today = LocalDate.parse("2026-11-16")

    /** Месяц записей из макета: (дней назад, вес в десятых, комментарий). */
    private val month = listOf(
        30 to 832, 28 to 834, 27 to 831, 25 to 830, 23 to 833, 21 to 829, 20 to 829, 18 to 827, 16 to 828, 14 to 826, 12 to 829,
        10 to 825, 9 to 826, 7 to 824, 5 to 826, 4 to 825, 2 to 827, 1 to 826, 0 to 824,
    ).map { (ago, tenths) ->
        WeightEntry(today.minusDays(ago.toLong()), tenths, if (ago == 7) "после тренировки" else null)
    }

    private fun mainState(settings: HealthSettings = HealthSettings(goalTenths = 780, heightCm = 180), entries: List<WeightEntry> = month, tip: LocalDate? = null, form: WeightForm? = null) =
        HealthState(today = today, entries = entries, settings = settings, tip = tip, form = form)

    /** Экран в каркасе: нижняя панель — как в приложении. */
    private fun show(initial: HealthState, dark: Boolean = false, onIntent: (HealthIntent) -> Unit = { intents += it }, content: (@Composable (HealthState) -> Unit)? = null) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                TaskCanvasTheme {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) { if (content != null) content(initial) else HealthScreen(initial, onIntent) }
                        HealthBottomBar(onAdd = { onIntent(HealthIntent.Add) }, onModule = {}, onSettings = {})
                    }
                }
            }
        }
    }

    @Test
    fun mainScreenShowsWeightChangeGoalAndHistory() {
        show(mainState())

        composeRule.onNodeWithTag(TAG_HEALTH_WEIGHT).assertTextEquals("82,4")
        composeRule.onNodeWithTag(TAG_HEALTH_CHANGE).assertTextEquals("−0,8 кг за месяц · последняя запись сегодня")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_TITLE, useUnmergedTree = true).assertTextEquals("Цель 78,0 кг · осталось 4,4 кг")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextContains("ИМТ 25,4 — выше нормы", substring = true)
        composeRule.onNodeWithTag("$TAG_HEALTH_PERIOD:MONTH").assertIsSelected()
        composeRule.onNodeWithTag(TAG_HEALTH_CHART).assertExists()
        composeRule.onNodeWithTag("$TAG_HEALTH_ROW:$today").assertExists()
        composeRule.onNodeWithText("16 ноября · сегодня").assertExists()
        composeRule.onNodeWithText("15 ноября · вчера").assertExists()
        composeRule.onNodeWithText("после тренировки").assertExists()
        composeRule.onNodeWithContentDescription("Раздел: Здоровье").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/health_main_light.png")
    }

    @Test
    fun tooltipRowIsHighlightedAndChartIsDrawn() {
        show(mainState(tip = today.minusDays(7)))

        composeRule.onNodeWithTag("$TAG_HEALTH_ROW:${today.minusDays(7)}").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/health_tooltip_light.png")
    }

    @Test
    fun emptyStateOffersFirstEntry() {
        show(HealthState(today = today, entries = emptyList()))

        composeRule.onNodeWithText("Запишите первый вес").assertExists()
        composeRule.onNodeWithTag(TAG_HEALTH_ADD_FIRST).performClick()
        composeRule.onNodeWithTag(TAG_HEALTH_ADD).performClick()
        assertEquals(listOf(HealthIntent.Add, HealthIntent.Add), intents)
        composeRule.onRoot().captureRoboImage("screenshots/health_empty_light.png")
    }

    @Test
    fun nothingIsShownWhileEntriesLoad() {
        show(HealthState(today = today, entries = null))
        composeRule.onNodeWithTag(TAG_HEALTH_EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_HEALTH_HERO).assertDoesNotExist()
    }

    @Test
    fun tapsOnPeriodGoalRowAndChartPointSendIntents() {
        show(mainState())

        composeRule.onNodeWithTag("$TAG_HEALTH_PERIOD:YEAR").performClick()
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL).performClick()
        composeRule.onNodeWithTag("$TAG_HEALTH_ROW:${today.minusDays(7)}").performClick()
        // Последняя точка графика стоит у правого края области рисования (за ней — поле подписей оси, 40 dp = 80 px).
        composeRule.onNodeWithTag(TAG_HEALTH_CHART).performTouchInput { click(Offset(width - 80f, height / 2f)) }

        assertEquals(
            listOf(HealthIntent.SelectPeriod(WeightPeriod.YEAR), HealthIntent.OpenGoal, HealthIntent.OpenEntry(today.minusDays(7)), HealthIntent.TapPoint(today)),
            intents,
        )
    }

    @Test
    fun changeIsHiddenWithOneEntryAndGoalRowAsksForGoal() {
        show(mainState(HealthSettings(), listOf(WeightEntry(today, 824))))

        composeRule.onNodeWithTag(TAG_HEALTH_CHANGE).assertTextEquals("последняя запись сегодня")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_TITLE, useUnmergedTree = true).assertTextEquals("Задать цель и рост")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextEquals("Для прогноза и ИМТ")
        composeRule.onRoot().captureRoboImage("screenshots/health_one_entry_light.png")
    }

    @Test
    fun goalRowStates() {
        // Только рост — ИМТ и приглашение задать цель.
        composeRule.setContent { AppTheme { TaskCanvasTheme { HealthScreen(mainState(HealthSettings(heightCm = 180)), {}) } } }
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_TITLE, useUnmergedTree = true).assertTextEquals("ИМТ 25,4 — выше нормы")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextEquals("Задать цель")
    }

    @Test
    fun goalRowWaitsForForecastDataThenShowsDate() {
        // Две записи — прогноза ещё нет.
        composeRule.setContent { AppTheme { TaskCanvasTheme { HealthScreen(mainState(entries = month.takeLast(2)), {}) } } }
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextContains("Прогноз — после 5 записей за 2 недели", substring = true)
    }

    @Test
    fun goalRowShowsForecastDate() {
        val steady = (0..5).map { WeightEntry(today.minusDays(it * 6L), 820 + it * 6) }.reversed()
        composeRule.setContent { AppTheme { TaskCanvasTheme { HealthScreen(mainState(HealthSettings(goalTenths = 780, heightCm = 180), steady), {}) } } }
        // −1 десятая в день, осталось 40 десятых → через 40 дней, 26 декабря.
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextContains("≈ к 26 декабря по текущему темпу", substring = true)
    }

    @Test
    fun goalRowSaysWeightGoesTheWrongWay() {
        val up = (0..5).map { WeightEntry(today.minusDays(it * 6L), 836 - it * 3) }.reversed()
        composeRule.setContent { AppTheme { TaskCanvasTheme { HealthScreen(mainState(entries = up), {}) } } }
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextContains("За 30 дней вес растёт — прогноза нет", substring = true)
    }

    @Test
    fun reachedGoalIsCongratulated() {
        val entries = month.dropLast(1) + WeightEntry(today, 778)
        composeRule.setContent { AppTheme { TaskCanvasTheme { HealthScreen(mainState(entries = entries), {}) } } }
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_TITLE, useUnmergedTree = true).assertTextEquals("Цель 78,0 кг достигнута 🎉")
        composeRule.onNodeWithTag(TAG_HEALTH_GOAL_SUB, useUnmergedTree = true).assertTextContains("Сейчас 77,8 кг", substring = true)
    }

    /** Окно живёт в состоянии, как во ViewModel: правки барабана и календаря возвращаются в форму. */
    private fun showDialog(start: WeightForm, entries: List<WeightEntry> = month, onSave: (HealthIntent) -> Unit = { intents += it }) {
        composeRule.setContent {
            var state by androidx.compose.runtime.remember { mutableStateOf(mainState(entries = entries, form = start)) }
            AppTheme {
                TaskCanvasTheme {
                    HealthScreen(state) { intent ->
                        when (intent) {
                            is HealthIntent.EditForm -> state = state.copy(form = state.form?.let(intent.edit))
                            else -> onSave(intent)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun newEntryDialogStartsFromTheLastWeight() {
        // Сегодняшней записи ещё нет: последняя — вчерашняя 82,6.
        showDialog(WeightForm.of(826, today), entries = month.dropLast(1))

        composeRule.onNodeWithText("Вес").assertExists()
        composeRule.onNodeWithTag("$TAG_WEIGHT_DRUM:whole").assertExists()
        composeRule.onNodeWithTag(TAG_WEIGHT_PREFILL).assertTextEquals("Заранее стоит последний вес — 82,6 кг")
        composeRule.onNodeWithTag(TAG_WEIGHT_DATE).assertExists()
        composeRule.onNodeWithText("Сегодня, 16 ноября").assertExists()
        composeRule.onNodeWithTag(TAG_WEIGHT_DELETE).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_WEIGHT_CLASH).assertDoesNotExist()
        composeRule.onRoot().captureRoboImage("screenshots/health_weight_new_light.png")
        composeRule.onNodeWithTag("$TAG_WEIGHT_WINDOW:confirm").performClick()
        assertEquals(listOf(HealthIntent.SaveForm), intents)
    }

    @Test
    fun drumsChangeTheWeight() {
        // Вес живёт в состоянии, как во ViewModel: после первого барабана второй видит новые килограммы.
        var form by mutableStateOf(WeightForm.of(824, today))
        composeRule.setContent {
            AppTheme {
                TaskCanvasTheme {
                    HealthScreen(mainState(form = form)) { intent -> if (intent is HealthIntent.EditForm) form = form.let(intent.edit) }
                }
            }
        }
        // Килограммы: индекс = значение − 30.
        composeRule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("$TAG_WEIGHT_DRUM:whole"))).performScrollToIndex(85 - 30)
        composeRule.waitForIdle()
        assertEquals(854, form.tenths)
        composeRule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("$TAG_WEIGHT_DRUM:tenth"))).performScrollToIndex(7)
        composeRule.waitForIdle()
        assertEquals(857, form.tenths)
    }

    @Test
    fun occupiedDateWarnsThatItWillBeReplaced() {
        showDialog(WeightForm.of(824, today.minusDays(1)))

        composeRule.onNodeWithTag(TAG_WEIGHT_CLASH).assertTextEquals("На 15 ноября уже 82,6 кг — заменится")
        composeRule.onNodeWithText("Вчера, 15 ноября").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/health_weight_replace_light.png")
    }

    @Test
    fun editingAnEntryHasTrashAndDoesNotWarnAboutItself() {
        showDialog(WeightForm.of(824, today.minusDays(7), "после тренировки", editingDate = today.minusDays(7)))

        composeRule.onNodeWithText("Запись · 9 ноября").assertExists()
        composeRule.onNodeWithTag(TAG_WEIGHT_CLASH).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_WEIGHT_PREFILL).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_WEIGHT_DELETE).performClick()
        assertEquals(listOf(HealthIntent.DeleteForm), intents)
        composeRule.onRoot().captureRoboImage("screenshots/health_weight_edit_light.png")
    }

    @Test
    fun calendarMarksDaysWithEntriesBlocksFutureAndPicksDate() {
        showDialog(WeightForm.of(824, today))

        composeRule.onNodeWithTag(TAG_WEIGHT_DATE).performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_MONTH).assertTextEquals("Ноябрь 2026")
        composeRule.onRoot().captureRoboImage("screenshots/health_calendar_light.png")
        // Будущий день нельзя выбрать, следующий месяц недоступен.
        composeRule.onNodeWithTag("$TAG_CALENDAR_DAY:2026-11-20").performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_NEXT).performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_MONTH).assertTextEquals("Ноябрь 2026")
        // Предыдущий месяц, 5 октября.
        composeRule.onNodeWithTag(TAG_CALENDAR_PREV).performClick()
        composeRule.onNodeWithTag(TAG_CALENDAR_MONTH).assertTextEquals("Октябрь 2026")
        composeRule.onNodeWithTag("$TAG_CALENDAR_DAY:2026-10-05").performClick()
        composeRule.onNodeWithTag("$TAG_CALENDAR:confirm").performClick()
        composeRule.onNodeWithText("5 октября").assertExists()
    }

    private fun showGoal(form: GoalForm, settings: HealthSettings = HealthSettings(), onIntent: (HealthIntent) -> Unit = { intents += it }) {
        composeRule.setContent {
            AppTheme { TaskCanvasTheme { HealthScreen(mainState(settings).copy(goalForm = form), onIntent) } }
        }
    }

    @Test
    fun goalDialogShowsBmiAndLetsRemoveGoal() {
        val goalForm = GoalForm(780, 180)
        showGoal(goalForm, HealthSettings(goalTenths = 780, heightCm = 180))

        composeRule.onNodeWithTag(TAG_GOAL_BMI).assertTextEquals("ИМТ сейчас 25,4 — выше нормы (норма 18,5–24,9). При 78,0 кг будет 24,1.")
        composeRule.onNodeWithTag(TAG_GOAL_REMOVE).performClick()
        composeRule.onNodeWithTag(TAG_HEIGHT_REMOVE).performClick()
        composeRule.onNodeWithTag("$TAG_GOAL_WINDOW:confirm").performClick()
        assertEquals(3, intents.size)
        assertEquals(HealthIntent.SaveGoal, intents.last())
        composeRule.onRoot().captureRoboImage("screenshots/health_goal_light.png")
    }

    @Test
    fun goalDialogWithoutGoalOffersToAddOne() {
        showGoal(GoalForm(null, null))

        composeRule.onNodeWithTag(TAG_GOAL_ADD).performClick()
        composeRule.onNodeWithTag(TAG_HEIGHT_ADD).performClick()
        composeRule.onNodeWithTag(TAG_GOAL_BMI).assertDoesNotExist()
        val edits = intents.filterIsInstance<HealthIntent.EditGoal>()
        assertEquals(listOf(GoalForm(824, null), GoalForm(null, 170)), edits.map { it.edit(GoalForm(null, null)) })
    }

    @Test
    fun healthIsInTheModuleMenu() {
        val modules = mutableListOf<AppModule>()
        composeRule.setContent {
            AppTheme { HealthBottomBar(onAdd = {}, onModule = { modules += it }, onSettings = {}) }
        }
        composeRule.onNodeWithTag("$TAG_HEALTH_TAB:WEIGHT").assertIsSelected()
        composeRule.onNodeWithTag(TAG_MODULE_BUTTON).performClick()
        composeRule.onNodeWithTag("$TAG_MODULE:HEALTH").assertExists()
        composeRule.onNodeWithTag("$TAG_MODULE:TASKS").performClick()
        composeRule.onNodeWithTag(TAG_MODULE_BUTTON).performClick()
        composeRule.onNodeWithTag("$TAG_MODULE:FINANCE").performClick()
        assertEquals(listOf(AppModule.TASKS, AppModule.FINANCE), modules)
    }

    @Test
    fun settingsPageControlsReminderAndOpensBackup() {
        var reminder = false
        var time = 0
        var backup = 0
        composeRule.setContent {
            var on by androidx.compose.runtime.remember { mutableStateOf(false) }
            AppTheme {
                HealthSettingsScreen(
                    state = HealthState(today = today, entries = month, settings = HealthSettings(reminderOn = on)),
                    onBack = {},
                    onBackup = { backup++ },
                    onReminder = { on = it; reminder = it },
                    onPickTime = { time++ },
                )
            }
        }
        composeRule.onNodeWithTag(TAG_HEALTH_REMINDER_TIME).assertDoesNotExist()
        composeRule.onNodeWithText("Только если сегодня записи нет").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/health_settings_off_light.png")
        composeRule.onNodeWithTag(TAG_HEALTH_REMINDER_SWITCH).performClick()
        assertTrue(reminder)
        composeRule.onNodeWithText("08:00").assertExists()
        composeRule.onNodeWithTag(TAG_HEALTH_REMINDER_TIME).performClick()
        composeRule.onNodeWithText("Копия ещё не сохранялась · записей веса: 19").assertExists()
        composeRule.onNodeWithTag(TAG_HEALTH_BACKUP).performClick()
        assertEquals(1, time)
        assertEquals(1, backup)
        composeRule.onRoot().captureRoboImage("screenshots/health_settings_light.png")
    }

    @Test
    fun backupPageSavesRestoresAndComparesBeforeReplacing() {
        val actions = mutableListOf<String>()
        val info = HealthBackupInfo(142, LocalDate.parse("2026-01-03"), LocalDate.parse("2026-11-02"), HealthSettings(goalTenths = 780, heightCm = 180))
        composeRule.setContent {
            AppTheme {
                HealthBackupScreen(
                    state = HealthState(today = today, entries = month, settings = HealthSettings(goalTenths = 780, heightCm = 180, reminderOn = true), restore = RestoreConfirm("{}", info)),
                    onBack = {},
                    onSave = { actions += "save" },
                    onRestore = { actions += "restore" },
                    onConfirm = { actions += "confirm" },
                    onCancel = { actions += "cancel" },
                )
            }
        }
        composeRule.onNodeWithText("Восстановить копию?").assertExists()
        composeRule.onNodeWithText("142").assertExists()
        composeRule.onNodeWithText("19").assertExists()
        composeRule.onNodeWithText("3 янв – 2 ноя").assertExists()
        composeRule.onNodeWithText("Данные «Здоровья» заменятся целиком. Задачи и финансы не меняются.").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/health_restore_light.png")
        composeRule.onNodeWithTag("$TAG_HEALTH_RESTORE:confirm").performClick()
        composeRule.onNodeWithTag("$TAG_HEALTH_RESTORE:cancel").performClick()
        assertEquals(listOf("confirm", "cancel"), actions)
    }
}
