package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("AppColors не предоставлены: оберните UI в AppTheme") }
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
val LocalAppSizes = staticCompositionLocalOf { AppSizes() }

/**
 * Корневая тема. Динамические цвета Material You намеренно не используются:
 * все цвета приходят только из designsystem/tokens.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = appColors(isDark = darkTheme)
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalAppSizes provides AppSizes(),
    ) {
        MaterialTheme(colorScheme = colors.toMaterialColorScheme(), typography = appTypography(), content = content)
    }
}

/** Доступ к токенам внутри Composable: `AppTheme.colors.accent`, `AppTheme.spacing.md`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val spacing: AppSpacing
        @Composable @ReadOnlyComposable get() = LocalAppSpacing.current
    val shapes: AppShapes
        @Composable @ReadOnlyComposable get() = LocalAppShapes.current
    val sizes: AppSizes
        @Composable @ReadOnlyComposable get() = LocalAppSizes.current
}

/** Стандартные компоненты Material 3 (Snackbar, TextField, диалоги) тоже получают цвета из токенов. */
private fun AppColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        background = background,
        onBackground = textPrimary,
        surface = background,
        onSurface = textPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = textSecondary,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerHigh = surface,
        error = overdue,
        outline = divider,
        outlineVariant = divider,
    )
}
