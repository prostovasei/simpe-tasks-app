package com.prostovasei.simpletasks.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightMath
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

internal suspend fun healthWidgetViews(context: Context, today: LocalDate, bar: RemoteViews, size: DpSize): RemoteViews {
    val entries = context.widgetEntryPoint().health().observeEntries().first()
    val latest = entries.lastOrNull()
    val period = WeightPeriod.MONTH
    val compact = size.height.value.isFinite() && size.height.value < SizeTokens.WIDGET_MODULE_BAR_THRESHOLD / 2
    val points = WeightMath.inPeriod(entries, period, today)
    return RemoteViews(context.packageName, R.layout.widget_health_root).apply {
        removeAllViews(R.id.widget_health_bar)
        addView(R.id.widget_health_bar, bar)
        setTextViewTextSize(R.id.widget_health_weight, android.util.TypedValue.COMPLEX_UNIT_SP, if (compact) 17f else 32f)
        setTextViewText(R.id.widget_health_weight, latest?.let { "${Weight.format(it.tenths)} кг" }
            ?: context.getString(R.string.health_empty_title))
        if (points.isEmpty()) {
            setViewVisibility(R.id.widget_health_chart, android.view.View.GONE)
            setViewVisibility(R.id.widget_health_dates, android.view.View.GONE)
            if (latest != null) setTextViewText(R.id.widget_health_weight, "${Weight.format(latest.tenths)} кг · за месяц записей нет")
        } else {
            val from = today.minusDays(requireNotNull(period.days).toLong())
            val width = (((size.width.value.takeIf { it.isFinite() } ?: 320f).coerceIn(180f, 440f) - 2 * SizeTokens.WIDGET_EDGE) * 2).toInt()
            val available = (size.height.value.takeIf { it.isFinite() } ?: 280f) - SizeTokens.WIDGET_BAR
            val height = ((available - 44f).coerceIn(18f, 160f) * 2).toInt()
            val chart = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            chart.density = android.util.DisplayMetrics.DENSITY_XHIGH
            val canvas = Canvas(chart)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = context.getColor(R.color.widget_flat_divider)
            repeat(3) { line ->
                val y = (line + 1) * height / 4f
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
            paint.color = context.getColor(R.color.widget_flat_text_primary)
            paint.strokeWidth = 3f
            paint.strokeCap = Paint.Cap.ROUND
            val min = points.minOf { it.tenths }
            val range = (points.maxOf { it.tenths } - min).coerceAtLeast(1)
            val positions = points.map {
                val x = 5f + (width - 10) * (it.date.toEpochDay() - from.toEpochDay()) / requireNotNull(period.days)
                val y = if (points.size == 1) height / 2f else height - 5f - (height - 10f) * (it.tenths - min) / range
                x to y
            }
            positions.zipWithNext().forEach { (a, b) -> canvas.drawLine(a.first, a.second, b.first, b.second, paint) }
            val end = positions.last()
            canvas.drawCircle(end.first, end.second, 5f, paint)
            setImageViewBitmap(R.id.widget_health_chart, chart)
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
            setTextViewText(R.id.widget_health_from, from.format(formatter))
            setTextViewText(R.id.widget_health_to, today.format(formatter))
            setContentDescription(R.id.widget_health_chart, "График веса за месяц. Последняя запись ${Weight.format(latest!!.tenths)} кг")
        }
    }
}
