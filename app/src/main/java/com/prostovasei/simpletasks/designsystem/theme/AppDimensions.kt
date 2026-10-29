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
    /** Плавающая нижняя панель. */
    val nav: RoundedCornerShape = RoundedCornerShape(RadiusTokens.NAV.dp),
    /** Шторка новой задачи: скругление сверху, как у нижней панели. */
    val sheetTop: RoundedCornerShape = RoundedCornerShape(topStart = RadiusTokens.NAV.dp, topEnd = RadiusTokens.NAV.dp),
)

@Immutable
data class AppSizes(
    val hairline: Dp = SizeTokens.HAIRLINE.dp,
    val cardGap: Dp = SizeTokens.CARD_GAP.dp,
    val listSquare: Dp = SizeTokens.LIST_SQUARE.dp,
    val listGlyph: Dp = SizeTokens.LIST_GLYPH.dp,
    val listSquareLarge: Dp = SizeTokens.LIST_SQUARE_LARGE.dp,
    val listGlyphLarge: Dp = SizeTokens.LIST_GLYPH_LARGE.dp,
    val iconTile: Dp = SizeTokens.ICON_TILE.dp,
    val navBar: Dp = SizeTokens.NAV_BAR.dp,
    val navTab: Dp = SizeTokens.NAV_TAB.dp,
    val navAdd: Dp = SizeTokens.NAV_ADD.dp,
    val navShadow: Dp = SizeTokens.NAV_SHADOW.dp,
    val checkbox: Dp = SizeTokens.CHECKBOX.dp,
    val checkboxStroke: Dp = SizeTokens.CHECKBOX_STROKE.dp,
    val swatch: Dp = SizeTokens.SWATCH.dp,
    val progressHeight: Dp = SizeTokens.PROGRESS_HEIGHT.dp,
    val listDot: Dp = SizeTokens.LIST_DOT.dp,
    val metaIcon: Dp = SizeTokens.META_ICON.dp,
    val reminderIcon: Dp = SizeTokens.REMINDER_ICON.dp,
    val pickerMaxHeight: Dp = SizeTokens.PICKER_MAX_HEIGHT.dp,
    val colorSwatch: Dp = SizeTokens.COLOR_SWATCH.dp,
    val icon: Dp = SizeTokens.ICON.dp,
    val checkboxTouch: Dp = SizeTokens.CHECKBOX_TOUCH.dp,
    val dustParticle: Dp = SizeTokens.DUST_PARTICLE.dp,
    val emptyDash: Dp = SizeTokens.EMPTY_DASH.dp,
    val ghostRow: Dp = SizeTokens.GHOST_ROW.dp,
    val ghostBar: Dp = SizeTokens.GHOST_BAR.dp,
    val emptyArrow: Dp = SizeTokens.EMPTY_ARROW.dp,
    val sheetHandleWidth: Dp = SizeTokens.SHEET_HANDLE_WIDTH.dp,
    val sheetHandleHeight: Dp = SizeTokens.SHEET_HANDLE_HEIGHT.dp,
    val toolButton: Dp = SizeTokens.TOOL_BUTTON.dp,
    val toolIcon: Dp = SizeTokens.TOOL_ICON.dp,
    val weekHour: Dp = SizeTokens.WEEK_HOUR.dp,
    val weekGutter: Dp = SizeTokens.WEEK_GUTTER.dp,
    val weekHeader: Dp = SizeTokens.WEEK_HEADER.dp,
    val weekClear: Dp = SizeTokens.WEEK_CLEAR.dp,
    val clearDot: Dp = SizeTokens.CLEAR_DOT.dp,
    val clearGlyph: Dp = SizeTokens.CLEAR_GLYPH.dp,
    val weekKnob: Dp = SizeTokens.WEEK_KNOB.dp,
    val dayTile: Dp = SizeTokens.DAY_TILE.dp,
    val dialogMaxWidth: Dp = SizeTokens.DIALOG_MAX_WIDTH.dp,
    val dialogRow: Dp = SizeTokens.DIALOG_ROW.dp,
    val dialogBadge: Dp = SizeTokens.DIALOG_BADGE.dp,
    val dialogBadgeIcon: Dp = SizeTokens.DIALOG_BADGE_ICON.dp,
    val stripHeight: Dp = SizeTokens.STRIP_HEIGHT.dp,
    val stripIcon: Dp = SizeTokens.STRIP_ICON.dp,
    val dateCell: Dp = SizeTokens.DATE_CELL.dp,
    val dateDay: Dp = SizeTokens.DATE_DAY.dp,
    val dateArrow: Dp = SizeTokens.DATE_ARROW.dp,
    val weekHourMin: Dp = SizeTokens.WEEK_HOUR_MIN.dp,
    val dateRangeMin: Dp = SizeTokens.DATE_RANGE_MIN.dp,
    val dateButtonMin: Dp = SizeTokens.DATE_BUTTON_MIN.dp,
    val doneCircle: Dp = SizeTokens.DONE_CIRCLE.dp,
    val doneCheck: Dp = SizeTokens.DONE_CHECK.dp,
)
