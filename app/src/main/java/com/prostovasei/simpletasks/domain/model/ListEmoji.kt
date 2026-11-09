package com.prostovasei.simpletasks.domain.model

import java.text.BreakIterator

/**
 * Эмодзи списка вместо значка и цвета (итерация 47, решение PM, макет `VariantH2`): у «Входящих» — [INBOX], у списка без своего
 * эмодзи — эмодзи по старому значку ([fromIcon]), иначе [DEFAULT]. TickTick хранит эмодзи в начале названия списка
 * («🏠Дом»): импорт отделяет его ([split]), экспорт возвращает в начало.
 */
object ListEmoji {
    const val INBOX = "📥"
    const val DEFAULT = "📁"

    /** «Без списка» (итерация 52.1, решение PM): задача без списка, но с датой; и строка раздела на «Списках». */
    const val NO_LIST = "📌"

    /** «Когда-нибудь» (итерация 52.1): строка раздела на «Списках». */
    const val SOMEDAY = "💭"

    /** Эмодзи у задачи: без списка — 📌 с датой и 📥 без даты (итерация 52.1), иначе эмодзи списка. */
    fun ofTask(isInbox: Boolean, hasDate: Boolean, listEmoji: String): String = when {
        !isInbox -> listEmoji
        hasDate -> NO_LIST
        else -> INBOX
    }

    /** Старые значки списков (итерация 14) → эмодзи; так же их переводит миграция БД 2 → 3. */
    val BY_ICON: Map<String, String> = linkedMapOf(
        "work" to "💼", "home" to "🏠", "health" to "❤️", "shopping" to "🛒", "study" to "📚",
        "money" to "💰", "travel" to "✈️", "sport" to "💪", "idea" to "💡", "star" to "⭐",
    )

    /** Быстрый выбор в окне создания списка и папки (итерация 56, макет iteration56.py); остальные — «…» → [POPULAR] и клавиатура. */
    val QUICK: List<String> = listOf("📁", "💼", "🏠", "🛒", "❤️", "💪", "📚", "💰", "✈️", "💡", "⭐", "🎮", "🏀", "🎯", "🧩")

    /** Сетка выбора: группы популярных эмодзи (решение PM: сетка + любой с клавиатуры). */
    val POPULAR: List<Pair<String, List<String>>> = listOf(
        "Дела" to listOf("📥", "📋", "✅", "📌", "⭐", "🔥", "💡", "🎯"),
        "Работа" to listOf("💼", "💻", "📊", "📈", "📝", "📅", "📞", "🗂️"),
        "Дом" to listOf("🏠", "🛒", "🧹", "🍳", "🛠️", "🌱", "🐶", "🧺"),
        "Здоровье" to listOf("❤️", "💪", "🏃", "🧘", "💊", "🥗", "😴", "🦷"),
        "Деньги" to listOf("💰", "💳", "🏦", "🧾", "📉", "🪙", "🎁", "🛍️"),
        "Отдых" to listOf("✈️", "🎬", "🎮", "📚", "🎵", "⚽", "🏀", "🎨"),
    )

    fun fromIcon(icon: String?): String? = icon?.let { BY_ICON[it] }

    /** Эмодзи, который показывается у списка. */
    fun of(isInbox: Boolean, emoji: String?, icon: String?): String = when {
        isInbox -> INBOX
        !emoji.isNullOrBlank() -> emoji
        else -> fromIcon(icon) ?: DEFAULT
    }

    /** Первый эмодзи строки (целиком, со всеми составными частями) или null, если строка начинается не с эмодзи. */
    fun leading(text: String): String? {
        val trimmed = text.trimStart()
        if (trimmed.isEmpty() || !isEmojiStart(trimmed.codePointAt(0))) return null
        val it = BreakIterator.getCharacterInstance().apply { setText(trimmed) }
        val end = it.next().takeIf { it != BreakIterator.DONE } ?: return null
        // BreakIterator старых Android режет составные эмодзи по ZWJ и модификаторам — дособираем вручную.
        var stop = end
        while (stop < trimmed.length) {
            val cp = trimmed.codePointAt(stop)
            val joins = cp == ZWJ || cp == VARIATION_SELECTOR || cp in SKIN_TONES || cp == KEYCAP ||
                (trimmed.codePointBefore(stop) == ZWJ && isEmojiStart(cp))
            if (!joins) break
            stop += Character.charCount(cp)
        }
        return trimmed.substring(0, stop)
    }

    /** «🏠 Дом» → (🏠, Дом); без эмодзи в начале — (null, название). Пустое после эмодзи название не отделяется. */
    fun split(name: String): Pair<String?, String> {
        val emoji = leading(name) ?: return null to name
        val rest = name.trimStart().removePrefix(emoji).trim()
        return if (rest.isEmpty()) null to name else emoji to rest
    }

    /** Строка — один эмодзи (для поля «Свой эмодзи»). */
    fun isSingle(text: String): Boolean = text.isNotBlank() && leading(text) == text.trim()

    private fun isEmojiStart(cp: Int): Boolean =
        cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27BF || cp in 0x2300..0x23FF || cp in 0x2B00..0x2BFF || cp == 0x3030 || cp == 0x303D || cp == 0x00A9 || cp == 0x00AE

    private const val ZWJ = 0x200D
    private const val VARIATION_SELECTOR = 0xFE0F
    private const val KEYCAP = 0x20E3
    private val SKIN_TONES = 0x1F3FB..0x1F3FF
}
