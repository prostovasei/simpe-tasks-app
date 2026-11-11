package com.prostovasei.simpletasks.ui.navigation

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import android.content.Context
import androidx.annotation.DrawableRes
import com.prostovasei.simpletasks.ui.feature.sorted.NoListRoute
import com.prostovasei.simpletasks.ui.feature.sorted.SomedayRoute
import com.prostovasei.simpletasks.ui.feature.sorted.SortCountsHost
import com.prostovasei.simpletasks.domain.model.SortCounts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.ui.feature.wontdo.WontDoRoute
import com.prostovasei.simpletasks.ui.feature.wontdo.WontDoTaskRoute
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.designsystem.components.UndoSnackbarHost
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import com.prostovasei.simpletasks.ui.feature.task.TaskDeletionsHost
import com.prostovasei.simpletasks.ui.common.showUndo
import androidx.compose.runtime.CompositionLocalProvider
import com.prostovasei.simpletasks.ui.feature.bulk.LocalSelectionMode
import com.prostovasei.simpletasks.ui.feature.bulk.LocalBulkActions
import com.prostovasei.simpletasks.ui.feature.bulk.BulkTexts
import com.prostovasei.simpletasks.ui.feature.bulk.BulkActionsHost
import com.prostovasei.simpletasks.ui.feature.all.OverdueMovesHost
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.first
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.feature.all.AllRoute
import com.prostovasei.simpletasks.ui.feature.week.DayRoute
import com.prostovasei.simpletasks.ui.feature.week.WeekRoute
import com.prostovasei.simpletasks.ui.feature.inbox.InboxRoute
import com.prostovasei.simpletasks.ui.feature.lists.ListsRoute
import com.prostovasei.simpletasks.ui.feature.project.ProjectRoute
import com.prostovasei.simpletasks.ui.feature.completed.CompletedRoute
import com.prostovasei.simpletasks.ui.feature.trash.TrashRestoresHost
import com.prostovasei.simpletasks.ui.feature.trash.TrashTaskRoute
import com.prostovasei.simpletasks.ui.feature.trash.TrashRoute
import com.prostovasei.simpletasks.ui.feature.search.SearchRoute
import com.prostovasei.simpletasks.ui.feature.settings.SettingsRoute
import com.prostovasei.simpletasks.ui.feature.task.QuickAddRoute
import com.prostovasei.simpletasks.ui.feature.task.TaskDetailRoute
import com.prostovasei.simpletasks.ui.feature.finance.AppModule
import com.prostovasei.simpletasks.ui.feature.finance.CategoriesRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceBottomBar
import com.prostovasei.simpletasks.ui.feature.finance.FinanceIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceViewModel
import com.prostovasei.simpletasks.ui.feature.finance.ModuleBar
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.reflect.KClass

/**
 * Разделы нижней панели (итерация 55, решение PM, макет iteration55.py): слева «Предстоящее» или «Неделя» (одно место, выбор —
 * удержанием, [NavPrefs]), посередине «+», справа «Списки». Значки — контурные, как в нижней строке виджета.
 */
enum class Tab(val route: Any, val routeClass: KClass<*>, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    UPCOMING(AllDestination, AllDestination::class, R.string.tab_upcoming, R.drawable.ic_widget_all),
    WEEK(WeekDestination, WeekDestination::class, R.string.tab_week, R.drawable.ic_tab_week),
    LISTS(ListsDestination, ListsDestination::class, R.string.tab_lists, R.drawable.ic_tab_lists),
}

/** «Входящие», «Поиск», «Настройки» (итерация 55) — экраны раздела «Списки»: панель видна, выбрана вкладка «Списки». */
private val LISTS_CHILDREN: List<KClass<*>> = listOf(InboxDestination::class, SearchDestination::class, SettingsDestination::class)

/** Какой вид открыт в левом месте панели: «Предстоящее» или «Неделя» (итерация 55; выбор удержанием запоминается). */
object NavPrefs {
    private const val PREFS = "navigation"
    private const val KEY = "left_tab"

