package com.prostovasei.simpletasks.designsystem.tokens

/**
 * Цвет для светлой и тёмной темы в формате ARGB (0xAARRGGBB).
 * Чистый Kotlin без Compose: из этих значений строятся и AppTheme, и тема виджета Glance.
 */
data class ThemedColor(val light: Long, val dark: Long)

/** Пара цветов плашки приоритета: фон и текст. */
data class PriorityColorTokens(val container: ThemedColor, val content: ThemedColor)

/** Базовая палитра приложения. Единственный источник цветов (см. docs/ARCHITECTURE.md §1.5). */
object ColorTokens {
    /** Светло-зелёный фон экранов и виджета (итерация 13, макет PM). */
    val background = ThemedColor(light = 0xFFEDF5EF, dark = 0xFF0E1511)

    /** Белые плашки задач, меню, окна и нижняя панель поверх фона. */
    val surface = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF1A231E)
    val textPrimary = ThemedColor(light = 0xFF15161F, dark = 0xFFE6EAE7)
    val textSecondary = ThemedColor(light = 0xFF5F6474, dark = 0xFF9DA8A1)
    val accent = ThemedColor(light = 0xFF15803D, dark = 0xFF4FC07F)

    /** Бледный акцент: подсветка найденного в поиске (итерация 15). */
    val accentSoft = ThemedColor(light = 0xFFD3ECDB, dark = 0xFF1F4430)
    val onAccent = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF0A1A10)

    /** Значок поверх квадратика цвета списка (итерация 14). */
    val onListSquare = ThemedColor(light = 0xFFFFFFFF, dark = 0xFFFFFFFF)

    /** Квадратик списка «Входящие» (у него нет своего цвета). */
    val inboxList = ThemedColor(light = 0xFF3D8BFD, dark = 0xFF6AA8FF)

    /** Рамка пустой галочки без приоритета и неактивные иконки нижней панели. */
    val inactive = ThemedColor(light = 0xFFA8B3AB, dark = 0xFF5F6B64)
    val overdue = ThemedColor(light = 0xFFC62828, dark = 0xFFFF6B6B)
    val divider = ThemedColor(light = 0xFFD5E0D8, dark = 0xFF2C3731)

    /** Опасные действия (удаление). Совпадает с [overdue], но означает другое — отдельная роль. */
    val danger = ThemedColor(light = 0xFFC62828, dark = 0xFFFF6B6B)

    /** Затемнение под всплывающим окном (выбор списка виджета). */
    val scrim = ThemedColor(light = 0x66000000, dark = 0x99000000)
}

/**
 * Готовые цвета для списков (выбор в меню «Цвет»). Хранятся в БД как HEX-строка — это данные пользователя,
 * а набор вариантов — часть дизайн-системы.
 */
object ListColorTokens {
    val presets: List<String> = listOf(
        "#E5484D", "#F76B15", "#FFC53D", "#46A758", "#12A594", "#3D8BFD", "#8E4EC6", "#D6409F", "#8D8D8D",
    )
}

/** Цвета плашек приоритетов (SPEC.md §3). Текст подобран с контрастом не ниже 4.5:1. */
object PriorityTokens {
    val high = PriorityColorTokens(
        container = ThemedColor(light = 0xFFFFCDD2, dark = 0xFF5C1D1D),
        content = ThemedColor(light = 0xFFB71C1C, dark = 0xFFFFB4AB),
    )
    val medium = PriorityColorTokens(
        container = ThemedColor(light = 0xFFFFE082, dark = 0xFF543B0C),
        content = ThemedColor(light = 0xFF8A4B00, dark = 0xFFFFD180),
    )
    val low = PriorityColorTokens(
        container = ThemedColor(light = 0xFFBBDEFB, dark = 0xFF0D3C61),
        content = ThemedColor(light = 0xFF0D47A1, dark = 0xFF9CCBFF),
    )
    val none = PriorityColorTokens(
        container = ThemedColor(light = 0xFFEEEEEE, dark = 0xFF242424),
        content = ThemedColor(light = 0xFF424242, dark = 0xFFBDBDBD),
    )
}

/**
 * Круглая галочка виджета: приоритет — цветом обводки и лёгкой заливки (решение PM, редизайн виджета).
 * [ring] — обводка, [fill] — заливка: непрозрачный оттенок поверх плашки дня (полупрозрачный оттенок
 * виджеты рисуют неровно). У задач без приоритета заливки нет.
 */
data class CheckTokens(val ring: ThemedColor, val fill: ThemedColor)

object WidgetCheckTokens {
    val high = CheckTokens(ring = ThemedColor(light = 0xFFE5484D, dark = 0xFFFF6B6B), fill = ThemedColor(light = 0xFFF4DFE2, dark = 0xFF43292B))
    val medium = CheckTokens(ring = ThemedColor(light = 0xFFE09400, dark = 0xFFFFB020), fill = ThemedColor(light = 0xFFF4EAD7, dark = 0xFF43361E))
    val low = CheckTokens(ring = ThemedColor(light = 0xFF3D8BFD, dark = 0xFF6AA8FF), fill = ThemedColor(light = 0xFFDDE9FA, dark = 0xFF283446))
    val none = CheckTokens(ring = ThemedColor(light = 0xFF8B949E, dark = 0xFF7D8590), fill = ThemedColor(light = 0x00000000, dark = 0x00000000))
}
