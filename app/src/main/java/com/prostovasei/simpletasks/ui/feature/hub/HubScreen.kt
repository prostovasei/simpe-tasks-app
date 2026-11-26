package com.prostovasei.simpletasks.ui.feature.hub

import java.time.LocalDate
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import com.prostovasei.simpletasks.ui.common.DayLabel
import com.prostovasei.simpletasks.widget.WidgetDates
import com.prostovasei.simpletasks.designsystem.components.OverdueHeader
import com.prostovasei.simpletasks.designsystem.components.DayHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.NewsAlerts
import com.prostovasei.simpletasks.data.news.NewsRead
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareColors
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.domain.model.finance.Account
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import com.prostovasei.simpletasks.domain.model.finance.financeThousands
import com.prostovasei.simpletasks.ui.common.DayNames
import com.prostovasei.simpletasks.ui.common.TaskItem
import com.prostovasei.simpletasks.ui.common.TaskListIntent
import com.prostovasei.simpletasks.ui.common.rememberNowMinute
import com.prostovasei.simpletasks.ui.common.timelineItems
import com.prostovasei.simpletasks.ui.feature.finance.FinanceDialogs
import com.prostovasei.simpletasks.ui.feature.finance.FinanceIntent
import com.prostovasei.simpletasks.ui.feature.finance.FinanceState
import com.prostovasei.simpletasks.ui.feature.finance.LimitBar
import com.prostovasei.simpletasks.ui.feature.finance.monthLimit
import com.prostovasei.simpletasks.ui.feature.health.HealthIntent
import com.prostovasei.simpletasks.ui.feature.health.HealthState
import com.prostovasei.simpletasks.ui.feature.health.WeightDialog
import com.prostovasei.simpletasks.widget.NewsWidgets
import com.prostovasei.simpletasks.widget.WidgetRenderer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * #329 — хаб «Главная» (финальный вариант PM 24.11.2026, design/approved/329): без шапки и заголовков блоков;
 * сверху вниз — полоса лимита со счетами, задачи «Просрочено · Сегодня · Завтра» (своя прокрутка), новости страницами 2 × 2.
 */

/** Карточка новости хаба: время и спорт, заголовок, непрочитана ли. */
internal data class HubNews(val url: String, val meta: String, val title: String, val unread: Boolean)

/** Сколько новостей в хабе и сколько на странице (§5 ТЗ). */
internal const val HUB_NEWS = 12
internal const val HUB_NEWS_PAGE = 4

@Composable
fun HubRoute(
    finance: FinanceState,
    onFinance: (FinanceIntent) -> Unit,
    health: HealthState,
    onHealth: (HealthIntent) -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onOpenSection: (String) -> Unit = {},
    habits: com.prostovasei.simpletasks.ui.feature.habits.HabitsState? = null,
    onHabits: (com.prostovasei.simpletasks.ui.feature.habits.HabitsIntent) -> Unit = {},
    viewModel: HubViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val newsState by SportsNews.state.collectAsStateWithLifecycle()
    val read by NewsRead.revision.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { WidgetRenderer.refreshNews(context.applicationContext) }
    val news = remember(newsState, read) { hubNews(context) }
    HubScreen(state, viewModel::onIntent, finance, onFinance, news, onOpenTask, onAddTask, onOpenSection, habits, onHabits)
    FinanceDialogs(finance, onFinance)
    health.form?.let { WeightDialog(health, it, onHealth) }
}

/** Последние [HUB_NEWS] новостей сохранённой ленты — те же, что во вкладке «Новости». */
internal fun hubNews(context: android.content.Context): List<HubNews> {
    val edition = SportsNews.cached(context) ?: return emptyList()
    val unread = NewsRead.unread(context, edition)
    val time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return edition.recent(Instant.now()).take(HUB_NEWS).map { story ->
        HubNews(story.url, listOf(time.format(Instant.ofEpochSecond(story.published)), story.sport).filter { it.isNotBlank() }.joinToString(" · "),
            story.title, NewsAlerts.key(story) in unread)
    }
}