    fun left(context: Context): Tab = if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) == Tab.WEEK.name) Tab.WEEK else Tab.UPCOMING

    fun setLeft(context: Context, tab: Tab) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, tab.name) }
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
    // Шторка задачи (новой или созданной) открыта поверх экрана — панель и вкладка остаются от экрана под ней.
    val destination = backStack?.destination?.let { top ->
        if (top.isSheet()) navController.previousBackStackEntry?.destination?.takeUnless { it.isSheet() } else top
    }
    val currentTab = Tab.entries.firstOrNull { destination.isOn(it) }
        ?: Tab.LISTS.takeIf { LISTS_CHILDREN.any { child -> destination?.hasRoute(child) == true } }
    val context = LocalContext.current
    // Левое место панели: «Предстоящее» или «Неделя» — что выбрано удержанием (с ним приложение и открывается).
    var left by remember { mutableStateOf(NavPrefs.left(context)) }
    val startDestination = remember { left.route }

    // Финансы (#217, этап 1 — #221): отдельный модуль со своей нижней панелью. Верхний бар модулей виден на главных экранах
    // задач и в финансах; при возврате в модуль открывается то же место (сохранённое состояние вкладки).
    val inFinance = destination?.hasRoute(FinanceDestination::class) == true
    val onRoot = destination == null || inFinance || Tab.entries.any { destination.isOn(it) }
    val finance = hiltViewModel<FinanceViewModel>()
    val financeState by finance.state.collectAsStateWithLifecycle()
    var lastTaskTab by rememberSaveable { mutableStateOf(left.name) }
    LaunchedEffect(currentTab) { currentTab?.let { lastTaskTab = it.name } }

    // Итерация 52: число неразобранных на вкладке «Входящие».
    val sortCounts by hiltViewModel<SortCountsHost>().counts.collectAsStateWithLifecycle(SortCounts())

    // Задачу удалили в шторке — под ней «Задача удалена · Отмена» (итерация 36); пока приложение не на экране, событие не доходит.
    val deletions = hiltViewModel<TaskDeletionsHost>().deletions
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.task_deleted)
    val undoText = stringResource(R.string.task_undo)
    val undoMessages = com.prostovasei.simpletasks.ui.feature.task.UndoMessages.load()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(deletions) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            deletions.events.collect { deletion -> launch { snackbar.showUndo(deletedText, undoText, UndoKind.Deleted) { deletions.undo(deletion) } } }
        }
    }
    LaunchedEffect(deletions) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            deletions.notices.collect { notice -> launch { snackbar.showUndo(undoMessages.text(notice.message), undoText, notice.kind) { deletions.undo(notice) } } }
        }
    }
    // «Перенести просроченные на сегодня» — под экраном «Перенесено на сегодня: N · Отмена» (итерация 37).
    val moves = hiltViewModel<OverdueMovesHost>().moves
    val movedText = stringResource(R.string.overdue_moved)
    LaunchedEffect(moves) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            moves.events.collect { moved ->
                if (moved.count > 0) launch { snackbar.showUndo(movedText.format(moved.count), undoText, UndoKind.Moved) { moves.undo(moved) } }
            }
        }
    }

    // Вернули из корзины — «Задача восстановлена · Отмена» / «Восстановлено: N · Отмена» (итерация 39).
    val trashRestores = hiltViewModel<TrashRestoresHost>().restores
    val restoredOne = stringResource(R.string.trash_restored)
    val restoredMany = stringResource(R.string.trash_restored_many)
    LaunchedEffect(trashRestores) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            trashRestores.events.collect { restoration ->
                val text = if (restoration.count == 1) restoredOne else restoredMany.format(restoration.count)
                launch { snackbar.showUndo(text, undoText, UndoKind.Restored) { trashRestores.undo(restoration) } }
            }
        }
    }

    // Массовое редактирование (итерация 40): подсказка «Выполнено: N · Отмена» и т. п.; в режиме выбора нижней панели нет — там лента действий.
    val bulk = hiltViewModel<BulkActionsHost>()
    val selectionMode = remember { mutableStateOf(false) }
    val bulkTexts = BulkTexts.load()
    LaunchedEffect(bulk) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            bulk.actions.events.collect { result ->
                launch { snackbar.showUndo(bulkTexts.text(result), undoText, bulkTexts.kind(result)) { bulk.actions.undo(result) } }
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        snackbarHost = { UndoSnackbarHost(snackbar) },
        topBar = {
            if (!selectionMode.value && onRoot) {
                ModuleBar(
                    if (inFinance) AppModule.FINANCE else AppModule.TASKS,
                    onSelect = { module ->
                        when {
                            module == AppModule.FINANCE && !inFinance -> navController.openRoot(FinanceDestination, restore = true)
                            module == AppModule.TASKS && inFinance -> navController.openTab(Tab.valueOf(lastTaskTab))
                        }
                    },
                    // Этап 2: шестерёнка финансов ведёт в категории; с этапа 4 — меню с корзиной и резервной копией.
                    onSettings = if (inFinance) { { navController.navigate(FinanceCategoriesDestination) { launchSingleTop = true } } } else null,
                )
            }
        },
        bottomBar = {
            if (inFinance) {
                FinanceBottomBar(financeState.tab, onSelect = { finance.onIntent(FinanceIntent.SelectTab(it)) }, onAdd = { finance.onIntent(FinanceIntent.Add) })
            } else
            // На экране списка нижний бар скрыт: это вложенный экран с кнопкой «Назад». В режиме выбора — тоже.
            if (!selectionMode.value && (destination == null || currentTab != null)) {
                val selected = currentTab ?: left
                BottomBar(
                    selected = selected,
                    left = left,
                    inboxCount = sortCounts.inbox,
                    onSelect = { tab ->
                        // «Списки» из «Входящих», «Поиска» или «Настроек» — назад к самим «Спискам».
                        if (tab == Tab.LISTS && currentTab == Tab.LISTS && destination?.hasRoute(ListsDestination::class) != true) {
                            navController.popBackStack(ListsDestination, inclusive = false)
                        } else {
                            navController.openTab(tab)
                        }
                    },
                    onPickLeft = { tab ->
                        left = tab
                        NavPrefs.setLeft(context, tab)
                        navController.openTab(tab)
                    },
                    // На «Предстоящем» и «Неделе» новая задача — на сегодня, в «Списках» — во «Входящие».
                    onAdd = { navController.navigate(QuickAddDestination(today = selected != Tab.LISTS)) },
                )
            }
        },
    ) { padding ->
        // consumeWindowInsets: экраны с клавиатурой (imePadding) не добавляют отступ панели навигации второй раз.
        // Переход между экранами — сдвиг вбок в сторону нажатой вкладки; вложенный экран — справа (итерация 15, C2).
        CompositionLocalProvider(LocalBulkActions provides bulk, LocalSelectionMode provides selectionMode) {
        NavHost(
            navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { slideIn(push = true) },
            exitTransition = { slideOut(push = true) },
            popEnterTransition = { slideIn(push = false) },
            popExitTransition = { slideOut(push = false) },
        ) {
            val openTask: (String) -> Unit = { navController.navigate(TaskDetailDestination(it)) }
            composable<InboxDestination> {
                InboxRoute(onOpenTask = openTask)
            }
            composable<AllDestination> {
                AllRoute(onOpenTask = openTask)
            }
            composable<WeekDestination> {
                WeekRoute(onOpenTask = openTask, onOpenDay = { navController.navigate(DayDestination(it.toString())) })
            }
            // Полный список дня: открывается нажатием на строку даты в ячейке недели (итерация 44).
            composable<DayDestination> { DayRoute(onBack = { navController.popBackStack() }, onOpenTask = openTask) }
            composable<ListsDestination> {
                ListsRoute(
                    onOpenInbox = { navController.navigate(InboxDestination) },
                    onOpenSearch = { navController.navigate(SearchDestination) },
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                    onOpenProject = { navController.navigate(ProjectDestination(it)) },
                    onOpenCompleted = { navController.navigate(CompletedDestination) },
                    onOpenTrash = { navController.navigate(TrashDestination) },
                    onOpenWontDo = { navController.navigate(WontDoDestination) },
                    onOpenSomeday = { navController.navigate(SomedayDestination) },
                    onOpenNoList = { navController.navigate(NoListDestination) },
                )
            }
            // Итерация 52: «Когда-нибудь» — «+» сразу с выбором списка; «Без списка» — «+» на сегодня.
            composable<SomedayDestination> {
                SomedayRoute(onBack = { navController.popBackStack() }, onOpenTask = openTask, onAddTask = { navController.navigate(QuickAddDestination(pickList = true)) })
            }
            composable<NoListDestination> {
                NoListRoute(onBack = { navController.popBackStack() }, onOpenTask = openTask, onAddTask = { navController.navigate(QuickAddDestination(today = true)) })
            }
            composable<TrashDestination> { TrashRoute(onBack = { navController.popBackStack() }, onOpenTask = { navController.navigate(TrashTaskDestination(it)) }) }
            // Задача в корзине — шторка только для чтения (итерация 39).
            dialog<TrashTaskDestination>(dialogProperties = SHEET_DIALOG) { TrashTaskRoute(onClose = { navController.popBackStack() }) }
            composable<WontDoDestination> { WontDoRoute(onBack = { navController.popBackStack() }, onOpenTask = { navController.navigate(WontDoTaskDestination(it)) }) }
            // Задача «Не буду делать» — шторка только для чтения (итерация 49).
            dialog<WontDoTaskDestination>(dialogProperties = SHEET_DIALOG) { WontDoTaskRoute(onClose = { navController.popBackStack() }) }
            composable<CompletedDestination> { CompletedRoute(onBack = { navController.popBackStack() }, onOpenTask = openTask) }
            composable<SearchDestination> { SearchRoute(onOpenTask = openTask) }
            composable<SettingsDestination> { SettingsRoute() }
            composable<FinanceDestination> { FinanceRoute(finance) }
            composable<FinanceCategoriesDestination> { CategoriesRoute(finance, onBack = { navController.popBackStack() }) }
            composable<ProjectDestination> { entry ->
                val projectId = entry.toRoute<ProjectDestination>().projectId
                ProjectRoute(
                    onBack = { navController.popBackStack() },
                    onAddTask = { navController.navigate(QuickAddDestination(projectId = projectId)) },
                    onOpenTask = openTask,
                )
            }
            // Новая задача — шторка поверх текущего экрана (итерация 16): экран под ней остаётся виден.
            dialog<QuickAddDestination>(dialogProperties = SHEET_DIALOG) { QuickAddRoute(onClose = { navController.popBackStack() }) }
            // Созданная задача — та же шторка (итерация 19).
            dialog<TaskDetailDestination>(dialogProperties = SHEET_DIALOG) {
                TaskDetailRoute(onClose = { navController.popBackStack() }, onOpenTask = openTask)
            }
        }
        }
    }
}

