package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.prostovasei.simpletasks.R

/** Шрифт Manrope (итерация 13, макет PM). Лицензия — licenses/Manrope-OFL.txt. */
val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

/** Стандартная шкала Material 3, но шрифтом Manrope. */
internal fun appTypography(): Typography {
    val base = Typography()
    fun TextStyle.manrope() = copy(fontFamily = Manrope)
    return Typography(
        displayLarge = base.displayLarge.manrope(),
        displayMedium = base.displayMedium.manrope(),
        displaySmall = base.displaySmall.manrope(),
        headlineLarge = base.headlineLarge.manrope(),
        headlineMedium = base.headlineMedium.manrope(),
        headlineSmall = base.headlineSmall.manrope(),
        titleLarge = base.titleLarge.manrope(),
        titleMedium = base.titleMedium.manrope(),
        titleSmall = base.titleSmall.manrope(),
        bodyLarge = base.bodyLarge.manrope(),
        bodyMedium = base.bodyMedium.manrope(),
        bodySmall = base.bodySmall.manrope(),
        labelLarge = base.labelLarge.manrope(),
        labelMedium = base.labelMedium.manrope(),
        labelSmall = base.labelSmall.manrope(),
    )
}
