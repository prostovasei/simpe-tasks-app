package com.prostovasei.simpletasks.data.news

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant

/**
 * Прочитанные новости (#300, ответы PM: 3В, 4Г, 5Б). Новая — та, которой нет среди прочитанных; прочитанной она становится
 * при нажатии на неё или по «Прочитать все» — в приложении и в виджете. Вход во вкладку новости не прочитывает.
 *
 * В первый раз (после обновления приложения) всё, что уже есть в ленте, считается прочитанным — иначе вся лента разом стала бы
 * полужирной. Ключ новости — как у ленты и уведомлений ([NewsAlerts.key]); ключи старше 48 часов забываются.
 */
internal object NewsRead {
    private const val PREFS = "news_read"
    private const val KEY_READ = "read"
    private const val KEY_READY = "ready"
    private val keep = Duration.ofHours(48).seconds

    private val mutableRevision = MutableStateFlow(0)

    /** Меняется после каждой отметки — экран ленты перерисовывается. */
    val revision: StateFlow<Int> = mutableRevision.asStateFlow()

    /** Ключи непрочитанных новостей выпуска [edition]. */
    fun unread(context: Context, edition: SportsEdition?): Set<String> {
        edition ?: return emptySet()
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_READY, false)) {
            save(context, edition.stories.associate { NewsAlerts.key(it) to it.published }, Instant.now().epochSecond)
            prefs.edit { putBoolean(KEY_READY, true) }
            return emptySet()
        }
        val read = read(context)
        return edition.stories.map(NewsAlerts::key).filterTo(linkedSetOf()) { it !in read }
    }

    /** Новость по адресу [url] прочитана — нажатие на неё в ленте или виджете. */
    fun markRead(context: Context, url: String) {
        val story = SportsNews.cached(context)?.stories?.firstOrNull { it.url == url }
        val key = story?.let(NewsAlerts::key) ?: return
        save(context, read(context) + (key to story.published), Instant.now().epochSecond)
    }

    /** «Прочитать все»: прочитана вся текущая лента. */
    fun markAllRead(context: Context) {
        val edition = SportsNews.cached(context) ?: return
        save(context, read(context) + edition.stories.associate { NewsAlerts.key(it) to it.published }, Instant.now().epochSecond)
    }

    private fun read(context: Context): Map<String, Long> =
        prefs(context).getStringSet(KEY_READ, emptySet()).orEmpty().mapNotNull { entry ->
            val cut = entry.lastIndexOf('|')
            if (cut < 0) null else entry.substring(0, cut) to (entry.substring(cut + 1).toLongOrNull() ?: return@mapNotNull null)
        }.toMap()

    private fun save(context: Context, read: Map<String, Long>, now: Long) {
        val kept = read.filterValues { now - it <= keep }.map { (key, published) -> "$key|$published" }.toSet()
        prefs(context).edit { putStringSet(KEY_READ, kept) }
        mutableRevision.value++
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
