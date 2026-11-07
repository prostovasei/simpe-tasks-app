package com.prostovasei.simpletasks.ui.feature.snooze

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

/** Окно «Отложить» из уведомления (итерация 53, макет — доска «Уведомление»). Сейчас — ср 11.11.2026, 16:58 UTC. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h900dp-xhdpi")
class SnoozeDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val time = FakeTimeProvider(now = Instant.parse("2026-11-11T16:58:00Z"), zone = ZoneOffset.UTC)

    private fun state(due: TaskDue) =
        SnoozeState("Созвон с командой", "Созвон с командой · Сегодня, 17:00", snoozeChoices(context, task("t1", title = "Созвон с командой", due = due), time), loaded = true)

    @Test
    fun timedTaskChoices() {
        val picked = mutableListOf<String>()
        composeRule.setContent { AppTheme { SnoozeDialog(state(TaskDue.AtTime(Instant.parse("2026-11-11T17:00:00Z"))), onDismiss = {}) { picked += it.label } } }
        composeRule.onNodeWithText("Через 1 час").assertExists()
        composeRule.onNodeWithText("18:00").assertExists()
        composeRule.onNodeWithText("Вечером").assertExists()
        composeRule.onNodeWithText("чт, 12 ноя, 17:00").assertExists()
        composeRule.onNodeWithText("Выбрать дату и время…").assertExists()
        composeRule.onNode(isDialog()).captureRoboImage("screenshots/snooze_dialog.png")
        composeRule.onNodeWithTag("$TAG_SNOOZE_OPTION:THREE_HOURS").performClick()
        composeRule.onNodeWithTag(TAG_SNOOZE_PICK).performClick()
        assertEquals(listOf("Через 3 часа", "Выбрать дату и время…"), picked)
    }

    @Test
    fun allDayTaskChoices() {
        composeRule.setContent { AppTheme { SnoozeDialog(state(TaskDue.AllDay(java.time.LocalDate.of(2026, 11, 11))), onDismiss = {}) {} } }
        composeRule.onNodeWithText("Послезавтра").assertExists()
        composeRule.onNodeWithText("сб, 14 ноя").assertExists()
        composeRule.onNodeWithText("Выбрать дату…").assertExists()
        composeRule.onNodeWithText("Через 1 час").assertDoesNotExist()
    }
}
