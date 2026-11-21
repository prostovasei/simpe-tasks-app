package com.prostovasei.simpletasks.data.news

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
internal data class SportsStory(val url: String, val title: String, val sport: String, val published: Long)

@Serializable
internal data class SportsEdition(val updated: Long, val stories: List<SportsStory>) {
    fun recent(now: Instant = Instant.now()) = stories.filter {
        it.published in now.minus(Duration.ofHours(24)).epochSecond..now.epochSecond
    }
}

internal object SportsNews {
    private val sports = mapOf(
        "football" to "Футбол", "tennis" to "Теннис", "figure-skating" to "Фигурное катание",
        "boxing" to "Бокс/MMA/UFC", "basketball" to "Баскетбол", "hockey" to "Хоккей",
        "automoto" to "Авто", "biathlon" to "Биатлон", "skiing" to "Лыжи", "rugby" to "Регби",
        "athletics" to "Лёгкая атлетика", "chess" to "Шахматы", "amfootball" to "Американский футбол",
        "sailing" to "Парусный спорт", "aquatics" to "Водные виды спорта", "skating" to "Коньки",
        "fencing" to "Фехтование", "gymnastics" to "Гимнастика", "martialarts" to "Единоборства",
        "volleyball" to "Волейбол", "cycling" to "Велоспорт", "handball" to "Гандбол",
    )
    private const val SITE = "https://www.sports.ru/news/top/"
    // У виджета обновление запускается фоновым broadcast, чья обработка ограничена по времени.
    private const val LOAD_BUDGET_MILLIS = 20_000
    private val zone = ZoneId.of("Europe/Moscow")
    private val dateFormat = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.forLanguageTag("ru"))
    private val lock = Mutex()
    data class State(val status: Status = Status.IDLE, val revision: Long = 0)
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    enum class Status { IDLE, LOADING, FAILED }

    private fun preferences(context: Context) = context.getSharedPreferences("sports_news", Context.MODE_PRIVATE)

    fun cached(context: Context): SportsEdition? = preferences(context).getString("edition", null)?.let {
        runCatching { Json.decodeFromString<SportsEdition>(it) }.getOrNull()
    }

    suspend fun refresh(context: Context, onChanged: suspend () -> Unit) {
        if (!lock.tryLock()) return
        try {
            mutableState.value = mutableState.value.copy(status = Status.LOADING)
            onChanged()
            val edition = withContext(Dispatchers.IO) {
                try {
                    load(Instant.now()).also {
                        check(preferences(context).edit().putString("edition", Json.encodeToString(SportsEdition.serializer(), it)).commit())
                    }
                } catch (_: java.io.IOException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                } catch (_: IllegalStateException) {
                    null
                } catch (_: java.time.DateTimeException) {
                    null
                }
            }
            mutableState.value = State(
                status = if (edition == null) Status.FAILED else Status.IDLE,
                revision = mutableState.value.revision + 1,
            )
        } finally {
            try {
                if (mutableState.value.status == Status.LOADING) {
                    mutableState.value = State(revision = mutableState.value.revision + 1)
                }
                withContext(NonCancellable) { onChanged() }
            } finally {
                lock.unlock()
            }
        }
    }

    internal fun load(
        now: Instant,
        fetch: (String, Int) -> Document = { url, timeout -> Jsoup.connect(url).timeout(timeout).get() },
    ): SportsEdition {
        val cutoff = now.minus(Duration.ofHours(24)).epochSecond
        val stories = linkedMapOf<String, SportsStory>()
        var page = 1
        val started = System.nanoTime()
        while (true) {
            val remaining = LOAD_BUDGET_MILLIS - (System.nanoTime() - started) / 1_000_000
            check(remaining > 0)
            val url = if (page == 1) SITE else "$SITE?page=$page"
            val panel = checkNotNull(fetch(url, remaining.toInt()).selectFirst("li.panel.active-panel"))
            val blocks = panel.select(".news .short-news")
            check(blocks.isNotEmpty()) { "На странице нет списка главных новостей" }
            val rows = blocks.flatMap { it.children() }
            var date: LocalDate? = null
            var oldest = Long.MAX_VALUE
            for (row in rows) {
                if (row.normalName() == "b") {
                    val day = row.text().trim()
                    val today = now.atZone(zone).toLocalDate()
                    date = if (day.last().isDigit()) LocalDate.parse(day, dateFormat) else {
                        val candidate = LocalDate.parse("$day ${today.year}", dateFormat)
                        if (candidate.isAfter(today)) candidate.minusYears(1) else candidate
                    }
                }
                if (row.normalName() != "p") continue
                val time = row.selectFirst(".time") ?: continue
                val published = checkNotNull(date).atTime(LocalTime.parse(time.text())).atZone(zone).toEpochSecond()
                oldest = minOf(oldest, published)
                if (published !in cutoff..now.epochSecond || row.selectFirst(".tests-games") != null) continue
                val link = checkNotNull(row.selectFirst("a.short-text"))
                val uri = URI.create(link.absUrl("href"))
                val host = uri.host.orEmpty().lowercase(Locale.ROOT)
                if (uri.scheme != "https" || host != "sports.ru" && !host.endsWith(".sports.ru")) continue
                val sport = sports[uri.path.substringAfter('/').substringBefore('/')].orEmpty()
                val key = "${host.removePrefix("www.")}${uri.path}"
                stories.putIfAbsent(key, SportsStory(uri.toString(), link.text(), sport, published))
            }
            val hasNext = panel.select("a[href]").any {
                val next = runCatching { URI.create(it.absUrl("href")) }.getOrNull()
                next?.path == "/news/top/" && next.query == "page=${page + 1}"
            }
            check(oldest != Long.MAX_VALUE || !hasNext && rows.isEmpty())
            if (oldest < cutoff || !hasNext) break
            page++
        }
        return SportsEdition(now.epochSecond, stories.values.sortedByDescending(SportsStory::published))
    }
}
