package com.prostovasei.simpletasks.ui.feature.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsNews
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun sportsTime(seconds: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))

internal fun sportsDay(seconds: Long): LocalDate =
    Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
internal fun NewsScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by SportsNews.state.collectAsStateWithLifecycle()
    val status = state.status
    val stories = SportsNews.cached(context)?.recent().orEmpty()
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxSize().padding(horizontal = spacing.md)) {
        Text(stringResource(R.string.module_news), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = spacing.md))
        Text(stringResource(R.string.news_source), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.sm))
        if (status == SportsNews.Status.LOADING) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status == SportsNews.Status.FAILED) Text(stringResource(R.string.news_failed))
        if (stories.isEmpty() && status == SportsNews.Status.IDLE) Text(stringResource(R.string.news_empty))
        LazyColumn {
            itemsIndexed(stories, key = { _, story -> story.url }) { index, story ->
                val date = sportsDay(story.published)
                if (index == 0 || sportsDay(stories[index - 1].published) != date) {
                    val name = if (date == LocalDate.now()) R.string.day_today else R.string.day_yesterday
                    Text(stringResource(name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = spacing.sm))
                }
                Column(Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(story.url) } }
                    .padding(vertical = spacing.sm)) {
                    val time = sportsTime(story.published)
                    Text(listOf(time, story.sport).filter(String::isNotBlank).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(story.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
        }
    }
}
