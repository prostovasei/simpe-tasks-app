package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** #336: плавающая капсула — дата, разделы, настройки, «+»; выбор раздела плитками. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h600dp-xhdpi")
class FloatingNavBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capsuleOpensSectionsAndPickerShowsTiles() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            AppTheme {
                TaskCanvasTheme {
                    Column {
                        TabPickerTiles(selected = Tab.WEEK.name, onPick = { events += it })
                        FloatingNavBar(
                            today = LocalDate.of(2026, 11, 25),
                            onHub = { events += "hub" }, onSettings = { events += "settings" }, onAdd = { events += "add" },
                            addTag = "add", addLabel = R.string.add_task,
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag(TAG_NAV_DATE).assertExists()
        composeRule.onNodeWithTag(TAG_NAV_HUB).performClick()
        composeRule.onNodeWithTag(TAG_MODULE_SETTINGS).performClick()
        composeRule.onNodeWithTag("add").performClick()
        composeRule.onNodeWithTag("tab:WEEK").assertIsSelected()
        composeRule.onNodeWithTag("module:FINANCE").performClick()
        composeRule.onNodeWithContentDescription("Настройки", useUnmergedTree = true)
        assertEquals(listOf("hub", "settings", "add", "finance"), events)
        composeRule.onRoot().captureRoboImage("screenshots/nav_capsule_picker_light.png")
    }
}
