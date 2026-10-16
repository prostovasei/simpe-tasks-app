package com.prostovasei.simpletasks.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.first
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.feature.all.AllRoute
import com.prostovasei.simpletasks.ui.feature.inbox.InboxRoute
import com.prostovasei.simpletasks.ui.feature.lists.ListsRoute
import com.prostovasei.simpletasks.ui.feature.project.ProjectRoute
import com.prostovasei.simpletasks.ui.feature.completed.CompletedRoute
import com.prostovasei.simpletasks.ui.feature.settings.SettingsRoute
import com.prostovasei.simpletasks.ui.feature.task.QuickAddRoute
import com.prostovasei.simpletasks.ui.feature.task.TaskDetailRoute
import kotlin.reflect.KClass

/** Вкладки нижнего бара (SPEC.md §4). */
enum class Tab(val route: Any, val routeClass: KClass<*>, @param:StringRes val label: Int, val icon: ImageVector) {
    INBOX(InboxDestination, InboxDestination::class, R.string.tab_inbox, Icons.Default.MailOutline),
    ALL(AllDestination, AllDestination::class, R.string.tab_all, Icons.Default.DateRange),
    LISTS(ListsDestination, ListsDestination::class, R.string.tab_lists, Icons.AutoMirrored.Filled.List),
    SETTINGS(SettingsDestination, SettingsDestination::class, R.string.tab_settings, Icons.Default.Settings),
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    openRoute: Any? = null,
    onRouteOpened: () -> Unit = {},
) {
    val backStack by navController.currentBackStackEntryAsState()
    // Уведомление или виджет: открыть задачу или создание задачи поверх текущего экрана.
    // При холодном запуске ждём, пока NavHost построит граф, — иначе navigate() падает.
    LaunchedEffect(openRoute) {
        openRoute ?: return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navController.navigate(openRoute) { launchSingleTop = true }
        onRouteOpened()
    }
    val destination = backStack?.destination
    val currentTab = Tab.entries.firstOrNull { destination.isOn(it) }

    Scaffold(
        containerColor = AppTheme.colors.background,
        bottomBar = {
            // На экране списка нижний бар скрыт: это вложенный экран с кнопкой «Назад».
            if (destination == null || currentTab != null) {
                val selected = currentTab ?: Tab.ALL
                BottomBar(
                    selected = selected,
                    onSelect = { tab -> navController.openTab(tab) },
                    // На «Все» новая задача — на сегодня, на остальных вкладках — во «Входящие».
                    onAdd = { navController.navigate(QuickAddDestination(today = selected == Tab.ALL)) },
                )
            }
        },
    ) { padding ->
        // consumeWindowInsets: экраны с клавиатурой (imePadding) не добавляют отступ панели навигации второй раз.
        // Переходы между экранами без анимации (решение PM).
        NavHost(
            navController,
            startDestination = AllDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            val openTask: (String) -> Unit = { navController.navigate(TaskDetailDestination(it)) }
            composable<InboxDestination> {
                InboxRoute(onOpenTask = openTask)
            }
            composable<AllDestination> {
                AllRoute(onOpenTask = openTask)
            }
            composable<ListsDestination> {
                ListsRoute(onOpenProject = { navController.navigate(ProjectDestination(it)) }, onOpenCompleted = { navController.navigate(CompletedDestination) })
            }
            composable<CompletedDestination> { CompletedRoute(onBack = { navController.popBackStack() }, onOpenTask = openTask) }
            composable<SettingsDestination> { SettingsRoute() }
            composable<ProjectDestination> { entry ->
                val projectId = entry.toRoute<ProjectDestination>().projectId
                ProjectRoute(
                    onBack = { navController.popBackStack() },
                    onAddTask = { navController.navigate(QuickAddDestination(projectId = projectId)) },
                    onOpenTask = openTask,
                )
            }
            composable<QuickAddDestination> { QuickAddRoute(onClose = { navController.popBackStack() }) }
            composable<TaskDetailDestination> { TaskDetailRoute(onClose = { navController.popBackStack() }, onOpenTask = openTask) }
        }
    }
}

/**
 * Плавающая нижняя панель (итерация 13, макет PM): иконки вкладок без подписей, открытая — зелёная,
 * в конце — кнопка «+» новой задачи.
 */
@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, onAdd: () -> Unit = {}) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.navBar)
                .shadow(sizes.navShadow, AppTheme.shapes.nav)
                .background(colors.surface, AppTheme.shapes.nav)
                .padding(start = spacing.sm, end = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Tab.entries.forEach { tab ->
                IconButton(
                    onClick = { onSelect(tab) },
                    modifier = Modifier.size(sizes.navTab).testTag("tab:${tab.name}"),
                ) {
                    Icon(tab.icon, contentDescription = stringResource(tab.label), tint = if (tab == selected) colors.accent else colors.textSecondary)
                }
            }
            Box(
                modifier = Modifier
                    .size(sizes.navAdd)
                    .clip(AppTheme.shapes.extraLarge)
                    .background(colors.accent)
                    .clickable(onClick = onAdd)
                    .testTag(TAG_ADD_TASK),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task), tint = colors.onAccent)
            }
        }
    }
}

/** Переход на вкладку с сохранением состояния каждой вкладки (стандартное поведение нижнего бара). */
private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isOn(tab: Tab): Boolean = this?.hasRoute(tab.routeClass) == true
