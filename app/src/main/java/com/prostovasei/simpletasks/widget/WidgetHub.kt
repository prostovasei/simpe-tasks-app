package com.prostovasei.simpletasks.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.domain.model.finance.BudgetRules
import com.prostovasei.simpletasks.domain.model.finance.MonthBudget
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import com.prostovasei.simpletasks.ui.feature.finance.monthLimit
import com.prostovasei.simpletasks.ui.feature.hub.HubNews
import com.prostovasei.simpletasks.ui.feature.hub.hubNews
import kotlinx.coroutines.flow.first
import java.time.YearMonth

/*
 * Виджет «Главная» (#329, этап 2; design/approved/329, §8 ТЗ). Тот же порядок, что у экрана: полоса лимита и счета,
 * прокручиваемый список задач «Просрочено · Сегодня · Завтра» (строки ленты [WidgetRows], нажатия — общий шаблон списка),
 * новости страницами по четыре 2 × 2. Свайпа вбок в виджетах нет — страницы листают стрелки ‹ ›.
 */

/** Новостей на странице и страниц в виджете: 12 новостей — три страницы. */
internal const val HUB_WIDGET_PAGE = 4

/** Модель задач хаба: просроченные, сегодня и завтра из ленты «Все» (без дней дальше завтра и без «без даты»). */
internal fun WidgetModel.forHub(): WidgetModel {
    val tomorrow = today.plusDays(1)
    return copy(days = days.filter { block -> (block.day as? WidgetDay.On)?.date?.let { it <= tomorrow } == true })
}

/** Страница новостей виджета: [page] по кругу в пределах [pages]. */
internal fun hubPage(page: Int, pages: Int): Int = if (pages <= 0) 0 else Math.floorMod(page, pages)

@RequiresApi(Build.VERSION_CODES.S)
internal suspend fun hubWidgetViews(context: Context, id: Int, model: WidgetModel, bar: RemoteViews, size: DpSize): RemoteViews {
    val width = size.width.value.takeIf { it.isFinite() }?.toInt() ?: 320
    val height = size.height.value.takeIf { it.isFinite() }?.toInt() ?: 640
    return hubViews(context, id, model, bar, hubFinance(context, width, height), hubNews(context))
}

/** Сборка без чтения данных — её проверяют тесты. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun hubViews(context: Context, id: Int, model: WidgetModel, bar: RemoteViews?, finance: List<RemoteViews>, news: List<HubNews>): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_hub_root).apply {
        removeAllViews(R.id.widget_hub_finance)
        finance.forEach { addView(R.id.widget_hub_finance, it) }

        val lines = if (model.isEmpty) emptyList() else model.lines(dayNames(context), withTail = false)
        setViewVisibility(R.id.widget_hub_list, if (lines.isEmpty()) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.widget_hub_empty, if (lines.isEmpty()) View.VISIBLE else View.GONE)
        val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(WidgetRows.VIEW_TYPES)
        lines.take(MAX_LINES).forEach { items.addItem(it.key.hashCode().toLong(), WidgetRows.views(context, it, model, liveClock = false)) }
        setRemoteAdapter(R.id.widget_hub_list, items.build())
        setPendingIntentTemplate(R.id.widget_hub_list, PendingIntent.getActivity(
            context, id,
            Intent(context, WidgetClickActivity::class.java).putExtra(WidgetActions.EXTRA_WIDGET_ID, id),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ))
        setOnClickPendingIntent(R.id.widget_hub_empty, PendingIntent.getActivity(
            context, id, quickAddIntent(context, WidgetFilter.All), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ))

        hubNewsViews(this, context, id, news)
        removeAllViews(R.id.widget_hub_bar)
        bar?.let { addView(R.id.widget_hub_bar, it) }
    }

/** Полоса лимита и до четырёх счетов — те же строки, что вверху виджета «Финансы». */
internal suspend fun hubFinance(context: Context, width: Int, height: Int): List<RemoteViews> {
    val entry = context.widgetEntryPoint()
    val repository = entry.finance()
    val rows = FinanceRows(context, width, height)
    val accounts = repository.observeAccounts().first()
    if (accounts.isEmpty()) return emptyList()
    val today = entry.time().today()
    val month = YearMonth.from(today)
    val operations = repository.observeOperations().first()
    val model = MonthSpending.of(operations, month, today)
    val budget = MonthBudget.of(BudgetRules.effective(repository.observeBudgets().first(), month, month),
        repository.observeCategories().first(), operations, month, today)
    return listOf(rows.limitViews(model, monthLimit(context, budget), month, today), rows.accountRow(accounts.take(HUB_ACCOUNTS), HUB_ACCOUNTS))
}

