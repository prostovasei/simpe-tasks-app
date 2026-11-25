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

/** #95: микрофон рядом с «+» — только на вкладке «Задачи»; на самом узком виджете не помещается и скрыт (К2). */
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

    // #336 (выбор PM): в капсуле только дата, «Разделы», «Настройки» и «+» — микрофона в панели виджета больше нет.
    @Test fun wideTasksBarHasNoMic() = bar(392, WidgetModule.TASKS, present = false)

    @Test fun narrowTasksBarHasNoMic() = bar(180, WidgetModule.TASKS, present = false)

    @Test fun financeHasNoMic() = bar(392, WidgetModule.FINANCE, present = false)
}
