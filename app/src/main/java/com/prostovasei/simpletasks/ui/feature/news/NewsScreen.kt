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
import com.prostovasei.simpletasks.data.news.SportsNews

private val regular = FontFamily(Font(R.font.manrope_400))
private val bold = FontFamily(Font(R.font.manrope_700))

@Composable
internal fun NewsScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by SportsNews.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val inset = dimensionResource(R.dimen.widget_news_inset)
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

    LazyColumn(Modifier.fillMaxSize()) {
        items(newsFeedRows(context, state.status)) { row ->
            when (row) {
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
                    Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(row.url) } }
                        .padding(start = inset, end = inset, bottom = rowGap),
                ) {
                    Text(row.meta, style = meta)
                    Text(row.title, style = title)
                }
            }
        }
    }
}
