package com.prostovasei.simpletasks.ui.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

/** Недавние запросы (итерация 38): порядок, повторы, предел и сохранение. */
class RecentSearchesTest {
    private val saved = mutableListOf<List<String>>()

    @Test
    fun newestOnTopNoRepeatsAtMostFive() {
        val recents = RecentSearches(emptyList()) { saved += it }
        listOf("а", "б", "в", "г", "д", "е").forEach(recents::add)
        assertEquals(listOf("е", "д", "г", "в", "б"), recents.items.value)

        recents.add("В")
        assertEquals("Повтор без учёта регистра поднимается наверх", listOf("В", "е", "д", "г", "б"), recents.items.value)
        assertEquals(recents.items.value, saved.last())

        recents.add("   ")
        assertEquals("Пустой запрос не запоминается", 5, recents.items.value.size)
    }

    @Test
    fun removeAndClear() {
        val recents = RecentSearches(listOf("а", "б")) { saved += it }
        recents.remove("а")
        assertEquals(listOf("б"), recents.items.value)
        recents.clear()
        assertEquals(emptyList<String>(), recents.items.value)
        assertEquals(emptyList<String>(), saved.last())
    }
}
