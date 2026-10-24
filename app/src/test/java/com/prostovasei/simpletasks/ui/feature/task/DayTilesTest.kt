package com.prostovasei.simpletasks.ui.feature.task

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Плитки дней в окне задачи (0.27.1): дата дальше двух недель не уводит ленту в конец. */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class DayTilesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 24)

    @Test
    fun dateBeyondTwoWeeksKeepsStripAtToday() {
        composeRule.setContent {
            AppTheme { DayTiles(today, today.plusDays(25), emptyList(), onDay = {}, onCalendar = {}, onClear = {}) }
        }
        composeRule.waitForIdle()
        // Выбранная 18 ноя — отдельной плиткой; лента — с сегодняшних 24 и 25 окт, а не с конца (05–06 ноя).
        composeRule.onNodeWithText("18").assertIsDisplayed()
        composeRule.onNodeWithText("24").assertIsDisplayed()
        composeRule.onNodeWithText("25").assertIsDisplayed()
    }
}
