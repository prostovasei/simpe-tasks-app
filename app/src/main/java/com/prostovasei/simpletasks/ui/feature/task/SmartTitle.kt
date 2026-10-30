package com.prostovasei.simpletasks.ui.feature.task

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.smartdate.SmartDate
import com.prostovasei.simpletasks.domain.smartdate.SmartDateParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Выключатель «Дата и время в названии» в настройках (итерация 41, бэклог №29): по умолчанию включён. */
@Singleton
class SmartDateSetting(initial: Boolean, private val save: (Boolean) -> Unit) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private constructor(prefs: SharedPreferences) : this(
        initial = prefs.getBoolean(KEY, true),
        save = { value -> prefs.edit(commit = true) { putBoolean(KEY, value) } },
    )

    private val _enabled = MutableStateFlow(initial)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun set(value: Boolean) {
        save(value)
        _enabled.value = value
    }

    private companion object {
        const val PREFS = "smart_date"
        const val KEY = "enabled"
    }
}

/**
 * Срок из названия в шторке задачи (итерация 41): найденная фраза [found] подсвечивается, под названием — чип; срок задачи
 * ставится сразу. Фраза стёрта — срок возвращается к [before]; «✕» на чипе ([off]) — для этой задачи фразы больше не разбираются.
 * [known] — фразы, которые уже стояли в названии при открытии задачи: их не трогаем, срок у задачи уже свой.
 */
data class SmartTitle(
    val found: SmartDate? = null,
    val off: Boolean = false,
    val before: Schedule? = null,
    val known: List<String> = emptyList(),
) {
    /** Новое состояние и срок, который надо поставить (null — срок не меняется). */
    fun update(text: String, enabled: Boolean, today: LocalDate, nowMinute: Int, zone: ZoneId, current: Schedule): Pair<SmartTitle, Schedule?> {
        val parsed = SmartDateParser.parse(text, today, nowMinute)
            ?.takeIf { enabled && !off && it.phrases(text) != known }
        val previous = found
        return when {
            parsed == null && previous == null -> this to null
            parsed == null -> copy(found = null, before = null) to before
            previous != null && previous.date == parsed.date && previous.minute == parsed.minute -> copy(found = parsed) to null
            else -> copy(found = parsed, before = before ?: current) to Schedule(parsed.due(zone), null)
        }
    }

    /** «✕» на чипе: фраза остаётся обычным текстом, срок возвращается к прежнему. */
    fun dismiss(): Pair<SmartTitle, Schedule?> = copy(found = null, off = true, before = null) to before

    /** Название для сохранения: без фраз срока; если без них ничего не остаётся — как написано. */
    fun clean(text: String): String {
        val ranges = found?.ranges ?: return text.trim()
        return SmartDateParser.strip(text, ranges).ifBlank { text.trim() }
    }

    companion object {
        /** Для открытой задачи: фразы, уже стоящие в названии, не разбираются. */
        fun opened(title: String, today: LocalDate, nowMinute: Int): SmartTitle =
            SmartTitle(known = SmartDateParser.parse(title, today, nowMinute)?.phrases(title).orEmpty())
    }
}
