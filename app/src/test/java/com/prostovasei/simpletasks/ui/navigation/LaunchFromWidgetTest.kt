package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextEquals
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.ui.feature.task.TAG_CHIP_DATE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_TITLE
import com.prostovasei.simpletasks.widget.WidgetFilter
import com.prostovasei.simpletasks.widget.quickAddIntent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** «+» в виджете открывает приложение сразу на создании задачи. */
@Suppress("DEPRECATION") // v1-правило: поле ввода с мигающим курсором вешает v2
@RunWith(RobolectricTestRunner::class)
class LaunchFromWidgetTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun plusFromAllWidgetOpensQuickAddForToday() {
        val intent = quickAddIntent(ApplicationProvider.getApplicationContext(), WidgetFilter.All)
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(TAG_CHIP_DATE).assertTextEquals("Сегодня")
        }
    }

    /** Нажатие на уведомление или задачу в виджете при закрытом приложении открывает экран задачи. */
    @Test
    fun taskFromNotificationOpensOnColdStart() {
        val intent = com.prostovasei.simpletasks.widget.openTaskIntent(ApplicationProvider.getApplicationContext(), "нет-такой")
        ActivityScenario.launch<MainActivity>(intent).use {
            // Задачи нет — экран задачи сам закрывается и возвращает на «Все». Главное — без падения.
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("tab:ALL").fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
