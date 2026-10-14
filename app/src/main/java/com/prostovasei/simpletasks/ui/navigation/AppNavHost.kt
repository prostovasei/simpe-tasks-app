package com.prostovasei.simpletasks.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.prostovasei.simpletasks.ui.feature.all.AllRoute
import com.prostovasei.simpletasks.ui.feature.inbox.InboxRoute
import com.prostovasei.simpletasks.ui.feature.lists.ListsRoute
import com.prostovasei.simpletasks.ui.feature.project.ProjectRoute
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
                BottomBar(selected = currentTab ?: Tab.ALL) { tab -> navController.openTab(tab) }
            }
        },
    ) { padding ->
        // consumeWindowInsets: экраны с клавиатурой (imePadding) не добавляют отступ панели навигации второй раз.
        NavHost(navController, startDestination = AllDestination, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            val openTask: (String) -> Unit = { navController.navigate(TaskDetailDestination(it)) }
            composable<InboxDestination> {
                InboxRoute(onAddTask = { navController.navigate(QuickAddDestination()) }, onOpenTask = openTask)
            }
            composable<AllDestination> {
                AllRoute(onAddTask = { navController.navigate(QuickAddDestination(today = true)) }, onOpenTask = openTask)
            }
            composable<ListsDestination> { ListsRoute(onOpenProject = { navController.navigate(ProjectDestination(it)) }) }
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

@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = AppTheme.colors
    NavigationBar(containerColor = colors.surface) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    indicatorColor = colors.background,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                ),
                modifier = Modifier.testTag("tab:${tab.name}"),
            )
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
