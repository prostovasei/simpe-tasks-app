package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.TAG_POPUP_SCRIM
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.ui.feature.task.TAG_CHIP_DATE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_TITLE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_TITLE
import com.prostovasei.simpletasks.widget.WidgetFilter
import com.prostovasei.simpletasks.widget.openTaskIntent
import com.prostovasei.simpletasks.widget.quickAddIntent
import com.prostovasei.simpletasks.widget.widgetEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Виджет и уведомления открывают задачу и создание задачи поверх рабочего стола, без приложения (решение PM). */
@Suppress("DEPRECATION") // v1-правило: поле ввода с мигающим курсором вешает v2
@RunWith(RobolectricTestRunner::class)
class LaunchFromWidgetTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun plusFromAllWidgetOpensQuickAddForToday() {
        ActivityScenario.launch<TaskPopupActivity>(quickAddIntent(context, WidgetFilter.All)).use {
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(TAG_CHIP_DATE).assertTextEquals("Сегодня")
        }
    }

    @Test
    fun taskOpensOverHomeScreenAndBackCloses() {
        val id = runBlocking {
            val entry = context.widgetEntryPoint()
            entry.tasks().createTask(NewTask(entry.projects().getInbox().id, "Позвонить маме"))
        }
        ActivityScenario.launch<TaskPopupActivity>(openTaskIntent(context, id)).use { scenario ->
            var activity: TaskPopupActivity? = null
            scenario.onActivity { activity = it }
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(TAG_DETAIL_TITLE).assertTextEquals("Позвонить маме")
            // Нажатие на затемнённый рабочий стол закрывает окно — приложение не открывается.
            composeRule.onNodeWithTag(TAG_POPUP_SCRIM).performTouchInput { click(Offset(centerX, top + 5f)) }
            assertTrue(checkNotNull(activity).isFinishing)
        }
    }

    @Test
    fun missingTaskClosesWithoutCrash() {
        ActivityScenario.launch<TaskPopupActivity>(openTaskIntent(context, "нет-такой")).use { scenario ->
            var activity: TaskPopupActivity? = null
            scenario.onActivity { activity = it }
            composeRule.waitUntil(5_000) { checkNotNull(activity).isFinishing }
        }
    }
}
