package com.prostovasei.simpletasks.ui.feature.habits

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/** Раздел «Привычки» (#340): экран, карточка, окно — по ТЗ и конструктору design/approved/340. Данные вымышленные. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class HabitsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HabitsIntent>()
    private val today = LocalDate.parse("2026-11-25")
    private fun days(vararg ago: Long) = ago.map { today.minusDays(it) }.toSet()

    private val habits = listOf(
        Habit("h1", "Зарядка", "🏃", HabitSchedule.Daily, 0, checks = days(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11) + (13L..30L).map { today.minusDays(it) }),
        Habit("h2", "Читать 20 минут", "📚", HabitSchedule.Daily, 1, checks = days(1, 2, 3, 4, 5)),
        Habit("h3", "Без сладкого", "💧", HabitSchedule.Daily, 2, checks = days(1, 2, 3)),
        Habit("h4", "Бросковая тренировка", "🏀", HabitSchedule.PerWeek(3), 3, checks = days(2, 6, 7, 8)),
        Habit("h5", "Растяжка", "🧘", HabitSchedule.Weekdays(0b0001010), 4, checks = days(1)),
    )

    private fun show(state: HabitsState) = composeRule.setContent {
        AppTheme { TaskCanvasTheme { HabitsScreen(state) { intents += it } } }
    }

    @Test
    fun listShowsTodayThenNotTodayWithStreaks() {
        show(HabitsState(today, habits))
        composeRule.onNodeWithTag(TAG_HABITS_SUMMARY).assertExists()
        composeRule.onNodeWithText("1 из 4 сегодня").assertExists()
        composeRule.onNodeWithText("Не сегодня").assertExists()
        composeRule.onNodeWithTag("$TAG_HABIT_STREAK:h1", useUnmergedTree = true).assertTextEquals("🔥 12")
        composeRule.onNodeWithTag("$TAG_HABIT_STREAK:h4", useUnmergedTree = true).assertTextEquals("🔥 1 нед.")
        composeRule.onNodeWithContentDescription("Отметить «Читать 20 минут»").performClick()
        composeRule.onNodeWithTag("$TAG_HABIT_ROW:h3").performClick()
        assertEquals(listOf(HabitsIntent.Toggle("h2", today), HabitsIntent.Open("h3")), intents)
        composeRule.onRoot().captureRoboImage("screenshots/habits_light.png")
    }

    @Test
    fun cardShowsStatsCalendarAndActions() {
        show(HabitsState(today, habits, openId = "h1", month = YearMonth.from(today)))
        composeRule.onNodeWithTag("$TAG_HABIT_CARD:streak").assertExists()
        composeRule.onNodeWithText("12 дн.").assertExists()
        composeRule.onNodeWithText("Ноябрь 2026").assertExists()
        // Будущий день не нажимается, прошедший — отмечается.
        composeRule.onNodeWithTag("$TAG_HABIT_DAY:2026-11-12").performClick()
        composeRule.onNodeWithTag("$TAG_HABIT_DAY:2026-11-28").performClick()
        composeRule.onNodeWithTag("$TAG_HABIT_CARD:archive").performClick()
        assertEquals(listOf(HabitsIntent.Toggle("h1", LocalDate.parse("2026-11-12")), HabitsIntent.Archive("h1", true)), intents)
        composeRule.onRoot().captureRoboImage("screenshots/habit_card_light.png")
    }

    @Test
    fun formNeedsNameAndShowsScheduleControls() {
        show(HabitsState(today, habits, form = HabitForm(name = "", kind = ScheduleKind.WEEKDAYS, mask = 0b0010101)))
        composeRule.onNodeWithTag(TAG_HABIT_SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag("$TAG_HABIT_DAY_CHIP:TUESDAY").performClick()
        composeRule.onNodeWithTag("$TAG_HABIT_KIND:PER_WEEK").performClick()
        assertEquals(2, intents.size)
        assertTrue(intents.all { it is HabitsIntent.EditForm })
        composeRule.onRoot().captureRoboImage("screenshots/habit_form_light.png")
    }

    @Test
    fun emptyScreenOffersFirstHabit() {
        show(HabitsState(today, emptyList()))
        composeRule.onNodeWithText("Привычек пока нет").assertExists()
        composeRule.onNodeWithTag(TAG_HABITS_ADD_FIRST).assertIsEnabled().performClick()
        assertEquals(listOf<HabitsIntent>(HabitsIntent.Add), intents)
    }
}
