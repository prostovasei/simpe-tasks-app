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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.runtime.rememberCoroutineScope
import com.prostovasei.simpletasks.ui.feature.news.NewsScreen
import com.prostovasei.simpletasks.ui.feature.news.NewsSettingsRoute
import com.prostovasei.simpletasks.widget.WidgetRenderer
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
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.ui.common.TAG_ADD_TASK
import com.prostovasei.simpletasks.ui.feature.all.AllRoute
import com.prostovasei.simpletasks.ui.feature.week.DayRoute
import com.prostovasei.simpletasks.ui.feature.week.WeekFilterDialog
import com.prostovasei.simpletasks.ui.feature.week.WeekFilterViewModel
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
import com.prostovasei.simpletasks.ui.feature.settings.AppSettingsRoute
import com.prostovasei.simpletasks.ui.feature.settings.AppInfoRoute
import com.prostovasei.simpletasks.ui.feature.task.QuickAddRoute
import com.prostovasei.simpletasks.ui.feature.task.TaskDetailRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceTab
import com.prostovasei.simpletasks.ui.feature.finance.TAG_FINANCE_ADD
import com.prostovasei.simpletasks.ui.feature.finance.TAG_FINANCE_TAB
import com.prostovasei.simpletasks.ui.feature.finance.BankSettingsRoute
import com.prostovasei.simpletasks.ui.feature.finance.CategoriesRoute
import com.prostovasei.simpletasks.ui.feature.finance.UnsortedRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceEffects
import com.prostovasei.simpletasks.ui.feature.hub.HubRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceSettingsRoute
import com.prostovasei.simpletasks.ui.feature.finance.PlacesRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceTrashRoute
import com.prostovasei.simpletasks.ui.feature.finance.FinanceViewModel
import com.prostovasei.simpletasks.ui.feature.health.HealthBackupRoute
import com.prostovasei.simpletasks.ui.feature.health.HealthIntent
import com.prostovasei.simpletasks.ui.feature.health.HealthRoute
import com.prostovasei.simpletasks.ui.feature.health.HealthSettingsRoute
import com.prostovasei.simpletasks.ui.feature.health.HealthViewModel
import com.prostovasei.simpletasks.ui.feature.health.TAG_HEALTH_ADD
import com.prostovasei.simpletasks.ui.feature.health.TAG_HEALTH_TAB
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

/** «Входящие» и «Поиск» (итерация 55) — экраны раздела «Списки»: панель видна, выбрана страница «Списки». Настройки (#241) — отдельные страницы без панели. */
private val LISTS_CHILDREN: List<KClass<*>> = listOf(InboxDestination::class, SearchDestination::class, ProjectDestination::class)

/** Страницы модуля «Задачи» в нижней панели (#241): все три всегда на виду; теги `tab:<имя>` прежние. */
internal val TASK_PAGES: List<BarPage> = Tab.entries.map { BarPage(it.name, it.icon, it.label, "tab:${it.name}") }

/** Нижняя панель модуля «Задачи» (#241): три страницы, «+» — новая задача. */
@Composable
internal fun TasksBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit,
    onModule: (AppModule) -> Unit,
    onSettings: () -> Unit,
    onWeekFilter: () -> Unit = {},
) = AppBottomBar(
    TASK_PAGES, selected.name, { onSelect(Tab.valueOf(it)) }, onAdd, TAG_ADD_TASK, R.string.add_task, AppModule.TASKS, onModule, onSettings,
    onPageLongClick = { if (it == Tab.WEEK.name) onWeekFilter() },
)

/** Нижняя панель модуля «Финансы» (#241): единый экран и «+» для новой операции. */
@Composable
internal fun FinanceBottomBar(
    onSelect: (FinanceTab) -> Unit,
    onAdd: () -> Unit,
    onModule: (AppModule) -> Unit,
    onSettings: () -> Unit,
) = TaskCanvasTheme {
    AppBottomBar(FINANCE_PAGES, FinanceTab.ANALYTICS.name, { onSelect(FinanceTab.valueOf(it)) },
        onAdd, TAG_FINANCE_ADD, R.string.finance_add, AppModule.FINANCE, onModule, onSettings)
}

