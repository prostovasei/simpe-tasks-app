package com.prostovasei.simpletasks.ui.feature.news

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.notifications.NewsAlertSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** #296: страница «Новости» в настройках — переключатели, время тишины и неактивное состояние. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class NewsSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledShowsQuietPeriodAndPickers() {
        val changes = mutableListOf<NewsAlertSettings>()
        val picks = mutableListOf<String>()
        composeRule.setContent {
            AppTheme { NewsSettingsScreen(NewsAlertSettings(), {}, { changes += it }, { picks += "from" }, { picks += "to" }) }
        }
        composeRule.onNodeWithText("Проверка примерно раз в 2 часа").assertExists()
        composeRule.onNodeWithText("23:00").assertExists()
        composeRule.onNodeWithText("08:00").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/news_settings_light.png")
        composeRule.onNodeWithTag(TAG_NEWS_QUIET_FROM).performClick()
        composeRule.onNodeWithTag(TAG_NEWS_QUIET_TO).performClick()
        composeRule.onNodeWithTag(TAG_NEWS_QUIET_SWITCH).performClick()
        composeRule.onNodeWithTag(TAG_NEWS_ALERTS_SWITCH).performClick()
        assertEquals(listOf("from", "to"), picks)
        assertEquals(listOf(NewsAlertSettings(quietOn = false), NewsAlertSettings(enabled = false)), changes)
    }

    @Test
    fun disabledGreysOutQuiet() {
        composeRule.setContent {
            AppTheme { NewsSettingsScreen(NewsAlertSettings(enabled = false), {}, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Фоновая проверка остановлена").assertExists()
        composeRule.onNodeWithTag(TAG_NEWS_QUIET_SWITCH).assertIsNotEnabled()
        composeRule.onNodeWithTag(TAG_NEWS_QUIET_FROM).assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("screenshots/news_settings_off_light.png")
    }
}
