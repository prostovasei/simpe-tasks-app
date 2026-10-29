package com.prostovasei.simpletasks.ui.feature.search

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Недавние запросы в поиске (итерация 38, бэклог №31): последние [LIMIT], новые сверху, без повторов (без учёта регистра).
 * Хранятся на телефоне; запрос запоминается, когда открыли найденную задачу или нажали «поиск» на клавиатуре.
 */
@Singleton
class RecentSearches(initial: List<String>, private val persist: (List<String>) -> Unit) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private constructor(prefs: SharedPreferences) : this(
        initial = prefs.getString(KEY, "").orEmpty().split(SEPARATOR).filter { it.isNotBlank() }.take(LIMIT),
        persist = { list -> prefs.edit(commit = true) { putString(KEY, list.joinToString(SEPARATOR.toString())) } },
    )

    private val _items = MutableStateFlow(initial)
    val items: StateFlow<List<String>> = _items.asStateFlow()

    fun add(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        set(listOf(text) + _items.value.filterNot { it.equals(text, ignoreCase = true) })
    }

    fun remove(query: String) = set(_items.value.filterNot { it == query })

    fun clear() = set(emptyList())

    private fun set(list: List<String>) {
        val next = list.take(LIMIT)
        _items.value = next
        persist(next)
    }

    companion object {
        const val LIMIT = 5
        private const val PREFS = "recent_searches"
        private const val KEY = "items"
        private const val SEPARATOR = '\n'
    }
}