/** Нижняя панель модуля «Здоровье» (#257): одна страница «Вес», «+» — новое взвешивание. */
@Composable
internal fun HealthBottomBar(onAdd: () -> Unit, onModule: (AppModule) -> Unit, onSettings: () -> Unit) =
    AppBottomBar(HEALTH_PAGES, HEALTH_WEIGHT_PAGE, {}, onAdd, TAG_HEALTH_ADD, R.string.health_add, AppModule.HEALTH, onModule, onSettings)

private const val HEALTH_WEIGHT_PAGE = "WEIGHT"

/** Страницы модуля «Здоровье» (#257): пока одна — «Вес»; позже рядом появятся другие показатели. */
internal val HEALTH_PAGES: List<BarPage> = listOf(BarPage(HEALTH_WEIGHT_PAGE, R.drawable.ic_health_weight, R.string.health_page_weight, "$TAG_HEALTH_TAB:$HEALTH_WEIGHT_PAGE"))

/** Страницы модуля «Финансы» в нижней панели (#241): «Аналитика». */
internal val FINANCE_PAGES: List<BarPage> = listOf(
    BarPage(FinanceTab.ANALYTICS.name, R.drawable.ic_finance_tab_analytics, R.string.finance_tab_analytics, "$TAG_FINANCE_TAB:${FinanceTab.ANALYTICS.name}"),
)

/** С какой страницы открывается приложение: «Предстоящее» или «Неделя» — та, что была выбрана последней (#241; раньше — выбор удержанием). */
object NavPrefs {
    private const val PREFS = "navigation"
    private const val KEY = "left_tab"

