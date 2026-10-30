package com.prostovasei.simpletasks.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.ColorTokens
import com.prostovasei.simpletasks.designsystem.tokens.RadiusTokens
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.designsystem.tokens.SpacingTokens
import com.prostovasei.simpletasks.designsystem.tokens.ThemedColor
import com.prostovasei.simpletasks.ui.common.DONE_ALPHA
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Размеры и цвета строк виджета в ресурсах Android (итерация 42) — копия токенов дизайн-системы: расхождение валит сборку. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "mdpi")
class WidgetRowResourcesTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun dimensMatchTokens() {
        val dimens = mapOf(
            R.dimen.widget_row to SizeTokens.WIDGET_ROW,
            R.dimen.widget_period_row to SizeTokens.WIDGET_PERIOD_ROW,
            R.dimen.widget_now_row to SizeTokens.WIDGET_NOW_ROW,
            R.dimen.widget_time to SizeTokens.WIDGET_TIME,
            R.dimen.widget_rail to SizeTokens.WIDGET_RAIL,
            R.dimen.widget_node to SizeTokens.WIDGET_NODE,
            R.dimen.widget_node_check to SizeTokens.NODE_CHECK,
            R.dimen.widget_period_stub to SizeTokens.WIDGET_PERIOD_STUB,
            R.dimen.widget_now_dot to SizeTokens.WIDGET_NOW_DOT,
            R.dimen.widget_meta_icon to SizeTokens.WIDGET_META_ICON,
            R.dimen.widget_reminder_icon to SizeTokens.WIDGET_REMINDER_ICON,
            R.dimen.widget_list_square to SizeTokens.WIDGET_LIST_SQUARE,
            R.dimen.widget_list_glyph to SizeTokens.WIDGET_LIST_GLYPH,
            R.dimen.widget_card_inset to SizeTokens.WIDGET_CARD_INSET,
            R.dimen.widget_progress to SizeTokens.WIDGET_PROGRESS,
            R.dimen.widget_fold_icon to SizeTokens.WIDGET_FOLD_ICON,
            R.dimen.widget_fade to SizeTokens.WIDGET_FADE,
            R.dimen.widget_edge to SizeTokens.WIDGET_EDGE,
            R.dimen.widget_list_edge to SizeTokens.WIDGET_LIST_EDGE,
            R.dimen.widget_day_gap to SizeTokens.WIDGET_DAY_GAP,
            R.dimen.widget_hairline to SizeTokens.HAIRLINE,
            R.dimen.widget_space_xxs to SpacingTokens.XXS,
            R.dimen.widget_space_xs to SpacingTokens.XS,
            R.dimen.widget_space_sm to SpacingTokens.SM,
            R.dimen.widget_radius_sm to RadiusTokens.SM,
            R.dimen.widget_radius_md to RadiusTokens.MD,
            R.dimen.widget_radius_lg to RadiusTokens.LG,
            R.dimen.widget_radius_xl to RadiusTokens.XL,
        )
        dimens.forEach { (res, token) ->
            assertEquals(context.resources.getResourceEntryName(res), token.toFloat(), context.resources.getDimension(res))
        }
    }

    private fun colors(dark: Boolean): Map<Int, Long> {
        fun ThemedColor.pick() = if (dark) this.dark else light
        return mapOf(
            R.color.widget_fade_end to ColorTokens.background.pick(),
            R.color.widget_surface to ColorTokens.surface.pick(),
            R.color.widget_selected to ColorTokens.selected.pick(),
            R.color.widget_divider to ColorTokens.divider.pick(),
            R.color.widget_now to ColorTokens.accent.pick(),
            R.color.widget_text_secondary to ColorTokens.textSecondary.pick(),
            R.color.widget_overdue to ColorTokens.overdue.pick(),
            R.color.widget_on_accent to ColorTokens.onAccent.pick(),
            R.color.widget_on_list_square to ColorTokens.onListSquare.pick(),
        )
    }

    private fun check(dark: Boolean) {
        colors(dark).forEach { (res, token) ->
            assertEquals(context.resources.getResourceEntryName(res), token.toInt(), context.getColor(res))
        }
        // Выполненная задача (0.42.2): те же цвета на 70%.
        fun ThemedColor.done() = androidx.compose.ui.graphics.Color(if (dark) this.dark else light).copy(alpha = DONE_ALPHA).toArgb()
        mapOf(
            R.color.widget_text_done to ColorTokens.textPrimary.done(),
            R.color.widget_icon_done to ColorTokens.textSecondary.done(),
            R.color.widget_accent_done to ColorTokens.accent.done(),
        ).forEach { (res, token) -> assertEquals(context.resources.getResourceEntryName(res), token, context.getColor(res)) }
    }

    @Test
    fun lightColorsMatchTokens() = check(dark = false)

    @Test
    @Config(qualifiers = "+night")
    fun darkColorsMatchTokens() = check(dark = true)
}
