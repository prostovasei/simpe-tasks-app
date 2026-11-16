package com.prostovasei.simpletasks.widget.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens
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
open class WidgetPalette(private val flat: Boolean = false) {
    val background = (if (flat) TaskCanvasTokens.background else ColorTokens.background).toProvider()
    val surface = ColorTokens.surface.toProvider()
    val textPrimary = ColorTokens.textPrimary.toProvider()
    val textSecondary = ColorTokens.textSecondary.toProvider()
    val accent = ColorTokens.accent.toProvider()
    val onAccent = ColorTokens.onAccent.toProvider()
    val overdue = ColorTokens.overdue.toProvider()
    val divider = (if (flat) TaskCanvasTokens.divider else ColorTokens.divider).toProvider()
    val selected = ColorTokens.selected.toProvider()
    val canvasPriority = TaskCanvasTokens.priority.toProvider()
    val canvasPriorityDone = TaskCanvasTokens.priorityDone.toProvider()
    val canvasDone = TaskCanvasTokens.done.toProvider()
    val canvasEmojiDone = ColorTokens.textPrimary.toProvider(TaskCanvasTokens.DONE_EMOJI_ALPHA)
    val inactive = ColorTokens.inactive.toProvider()
    val onListSquare = ColorTokens.onListSquare.toProvider()

    /** Выполненная задача (итерация 27): название, значки и квадратик списка бледнее — 70%. */
    val textDone = ColorTokens.textPrimary.toProvider(DONE_OPACITY)
    val iconDone = ColorTokens.textSecondary.toProvider(DONE_OPACITY)
    val accentDone = ColorTokens.accent.toProvider(DONE_OPACITY)
    val inactiveDone = ColorTokens.inactive.toProvider(DONE_OPACITY)

    val checkHigh = WidgetCheckTokens.high.toProviders()
    val checkNone = WidgetCheckTokens.none.toProviders()

    val priorityHigh = PriorityTokens.high.toProviders()
    val priorityNone = PriorityTokens.none.toProviders()
    private fun ThemedColor.toProvider(alpha: Float = 1f): ColorProvider =
        ColorProvider(day = Color(light).copy(alpha = alpha), night = Color(if (flat) light else dark).copy(alpha = alpha))
    private fun PriorityColorTokens.toProviders() = WidgetPriorityColors(container.toProvider(), content.toProvider())
    private fun CheckTokens.toProviders() = WidgetCheckColors(ring.toProvider(), fill.toProvider(), done.toProvider())
}
object WidgetColors : WidgetPalette()
object TaskWidgetColors : WidgetPalette(flat = true)
val LocalWidgetColors = staticCompositionLocalOf<WidgetPalette> { WidgetColors }

data class WidgetCheckColors(val ring: ColorProvider, val fill: ColorProvider, val done: ColorProvider)
data class WidgetPriorityColors(val container: ColorProvider, val content: ColorProvider)

private const val DONE_OPACITY = 0.7f