/**
 * Нижняя панель (итерация 55, решение PM, макет iteration55.py): три места — слева «Предстоящее» или «Неделя» ([left]; удержание —
 * меню из двух пунктов, точка под значком подсказывает, что оно есть), посередине чёрная «+», справа «Списки» с числом неразобранных
 * во «Входящих». Вкладки — как в нижней строке виджета: контурный значок, у выбранной — плашка `selected`. Закреплена внизу на всю
 * ширину (итерация 44), сверху тонкая линия, фон доходит до низа экрана.
 */
@Composable
fun BottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit = {},
    inboxCount: Int = 0,
    left: Tab = Tab.UPCOMING,
    onPickLeft: (Tab) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(sizes.hairline).background(colors.divider))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(sizes.navBar)
                .padding(horizontal = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavTab(left, selected == left || (selected != Tab.LISTS), hint = true, onClick = { onSelect(left) }, onLongClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surface, modifier = Modifier.testTag(TAG_LEFT_MENU)) {
                    listOf(Tab.UPCOMING, Tab.WEEK).forEach { tab ->
                        DropdownMenuItem(
                            text = { Text(stringResource(tab.label), color = colors.textPrimary, fontWeight = if (tab == left) FontWeight.Bold else FontWeight.SemiBold) },
                            leadingIcon = { Icon(painterResource(tab.icon), null, tint = colors.textSecondary, modifier = Modifier.size(sizes.icon)) },
                            trailingIcon = if (tab == left) { { Icon(Icons.Default.Check, null, tint = colors.accent) } } else null,
                            onClick = {
                                menu = false
                                onPickLeft(tab)
                            },
                            modifier = Modifier.testTag("$TAG_LEFT_MENU:${tab.name}"),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(width = sizes.navAddWidth, height = sizes.navAddHeight)
                    .clip(AppTheme.shapes.extraLarge)
                    .background(colors.accent)
                    .clickable(onClick = onAdd)
                    .testTag(TAG_ADD_TASK),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task), tint = colors.onAccent)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavTab(Tab.LISTS, selected == Tab.LISTS, badge = inboxCount, onClick = { onSelect(Tab.LISTS) })
            }
        }
    }
}

