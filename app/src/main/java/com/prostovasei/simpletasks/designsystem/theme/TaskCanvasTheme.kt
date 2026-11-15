package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.prostovasei.simpletasks.designsystem.tokens.TaskCanvasTokens

/** Изолированная итерация #247: остальные экраны ещё не согласованы. */
val LocalTaskCanvas = staticCompositionLocalOf { false }

@Composable
fun TaskCanvasTheme(enabled: Boolean = true, content: @Composable () -> Unit) {
    if (!enabled) { content(); return }
    AppTheme(darkTheme = false) {
        val colors = AppTheme.colors.copy(
            background = Color(TaskCanvasTokens.background.light),
            divider = Color(TaskCanvasTokens.divider.light),
        )
        CompositionLocalProvider(LocalAppColors provides colors, LocalTaskCanvas provides true, content = content)
    }
}
