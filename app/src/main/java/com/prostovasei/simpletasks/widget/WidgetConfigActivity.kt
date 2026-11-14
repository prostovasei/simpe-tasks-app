package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.WeekFilter
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.ui.feature.week.WeekFilterPanel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Фильтр вида «Неделя» этого виджета (#243): какие списки показывать или скрывать. Открывается при добавлении виджета
 * (Android 11 и старше) и через «Настроить» по долгому нажатию на виджет. Вкладки виджета («Все», «Входящие», «Неделя») не меняются.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var projects: ProjectRepository

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Закрыли без выбора — виджет остаётся как был (по умолчанию без фильтра).
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        setContent {
            AppTheme {
                val tree by remember { projects.observeProjectTree() }.collectAsStateWithLifecycle(null)
                var current by remember { mutableStateOf(WeekFilter()) }
                // Сохранённый фильтр читаем один раз при открытии окна.
                remember { current = WidgetSelection.weekFilter(this@WidgetConfigActivity, appWidgetId); true }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.scrim)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::finish)
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    WeekFilterPanel(tree, current, onApply = ::choose)
                }
            }
        }
    }

    private fun choose(filter: WeekFilter) {
        // Окошко закрывается сразу; сохранение и перерисовка виджета доживают до конца
        // в области приложения (у окошка она отменилась бы при закрытии).
        val context = applicationContext
        val id = appWidgetId
        appScope.launch {
            WidgetLog.add(context, "фильтр недели: ${filter.encode()}")
            WidgetSelection.setWeekFilter(context, id, filter)
            WidgetRenderer.render(context, id, "фильтр недели")
        }
        finish()
    }
}
