package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Шторка дня поверх рабочего стола (итерация 51): подпись, «выполнено из всех», лента, «Задача на …», закрытие, снимок. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class DaySheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val intents = mutableListOf<Any>()
    private val opened = mutableListOf<String>()
    private val added = mutableListOf<LocalDate>()
    private var closed = 0
    private val today = LocalDate.of(2026, 11, 7)

    private fun item(id: String, title: String, time: String?, done: Boolean = false, recurring: Boolean = false) =
        TaskItem(id, title, Priority.NONE, DueText(DayLabel.Today, time), "Дом", "🏠", isInboxList = false, isDone = done, isRecurring = recurring)

    private val state = DayState(
        date = today,
        today = today,
        items = listOf(
            item("tg", "Telegram", "10:30", done = true, recurring = true),
            item("hookah", "Кальян", "19:30–21:30"),
            item("football", "Футбол", "21:30–23:30"),
            item("flowers", "Цветы", "23:00", recurring = true),
        ),
        isLoading = false,
    )

    private fun show(value: DayState = state, fontScale: Float = 1f) {
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                DaySheet(value, { intents += it }, onClose = { closed++ }, onOpenTask = { opened += it }, onAdd = { added += it }, nowMinute = 23 * 60 + 38)
                }
            }
        }
    }

    @Test
    fun showsCompactDateWithoutRelativeLabelOrCount() {
        show()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("СБ 07 ноя")
        composeRule.onNodeWithTag(TAG_DAY_SHEET_COUNT).assertDoesNotExist()
        composeRule.onNodeWithText("Сегодня").assertDoesNotExist()
        listOf("tg", "hookah", "football", "flowers").forEach { composeRule.onNodeWithTag("task:$it").assertExists() }
    }

    @Test
    fun circleTogglesPlaqueOpensAddAndScrimClose() {
        show()
        composeRule.onNodeWithTag("task:hookah:check").performClick()
        assertEquals(TaskListIntent.ToggleComplete("hookah", recurring = false, done = false), intents.last())
        composeRule.onNodeWithTag("task:football").performClick()
        assertEquals(listOf("football"), opened)
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).assertContentDescriptionEquals("Задача на сб, 07 ноя").performClick()
        assertEquals(listOf(today), added)
        assertEquals("Нажатие по шторке не закрывает", 0, closed)
        composeRule.onNodeWithTag(TAG_DAY_SCRIM).performClick()
        composeRule.waitForIdle()
        assertEquals("#314: закрывается после анимации", 1, closed)
    }

    @Test
    fun emptyDayStillOffersNewTask() {
        show(state.copy(items = emptyList()))
        composeRule.onNodeWithTag(TAG_DAY_SHEET_COUNT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).assertExists().performClick()
        assertEquals(listOf(today), added)
    }

    @Test
    fun periodMatchesOrdinaryHeightAndRoundNodeWhileKeepingBothTimesWithoutNow() {
        show()
        val regular = composeRule.onNodeWithTag("task:flowers").getUnclippedBoundsInRoot()
        val period = composeRule.onNodeWithTag("task:hookah").getUnclippedBoundsInRoot()
        assertEquals(regular.bottom - regular.top, period.bottom - period.top)
        val node = composeRule.onNodeWithTag("task:hookah:node", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(node.right - node.left, node.bottom - node.top)
        composeRule.onNodeWithText("19:30", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("23:30", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("23:38", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi")
    fun narrowDayKeepsDateAddAndTimesVisibleWithLargeFont() {
        show(fontScale = 1.3f)
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_COUNT).assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).assertIsDisplayed()
        composeRule.onNodeWithText("19:30", useUnmergedTree = true).assertIsDisplayed()
        listOf("19:30", "23:30").forEach { time ->
            val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            composeRule.onNodeWithText(time, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            org.junit.Assert.assertFalse("время $time: width=${results.single().didOverflowWidth}, height=${results.single().didOverflowHeight}, size=${results.single().size}, constraints=${results.single().layoutInput.constraints}", results.single().hasVisualOverflow)
        }

        composeRule.onNodeWithText("23:30", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("screenshots/day_sheet_small_large_font.png")
    }

    @Test
    fun addIsInDateRowAndUsesExistingWideButtonGeometry() {
        show()
        val date = composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).getUnclippedBoundsInRoot()
        val add = composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).getUnclippedBoundsInRoot()
        val visual = composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD_VISUAL, useUnmergedTree = true).getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(date.right <= add.left)
        org.junit.Assert.assertTrue(date.top >= add.top && date.bottom <= add.bottom)
        assertEquals(64.dp, visual.right - visual.left)
        assertEquals(32.dp, visual.bottom - visual.top)
        org.junit.Assert.assertTrue("область нажатия не меньше 48 dp", add.bottom - add.top >= 48.dp)
    }

    @Test
    fun tomorrowHeaderHasOnlyDateAndPlusAddsOnThatDate() {
        val date = today.plusDays(1)
        show(state.copy(date = date))
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("ВС 08 ноя")
        composeRule.onNodeWithText("Завтра").assertDoesNotExist()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).performClick()
        assertEquals(listOf(date), added)
    }

    @Test
    fun yesterdayHeaderHasOnlyDate() {
        show(state.copy(date = today.minusDays(1)))
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("ПТ 06 ноя")
        composeRule.onNodeWithText("Вчера").assertDoesNotExist()
    }

    @Test
    fun swipeShowsNextDayAndPlusAddsThere() {
        show()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_PAGER).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("ВС 08 ноя")
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).performClick()
        assertEquals(listOf(today.plusDays(1)), added)
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("СБ 07 ноя")
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertTextEquals("ПТ 06 ноя")
    }

    @Test
    fun shortPullSpringsBackLongPullExpandsAndPullDownCollapses() {
        show()
        val before = composeRule.onNodeWithTag(TAG_DAY_SHEET).getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeUp(centerY, centerY - 60f) }
        composeRule.waitForIdle()
        assertEquals("резинка пружинит обратно", before, composeRule.onNodeWithTag(TAG_DAY_SHEET).getUnclippedBoundsInRoot())
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeUp(centerY, centerY - 600f) }
        composeRule.waitForIdle()
        val full = composeRule.onNodeWithTag(TAG_DAY_SHEET).getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("на весь экран: ${full.top}", full.bottom - full.top > 850.dp)
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeDown(centerY, centerY + 300f) }
        composeRule.waitForIdle()
        assertEquals("вниз из развёрнутого — обратно, не закрыть", 0, closed)
        assertEquals(before, composeRule.onNodeWithTag(TAG_DAY_SHEET).getUnclippedBoundsInRoot())
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).performTouchInput { swipeDown(centerY, centerY + 300f) }
        composeRule.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun lastTaskOfDayCelebratesOthersDoNot() {
        val all = state.items
        assertEquals(false, closesDay(all, all[1]))
        val last = all.map { if (it.id == "hookah") it else it.copy(isDone = true) }
        assertEquals(true, closesDay(last, last[1]))
        assertEquals("снять отметку — не праздник", false, closesDay(last, last[0]))
        show(state.copy(items = last))
        composeRule.onNodeWithTag(TAG_DAY_CELEBRATION).assertDoesNotExist()
        composeRule.onNodeWithTag("task:hookah:check").performClick()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(com.prostovasei.simpletasks.ui.common.DONE_CHECK_MS + 300L)
        composeRule.onNodeWithTag(TAG_DAY_CELEBRATION).assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/day_sheet_celebration.png")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_DAY_CELEBRATION).assertDoesNotExist()
    }

    @Test
    fun growsFromWidgetCellAndClosesBackIntoIt() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                DaySheet(state, { intents += it }, onClose = { closed++ }, onOpenTask = { opened += it }, onAdd = { added += it }, nowMinute = 23 * 60 + 38,
                    origin = android.graphics.Rect(560, 300, 760, 420))
            }
        }
        composeRule.mainClock.advanceTimeBy(DAY_GROW_MS / 2L)
        composeRule.onRoot().captureRoboImage("screenshots/day_sheet_grow_midway.png")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_DAY_SHEET_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DAY_SCRIM).performClick()
        composeRule.waitForIdle()
        assertEquals(1, closed)
    }

    /** #323: «+» и задача сообщают своё место — из него растёт окно задачи поверх окна дня. */
    @Test
    fun plusAndTaskReportWhereTheTaskWindowGrowsFrom() {
        val origins = mutableListOf<android.graphics.Rect>()
        composeRule.setContent {
            AppTheme {
                DaySheet(state, { intents += it }, onClose = { closed++ }, onOpenTask = { opened += it }, onAdd = { added += it }, onOrigin = { origins += it })
            }
        }
        composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).performClick()
        val plus = composeRule.onNodeWithTag(TAG_DAY_SHEET_ADD).getUnclippedBoundsInRoot()
        val density = composeRule.density.density
        val rect = origins.single()
        assertEquals((plus.right - plus.left).value * density, rect.width().toFloat(), 2f)
        composeRule.onNodeWithTag("task:football").performClick()
        val row = origins.last()
        val sheet = composeRule.onNodeWithTag(TAG_DAY_SHEET).getUnclippedBoundsInRoot()
        assertEquals("строка — на ширину окна дня", (sheet.right - sheet.left).value * density, row.width().toFloat(), 2f)
        assertEquals(listOf("football"), opened)
    }

    /** #323: пока над окном дня открыто окно задачи, «Назад» не закрывает окно дня; без окна задачи — закрывает. */
    @Test
    fun coveredSheetLeavesBackToTheTaskWindow() {
        var covered by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                DaySheet(state, { intents += it }, onClose = { closed++ }, onOpenTask = { opened += it }, onAdd = { added += it }, covered = covered)
            }
        }
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(0, closed)
        covered = false
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun daySheetLight() {
        show()
        composeRule.onRoot().captureRoboImage("screenshots/day_sheet_light.png")
    }
}