@Composable
internal fun HubScreen(
    state: HubState,
    onIntent: (TaskListIntent) -> Unit,
    finance: FinanceState,
    onFinance: (FinanceIntent) -> Unit,
    news: List<HubNews>,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onOpenSection: (String) -> Unit = {},
    habits: com.prostovasei.simpletasks.ui.feature.habits.HabitsState? = null,
    onHabits: (com.prostovasei.simpletasks.ui.feature.habits.HabitsIntent) -> Unit = {},
) {
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().testTag(TAG_HUB)) {
        // #336 (выбор PM): финансы, новости, задачи; #338 — у каждого раздела заголовок «Финансы ›», нажатие открывает раздел.
        // #346 (выбор PM): заголовков разделов нет — между разделами отступ.
        Spacer(Modifier.height(spacing.sm))
        HubFinance(finance, onFinance)
        // #340: привычки на сегодня — плашками после финансов; нажатие отмечает.
        habits?.let { h ->
            val today = h.today
            val due = h.active.filter { com.prostovasei.simpletasks.domain.model.habits.HabitMath.dueOn(it, today) }
            if (due.isNotEmpty()) {
                Spacer(Modifier.height(SizeTokens.HUB_SECTION_GAP.dp))
                HubHabits(due, today) { onHabits(com.prostovasei.simpletasks.ui.feature.habits.HabitsIntent.Toggle(it, today)) }
            }
        }
        // #354 (выбор PM): между разделами 24 — разделы не сливаются; у задач 12 из них — верхнее поле подписи дня.
        if (news.isNotEmpty()) {
            Spacer(Modifier.height(SizeTokens.HUB_SECTION_GAP.dp))
            HubNewsPager(news)
        }
        Spacer(Modifier.height((SizeTokens.HUB_SECTION_GAP - SpacingTokens.SM).dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.isEmpty) HubNoTasks(onAddTask) else HubTasks(state, onIntent, onOpenTask)
        }
    }
}

/** Заголовок раздела хаба (#338, выбор PM «Заголовки разделов»): «Финансы ›» 13 sp полужирным, нажатие открывает раздел. */
@Composable
private fun HubSectionTitle(@androidx.annotation.StringRes label: Int, key: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        modifier.padding(horizontal = spacing.sm).clip(AppTheme.shapes.small).clickable(role = Role.Button) { onOpen(key) }
            .padding(horizontal = spacing.xs, vertical = spacing.xxs).testTag("$TAG_HUB_SECTION:$key"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_hub_next), null,
            Modifier.size(SizeTokens.LIMIT_NOTE_ICON.dp), tint = colors.textSecondary)
    }
}

/** Плашки привычек хаба (#340): «эмодзи название», сделанная — фиолетовая с ✓; ряд листается вбок. */
@Composable
private fun HubHabits(habits: List<com.prostovasei.simpletasks.domain.model.habits.Habit>, today: java.time.LocalDate, onToggle: (String) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = spacing.sm).testTag(TAG_HUB_HABITS),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        habits.forEach { habit ->
            val done = habit.isDone(today)
            val description = stringResource(if (done) R.string.habit_uncheck else R.string.habit_check, habit.name)
            Text(
                "${habit.emojiShown} ${habit.name}${if (done) " ✓" else ""}",
                Modifier.clip(CircleShape).background(if (done) colors.priority.high.container else FinanceShareColors.accountSurface)
                    .clickable(role = Role.Checkbox) { onToggle(habit.id) }
                    .semantics { contentDescription = description }
                    .padding(horizontal = spacing.md, vertical = spacing.xs).testTag("$TAG_HUB_HABIT:${habit.id}"),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = if (done) colors.priority.high.content else colors.textPrimary, maxLines = 1,
            )
        }
    }
}

const val HUB_HABITS = "habits"
const val TAG_HUB_HABITS = "hub_habits"
const val TAG_HUB_HABIT = "hub_habit"
const val HUB_FINANCE = "finance"
const val HUB_NEWS_SECTION = "news"
const val HUB_TASKS_SECTION = "tasks"
const val TAG_HUB_SECTION = "hub_section"

