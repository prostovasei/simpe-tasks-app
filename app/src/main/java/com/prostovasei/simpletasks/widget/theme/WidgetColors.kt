package com.prostovasei.simpletasks.widget.theme

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.prostovasei.simpletasks.designsystem.tokens.CheckTokens
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.PriorityTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.designsystem.tokens.WidgetCheckTokens

/**
 * Цвета виджета Glance из тех же токенов, что и AppTheme.
 * Glance сам выбирает светлый или тёмный вариант по теме системы.
 */
object WidgetColors {
    val background = ColorTokens.background.toProvider()
    val surface = ColorTokens.surface.toProvider()
    val textPrimary = ColorTokens.textPrimary.toProvider()
    val textSecondary = ColorTokens.textSecondary.toProvider()
    val accent = ColorTokens.accent.toProvider()
    val onAccent = ColorTokens.onAccent.toProvider()
    val overdue = ColorTokens.overdue.toProvider()
    val divider = ColorTokens.divider.toProvider()
    val reminderBadge = ColorTokens.reminderBadge.toProvider()
    val inactive = ColorTokens.inactive.toProvider()
    val inboxList = ColorTokens.inboxList.toProvider()
    val onListSquare = ColorTokens.onListSquare.toProvider()

    val checkHigh = WidgetCheckTokens.high.toProviders()
    val checkMedium = WidgetCheckTokens.medium.toProviders()
    val checkLow = WidgetCheckTokens.low.toProviders()
    val checkNone = WidgetCheckTokens.none.toProviders()

    val priorityHigh = PriorityTokens.high.toProviders()
    val priorityMedium = PriorityTokens.medium.toProviders()
    val priorityLow = PriorityTokens.low.toProviders()
    val priorityNone = PriorityTokens.none.toProviders()
}

data class WidgetCheckColors(val ring: ColorProvider, val fill: ColorProvider)

data class WidgetPriorityColors(val container: ColorProvider, val content: ColorProvider)

private fun ThemedColor.toProvider(): ColorProvider = ColorProvider(day = Color(light), night = Color(dark))

private fun PriorityColorTokens.toProviders() = WidgetPriorityColors(container.toProvider(), content.toProvider())

private fun CheckTokens.toProviders() = WidgetCheckColors(ring.toProvider(), fill.toProvider())
