package com.prostovasei.simpletasks.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import java.time.LocalDate
import kotlin.math.ceil

/** Aligns the actual glyph bounds of the day number and the two-line caption. */
internal fun widgetBarDate(date: LocalDate, width: Int, fontScale: Float, ink: Int, secondary: Int): Bitmap {
    val scale = 2f
    val (day, weekday, month) = WidgetDates.tile(date)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = SizeTokens.WIDGET_DATE_SMALL_SP * scale * fontScale
        color = secondary
    }
    fun bounds(text: String) = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
    val weekdayBounds = bounds(weekday)
    val monthBounds = bounds(month)
    val lineHeight = (SizeTokens.WIDGET_DATE_SMALL_SP + 1) * scale * fontScale
    val captionHeight = lineHeight + monthBounds.bottom - weekdayBounds.top
    val captionWidth = maxOf(paint.measureText(weekday), paint.measureText(month))
    val captionSize = paint.textSize
    paint.textSize *= captionHeight / bounds(day).height().coerceAtLeast(1)
    paint.fontFeatureSettings = "'tnum'"
    val numberBounds = bounds(day)
    val numberWidth = paint.measureText(day)
    val gap = SizeTokens.WIDGET_DATE_GAP * scale
    val bitmap = Bitmap.createBitmap(width * 2, ceil(captionHeight + 2 * scale).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val numberX = (bitmap.width - numberWidth - gap - captionWidth) / 2
    val centerY = bitmap.height / 2f
    paint.color = ink
    canvas.drawText(day, numberX, centerY - (numberBounds.top + numberBounds.bottom) / 2f, paint)
    paint.textSize = captionSize
    paint.color = secondary
    val textX = numberX + numberWidth + gap
    val baseline = centerY - captionHeight / 2 - weekdayBounds.top
    canvas.drawText(weekday, textX, baseline, paint)
    canvas.drawText(month, textX, baseline + lineHeight, paint)
    return bitmap
}
