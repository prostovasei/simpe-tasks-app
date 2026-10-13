package com.prostovasei.simpletasks.designsystem.tokens

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Проверяет читаемость текста по WCAG AA: контраст не ниже 4.5:1 в обеих темах. */
class ColorTokensContrastTest {

    @Test
    fun priorityBadgeTextIsReadable() {
        val priorities = mapOf(
            "high" to PriorityTokens.high,
            "medium" to PriorityTokens.medium,
            "low" to PriorityTokens.low,
            "none" to PriorityTokens.none,
        )
        priorities.forEach { (name, tokens) ->
            assertReadable("priority.$name", tokens.content, tokens.container)
        }
    }

    @Test
    fun textIsReadableOnBackgroundAndSurface() {
        listOf(ColorTokens.background, ColorTokens.surface).forEach { base ->
            assertReadable("textPrimary", ColorTokens.textPrimary, base)
            assertReadable("textSecondary", ColorTokens.textSecondary, base)
            assertReadable("overdue", ColorTokens.overdue, base)
        }
    }

    @Test
    fun onAccentIsReadableOnAccent() {
        assertReadable("onAccent", ColorTokens.onAccent, ColorTokens.accent)
    }

    private fun assertReadable(name: String, foreground: ThemedColor, background: ThemedColor) {
        val light = contrast(foreground.light, background.light)
        val dark = contrast(foreground.dark, background.dark)
        assertTrue("$name: светлая тема, контраст $light < 4.5", light >= MIN_CONTRAST)
        assertTrue("$name: тёмная тема, контраст $dark < 4.5", dark >= MIN_CONTRAST)
    }

    private fun contrast(a: Long, b: Long): Double {
        val (lighter, darker) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
