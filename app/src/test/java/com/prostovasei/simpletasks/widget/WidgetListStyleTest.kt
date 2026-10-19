package com.prostovasei.simpletasks.widget

import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.roundToInt

/** Список виджета (разметки Glance) берёт нашу полоску прокрутки: тонкую и у самого края (доработка 0.19.4). */
@RunWith(RobolectricTestRunner::class)
class WidgetListStyleTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun glanceListsUseThinScrollbarAtTheEdge() {
        val layouts = listOf("glance_list", "glance_list_start_top", "glance_list_expandwidth_wrapheight")
        layouts.forEach { name ->
            val id = context.resources.getIdentifier(name, "layout", context.packageName)
            assertTrue("Разметка $name есть в сборке", id != 0)
            val list = LayoutInflater.from(context).inflate(id, null) as ListView
            assertEquals("$name: полоска поверх края, без отступа", View.SCROLLBARS_OUTSIDE_OVERLAY, list.scrollBarStyle)
            assertEquals("$name: ширина 3 dp", (3 * context.resources.displayMetrics.density).roundToInt(), list.verticalScrollbarWidth)
            assertTrue("$name: вертикальная полоска включена", list.isVerticalScrollBarEnabled)
        }
    }
}
