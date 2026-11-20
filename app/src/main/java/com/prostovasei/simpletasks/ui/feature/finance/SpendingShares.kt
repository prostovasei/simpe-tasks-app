package com.prostovasei.simpletasks.ui.feature.finance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareColors
import com.prostovasei.simpletasks.designsystem.tokens.FinanceShareSizes
import com.prostovasei.simpletasks.domain.model.finance.Money
import com.prostovasei.simpletasks.domain.model.finance.MonthSpending
import com.prostovasei.simpletasks.domain.model.finance.financeThousands
import com.prostovasei.simpletasks.domain.model.finance.sharePercentages
import kotlin.math.ceil

internal fun sharesDescription(title: String, rows: List<MonthSpending.Rank>, total: Long): String =
    title + ". " + rows.zip(sharePercentages(rows, total)).joinToString(". ") { (row, percent) ->
        "${row.name}, $percent%, ${Money.format(row.amount)}"
    }

internal fun sharesBitmap(rows: List<MonthSpending.Rank>, total: Long, width: Int, density: Float, fontScale: Float, ink: Int): Bitmap {
    val percentages = sharePercentages(rows, total)
    val gap = FinanceShareSizes.gap * density
    val inset = FinanceShareSizes.inset * density
    val strip = FinanceShareSizes.strip * density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bold = Typeface.DEFAULT_BOLD
    paint.typeface = bold
    paint.textSize = FinanceShareSizes.percent * density * fontScale
    var preceding = 0L
    val starts = rows.map {
        val start = (preceding.toDouble() / total * width).toFloat()
        preceding += it.amount
        start
    }
    val ends = mutableListOf<Float>()
    val labels = percentages.mapIndexed { index, percent ->
        val text = "$percent%"
        val measured = paint.measureText(text)
        val x = starts[index].coerceAtMost((width - measured).coerceAtLeast(0f))
        val available = ends.indexOfFirst { it <= x }
        val row = if (available >= 0) available else ends.size.also { ends.add(0f) }
        ends[row] = x + measured + gap
        Triple(text, x, row)
    }
    val labelHeight = paint.fontSpacing
    val barTop = ends.size * labelHeight + gap
    val chipTop = barTop + strip + gap
    paint.textSize = FinanceShareSizes.label * density * fontScale
    val textHeight = paint.fontSpacing
    val height = chipTop + 2 * textHeight + 2 * inset
    val bitmap = Bitmap.createBitmap(width, ceil(height).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    paint.textSize = FinanceShareSizes.percent * density * fontScale
    paint.color = ink
    labels.forEach { (text, x, row) -> canvas.drawText(text, x, row * labelHeight - paint.ascent(), paint) }
    val cell = width / 5f
    rows.forEachIndexed { index, row ->
        val color = FinanceShareColors.ranks[index].toInt()
        val left = starts[index]
        val right = starts.getOrNull(index + 1) ?: width.toFloat()
        val margin = minOf(gap / 2, (right - left) / 4)
        paint.color = color
        canvas.drawRoundRect(left + margin, barTop, right - margin, barTop + strip, strip / 2, strip / 2, paint)
        val chipLeft = index * cell + gap / 2
        val chipRight = (index + 1) * cell - gap / 2
        paint.color = ColorUtils.blendARGB(Color.WHITE, color, FinanceShareColors.tintFraction)
        canvas.drawRoundRect(chipLeft, chipTop, chipRight, height, height - chipTop, height - chipTop, paint)
        val textLeft = chipLeft + inset
        paint.textSize = FinanceShareSizes.label * density * fontScale
        paint.color = ink
        canvas.save()
        canvas.clipRect(textLeft, chipTop, chipRight - inset, height)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(row.name, textLeft, chipTop + inset - paint.ascent(), paint)
        paint.typeface = bold
        val amount = financeThousands(row.amount)
        val room = (chipRight - inset - textLeft).coerceAtLeast(1f)
        paint.textSize *= minOf(1f, room / paint.measureText(amount))
        canvas.drawText(amount, textLeft, chipTop + inset + textHeight - paint.ascent(), paint)
        canvas.restore()
    }
    return bitmap
}
