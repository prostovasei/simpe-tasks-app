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
    val medium: PriorityColors,
    val low: PriorityColors,
    val none: PriorityColors,
)

/** Рамка галочки по приоритету (итерация 13): как круглые галочки виджета, только в приложении — квадратные. */
@Immutable
data class CheckRings(val high: Color, val medium: Color, val low: Color, val none: Color)

/** Строки «Недели» (итерация 45; 46 — графит): фон по приоритету, цвета текста, времени и просрочки на высоком и среднем, отметка выполненной. */
@Immutable
data class WeekRowColors(
    val high: Color,
    val highContent: Color,
    val highTime: Color,
    val highOverdue: Color,
    val medium: Color,
    val mediumContent: Color,
    val mediumTime: Color,
    val mediumOverdue: Color,
    val low: Color,
    val doneMark: Color,
)

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
    val inboxList: Color,
    val onListSquare: Color,
    val checkRings: CheckRings,
    /** Галочка выполненной на залитом кружке по приоритету (итерация 46). */
    val checkMarks: CheckRings,
    val weekRow: WeekRowColors,
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
        accentSoft = ColorTokens.accentSoft.resolve(),
        onAccent = ColorTokens.onAccent.resolve(),
        overdue = ColorTokens.overdue.resolve(),
        divider = ColorTokens.divider.resolve(),
        selected = ColorTokens.selected.resolve(),
        inactive = ColorTokens.inactive.resolve(),
        inboxList = ColorTokens.inboxList.resolve(),
        onListSquare = ColorTokens.onListSquare.resolve(),
        checkRings = CheckRings(
            high = WidgetCheckTokens.high.ring.resolve(),
            medium = WidgetCheckTokens.medium.ring.resolve(),
            low = WidgetCheckTokens.low.ring.resolve(),
            none = WidgetCheckTokens.none.ring.resolve(),
        ),
        checkMarks = CheckRings(
            high = WidgetCheckTokens.high.check.resolve(),
            medium = WidgetCheckTokens.medium.check.resolve(),
            low = WidgetCheckTokens.low.check.resolve(),
            none = WidgetCheckTokens.none.check.resolve(),
        ),
        weekRow = WeekRowColors(
            high = WeekRowTokens.high.resolve(),
            highContent = WeekRowTokens.highContent.resolve(),
            highTime = WeekRowTokens.highTime.resolve(),
            highOverdue = WeekRowTokens.highOverdue.resolve(),
            medium = WeekRowTokens.medium.resolve(),
            mediumContent = WeekRowTokens.mediumContent.resolve(),
            mediumTime = WeekRowTokens.mediumTime.resolve(),
            mediumOverdue = WeekRowTokens.mediumOverdue.resolve(),
            low = WeekRowTokens.low.resolve(),
            doneMark = WeekRowTokens.doneMark.resolve(),
        ),
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
