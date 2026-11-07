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
import com.prostovasei.simpletasks.domain.model.SnoozeKind
import com.prostovasei.simpletasks.domain.model.SnoozeOption
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.ui.feature.snooze.SnoozeChoice
import com.prostovasei.simpletasks.ui.feature.snooze.TAG_SNOOZE_OPTION
import com.prostovasei.simpletasks.ui.feature.snooze.TAG_SNOOZE_PICK
import java.time.Instant
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

    private fun strip(id: String, title: String) = PopupStrip(id, title, "Сегодня, 18:00 · Дом")

    private val due = TaskDue.AtTime(Instant.parse("2026-11-11T15:00:00Z"))
    private val menu = listOf(
        SnoozeChoice("Через 1 час", "18:00", SnoozeOption(SnoozeKind.HOUR, due, null), "$TAG_SNOOZE_OPTION:HOUR"),
        SnoozeChoice("Через 3 часа", "20:00", SnoozeOption(SnoozeKind.THREE_HOURS, due, null), "$TAG_SNOOZE_OPTION:THREE_HOURS"),
        SnoozeChoice("Вечером", "19:00", SnoozeOption(SnoozeKind.EVENING, due, null), "$TAG_SNOOZE_OPTION:EVENING"),
        SnoozeChoice("Завтра", "чт, 12 ноя, 17:00", SnoozeOption(SnoozeKind.TOMORROW, due, null), "$TAG_SNOOZE_OPTION:TOMORROW"),
        SnoozeChoice("Выбрать дату и время…", "", null, TAG_SNOOZE_PICK),
    )

    private fun show(strips: List<PopupStrip>) {
        composeRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background), contentAlignment = Alignment.BottomCenter) {
                    ReminderStrips(
                        strips = strips,
                        onDone = { events += "done:$it" },
                        onSnooze = { events += "snooze:$it" },
                        onSnoozeChoice = { id, choice -> events += "choice:$id:${choice.label}" },
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
        // Выполнить срабатывает после анимации, поэтому у каждого действия своя полоска; «Отложить» — сразу (открывает меню).
        show(listOf(strip("a", "Забрать посылку"), strip("b", "Позвонить маме"), strip("c", "Оплатить интернет")))
        composeRule.onNodeWithTag("$TAG_POPUP_DONE:a").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_SNOOZE:b").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP_OPEN:c").performClick()
        composeRule.onNodeWithTag("$TAG_POPUP:c").performTouchInput { swipeRight(startX = left, endX = right) }
        composeRule.waitForIdle()
        assertEquals(listOf("done:a", "snooze:b", "open:c", "dismiss:c"), events)
    }

    /** Итерация 53: меню «Отложить» над полоской — варианты с новым сроком справа, нажатие отдаёт выбранный вариант. */
    @Test
    fun snoozeMenuOffersChoices() {
        show(listOf(strip("a", "Созвон с командой").copy(snooze = menu)))
        composeRule.onNodeWithTag("$TAG_POPUP_SNOOZE_MENU:a").assertExists()
        composeRule.onNodeWithText("ОТЛОЖИТЬ «СОЗВОН С КОМАНДОЙ»").assertExists()
        composeRule.onNodeWithText("чт, 12 ноя, 17:00").assertExists()
        composeRule.onRoot().captureRoboImage("screenshots/reminder_snooze_menu.png")
        composeRule.onNodeWithTag("$TAG_SNOOZE_OPTION:HOUR:a").performClick()
        composeRule.onNodeWithTag("$TAG_SNOOZE_PICK:a").performClick()
        assertEquals(listOf("choice:a:Через 1 час", "choice:a:Выбрать дату и время…"), events)
    }

    @Test
    fun snoozeMenuDark() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background), contentAlignment = Alignment.BottomCenter) {
                    ReminderStrips(listOf(strip("a", "Созвон с командой").copy(snooze = menu)), onDone = {}, onSnooze = {}, onOpen = {}, onDismiss = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/reminder_snooze_menu_dark.png")
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
                    strips = listOf(strip("a", "Забрать посылку").copy(undo = "Выполнено"), strip("b", "Позвонить маме").copy(undo = "Отложено на 18:00")),
                    onDone = {},
                    onSnooze = {},
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
        const val HALF_DUST_MS = 300L
        const val FINISHED_MS = 2000L
    }
}