/**
 * Вкладка панели: место `size-nav-tab` без плашки (0.59.1, решение PM): выбранная — чёрный значок `accent`, остальные — серый `inactive`.
 * [badge] — число справа сверху, [hint] — точка «есть меню».
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavTab(tab: Tab, active: Boolean, onClick: () -> Unit, badge: Int = 0, hint: Boolean = false, onLongClick: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val sizes = AppTheme.sizes
    val tint = if (active) colors.accent else colors.inactive
    val label = stringResource(tab.label)
    val hintLabel = stringResource(R.string.tab_switch_hint)
    Box(
        Modifier
            .size(width = sizes.navTabWidth, height = sizes.navTabHeight)
            .clip(AppTheme.shapes.large)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick, onLongClickLabel = if (onLongClick != null) hintLabel else null)
            .semantics { contentDescription = label }
            .testTag("tab:${if (hint) TAG_LEFT_TAB else tab.name}"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(tab.icon), contentDescription = null, tint = tint, modifier = Modifier.size(sizes.icon))
        if (hint) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = AppTheme.spacing.xxs).size(sizes.navHintDot).background(tint.copy(alpha = HINT_ALPHA), CircleShape))
        }
        if (badge > 0) {
            Badge(containerColor = colors.accent, contentColor = colors.onAccent, modifier = Modifier.align(Alignment.TopEnd).padding(end = AppTheme.spacing.xs).testTag(TAG_INBOX_BADGE)) { Text(badge.toString()) }
        }
    }
}

private const val HINT_ALPHA = 0.6f

private const val SLIDE_MS = 270

/** Шторка задачи — диалог навигации на весь экран: экран под ней виден, затемнение и «Назад» — свои. */
private val SHEET_DIALOG = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

