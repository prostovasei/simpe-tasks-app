package com.prostovasei.simpletasks.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Полоска напоминания (T-69): стопка до трёх, «ещё N», кнопки и смахивание вниз. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h400dp-xhdpi")
class ReminderStripsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun strip(id: String, title: String) = PopupStrip(id, title, "Сегодня, 18:00 · Дом")

    private fun show(strips: List<PopupStrip>) {
        composeRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background), contentAlignment = Alignment.BottomCenter) {
                    ReminderStrips(
                        strips = strips,
                        onDone = { events += "done:$it" },
                        onTomorrow = { events += "tomorrow:$it" },
                        onOpen = { events += "open:$it" },
                        onDismiss = { events += "dismiss:$it" },
                    )
                }
            }
        }
    }

    @Test
    fun stackShowsThreeAndMore() {
        show(listOf(strip("a", "Забрать посылку"), strip("b", "Позвонить маме"), strip("c", "Оплатить интернет"), strip("d", "Купить хлеб"), strip("e", "Почта")))
        composeRule.onNodeWithText("Забрать посылку").assertExists()
        composeRule.onNodeWithText("Оплатить интернет").assertExists()
        composeRule.onNodeWithText("Купить хлеб").assertDoesNotExist()
        composeRule.onNodeWithText("ещё 2").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/reminder_strips.png")
    }

    @Test
    fun buttonsAndSwipe() {
        show(listOf(strip("a", "Забрать посылку")))
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_TOMORROW:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_OPEN:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP:a").performTouchInput { swipeDown(startY = top, endY = bottom + height * 2) }
        composeRule.waitForIdle()
        assertEquals(listOf("done:a", "tomorrow:a", "open:a", "dismiss:a"), events)
    }
}
