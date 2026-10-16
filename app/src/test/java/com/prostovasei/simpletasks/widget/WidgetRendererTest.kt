package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.view.View
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Отрисовка без сеанса Glance (0.9.2): виджет на «рабочем столе» Robolectric получает картинку сразу,
 * вкладка меняет выбор и сразу перерисовывает виджет.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetRendererTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    private fun View.hasDescription(text: String): Boolean {
        val found = ArrayList<View>()
        findViewsWithText(found, text, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return found.isNotEmpty()
    }

    @Test
    fun renderPutsWidgetOnHomeScreenRightAway() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetRenderer.render(context, id, "тест")
        val view = manager.getViewFor(id)
        listOf("Все", "Входящие", "Другие списки", "Открыть приложение", "Новая задача").forEach {
            assertTrue(it, view.hasDescription(it))
        }
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: all"))
    }

    @Test
    fun tabSwitchesListAndRedraws() = runBlocking {
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetRenderer.render(context, id, "тест")
        WidgetActionReceiver.handle(context, WidgetActions.switch(context, id, WidgetFilter.Inbox))
        assertEquals(WidgetFilter.Inbox, WidgetSelection.get(context, id))
        assertTrue(WidgetLog.read(context).contains("отрисовка $id: inbox"))
    }

    @Test
    fun eachTapHasItsOwnAddress() {
        assertEquals("Разные вкладки — разные нажатия", false, WidgetActions.switch(context, 1, WidgetFilter.All).filterEquals(WidgetActions.switch(context, 1, WidgetFilter.Inbox)))
        assertEquals(false, WidgetActions.complete(context, "a").filterEquals(WidgetActions.complete(context, "b")))
    }

    @Test
    fun deletedWidgetForgetsSelection() {
        WidgetSelection.set(context, 42, WidgetFilter.Inbox)
        TasksWidgetReceiver().onDeleted(context, intArrayOf(42))
        assertEquals(null, WidgetSelection.get(context, 42))
    }
}
