package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Priority
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.ui.common.DueText
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/** Вид «Неделя» (итерация 44): ячейки дней, нажатия на кружок и плашку, мини-календарь, снимки. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class WeekScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<Any>()
    private val opened = mutableListOf<String>()

    private val openedDays = mutableListOf<LocalDate>()

    private fun show(
        dark: Boolean = false,
        state: WeekState = weekState,
        content: @Composable () -> Unit = { WeekScreen(state, { intents += it }, onOpenTask = { opened += it }, onOpenDay = { openedDays += it }) },
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { content() } }
    }

    private val monday = LocalDate.of(2026, 10, 26)
    private val today = LocalDate.of(2026, 10, 30)

    private fun item(
        id: String,
        title: String,
        time: String? = null,
        priority: Priority = Priority.NONE,
        done: Boolean = false,
        ghostOf: String? = null,
    ) = TaskItem(
        id, title, priority, DueText(DayLabel.Today, time), "Работа", "💼", isInboxList = false, isDone = done, ghostOf = ghostOf,
        isRecurring = ghostOf != null,
    )

    private val weekState = WeekState(
        weekStart = monday,
        today = today,
        days = listOf(
            WeekDay(monday, listOf(item("a1", "Купить продукты", "10:00", Priority.NONE, done = true), item("a2", "Тренировка"))),
            WeekDay(monday.plusDays(1), listOf(item("b1", "Оплатить интернет", "12:00", Priority.HIGH), item("b2", "Позвонить маме"))),
            WeekDay(monday.plusDays(2), listOf(item("c1", "Встреча", "15:00", Priority.HIGH))),
            WeekDay(monday.plusDays(3), emptyList()),
            WeekDay(
                monday.plusDays(4),
                listOf(item("e1", "Отчёт за неделю", "17:00", Priority.HIGH), item("e2", "Кино"), item("e3", "Ужин с Аней", "20:00"), item("e4", "Зарядка", "08:00", done = true)),
            ),
            WeekDay(monday.plusDays(5), listOf(item("f1", "Уборка", "11:00"), item("f2@0", "Созвон с командой", "10:00", ghostOf = "f2"))),
            WeekDay(monday.plusDays(6), listOf(item("g1", "План на неделю"))),
        ),
        month = YearMonth.of(2026, 10),
        isLoading = false,
    )

    @Test
    fun showsSevenDaysAndTheirTasks() {
        show()
        (0L..6L).forEach { composeRule.onNodeWithTag("week-day:${monday.plusDays(it)}").assertExists() }
        listOf("Купить продукты", "Оплатить интернет", "Отчёт за неделю", "План на неделю").forEach {
            composeRule.onNodeWithTag("week-task:" + weekState.days.flatMap { d -> d.items }.first { item -> item.title == it }.id).assertExists()
        }
    }

    @Test
    fun tapOnCircleTogglesAndTapOnPlaqueOpens() {
        show()
        composeRule.onNodeWithTag("week-check:b1").performClick()
        assertEquals(listOf<Any>(TaskListIntent.ToggleComplete("b1", recurring = false, done = false)), intents)
        // Выполненная: нажатие возвращает её.
        composeRule.onNodeWithTag("week-check:a1").performClick()
        assertEquals(TaskListIntent.ToggleComplete("a1", recurring = false, done = true), intents.last())
        composeRule.onNodeWithTag("week-task:c1").performClick()
        assertEquals(listOf("c1"), opened)
    }

    @Test
    fun futureRepeatHasNoCircleButOpensTheSeriesTask() {
        show()
        composeRule.onNodeWithTag("week-check:f2@0").assertDoesNotExist()
        // Итерация 47: у повторяющейся — знак повтора вместо кружка.
        composeRule.onNodeWithTag("week-repeat:f2@0", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("week-task:f2@0").performClick()
        assertEquals(listOf("f2"), opened)
    }

    /** Проверка PM (0.47.0): выполненная повторяющаяся — без галочки, знак повтора остаётся; нажатие возвращает её. */
    @Test
    fun doneRepeatKeepsTheRepeatMark() {
        val state = weekState.copy(days = weekState.days.mapIndexed { i, d -> if (i == 0) d.copy(items = listOf(item("r1", "Бритьё", "09:00", done = true).copy(isRecurring = true))) else d })
        show(state = state)
        composeRule.onNodeWithTag("week-repeat:r1", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("week-check:r1").performClick()
        assertEquals(TaskListIntent.ToggleComplete("r1", recurring = true, done = true), intents.last())
    }

    /** Итерация 55 (как в виджете): эмодзи списка слева от времени, дата капсом «ПН 2». */
    @Test
    fun rowsShowListEmojiAndCapsDate() {
        show()
        composeRule.onNodeWithTag("week-emoji:a1", useUnmergedTree = true).assertTextEquals("💼")
        composeRule.onNodeWithText("ПН ${monday.dayOfMonth}").assertExists()
    }

    @Test
    fun calendarSelectsWeekWithoutMonthArrows() {
        show()
        // 0.46.0: строки месяца со стрелками нет — месяц листается выбором недели (WeekViewModelTest).
        composeRule.onNodeWithTag("week-cal-prev").assertDoesNotExist()
        composeRule.onNodeWithTag("week-cal-next").assertDoesNotExist()
        // Октябрь 2026: первая строка — с понедельника 28 сентября.
        composeRule.onNodeWithTag("week-cal-row:2026-10-05").performClick()
        assertEquals(SelectWeek(LocalDate.of(2026, 10, 5)), intents.last())
    }

    @Test
    fun calendarAlwaysHasSixWeekRows() {
        // Октябрь 2026 начинается с четверга: строки с 28 сентября по 2 ноября.
        show()
        listOf("2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19", "2026-10-26", "2026-11-02").forEach {
            composeRule.onNodeWithTag("week-cal-row:$it").assertExists()
        }
        composeRule.onNodeWithTag("week-cal-row:2026-11-09").assertDoesNotExist()
    }

    @Test
    fun calendarOfAMonthStartingOnSundayReachesSixRows() {
        // Ноябрь 2026 начинается с воскресенья: первая строка — с 26 октября, последняя — с 30 ноября.
        show(state = weekState.copy(month = YearMonth.of(2026, 11)))
        composeRule.onNodeWithTag("week-cal-row:2026-10-26").assertExists()
        composeRule.onNodeWithTag("week-cal-row:2026-11-30").assertExists()
    }

    /** Решение PM (0.44.0): все ячейки одинаковой высоты и ширины, неделя помещается на экран без прокрутки. */
    @Test
    fun allCellsHaveTheSameSizeAndFitTheScreen() {
        val crowded = weekState.copy(days = weekState.days.mapIndexed { i, d -> if (i == 4) d.copy(items = (1..12).map { item("x$it", "Задача $it") }) else d })
        show(state = crowded)
        val cells = (0L..6L).map { composeRule.onNodeWithTag("week-day:${monday.plusDays(it)}").getUnclippedBoundsInRoot() }
        assertEquals("высота", 1, cells.map { it.bottom - it.top }.distinct().size)
        assertEquals("ширина", 1, cells.map { it.right - it.left }.distinct().size)
        val screen = composeRule.onNodeWithTag("week-screen").getUnclippedBoundsInRoot()
        assertTrue("неделя помещается", cells.maxOf { it.bottom } <= screen.bottom)
        // Мини-календарь — в последней ячейке, того же размера.
        val calendar = composeRule.onNodeWithTag("week-calendar").getUnclippedBoundsInRoot()
        assertTrue("календарь $calendar в ячейке ${cells[6]}", calendar.top >= cells[6].top - TOLERANCE && calendar.bottom <= cells[6].bottom + TOLERANCE)
    }

    @Test
    fun tasksThatDoNotFitBecomePlusNInTheDateRow() {
        val crowded = weekState.copy(days = weekState.days.mapIndexed { i, d -> if (i == 4) d.copy(items = (1..12).map { item("x$it", "Задача $it") }) else d })
        show(state = crowded)
        val friday = monday.plusDays(4)
        composeRule.onNodeWithTag("week-more:$friday", useUnmergedTree = true).assertExists()
        val shown = (1..12).count { composeRule.onAllNodesWithTag("week-task:x$it").fetchSemanticsNodes().isNotEmpty() }
        assertTrue("часть задач скрыта", shown in 1..11)
        composeRule.onNodeWithTag("week-more:$friday", useUnmergedTree = true).assertTextEquals("+${12 - shown}")
        // Там, где всё помещается, «+N» нет.
        composeRule.onNodeWithTag("week-more:${monday}", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun tapOnDateRowOpensTheDay() {
        show()
        composeRule.onNodeWithTag("week-day-open:${monday.plusDays(1)}").performClick()
        assertEquals(listOf(monday.plusDays(1)), openedDays)
    }

    @Test
    fun blankAreaOpensItsDay() {
        show()
        composeRule.onNodeWithTag("week-blank:${monday.plusDays(3)}").performClick()
        composeRule.onNodeWithTag("week-blank:$monday").performClick()
        assertEquals(listOf(monday.plusDays(3), monday), openedDays)
        assertTrue(intents.isEmpty())
    }

    @Test
    fun todayLabelSharesTheDateAndLeavesSpaceForMore() {
        val crowded = weekState.copy(days = weekState.days.map { day ->
            if (day.date == today) day.copy(items = (1..12).map { item("today$it", "Задача $it") }) else day
        })
        show(state = crowded)
        val label = composeRule.onNodeWithTag("week-today:$today", useUnmergedTree = true)
        label.assertTextEquals("ПТ 30 сегодня")
        composeRule.onNodeWithTag("week-date:$monday", useUnmergedTree = true).assertTextEquals("ПН 26")
        val labelBounds = label.getUnclippedBoundsInRoot()
        val moreBounds = composeRule.onNodeWithTag("week-more:$today", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("дата и +N не накладываются", labelBounds.right <= moreBounds.left)
    }

    @Test
    fun timeColumnPrecedesToggleAndUntimedTasksStayAligned() {
        show()
        val time = composeRule.onNodeWithTag("week-time:a1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val timed = composeRule.onNodeWithTag("week-check:a1").getUnclippedBoundsInRoot()
        val untimed = composeRule.onNodeWithTag("week-check:a2").getUnclippedBoundsInRoot()
        val day = composeRule.onNodeWithTag("week-date:$monday", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(day.left, time.left)
        val title = composeRule.onNodeWithTag("week-title:a1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(2.dp, timed.left - time.right)
        assertEquals(2.dp, title.left - timed.right)
        assertEquals(timed.left, untimed.left)
        composeRule.onNodeWithTag("week-time:a1", useUnmergedTree = true).performTouchInput { click() }
        assertEquals(TaskListIntent.ToggleComplete("a1", recurring = false, done = true), intents.last())
        assertTrue(opened.isEmpty())
        composeRule.onNodeWithTag("week-time:a2", useUnmergedTree = true).performClick()
        assertEquals(TaskListIntent.ToggleComplete("a2", recurring = false, done = false), intents.last())
        composeRule.onNodeWithTag("week-time:f2@0", useUnmergedTree = true).performClick()
        assertEquals(listOf("f2"), opened)
    }

    @Test
    fun weekPriorityStates() {
        val state = weekState.copy(days = weekState.days.mapIndexed { i, day ->
            if (i == 0) day.copy(items = listOf(
                item("p1", "Высокий приоритет", "10:00", Priority.HIGH),
                item("p2", "Выполненный приоритет", "11:00", Priority.HIGH, done = true),
                item("p3", "Повтор выполнен", "12:00", Priority.HIGH, done = true).copy(isRecurring = true),
                item("p4", "Без времени", priority = Priority.HIGH),
            )) else day
        })
        show(state = state)
        composeRule.onNodeWithTag("week-screen").captureRoboImage("screenshots/week_priority_states.png")
    }

    @Test
    fun weekScreenLight() {
        show()
        composeRule.onNodeWithTag("week-screen").captureRoboImage("screenshots/week_light.png")
    }

    @Test
    fun weekScreenDark() {
        show(dark = true)
        composeRule.onNodeWithTag("week-screen").captureRoboImage("screenshots/week_dark.png")
    }

    private companion object {
        /** Ячейка отмечена тегом внутри своих отступов, календарь — ближе к краю строки. */
        val TOLERANCE = 10.dp
    }
}
