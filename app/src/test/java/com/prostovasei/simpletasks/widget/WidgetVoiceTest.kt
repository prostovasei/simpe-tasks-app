package com.prostovasei.simpletasks.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.MainActivity
import com.prostovasei.simpletasks.TaskPopupActivity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #95 → #338: микрофон в капсуле виджета — открывает окно голосового ввода. */
@RunWith(RobolectricTestRunner::class)
class WidgetVoiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val model = WidgetModel(WidgetFilter.All, null, LocalDate.of(2026, 11, 22), emptyList())
    private val voice = "Создать задачу голосом"

    private fun bar(width: Int, module: WidgetModule, present: Boolean) = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(width.dp, 400.dp))
        setContext(context)
        provideComposable { BottomBar(model, appWidgetId = 7, imageButtons = false, module = module) }
        onAllNodes(hasContentDescription(voice)).assertCountEquals(if (present) 1 else 0)
    }

    // #338 (выбор PM): микрофон вернулся в капсулу — во всех разделах и на любой ширине.
    @Test fun wideTasksBarHasMic() = bar(392, WidgetModule.TASKS, present = true)

    @Test fun narrowTasksBarHasMic() = bar(180, WidgetModule.TASKS, present = true)

    @Test fun financeHasMic() = bar(392, WidgetModule.FINANCE, present = true)

    // Окно голосового ввода поверх рабочего стола осталось (адрес `simpletasks://voice/…`): кнопки в капсуле нет, путь проверяется отдельно.
    @Test
    fun voiceIntentOpensPopupLikePlus() {
        val all = voiceIntent(context, WidgetFilter.All)
        assertEquals(TaskPopupActivity::class.java.name, all.component?.className)
        assertTrue(all.getBooleanExtra(MainActivity.EXTRA_VOICE, false))
        assertTrue("Вид «Все» — на сегодня, как «+»", all.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, false))
        val inbox = voiceIntent(context, WidgetFilter.Inbox)
        assertEquals(false, inbox.getBooleanExtra(MainActivity.EXTRA_QUICK_ADD_TODAY, true))
        assertTrue("У каждого вида свой адрес", all.data != inbox.data)
    }
}
