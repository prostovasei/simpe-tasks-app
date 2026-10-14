package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor

@Immutable
data class PriorityColors(val container: Color, val content: Color)

@Immutable
data class PriorityPalette(
    val high: PriorityColors,
    val medium: PriorityColors,
    val low: PriorityColors,
    val none: PriorityColors,
)

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val overdue: Color,
    val divider: Color,
    val danger: Color,
    val scrim: Color,
    val priority: PriorityPalette,
    val isDark: Boolean,
)

internal fun appColors(isDark: Boolean): AppColors {
    fun ThemedColor.resolve() = Color(if (isDark) dark else light)
    fun PriorityColorTokens.resolve() = PriorityColors(container.resolve(), content.resolve())

    return AppColors(
        background = ColorTokens.background.resolve(),
        surface = ColorTokens.surface.resolve(),
        textPrimary = ColorTokens.textPrimary.resolve(),
        textSecondary = ColorTokens.textSecondary.resolve(),
        accent = ColorTokens.accent.resolve(),
        onAccent = ColorTokens.onAccent.resolve(),
        overdue = ColorTokens.overdue.resolve(),
        divider = ColorTokens.divider.resolve(),
        danger = ColorTokens.danger.resolve(),
        scrim = ColorTokens.scrim.resolve(),
        priority = PriorityPalette(
            high = PriorityTokens.high.resolve(),
            medium = PriorityTokens.medium.resolve(),
            low = PriorityTokens.low.resolve(),
            none = PriorityTokens.none.resolve(),
        ),
        isDark = isDark,
    )
}