private fun NavDestination.isSheet() = hasRoute(QuickAddDestination::class) || hasRoute(TaskDetailDestination::class) || hasRoute(TrashTaskDestination::class) || hasRoute(WontDoTaskDestination::class)

/** Порядок экрана слева направо: «Предстоящее» и «Неделя» — слева, «Списки» — справа, вложенные экраны — правее всех. */
private fun NavDestination.order(): Int = when {
    isOn(Tab.UPCOMING) || isOn(Tab.WEEK) -> 0
    isOn(Tab.LISTS) -> 1
    // Финансы — правее всего: модуль приходит справа и уходит вправо.
    hasRoute(FinanceDestination::class) -> 3
    else -> 2
}

/** 1 — новый экран приходит справа, -1 — слева. Между экранами одного уровня: вперёд — справа, «Назад» — слева. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(push: Boolean): Int {
    val delta = targetState.destination.order().compareTo(initialState.destination.order())
    return if (delta != 0) delta else if (push) 1 else -1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideIn(push: Boolean): EnterTransition {
    val dir = direction(push)
    return slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it * dir }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOut(push: Boolean): ExitTransition {
    val dir = direction(push)
    return slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it * dir }
}

/** Переход на вкладку с сохранением состояния каждой вкладки (стандартное поведение нижнего бара). */
// Итерация 55: «Списки» всегда открываются с корня — «Входящие», «Поиск» и «Настройки» внутри не «залипают».
private fun NavHostController.openTab(tab: Tab) = openRoot(tab.route, restore = tab != Tab.LISTS)

/** Главный экран вкладки или модуля: прежний уходит с сохранением состояния, [restore] — вернуть сохранённое состояние нового. */
private fun NavHostController.openRoot(route: Any, restore: Boolean) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restore
    }
}

private fun NavDestination?.isOn(tab: Tab): Boolean = this?.hasRoute(tab.routeClass) == true

const val TAG_INBOX_BADGE = "inbox_badge"

/** Левое место панели («Предстоящее» или «Неделя»): тег `tab:left`, меню удержания — `tab_left_menu`. */
const val TAG_LEFT_TAB = "left"
const val TAG_LEFT_MENU = "tab_left_menu"
