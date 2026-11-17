package com.prostovasei.simpletasks.widget

import android.content.Context
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import kotlin.math.ceil

/** Измерение той же типографики, что у TextView/Glance HH:mm, включая масштаб шрифта. */
internal fun canvasTimeWidthPx(context: Context, textSp: Int = SizeTokens.CANVAS_TIME_TEXT, extraBold: Boolean = false): Int = ceil(TextPaint().apply {
    typeface = if (extraBold && android.os.Build.VERSION.SDK_INT >= 28) Typeface.create("sans-serif", 800, false) else Typeface.DEFAULT_BOLD
    textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSp.toFloat(), context.resources.displayMetrics)
    fontFeatureSettings = "tnum"
}.measureText("00:00")).toInt()
