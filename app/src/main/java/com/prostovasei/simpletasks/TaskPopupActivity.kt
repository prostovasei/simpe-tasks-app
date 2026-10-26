package com.prostovasei.simpletasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.feature.task.LocalSheetOrigin
import com.prostovasei.simpletasks.ui.feature.task.QuickAddRoute
import com.prostovasei.simpletasks.ui.feature.task.TaskDetailRoute
import com.prostovasei.simpletasks.ui.navigation.QuickAddDestination
import com.prostovasei.simpletasks.ui.navigation.TaskDetailDestination
import dagger.hilt.android.AndroidEntryPoint

/**
 * Задача или создание задачи поверх рабочего стола (решение PM): открывается из виджета и уведомлений,
 * само приложение не запускается, «Назад» сразу возвращает туда, откуда открыли.
 * Отдельная задача Android (taskAffinity), в списке недавних не показывается.
 */
@AndroidEntryPoint
class TaskPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val start = MainActivity.routeFrom(intent)
        if (start == null) {
            finish()
            return
        }
        setContent {
            AppTheme {
                // Р1: окно растёт из плашки или «+», на которые нажали в виджете (рабочий стол передаёт их место).
                TaskPopup(start, onDismiss = ::finish, origin = intent.sourceBounds)
            }
        }
    }
}

/**
 * Окно поверх рабочего стола: задача и новая задача — та же шторка с сеткой, что и в приложении (итерация 19).
 * Нажатие на затемнение или «Назад» закрывает окно.
 */
@Composable
fun TaskPopup(start: Any, onDismiss: () -> Unit, origin: android.graphics.Rect? = null, navController: NavHostController = rememberNavController()) {
    val close: () -> Unit = { if (!navController.popBackStack()) onDismiss() }
    NavHost(
        navController,
        startDestination = start,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        // Растёт из виджета только первое окно; подзадача, открытая из него, выезжает снизу.
        composable<TaskDetailDestination> {
            CompositionLocalProvider(LocalSheetOrigin provides origin.takeIf { navController.previousBackStackEntry == null }) {
                TaskDetailRoute(onClose = close, onOpenTask = { navController.navigate(TaskDetailDestination(it)) })
            }
        }
        composable<QuickAddDestination> {
            CompositionLocalProvider(LocalSheetOrigin provides origin.takeIf { navController.previousBackStackEntry == null }) { QuickAddRoute(onClose = close) }
        }
    }
}