@Composable
private fun HubFinance(state: FinanceState, onIntent: (FinanceIntent) -> Unit) {
    val spacing = AppTheme.spacing
    val context = LocalContext.current
    val today = state.today
    val month = java.time.YearMonth.from(today)
    val model = remember(state.operations, today) { MonthSpending.of(state.operations.orEmpty(), month, today) }
    val budget = remember(state.budgets, state.categories, state.operations, today) { state.monthBudget(month) }
    // #350 (выбор PM): счета над полосой, без общей суммы; отступ до полосы 10; поля 12, как у всех блоков хаба.
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.sm).testTag(TAG_HUB_FINANCE)) {
        state.accounts?.takeIf { it.isNotEmpty() }?.let { HubAccounts(it) }
        LimitBar(model.total, monthLimit(context, budget), model.forecast, month, today, daily = true) { onIntent(FinanceIntent.OpenBudget(month)) }
    }
}

@Composable
private fun HubAccounts(accounts: List<Account>) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    // #336 (выбор PM): плашки счетов крупнее — 14 sp и поля 12 × 8; не помещаются — листаются вбок.
    Row(
        Modifier.fillMaxWidth().padding(bottom = (SizeTokens.HUB_FINANCE_GAP - SpacingTokens.XXS).dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically,
    ) {
        accounts.take(HUB_ACCOUNTS).forEach { account ->
            Text("${account.emojiShown} ${financeThousands(account.balance)}", Modifier.clip(CircleShape).background(FinanceShareColors.accountSurface)
                .padding(horizontal = spacing.md, vertical = spacing.xs).testTag(TAG_HUB_ACCOUNT), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = colors.textPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun HubTasks(state: HubState, onIntent: (TaskListIntent) -> Unit, onOpenTask: (String) -> Unit) {
    val today = state.today ?: return
    val days = DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday))
    val nowMinute = rememberNowMinute()
    val toggle: (TaskItem) -> Unit = { onIntent(TaskListIntent.ToggleComplete(it.id, it.isRecurring, it.showsDone)) }
    val open: (TaskItem) -> Unit = { onOpenTask(it.ghostOf ?: it.id) }
    // #350 (выбор PM): неделя списком вниз; пустые дни скрыты.
    val groups = listOf(Triple<LocalDate?, List<TaskItem>, Boolean>(null, state.overdue, true)) + state.days.map { (date, items) -> Triple(date, items, false) }
    val edge = AppTheme.spacing.xxs
    // #338: список идёт под капсулу; снизу отступ в её высоту, чтобы последнюю задачу можно было поднять над панелью.
    LazyColumn(Modifier.fillMaxSize().testTag(TAG_HUB_TASKS), contentPadding = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues().let { androidx.compose.foundation.layout.PaddingValues(bottom = it.calculateBottomPadding() + SizeTokens.NAV_BAR_SPACE.dp) }) {
        groups.forEachIndexed { index, (date, items, late) ->
            // #344: «Просрочено» только из выполненных — без подписи.
            // #352: и день, где всё выполнено, — без подписи.
            if (items.isEmpty() || items.none { !it.isDone || it.isCompleting }) return@forEachIndexed
            // #334: подписи как в «Предстоящем» — «Просрочено» красным с числом справа, дни 12 sp серым с полосой у «Сегодня».
            item("hub-group-$index") {
                if (date == null) {
                    OverdueHeader(items.count { !it.showsDone }, collapsed = false, foldDescription = "", moveDescription = "", onToggle = null, onMoveToday = null)
                } else {
                    DayHeader(
                        text = WidgetDates.header(date),
                        relative = when (index) { 1 -> days.day(DayLabel.Today); 2 -> days.day(DayLabel.Tomorrow); else -> null },
                        isOverdue = false,
                        count = items.size,
                        progress = if (index == 1) items.count { it.isDone } to items.size else null,
                    )
                }
            }
            // #342: как в «Предстоящем» — выполненные исчезают, прогресс «N из M» — по всем.
            timelineItems(
                items = items.filter { !it.isDone || it.isCompleting },
                keyPrefix = "hub-$index",
                days = days,
                nowMinute = if (index == 1) nowMinute else null,
                showNowMarker = false,
                isOverdue = { late && !it.showsDone },
                showList = true,
                onToggle = toggle,
                onOpen = open,
                dayLabels = late,
                today = today,
                inset = edge,
            )
        }
    }
}

@Composable
private fun HubNoTasks(onAddTask: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().testTag(TAG_HUB_EMPTY), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.hub_no_tasks), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        Text(stringResource(R.string.hub_add_task_button), Modifier.padding(top = spacing.sm).clip(AppTheme.shapes.large).background(colors.selected)
            .clickable(role = Role.Button, onClick = onAddTask).padding(horizontal = spacing.md, vertical = spacing.xs),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}

@Composable
private fun HubNewsPager(news: List<HubNews>) {
    val spacing = AppTheme.spacing
    val colors = AppTheme.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val pages = news.chunked(HUB_NEWS_PAGE)
    val pager = rememberPagerState { pages.size }
    val accentNew = colorResource(R.color.widget_flat_new)
    val newMark = stringResource(R.string.news_new_mark)
    Column(Modifier.fillMaxWidth().testTag(TAG_HUB_NEWS)) {
        // #346 (выбор PM): стрелок и точек нет; касание у левого или правого края (18 % ширины) листает страницу, свайп тоже работает.
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val prev = stringResource(R.string.hub_news_prev)
        val next = stringResource(R.string.hub_news_next)
        Box(Modifier.fillMaxWidth()) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.sm),
            pageSpacing = spacing.sm) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                pages[page].chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        pair.forEach { item ->
                            Column(
                                Modifier.weight(1f).heightIn(min = SizeTokens.HUB_NEWS_CARD.dp).clip(AppTheme.shapes.large).background(colors.selected)
                                    .clickable {
                                        NewsRead.markRead(context, item.url)
                                        NewsWidgets.refreshList(context)
                                        runCatching { uriHandler.openUri(item.url) }
                                    }
                                    .padding(horizontal = spacing.sm, vertical = spacing.xs).testTag("$TAG_HUB_NEWS_CARD:${item.url}"),
                            ) {
                                Text(com.prostovasei.simpletasks.ui.feature.news.newsMeta(item.meta, item.unread, newMark, accentNew),
                                    style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.title, style = MaterialTheme.typography.bodySmall, fontWeight = if (item.unread) FontWeight.Bold else FontWeight.Normal,
                                    color = colors.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
            if (pages.size > 1) {
                Row(Modifier.matchParentSize()) {
                    listOf(-1 to prev, 0 to "", 1 to next).forEach { (delta, label) ->
                        if (delta == 0) {
                            Spacer(Modifier.weight(1f - 2 * HUB_NEWS_EDGE))
                        } else {
                            Box(
                                Modifier.weight(HUB_NEWS_EDGE).fillMaxHeight()
                                    .clickable(interactionSource = null, indication = null, role = Role.Button) {
                                        scope.launch { pager.animateScrollToPage((pager.currentPage + delta).mod(pages.size)) }
                                    }
                                    .semantics { contentDescription = label }
                                    .testTag(if (delta < 0) TAG_HUB_NEWS_PREV else TAG_HUB_NEWS_NEXT),
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val HUB_ACCOUNTS = 4
private const val HUB_NEWS_EDGE = 0.18f
const val TAG_HUB_NEWS_PREV = "hub_news_prev"
const val TAG_HUB_NEWS_NEXT = "hub_news_next"

const val TAG_HUB = "hub"
const val TAG_HUB_ACCOUNT = "hub_account"
const val TAG_HUB_FINANCE = "hub_finance"
const val TAG_HUB_TASKS = "hub_tasks"
const val TAG_HUB_EMPTY = "hub_empty"
const val TAG_HUB_NEWS = "hub_news"
const val TAG_HUB_NEWS_CARD = "hub_news_card"
