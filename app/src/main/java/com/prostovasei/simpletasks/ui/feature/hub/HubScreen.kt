package com.prostovasei.simpletasks.ui.feature.hub

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
    viewModel: HubViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val newsState by SportsNews.state.collectAsStateWithLifecycle()
    val read by NewsRead.revision.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { WidgetRenderer.refreshNews(context.applicationContext) }
    val news = remember(newsState, read) { hubNews(context) }
    HubScreen(state, viewModel::onIntent, finance, onFinance, news, onOpenTask, onAddTask, onOpenSection)
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
) {
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().testTag(TAG_HUB)) {
        // #336 (выбор PM): финансы, новости, задачи; #338 — у каждого раздела заголовок «Финансы ›», нажатие открывает раздел.
        HubSectionTitle(R.string.module_finance, HUB_FINANCE, onOpenSection, Modifier.padding(top = spacing.sm))
        HubFinance(finance, onFinance)
        if (news.isNotEmpty()) {
            HubSectionTitle(R.string.module_news, HUB_NEWS_SECTION, onOpenSection, Modifier.padding(top = spacing.sm))
            HubNewsPager(news)
        }
        HubSectionTitle(R.string.module_tasks, HUB_TASKS_SECTION, onOpenSection, Modifier.padding(top = spacing.sm))
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
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag(TAG_HUB_FINANCE)) {
        LimitBar(model.total, monthLimit(context, budget), model.forecast, month, today, daily = true) { onIntent(FinanceIntent.OpenBudget(month)) }
        state.accounts?.takeIf { it.isNotEmpty() }?.let { HubAccounts(it, state.total) }
    }
}

@Composable
private fun HubAccounts(accounts: List<Account>, total: Long) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    // #336 (выбор PM): плашки счетов крупнее — 14 sp и поля 12 × 8; не помещаются — листаются вбок.
    Row(
        Modifier.fillMaxWidth().padding(top = spacing.xs).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${financeThousands(total)} ₽", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
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
    val groups = listOf(
        Triple(null, state.overdue, true),
        Triple(today, state.todayItems, false),
        Triple(today.plusDays(1), state.tomorrowItems, false),
    )
    // #338: список идёт под капсулу; снизу отступ в её высоту, чтобы последнюю задачу можно было поднять над панелью.
    LazyColumn(Modifier.fillMaxSize().testTag(TAG_HUB_TASKS), contentPadding = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues().let { androidx.compose.foundation.layout.PaddingValues(bottom = it.calculateBottomPadding() + SizeTokens.NAV_BAR_SPACE.dp) }) {
        groups.forEachIndexed { index, (date, items, late) ->
            if (items.isEmpty()) return@forEachIndexed
            // #334: подписи как в «Предстоящем» — «Просрочено» красным с числом справа, дни 12 sp серым с полосой у «Сегодня».
            item("hub-group-$index") {
                if (date == null) {
                    OverdueHeader(items.count { !it.showsDone }, collapsed = false, foldDescription = "", moveDescription = "", onToggle = null, onMoveToday = null)
                } else {
                    DayHeader(
                        text = WidgetDates.header(date),
                        relative = if (index == 1) days.day(DayLabel.Today) else days.day(DayLabel.Tomorrow),
                        isOverdue = false,
                        count = items.size,
                        progress = if (index == 1) items.count { it.isDone } to items.size else null,
                    )
                }
            }
            timelineItems(
                items = items,
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
    Column(Modifier.fillMaxWidth().padding(top = spacing.sm, bottom = spacing.xs).testTag(TAG_HUB_NEWS)) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.md),
            pageSpacing = spacing.md) { page ->
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
            val label = stringResource(R.string.hub_news_page, pager.currentPage + 1, pages.size)
            Row(Modifier.fillMaxWidth().padding(top = spacing.xs).semantics { contentDescription = label }, horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { index ->
                    Box(Modifier.padding(horizontal = spacing.xxs).size(SizeTokens.HUB_DOT.dp).clip(CircleShape)
                        .background(if (index == pager.currentPage) colors.textPrimary else colors.selected))
                }
            }
        }
    }
}

private const val HUB_ACCOUNTS = 4

const val TAG_HUB = "hub"
const val TAG_HUB_ACCOUNT = "hub_account"
const val TAG_HUB_FINANCE = "hub_finance"
const val TAG_HUB_TASKS = "hub_tasks"
const val TAG_HUB_EMPTY = "hub_empty"
const val TAG_HUB_NEWS = "hub_news"
const val TAG_HUB_NEWS_CARD = "hub_news_card"
