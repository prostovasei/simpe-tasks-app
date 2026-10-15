package com.prostovasei.simpletasks.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens

@Immutable
data class AppSpacing(
    val xxs: Dp = SpacingTokens.XXS.dp,
    val xs: Dp = SpacingTokens.XS.dp,
    val sm: Dp = SpacingTokens.SM.dp,
    val md: Dp = SpacingTokens.MD.dp,
    val lg: Dp = SpacingTokens.LG.dp,
    val xl: Dp = SpacingTokens.XL.dp,
)

@Immutable
data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(RadiusTokens.SM.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(RadiusTokens.MD.dp),
    val large: RoundedCornerShape = RoundedCornerShape(RadiusTokens.LG.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(RadiusTokens.XL.dp),
    /** Скругление только сверху — карточка, выезжающая снизу. */
    val extraLargeTop: RoundedCornerShape = RoundedCornerShape(topStart = RadiusTokens.XL.dp, topEnd = RadiusTokens.XL.dp),
)

@Immutable
data class AppSizes(
    val hairline: Dp = SizeTokens.HAIRLINE.dp,
    val checkbox: Dp = SizeTokens.CHECKBOX.dp,
    val checkboxStroke: Dp = SizeTokens.CHECKBOX_STROKE.dp,
    val swatch: Dp = SizeTokens.SWATCH.dp,
    val progressHeight: Dp = SizeTokens.PROGRESS_HEIGHT.dp,
    val listDot: Dp = SizeTokens.LIST_DOT.dp,
    val metaIcon: Dp = SizeTokens.META_ICON.dp,
    val pickerMaxHeight: Dp = SizeTokens.PICKER_MAX_HEIGHT.dp,
    val colorSwatch: Dp = SizeTokens.COLOR_SWATCH.dp,
    val icon: Dp = SizeTokens.ICON.dp,
    val checkboxTouch: Dp = SizeTokens.CHECKBOX_TOUCH.dp,
)
