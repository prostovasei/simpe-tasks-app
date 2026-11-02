package com.prostovasei.simpletasks.ui.feature.week

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

    private fun show(dark: Boolean = false, state: WeekState = weekState, content: @Composable () -> Unit = { WeekScreen(state, { intents += it }, onOpenTask = { opened += it }) }) {
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
        id, title, priority, DueText(DayLabel.Today, time), "Работа", "#3D8BFD", isInboxList = false, isDone = done, ghostOf = ghostOf,
    )

    private val weekState = WeekState(
        weekStart = monday,
        today = today,
        days = listOf(
            WeekDay(monday, listOf(item("a1", "Купить продукты", "10:00", Priority.LOW, done = true), item("a2", "Тренировка"))),
            WeekDay(monday.plusDays(1), listOf(item("b1", "Оплатить интернет", "12:00", Priority.MEDIUM), item("b2", "Позвонить маме"))),
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
        composeRule.onNodeWithTag("week-task:f2@0").performScrollTo().performClick()
        assertEquals(listOf("f2"), opened)
    }

    @Test
    fun calendarSelectsWeekAndShiftsMonth() {
        show()
        composeRule.onNodeWithTag("week-cal-title").assertIsDisplayed()
        // Октябрь 2026: первая строка — с понедельника 28 сентября.
        composeRule.onNodeWithTag("week-cal-row:2026-10-05").performScrollTo().performClick()
        assertEquals(SelectWeek(LocalDate.of(2026, 10, 5)), intents.last())
        composeRule.onNodeWithTag("week-cal-next").performClick()
        assertEquals(ShiftMonth(1), intents.last())
        composeRule.onNodeWithTag("week-cal-prev").performClick()
        assertEquals(ShiftMonth(-1), intents.last())
    }

    @Test
    fun calendarAlwaysHasSixWeekRows() {
        // Октябрь 2026 начинается с четверга: строки с 28 сентября по 2 ноября.
        show()
        listOf("2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19", "2026-10-26", "2026-11-02").forEach {
            composeRule.onNodeWithTag("week-cal-row:$it").performScrollTo().assertExists()
        }
        composeRule.onNodeWithTag("week-cal-row:2026-11-09").assertDoesNotExist()
    }

    @Test
    fun calendarOfAMonthStartingOnSundayReachesSixRows() {
        // Ноябрь 2026 начинается с воскресенья: первая строка — с 26 октября, последняя — с 30 ноября.
        show(state = weekState.copy(month = YearMonth.of(2026, 11)))
        composeRule.onNodeWithTag("week-cal-row:2026-10-26").performScrollTo().assertExists()
        composeRule.onNodeWithTag("week-cal-row:2026-11-30").performScrollTo().assertExists()
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
}