@RequiresApi(Build.VERSION_CODES.S)
private fun hubNewsViews(views: RemoteViews, context: Context, id: Int, news: List<HubNews>) {
    val pages = (news.size + HUB_WIDGET_PAGE - 1) / HUB_WIDGET_PAGE
    if (pages == 0) {
        views.setViewVisibility(R.id.widget_hub_news, View.GONE)
        return
    }
    views.setViewVisibility(R.id.widget_hub_news, View.VISIBLE)
    val page = hubPage(WidgetSelection.hubPage(context, id), pages)
    val shown = news.drop(page * HUB_WIDGET_PAGE).take(HUB_WIDGET_PAGE)
    listOf(R.id.widget_hub_news_row1, R.id.widget_hub_news_row2).forEachIndexed { row, rowId ->
        views.removeAllViews(rowId)
        repeat(2) { column ->
            val index = row * 2 + column
            val story = shown.getOrNull(index)
            val card = RemoteViews(context.packageName, R.layout.widget_hub_card)
            if (story == null) card.setViewVisibility(R.id.hub_card, View.INVISIBLE)
            else {
                card.setTextViewText(R.id.hub_card_meta, if (story.unread) "${story.meta} · ${context.getString(R.string.news_new_mark)}" else story.meta)
                card.setTextViewText(R.id.hub_card_title, story.title)
                if (story.unread) card.setTextViewText(R.id.hub_card_title, android.text.SpannableString(story.title).apply {
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
                })
                val open = Intent(context, WidgetNewsLinkActivity::class.java)
                    .setData("simpletasks://widget/$id/hub-news/$index".toUri())
                    .putExtra(EXTRA_NEWS_URL, story.url)
                card.setOnClickPendingIntent(R.id.hub_card, PendingIntent.getActivity(context, id * HUB_WIDGET_PAGE + index, open,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
            // Зазор сетки 8 dp (ТЗ §7) — отступом второй колонки.
            if (column == 1) card.setViewLayoutMargin(R.id.hub_card, RemoteViews.MARGIN_START, HUB_GRID_GAP, android.util.TypedValue.COMPLEX_UNIT_DIP)
            views.addView(rowId, card)
        }
    }
    views.setTextViewText(R.id.widget_hub_dots, (0 until pages).joinToString(" ") { if (it == page) "●" else "○" })
    views.setContentDescription(R.id.widget_hub_dots, context.getString(R.string.hub_news_page, page + 1, pages))
    val arrows = if (pages > 1) View.VISIBLE else View.INVISIBLE
    views.setViewVisibility(R.id.widget_hub_prev, arrows)
    views.setViewVisibility(R.id.widget_hub_next, arrows)
    views.setOnClickPendingIntent(R.id.widget_hub_prev, hubPageIntent(context, id, -1))
    views.setOnClickPendingIntent(R.id.widget_hub_next, hubPageIntent(context, id, 1))
}

private fun hubPageIntent(context: Context, id: Int, delta: Int): PendingIntent = PendingIntent.getBroadcast(
    context, id * 2 + (if (delta > 0) 1 else 0), WidgetActions.hubPage(context, id, delta),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private const val HUB_ACCOUNTS = 4
private const val HUB_GRID_GAP = 8f
