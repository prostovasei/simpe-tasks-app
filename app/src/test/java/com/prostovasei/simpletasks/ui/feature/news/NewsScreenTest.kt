package com.prostovasei.simpletasks.ui.feature.news

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.data.news.SportsEdition
import com.prostovasei.simpletasks.data.news.SportsStory
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.ui.navigation.AppBottomBar
import com.prostovasei.simpletasks.ui.navigation.AppModule
import com.prostovasei.simpletasks.ui.navigation.BarPage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class NewsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedNewsShowsBothDaysAndOpensTheStory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("sports_news", Context.MODE_PRIVATE)
        val defaultZone = TimeZone.getDefault()
        try {
            val now = Instant.now()
            // Держим время теста вдали от полуночи, чтобы даты и время на снимке были стабильны.
            val zone = ZoneOffset.ofHours(12 - now.atOffset(ZoneOffset.UTC).hour)
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val today = now.atOffset(zone).toLocalDate()
            val yesterday = today.minusDays(1)
            fun story(day: LocalDate, time: String, path: String, sport: String, title: String) = SportsStory(
                url = "https://www.sports.ru/$path.html",
                title = title,
                sport = sport,
                published = day.atTime(LocalTime.parse(time)).toEpochSecond(zone),
            )
            val latest = story(today, "00:50", "football/match", "Футбол", "Казанская команда выиграла матч со счётом 2:1")
            val edition = SportsEdition(now.epochSecond, listOf(
                latest,
                story(today, "00:35", "hockey/match", "Хоккей", "Нападающий забросил три шайбы в домашней игре"),
                story(yesterday, "23:55", "tennis/final", "Теннис", "Определились финалисты итогового турнира"),
                story(yesterday, "23:45", "basketball/match", "Баскетбол", "Матч лиги завершился в овертайме"),
                story(yesterday, "23:30", "skiing/race", "Лыжи", "Лыжники открыли сезон спринтерской гонкой"),
                story(yesterday.minusDays(1), "23:59", "football/archive", "Футбол", "Архивный материал"),
            ))
            preferences.edit().putString("edition", Json.encodeToString(SportsEdition.serializer(), edition)).commit()

            val opened = mutableListOf<String>()
            val linkHandler = object : UriHandler {
                override fun openUri(uri: String) {
                    opened += uri
                }
            }
            composeRule.setContent {
                TaskCanvasTheme {
                    CompositionLocalProvider(LocalUriHandler provides linkHandler) {
                        Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                            Box(Modifier.weight(1f)) { NewsScreen() }
                            AppBottomBar(
                                pages = listOf(
                                    BarPage("tasks", R.drawable.ic_module_tasks, R.string.module_tasks, "module:TASKS"),
                                    BarPage("finance", R.drawable.ic_module_finance, R.string.module_finance, "module:FINANCE"),
                                    BarPage("health", R.drawable.ic_module_health, R.string.module_health, "module:HEALTH"),
                                    BarPage("news", R.drawable.ic_module_news, R.string.module_news, "module:NEWS"),
                                ),
                                selected = "news",
                                onSelect = {},
                                onAdd = {},
                                addTag = "news:refresh",
                                addLabel = R.string.news_refresh,
                                module = AppModule.TASKS,
                                onModule = {},
                                onSettings = {},
                                inlineModules = true,
                                refreshAction = true,
                            )
                        }
                    }
                }
            }

            composeRule.onNodeWithText("Новости").assertDoesNotExist()
            composeRule.onNodeWithText("Sports.ru · последние 24 часа").assertDoesNotExist()
            val updated = now.atOffset(zone).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            composeRule.onNodeWithContentDescription("Обновлено в $updated").assertExists()
            composeRule.onNodeWithText("Сегодня").assertExists()
            composeRule.onNodeWithText("Вчера").assertExists()
            composeRule.onNodeWithText("00:50 · Футбол").assertExists()
            composeRule.onNodeWithText("23:55 · Теннис").assertExists()
            composeRule.onNodeWithText("Архивный материал").assertDoesNotExist()
            composeRule.onNodeWithTag("module:NEWS").assertIsSelected()
            composeRule.onNodeWithContentDescription("Обновить новости").assertExists()
            composeRule.onRoot().captureRoboImage("screenshots/news_feed_light.png")

            composeRule.onNodeWithText(latest.title).performClick()
            composeRule.runOnIdle { assertEquals(listOf(latest.url), opened) }
        } finally {
            preferences.edit().remove("edition").commit()
            TimeZone.setDefault(defaultZone)
        }
    }

    /** #300: новые новости полужирные, строка «Новые · N — Прочитать все», нажатие и «Прочитать все» снимают выделение. */
    @Test
    fun unreadNewsAreBoldUntilOpenedOrReadAll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("sports_news", Context.MODE_PRIVATE)
        val defaultZone = TimeZone.getDefault()
        try {
            val now = Instant.now()
            val zone = ZoneOffset.ofHours(12 - now.atOffset(ZoneOffset.UTC).hour)
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val today = now.atOffset(zone).toLocalDate()
            val yesterday = today.minusDays(1)
            fun story(day: LocalDate, time: String, path: String, sport: String, title: String) = SportsStory(
                "https://www.sports.ru/$path.html", title, sport, day.atTime(LocalTime.parse(time)).toEpochSecond(zone),
            )
            val old = listOf(
                story(yesterday, "23:55", "tennis/final", "Теннис", "Определились финалисты итогового турнира"),
                story(yesterday, "23:45", "basketball/match", "Баскетбол", "Матч лиги завершился в овертайме"),
            )
            fun save(stories: List<SportsStory>) = SportsEdition(now.epochSecond, stories).also {
                preferences.edit().putString("edition", Json.encodeToString(SportsEdition.serializer(), it)).commit()
            }
            // Прошлое открытие ленты: старые новости уже видели.
            com.prostovasei.simpletasks.data.news.NewsRead.unread(context, save(old))
            val latest = story(today, "00:50", "football/match", "Футбол", "Казанская команда выиграла матч со счётом 2:1")
            save(listOf(latest, story(today, "00:35", "hockey/match", "Хоккей", "Нападающий забросил три шайбы в домашней игре")) + old)

            val opened = mutableListOf<String>()
            val linkHandler = object : UriHandler { override fun openUri(uri: String) { opened += uri } }
            composeRule.setContent {
                TaskCanvasTheme {
                    CompositionLocalProvider(LocalUriHandler provides linkHandler) {
                        Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) { NewsScreen() }
                    }
                }
            }

            composeRule.onNodeWithText("Новые · 2").assertExists()
            assertEquals(2, composeRule.onAllNodesWithTag(TAG_NEWS_TITLE_UNREAD, useUnmergedTree = true).fetchSemanticsNodes().size)
            assertEquals(2, composeRule.onAllNodesWithTag(TAG_NEWS_TITLE, useUnmergedTree = true).fetchSemanticsNodes().size)
            // #307: у новых в строке времени «Новое».
            assertEquals(2, composeRule.onAllNodesWithText("· Новое", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
            composeRule.onRoot().captureRoboImage("screenshots/news_feed_unread_light.png")

            composeRule.onNodeWithText(latest.title).performClick()
            composeRule.runOnIdle { assertEquals(listOf(latest.url), opened) }
            composeRule.onNodeWithText("Новые · 1").assertExists()

            composeRule.onNodeWithTag(TAG_NEWS_READ_ALL).performClick()
            composeRule.onNodeWithTag(TAG_NEWS_UNREAD).assertDoesNotExist()
            assertEquals(0, composeRule.onAllNodesWithTag(TAG_NEWS_TITLE_UNREAD, useUnmergedTree = true).fetchSemanticsNodes().size)
        } finally {
            preferences.edit().remove("edition").commit()
            TimeZone.setDefault(defaultZone)
        }
    }
}
