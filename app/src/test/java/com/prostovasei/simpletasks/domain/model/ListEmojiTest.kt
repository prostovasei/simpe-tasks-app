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
