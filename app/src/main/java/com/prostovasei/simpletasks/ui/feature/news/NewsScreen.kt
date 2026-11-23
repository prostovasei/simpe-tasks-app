package com.prostovasei.simpletasks.ui.feature.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.NewsRead
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.widget.NewsWidgets

private val regular = FontFamily(Font(R.font.manrope_400))
private val semibold = FontFamily(Font(R.font.manrope_600))
private val bold = FontFamily(Font(R.font.manrope_700))

@Composable
internal fun NewsScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by SportsNews.state.collectAsStateWithLifecycle()
    // #300: отметки «прочитано» перерисовывают ленту; виджет обновляется тем же списком.
    val read by NewsRead.revision.collectAsStateWithLifecycle()
    // Для TalkBack: полужирность не слышна, поэтому новая новость помечается словом «Новая».
    val newLabel = stringResource(R.string.news_unread_state)
    val density = LocalDensity.current
    val inset = dimensionResource(R.dimen.widget_news_inset)
    val metaGap = dimensionResource(R.dimen.widget_news_meta_gap)
    val rowGap = dimensionResource(R.dimen.widget_news_row_gap)
    val dayInset = dimensionResource(R.dimen.widget_news_day_inset)
    val primary = colorResource(R.color.widget_flat_text_primary)
    val secondary = colorResource(R.color.widget_flat_text_secondary)
    val titleSize = dimensionResource(R.dimen.widget_news_title)
    val metaSize = dimensionResource(R.dimen.widget_news_meta)
    val daySize = dimensionResource(R.dimen.widget_news_day)
    val title = TextStyle(color = primary, fontSize = with(density) { titleSize.toSp() }, fontFamily = regular)
    val meta = TextStyle(color = secondary, fontSize = with(density) { metaSize.toSp() }, fontFamily = regular)
    val day = TextStyle(color = secondary, fontSize = with(density) { daySize.toSp() }, fontFamily = bold)

    val rows = remember(state, read) { newsFeedRows(context, state.status) }
    LazyColumn(Modifier.fillMaxSize()) {
        items(rows) { row ->
            when (row) {
                is NewsFeedRow.Unread -> Row(
                    Modifier.fillMaxWidth().padding(start = inset, end = inset, top = dayInset).testTag(TAG_NEWS_UNREAD),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.news_unread_count, row.count), style = day)
                    Text(
                        stringResource(R.string.news_read_all),
                        style = day.copy(color = primary),
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                NewsRead.markAllRead(context)
                                NewsWidgets.refreshList(context)
                            }
                            .padding(vertical = dayInset)
                            .testTag(TAG_NEWS_READ_ALL),
                    )
                }
                is NewsFeedRow.Day -> Row(
                    Modifier.fillMaxWidth().padding(horizontal = inset, vertical = dayInset),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.title, style = day)
                    row.updatedAt?.let { time ->
                        val description = stringResource(R.string.news_updated, time)
                        Text(time, style = meta, modifier = Modifier.clearAndSetSemantics {
                            contentDescription = description
                        })
                    }
                }
                is NewsFeedRow.Message -> Text(row.text, style = title,
                    modifier = Modifier.padding(start = inset, end = inset, bottom = rowGap))
                is NewsFeedRow.Article -> Column(
                    Modifier.fillMaxWidth().clickable {
                        NewsRead.markRead(context, row.url)
                        NewsWidgets.refreshList(context)
                        runCatching { uriHandler.openUri(row.url) }
                    }
                        .padding(start = inset, end = inset, bottom = rowGap),
                ) {
                    if (row.meta.isNotBlank()) Text(row.meta, style = meta, modifier = Modifier.padding(bottom = metaGap))
                    Text(row.title, style = if (row.unread) title.copy(fontFamily = semibold) else title, modifier = Modifier.testTag(if (row.unread) TAG_NEWS_TITLE_UNREAD else TAG_NEWS_TITLE)
                        .then(if (row.unread) Modifier.semantics { stateDescription = newLabel } else Modifier))
                }
            }
        }
    }
}

const val TAG_NEWS_UNREAD = "news_unread"
const val TAG_NEWS_READ_ALL = "news_read_all"
const val TAG_NEWS_TITLE = "news_title"
const val TAG_NEWS_TITLE_UNREAD = "news_title_unread"
