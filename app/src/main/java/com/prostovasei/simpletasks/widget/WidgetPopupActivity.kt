package com.prostovasei.simpletasks.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.ui.feature.finance.AccountWindow
import com.prostovasei.simpletasks.ui.feature.finance.FinanceDialogs
import com.prostovasei.simpletasks.ui.feature.finance.FinanceEffects
import com.prostovasei.simpletasks.ui.feature.finance.FinanceIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceViewModel
import com.prostovasei.simpletasks.ui.feature.health.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** #336: окно выбора раздела виджета — плитки, как в приложении. */
internal fun widgetSectionsIntent(context: Context, id: Int): Intent =
    Intent(context, WidgetPopupActivity::class.java)
        .setData("simpletasks://widget/$id/sections".toUri())
        .putExtra(WidgetActions.EXTRA_WIDGET_ID, id)
        .putExtra("sections", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** #338: «+» виджета — меню «что добавить» (задача, расход, вес; в «Новостях» — ещё «Обновить новости»). */
internal fun widgetAddIntent(context: Context, id: Int): Intent =
    Intent(context, WidgetPopupActivity::class.java)
        .setData("simpletasks://widget/$id/add".toUri())
        .putExtra(WidgetActions.EXTRA_WIDGET_ID, id)
        .putExtra("add", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** #336: разделы виджета — «Главная», три вида задач («Все», «Неделя», «Входящие»), финансы, здоровье, новости, настройки. */
internal val WIDGET_PICKER_ENTRIES: List<com.prostovasei.simpletasks.ui.navigation.PickerEntry> = listOf(
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("HUB", R.drawable.ic_nav_grid, R.string.module_hub, "widget_pick:HUB"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("all", R.drawable.ic_widget_bar_all, R.string.tab_upcoming, "widget_pick:all"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("week", R.drawable.ic_widget_bar_week, R.string.tab_week, "widget_pick:week"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("inbox", R.drawable.ic_widget_bar_inbox, R.string.inbox_list_name, "widget_pick:inbox"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("FINANCE", R.drawable.ic_module_finance, R.string.module_finance, "widget_pick:FINANCE"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("HEALTH", R.drawable.ic_module_health, R.string.module_health, "widget_pick:HEALTH"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("NEWS", R.drawable.ic_module_news, R.string.module_news, "widget_pick:NEWS"),
    com.prostovasei.simpletasks.ui.navigation.PickerEntry("settings", R.drawable.ic_nav_sliders, R.string.settings_title, "widget_pick:settings"),
)

/** Ключ выбранной плитки: модуль виджета, у «Задач» — вид ленты. */
internal fun widgetPickerSelected(module: WidgetModule, filter: WidgetFilter?): String = when (module) {
    WidgetModule.TASKS -> (filter ?: WidgetFilter.All).encode()
    else -> module.name
}

internal fun widgetPopupIntent(context: Context, id: Int, finance: Boolean, health: Boolean = false): Intent =
    Intent(context, WidgetPopupActivity::class.java)
        .setData("simpletasks://widget/$id/popup/$finance/$health".toUri())
        .putExtra(WidgetActions.EXTRA_WIDGET_ID, id)
        .putExtra("finance", finance)
        .putExtra("health", health)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@AndroidEntryPoint
class WidgetPopupActivity : ComponentActivity() {
    private val healthViewModel by viewModels<HealthViewModel>()
    private val financeViewModel by viewModels<FinanceViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(WidgetActions.EXTRA_WIDGET_ID, -1)
        if (id !in WidgetRenderer.widgetIds(this)) {
            finish()
            return
        }
        setContent {
            TaskCanvasTheme {
                if (intent.getBooleanExtra("add", false)) {
                    val module = WidgetSelection.module(this, id)
                    com.prostovasei.simpletasks.ui.navigation.TabPickerSheet(
                        selected = null,
                        entries = com.prostovasei.simpletasks.ui.navigation.addEntries(news = module == WidgetModule.NEWS),
                        onDismiss = ::finish,
                        onPick = { key ->
                            val context = this@WidgetPopupActivity
                            when (key) {
                                com.prostovasei.simpletasks.ui.navigation.ADD_TASK ->
                                    startActivity(quickAddIntent(context, if (module == WidgetModule.TASKS) WidgetSelection.get(context, id) ?: WidgetFilter.All else WidgetFilter.All))
                                com.prostovasei.simpletasks.ui.navigation.ADD_EXPENSE -> startActivity(widgetPopupIntent(context, id, finance = true))
                                com.prostovasei.simpletasks.ui.navigation.ADD_WEIGHT -> startActivity(widgetPopupIntent(context, id, finance = false, health = true))
                                com.prostovasei.simpletasks.ui.navigation.ADD_NEWS -> sendBroadcast(WidgetActions.refreshNews(context, id))
                            }
                            finish()
                        },
                    )
                } else if (intent.getBooleanExtra("sections", false)) {
                    val entries = WIDGET_PICKER_ENTRIES.filter { it.key != "week" || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
                    com.prostovasei.simpletasks.ui.navigation.TabPickerSheet(
                        selected = widgetPickerSelected(WidgetSelection.module(this, id), WidgetSelection.get(this, id)),
                        entries = entries,
                        onDismiss = ::finish,
                        onPick = { key ->
                            lifecycleScope.launch {
                                when (key) {
                                    "settings" -> startActivity(Intent(this@WidgetPopupActivity, com.prostovasei.simpletasks.MainActivity::class.java)
                                        .putExtra(com.prostovasei.simpletasks.MainActivity.EXTRA_OPEN_SETTINGS, true)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                    "all", "week", "inbox" -> {
                                        WidgetSelection.setModule(this@WidgetPopupActivity, id, WidgetModule.TASKS)
                                        WidgetSelection.set(this@WidgetPopupActivity, id, WidgetFilter.decode(key))
                                        WidgetRenderer.render(applicationContext, id, "выбран раздел")
                                    }
                                    else -> WidgetModule.entries.firstOrNull { it.name == key }?.let {
                                        WidgetSelection.setModule(this@WidgetPopupActivity, id, it)
                                        WidgetRenderer.render(applicationContext, id, "выбран раздел")
                                    }
                                }
                                finish()
                            }
                        },
                    )
                } else if (intent.getBooleanExtra("health", false)) {
                    val vm = healthViewModel
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.entries != null) {
                        if (state.entries != null && state.form == null) vm.onIntent(HealthIntent.Add)
                    }
                    LaunchedEffect(vm) {
                        vm.effects.collect { if (it == HealthEffect.EntrySaved) finish() }
                    }
                    state.form?.let { form ->
                        WeightDialog(state, form) { action ->
                            if (action == HealthIntent.CloseForm) {
                                if (!vm.isSaving) finish()
                            } else vm.onIntent(action)
                        }
                    }
                } else if (intent.getBooleanExtra("finance", false)) {
                    val vm = financeViewModel
                    val state by vm.state.collectAsStateWithLifecycle()
                    FinanceEffects(vm, onAccountSaved = ::finish)
                    LaunchedEffect(state.accounts != null) {
                        if (state.accounts != null && state.form == null && state.window == AccountWindow.None) {
                            vm.onIntent(FinanceIntent.Add)
                        }
                    }
                    FinanceDialogs(state) { action ->
                        vm.onIntent(action)
                        if (action == FinanceIntent.CloseForm || action == FinanceIntent.CloseWindow) finish()
                    }
                } else {
                    val modes = listOf(WidgetFilter.All to R.string.widget_all, WidgetFilter.Inbox to R.string.inbox_list_name,
                        WidgetFilter.Week to R.string.tab_week).filter {
                        it.first != WidgetFilter.Week || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    }
                    val current = WidgetSelection.get(this, id)
                    AlertDialog(onDismissRequest = ::finish, title = { Text(getString(R.string.widget_task_view)) }, confirmButton = {},
                        text = {
                            Column {
                                modes.forEach { (mode, label) ->
                                    TextButton(onClick = {
                                        lifecycleScope.launch {
                                            WidgetSelection.set(this@WidgetPopupActivity, id, mode)
                                            WidgetRenderer.render(applicationContext, id, "выбран вид задач")
                                            finish()
                                        }
                                    }) { Text(getString(label) + if (mode == current) "  ✓" else "") }
                                }
                            }
                        })
                }
            }
        }
    }
}
