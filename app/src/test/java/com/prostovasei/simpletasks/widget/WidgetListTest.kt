package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Классический список виджета (итерация 42): рамка без строк, строки отдаёт фабрика — по одной, с зонами нажатий;
 * после изменения задач фабрика по «перечитай» отдаёт новые строки.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class WidgetListTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    private fun View.texts(): List<String> = when (this) {
        is TextView -> listOf(text.toString())
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).texts() }
        else -> emptyList()
    }

    private fun addTasks(count: Int) = runBlocking {
        val entry = context.widgetEntryPoint()
        val inbox = entry.projects().observeProjectTree().first().inbox!!.project.id
        val today = entry.time().today()
        repeat(count) { entry.tasks().createTask(NewTask(inbox, "Задача $it", due = TaskDue.AllDay(today.plusDays((it % 10).toLong())))) }
    }

    /** Строка, как её применяет рабочий стол: с внутренней пометкой «элемент списка» (иначе Android не сохраняет нажатия строки). */
    private fun row(factory: WidgetListFactory, position: Int): View {
        val views = factory.getViewAt(position)!!
        val rv = android.widget.RemoteViews::class.java
        val flag = rv.getField("FLAG_WIDGET_IS_COLLECTION_CHILD").getInt(null)
        rv.getMethod("addFlags", Int::class.javaPrimitiveType).invoke(views, flag)
        return views.apply(context, FrameLayout(context))
    }

    @Test
    fun frameHasListAndFactoryGivesRowsWithHits() = runBlocking {
        addTasks(40)
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        WidgetRenderer.render(context, id, "тест")
        assertNotNull("В рамке — классический список", manager.getViewFor(id).findViewById<ListView>(R.id.widget_list))

        val factory = WidgetListFactory(context, id).apply { onCreate(); onDataSetChanged() }
        assertTrue("Строк больше, чем задач: подписи дней и хвост", factory.count > 40)
        val texts = (0 until factory.count).flatMap { row(factory, it).texts() }
        assertTrue("Задача 0" in texts)
        assertTrue("Задача 39" in texts)

        // Строка задачи: зона кружка — «выполнить», остальное — открыть задачу.
        val taskRow = (0 until factory.count).first { "Задача 0" in row(factory, it).texts() }
        // Нажатия строки — «добавки» к общему шаблону списка: в строке, применённой как элемент списка, зоны нажимаются.
        val view = row(factory, taskRow)
        assertTrue("Кружок нажимается", view.findViewById<View>(R.id.hit_one).hasOnClickListeners())
        assertTrue("Плашка нажимается", view.findViewById<View>(R.id.hit_rest).hasOnClickListeners())
        val dayRow = (0 until factory.count).first { row(factory, it).texts().any { t -> t.contains("Сегодня") } }
        assertTrue("Подпись дня не нажимается", !row(factory, dayRow).findViewById<View>(R.id.hit_rest).hasOnClickListeners())
    }

    @Test
    fun rowsFollowDataChanges() = runBlocking {
        addTasks(3)
        val id = manager.createWidget(TasksWidgetReceiver::class.java, androidx.glance.appwidget.R.layout.glance_default_loading_layout)
        val factory = WidgetListFactory(context, id).apply { onCreate(); onDataSetChanged() }
        val before = factory.count
        addTasks(1)
        factory.onDataSetChanged()
        assertEquals("Новая задача — новая строка (день у неё тот же, что у первой)", before + 1, factory.count)
        assertTrue(WidgetLog.read(context).contains("строки $id"))
    }

    @Test
    fun clickActivityTurnsHitIntoAction() {
        val hit = Intent(context, WidgetClickActivity::class.java).setAction(WidgetHits.OPEN).putExtra(WidgetActions.EXTRA_TASK_ID, "t1")
        val activity = Robolectric.buildActivity(WidgetClickActivity::class.java, hit).create().get()
        val started = shadowOf(activity).nextStartedActivity
        assertEquals("simpletasks://task/t1", started.dataString)
        assertTrue(activity.isFinishing)

        val check = Intent(context, WidgetClickActivity::class.java).setAction(WidgetActions.COMPLETE).putExtra(WidgetActions.EXTRA_TASK_ID, "t1")
        Robolectric.buildActivity(WidgetClickActivity::class.java, check).create()
        val sent = shadowOf(context).broadcastIntents.last()
        assertEquals(WidgetActions.COMPLETE, sent.action)
        assertEquals("t1", sent.getStringExtra(WidgetActions.EXTRA_TASK_ID))
    }
}
