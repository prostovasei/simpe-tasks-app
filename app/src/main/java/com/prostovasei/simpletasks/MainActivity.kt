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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderStore: PrefsReminderStore

    /** Задача из уведомления, которую нужно открыть. */
    private var openTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openTaskId = intent.getStringExtra(EXTRA_TASK_ID)
        setContent {
            AskNotificationsOnce()
            AppTheme {
                AppNavHost(openTaskId = openTaskId, onTaskOpened = { openTaskId = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TASK_ID)?.let { openTaskId = it }
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
        const val EXTRA_TASK_ID = "open_task_id"
    }
}
