package com.prostovasei.simpletasks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.notifications.PrefsReminderStore
import com.prostovasei.simpletasks.ui.navigation.AppNavHost
import com.prostovasei.simpletasks.ui.navigation.QuickAddDestination
import com.prostovasei.simpletasks.ui.navigation.TaskDetailDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderStore: PrefsReminderStore

    /** Экран, который нужно открыть: задача из уведомления или виджета, создание задачи из виджета. */
    private var openRoute by mutableStateOf<Any?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openRoute = routeFrom(intent)
        setContent {
            AskNotificationsOnce()
            AppTheme {
                AppNavHost(openRoute = openRoute, onRouteOpened = { openRoute = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFrom(intent)?.let { openRoute = it }
    }

    /** Android 13+: разрешение на уведомления спрашивается один раз, при первом запуске. */
    @androidx.compose.runtime.Composable
    private fun AskNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted && !reminderStore.permissionAsked) {
                reminderStore.permissionAsked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        /** Экран из интента: задача ([EXTRA_TASK_ID]) или создание задачи ([EXTRA_QUICK_ADD]). */
        fun routeFrom(intent: Intent): Any? = when {
            intent.getBooleanExtra(EXTRA_QUICK_ADD, false) -> QuickAddDestination(
                projectId = intent.getStringExtra(EXTRA_QUICK_ADD_PROJECT),
                today = intent.getBooleanExtra(EXTRA_QUICK_ADD_TODAY, false),
            )
            else -> intent.getStringExtra(EXTRA_TASK_ID)?.let { TaskDetailDestination(it) }
        }

        const val EXTRA_TASK_ID = "open_task_id"
        const val EXTRA_MOVE_OVERDUE = "move_overdue"
        const val EXTRA_QUICK_ADD = "quick_add"
        const val EXTRA_QUICK_ADD_PROJECT = "quick_add_project"
        const val EXTRA_QUICK_ADD_TODAY = "quick_add_today"
    }
}
