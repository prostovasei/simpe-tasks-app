package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.designsystem.tokens.WeekRowTokens
import com.prostovasei.simpletasks.designsystem.tokens.WidgetCheckTokens

@Immutable
data class PriorityColors(val container: Color, val content: Color)

@Immutable
data class PriorityPalette(
    val high: PriorityColors,
    val none: PriorityColors,
)

/** Рамка галочки по приоритету (итерация 13): как круглые галочки виджета, только в приложении — квадратные. */
@Immutable
/** [highDone] — заливка выполненной задачи высокого приоритета (итерация 47: тусклый фиолетовый). */
data class CheckRings(val high: Color, val highDone: Color)

/** Строки «Недели» (итерация 45): фон по приоритету и серая отметка выполненной. */
@Immutable
data class WeekRowColors(val high: Color, val doneMark: Color)

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val overdue: Color,
    val divider: Color,
    val selected: Color,
    val inactive: Color,
    val onListSquare: Color,
    val checkRings: CheckRings,
    val weekRow: WeekRowColors,
    val danger: Color,
    val income: Color,
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
        accentSoft = ColorTokens.accentSoft.resolve(),
        onAccent = ColorTokens.onAccent.resolve(),
        overdue = ColorTokens.overdue.resolve(),
        divider = ColorTokens.divider.resolve(),
        selected = ColorTokens.selected.resolve(),
        inactive = ColorTokens.inactive.resolve(),
        onListSquare = ColorTokens.onListSquare.resolve(),
        checkRings = CheckRings(
            high = WidgetCheckTokens.high.ring.resolve(),
            highDone = WidgetCheckTokens.high.done.resolve(),
        ),
        weekRow = WeekRowColors(
            high = WeekRowTokens.high.resolve(),
            doneMark = WeekRowTokens.doneMark.resolve(),
        ),
        danger = ColorTokens.danger.resolve(),
        income = ColorTokens.income.resolve(),
        scrim = ColorTokens.scrim.resolve(),
        priority = PriorityPalette(
            high = PriorityTokens.high.resolve(),
            none = PriorityTokens.none.resolve(),
        ),
        isDark = isDark,
    )
}
