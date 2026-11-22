package com.prostovasei.simpletasks.data.news

import kotlinx.serialization.Serializable
import java.net.URI
import java.time.Duration
import java.util.Locale

/**
 * Уведомления о новостях (#296): чистая логика без Android, чтобы её проверяли обычные тесты.
 *
 * [known] — уже известные материалы (ключ → время публикации, секунды); null — проверок ещё не было.
 * [pending] — новые материалы, о которых ещё не уведомили (копятся в тишине).
 * [shown] — материалы в последнем показанном уведомлении: если оно ещё в шторке, новое уведомление их включает.
 */
@Serializable
internal data class NewsAlertState(
    val known: Map<String, Long>? = null,
    val pending: List<SportsStory> = emptyList(),
    val shown: List<SportsStory> = emptyList(),
)

/** Итог проверки: новое состояние и что показать (null — ничего не показывать). */
internal data class NewsAlertDecision(val state: NewsAlertState, val notify: List<SportsStory>?)

internal object NewsAlerts {
    /** Ключи старше этого срока забываем: в выдаче только последние 24 часа, запас — вдвое. */
    private val keep = Duration.ofHours(48).seconds

    /** Тот же ключ, что у ленты: хост без «www.» и путь. */
    fun key(story: SportsStory): String = runCatching {
        val uri = URI.create(story.url)
        "${uri.host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")}${uri.path}"
    }.getOrDefault(story.url)

    /**
     * Фоновая проверка. Первая — только запоминает выдачу. Дальше новые материалы копятся в [NewsAlertState.pending];
     * вне тишины накопленное уходит одним уведомлением вместе с ещё не снятым прежним ([shownActive]).
     */
    fun check(state: NewsAlertState, edition: SportsEdition, now: Long, quiet: Boolean, shownActive: Boolean): NewsAlertDecision {
        val known = state.known
        if (known == null) return NewsAlertDecision(NewsAlertState(remember(emptyMap(), edition, now), shown = state.shown), null)
        val fresh = edition.stories.filter { key(it) !in known }
        val pending = (fresh + state.pending).distinctBy(::key).filter { now - it.published <= keep }
        val base = NewsAlertState(remember(known, edition, now), pending, state.shown)
        if (quiet || pending.isEmpty()) return NewsAlertDecision(base, null)
        val previous = if (shownActive) state.shown else emptyList()
        val notify = (pending + previous).distinctBy(::key).sortedByDescending(SportsStory::published)
        return NewsAlertDecision(NewsAlertState(base.known, emptyList(), notify), notify)
    }

    /** Ручное обновление или открытая вкладка: всё в ленте уже видно, уведомлять о нём не нужно. */
    fun seen(state: NewsAlertState, edition: SportsEdition, now: Long): NewsAlertState =
        NewsAlertState(remember(state.known.orEmpty(), edition, now), emptyList(), emptyList())

    private fun remember(known: Map<String, Long>, edition: SportsEdition, now: Long): Map<String, Long> =
        (known + edition.stories.associate { key(it) to it.published }).filterValues { now - it <= keep }

    /** Минута суток [minute] внутри тишины [from]–[to]; период может переходить через полночь, from == to — тишины нет. */
    fun inQuiet(minute: Int, from: Int, to: Int): Boolean = when {
        from == to -> false
        from < to -> minute in from until to
        else -> minute >= from || minute < to
    }
}
