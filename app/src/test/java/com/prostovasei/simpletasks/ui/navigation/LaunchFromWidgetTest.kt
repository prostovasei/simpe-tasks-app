package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.TaskPopupActivity
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_SCRIM
import com.prostovasei.simpletasks.ui.feature.task.TAG_DATE_TEXT
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_DELETE
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_MENU
import com.prostovasei.simpletasks.ui.feature.task.TAG_DETAIL_TITLE
import com.prostovasei.simpletasks.ui.feature.task.TAG_QUICK_TITLE
import com.prostovasei.simpletasks.widget.WidgetFilter
import com.prostovasei.simpletasks.widget.openTaskIntent
import com.prostovasei.simpletasks.widget.quickAddIntent
import com.prostovasei.simpletasks.widget.widgetEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper

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
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) { composeRule.onAllNodesWithTag(TAG_QUICK_TITLE).fetchSemanticsNodes().isNotEmpty() }
            // «+» в «Все» — задача на сегодня: в поле даты дата, а не «Без даты и времени».
            composeRule.onNodeWithTag(TAG_DATE_TEXT, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithText("Без даты и времени").assertDoesNotExist()
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
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(TAG_DETAIL_TITLE).assertTextEquals("Позвонить маме")
            // Нажатие на затемнённый рабочий стол закрывает окно — приложение не открывается. С 0.23.3 окно — карточка
            // над местом клавиатуры, поэтому нажимаем на затемнение под ней. Окно сначала уезжает вниз, потом закрывается.
            composeRule.onNodeWithTag(TAG_QUICK_SCRIM).performTouchInput { click(Offset(centerX, bottom - 5f)) }
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) { checkNotNull(activity).isFinishing }
            assertTrue(checkNotNull(activity).isFinishing)
        }
    }

    /** Итерация 36: из виджета удаление без вопроса; окно остаётся с подсказкой «Задача удалена · Отмена», «Отмена» возвращает задачу. */
    @Test
    fun deleteFromPopupShowsUndoAndUndoRestoresTask() {
        val (id, tasks) = runBlocking {
            val entry = context.widgetEntryPoint()
            entry.tasks().createTask(NewTask(entry.projects().getInbox().id, "Удалить и вернуть")) to entry.tasks()
        }
        ActivityScenario.launch<TaskPopupActivity>(openTaskIntent(context, id)).use { scenario ->
            var activity: TaskPopupActivity? = null
            scenario.onActivity { activity = it }
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) { composeRule.onAllNodesWithTag(TAG_DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(TAG_DETAIL_MENU).performClick()
            composeRule.onNodeWithTag(TAG_DETAIL_DELETE).performClick()
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) {
                shadowOf(Looper.getMainLooper()).idle()
                composeRule.onAllNodesWithText("Задача удалена").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue("Пока подсказка на экране, окно не закрыто", !checkNotNull(activity).isFinishing)
            assertEquals(null, runBlocking { tasks.getTask(id) })
            composeRule.onNodeWithText("Отмена").performClick()
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) {
                shadowOf(Looper.getMainLooper()).idle()
                runBlocking { tasks.getTask(id) } != null
            }
            assertEquals("Удалить и вернуть", runBlocking { tasks.getTask(id) }?.title)
        }
    }

    @Test
    fun missingTaskClosesWithoutCrash() {
        ActivityScenario.launch<TaskPopupActivity>(openTaskIntent(context, "нет-такой")).use { scenario ->
            var activity: TaskPopupActivity? = null
            scenario.onActivity { activity = it }
            // Закрытие приходит из базы (задачи нет) и проходит через очередь Android — прокручиваем её явно,
            // с запасом по времени на медленном CI.
            composeRule.waitUntil(LAUNCH_TIMEOUT_MS) {
                shadowOf(Looper.getMainLooper()).idle()
                checkNotNull(activity).isFinishing
            }
        }
    }

    private companion object {
        /** Холодный запуск окна с Hilt и базой на CI бывает медленным. */
        const val LAUNCH_TIMEOUT_MS = 20_000L
    }
}
