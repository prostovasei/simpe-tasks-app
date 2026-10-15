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
                TaskPopup(start, onDismiss = ::finish)
            }
        }
    }
}

/** Затемнённый рабочий стол и карточка снизу; нажатие на затемнение закрывает окно. */
@Composable
fun TaskPopup(start: Any, onDismiss: () -> Unit, navController: NavHostController = rememberNavController()) {
    val close: () -> Unit = { if (!navController.popBackStack()) onDismiss() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .systemBarsPadding()
            .testTag(TAG_POPUP_SCRIM),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(POPUP_HEIGHT)
                .clip(AppTheme.shapes.extraLargeTop)
                .background(AppTheme.colors.background)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            NavHost(
                navController,
                startDestination = start,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable<TaskDetailDestination> {
                    TaskDetailRoute(onClose = close, onOpenTask = { navController.navigate(TaskDetailDestination(it)) })
                }
                composable<QuickAddDestination> { QuickAddRoute(onClose = close) }
            }
        }
    }
}

/** Доля высоты экрана под карточкой: сверху остаётся полоска затемнённого рабочего стола. */
private const val POPUP_HEIGHT = 0.92f

const val TAG_POPUP_SCRIM = "popup_scrim"
