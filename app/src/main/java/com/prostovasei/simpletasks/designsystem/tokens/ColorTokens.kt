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
    /** Еле-голубой фон экранов и виджета (итерация 12, вариант A). */
    val background = ThemedColor(light = 0xFFEEF4FC, dark = 0xFF0E141B)

    /** Белые плашки задач, меню и окна поверх фона. */
    val surface = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF1A222D)
    val textPrimary = ThemedColor(light = 0xFF1F2328, dark = 0xFFE6E6E6)
    val textSecondary = ThemedColor(light = 0xFF57606A, dark = 0xFF9DA5AE)
    val accent = ThemedColor(light = 0xFF0969DA, dark = 0xFF4C9AFF)
    val onAccent = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF0B1A2E)
    val overdue = ThemedColor(light = 0xFFD32F2F, dark = 0xFFFF6B6B)
    val divider = ThemedColor(light = 0xFFD0D7DE, dark = 0xFF30363D)

    /** Опасные действия (удаление). Совпадает с [overdue], но означает другое — отдельная роль. */
    val danger = ThemedColor(light = 0xFFD32F2F, dark = 0xFFFF6B6B)

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
