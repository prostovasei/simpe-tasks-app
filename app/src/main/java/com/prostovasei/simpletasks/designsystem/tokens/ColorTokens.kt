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
    val background = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF121212)
    val surface = ThemedColor(light = 0xFFF6F8FA, dark = 0xFF1E1E1E)
    val textPrimary = ThemedColor(light = 0xFF1F2328, dark = 0xFFE6E6E6)
    val textSecondary = ThemedColor(light = 0xFF57606A, dark = 0xFF9DA5AE)
    val accent = ThemedColor(light = 0xFF0969DA, dark = 0xFF4C9AFF)
    val onAccent = ThemedColor(light = 0xFFFFFFFF, dark = 0xFF0B1A2E)
    val overdue = ThemedColor(light = 0xFFD32F2F, dark = 0xFFFF6B6B)
    val divider = ThemedColor(light = 0xFFD0D7DE, dark = 0xFF30363D)
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
