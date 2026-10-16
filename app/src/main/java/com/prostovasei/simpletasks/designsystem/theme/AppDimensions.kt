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
) {
    /** Плашка в блоке: скруглены только внешние углы блока (итерация 12, вариант A). */
    fun card(position: CardPosition): RoundedCornerShape {
        val round = RadiusTokens.LG.dp
        val flat = RadiusTokens.NONE.dp
        val top = if (position.isBlockStart) round else flat
        val bottom = if (position.isBlockEnd) round else flat
        return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }
}

/** Место плашки в блоке задач одного дня или раздела. */
enum class CardPosition {
    Single, First, Middle, Last;

    val isBlockStart: Boolean get() = this == Single || this == First
    val isBlockEnd: Boolean get() = this == Single || this == Last

    companion object {
        fun of(index: Int, count: Int): CardPosition = when {
            count <= 1 -> Single
            index == 0 -> First
            index == count - 1 -> Last
            else -> Middle
        }
    }
}

@Immutable
data class AppSizes(
    val hairline: Dp = SizeTokens.HAIRLINE.dp,
    val cardGap: Dp = SizeTokens.CARD_GAP.dp,
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
