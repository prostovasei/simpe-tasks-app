package com.prostovasei.simpletasks.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.designsystem.components.ColorDot
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.domain.repository.ProjectRepository
import com.prostovasei.simpletasks.ui.feature.task.ProjectOption
import com.prostovasei.simpletasks.ui.feature.task.options
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Выбор, что показывает виджет: «Все», «Входящие» или список. Открывается при добавлении виджета
 * (Android 11 и старше), нажатием на заголовок виджета и через «Настроить» по долгому нажатию.
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
        // Закрыли без выбора — виджет остаётся со «Все» (решение PM: по умолчанию «Все»).
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        setContent {
            AppTheme {
                val options by remember { projects.observeProjectTree().map { it.options() } }.collectAsStateWithLifecycle(emptyList())
                var current by remember { mutableStateOf<WidgetFilter>(WidgetFilter.All) }
                LaunchedEffect(Unit) { current = currentFilter() }
                WidgetPicker(options, current, onPick = ::choose, onDismiss = ::finish)
            }
        }
    }

    private suspend fun currentFilter(): WidgetFilter {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        val prefs: Preferences = getAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId)
        return WidgetFilter.decode(prefs[TasksWidget.FILTER_KEY])
    }

    private fun choose(filter: WidgetFilter) {
        // Окошко закрывается сразу; сохранение и перерисовка виджета доживают до конца
        // в области приложения (у окошка она отменилась бы при закрытии).
        val context = applicationContext
        val id = appWidgetId
        appScope.launch {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
            WidgetLog.add(context, "окошко списков: ${filter.encode()}")
            updateAppWidgetState(context, glanceId) { it[TasksWidget.FILTER_KEY] = filter.encode() }
            WidgetRefresh.one(context, glanceId, "окошко списков")
        }
        finish()
    }
}

/** Окошко поверх рабочего стола: «Все», «Входящие», затем списки. Нажатие мимо — закрыть. */
@Composable
fun WidgetPicker(options: List<ProjectOption>, current: WidgetFilter, onPick: (WidgetFilter) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .systemBarsPadding()
            .padding(spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, AppTheme.shapes.extraLarge)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(vertical = spacing.md),
        ) {
            Text(
                stringResource(R.string.widget_pick_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
            )
            Column(Modifier.heightIn(max = AppTheme.sizes.pickerMaxHeight).verticalScroll(rememberScrollState())) {
                PickerRow(stringResource(R.string.widget_all), stringResource(R.string.widget_all_hint), null, current == WidgetFilter.All, TAG_PICK_ALL) {
                    onPick(WidgetFilter.All)
                }
                PickerRow(stringResource(R.string.inbox_list_name), null, null, current == WidgetFilter.Inbox, TAG_PICK_INBOX) {
                    onPick(WidgetFilter.Inbox)
                }
                val lists = options.filterNot { it.isInbox }
                if (lists.isNotEmpty()) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = spacing.xs))
                lists.forEach { option ->
                    val filter = WidgetFilter.ProjectList(option.id)
                    PickerRow(option.name, null, option.color, current == filter, "$TAG_PICK_LIST:${option.id}") { onPick(filter) }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(title: String, hint: String?, color: String?, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = spacing.lg, vertical = spacing.sm).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        parseListColor(color)?.let { ColorDot(it) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else null, color = colors.textPrimary)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
        }
        if (selected) Icon(Icons.Default.Check, null, tint = colors.accent) else Spacer(Modifier.size(AppTheme.sizes.icon))
    }
}

const val TAG_PICK_ALL = "pick_all"
const val TAG_PICK_INBOX = "pick_inbox"
const val TAG_PICK_LIST = "pick_list"
