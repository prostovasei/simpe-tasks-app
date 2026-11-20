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
                if (intent.getBooleanExtra("health", false)) {
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
