package com.prostovasei.simpletasks.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Эмодзи списков (итерация 47): разбор названий TickTick и выбор эмодзи для показа. */
class ListEmojiTest {

    @Test
    fun tickTickNameWithEmojiIsSplit() {
        assertEquals("🏠" to "Дом", ListEmoji.split("🏠Дом"))
        assertEquals("💼" to "Работа", ListEmoji.split("💼 Работа"))
        assertEquals("❤️" to "Здоровье", ListEmoji.split("❤️Здоровье"))
        assertEquals("👨‍👩‍👧" to "Семья", ListEmoji.split("👨‍👩‍👧Семья"))
        assertEquals("👍🏽" to "Дела", ListEmoji.split("👍🏽 Дела"))
    }

    @Test
    fun plainNamesStayAsTheyAre() {
        assertEquals(null to "Работа", ListEmoji.split("Работа"))
        assertEquals(null to "2026 планы", ListEmoji.split("2026 планы"))
        // Название из одного эмодзи не делится: иначе список остался бы без названия.
        assertEquals(null to "🏠", ListEmoji.split("🏠"))
        assertNull(ListEmoji.leading("Дом 🏠"))
    }

    @Test
    fun shownEmojiFallsBackToIconThenFolder() {
        assertEquals(ListEmoji.INBOX, ListEmoji.of(isInbox = true, emoji = "🏠", icon = null))
        assertEquals("🦄", ListEmoji.of(isInbox = false, emoji = "🦄", icon = "work"))
        assertEquals("💼", ListEmoji.of(isInbox = false, emoji = null, icon = "work"))
        assertEquals(ListEmoji.DEFAULT, ListEmoji.of(isInbox = false, emoji = null, icon = null))
    }

    /** 0.52.1: задача без списка — 📌 с датой и 📥 без даты; у задачи из списка — эмодзи списка. */
    @Test
    fun taskWithoutListShowsPinWhenDated() {
        assertEquals(ListEmoji.NO_LIST, ListEmoji.ofTask(isInbox = true, hasDate = true, listEmoji = ListEmoji.INBOX))
        assertEquals(ListEmoji.INBOX, ListEmoji.ofTask(isInbox = true, hasDate = false, listEmoji = ListEmoji.INBOX))
        assertEquals("🏠", ListEmoji.ofTask(isInbox = false, hasDate = true, listEmoji = "🏠"))
        assertEquals("🏠", ListEmoji.ofTask(isInbox = false, hasDate = false, listEmoji = "🏠"))
    }

    @Test
    fun singleEmojiIsRecognised() {
        assertTrue(ListEmoji.isSingle("🦄"))
        assertFalse(ListEmoji.isSingle("🦄 единорог"))
        assertFalse(ListEmoji.isSingle("а"))
    }

    @Test
    fun popularGridHasNoDuplicatesInsideAGroup() {
        ListEmoji.POPULAR.forEach { (group, emojis) -> assertEquals(group, emojis.size, emojis.toSet().size) }
    }
}
