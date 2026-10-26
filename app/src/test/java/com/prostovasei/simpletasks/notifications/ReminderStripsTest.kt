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
import androidx.compose.ui.test.swipeRight
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Полоска напоминания (T-69): стопка до трёх, «ещё N», кнопки, смахивание вправо (0.29.2) и анимации (0.28.1). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h400dp-xhdpi")
class ReminderStripsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun strip(id: String, title: String) = PopupStrip(id, title, "Сегодня, 18:00 · Дом", "Завтра, 18:00")

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
        // Выполнить и «на завтра» срабатывают после анимации, поэтому у каждого действия своя полоска.
        show(listOf(strip("a", "Забрать посылку"), strip("b", "Позвонить маме"), strip("c", "Оплатить интернет")))
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_TOMORROW:b").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_OPEN:c").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP:c").performTouchInput { swipeRight(startX = left, endX = right) }
        composeRule.waitForIdle()
        assertEquals(listOf("done:a", "tomorrow:b", "open:c", "dismiss:c"), events)
    }

    @Test
    fun tomorrowFlipsToLaterThenActs() {
        show(listOf(strip("a", "Забрать посылку")))
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("$TAG_POPUP_TOMORROW:a").performClick()
        composeRule.mainClock.advanceTimeBy(FLIPPED_MS)
        composeRule.onNodeWithText("Завтра, 18:00").assertExists()
        assertEquals(emptyList<String>(), events)
        composeRule.mainClock.advanceTimeBy(FINISHED_MS)
        assertEquals(listOf("tomorrow:a"), events)
    }

    @Test
    fun doneActsOnceAfterDust() {
        show(listOf(strip("a", "Забрать посылку")))
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").performClick()
        composeRule.mainClock.advanceTimeBy(HALF_DUST_MS)
        assertEquals(emptyList<String>(), events)
        composeRule.mainClock.advanceTimeBy(FINISHED_MS)
        assertEquals(listOf("done:a"), events)
    }

    @Test
    fun undoStripUndoesOrExpires() {
        // Без автопрокрутки времени: иначе ожидание покоя доиграет 3 секунды «Отменить» до нажатия.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                ReminderStrips(
                    strips = listOf(strip("a", "Забрать посылку").copy(undo = "Выполнено"), strip("b", "Позвонить маме").copy(undo = "Завтра, 18:00")),
                    onDone = {},
                    onTomorrow = {},
                    onOpen = {},
                    onDismiss = {},
                    onUndo = { events += "undo:$it" },
                    onExpire = { events += "expire:$it" },
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(UNDO_SHOWN_MS)
        composeRule.onNodeWithText("Выполнено").assertExists()
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_POPUP_UNDO:a").performClick()
        assertEquals(listOf("undo:a"), events)
        composeRule.mainClock.advanceTimeBy(UNDO_GONE_MS)
        assertEquals(setOf("undo:a", "expire:a", "expire:b"), events.toSet())
    }

    private companion object {
        const val UNDO_SHOWN_MS = 300L
        const val UNDO_GONE_MS = 4000L
        const val FLIPPED_MS = 600L
        const val HALF_DUST_MS = 300L
        const val FINISHED_MS = 2000L
    }
}