    fun left(context: Context): Tab = if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) == Tab.WEEK.name) Tab.WEEK else Tab.UPCOMING

    fun lastTask(context: Context): Tab =
        Tab.entries.firstOrNull { it.name == context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last_task", null) } ?: left(context)

    fun setLastTask(context: Context, tab: Tab) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString("last_task", tab.name) }

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
        // Покупка из уведомления банка (#233) открывается ниже, когда готова модель финансов.
        if (openRoute is OpenBankPurchase || openRoute is OpenBankQueue || openRoute is OpenBudget || openRoute is OpenHealthWeight || openRoute is OpenNews || openRoute is HabitsDestination || openRoute is OpenHabitAdd || openRoute is HubDestination) return@LaunchedEffect
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
    // #357: сортировка открытого экрана задач — «Предстоящее», «Входящие» или список; на других экранах кнопки ↑↓ нет.
    val sortScope: com.prostovasei.simpletasks.domain.model.SortScope? = when {
        destination?.hasRoute(AllDestination::class) == true -> com.prostovasei.simpletasks.domain.model.SortScope.Upcoming
        destination?.hasRoute(InboxDestination::class) == true -> com.prostovasei.simpletasks.domain.model.SortScope.Inbox
        destination?.hasRoute(ProjectDestination::class) == true -> (if (backStack?.destination?.isSheet() == true) navController.previousBackStackEntry else backStack)
            ?.takeIf { it.destination.hasRoute(ProjectDestination::class) }?.toRoute<ProjectDestination>()?.projectId
            ?.let { com.prostovasei.simpletasks.domain.model.SortScope.Project(it) }
        else -> null
    }
    val sortModel: com.prostovasei.simpletasks.ui.common.TaskSortViewModel = hiltViewModel()
    val currentSort by remember(sortScope) { sortScope?.let { sortModel.observe(it) } ?: kotlinx.coroutines.flow.flowOf(null) }
        .collectAsStateWithLifecycle(initialValue = null)
    val sortListName by remember(sortScope) { sortScope?.let { sortModel.listName(it) } ?: kotlinx.coroutines.flow.flowOf(null) }
        .collectAsStateWithLifecycle(initialValue = null)
    var sortOpen by remember { mutableStateOf(false) }
    if (sortOpen && sortScope != null) {
        currentSort?.let { sort ->
            val subtitle = when (sortScope) {
                com.prostovasei.simpletasks.domain.model.SortScope.Upcoming -> stringResource(R.string.sort_subtitle_upcoming)
                com.prostovasei.simpletasks.domain.model.SortScope.Inbox -> stringResource(R.string.sort_subtitle_list, stringResource(R.string.inbox_title))
                is com.prostovasei.simpletasks.domain.model.SortScope.Project -> stringResource(R.string.sort_subtitle_list, sortListName.orEmpty())
            }
            com.prostovasei.simpletasks.ui.common.SortSheet(
                subtitle = subtitle, sort = sort, default = sortScope.default,
                upcoming = sortScope == com.prostovasei.simpletasks.domain.model.SortScope.Upcoming,
                onChange = { sortModel.set(sortScope, it) }, onDismiss = { sortOpen = false },
            )
        }
    }
    val context = LocalContext.current
    // Страница, с которой открывается приложение: «Предстоящее» или «Неделя» — выбранная последней.
    var left by remember { mutableStateOf(NavPrefs.left(context)) }
    // #329: приложение открывается на хабе «Главная»; раздел задач помнит последний выбор (Предстоящие, Неделя, Списки).
    val startDestination = HubDestination

    // Финансы (#217, этап 1 — #221): отдельный модуль. Нижняя панель общая (#241): модуль меняется кнопкой справа;
    // при возврате в модуль открывается то же место (сохранённое состояние страницы).
    val inFinance = destination?.hasRoute(FinanceDestination::class) == true
    // «Здоровье» (#257) — отдельный модуль со своей панелью; экран белый плоский, как «Неделя» (макеты design/approved/257).
    val inHealth = destination?.hasRoute(HealthDestination::class) == true
    val inNews = destination?.hasRoute(NewsDestination::class) == true
    // #340: «Привычки» — отдельный модуль, белый плоский экран, как «Здоровье».
    val inHabits = destination?.hasRoute(HabitsDestination::class) == true
    val inHub = destination?.hasRoute(HubDestination::class) == true
    var addOpen by rememberSaveable { mutableStateOf(false) }
    val newsScope = rememberCoroutineScope()
    val refreshNews: suspend () -> Unit = {
        WidgetRenderer.refreshNews(context.applicationContext, cancelNotification = true)
    }
    LaunchedEffect(inNews) { if (inNews) newsScope.launch { refreshNews() } }
    // #296: нажатие на «N новых новостей» — вкладка «Новости»; вход во вкладку сам обновит ленту и снимет уведомление.
    // #340: напоминание о привычках и раздел «Привычки» виджета открывают раздел.
    val habitsModel = hiltViewModel<com.prostovasei.simpletasks.ui.feature.habits.HabitsViewModel>()
    LaunchedEffect(openRoute) {
        if (openRoute !is HabitsDestination && openRoute !is OpenHabitAdd) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navController.openRoot(HabitsDestination, restore = true)
        if (openRoute is OpenHabitAdd) habitsModel.onIntent(com.prostovasei.simpletasks.ui.feature.habits.HabitsIntent.Add)
        onRouteOpened()
    }
    LaunchedEffect(openRoute) {
        if (openRoute !is HubDestination) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navController.openRoot(HubDestination, restore = false)
        onRouteOpened()
    }
    LaunchedEffect(openRoute) {
        if (openRoute !is OpenNews) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        navController.openRoot(NewsDestination, restore = true)
        onRouteOpened()
    }
    val taskCanvas = currentTab == Tab.UPCOMING || currentTab == Tab.WEEK || destination?.hasRoute(ProjectDestination::class) == true || inHealth || inNews || inHub || inHabits
    val lightBars = taskCanvas || !isSystemInDarkTheme()
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    val finance = hiltViewModel<FinanceViewModel>()
    // #233: «Изменить» в уведомлении о покупке — раздел финансов с окном нового расхода.
    LaunchedEffect(openRoute) {
        if (openRoute !is OpenBankPurchase && openRoute !is OpenBankQueue && openRoute !is OpenBudget) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        val inStack = runCatching { navController.getBackStackEntry<FinanceDestination>() }.isSuccess
        if (inStack) navController.popBackStack<FinanceDestination>(inclusive = false) else navController.openRoot(FinanceDestination, restore = true)
        when (openRoute) {
            is OpenBankPurchase -> finance.onIntent(FinanceIntent.EditPurchase(openRoute.purchaseId))
            is OpenBankQueue -> {
                val opened = openRoute.purchaseId?.let { finance.openPendingPurchase(it) } == true
                if (!opened) navController.navigate(FinanceUnsortedDestination) { launchSingleTop = true }
            }
            is OpenBudget -> runCatching { java.time.YearMonth.parse(openRoute.month) }.getOrNull()?.let {
                finance.onIntent(FinanceIntent.OpenBudget(it, openRoute.prepare))
            }
        }
        onRouteOpened()
    }
    val health = hiltViewModel<HealthViewModel>()
    val habits = habitsModel
    // #257: нажатие на «Пора взвеситься» — раздел «Здоровье» с окном ввода веса.
    LaunchedEffect(openRoute) {
        if (openRoute !is OpenHealthWeight) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        val inStack = runCatching { navController.getBackStackEntry<HealthDestination>() }.isSuccess
        if (inStack) navController.popBackStack<HealthDestination>(inclusive = false) else navController.openRoot(HealthDestination, restore = true)
        health.onIntent(HealthIntent.Add)
        onRouteOpened()
    }
    var lastTaskTab by rememberSaveable { mutableStateOf(NavPrefs.lastTask(context).name) }
    var taskPickerOpen by rememberSaveable { mutableStateOf(false) }
    var sectionsOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(currentTab) { currentTab?.let { lastTaskTab = it.name; NavPrefs.setLastTask(context, it) } }

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
    // Фильтр страницы «Неделя» (#243): долгое нажатие на значок «Неделя» в нижней панели.
    var weekFilterOpen by remember { mutableStateOf(false) }
    if (weekFilterOpen) {
        val filterModel: WeekFilterViewModel = hiltViewModel()
        val tree by filterModel.tree.collectAsStateWithLifecycle()
        val applied by filterModel.filter.collectAsStateWithLifecycle()
        WeekFilterDialog(tree, applied, onApply = { filterModel.apply(it); weekFilterOpen = false }, onDismiss = { weekFilterOpen = false })
    }
    val bulkTexts = BulkTexts.load()
    LaunchedEffect(bulk) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            bulk.actions.events.collect { result ->
                launch { snackbar.showUndo(bulkTexts.text(result), undoText, bulkTexts.kind(result)) { bulk.actions.undo(result) } }
            }
        }
    }

    Scaffold(
        containerColor = if (taskCanvas) Color(TaskCanvasTokens.background.light) else AppTheme.colors.background,
        snackbarHost = { UndoSnackbarHost(snackbar) },
        bottomBar = {
            if (inHub || inFinance || inHealth || inNews || inHabits || (!selectionMode.value && (destination == null || currentTab != null))) {
                val task = currentTab ?: Tab.valueOf(lastTaskTab)
                val active = when {
                    inHub -> "hub"
                    inFinance -> "finance"
                    inHealth -> "health"
                    inNews -> "news"
                    inHabits -> "habits"
                    else -> "tasks"
                }
                val pick: (Tab) -> Unit = { tab ->
                    taskPickerOpen = false
                    if (tab != Tab.LISTS) {
                        left = tab
                        NavPrefs.setLeft(context, tab)
                    }
                    if (tab == Tab.LISTS && currentTab == Tab.LISTS && destination?.hasRoute(ListsDestination::class) != true) {
                        navController.popBackStack(ListsDestination, inclusive = false)
                    } else if (currentTab != tab) navController.openTab(tab)
                }
                TaskCanvasTheme(enabled = inHub || inFinance || inHealth || inNews || inHabits || active == "tasks" && task != Tab.LISTS) {
                    androidx.compose.foundation.layout.Box {
                        // #336/#338 (конструктор PM): плавающая капсула справа — разделы, настройки, голос, «+» (меню «что добавить»).
                        FloatingNavBar(
                            onHub = { sectionsOpen = true },
                            onSettings = { navController.navigate(AppSettingsDestination) { launchSingleTop = true } },
                            onVoice = { context.startActivity(com.prostovasei.simpletasks.widget.voiceIntent(context, com.prostovasei.simpletasks.widget.WidgetFilter.All)) },
                            onAdd = { addOpen = true },
                            onHubLongClick = { weekFilterOpen = true }.takeIf { active == "tasks" && task == Tab.WEEK },
                            onSort = { sortOpen = true }.takeIf { active == "tasks" && sortScope != null },
                            sortChanged = sortScope != null && currentSort != null && currentSort != sortScope.default,
                        )
                        if (addOpen) {
                            TabPickerSheet(
                                selected = null,
                                entries = addEntries(news = active == "news"),
                                onPick = { key ->
                                    addOpen = false
                                    when (key) {
                                        // #357: на экране списка «Задача» создаётся в этом списке (своей «+» у экрана больше нет).
                                        ADD_TASK -> navController.navigate(QuickAddDestination(
                                            projectId = (sortScope as? com.prostovasei.simpletasks.domain.model.SortScope.Project)?.id,
                                            today = !(active == "tasks" && task == Tab.LISTS),
                                        ))
                                        ADD_EXPENSE -> finance.onIntent(FinanceIntent.Add)
                                        ADD_WEIGHT -> health.onIntent(HealthIntent.Add)
                                        ADD_HABIT -> {
                                            if (active != "habits" && active != "hub") navController.openRoot(HabitsDestination, restore = true)
                                            habits.onIntent(com.prostovasei.simpletasks.ui.feature.habits.HabitsIntent.Add)
                                        }
                                        ADD_NEWS -> newsScope.launch { refreshNews() }
                                    }
                                },
                                onDismiss = { addOpen = false },
                            )
                        }
                        if (sectionsOpen) {
                            TabPickerSheet(
                                selected = if (active == "tasks") task.name else active,
                                onPick = { key ->
                                    sectionsOpen = false
                                    when (key) {
                                        active -> Unit
                                        "hub" -> navController.popBackStack(HubDestination, inclusive = false)
                                        "finance" -> navController.openRoot(FinanceDestination, restore = true)
                                        "health" -> navController.openRoot(HealthDestination, restore = true)
                                        "news" -> navController.openRoot(NewsDestination, restore = true)
                                        "habits" -> navController.openRoot(HabitsDestination, restore = true)
                                        "settings" -> navController.navigate(AppSettingsDestination) { launchSingleTop = true }
                                        else -> Tab.entries.firstOrNull { it.name == key }?.let { tab ->
                                            if (active == "tasks" && tab == task && tab != Tab.LISTS) Unit else pick(tab)
                                        }
                                    }
                                },
                                onDismiss = { sectionsOpen = false },
                            )
                        }
                        if (active == "tasks" && taskPickerOpen) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { taskPickerOpen = false },
                                title = { Text(stringResource(R.string.widget_task_view)) },
                                confirmButton = {},
                                text = {
                                    androidx.compose.foundation.layout.Column {
                                        Tab.entries.forEach { tab ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(tab.label)) },
                                                leadingIcon = { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(tab.icon), null) },
                                                trailingIcon = { if (tab == task) Text("✓") },
                                                onClick = { pick(tab) },
                                                modifier = Modifier.testTag("task_view:${tab.name}"),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        // consumeWindowInsets: экраны с клавиатурой (imePadding) не добавляют отступ панели навигации второй раз.
        // Переход между экранами — сдвиг вбок в сторону нажатой вкладки; вложенный экран — справа (итерация 15, C2).
        CompositionLocalProvider(LocalBulkActions provides bulk, LocalSelectionMode provides selectionMode) {
        NavHost(
            navController,
            startDestination = startDestination,
            // #338: на хабе задачи идут до низа экрана, капсула лежит поверх (в конце списка — отступ под неё).
            modifier = (if (inHub) Modifier.padding(top = padding.calculateTopPadding()) else Modifier.padding(padding)).consumeWindowInsets(padding),
            enterTransition = { slideIn(push = true) },
            exitTransition = { slideOut(push = true) },
            popEnterTransition = { slideIn(push = false) },
            popExitTransition = { slideOut(push = false) },
            // #239: жест «назад» — то же движение, что и стрелка «назад», экран следует за пальцем. Без этих двух строк навигация
            // показывала свою анимацию по умолчанию — экран сжимался в карточку (запись экрана PM 13.11.2026).
            predictivePopEnterTransition = { slideIn(push = false) },
            predictivePopExitTransition = { slideOut(push = false) },
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
            // Единые настройки (#241): главная страница и разделы — «Задачи», «Финансы», «Приложение».
            composable<AppSettingsDestination> {
                AppSettingsRoute(
                    onBack = { navController.popBackStack() },
                    onTasks = { navController.navigate(SettingsDestination) { launchSingleTop = true } },
                    onFinance = { navController.navigate(FinanceSettingsDestination) { launchSingleTop = true } },
                    onHealth = { navController.navigate(HealthSettingsDestination) { launchSingleTop = true } },
                    onNews = { navController.navigate(NewsSettingsDestination) { launchSingleTop = true } },
                    onHabits = { navController.navigate(HabitsSettingsDestination) { launchSingleTop = true } },
                    onApp = { navController.navigate(AppInfoDestination) { launchSingleTop = true } },
                )
            }
            composable<SettingsDestination> { SettingsRoute(onBack = { navController.popBackStack() }) }
            composable<AppInfoDestination> { AppInfoRoute(onBack = { navController.popBackStack() }) }
            composable<HubDestination> {
                val financeState by finance.state.collectAsStateWithLifecycle()
                val healthState by health.state.collectAsStateWithLifecycle()
                val habitsState by habits.state.collectAsStateWithLifecycle()
                FinanceEffects(finance)
                HubRoute(financeState, finance::onIntent, healthState, health::onIntent, onOpenTask = openTask,
                    onAddTask = { navController.navigate(QuickAddDestination(today = true)) },
                    onOpenSection = { key ->
                        when (key) {
                            com.prostovasei.simpletasks.ui.feature.hub.HUB_FINANCE -> navController.openRoot(FinanceDestination, restore = true)
                            com.prostovasei.simpletasks.ui.feature.hub.HUB_NEWS_SECTION -> navController.openRoot(NewsDestination, restore = true)
                            com.prostovasei.simpletasks.ui.feature.hub.HUB_HABITS -> navController.openRoot(HabitsDestination, restore = true)
                            else -> navController.openTab(Tab.valueOf(lastTaskTab).takeIf { it != Tab.LISTS } ?: Tab.UPCOMING)
                        }
                    },
                    habits = habitsState, onHabits = habits::onIntent)
                // #340: «+ Привычка» с хаба открывает окно привычки прямо поверх хаба.
                habitsState.form?.let { com.prostovasei.simpletasks.ui.feature.habits.HabitFormHost(habitsState, habits::onIntent) }
            }
            composable<FinanceDestination> {
                FinanceRoute(finance, onOpenUnsorted = { navController.navigate(FinanceUnsortedDestination) { launchSingleTop = true } })
            }
            composable<HealthDestination> { HealthRoute(health, snackbar) }
            composable<HabitsDestination> { com.prostovasei.simpletasks.ui.feature.habits.HabitsRoute(habits) }
            composable<HabitsSettingsDestination> {
                com.prostovasei.simpletasks.ui.feature.habits.HabitsSettingsRoute(habits, onBack = { navController.popBackStack() },
                    onBackup = { navController.navigate(HabitsBackupDestination) { launchSingleTop = true } })
            }
            composable<HabitsBackupDestination> { com.prostovasei.simpletasks.ui.feature.habits.HabitsBackupRoute(habits, onBack = { navController.popBackStack() }) }
            composable<NewsDestination> { TaskCanvasTheme { NewsScreen() } }
            composable<NewsSettingsDestination> { NewsSettingsRoute(onBack = { navController.popBackStack() }) }
            composable<HealthSettingsDestination> {
                HealthSettingsRoute(health, onBack = { navController.popBackStack() }, onBackup = { navController.navigate(HealthBackupDestination) { launchSingleTop = true } })
            }
            composable<HealthBackupDestination> { HealthBackupRoute(health, onBack = { navController.popBackStack() }) }
            composable<FinanceSettingsDestination> {
                FinanceSettingsRoute(
                    finance,
                    onBack = { navController.popBackStack() },
                    onCategories = { navController.navigate(FinanceCategoriesDestination) { launchSingleTop = true } },
                    onPlaces = { navController.navigate(FinancePlacesDestination) { launchSingleTop = true } },
                    onTrash = { navController.navigate(FinanceTrashDestination) { launchSingleTop = true } },
                    onBank = { navController.navigate(FinanceBankDestination) { launchSingleTop = true } },
                )
            }
            composable<FinancePlacesDestination> { PlacesRoute(finance, onBack = { navController.popBackStack() }) }
            composable<FinanceTrashDestination> { FinanceTrashRoute(finance, onBack = { navController.popBackStack() }) }
            composable<FinanceBankDestination> { BankSettingsRoute(finance, onBack = { navController.popBackStack() }) }
            composable<FinanceUnsortedDestination> { UnsortedRoute(finance, onBack = { navController.popBackStack() }) }
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


private const val SLIDE_MS = 300

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
    isOn(Tab.UPCOMING) -> 0
    isOn(Tab.WEEK) -> 1
    isOn(Tab.LISTS) -> 2
    // Финансы — правее всего: модуль приходит справа и уходит вправо.
    hasRoute(FinanceDestination::class) -> 4
    hasRoute(HealthDestination::class) -> 5
    hasRoute(HabitsDestination::class) -> 5
    hasRoute(NewsDestination::class) -> 6
    else -> 3
}

/** 1 — новый экран приходит справа, -1 — слева. Между экранами одного уровня: вперёд — справа, «Назад» — слева. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(push: Boolean): Int {
    val delta = targetState.destination.order().compareTo(initialState.destination.order())
    return if (delta != 0) delta else if (push) 1 else -1
}

/**
 * Переход «сдвиг с растворением» (#239, вариант C, решение PM 13.11.2026; превью — design/approved/239/motion-C.gif): новый экран
 * сдвигается на десятую часть ширины в сторону [direction] и проявляется во второй половине перехода, прежний — сдвигается на столько же
 * и растворяется в первой. Одно движение для вкладок, модулей, вложенных экранов, «назад» и жеста «назад».
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideIn(push: Boolean): EnterTransition {
    val dir = direction(push)
    return slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it * dir / SHIFT_PART } +
        fadeIn(tween(SLIDE_MS / 2, delayMillis = SLIDE_MS / 2, easing = LinearOutSlowInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOut(push: Boolean): ExitTransition {
    val dir = direction(push)
    return slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it * dir / SHIFT_PART } +
        fadeOut(tween(SLIDE_MS / 2, easing = FastOutLinearInEasing))
}

/** Сдвиг экрана при переходе — десятая часть ширины. */
private const val SHIFT_PART = 10


/** Переход на вкладку с сохранением состояния каждой вкладки (стандартное поведение нижнего бара). */
// Итерация 55: «Списки» всегда открываются с корня — «Входящие», «Поиск» и «Настройки» внутри не «залипают».
private fun NavHostController.openTab(tab: Tab) = openRoot(tab.route, restore = tab != Tab.LISTS)

/** Главный экран вкладки или модуля: прежний уходит с сохранением состояния, [restore] — вернуть сохранённое состояние нового. */
private fun NavHostController.openRoot(route: Any, restore: Boolean) {
    closeSettings()
    if (currentDestination?.hasRoute(route::class) == true) return
    fun open(restoreModule: Boolean) = navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restoreModule
    }
    open(restore)
    // После обновления в сохранённом стеке 0.79.0 ещё могут находиться настройки.
    if (closeSettings() && currentDestination?.hasRoute(route::class) != true) open(false)
}

private fun NavHostController.closeSettings(): Boolean {
    if (runCatching { getBackStackEntry<AppSettingsDestination>() }.isFailure) return false
    return popBackStack<AppSettingsDestination>(inclusive = true)
}

private fun NavDestination?.isOn(tab: Tab): Boolean = this?.hasRoute(tab.routeClass) == true
